package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetWorkspaceFromPathCommand implements ParseableCommand<Workspace>, Command {

    private static final Pattern parserPattern = Pattern.compile("^(.*)#(.*)#(.*)$");

    private final String workspacePath;

    public GetWorkspaceFromPathCommand(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("gwp");
        arguments.add(workspacePath);
        arguments.add("--format={0}#{1}#{4}");

        return arguments;
    }

    public Workspace parse(Reader r) throws IOException {
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        Matcher matcher = parserPattern.matcher(line);
        if (matcher.find()) {
            return new Workspace(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return null;
    }
}
