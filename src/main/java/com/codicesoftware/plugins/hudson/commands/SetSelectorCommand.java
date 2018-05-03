package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

public class SetSelectorCommand implements Command {
    private final String workspaceName;
    private final FilePath selectorFile;

    public SetSelectorCommand(String workspaceName,
            FilePath selectorFile) {
        this.workspaceName = workspaceName;
        this.selectorFile = selectorFile;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("sts");
        arguments.add("--file=" + selectorFile.getRemote());
        arguments.add("wk:" + workspaceName);

        return arguments;
    }
}