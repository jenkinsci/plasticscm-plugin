package com.codicesoftware.plugins.hudson.util;

import hudson.Util;
import hudson.util.FormValidation;

import java.util.regex.Pattern;

public class FormChecker {
    private static final Pattern WORKSPACE_REGEX = Pattern.compile("^[^@#/:]+$");
    private static final Pattern SELECTOR_REGEX = Pattern.compile(
        "^(\\s*(rep|repository)\\s+\"(.*)\"(\\s+mount\\s+\"(.*)\")?(\\s+path\\s+"
        + "\"(.*)\"(\\s+norecursive)?(\\s+((((((branch|br)\\s+\"(.*)\")(\\s+(revno\\s+"
        + "(\"\\d+\"|LAST|FIRST)|changeset\\s+\"\\S+\"))?(\\s+(label|lb)\\s+\"(.*)\")?)|"
        + "(label|lb)\\s+\"(.*)\")(\\s+(checkout|co)\\s+\"(.*\"))?)|(branchpertask\\s+"
        + "\"(.*)\"(\\s+baseline\\s+\"(.*)\")?)|(smartbranch\\s+\"(.*)\"))))+\\s*)+$",
        Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
    private static final String DEFAULT_SELECTOR =
        "repository \"default\"\n  path \"/\"\n    smartbranch \"/main\"";

    private static FormValidation doRegexCheck(final Pattern regex, final String noMatchText,
            final String nullText, String value) {
        value = Util.fixEmpty(value);
        if (value == null)
            return FormValidation.error(nullText);

        if (regex.matcher(value).matches())
            return FormValidation.ok();

        return FormValidation.error(noMatchText);
    }

    public static FormValidation doCheckWorkspaceName(String value) {
        return doRegexCheck(WORKSPACE_REGEX, "Workspace name should not include @, #, / or :",
            "Workspace name is mandatory", value);
    }

    public static FormValidation doCheckSelector(String value) {
        return doRegexCheck(SELECTOR_REGEX, "Selector is not in valid format",
            "Selector is mandatory", value);
    }

    public static String getDefaultSelector() {
        return DEFAULT_SELECTOR;
    }

}
