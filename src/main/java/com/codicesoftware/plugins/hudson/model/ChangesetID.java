package com.codicesoftware.plugins.hudson.model;

public class ChangesetID {
    private final String id;
    private final String repoName;
    private final String host;
    private final String port;

    public ChangesetID(String id, String repoName, String host, String port) {
        this.id = id;
        this.repoName = repoName;
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getRepository() {
        return "rep:" + repoName + "@repserver:" + host + ":" + port;
    }
}