package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class GetFilesToWorkFolderCommand extends AbstractCommand {
    private final String workFolder;

    public GetFilesToWorkFolderCommand(ServerConfigurationProvider configurationProvider,
            String workFolder) {
        super(configurationProvider);
        this.workFolder = workFolder;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("update");
        arguments.add(workFolder);
        
        return arguments;
    }
}