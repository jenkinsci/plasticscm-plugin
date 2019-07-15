package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

public class UndoCheckoutCommand implements Command {

    private final FilePath workspacePath;

    public UndoCheckoutCommand(FilePath workspacePath) {
        this.workspacePath = workspacePath;
    }

    @Override
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("unco");
        arguments.add("--all");
        arguments.add(workspacePath.getRemote());

        return arguments;
    }
}
