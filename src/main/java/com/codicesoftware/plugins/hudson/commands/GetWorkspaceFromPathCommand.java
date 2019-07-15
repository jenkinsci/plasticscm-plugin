package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class GetWorkspaceFromPathCommand implements ParseableCommand<String>, Command {

    private String workspacePath;

    public GetWorkspaceFromPathCommand(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("gwp");
        arguments.add(workspacePath);
        arguments.add("--format={1}");

        return arguments;
    }

    public String parse(Reader r) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(r);
        return reader.readLine();
    }
}
