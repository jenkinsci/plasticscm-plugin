package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class DeleteWorkspaceCommand extends AbstractCommand {
    private final String workspaceName;

    public DeleteWorkspaceCommand(ServerConfigurationProvider provider, String workspaceName) {
        super(provider);
        this.workspaceName = workspaceName;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("rmwk");
        arguments.add("wk:" + workspaceName);

        return arguments;
    }
}