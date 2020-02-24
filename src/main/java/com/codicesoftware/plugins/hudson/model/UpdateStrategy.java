package com.codicesoftware.plugins.hudson.model;

public enum UpdateStrategy {
    BASIC("Minimal cleanup"),
    STANDARD("Standard cleanup"),
    FULL("Full cleanup"),
    DELETE("Delete workspace");

    private final String label;

    UpdateStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public boolean removesPrivate() {
        return (this == STANDARD) || (this == FULL);
    }

    public boolean removesIgnored() {
        return (this == FULL);
    }

}
