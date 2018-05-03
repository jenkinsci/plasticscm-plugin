package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetBranchForChangesetCommand implements ParseableCommand<String>, Command {
    private final String cs;
    private final String repoName;

    private static final Pattern branchRegex = Pattern.compile("^\\s*<BRANCH>(.+)</BRANCH>$");

    public GetBranchForChangesetCommand(String cs, String repoName) {
        this.cs = cs;
        this.repoName = repoName;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("find");
        arguments.add("changeset");
        arguments.add("where");
        arguments.add("changesetid=" + cs);
        arguments.add("on");
        arguments.add("repositories");
        arguments.add("'" + repoName + "'");
        arguments.add("--xml");
        arguments.add("--dateformat=" + DateUtil.DEFAULT_SORTABLE_FORMAT);

        return arguments;
    }

    public String parse(Reader r) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = branchRegex.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
            line = reader.readLine();
        }

        throw new ParseException("Could not find branch in query results", 0);
    }
}