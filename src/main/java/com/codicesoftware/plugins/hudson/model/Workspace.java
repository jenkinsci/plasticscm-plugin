package com.codicesoftware.plugins.hudson.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.Util;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.Serializable;

@ExportedBean(defaultVisibility = 999)
public class Workspace implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient FilePath path;
    private String pathStr;
    private String guid;

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

    @Exported
    public String getName() {
        return name;
    }

    public FilePath getPath() {
        if ((path == null) && (Util.fixEmpty(pathStr) != null)) {
            path = new FilePath(new File(pathStr));
        }
        return path;
    }

    @Exported(name = "path")
    public String getLocalPath() {
        return getPath().getRemote();
    }

    @Exported
    public String getGuid() {
        return guid;
    }

    @Override
    public String toString() {
        return "Workspace{name='" + name + "',path='" + path + "',pathStr='" + pathStr + "',guid='" + guid + "'}";
    }
}
