package com.mycompany.app;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

public class ApplicationStore
{
    private final ConcurrentMap<String, Application> apps;

    public ApplicationStore()
    {
        Cache<String, Application> appCache = CacheBuilder.newBuilder().build();
        apps = appCache.asMap();
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
