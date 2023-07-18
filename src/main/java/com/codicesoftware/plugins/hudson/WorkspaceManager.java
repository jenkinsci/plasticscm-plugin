package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.commands.CleanupWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.DeleteWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.GetWorkspaceFromPathCommand;
import com.codicesoftware.plugins.hudson.commands.ListWorkspacesCommand;
import com.codicesoftware.plugins.hudson.commands.NewWorkspaceCommand;
import com.codicesoftware.plugins.hudson.commands.SetSelectorCommand;
import com.codicesoftware.plugins.hudson.commands.SwitchCommand;
import com.codicesoftware.plugins.hudson.commands.UndoCheckoutCommand;
import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.StringUtil;
import com.codicesoftware.plugins.jenkins.AbortExceptionBuilder;
import com.codicesoftware.plugins.jenkins.mergebot.UpdateToSpec;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkspaceManager {

    private static final Logger LOGGER = Logger.getLogger(WorkspaceManager.class.getName());
    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:/.*$");

    private WorkspaceManager() { }

    public static Workspace prepare(
            @Nonnull final PlasticTool tool,
            @Nonnull final TaskListener listener,
            @Nonnull final FilePath workspacePath,
            @Nonnull final CleanupMethod cleanup) throws IOException, InterruptedException {
        try {
            if (!workspacePath.exists()) {
                workspacePath.mkdirs();
            }

            List<Workspace> workspaces = WorkspaceManager.loadWorkspaces(tool, workspacePath.getChannel());

            deleteOldWorkspacesIfNeeded(tool, workspacePath, cleanup, workspaces);
            return cleanup(tool, workspacePath, cleanup, workspaces);
        } catch (ParseException | IOException e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        }
    }

    public static Workspace cleanup(
            PlasticTool tool,
            FilePath workspacePath,
            CleanupMethod cleanup,
            List<Workspace> workspaces)
            throws IOException, InterruptedException, ParseException {
        Workspace workspace = findWorkspaceByPath(workspacePath, workspaces);

        if (workspace != null) {
            LOGGER.fine("Using existing workspace: " + workspace.getName());
            WorkspaceManager.cleanWorkspace(tool, workspace.getPath(), cleanup);
        } else {
            String workspaceName = WorkspaceManager.generateUniqueWorkspaceName();
            LOGGER.fine("Creating new workspace: " + workspaceName);
            if (workspacePath.exists()) {
                workspacePath.deleteContents();
            }
            workspace = WorkspaceManager.createWorkspace(tool, workspacePath, workspaceName);
        }

        return workspace;
    }

    public static List<Workspace> loadWorkspaces(PlasticTool tool, VirtualChannel channel)
            throws IOException, InterruptedException, ParseException {
        ListWorkspacesCommand command = new ListWorkspacesCommand(channel);
        return CommandRunner.executeAndRead(tool, command);
    }

    public static Workspace createWorkspace(PlasticTool tool, FilePath workspacePath, String workspaceName)
            throws IOException, InterruptedException, ParseException {
        NewWorkspaceCommand mkwkCommand = new NewWorkspaceCommand(workspaceName, workspacePath);
        CommandRunner.execute(tool, mkwkCommand);
        GetWorkspaceFromPathCommand gwpCommand = new GetWorkspaceFromPathCommand(workspacePath);
        return CommandRunner.executeAndRead(tool, gwpCommand);
    }

    public static void deleteWorkspace(PlasticTool tool, FilePath workspacePath)
            throws IOException, InterruptedException {
        DeleteWorkspaceCommand command = new DeleteWorkspaceCommand(workspacePath.getRemote());
        CommandRunner.execute(tool, command);
    }

    public static void cleanWorkspace(PlasticTool tool, FilePath workspacePath, CleanupMethod cleanup)
            throws IOException, InterruptedException {
        if (cleanup.removesPrivate()) {
            CleanupWorkspaceCommand cleanupCommands = new CleanupWorkspaceCommand(
                workspacePath.getRemote(), cleanup.removesIgnored());
            CommandRunner.execute(tool, cleanupCommands);
        }
        UndoCheckoutCommand command = new UndoCheckoutCommand(workspacePath.getRemote());
        CommandRunner.execute(tool, command);
    }

    public static void setSelector(
            @Nonnull final PlasticTool tool,
            @Nonnull final FilePath workspacePath,
            @Nonnull final String selector) throws IOException, InterruptedException {
        LOGGER.fine("Changing workspace selector to '" + StringUtil.singleLine(selector) + "'");

        FilePath selectorPath = workspacePath.createTextTempFile("selector", ".txt", selector);
        SetSelectorCommand command = new SetSelectorCommand(workspacePath.getRemote(), selectorPath.getRemote());
        CommandRunner.execute(tool, command);
        selectorPath.delete();
    }

    public static void switchTo(
            @Nonnull final PlasticTool tool,
            @Nonnull final FilePath workspacePath,
            @Nonnull final UpdateToSpec spec) throws IOException, InterruptedException {
        String targetWorkspacePath = workspacePath.getRemote();
        String targetSpec = spec.getFullObjectSpec();

        LOGGER.fine(String.format("Switching workspace at '%s' to '%s'", targetWorkspacePath, targetSpec));
        SwitchCommand command = new SwitchCommand(targetWorkspacePath, targetSpec);
        CommandRunner.execute(tool, command);
    }

    public static String generateUniqueWorkspaceName() {
        return "jenkins_" + UUID.randomUUID().toString().replaceAll("-", "");
    }

    private static void deleteOldWorkspacesIfNeeded(
            PlasticTool tool,
            FilePath workspacePath,
            CleanupMethod cleanup,
            List<Workspace> workspaces)
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

        if (cleanup == CleanupMethod.DELETE) {
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
        return matchWorkspaces(workspaces, testPath -> isNestedWorkspacePath(workspacePath.getRemote(), testPath));
    }

    protected static List<Workspace> findWorkspacesOutsidePath(FilePath workspacePath, List<Workspace> workspaces) {
        return matchWorkspaces(workspaces, testPath -> isNestedWorkspacePath(testPath, workspacePath.getRemote()));
    }

    protected static List<Workspace> matchWorkspaces(List<Workspace> workspaces, WorkspacePathMatcher matcher) {
        List<Workspace> result = new ArrayList<>();
        for (Workspace workspace : workspaces) {
            String testPath = workspace.getPath().getRemote();

            if (matcher.matches(testPath)) {
                result.add(workspace);
            }
        }
        return result;
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

    private interface WorkspacePathMatcher {
        boolean matches(String testPath);
    }
}
