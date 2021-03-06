package com.mycompany.app;

import com.facebook.presto.discovery.EmbeddedDiscoveryModule;
import com.facebook.presto.eventlistener.EventListenerManager;
import com.facebook.presto.eventlistener.EventListenerModule;
import com.facebook.presto.execution.resourceGroups.ResourceGroupManager;
import com.facebook.presto.execution.scheduler.NodeSchedulerConfig;
import com.facebook.presto.metadata.Catalog;
import com.facebook.presto.metadata.CatalogManager;
import com.facebook.presto.metadata.StaticCatalogStore;
import com.facebook.presto.security.AccessControlManager;
import com.facebook.presto.security.AccessControlModule;
import com.facebook.presto.server.GracefulShutdownModule;
import com.facebook.presto.server.PluginManager;
import com.facebook.presto.server.PrestoServer;
import com.facebook.presto.server.ServerConfig;
import com.facebook.presto.server.ServerMainModule;
import com.facebook.presto.server.security.ServerSecurityModule;
import com.facebook.presto.sql.parser.SqlParserOptions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.discovery.client.Announcer;
import io.airlift.discovery.client.DiscoveryModule;
import io.airlift.discovery.client.ServiceAnnouncement;
import io.airlift.event.client.HttpEventModule;
import io.airlift.event.client.JsonEventModule;
import io.airlift.http.server.HttpServerModule;
import io.airlift.jaxrs.JaxrsModule;
import io.airlift.jmx.JmxHttpModule;
import io.airlift.jmx.JmxModule;
import io.airlift.json.JsonModule;
import io.airlift.log.LogJmxModule;
import io.airlift.log.Logger;
import io.airlift.node.NodeModule;
import io.airlift.tracetoken.TraceTokenModule;
import org.weakref.jmx.guice.MBeanModule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static io.airlift.discovery.client.ServiceAnnouncement.serviceAnnouncement;
import static java.util.Objects.requireNonNull;

public class App
{
//    private final SqlParserOptions sqlParserOptions;
//
//    public App()
//    {
//        this(new SqlParserOptions());
//    }
//
//    public App(SqlParserOptions sqlParserOptions)
//    {
//        this.sqlParserOptions = requireNonNull(sqlParserOptions, "sqlParserOptions is null");
//    }

    public static void main(String[] args)
            throws Exception
    {
        new App().run();
    }

    private void run()
    {
        Logger log = Logger.get(App.class);

        System.setProperty("node.environment", "test");
        Bootstrap app = new Bootstrap(
                new NodeModule(),
                new DiscoveryModule(),
                new HttpServerModule(),
                new JsonModule(),
                new JaxrsModule(true),
                new TraceTokenModule(),
                new HttpEventModule(),
                new EmbeddedDiscoveryModule(),
                new EventListenerModule(),
                new MainModule());

        try {
            Injector injector = app.strictConfig().initialize();

            injector.getInstance(EventListenerManager.class).loadConfiguredEventListener();

            injector.getInstance(Announcer.class).start();

            log.info("======== SERVER STARTED ========");
//        Logger log = Logger.get(App.class);
//
//        System.setProperty("node.environment", "test");
//        ImmutableList.Builder<Module> modules = ImmutableList.builder();
//        modules.add(
//                new NodeModule(),
//                new DiscoveryModule(),
//                new HttpServerModule(),
//                new JsonModule(),
//                new JaxrsModule(true),
//                new MBeanModule(),
//                new JmxModule(),
//                new JmxHttpModule(),
//                new LogJmxModule(),
//                new TraceTokenModule(),
//                new JsonEventModule(),
//                new HttpEventModule(),
//                new EmbeddedDiscoveryModule(),
//                new ServerSecurityModule(),
//                new EventListenerModule(),
//                new MainModule(sqlParserOptions));
//
//        Bootstrap app = new Bootstrap(modules.build());
//
//        try {
//            Injector injector = app.strictConfig().initialize();
//
//            injector.getInstance(PluginManager.class).loadPlugins();
//
//            injector.getInstance(StaticCatalogStore.class).loadCatalogs();
//
//            // TODO: remove this huge hack
//            updateConnectorIds(
//                    injector.getInstance(Announcer.class),
//                    injector.getInstance(CatalogManager.class),
//                    injector.getInstance(ServerConfig.class),
//                    injector.getInstance(NodeSchedulerConfig.class));
//
//            injector.getInstance(ResourceGroupManager.class).loadConfigurationManager();
//            injector.getInstance(AccessControlManager.class).loadSystemAccessControl();
//            injector.getInstance(EventListenerManager.class).loadConfiguredEventListener();
//
//            injector.getInstance(Announcer.class).start();
//
//            log.info("======== SERVER STARTED ========");
        }
        catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

//    private static void updateConnectorIds(Announcer announcer, CatalogManager metadata, ServerConfig serverConfig, NodeSchedulerConfig schedulerConfig)
//    {
//        // get existing announcement
//        ServiceAnnouncement announcement = getPrestoAnnouncement(announcer.getServiceAnnouncements());
//
//        // get existing connectorIds
//        String property = nullToEmpty(announcement.getProperties().get("connectorIds"));
//        List<String> values = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(property);
//        Set<String> connectorIds = new LinkedHashSet<>(values);
//
//        // automatically build connectorIds if not configured
//        if (connectorIds.isEmpty()) {
//            List<Catalog> catalogs = metadata.getCatalogs();
//            // if this is a dedicated coordinator, only add jmx
//            if (serverConfig.isCoordinator() && !schedulerConfig.isIncludeCoordinator()) {
//                catalogs.stream()
//                        .map(Catalog::getConnectorId)
//                        .filter(connectorId -> connectorId.getCatalogName().equals("jmx"))
//                        .map(Object::toString)
//                        .forEach(connectorIds::add);
//            }
//            else {
//                catalogs.stream()
//                        .map(Catalog::getConnectorId)
//                        .map(Object::toString)
//                        .forEach(connectorIds::add);
//            }
//        }
//
//        // build announcement with updated sources
//        ServiceAnnouncement.ServiceAnnouncementBuilder builder = serviceAnnouncement(announcement.getType());
//        for (Map.Entry<String, String> entry : announcement.getProperties().entrySet()) {
//            if (!entry.getKey().equals("connectorIds")) {
//                builder.addProperty(entry.getKey(), entry.getValue());
//            }
//        }
//        builder.addProperty("connectorIds", Joiner.on(',').join(connectorIds));
//
//        // update announcement
//        announcer.removeServiceAnnouncement(announcement.getId());
//        announcer.addServiceAnnouncement(builder.build());
//    }
//
//    private static ServiceAnnouncement getPrestoAnnouncement(Set<ServiceAnnouncement> announcements)
//    {
//        for (ServiceAnnouncement announcement : announcements) {
//            if (announcement.getType().equals("presto")) {
//                return announcement;
//            }
//        }
//        throw new IllegalArgumentException("Presto announcement not found: " + announcements);
//    }
}
