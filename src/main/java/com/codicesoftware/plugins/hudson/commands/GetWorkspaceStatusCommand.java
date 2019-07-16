package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangeSetID;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetWorkspaceStatusCommand implements ParseableCommand<List<ChangeSetID>>, Command {

    private static final Pattern statusRegex = Pattern.compile("^cs:(\\d+)@rep:(.+)@repserver:(.+):(\\d+)$");

    private final String workspacePath;

    public GetWorkspaceStatusCommand(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("status");
        arguments.add("--cset");
        arguments.add(workspacePath);

        return arguments;
    }

    public List<ChangeSetID> parse(Reader r) throws IOException, ParseException {
        List<ChangeSetID> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = statusRegex.matcher(line);
            if (matcher.find()) {
                ChangeSetID cs = new ChangeSetID(Integer.parseInt(matcher.group(1)), matcher.group(2),
                        matcher.group(3), Integer.parseInt(matcher.group(4)));
                list.add(cs);
            }
            line = reader.readLine();
        }

        return list;
    }
}
