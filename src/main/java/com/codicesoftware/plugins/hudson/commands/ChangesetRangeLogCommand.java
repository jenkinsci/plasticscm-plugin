package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.LogOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class ChangesetRangeLogCommand implements ParseableCommand<List<ChangeSet>>, Command {
    private final String csetSpecFrom;
    private final String csetSpecTo;
    private final String xmlOutputPath;

    public ChangesetRangeLogCommand(String csetSpecFrom, String csetSpecTo, String xmlOutputPath) {
        this.csetSpecFrom = csetSpecFrom;
        this.csetSpecTo = csetSpecTo;
        this.xmlOutputPath = xmlOutputPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("log");
        arguments.add("--from=" + csetSpecFrom);
        arguments.add("" + csetSpecTo);
        arguments.add("--xml=" + xmlOutputPath);
        arguments.add("--encoding=utf-8");

        return arguments;
    }

    public List<ChangeSet> parse(Reader reader) throws IOException, ParseException {
        return LogOutputParser.parseFile(xmlOutputPath);
    }
}
