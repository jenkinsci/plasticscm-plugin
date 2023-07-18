package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.LogOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class ChangesetRangeLogCommand implements ParseableCommand<List<ChangeSet>>, Command {
    private final ChangeSet fromCset;
    private final ChangeSet toCset;
    private final FilePath xmlOutputPath;

    public ChangesetRangeLogCommand(ChangeSet fromCset, ChangeSet toCset, FilePath xmlOutputPath) {
        this.fromCset = fromCset;
        this.toCset = toCset;
        this.xmlOutputPath = xmlOutputPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("log");
        arguments.add("--from=" + fromCset.getCsetSpec());
        arguments.add(toCset.getCsetSpec());
        arguments.add("--xml=" + xmlOutputPath);
        arguments.add("--encoding=utf-8");

        return arguments;
    }

    public List<ChangeSet> parse(Reader reader) throws IOException, ParseException {
        return LogOutputParser.parseFile(xmlOutputPath, toCset.getRepoName(), toCset.getRepoServer());
    }
}
