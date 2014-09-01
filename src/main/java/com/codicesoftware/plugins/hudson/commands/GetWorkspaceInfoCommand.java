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
    private static final String DEFAULT_SEPARATOR = "def#_#sep";
    private static final String ERROR_MSG_PREFIX = "ERROR";

    public GetWorkspaceInfoCommand(ServerConfigurationProvider provider) {
        super(provider);
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("wi");
        arguments.add("--machinereadable");
        arguments.add("--fieldseparator=" + DEFAULT_SEPARATOR);

        return arguments;
    }

    public WorkspaceInfo parse(Reader r) throws IOException, ParseException {
        
    	BufferedReader reader = new BufferedReader(r);
        
        String line = reader.readLine();
        String[] fields = null;
        if (line != null) {
        	
        	fields = line.split(DEFAULT_SEPARATOR, -1);
        }
        
        if (fields == null || fields.length == 0) {
            return null;
        }

        if (ERROR_MSG_PREFIX.equals(fields[0])) {
            return null;
        }

        String branch = "";
        String label = "";
        String changeset = "";
        
        if (fields[0].equals("BR"))
        {
        	branch = fields[1];
        }
        else if (fields[0].equals("LB"))
        {
        	label = fields[1];
        }
        else if (fields[0].equals("CS"))
        {
            changeset = fields[1];
        }
        
        return new WorkspaceInfo(fields[2], branch, label, changeset);
    }
}