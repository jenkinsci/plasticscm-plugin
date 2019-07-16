package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListWorkspacesCommand implements ParseableCommand<List<Workspace>>, Command {

    private static final Pattern parserPattern = Pattern.compile("^(.*)#(.*)#(.*)$");

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("lwk");
        arguments.add("--format={wkname}#{path}#{wkid}");

        return arguments;
    }

    public List<Workspace> parse(Reader consoleReader) throws IOException {
        List<Workspace> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(consoleReader);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = parserPattern.matcher(line);
            if (matcher.find()) {
                Workspace workspace = new Workspace(matcher.group(1), matcher.group(2), matcher.group(3));
                list.add(workspace);
            }
            line = reader.readLine();
        }
        return list;
    }
}
