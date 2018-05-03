package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class GetWorkspaceInfoCommand implements ParseableCommand<WorkspaceInfo>, Command {
    private String mWkPath;

    public GetWorkspaceInfoCommand(String wkPath) {
        mWkPath = wkPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("wi");
        arguments.add(mWkPath);
        arguments.add("--machinereadable");
        arguments.add("--fieldseparator=" + WorkspaceInfoParser.DEFAULT_SEPARATOR);

        return arguments;
    }

    public WorkspaceInfo parse(Reader r) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(r);
        try {
            String line = reader.readLine();
            return WorkspaceInfoParser.parse(line);
        } catch (Exception e) {
            throw new ParseException("Parse error: " + e.getMessage(), 0);
        }
        finally {
            reader.close();
        }
    }
}