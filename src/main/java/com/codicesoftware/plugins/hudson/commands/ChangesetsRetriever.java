package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.model.*;
import java.io.IOException;
import java.io.Reader;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import hudson.FilePath;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class ChangesetsRetriever {
    public static List<ChangeSet> getChangesets(
            PlasticTool tool,
            FilePath wkPath,
            String branchName,
            String repoSpec,
            Calendar fromTimestamp,
            Calendar toTimestamp) throws IOException, InterruptedException, ParseException {
        List<ChangeSet> list = new ArrayList<ChangeSet>();

        if(branchName == null || repoSpec == null) {
            GetWorkspaceInfoCommand wiCommand = new GetWorkspaceInfoCommand(wkPath.getRemote());
            WorkspaceInfo wi = CommandRunner.executeAndRead(tool, wiCommand, wiCommand);
            branchName = GetBranchFromWorkspaceInfo(tool, wi);
            repoSpec = wi.getRepoName();
        }
        else {
            // when we have all the information needed
            // if the workspace does not exist yet,
            // don't specify the execution path.
            wkPath = null;
        }

        DetailedHistoryCommand histCommand = new DetailedHistoryCommand(
                fromTimestamp, toTimestamp, branchName, repoSpec);
        return CommandRunner.executeAndRead(tool, histCommand, histCommand, wkPath);
    }

    public static List<ChangeSet> getDetailedHistory(
            PlasticTool tool, FilePath wkPath, Calendar fromTimestamp, Calendar toTimestamp)
            throws IOException, InterruptedException, ParseException {
        List<ChangeSet> list = getChangesets(tool, wkPath, null, null, fromTimestamp, toTimestamp);

        GetWorkspaceFromPathCommand gwpCommand = new GetWorkspaceFromPathCommand(wkPath.getRemote());
        String workspaceDir = CommandRunner.executeAndRead(tool, gwpCommand, gwpCommand);

        for(ChangeSet cs : list) {
            cs.setWorkspaceDir(workspaceDir);

            GetChangesetRevisionsCommand revs = new GetChangesetRevisionsCommand(
                    cs.getVersion(), cs.getRepository());
            Reader reader = null;
            try {
                reader = tool.execute(revs.getArguments().toCommandArray());
                revs.parse(reader, cs);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return list;
    }

    private static String GetBranchFromWorkspaceInfo(
            PlasticTool tool, WorkspaceInfo wi)
            throws InterruptedException, ParseException, IOException {
        String branch = wi.getBranch();
        if (branch != null && !branch.isEmpty())
            return branch;

        String label = wi.getLabel();
        if (label != null && !label.isEmpty())
            return GetBranchFromLabel(tool, label, wi.getRepoName());

        String changeset = wi.getChangeset();
        if (changeset != null && !changeset.isEmpty())
            return GetBranchFromChangeset(tool, changeset, wi.getRepoName());

        return "";
    }

    private static String GetBranchFromLabel(PlasticTool tool, String label, String repositoryName)
            throws InterruptedException, ParseException, IOException {
        GetBranchForLabelCommand brCommand = new GetBranchForLabelCommand(
                label, repositoryName);
        return CommandRunner.executeAndRead(tool, brCommand, brCommand);
    }

    private static String GetBranchFromChangeset(PlasticTool tool, String id, String repositoryName)
            throws InterruptedException, ParseException, IOException {
        GetBranchForChangesetCommand brCommand = new GetBranchForChangesetCommand(
                id, repositoryName);
        return CommandRunner.executeAndRead(tool, brCommand, brCommand);
    }
}