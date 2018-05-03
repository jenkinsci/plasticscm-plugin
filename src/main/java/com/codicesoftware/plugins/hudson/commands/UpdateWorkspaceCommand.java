package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class UpdateWorkspaceCommand implements Command {
    private final String workFolder;

    public UpdateWorkspaceCommand(String workFolder) {
        this.workFolder = workFolder;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("update");
        arguments.add(workFolder);
        
        return arguments;
    }
}