package com.codicesoftware.plugins.hudson.actions;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.WorkspaceManager;
import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.StringUtil;
import hudson.FilePath;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckoutAction {

    private static final Logger LOGGER = Logger.getLogger(CheckoutAction.class.getName());
    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:/.*$");

    private CheckoutAction() { }

    public static Workspace checkout(PlasticTool tool, FilePath workspacePath, String selector, CleanupMethod cleanupMethod)
            throws IOException, InterruptedException, ParseException {
        List<Workspace> workspaces = WorkspaceManager.loadWorkspaces(tool);

        deleteOldWorkspacesIfNeeded(tool, workspacePath, cleanupMethod, workspaces);

        return checkoutWorkspace(tool, workspacePath, selector, cleanupMethod, workspaces);
    }

    private static Workspace checkoutWorkspace(PlasticTool tool, FilePath workspacePath, String selector, CleanupMethod cleanupMethod, List<Workspace> workspaces)
            throws IOException, InterruptedException, ParseException {
        Workspace workspace = findWorkspaceByPath(workspacePath, workspaces);

        if (workspace != null) {
            LOGGER.fine("Using existing workspace: " + workspace.getName());
            WorkspaceManager.cleanWorkspace(tool, workspace.getPath(), cleanupMethod);
        } else {
            String workspaceName = WorkspaceManager.generateUniqueWorkspaceName();
            LOGGER.fine("Creating new workspace: " + workspaceName);
            if (workspacePath.exists()) {
                workspacePath.deleteContents();
            }
            workspace = WorkspaceManager.createWorkspace(tool, workspacePath, workspaceName, selector);
        }

        LOGGER.fine("Changing workspace selector to '" + StringUtil.singleLine(selector) + "'");
        WorkspaceManager.setSelector(tool, workspacePath, selector);
        // Setting workspace selector triggers workspace update
        // WorkspaceManager.updateWorkspace(tool, workspace.getPath());

        return workspace;
    }

    private static boolean mustUpdateSelector(PlasticTool tool, FilePath workspacePath, String selector)
            throws IOException, InterruptedException, ParseException {
        String actualSelector = StringUtil.removeNewLines(WorkspaceManager.getSelector(tool, workspacePath));
        String expectedSelector = StringUtil.removeNewLines(selector);
        return !actualSelector.equals(expectedSelector);
    }

    private static void deleteOldWorkspacesIfNeeded(PlasticTool tool, FilePath workspacePath, CleanupMethod cleanupMethod, List<Workspace> workspaces)
            throws IOException, InterruptedException {

        // Handle situation where workspace exists in child path.
        List<Workspace> innerWorkspaces = findWorkspacesInsidePath(workspacePath, workspaces);
        for (Workspace workspace : innerWorkspaces) {
            deleteWorkspace(tool, workspace, workspaces);
        }

        // Handle situation where workspace exists in parent path.
        List<Workspace> outerWorkspaces = findWorkspacesOutsidePath(workspacePath, workspaces);
        for (Workspace workspace : outerWorkspaces) {
            deleteWorkspace(tool, workspace, workspaces);
        }

        if (cleanupMethod == CleanupMethod.DELETE) {
            Workspace workspace = findWorkspaceByPath(workspacePath, workspaces);
            if (workspace != null) {
                deleteWorkspace(tool, workspace, workspaces);
            }
        }
    }

    protected static boolean isSameWorkspacePath(String actual, String expected) {
        String actualFixed = actual.replaceAll("\\\\", "/");
        String expectedFixed = expected.replaceAll("\\\\", "/");

        Matcher windowsPathMatcher = WINDOWS_PATH_PATTERN.matcher(expectedFixed);
        // Windows paths are case insensitive
        if (windowsPathMatcher.matches()) {
            return actualFixed.equalsIgnoreCase(expectedFixed);
        }
        return actualFixed.equals(expectedFixed);
    }

    protected static boolean isNestedWorkspacePath(String base, String nested) {
        String baseFixed = base.replaceAll("\\\\", "/") + "/";
        String nestedFixed = nested.replaceAll("\\\\", "/");

        Matcher windowsPathMatcher = WINDOWS_PATH_PATTERN.matcher(nestedFixed);
        // Windows paths are case insensitive
        if (windowsPathMatcher.matches()) {
            return nestedFixed.toLowerCase().startsWith(baseFixed.toLowerCase());
        }
        return nestedFixed.startsWith(baseFixed);
    }

    private static void deleteWorkspace(
            PlasticTool tool, Workspace workspace, List<Workspace> workspaces)
            throws IOException, InterruptedException {
        WorkspaceManager.deleteWorkspace(tool, workspace.getPath());
        workspace.getPath().deleteContents();
        workspaces.remove(workspace);
    }

    protected static Workspace findWorkspaceByPath(FilePath workspacePath, List<Workspace> workspaces) {
        for (Workspace workspace : workspaces) {
            if (isSameWorkspacePath(workspace.getPath().getRemote(), workspacePath.getRemote())) {
                return workspace;
            }
        }
        return null;
    }

    protected static List<Workspace> findWorkspacesInsidePath(FilePath workspacePath, List<Workspace> workspaces) {
        String basePath = workspacePath.getRemote();

        List<Workspace> result = new ArrayList<>();
        for (Workspace workspace : workspaces) {
            String testPath = workspace.getPath().getRemote();

            if (isNestedWorkspacePath(basePath, testPath)) {
                result.add(workspace);
            }
        }
        return result;
    }

    protected static List<Workspace> findWorkspacesOutsidePath(FilePath workspacePath, List<Workspace> workspaces) {
        String basePath = workspacePath.getRemote();

        List<Workspace> result = new ArrayList<>();
        for (Workspace workspace : workspaces) {
            String testPath = workspace.getPath().getRemote();

            if (isNestedWorkspacePath(testPath, basePath)) {
                result.add(workspace);
            }
        }
        return result;
    }
}
