package com.codicesoftware.plugins.hudson.commands.parsers;

import com.codicesoftware.plugins.DigesterUtils;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import hudson.FilePath;
import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class LogOutputParser {
    private static final Logger LOGGER = Logger.getLogger(LogOutputParser.class.getName());

    // Utility classes shouldn't have default constructors
    private LogOutputParser() { }

    public static List<ChangeSet> parseFile(
            FilePath path, String repoName, String server) throws IOException, ParseException {
        List<ChangeSet> csetList = new ArrayList<>();

        if (!SafeFilePath.exists(path)) {
            LOGGER.warning("Log command XML output file not found: " + path);
            return csetList;
        }

        try (InputStream stream = SafeFilePath.read(path)) {
            Digester digester = DigesterUtils.createDigester(
                !Boolean.getBoolean(LogOutputParser.class.getName() + ".UNSAFE"));
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
            if (stream != null) {
                digester.parse(stream);
            }
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }

        for (ChangeSet cset : csetList) {
            cset.setRepoName(repoName);
            cset.setRepoServer(server);
        }

        return csetList;
    }
}
