package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.OutputTempFile;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.ChangesetLogCommand;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.DiffCommand;
import com.codicesoftware.plugins.hudson.commands.FindChangesetCommand;
import com.codicesoftware.plugins.hudson.commands.ParseableCommand;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.ObjectSpec;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

public class ChangesetDetails {

    private static final Logger LOGGER = Logger.getLogger(ChangesetDetails.class.getName());

    private ChangesetDetails() {
    }

    @Nonnull
    public static ChangeSet forWorkspace(
            @Nonnull final PlasticTool tool,
            @Nonnull final FilePath workspace,
            @Nonnull final TaskListener listener) throws IOException, InterruptedException {

        ObjectSpec csetId = CurrentWorkspace.findSpecId(tool, listener, workspace);

        if (csetId == null) {
            throw new AbortException("No changeset ID provided");
        }

        ObjectSpecType type = csetId.getType();
        if (type != ObjectSpecType.Changeset && type != ObjectSpecType.Shelve) {
            throw new AbortException("Invalid object type provided");
        }

        FilePath xmlOutputPath = OutputTempFile.getPathForXml(workspace);
        try {
            if (type == ObjectSpecType.Changeset) {
                return getChangeset(tool, csetId, xmlOutputPath);
            }
            return getShelve(tool, csetId, xmlOutputPath);
        } catch (ParseException e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        } finally {
            OutputTempFile.safeDelete(xmlOutputPath);
        }
    }

    @Nonnull
    private static ChangeSet getChangeset(
            @Nonnull final PlasticTool tool,
            @Nonnull final ObjectSpec csetId,
            @Nonnull final FilePath xmlOutputPath) throws IOException, InterruptedException, ParseException {
        ParseableCommand<ChangeSet> command = new ChangesetLogCommand(csetId, xmlOutputPath);
        return CommandRunner.executeAndRead(tool, command, false);
    }

    @Nonnull
    private static ChangeSet getShelve(
            @Nonnull final PlasticTool tool,
            @Nonnull final ObjectSpec csetId,
            @Nonnull final FilePath xmlOutputPath) throws IOException, InterruptedException, ParseException {
        ParseableCommand<ChangeSet> findChangeset = new FindChangesetCommand(csetId, xmlOutputPath);
        ChangeSet result = CommandRunner.executeAndRead(tool, findChangeset, false);

        ParseableCommand<List<ChangeSet.Item>> command = new DiffCommand(csetId);
        List<ChangeSet.Item> items = CommandRunner.executeAndRead(tool, command, false);
        for (ChangeSet.Item item : items) {
            result.addItem(item);
        }
        return result;
    }
}
