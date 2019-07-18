package com.codicesoftware.plugins.hudson.model;

import hudson.model.Action;
import hudson.model.Run;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable, Cloneable {

    private static final long serialVersionUID = -3335648855305648577L;

    private Workspace workspace;
    private ChangeSet changeset;

    /**
     * Avoids URL ambiguity when having multiple {@link BuildData} actions.
     */
    private int index;

    @SuppressWarnings("unused")
    public BuildData() {
    }

    public BuildData(Workspace workspace, ChangeSet changeset) {
        this.workspace = workspace;
        this.changeset = changeset;
    }

    @Exported
    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Exported
    public ChangeSet getChangeset() {
        return changeset;
    }

    public void setChangeset(ChangeSet changeset) {
        this.changeset = changeset;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/plasticscm-plugin/images/24x24/plasticscm.png";
    }

    @Override
    public String getDisplayName() {
        return (index == 0) ? "Plastic SCM" : "Plastic SCM #" + index;
    }

    @Override
    public String getUrlName() {
        return (index == 0) ? "plasticscm-plugin" : "plasticscm-plugin-" + index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Restricted(NoExternalUse.class)
    @SuppressWarnings("unused")
    public Run<?, ?> getOwningRun() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) {
            return null;
        }
        return req.findAncestorObject(Run.class);
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
        result.setChangeset(new ChangeSet(changeset));
        return result;
    }
}
