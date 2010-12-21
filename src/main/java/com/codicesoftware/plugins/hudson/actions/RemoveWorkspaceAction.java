package com.codicesoftware.plugins.hudson.actions;

import com.codicesoftware.plugins.hudson.model.Server;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.model.Workspaces;
import java.io.IOException;

/**
 * Removes a workspace from a Plastic SCM server.
 *
 * @author Erik Ramfelt
 * @author Dick Porter
 */
public class RemoveWorkspaceAction {
    private final String workspaceName;

    public RemoveWorkspaceAction(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public boolean remove(Server server) throws IOException, InterruptedException {
        Workspaces workspaces = server.getWorkspaces();
        if (workspaces.exists(workspaceName)) {
            Workspace workspace = workspaces.getWorkspace(workspaceName);
            workspaces.deleteWorkspace(workspace);
            return true;
        }
        return false;
    }
}