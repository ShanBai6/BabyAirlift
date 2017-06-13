package com.mycompany.app;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;

@Path("/v1/app")
public class ApplicationResource
{
    private final ApplicationStore store;

    @Inject
    public ApplicationResource(ApplicationStore store)
    {
        this.store = store;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listofApps()
    {
        return Response.ok(store.getAll()).build();
    }

    @PUT
    @Path("/{id: \\w+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("id") String id, Application app)
            throws URISyntaxException
    {
        boolean added = store.put(id, app);
        if (added) {
            return Response.created(new URI("/v1/app/" + id)).build();
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id: \\w+}")
    public Response delete(@PathParam("id") String id)
    {
        if (!store.delete(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
