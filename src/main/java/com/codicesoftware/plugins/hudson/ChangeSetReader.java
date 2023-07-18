package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.DigesterUtils;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.ChangeSetList;
import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.RepositoryBrowser;
import org.apache.commons.digester3.Digester;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Plastic SCM change log reader, based on tfs version.
 */
public class ChangeSetReader extends ChangeLogParser {

    @Override
    public ChangeSetList parse(
            Run run, RepositoryBrowser<?> browser, File changelogFile)
            throws IOException, SAXException {
        BufferedReader reader = Files.newBufferedReader(changelogFile.toPath(), StandardCharsets.UTF_8);
        try {
            return parse(run, browser, reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public ChangeSetList parse(
            Run<?, ?> run, RepositoryBrowser<?> browser, Reader reader)
            throws IOException, SAXException {
        List<ChangeSet> changesetList = new ArrayList<>();
        Digester digester = DigesterUtils.createDigester(
            !Boolean.getBoolean(ChangeSetReader.class.getName() + ".UNSAFE"));
        digester.push(changesetList);

        digester.addObjectCreate("*/changeset", ChangeSet.class);
        digester.addSetProperties("*/changeset");
        digester.addBeanPropertySetter("*/changeset/type");
        digester.addBeanPropertySetter("*/changeset/date", "xmlDate");
        digester.addBeanPropertySetter("*/changeset/user");
        digester.addBeanPropertySetter("*/changeset/comment");
        digester.addBeanPropertySetter("*/changeset/branch");
        digester.addBeanPropertySetter("*/changeset/repname", "repoName");
        digester.addBeanPropertySetter("*/changeset/repserver", "repoServer");
        digester.addBeanPropertySetter("*/changeset/guid");
        digester.addSetNext("*/changeset", "add");

        digester.addObjectCreate("*/changeset/items/item", ChangeSet.Item.class);
        digester.addSetProperties("*/changeset/items/item");
        digester.addBeanPropertySetter("*/changeset/items/item", "path");
        digester.addSetNext("*/changeset/items/item", "addItem");

        digester.parse(reader);

        return new ChangeSetList(run, browser, changesetList);
    }
}
