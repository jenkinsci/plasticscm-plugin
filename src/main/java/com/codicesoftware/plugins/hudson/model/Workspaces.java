package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.hudson.commands.DeleteWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.ListWorkspacesCommand;
import com.codicesoftware.plugins.hudson.commands.NewWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.SetSelectorCommand;
import hudson.FilePath;
import hudson.util.IOUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that creates, deletes and gets workspaces from a Plastic SCM server.
 *
 * @author Erik Ramfelt
 * @author Dick Porter
 */
public class Workspaces implements ListWorkspacesCommand.WorkspaceFactory {
    private Map<String,Workspace> workspaces = new HashMap<String,Workspace>();
    private Server server;
    private boolean mapIsPopulatedFromServer;

    public Workspaces(Server server) {
        this.server = server;
    }

    /**
     * Get the list of workspaces from the server
     * @return the list of workspaces at the server
     * @throws IOException
     * @throws InterruptedException
     */
    private List<Workspace> getListFromServer() throws IOException, InterruptedException {
        ListWorkspacesCommand command = new ListWorkspacesCommand(this, server);

        Reader reader = null;
        try {
            reader = server.execute(command.getArguments());
            return command.parse(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Populate the map field with workspaces from the server once.
     * @throws IOException
     * @throws InterruptedException
     */
    private void populateMapFromServer() throws IOException, InterruptedException {
        if (!mapIsPopulatedFromServer) {
            for (Workspace workspace : getListFromServer()) {
                workspaces.put(workspace.getName(), workspace);
            }
            mapIsPopulatedFromServer = true;
        }
    }

    /**
     * Returns the workspace with the specified name
     * @param workspaceName the name of the workspace
     * @return the workspace with the specified name; null if it wasn't found
     * @throws IOException
     * @throws InterruptedException
     */
    public Workspace getWorkspace(String workspaceName) throws IOException, InterruptedException {
        if (!workspaces.containsKey(workspaceName)) {
            populateMapFromServer();
        }
        return workspaces.get(workspaceName);
    }

    /**
     * Returns true if the workspace with the specified name exists on the server
     *
     * @param workspaceName the name of the workspace
     * @return true if the workspace exists on server; false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean exists(String workspaceName) throws IOException, InterruptedException {
        if (!workspaces.containsKey(workspaceName)) {
            populateMapFromServer();
        }
        return workspaces.containsKey(workspaceName);
    }

    /**
     * Returns true if the workspace exists on the server
     *
     * @param workspace the workspace
     * @return true if the workspace exists on server; false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean exists(Workspace workspace) throws IOException, InterruptedException {
        return exists(workspace.getName());
    }

    /**
     * Create workspace on server and return a workspace object with the specified name
     * @param workspacePath the base path of the workspace on the filesystem
     * @param name the name of the new workspace
     * @param selector the initial selector of the new workspace
     * @return a workspace
     * @throws IOException
     * @throws InterruptedException
     */
    public Workspace newWorkspace(FilePath workspacePath, String name, String selector) throws IOException, InterruptedException {
        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", selector);

        NewWorkspaceCommand command = new NewWorkspaceCommand(server, name, workspacePath, selectorPath);
        server.execute(command.getArguments()).close();
        selectorPath.delete();
        Workspace workspace = new Workspace(server, name, workspacePath.getRemote(), selector);
        workspaces.put(name, workspace);
        return workspace;
    }

    /**
     * Set the selector for a workspace
     * @param workspacePath the base path of the workspace
     * @param workspace the workspace
     * @throws IOException
     * @throws InterruptedException
     */
    public void setWorkspaceSelector(FilePath workspacePath, Workspace workspace) throws IOException, InterruptedException {
        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", workspace.getSelector());
        SetSelectorCommand command = new SetSelectorCommand(server, workspace.getName(), selectorPath);
        server.execute(command.getArguments()).close();
        selectorPath.delete();
    }

    /**
     * Deletes the workspace from the server
     * @param workspace the workspace to delete
     * @throws IOException
     * @throws InterruptedException
     */
    public void deleteWorkspace(Workspace workspace) throws IOException, InterruptedException {
        DeleteWorkspaceCommand command = new DeleteWorkspaceCommand(server, workspace.getName());
        workspaces.remove(workspace.getName());
        server.execute(command.getArguments()).close();
    }

    public Workspace createWorkspace(String name, String path, String selector) {
        return new Workspace (server, name, path, selector);
    }
}