package com.mycompany.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class Application
{
    private final int id;
    private final String name;

    @JsonCreator
    public Application(@JsonProperty("id") int id, @JsonProperty("name") String name)
    {
        this.id = id;
        this.name = requireNonNull(name, "name is null");
    }

    @JsonProperty("id")
    public int getId()
    {
        return id;
    }

    @JsonProperty("name")
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .toString();
    }
}
