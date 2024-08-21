package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.GetWorkspaceStatusCommand;
import com.codicesoftware.plugins.hudson.commands.ParseableCommand;
import com.codicesoftware.plugins.hudson.model.ObjectSpec;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

public class CurrentWorkspace {

    private static final Logger LOGGER = Logger.getLogger(CurrentWorkspace.class.getName());

    private CurrentWorkspace() {
    }

    /**
     * Returns changeset identifier for the given workspace.
     */
    public static ObjectSpec findSpecId(
            PlasticTool tool,
            TaskListener listener,
            FilePath workspacePath) throws IOException, InterruptedException {
        try {
            ParseableCommand<ObjectSpec> statusCommand = new GetWorkspaceStatusCommand(workspacePath.getRemote());
            return CommandRunner.executeAndRead(tool, statusCommand);
        } catch (ParseException e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        }
    }
}
