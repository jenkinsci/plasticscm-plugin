package com.codicesoftware.plugins.hudson.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;

import java.io.File;
import java.io.Serializable;

public class Workspace implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient final FilePath path;
    private final String pathStr;
    private final String guid;

    public Workspace(String name, String path, String guid) {
        this.name = name;
        this.path = new FilePath(new File(path));
        this.pathStr = path;
        this.guid = guid;
    }

    /**
     * Copy constructor.
     */
    public Workspace(Workspace o) {
        this.name = o.name;
        this.path = new FilePath(new File(o.pathStr));
        this.pathStr = o.pathStr;
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
