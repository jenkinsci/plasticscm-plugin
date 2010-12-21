package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangesetID;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetWorkspaceStatusCommand extends AbstractCommand implements ParseableCommand<List<ChangesetID>> {
    private static final Pattern statusRegex = Pattern.compile("^cs:(\\d+)@rep:(.+)@repserver:(.+):(\\d+)$");

    public GetWorkspaceStatusCommand(ServerConfigurationProvider provider) {
        super(provider);
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("status");

        return arguments;
    }

    public List<ChangesetID> parse(Reader r) throws IOException, ParseException {
        List<ChangesetID> list = new ArrayList<ChangesetID>();
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = statusRegex.matcher(line);
            if (matcher.find()) {
                ChangesetID cs = new ChangesetID(matcher.group(1), matcher.group(2),
                        matcher.group(3), matcher.group(4));
                list.add(cs);
            }
            line = reader.readLine();
        }

        return list;
    }
}