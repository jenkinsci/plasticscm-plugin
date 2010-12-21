package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetWorkspaceInfoCommand extends AbstractCommand implements ParseableCommand<WorkspaceInfo> {
    private static final Pattern repoRegex = Pattern.compile("^Repository:\\s*(.+)$");
    private static final Pattern branchRegex = Pattern.compile("^Branch:\\s*(.+)$");
    private static final Pattern labelRegex = Pattern.compile("^Label:\\s*(.+)$");

    public GetWorkspaceInfoCommand(ServerConfigurationProvider provider) {
        super(provider);
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("wi");

        return arguments;
    }

    public WorkspaceInfo parse(Reader r) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        String repoName = "";
        String branch = "";
        String label = "";
        while (line != null) {
            Matcher matcher = repoRegex.matcher(line);
            if (matcher.find()) {
                repoName = matcher.group(1);
            } else {
                matcher = branchRegex.matcher(line);
                if (matcher.find()) {
                    branch = matcher.group(1);
                } else {
                    matcher = labelRegex.matcher(line);
                    if (matcher.find()) {
                        label = matcher.group(1);
                    }
                }
            }
            line = reader.readLine();
        }

        return new WorkspaceInfo(repoName, branch, label);
    }
}