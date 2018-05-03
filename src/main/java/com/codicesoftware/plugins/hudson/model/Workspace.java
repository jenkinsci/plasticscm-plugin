package com.codicesoftware.plugins.hudson.model;

public class Workspace {
    private final String name;
    private final String path;

    public Workspace (String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}