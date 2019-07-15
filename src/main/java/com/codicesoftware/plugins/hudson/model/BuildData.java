package com.codicesoftware.plugins.hudson.model;

import hudson.model.Action;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

import static java.lang.String.format;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable, Cloneable {
    private static final long serialVersionUID = -3335648855305648577L;

    private static final String NONE = "NONE";

    public String wkName;
    public ChangeSet builtCset;

    public BuildData() {
    }

    public BuildData(String wkName, ChangeSet builtCset) {
        this.wkName = wkName;
        this.builtCset = builtCset;
    }

    @Exported
    public ChangeSet getBuiltCset() {
        return builtCset;
    }

    public void setBuiltCset(ChangeSet builtCset) {
        this.builtCset = builtCset;
    }

    @Exported
    public String getWkName() {
        return wkName;
    }

    public void setWkName(String wkName) {
        this.wkName = wkName;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return format("Workspace '%s', built cset: %s", wkName, getCsetSpec());
    }

    @Override
    public String getUrlName() {
        return "platicscm";
    }

    @Override
    public Object clone() {
        BuildData result;
        try {
            result = (BuildData)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning BuildData", e);
        }

        result.setWkName(wkName);
        result.setBuiltCset(builtCset);
        return result;
    }

    private String getCsetSpec() {
        if (builtCset == null)
            return NONE;
        return format("cs:%s@%s", builtCset.getCommitId(), builtCset.getRepository());
    }
}
