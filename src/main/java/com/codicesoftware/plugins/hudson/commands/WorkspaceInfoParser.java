package com.codicesoftware.plugins.hudson.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;

public class WorkspaceInfoParser {
    public static final String DEFAULT_SEPARATOR = "def#_#sep";

    public static WorkspaceInfo parse(String line){
        if (line == null)
            return null;

        String[] fields = line.split(DEFAULT_SEPARATOR, -1);

        if (fields.length == 0)
            return null;

        if (ERROR_MSG_PREFIX.equals(fields[0]))
            return null;

        String key = fields[0];
        String value = fields[1];
        String repoName = fields[2];

        String branch = "";
        String label = "";
        String changeset = "";

        if (key.equals("BR"))
            branch = value;
        else if (key.equals("LB"))
            label = value;
        else if (key.equals("CS"))
            changeset = value;

        return new WorkspaceInfo(repoName, branch, label, changeset);
    }

    private static final String ERROR_MSG_PREFIX = "ERROR";
}
