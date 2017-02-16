package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.model.ChangeLogSet;
import com.codicesoftware.plugins.hudson.model.ChangeSet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import hudson.model.Run;
import hudson.scm.RepositoryBrowser;
import org.apache.commons.digester.Digester;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import hudson.scm.ChangeLogParser;
import hudson.util.Digester2;

/**
 * Plastic SCM change log reader, based on tfs version.
 * 
 * @author Erik Ramfelt
 * @author Dick Porter
 */ 
public class ChangeSetReader extends ChangeLogParser {

    @Override
    public ChangeLogSet parse(
            Run run, RepositoryBrowser<?> browser, File changelogFile)
            throws IOException, SAXException {
        FileReader reader = new FileReader(changelogFile);
        try {
            return parse(run, browser, reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public ChangeLogSet parse(
            Run<?,?> run, RepositoryBrowser<?> browser, Reader reader)
            throws IOException, SAXException {
        List<ChangeSet> changesetList = new ArrayList<ChangeSet>();
        Digester digester = new Digester2();
        digester.push(changesetList);

        digester.addObjectCreate("*/changeset", ChangeSet.class);
        digester.addSetProperties("*/changeset");
        digester.addBeanPropertySetter("*/changeset/date", "dateStr");
        digester.addBeanPropertySetter("*/changeset/user");
        digester.addBeanPropertySetter("*/changeset/comment");
        digester.addBeanPropertySetter("*/changeset/repname", "repoName");
        digester.addBeanPropertySetter("*/changeset/repserver", "repoServer");
        digester.addSetNext("*/changeset", "add");

        digester.addObjectCreate("*/changeset/items/item", ChangeSet.Item.class);
        digester.addSetProperties("*/changeset/items/item");
        digester.addBeanPropertySetter("*/changeset/items/item", "path");
        digester.addSetNext("*/changeset/items/item", "add");

        digester.parse(reader);

        return new ChangeLogSet(run, browser, changesetList);
    }
}
