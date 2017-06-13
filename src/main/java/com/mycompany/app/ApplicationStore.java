package com.mycompany.app;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ApplicationStore
{
    private final Map<String, Application> apps;

    public ApplicationStore()
    {
        apps = new HashMap<>();
    }

    public boolean put(String id, Application app){
        boolean added = apps.put(id, app) == null;

        return added;
    }

    public boolean delete(String id)
    {
        boolean deleted = apps.remove(id) == null;

        return deleted;
    }

    public Collection<Application> getAll()
    {
        return ImmutableList.copyOf(apps.values());
    }
}
