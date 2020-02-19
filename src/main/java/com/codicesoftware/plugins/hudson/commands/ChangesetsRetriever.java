package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

public class ChangesetsRetriever {

    private ChangesetsRetriever() { }

    public static List<ChangeSet> getChangesets(
            PlasticTool tool,
            String branchName,
            String repoSpec,
            Calendar fromTimestamp,
            Calendar toTimestamp) throws IOException, InterruptedException, ParseException {
        DetailedHistoryCommand histCommand = new DetailedHistoryCommand(
                fromTimestamp, toTimestamp, branchName, repoSpec);

        return CommandRunner.executeAndRead(tool, histCommand);
    }

    public static List<ChangeSet> getDetailedHistory(
            PlasticTool tool,
            FilePath workspacePath,
            String branchName,
            String repoSpec,
            Calendar fromTimestamp,
            Calendar toTimestamp) throws IOException, InterruptedException, ParseException {
        List<ChangeSet> list = getChangesets(tool, branchName, repoSpec, fromTimestamp, toTimestamp);

        GetWorkspaceFromPathCommand gwpCommand = new GetWorkspaceFromPathCommand(workspacePath.getRemote());
        Workspace workspace = CommandRunner.executeAndRead(tool, gwpCommand);

        for (ChangeSet cs : list) {
            cs.setWorkspaceDir(workspace.getPath().getRemote());

            GetChangesetRevisionsCommand revs = new GetChangesetRevisionsCommand(cs.getVersion(), cs.getRepository());
            Reader reader = null;
            try {
                reader = tool.execute(revs.getArguments().toCommandArray(), null, true);
                revs.parse(reader, cs);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return list;
    }

    private static String getBranchFromWorkspaceInfo(PlasticTool tool, WorkspaceInfo wi)
            throws InterruptedException, ParseException, IOException {
        String branch = wi.getBranch();
        if (branch != null && !branch.isEmpty()) {
            return branch;
        }

        String label = wi.getLabel();
        if (label != null && !label.isEmpty()) {
            return getBranchFromLabel(tool, label, wi.getRepoName());
        }

        String changeset = wi.getChangeset();
        if (changeset != null && !changeset.isEmpty()) {
            return getBranchFromChangeset(tool, changeset, wi.getRepoName());
        }

        return "";
    }

    private static String getBranchFromLabel(PlasticTool tool, String label, String repositoryName)
            throws InterruptedException, ParseException, IOException {
        GetBranchForLabelCommand brCommand = new GetBranchForLabelCommand(label, repositoryName);
        return CommandRunner.executeAndRead(tool, brCommand);
    }

    private static String getBranchFromChangeset(PlasticTool tool, String id, String repositoryName)
            throws InterruptedException, ParseException, IOException {
        GetBranchForChangesetCommand brCommand = new GetBranchForChangesetCommand(id, repositoryName);
        return CommandRunner.executeAndRead(tool, brCommand);
    }
}
