package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ObjectSpec;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import com.codicesoftware.plugins.jenkins.mergebot.ObjectSpecType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetWorkspaceStatusCommand implements ParseableCommand<ObjectSpec>, Command {

    private static final Pattern statusRegex = Pattern.compile("^(cs|sh):(\\d+)@rep:(.+)@repserver:(.+)$");

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

    public ObjectSpec parse(Reader r) throws IOException {
        ObjectSpec result = null;
        BufferedReader reader = new BufferedReader(r);

        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = statusRegex.matcher(line);
            line = reader.readLine();

            if (!matcher.find()) {
                continue;
            }

            ObjectSpecType type = ObjectSpecType.from(matcher.group(1));
            if (type == null) {
                continue;
            }

            result = new ObjectSpec(type, Integer.parseInt(matcher.group(2)), matcher.group(3), matcher.group(4));
        }

        return result;
    }
}
