package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.commands.parsers.FindOutputParser;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.ObjectSpec;
import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class FindChangesetCommand implements ParseableCommand<ChangeSet>, Command {

    private final ObjectSpec objectSpec;
    private final FilePath xmlOutputPath;

    public FindChangesetCommand(
        ObjectSpec objectSpec, FilePath xmlOutputPath) {
        this.objectSpec = objectSpec;
        this.xmlOutputPath = xmlOutputPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        String object = objectSpec.getType().toFindObject();
        arguments.add("find");
        arguments.add(object);
        arguments.add("where");
        arguments.add(String.format("%sid=%d", object, objectSpec.getId()));
        arguments.add("on");
        arguments.add("repository");
        arguments.add(String.format("'%s@%s'", objectSpec.getRepository(), objectSpec.getServer()));

        arguments.add("--xml");
        arguments.add("--file=" + xmlOutputPath.getRemote());
        arguments.add("--dateformat=" + DateUtil.ISO_DATE_TIME_OFFSET_CSHARP_FORMAT);

        return arguments;
    }

    public ChangeSet parse(Reader reader) throws IOException, ParseException {
        List<ChangeSet> csetList = FindOutputParser.parseReader(objectSpec.getType(), xmlOutputPath);

        if (!csetList.isEmpty()) {
            return csetList.get(0);
        }
        return null;
    }
}
