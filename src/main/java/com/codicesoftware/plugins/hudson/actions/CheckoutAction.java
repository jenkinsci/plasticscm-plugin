package com.codicesoftware.plugins.hudson.actions;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.WorkspaceManager;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.GetSelectorSpecCommand;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import hudson.FilePath;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckoutAction {

    private static final Logger LOGGER = Logger.getLogger(CheckoutAction.class.getName());
    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:/.*$");

    private CheckoutAction() { }

    public static Workspace checkout(
            PlasticTool tool,
            FilePath workspacePath,
            String selector,
            boolean useUpdate)
            throws IOException, InterruptedException, ParseException {
        List<Workspace> workspaces = WorkspaceManager.loadWorkspaces(tool);

        cleanOldWorkspacesIfNeeded(tool, workspacePath, useUpdate, workspaces);

        if (!useUpdate && workspacePath.exists()) {
            workspacePath.deleteContents();
        }

        return checkoutWorkspace(tool, workspacePath, selector, workspaces);
    }

    private static Workspace checkoutWorkspace(
            PlasticTool tool,
            FilePath workspacePath,
            String selector,
            List<Workspace> workspaces) throws IOException, InterruptedException, ParseException {

        Workspace workspace = findWorkspaceByPath(workspaces, workspacePath);

        if (workspace == null) {
            LOGGER.fine("Creating new workspace");

            String uniqueWorkspaceName = WorkspaceManager.generateUniqueWorkspaceName();
            workspace = WorkspaceManager.newWorkspace(tool, workspacePath, uniqueWorkspaceName, selector);
        }

        WorkspaceManager.cleanWorkspace(tool, workspace.getPath());
        WorkspaceManager.setWorkspaceSelector(tool, workspacePath, selector);

        return workspace;
    }

    private static String removeNewLinesFromSelector(String selector) {
        return selector.trim().replace("\r\n", "").replace("\n", "").replace("\r", "");
    }

    private static void cleanOldWorkspacesIfNeeded(
            PlasticTool tool,
            FilePath workspacePath,
            boolean shouldUseUpdate,
            List<Workspace> workspaces) throws IOException, InterruptedException {

        // handle situation where workspace exists in parent path
        Workspace parentWorkspace = findWorkspaceByPath(workspaces, workspacePath.getParent());
        if (parentWorkspace != null) {
            deleteWorkspace(tool, parentWorkspace, workspaces);
        }

        // handle situation where workspace exists in child path
        List<Workspace> nestedWorkspaces = findWorkspacesInsidePath(workspaces, workspacePath);
        for (Workspace workspace : nestedWorkspaces) {
            deleteWorkspace(tool, workspace, workspaces);
        }

        if (shouldUseUpdate) {
            return;
        }

        Workspace workspace = findWorkspaceByPath(workspaces, workspacePath);
        if (workspace != null) {
            deleteWorkspace(tool, workspace, workspaces);
        }
    }

    private static boolean isSameWorkspacePath(String actual, String expected) {
        String actualFixed = actual.replaceAll("\\\\", "/");
        String expectedFixed = expected.replaceAll("\\\\", "/");

        Matcher windowsPathMatcher = WINDOWS_PATH_PATTERN.matcher(expectedFixed);
        if (windowsPathMatcher.matches()) {
            return actualFixed.equalsIgnoreCase(expectedFixed);
        }
        return actualFixed.equals(expectedFixed);
    }

    private static void deleteWorkspace(
            PlasticTool tool, Workspace workspace, List<Workspace> workspaces)
            throws IOException, InterruptedException {
        WorkspaceManager.deleteWorkspace(tool, workspace.getPath());
        workspace.getPath().deleteContents();
        workspaces.remove(workspace);
    }

    @Deprecated
    private static Workspace findWorkspaceByName(List<Workspace> workspaces, String workspaceName) {
        for (Workspace workspace : workspaces) {
            if (workspace.getName().equals(workspaceName)) {
                return workspace;
            }
        }
        return null;
    }

    private static Workspace findWorkspaceByPath(List<Workspace> workspaces, FilePath workspacePath) {
        for (Workspace workspace : workspaces) {
            if (isSameWorkspacePath(workspace.getPath().getRemote(), workspacePath.getRemote())) {
                return workspace;
            }
        }
        return null;
    }

    private static List<Workspace> findWorkspacesInsidePath(List<Workspace> workspaces, FilePath workspacePath) {
        List<Workspace> result = new ArrayList<>();

        for (Workspace workspace : workspaces) {
            String parentPath = FilenameUtils.getFullPathNoEndSeparator(workspace.getPath().getRemote());
            if (isSameWorkspacePath(parentPath, workspacePath.getRemote())) {
                result.add(workspace);
            }
        }
        return result;
    }
}
