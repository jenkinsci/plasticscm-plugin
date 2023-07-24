package com.codicesoftware.plugins.hudson.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WorkspaceInfo {

    @Nonnull
    private final String repoName;
    @CheckForNull
    private final String branch;
    @CheckForNull
    private final String label;
    @CheckForNull
    private final String changeset;

    public WorkspaceInfo(
            @Nonnull String repoName,
            @Nullable String branch,
            @Nullable String label,
            @Nullable String changeset) {
        this.repoName = repoName;
        this.branch = branch;
        this.label = label;
        this.changeset = changeset;
    }

    @Nonnull
    public String getRepoName() {
        return repoName;
    }

    @CheckForNull
    public String getBranch() {
        return branch;
    }

    @CheckForNull
    public String getLabel() {
        return label;
    }

    @CheckForNull
    public String getChangeset() {
        return changeset;
    }

    @Nonnull
    public String getRepObjectSpec() {
        return String.format("%s@%s", getObjectSpec(), repoName);
    }

    @CheckForNull
    private String getObjectSpec() {
        if (containsValue(branch)) {
            return "br:" + branch;
        }

        if (containsValue(changeset)) {
            return "cs:" + changeset;
        }

        if (containsValue(label)) {
            return "lb:" + label;
        }

        return null;
    }

    private static boolean containsValue(@CheckForNull final String value) {
        return value != null && !value.isEmpty();
    }
}
