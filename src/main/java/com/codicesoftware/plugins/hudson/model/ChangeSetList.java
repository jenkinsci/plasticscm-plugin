package com.codicesoftware.plugins.hudson.model;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import org.kohsuke.stapler.export.Exported;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * ChangeSetList for the Plastic SCM
 * The log set will set the parent of the log entries in the constructor.
 */
public class ChangeSetList extends ChangeLogSet<ChangeSet> {
    private final List<ChangeSet> changesets;

    public ChangeSetList(
            Run<?, ?> run, RepositoryBrowser<?> browser, List<ChangeSet> changesetList) {
        super(run, browser);
        this.changesets = Collections.unmodifiableList(changesetList);
        for (ChangeSet changeset : changesets) {
            changeset.setParent(this);
        }
    }

    @Exported
    @Override
    public String getKind() {
        return "plasticscm";
    }

    @Override
    public boolean isEmptySet() {
        return changesets.isEmpty();
    }

    @Override
    public Iterator<ChangeSet> iterator() {
        return changesets.iterator();
    }

    @SuppressWarnings("unused")
    public List<ChangeSet> getLogs() {
        return changesets;
    }
}
