package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.DeleteWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.GetSelectorCommand;
import com.codicesoftware.plugins.hudson.commands.GetWorkspaceFromPathCommand;
import com.codicesoftware.plugins.hudson.commands.ListWorkspacesCommand;
import com.codicesoftware.plugins.hudson.commands.NewWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.SetSelectorCommand;
import com.codicesoftware.plugins.hudson.commands.UndoCheckoutCommand;
import com.codicesoftware.plugins.hudson.commands.UpdateWorkspaceCommand;
import com.codicesoftware.plugins.hudson.model.Workspace;
import hudson.FilePath;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class WorkspaceManager {

    private WorkspaceManager() { }

    public static List<Workspace> loadWorkspaces(PlasticTool tool)
            throws IOException, InterruptedException, ParseException {
        ListWorkspacesCommand command = new ListWorkspacesCommand();
        return CommandRunner.executeAndRead(tool, command, command);
    }

    public static Workspace createWorkspace(PlasticTool tool, FilePath workspacePath, String workspaceName, String selector)
            throws IOException, InterruptedException, ParseException {
        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", selector);
        NewWorkspaceCommand mkwkCommand = new NewWorkspaceCommand(workspaceName, workspacePath, selectorPath);
        CommandRunner.execute(tool, mkwkCommand);
        selectorPath.delete();
        GetWorkspaceFromPathCommand gwpCommand = new GetWorkspaceFromPathCommand(workspacePath.getRemote());
        return CommandRunner.executeAndRead(tool, gwpCommand, gwpCommand);
    }

    public static void deleteWorkspace(PlasticTool tool, FilePath workspacePath)
            throws IOException, InterruptedException {
        DeleteWorkspaceCommand command = new DeleteWorkspaceCommand(workspacePath.getRemote());
        CommandRunner.execute(tool, command);
    }

    public static void updateWorkspace(PlasticTool tool, FilePath workspacePath)
            throws IOException, InterruptedException {
        UpdateWorkspaceCommand command = new UpdateWorkspaceCommand(workspacePath.getRemote());
        CommandRunner.execute(tool, command);
    }

    public static void cleanWorkspace(PlasticTool tool, FilePath workspacePath)
            throws IOException, InterruptedException {
        UndoCheckoutCommand command = new UndoCheckoutCommand(workspacePath.getRemote());
        CommandRunner.execute(tool, command);
    }

    public static String getSelector(PlasticTool tool, FilePath workspacePath)
            throws IOException, InterruptedException, ParseException {
        GetSelectorCommand command = new GetSelectorCommand(workspacePath.getRemote());
        return CommandRunner.executeAndRead(tool, command, command, false);
    }

    public static void setSelector(PlasticTool tool, FilePath workspacePath, String selector)
            throws IOException, InterruptedException {
        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", selector);
        SetSelectorCommand command = new SetSelectorCommand(workspacePath.getRemote(), selectorPath.getRemote());
        CommandRunner.execute(tool, command);
        selectorPath.delete();
    }

    public static String generateUniqueWorkspaceName() {
        return "jenkins_" + UUID.randomUUID().toString().replaceAll("-", "");
    }
}
