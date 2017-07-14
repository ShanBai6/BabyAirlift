package com.mycompany.app;

import com.facebook.presto.client.NodeVersion;
import com.facebook.presto.failureDetector.FailureDetector;
import com.facebook.presto.metadata.AllNodes;
import com.facebook.presto.metadata.PrestoNode;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.Node;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.mycompany.app.discovery.DiscoveryNodeManager;
import io.airlift.discovery.client.ServiceDescriptor;
import io.airlift.discovery.client.ServiceSelector;
import io.airlift.discovery.client.testing.StaticServiceSelector;
import io.airlift.http.client.HttpStatus;
import io.airlift.http.client.testing.TestingHttpClient;
import io.airlift.http.client.testing.TestingResponse;
import io.airlift.node.NodeConfig;
import io.airlift.node.NodeInfo;

import org.testng.annotations.BeforeMethod;
import io.airlift.http.client.HttpClient;
import org.testng.annotations.Test;

import static com.facebook.presto.spi.NodeState.ACTIVE;
import static com.facebook.presto.spi.NodeState.INACTIVE;
import static com.facebook.presto.testing.assertions.Assert.assertEquals;
import static io.airlift.discovery.client.ServiceDescriptor.serviceDescriptor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.airlift.testing.Assertions.assertEqualsIgnoreOrder;

/**
 * Created by tony on 7/14/17.
 */
public class TestDiscoveryNodeManager
{
    private final NodeInfo nodeInfo = new NodeInfo("test");
    private NodeVersion expectedVersion;
    private List<PrestoNode> activeNodes;
    private List<PrestoNode> inactiveNodes;
    private PrestoNode coordinator;
    private ServiceSelector selector;
    private HttpClient testHttpClient;

    @BeforeMethod
    public void setup()
    {
        testHttpClient = new TestingHttpClient(input -> new TestingResponse(HttpStatus.OK, ArrayListMultimap.create(), ACTIVE.name().getBytes()));

        expectedVersion = new NodeVersion("1");
        coordinator = new PrestoNode(UUID.randomUUID().toString(), URI.create("https://192.0.2.8"), expectedVersion, false);
        activeNodes = ImmutableList.of(
                new PrestoNode(nodeInfo.getNodeId(), URI.create("http://192.0.1.1"), expectedVersion, false),
                new PrestoNode(UUID.randomUUID().toString(), URI.create("http://192.0.2.1:8080"), expectedVersion, false),
                new PrestoNode(UUID.randomUUID().toString(), URI.create("http://192.0.2.3"), expectedVersion, false),
                coordinator);
        inactiveNodes = ImmutableList.of(
                new PrestoNode(UUID.randomUUID().toString(), URI.create("https://192.0.3.9"), NodeVersion.UNKNOWN, false),
                new PrestoNode(UUID.randomUUID().toString(), URI.create("https://192.0.4.9"), new NodeVersion("2"), false)
        );

        List<ServiceDescriptor> descriptors = new ArrayList<>();
        for (PrestoNode node : Iterables.concat(activeNodes, inactiveNodes)) {
            descriptors.add(serviceDescriptor("presto")
                    .setNodeId(node.getNodeIdentifier())
                    .addProperty("http", node.getHttpUri().toString())
                    .addProperty("node_version", node.getNodeVersion().toString())
                    .addProperty("coordinator", String.valueOf(node.equals(coordinator)))
                    .build());
        }

        selector = new StaticServiceSelector(descriptors);
    }

    @Test
    public void testGetAllNodes()
            throws Exception
    {
        DiscoveryNodeManager manager = new DiscoveryNodeManager(selector, new FailureDetector()
        {
            @Override
            public Set<ServiceDescriptor> getFailed()
            {
                return ImmutableSet.of();
            }

            @Override
            public State getState(HostAddress hostAddress)
            {
                return State.UNKNOWN;
            }
        }, expectedVersion.toString(), testHttpClient, nodeInfo);
        AllNodes allNodes = manager.getAllNodes();

        Set<Node> activeNodes = allNodes.getActiveNodes();
        assertEqualsIgnoreOrder(activeNodes, this.activeNodes);

        Set<Node> inactiveNodes = allNodes.getInactiveNodes();
        assertEqualsIgnoreOrder(inactiveNodes, this.inactiveNodes);
    }

    @Test
    public void testGetCurrentNode()
    {
        Node expected = activeNodes.get(0);
        NodeInfo nodeInfo = new NodeInfo(new NodeConfig().setEnvironment("test").setNodeId(expected.getNodeIdentifier()));
        DiscoveryNodeManager manager = new DiscoveryNodeManager(selector, new FailureDetector()
        {
            @Override
            public Set<ServiceDescriptor> getFailed()
            {
                return ImmutableSet.of();
            }

            @Override
            public State getState(HostAddress hostAddress)
            {
                return State.UNKNOWN;
            }
        }, expectedVersion.toString(), testHttpClient, nodeInfo);

        assertEquals(manager.getCurrentNode(), expected);
    }

    @Test
    public void testGetCoordinators()
            throws Exception
    {
        DiscoveryNodeManager manager = new DiscoveryNodeManager(selector, new FailureDetector()
        {
            @Override
            public Set<ServiceDescriptor> getFailed()
            {
                return ImmutableSet.of();
            }

            @Override
            public State getState(HostAddress hostAddress)
            {
                return State.UNKNOWN;
            }
        }, expectedVersion.toString(), testHttpClient, nodeInfo);
        assertEquals(manager.getCoordinators(), ImmutableSet.of(coordinator));
    }
}
