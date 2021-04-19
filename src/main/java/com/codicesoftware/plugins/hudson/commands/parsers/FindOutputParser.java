package com.codicesoftware.plugins.hudson.commands.parsers;

import com.codicesoftware.plugins.hudson.model.ChangeSet;
import hudson.FilePath;
import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
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

        Digester digester = new Digester();

        digester.setXIncludeAware(false);

        if (!Boolean.getBoolean(FindOutputParser.class.getName() + ".UNSAFE")) {
            try {
                digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }
            catch ( SAXException | ParserConfigurationException ex) {
                throw new IOException("Failed to securely configure CVS changelog parser", ex);
            }
        }

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

        try (InputStream stream = SafeFilePath.read(path)) {
            if (stream != null) {
                digester.parse(stream);
            }
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }
        return csetList;
    }
}
