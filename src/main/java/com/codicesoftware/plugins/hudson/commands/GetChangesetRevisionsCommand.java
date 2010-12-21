package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.util.Digester2;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class GetChangesetRevisionsCommand extends AbstractCommand {
    private final String csVersion;
    private final String repoName;

    public GetChangesetRevisionsCommand(ServerConfigurationProvider configurationProvider,
            String csVersion, String repoName) {
        super(configurationProvider);
        this.csVersion = csVersion;
        this.repoName = repoName;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("find");
        arguments.add("revisions");
        arguments.add("where");
        arguments.add("changeset=" + csVersion);
        arguments.add("on");
        arguments.add("repositories");
        arguments.add("'" + repoName + "'");
        arguments.add("--xml");

        return arguments;
    }

    public void parse(Reader reader, ChangeSet cs) throws IOException, ParseException {
        Digester digester = new Digester2();
        digester.push(cs);

        digester.addObjectCreate("*/REVISION", ChangeSet.Item.class);
        digester.addBeanPropertySetter("*/REVISION/ITEM", "path");
        digester.addBeanPropertySetter("*/REVISION/REVNO", "revno");
        digester.addBeanPropertySetter("*/REVISION/PARENT", "parentRevno");
        digester.addSetNext("*/REVISION", "add");

        try {
            digester.parse(reader);
        } catch (SAXException e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }
    }
}
