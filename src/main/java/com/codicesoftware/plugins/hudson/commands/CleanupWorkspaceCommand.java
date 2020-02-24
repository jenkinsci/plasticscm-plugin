package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class CleanupWorkspaceCommand implements Command {

    private final String workspacePath;
    private final boolean removeIgnored;

    public CleanupWorkspaceCommand(String workspacePath, boolean removeIgnored) {
        this.workspacePath = workspacePath;
        this.removeIgnored = removeIgnored;
    }

    @Override
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("remove");
        arguments.add("private");
        if (removeIgnored) {
            arguments.add("--ignored");
        }
        arguments.add("-r");
        arguments.add(workspacePath);

        return arguments;
    }
}
