package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class UpdateWorkspaceCommand implements Command {

    private final String workspacePath;

    public UpdateWorkspaceCommand(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("update");
        arguments.add(workspacePath);

        return arguments;
    }
}
