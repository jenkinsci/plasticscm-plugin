package com.codicesoftware.plugins.jenkins;

public class SelectorTemplates {

    private SelectorTemplates() {
    }

    public static final String DEFAULT = "repository \"default\"%n  path \"/\"%n    smartbranch \"main\"";
    public static final String BRANCH = "repository \"%s@%s\"%n  path \"/\"%n    smartbranch \"%s\"";
    public static final String CHANGESET_WITH_BRANCH =
            "repository \"%s@%s\"%n  path \"/\"%n    smartbranch \"%s\" changeset \"%s\"";
    public static final String CHANGESET = "";
    public static final String LABEL = "";
    public static final String SHELVE = "";
}
