package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.OutputTempFile;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import hudson.FilePath;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

public class ChangesetsRetriever {

    private ChangesetsRetriever() { }

    public static List<ChangeSet> getChangesets(
            PlasticTool tool,
            FilePath workspacePath,
            String branchName,
            String repoSpec,
            Calendar fromTimestamp,
            Calendar toTimestamp) throws IOException, InterruptedException, ParseException {
        FilePath xmlOutputFile = OutputTempFile.getPathForXml(workspacePath);

        DetailedHistoryCommand histCommand = new DetailedHistoryCommand(
                fromTimestamp, toTimestamp, branchName, repoSpec, xmlOutputFile);

        try {
            return CommandRunner.executeAndRead(tool, histCommand);
        } finally {
            OutputTempFile.safeDelete(xmlOutputFile);
        }
    }
}
