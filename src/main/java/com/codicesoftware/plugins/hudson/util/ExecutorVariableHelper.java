package com.codicesoftware.plugins.hudson.util;

import hudson.FilePath;
import hudson.slaves.WorkspaceList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutorVariableHelper {

    private static final String COMBINATOR = System.getProperty(WorkspaceList.class.getName(), "@");
    public static final String UNKNOWN_EXECUTOR = "1";

    private ExecutorVariableHelper() {
    }

    public static String getExecutorID(FilePath workspace) {
        String id = UNKNOWN_EXECUTOR;
        String name = workspace.getName();
        Pattern pattern = Pattern.compile(".*" + COMBINATOR + "([0-9]+)");
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            id = matcher.group(1);
        }
        return id;
    }

}
