package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.FindOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class FindChangesetRangeCommand implements ParseableCommand<List<ChangeSet>>, Command {

    private final int csetIdFrom;
    private final int csetIdTo;
    private final String branch;
    private final String repository;

    public FindChangesetRangeCommand(int csetIdFrom, int csetIdTo, String branch, String repository) {
        this.csetIdFrom = csetIdFrom;
        this.csetIdTo = csetIdTo;
        this.branch = branch;
        this.repository = repository;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("find");
        arguments.add("changeset");
        arguments.add("where");
        arguments.add("branch='" + branch + "'");
        arguments.add("and");
        arguments.add("changesetid");
        arguments.add("between");
        arguments.add(csetIdFrom);
        arguments.add("and");
        arguments.add(csetIdTo);
        arguments.add("on");
        arguments.add("repository");
        arguments.add("'" + repository + "'");

        arguments.add("--xml");
        arguments.add("--dateformat=" + DateUtil.ISO_DATE_TIME_OFFSET_CSHARP_FORMAT);

        return arguments;
    }

    public List<ChangeSet> parse(Reader reader) throws IOException, ParseException {
        return FindOutputParser.parseReader(reader);
    }
}
