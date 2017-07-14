package com.mycompany.app.discovery;

import com.facebook.presto.client.NodeVersion;
import com.facebook.presto.connector.ConnectorId;
import com.facebook.presto.connector.system.GlobalSystemConnector;
import com.facebook.presto.failureDetector.FailureDetector;
import com.facebook.presto.metadata.AllNodes;
import com.facebook.presto.metadata.InternalNodeManager;
import com.facebook.presto.metadata.PrestoNode;
import com.facebook.presto.metadata.RemoteNodeState;
import com.facebook.presto.spi.Node;
import com.facebook.presto.spi.NodeState;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import io.airlift.discovery.client.ServiceDescriptor;
import io.airlift.discovery.client.ServiceSelector;
import io.airlift.discovery.client.ServiceType;
import io.airlift.http.client.HttpClient;
import io.airlift.node.NodeInfo;
import io.airlift.units.Duration;
import javax.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.facebook.presto.spi.NodeState.ACTIVE;
import static com.facebook.presto.spi.NodeState.INACTIVE;
import static com.facebook.presto.spi.NodeState.SHUTTING_DOWN;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Sets.difference;
import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

/**
 * Created by tony on 7/13/17.
 */
public class DiscoveryNodeManager
        implements InternalNodeManager
{
    private static final Duration MAX_AGE = new Duration(5, TimeUnit.SECONDS);

    private long lastUpdateTimestamp;

    private AllNodes allNodes;

    private final PrestoNode currentNode;

    private Set<Node> coordinators;

    private final NodeInfo nodeInfo;

    private final String expectedNodeVersion;

    private final HttpClient httpClient;

    private final FailureDetector failureDetector;

    private final ConcurrentHashMap<String, RemoteNodeState> nodeStates = new ConcurrentHashMap<>();

    private final ServiceSelector serviceSelector;

    private static final Splitter CONNECTOR_ID_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    @Inject
    public DiscoveryNodeManager(
            @ServiceType("presto") ServiceSelector serviceSelector,
            FailureDetector failureDetector,
            String expectedNodeVersion,
            HttpClient httpClient,
            NodeInfo nodeInfo
    )
    {
        this.serviceSelector = serviceSelector;
        this.failureDetector = failureDetector;
        this.httpClient = httpClient;
        this.nodeInfo = nodeInfo;
        this.expectedNodeVersion = expectedNodeVersion;
        this.currentNode = refreshNodesInternal();
    }

    private synchronized PrestoNode refreshNodesInternal()
    {
        lastUpdateTimestamp = System.nanoTime();

        Set<ServiceDescriptor> services = serviceSelector.selectAllServices().stream()
                .filter(service -> !failureDetector.getFailed().contains(service))
                .collect(toImmutableSet());

        PrestoNode currentNode = null;
        ImmutableSet.Builder<Node> activeNodesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<Node> inactiveNodesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<Node> shuttingDownNodesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<Node> coordinatorsBuilder = ImmutableSet.builder();
        ImmutableSetMultimap.Builder<ConnectorId, Node> byConnectorIdBuilder = ImmutableSetMultimap.builder();

        for(ServiceDescriptor service : services){
            URI uri = getHttpUri(service);
            NodeVersion nodeVersion = getNodeVersion(service);
            boolean coordinator = isCoordinator(service);
            if(uri != null && nodeVersion != null) {
                PrestoNode node = new PrestoNode(service.getNodeId(), uri, nodeVersion, coordinator);
                NodeState nodeState = getNodeState(node);

                if(node.getNodeIdentifier().equals(nodeInfo.getNodeId())){
                    currentNode = node;
                }

                switch (nodeState) {
                    case ACTIVE:
                        activeNodesBuilder.add(node);
                        if (coordinator) {
                            coordinatorsBuilder.add(node);
                        }

                        String connectorIds = service.getProperties().get("connectorIds");
                        if (connectorIds != null) {
                            connectorIds = connectorIds.toLowerCase(ENGLISH);
                            for (String connectorId : CONNECTOR_ID_SPLITTER.split(connectorIds)) {
                                byConnectorIdBuilder.put(new ConnectorId(connectorId), node);
                            }
                        }

                        byConnectorIdBuilder.put(new ConnectorId(GlobalSystemConnector.NAME), node);
                        break;
                    case INACTIVE:
                        inactiveNodesBuilder.add(node);
                        break;
                    case SHUTTING_DOWN:
                        shuttingDownNodesBuilder.add(node);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown node state " + nodeState);
                }
            }
        }

        if(allNodes != null){
            Sets.SetView<Node> missingNodes = difference(allNodes.getActiveNodes(), Sets.union(activeNodesBuilder.build(), shuttingDownNodesBuilder.build()));
            for(Node missingNode : missingNodes){
                //TODO: log these nodes
            }
        }

        allNodes = new AllNodes(activeNodesBuilder.build(), inactiveNodesBuilder.build(), shuttingDownNodesBuilder.build());

        coordinators = coordinatorsBuilder.build();

        requireNonNull(currentNode);
        return currentNode;
    }

    @Override
    public Set<Node> getNodes(NodeState state)
    {
        switch (state) {
            case ACTIVE:
                return getAllNodes().getActiveNodes();
            case INACTIVE:
                return getAllNodes().getInactiveNodes();
            case SHUTTING_DOWN:
                return getAllNodes().getShuttingDownNodes();
            default:
                throw new IllegalArgumentException("Unknown node state " + state);
        }
    }

    @Override
    public Set<com.facebook.presto.spi.Node> getActiveConnectorNodes(ConnectorId connectorId)
    {
        return null;
    }

    private synchronized void refreshIfNecessary()
    {
        if (Duration.nanosSince(lastUpdateTimestamp).compareTo(MAX_AGE) > 0) {
            refreshNodesInternal();
        }
    }

    private NodeState getNodeState(PrestoNode node)
    {
        if (expectedNodeVersion.equals(node.getVersion())) {
            if (isNodeShuttingDown(node.getNodeIdentifier())) {
                return SHUTTING_DOWN;
            }
            else {
                return ACTIVE;
            }
        }
        else {
            return INACTIVE;
        }
    }

    private boolean isNodeShuttingDown(String nodeId)
    {
        Optional<NodeState> remoteNodeState = nodeStates.containsKey(nodeId)
                ? nodeStates.get(nodeId).getNodeState()
                : Optional.empty();
        return remoteNodeState.isPresent() && remoteNodeState.get().equals(SHUTTING_DOWN);
    }

    private static URI getHttpUri(ServiceDescriptor descriptor)
    {
        for (String type : asList("http", "https")) {
            String url = descriptor.getProperties().get(type);
            if (url != null) {
                try {
                    return new URI(url);
                }
                catch (URISyntaxException ignored) {
                }
            }
        }
        return null;
    }

    private static NodeVersion getNodeVersion(ServiceDescriptor descriptor)
    {
        String nodeVersion = descriptor.getProperties().get("node_version");
        return nodeVersion == null ? null : new NodeVersion(nodeVersion);
    }

    private static boolean isCoordinator(ServiceDescriptor service)
    {
        return Boolean.parseBoolean(service.getProperties().get("coordinator"));
    }

    @Override
    public Node getCurrentNode()
    {
        return currentNode;
    }

    @Override
    public Set<Node> getCoordinators()
    {
        refreshIfNecessary();
        return coordinators;
    }

    @Override
    public AllNodes getAllNodes()
    {
        refreshIfNecessary();
        return allNodes;
    }

    @Override
    public void refreshNodes()
    {
        refreshNodesInternal();
    }
}
