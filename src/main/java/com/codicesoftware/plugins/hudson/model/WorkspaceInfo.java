package com.codicesoftware.plugins.hudson.model;

public class WorkspaceInfo {
    private final String repoName;
    private final String branch;
    private final String label;
    private final String changeset;

    public WorkspaceInfo(String repoName, String branch, String label, String changeset) {
        this.repoName = repoName;
        this.branch = branch;
        this.label = label;
        this.changeset = changeset;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getBranch() {
        return branch;
    }

    public String getLabel() {
        return label;
    }

    public String getChangeset() { return changeset; }
}