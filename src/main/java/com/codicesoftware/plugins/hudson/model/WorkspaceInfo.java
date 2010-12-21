package com.codicesoftware.plugins.hudson.model;

public class WorkspaceInfo {
    private final String repoName;
    private final String branch;
    private final String label;

    public WorkspaceInfo(String repoName, String branch, String label) {
        this.repoName = repoName;
        this.branch = branch;
        this.label = label;
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
}