package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class DeleteWorkspaceCommand implements Command {
    private final String workspacePath;

    public DeleteWorkspaceCommand(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("workspace");
        arguments.add("delete");
        arguments.add(workspacePath);

        return arguments;
    }
}
