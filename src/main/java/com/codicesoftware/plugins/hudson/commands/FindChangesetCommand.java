package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.FindOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class FindChangesetCommand implements ParseableCommand<ChangeSet>, Command {

    private final int csetId;
    private final String branch;
    private final String repository;
    private final String xmlOutputPath;

    public FindChangesetCommand(
        int csetId, String branch, String repository, String xmlOutputPath) {
        this.csetId = csetId;
        this.branch = branch;
        this.repository = repository;
        this.xmlOutputPath = xmlOutputPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("find");
        arguments.add("changeset");
        arguments.add("where");
        arguments.add("branch='" + branch + "'");
        arguments.add("and");
        arguments.add("changesetid=" + csetId);
        arguments.add("on");
        arguments.add("repository");
        arguments.add("'" + repository + "'");

        arguments.add("--xml");
        arguments.add("--file=" + xmlOutputPath);
        arguments.add("--dateformat=" + DateUtil.ISO_DATE_TIME_OFFSET_CSHARP_FORMAT);

        return arguments;
    }

    public ChangeSet parse(Reader reader) throws IOException, ParseException {
        List<ChangeSet> csetList = FindOutputParser.parseReader(xmlOutputPath);

        if (!csetList.isEmpty()) {
            return csetList.get(0);
        }
        return null;
    }
}
