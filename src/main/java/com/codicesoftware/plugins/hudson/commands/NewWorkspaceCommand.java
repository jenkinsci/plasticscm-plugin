package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

import javax.annotation.Nonnull;

public class NewWorkspaceCommand implements Command {
    @Nonnull
    private final String workspaceName;
    @Nonnull
    private final FilePath workspacePath;

    public NewWorkspaceCommand(@Nonnull final String workspaceName, @Nonnull final FilePath workspacePath) {
        this.workspaceName = workspaceName;
        this.workspacePath = workspacePath;
    }

    @Nonnull
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("workspace");
        arguments.add("create");
        arguments.add(workspaceName);
        arguments.add(workspacePath.getRemote());

        return arguments;
    }
}
