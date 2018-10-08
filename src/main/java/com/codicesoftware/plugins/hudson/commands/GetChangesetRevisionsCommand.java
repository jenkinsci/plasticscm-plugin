package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class GetChangesetRevisionsCommand implements Command {
    private final String csVersion;
    private final String repoName;

    public GetChangesetRevisionsCommand(
            String csVersion, String repoName) {
        this.csVersion = csVersion;
        this.repoName = repoName;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("diff");
        arguments.add(String.format("cs:%s@%s", csVersion, repoName));
        arguments.add(String.format("--format={path}%1$s{revid}%1$s{parentrevid}%1$s{status}", SEPARATOR ));
        arguments.add("--repositorypaths");

        return arguments;
    }

    public void parse(Reader reader, ChangeSet cs) throws IOException, ParseException {
        BufferedReader bReader = new BufferedReader(reader);
        String line = null;
        try {
            while ((line = bReader.readLine()) != null) {
                String[] chunks = line.split(SEPARATOR);

                cs.add(new ChangeSet.Item(trimQuotes(
                    chunks[0]), chunks[1], chunks[2], chunks[3]));
            }
        } catch (Exception e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }
        finally {
            bReader.close();
        }
    }

    String trimQuotes(String str) {
        return str.replaceAll("^\"|\"$", "");
    }

    static final String SEPARATOR = "#@_sep_@#";
}
