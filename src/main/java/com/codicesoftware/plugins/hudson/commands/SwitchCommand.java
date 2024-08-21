package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class SwitchCommand implements Command {

    private final String workspacePath;
    private final String targetSpec;

    public SwitchCommand(String workspacePath, String targetSpec) {
        this.workspacePath = workspacePath;
        this.targetSpec = targetSpec;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("switch");
        arguments.add(targetSpec);
        arguments.add("--workspace=" + workspacePath);
        arguments.add("--noinput");

        return arguments;
    }
}
