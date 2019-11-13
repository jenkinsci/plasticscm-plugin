package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.util.Digester2;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ChangesetLogCommand implements ParseableCommand<ChangeSet>, Command {
    private final String csetSpec;

    public ChangesetLogCommand(String csetSpec) {
        this.csetSpec = csetSpec;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("log");
        arguments.add(csetSpec);
        arguments.add("--xml");
        arguments.add("--encoding=utf-8");

        return arguments;
    }

    public ChangeSet parse(Reader reader) throws IOException, ParseException {
        List<ChangeSet> csetList = new ArrayList<>();

        Digester digester = new Digester2();
        digester.push(csetList);

        digester.addObjectCreate("LogList/Changeset", ChangeSet.class);
        digester.addBeanPropertySetter("LogList/Changeset/ChangesetId", "version");
        digester.addBeanPropertySetter("LogList/Changeset/Comment", "comment");
        digester.addBeanPropertySetter("LogList/Changeset/Date", "xmlDate");
        digester.addBeanPropertySetter("LogList/Changeset/Branch", "branch");
        digester.addBeanPropertySetter("LogList/Changeset/Owner", "user");
        // no "*/CHANGESET/REPNAME" tag
        // no "*/CHANGESET/REPSERVER" tag
        digester.addBeanPropertySetter("LogList/Changeset/GUID", "guid");
        digester.addSetNext("LogList/Changeset", "add");

        digester.addObjectCreate("LogList/Changeset/Changes/Item", ChangeSet.Item.class);
        digester.addBeanPropertySetter("LogList/Changeset/Changes/Item/RevId", "revId");
        digester.addBeanPropertySetter("LogList/Changeset/Changes/Item/ParentRevId", "parentRevId");
        digester.addBeanPropertySetter("LogList/Changeset/Changes/Item/DstCmPath", "path");
        digester.addBeanPropertySetter("LogList/Changeset/Changes/Item/Type", "status");
        digester.addSetNext("LogList/Changeset/Changes/Item", "addItem");

        try {
            digester.parse(reader);
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }

        if (!csetList.isEmpty()) {
            return csetList.get(0);
        }
        return null;
    }
}
