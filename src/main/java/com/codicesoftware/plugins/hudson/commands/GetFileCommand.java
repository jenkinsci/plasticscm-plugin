package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class GetFileCommand extends AbstractCommand {
    public GetFileCommand(
        ServerConfigurationProvider provider,
        String revSpec, String filePath) {
        super(provider);
        this.revSpec = revSpec;
        this.filePath = filePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();
        arguments.add("cat");
        arguments.add(revSpec);
        arguments.add("--file=" + filePath);
        return arguments;
    }

    private final String filePath;
    private final String revSpec;
}
