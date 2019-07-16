package com.codicesoftware.plugins.hudson.model;

public class ChangeSetID {
    private final int id;
    private final String repository;
    private final String host;
    private final int port;
    private final String protocol;

    public ChangeSetID(int id, String repository, String host, int port) {
        this.id = id;
        this.repository = repository;
        int index = host.indexOf("://");
        if (index < 0) {
            this.host = host;
            this.protocol = "plastic";
        } else {
            this.host = host.substring(index + 3);
            this.protocol = host.substring(0, index);
        }
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public String getRepository() {
        return repository;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isSslProtocol() {
        return "ssl".equals(protocol);
    }
}
