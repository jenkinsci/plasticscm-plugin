package com.codicesoftware.plugins.hudson.model;

public class ChangeSetID {
    private final int id;
    private final String repository;
    private final String server;

    public ChangeSetID(int id, String repository, String server) {
        this.id = id;
        this.repository = repository;
        this.server = server;
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
}
