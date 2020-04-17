package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class SetSelectorCommand implements Command {

    private final String workspacePath;
    private final String selectorFile;

    public SetSelectorCommand(String workspacePath, String selectorFile) {
        this.workspacePath = workspacePath;
        this.selectorFile = selectorFile;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("setselector");
        arguments.add("--file=" + selectorFile);
        arguments.add(workspacePath);

        return arguments;
    }
}
