package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.jenkins.ObjectSpecType;

public class ObjectSpec {
    private final ObjectSpecType specObjectType;
    private final int id;
    private final String repository;
    private final String server;

    public ObjectSpec(ObjectSpecType specObjectType, int id, String repository, String server) {
        this.specObjectType = specObjectType;
        this.id = id;
        this.repository = repository;
        this.server = server;
    }

    public ObjectSpecType getType() {
        return specObjectType;
    }

    public int getId() {
        return id;
    }

    public String getRepository() {
        return repository;
    }

    public String getServer() {
        return server;
    }

    public String getFullSpec() {
        return String.format("%s:%d@%s@%s", specObjectType.toSpecObject(), id, repository, server);
    }
}
