package com.codicesoftware.plugins.hudson.model;

import hudson.model.Action;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

import static java.lang.String.format;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable, Cloneable {

    private static final long serialVersionUID = -3335648855305648577L;

    public Workspace workspace;
    public ChangeSet builtCset;

    @SuppressWarnings("unused")
    public BuildData() {
    }

    public BuildData(Workspace workspace, ChangeSet builtCset) {
        this.workspace = workspace;
        this.builtCset = builtCset;
    }

    @Exported
    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Exported
    public ChangeSet getBuiltCset() {
        return builtCset;
    }

    public void setBuiltCset(ChangeSet builtCset) {
        this.builtCset = builtCset;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return format("Workspace '%s', built cset: %s", workspace.getName(), getCsetSpec());
    }

    @Override
    public String getUrlName() {
        return "platicscm";
    }

    @Override
    public Object clone() {
        BuildData result;
        try {
            result = (BuildData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning BuildData", e);
        }

        result.setWorkspace(new Workspace(workspace));
        result.setBuiltCset(new ChangeSet(builtCset));
        return result;
    }

    private String getCsetSpec() {
        if (builtCset == null)
            return "NONE";
        return format("cs:%s@%s", builtCset.getCommitId(), builtCset.getRepository());
    }
}
