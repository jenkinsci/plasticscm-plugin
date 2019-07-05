package com.codicesoftware.plugins.hudson.actions;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.UndoCheckoutCommand;
import com.codicesoftware.plugins.hudson.model.Workspace;

import hudson.FilePath;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CheckoutAction {
    private CheckoutAction() {
    }

    public static void checkout(
            PlasticTool tool,
            String workspaceName,
            FilePath workspacePath,
            String selector,
            boolean useUpdate)
            throws IOException, InterruptedException, ParseException {
        List<Workspace> workspaces = Workspaces.loadWorkspaces(tool);

        cleanOldWorkspacesIfNeeded(
            tool, workspaces, workspaceName, workspacePath, useUpdate);

        checkoutWorkspace(
            tool, workspaceName, workspacePath, selector, useUpdate, workspaces);
    }

    private static Workspace checkoutWorkspace(
            PlasticTool tool,
            String workspaceName,
            FilePath workspacePath,
            String selector,
            boolean useUpdate,
            List<Workspace> workspaces) throws IOException, InterruptedException {
        Workspace workspace = findWorkspaceByPath(workspaces, workspacePath);

        UndoCheckoutCommand undoCommand = new UndoCheckoutCommand(workspacePath);

        if (workspace != null) {
            CommandRunner.execute(tool, undoCommand);

            if (mustUpdateSelector(tool, workspaceName, selector)) {
                Workspaces.setWorkspaceSelector(tool, workspacePath, workspaceName, selector);
                return workspace;
            }

            Workspaces.updateWorkspace(tool, workspace.getPath());
            return workspace;
        }

        if (!useUpdate && workspacePath.exists()) {
            workspacePath.deleteContents();
        }

        workspace = Workspaces.newWorkspace(tool, workspacePath, workspaceName, selector);

        CommandRunner.execute(tool, undoCommand);
        Workspaces.updateWorkspace(tool, workspace.getPath());

        return workspace;
    }

    private static boolean mustUpdateSelector(PlasticTool tool, String name, String selector) {
        String wkSelector = removeNewLinesFromSelector(
            Workspaces.loadSelector(tool, name));
        String currentSelector = removeNewLinesFromSelector(selector);

        return !wkSelector.equals(currentSelector);
    }

    private static String removeNewLinesFromSelector(String selector) {
        return selector.trim().replace("\r\n", "").replace("\n", "").replace("\r", "");
    }

    private static void cleanOldWorkspacesIfNeeded(
            PlasticTool tool,
            List<Workspace> workspaces,
            String workspaceName,
            FilePath workspacePath,
            boolean shouldUseUpdate) throws IOException, InterruptedException {

        // handles from single workspace to additional workspace support
        Workspace parentWorkspace = findWorkspaceByPath(workspaces, workspacePath.getParent());
        if(parentWorkspace != null)
            deleteWorkspace(tool, parentWorkspace, workspaces);

        // handle from additional workspaces to single workspace support
        List<Workspace> nestedWorkspaces = findWorkspacesInsidePath(workspaces, workspacePath);
        for (Workspace workspace : nestedWorkspaces) {
            deleteWorkspace(tool, workspace, workspaces);
        }

        Workspace workspace = findWorkspaceByPath(workspaces, workspacePath);
        if(workspace == null)
            return;

        boolean bHasSameName = workspace.getName().equals(workspaceName);
        boolean bHasSamePath = isSameWorkspacePath(workspace.getPath(), workspacePath.getRemote());

        if (shouldUseUpdate && bHasSameName && bHasSamePath)
            return;

        deleteWorkspace(tool, workspace, workspaces);
    }

    private static boolean isSameWorkspacePath(String actual, String expected) {
        String actualFixed = actual.replaceAll("\\\\", "/");
        String expectedFixed = expected.replaceAll("\\\\", "/");

        Matcher windowsPathMatcher = windowsPathPattern.matcher(expectedFixed);
        if (windowsPathMatcher.matches()) {
            return actualFixed.equalsIgnoreCase(expectedFixed);
        }
        return actualFixed.equals(expectedFixed);
    }

    private static void deleteWorkspace(
            PlasticTool tool, Workspace workspace, List<Workspace> workspaces)
            throws IOException, InterruptedException {
        Workspaces.deleteWorkspace(tool, workspace.getName());
        new FilePath(new File(workspace.getPath())).deleteContents();

        for (int i = workspaces.size() - 1; i >= 0; i--) {
            if(!workspace.getName().equals(workspaces.get(i).getName()))
                continue;
            workspaces.remove(i);
            break;
        }
    }

    private static Workspace findWorkspaceByName(List<Workspace> workspaces, String name)
    {
        for (Workspace workspace : workspaces) {
            if(workspace.getName().equals(name))
                return workspace;
        }
        return null;
    }

    private static Workspace findWorkspaceByPath(List<Workspace> workspaces, FilePath wkPath)
    {
        for (Workspace workspace : workspaces) {
            if(isSameWorkspacePath(workspace.getPath(), wkPath.getRemote()))
                return workspace;
        }
        return null;
    }

    private static List<Workspace> findWorkspacesInsidePath(List<Workspace> workspaces, FilePath wkPath)
    {
        List<Workspace> result = new ArrayList<Workspace>();

        for (Workspace workspace : workspaces) {
            String parentPath = FilenameUtils.getFullPathNoEndSeparator(workspace.getPath());
            if(isSameWorkspacePath(parentPath, wkPath.getRemote()))
                result.add(workspace);
        }
        return result;
    }

    private static Pattern windowsPathPattern = Pattern.compile("^[a-zA-Z]:/.*$");
}
