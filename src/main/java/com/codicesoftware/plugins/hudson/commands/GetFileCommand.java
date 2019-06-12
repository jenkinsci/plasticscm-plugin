package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class GetFileCommand implements Command {
    public GetFileCommand(String revSpec, String filePath) {
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
