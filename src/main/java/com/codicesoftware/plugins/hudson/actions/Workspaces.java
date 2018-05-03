package com.codicesoftware.plugins.hudson.actions;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.*;
import com.codicesoftware.plugins.hudson.model.Workspace;

import hudson.FilePath;
import hudson.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

public class Workspaces {
    public static List<Workspace> loadWorkspaces(PlasticTool tool)
            throws IOException, InterruptedException, ParseException {
        ListWorkspacesCommand command = new ListWorkspacesCommand();
        return CommandRunner.executeAndRead(tool, command, command);
    }

    public static Workspace newWorkspace(
            PlasticTool tool,
            FilePath workspacePath,
            String name,
            String selector) throws IOException, InterruptedException {
        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", selector);

        NewWorkspaceCommand command = new NewWorkspaceCommand(name, workspacePath, selectorPath);
        CommandRunner.execute(tool, command);
        selectorPath.delete();
        return new Workspace(name, workspacePath.getRemote());
    }

    public static void setWorkspaceSelector(
            PlasticTool tool,
            FilePath workspacePath,
            String name,
            String selector) throws IOException, InterruptedException {
        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", selector);
        SetSelectorCommand command = new SetSelectorCommand(name, selectorPath);
        CommandRunner.execute(tool, command);
        selectorPath.delete();
    }

    public static void deleteWorkspace(PlasticTool tool, String name) throws IOException, InterruptedException {
        DeleteWorkspaceCommand command = new DeleteWorkspaceCommand(name);
        CommandRunner.execute(tool, command);
    }

    public static void updateWorkspace(PlasticTool tool, String localPath) throws IOException, InterruptedException {
        UpdateWorkspaceCommand command = new UpdateWorkspaceCommand(localPath);
        CommandRunner.execute(tool, command);
    }

    public static String loadSelector(PlasticTool tool, String name) {
        GetSelectorCommand command = new GetSelectorCommand(name);
        try {
            return CommandRunner.executeAndRead(tool, command, command);
        } catch (Exception e) {
            return null;
        }
    }
}