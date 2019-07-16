package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class UndoCheckoutCommand implements Command {

    private final String workspacePath;

    public UndoCheckoutCommand(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    @Override
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("unco");
        arguments.add("--all");
        arguments.add(workspacePath);

        return arguments;
    }
}
