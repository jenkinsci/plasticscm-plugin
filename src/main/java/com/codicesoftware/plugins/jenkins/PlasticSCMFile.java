package com.codicesoftware.plugins.jenkins;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import javafx.scene.control.TextFormatter;
import jenkins.scm.api.SCMFile;
import hudson.AbortException;

import com.codicesoftware.plugins.hudson.PlasticSCM;
import com.codicesoftware.plugins.hudson.commands.GetFileCommand;
import com.codicesoftware.plugins.hudson.commands.GetSelectorSpecCommand;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import com.codicesoftware.plugins.hudson.util.DeleteOnCloseFileInputStream;

public class PlasticSCMFile extends SCMFile {
    public PlasticSCMFile(PlasticSCMFileSystem fs) {
        this.fs = fs;
        this.isDir = true;
    }

    public PlasticSCMFile(PlasticSCMFileSystem fs, @Nonnull PlasticSCMFile parent, String name, boolean isDir) {
        super(parent, name);
        this.fs = fs;
        this.isDir = isDir;
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull String name, boolean assumeIsDirectory) {
        return new PlasticSCMFile(fs, this, name, assumeIsDirectory);
    }

    @Nonnull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        return new ArrayList<SCMFile>();
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    protected Type type() throws IOException, InterruptedException {
        return isDir ? Type.DIRECTORY : Type.REGULAR_FILE;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        PlasticSCM.WorkspaceInfo workspaceInfo = fs.getSCM().getFirstWorkspace();
        String serverFile = getPath();

        if(fs.getSCM().isUseMultipleWorkspaces()) {
            String workspaceName = getWorkspaceNameFromScriptPath(getPath());
            serverFile = getServerFileFromScriptPath(getPath());

            if (workspaceName == null || serverFile == null)
                throw new FileNotFoundException("The pipeline script path is not valid. " +
                    "Maybe you didn't use '/' as directory separator.");

            workspaceInfo = getWorkspaceInfo(fs.getSCM(), workspaceName);

            if (workspaceInfo == null) {
                throw new FileNotFoundException(String.format(
                    "The pipeline script path must start by a valid workspace name. " +
                    "The workspace '%s' was not found.", workspaceName));
            }
        }

        PlasticTool tool = new PlasticTool(
            fs.getSCM().getDescriptor().getCmExecutable(),
            fs.getLauncher(), fs.getLauncher().getListener(), null);

        String repObjectSpec = getRepObjectSpecFromSelector(tool, workspaceInfo.getSelector());

        serverFile = ensureScripPathStartsBySlash(serverFile);

        return getFileContent(tool, serverFile, repObjectSpec);
    }

    private static DeleteOnCloseFileInputStream getFileContent(
        PlasticTool tool, String serverFile, String repObjectSpec) throws IOException, InterruptedException {
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");

        String serverPathRevSpec = "serverpath:" + serverFile + "#" + repObjectSpec;
        getFile(tool, serverPathRevSpec, tempFile.getPath());

        return new DeleteOnCloseFileInputStream(tempFile);
    }

    private static String getRepObjectSpecFromSelector(
        PlasticTool tool, String selectorText) throws IOException, InterruptedException {
        File tempFile = null;
        BufferedWriter out = null;
        try {
            tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");

            out = new BufferedWriter(new FileWriter(tempFile));
            out.write(selectorText);
            out.close();
            out = null;

            WorkspaceInfo workspaceInfo = getSelectorSpec(tool, tempFile.getPath());
            return workspaceInfo.getRepObjectSpec();
        } catch (AbortException e) {
            logger.severe(e.getMessage());
            throw new AbortException(
                "An error was detected while getting the selector spec. " +
                "This usually happens with old versions of 'cm'. " +
                "Please ensure the latest version of 'cm' is installed.");
        } catch (ParseException e) {
            logger.severe(e.getMessage());
            throw new AbortException("The selector is not valid.");
        }
        finally {
            if (out != null)
                out.close();

            if (tempFile != null)
                tempFile.delete();
        }
    }

    private static PlasticSCM.WorkspaceInfo getWorkspaceInfo(PlasticSCM scm, String workspaceName){
        for (PlasticSCM.WorkspaceInfo workspace : scm.getAllWorkspaces()) {
            if (workspaceName.equals(workspace.getWorkspaceName()))
                return workspace;
        }
        return null;
    }

    private static String getWorkspaceNameFromScriptPath(String scriptPath){
        int separatorIndex = scriptPath.indexOf(SEPARATOR);

        if (separatorIndex == -1)
            return null;

        return scriptPath.substring(0, separatorIndex).trim();
    }

    private static String getServerFileFromScriptPath(String scriptPath){
        int separatorIndex = scriptPath.indexOf(SEPARATOR);

        if (separatorIndex == -1)
            return null;

        return scriptPath.substring(separatorIndex).trim();
    }

    private static String ensureScripPathStartsBySlash(String scriptPath){
        return scriptPath.startsWith(SEPARATOR) ? scriptPath : SEPARATOR + scriptPath;
    }

    private static void getFile(PlasticTool tool, String revSpec, String filePath) throws IOException, InterruptedException {
        GetFileCommand command = new GetFileCommand(revSpec, filePath);
        CommandRunner.execute(tool, command).close();
    }

    private static WorkspaceInfo getSelectorSpec(PlasticTool tool, String filePath) throws IOException, ParseException, InterruptedException {
        GetSelectorSpecCommand command = new GetSelectorSpecCommand(filePath);
        return CommandRunner.executeAndRead(tool, command, command);
    }

    private final PlasticSCMFileSystem fs;
    private final boolean isDir;

    private static final String SEPARATOR = "/";
    private static final Logger logger = Logger.getLogger(PlasticSCMFile.class.getName());
}
