package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.actions.CheckoutAction;
import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.Workspace;
import hudson.FilePath;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

public class WorkspaceSetup {

    private static final Logger LOGGER = Logger.getLogger(WorkspaceSetup.class.getName());

    private WorkspaceSetup() {
    }

    public static Workspace perform(
            @Nonnull final PlasticTool tool,
            @Nonnull final TaskListener listener,
            @Nonnull final FilePath workspacePath,
            @Nonnull final String selector,
            @Nonnull final CleanupMethod cleanup) throws IOException, InterruptedException {
        try {
            if (!workspacePath.exists()) {
                workspacePath.mkdirs();
            }
            return CheckoutAction.checkout(tool, workspacePath, selector, cleanup);
        } catch (ParseException | IOException e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        }
    }
}
