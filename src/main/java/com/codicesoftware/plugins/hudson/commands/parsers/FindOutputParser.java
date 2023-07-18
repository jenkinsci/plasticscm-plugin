package com.codicesoftware.plugins.hudson.commands.parsers;

import com.codicesoftware.plugins.DigesterUtils;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.jenkins.ObjectSpecType;
import hudson.FilePath;
import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
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

    @Nonnull
    public static List<ChangeSet> parseReader(
            @Nonnull final ObjectSpecType specType,
            @Nonnull final FilePath path) throws IOException, ParseException {
        if (specType != ObjectSpecType.Changeset && specType != ObjectSpecType.Shelve) {
            throw new ParseException("Invalid object type provided", 0);
        }

        List<ChangeSet> csetList = new ArrayList<>();

        if (!SafeFilePath.exists(path)) {
            LOGGER.warning("Find command XML output file not found: " + path);
            return csetList;
        }

        try (InputStream stream = SafeFilePath.read(path)) {
            Digester digester = DigesterUtils.createDigester(
                !Boolean.getBoolean(FindOutputParser.class.getName() + ".UNSAFE"));

            digester.push(csetList);

            String objectTag = specType == ObjectSpecType.Shelve ? "SHELVE" : "CHANGESET";
            String root = "*/" + objectTag;

            digester.addObjectCreate(root, ChangeSet.class);
            digester.addBeanPropertySetter(String.format("%s/%sID", root, objectTag), "version");
            digester.addBeanPropertySetter(root + "/COMMENT", "comment");
            digester.addBeanPropertySetter(root + "/DATE", "xmlDate");
            if (specType == ObjectSpecType.Changeset) {
                digester.addBeanPropertySetter(root + "/BRANCH", "branch");
            }
            digester.addBeanPropertySetter(root + "/OWNER", "user");
            digester.addBeanPropertySetter(root + "/REPNAME", "repoName");
            digester.addBeanPropertySetter(root + "/REPSERVER", "repoServer");
            digester.addBeanPropertySetter(root + "/GUID", "guid");
            digester.addSetNext(root, "add");
            if (stream != null) {
                digester.parse(stream);
            }
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }
        return csetList;
    }
}
