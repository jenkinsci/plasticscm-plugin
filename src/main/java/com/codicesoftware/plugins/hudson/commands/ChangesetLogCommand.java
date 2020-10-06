package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.LogOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class ChangesetLogCommand implements ParseableCommand<ChangeSet>, Command {
    private final String csetSpec;
    private final FilePath xmlOutputPath;

    public ChangesetLogCommand(String csetSpec, FilePath xmlOutputPath) {
        this.csetSpec = csetSpec;
        this.xmlOutputPath = xmlOutputPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("log");
        arguments.add(csetSpec);
        arguments.add("--xml=" + xmlOutputPath.getRemote());
        arguments.add("--encoding=utf-8");

        return arguments;
    }

    public ChangeSet parse(Reader reader) throws IOException, ParseException {
        List<ChangeSet> csetList = LogOutputParser.parseFile(xmlOutputPath);

        if (!csetList.isEmpty()) {
            return csetList.get(0);
        }
        return null;
    }
}
