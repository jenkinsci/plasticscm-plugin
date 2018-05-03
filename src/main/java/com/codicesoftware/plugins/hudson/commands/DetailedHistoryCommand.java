package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.util.Digester2;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class DetailedHistoryCommand implements ParseableCommand<List<ChangeSet>>, Command {
    private final Calendar fromTimestamp;
    private final Calendar toTimestamp;
    private final String branch;
    private final String repository;

    private final SimpleDateFormat dateFormatter =
    		new SimpleDateFormat(DateUtil.DEFAULT_SORTABLE_FORMAT);

    public DetailedHistoryCommand(
            Calendar fromTimestamp, Calendar toTimestamp, String branch, String repository) {
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
        this.branch = branch;
        this.repository = repository;
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
        arguments.add("--dateformat=" + DateUtil.DEFAULT_SORTABLE_FORMAT);
        

        return arguments;
    }

    public List<ChangeSet> parse(Reader reader) throws IOException, ParseException {
        ArrayList<ChangeSet> list = new ArrayList<ChangeSet>();

        Digester digester = new Digester2();
        digester.push(list);

        digester.addObjectCreate("*/CHANGESET", ChangeSet.class);
        digester.addBeanPropertySetter("*/CHANGESET/CHANGESETID", "version");
        digester.addBeanPropertySetter("*/CHANGESET/COMMENT", "comment");
        digester.addBeanPropertySetter("*/CHANGESET/DATE", "changesetDateStr");
        digester.addBeanPropertySetter("*/CHANGESET/BRANCH", "branch");
        digester.addBeanPropertySetter("*/CHANGESET/OWNER", "user");
        digester.addBeanPropertySetter("*/CHANGESET/REPNAME", "repoName");
        digester.addBeanPropertySetter("*/CHANGESET/REPSERVER", "repoServer");
        digester.addBeanPropertySetter("*/CHANGESET/GUID", "guid");
        digester.addSetNext("*/CHANGESET", "add");

        try {
            digester.parse(reader);
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }

        return list;
    }
}