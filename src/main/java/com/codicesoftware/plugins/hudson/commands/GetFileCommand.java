package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class GetFileCommand implements Command {

    private final String revSpec;
    private final String filePath;

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
}
