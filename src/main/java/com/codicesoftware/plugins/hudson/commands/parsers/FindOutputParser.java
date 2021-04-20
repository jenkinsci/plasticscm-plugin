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

public class FindOutputParser {
    private static final Logger LOGGER = Logger.getLogger(FindOutputParser.class.getName());

    // Utility classes shouldn't have default constructors
    private FindOutputParser() { }

    public static List<ChangeSet> parseReader(FilePath path) throws IOException, ParseException {
        List<ChangeSet> csetList = new ArrayList<>();

        if (!SafeFilePath.exists(path)) {
            LOGGER.warning("Find command XML output file not found: " + path);
            return csetList;
        }

        try (InputStream stream = SafeFilePath.read(path)) {
            Digester digester = DigesterUtils.createDigester(!Boolean.getBoolean(FindOutputParser.class.getName() + ".UNSAFE"));

            digester.push(csetList);

            digester.addObjectCreate("*/CHANGESET", ChangeSet.class);
            digester.addBeanPropertySetter("*/CHANGESET/CHANGESETID", "version");
            digester.addBeanPropertySetter("*/CHANGESET/COMMENT", "comment");
            digester.addBeanPropertySetter("*/CHANGESET/DATE", "xmlDate");
            digester.addBeanPropertySetter("*/CHANGESET/BRANCH", "branch");
            digester.addBeanPropertySetter("*/CHANGESET/OWNER", "user");
            digester.addBeanPropertySetter("*/CHANGESET/REPNAME", "repoName");
            digester.addBeanPropertySetter("*/CHANGESET/REPSERVER", "repoServer");
            digester.addBeanPropertySetter("*/CHANGESET/GUID", "guid");
            digester.addSetNext("*/CHANGESET", "add");
            if (stream != null) {
                digester.parse(stream);
            }
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }
        return csetList;
    }
}
