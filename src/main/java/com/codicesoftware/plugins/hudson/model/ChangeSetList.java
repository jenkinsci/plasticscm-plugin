package com.codicesoftware.plugins.hudson.model;

import hudson.model.Run;
import hudson.scm.RepositoryBrowser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ChangeSetList for the Plastic SCM
 * The log set will set the parent of the log entries in the constructor.
 *
 * @author Erik Ramfelt
 * @author Dick Porter
 */
public class ChangeSetList extends hudson.scm.ChangeLogSet<ChangeSet> {
    private final List<ChangeSet> changesets;

    public ChangeSetList(
            Run<?, ?> run, RepositoryBrowser<?> browser, List<ChangeSet> changesets) {
        super(run, browser);
        this.changesets = changesets;
        for (ChangeSet changeset : changesets) {
            changeset.setParent(this);
        }
    }

    public ChangeSetList(
            Run<?, ?> run, RepositoryBrowser<?> browser, ChangeSet[] changesetArray) {
        super(run, browser);
        changesets = new ArrayList<ChangeSet>();
        for (ChangeSet changeset : changesetArray) {
            changeset.setParent(this);
            changesets.add(changeset);
        }
    }

    @Override
    public boolean isEmptySet() {
        return changesets.isEmpty();
    }

    public Iterator<ChangeSet> iterator() {
        return changesets.iterator();
    }
}