package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.FindOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import com.codicesoftware.plugins.jenkins.ObjectSpecType;
import hudson.FilePath;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class DetailedHistoryCommand implements ParseableCommand<List<ChangeSet>>, Command {
    private final Calendar fromTimestamp;
    private final Calendar toTimestamp;
    private final String branch;
    private final String repository;
    private final FilePath xmlOutputPath;

    private final SimpleDateFormat dateFormatter =
            new SimpleDateFormat(DateUtil.DEFAULT_SORTABLE_FORMAT);

    public DetailedHistoryCommand(
            Calendar fromTimestamp,
            Calendar toTimestamp,
            String branch,
            String repository,
            FilePath xmlOutputPath) {
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
        this.branch = branch;
        this.repository = repository;
        this.xmlOutputPath = xmlOutputPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("find");
        arguments.add("changeset");
        arguments.add("where");
        arguments.add("date");
        arguments.add("between");
        arguments.add("'" + dateFormatter.format(fromTimestamp.getTime()) + "'");
        arguments.add("and");
        arguments.add("'" + dateFormatter.format(toTimestamp.getTime()) + "'");
        arguments.add("and");
        arguments.add("branch='" + branch + "'");
        arguments.add("on");
        arguments.add("repositories");
        arguments.add("'" + repository + "'");

        arguments.add("--xml");
        arguments.add("--file=" + xmlOutputPath.getRemote());
        arguments.add("--dateformat=" + DateUtil.DEFAULT_SORTABLE_FORMAT);


        return arguments;
    }

    public List<ChangeSet> parse(Reader reader) throws IOException, ParseException {
        return FindOutputParser.parseReader(ObjectSpecType.Changeset, xmlOutputPath);
    }
}
