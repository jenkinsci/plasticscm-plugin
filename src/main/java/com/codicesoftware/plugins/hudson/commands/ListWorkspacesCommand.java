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

public class ListWorkspacesCommand extends AbstractCommand implements ParseableCommand<List<Workspace>> {
    private final WorkspaceFactory factory;

    private static final Pattern parserPattern = Pattern.compile("^(.*)#(.*)#(.*)$");

    public interface WorkspaceFactory {
        Workspace createWorkspace(String name, String path, String selector);
    }

    public ListWorkspacesCommand(WorkspaceFactory factory, ServerConfigurationProvider provider) {
        super(provider);
        this.factory = factory;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("lwk");
        arguments.add("--format={0}#{1}#{2}");

        return arguments;
    }

    public List<Workspace> parse(Reader consoleReader) throws IOException {
        List<Workspace> list = new ArrayList<Workspace>();
        BufferedReader reader = new BufferedReader(consoleReader);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = parserPattern.matcher(line);
            if (matcher.find()) {
                Workspace workspace = factory.createWorkspace(matcher.group(1), matcher.group(3), null);
                list.add(workspace);
            }
            line = reader.readLine();
        }

        return list;
    }
}