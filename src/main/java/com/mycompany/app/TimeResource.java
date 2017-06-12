package com.mycompany.app;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/date")
public class TimeResource
{
    private final CurrentTime time;

    @Inject
    public TimeResource(CurrentTime time)
    {
        this.time = time;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String ShowTime()
    {
        return "The current time is: " + time.getTime();
    }
}
