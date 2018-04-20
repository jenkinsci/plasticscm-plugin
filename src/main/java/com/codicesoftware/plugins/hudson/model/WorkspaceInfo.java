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

    public String getRepObjectSpec(){
        return String.format("%s@%s", getObjectSpec(), repoName);
    }

    private String getObjectSpec(){
        if (!isNullOrEmpty(branch))
            return "br:" + branch;

        if (!isNullOrEmpty(changeset))
            return "cs:" + changeset;

        if (!isNullOrEmpty(label))
            return "lb:" + label;

        return null;
    }

    private static boolean isNullOrEmpty(String value) {
        if (value == null)
            return true;

        return value.isEmpty();
    }
}