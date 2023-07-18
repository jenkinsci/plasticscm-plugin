package com.codicesoftware.plugins.jenkins.mergebot;

public enum ObjectSpecType {
    Branch,
    Changeset,
    Label,
    Shelve;

    public static ObjectSpecType from(String type) {
        if ("cs".equals(type)) {
            return Changeset;
        }
        if ("br".equals(type)) {
            return Branch;
        }
        if ("lb".equals(type)) {
            return Label;
        }
        if ("sh".equals(type)) {
            return Shelve;
        }
        return null;
    }

    public String toSpecObject() {
        switch (this) {
            case Branch:
                return "br";
            case Changeset:
                return "cs";
            case Label:
                return "lb";
            case Shelve:
                return "sh";
            default:
                return "unknown";
        }
    }

    public String toFindObject() {
        switch (this) {
            case Branch:
                return "branch";
            case Changeset:
                return "changeset";
            case Label:
                return "label";
            case Shelve:
                return "shelve";
            default:
                return "unknown";
        }
    }
}
