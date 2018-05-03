package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class DeleteWorkspaceCommand implements Command {
    private final String workspaceName;

    public DeleteWorkspaceCommand(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("rmwk");
        arguments.add("wk:" + workspaceName);

        return arguments;
    }
}