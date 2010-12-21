package com.codicesoftware.plugins.hudson.model;

public class Branch {
    private final String name;

    public Branch(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}