package com.mycompany.app;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

import static io.airlift.jaxrs.JaxrsBinder.jaxrsBinder;
import static org.weakref.jmx.guice.ExportBinder.newExporter;
import static io.airlift.discovery.client.DiscoveryBinder.discoveryBinder;

public class MainModule
        implements Module
{
    public void configure(Binder binder)
    {
        //binder.bind(CurrentTime.class).in(Scopes.SINGLETON);
        //jaxrsBinder(binder).bind(TimeResource.class);

        binder.bind(ApplicationStore.class).in(Scopes.SINGLETON);
        newExporter(binder).export(ApplicationStore.class).withGeneratedName();

        jaxrsBinder(binder).bind(ApplicationResource.class);

        discoveryBinder(binder).bindHttpAnnouncement("application");

    }
}
