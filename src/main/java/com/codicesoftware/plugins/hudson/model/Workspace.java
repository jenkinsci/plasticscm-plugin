package com.codicesoftware.plugins.hudson.model;

import hudson.FilePath;

import java.io.File;
import java.io.Serializable;

public class Workspace implements Serializable {

    private final String name;
    private transient final FilePath path;
    private final String guid;

    public Workspace(String name, String path, String guid) {
        this.name = name;
        this.path = new FilePath(new File(path));
        this.guid = guid;
    }

    /**
     * Copy constructor.
     */
    public Workspace(Workspace o) {
        this.name = o.name;
        this.path = o.path;
        this.guid = o.guid;
    }

    public String getName() {
        return name;
    }

    public FilePath getPath() {
        return path;
    }

    public String getGuid() {
        return guid;
    }
}
