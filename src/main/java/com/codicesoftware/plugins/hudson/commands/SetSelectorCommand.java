package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

public class SetSelectorCommand extends AbstractCommand {
    private final String workspaceName;
    private final FilePath selectorFile;

    public SetSelectorCommand(ServerConfigurationProvider provider, String workspaceName,
            FilePath selectorFile) {
        super(provider);
        this.workspaceName = workspaceName;
        this.selectorFile = selectorFile;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("sts");
        arguments.add("--file=" + selectorFile.getName());
        arguments.add("wk:" + workspaceName);

        return arguments;
    }
}