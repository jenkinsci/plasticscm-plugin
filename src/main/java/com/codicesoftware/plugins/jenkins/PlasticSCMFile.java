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

import jenkins.scm.api.SCMFile;
import hudson.AbortException;

import com.codicesoftware.plugins.hudson.PlasticSCM;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.model.Server;
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
        String workspaceName = getWorkspaceNameFromScriptPath(getPath());
        String serverFile = getServerFileFromScriptPath(getPath());

        if (workspaceName == null || serverFile == null)
            throw new FileNotFoundException("The pipeline script path is not valid.");

        PlasticSCM.WorkspaceInfo workspaceInfo = getWorkspaceInfo(fs.getSCM(), workspaceName);

        if (workspaceInfo == null) {
            throw new FileNotFoundException(String.format(
               "The pipeline script path must start by a valid workspace name. " +
                "The workspace '%s' was not found.", workspaceName));
        }

        Server server = new Server(new PlasticTool(
            fs.getSCM().getDescriptor().getCmExecutable(),
            fs.getLauncher(), fs.getLauncher().getListener(), null));

        String repObjectSpec = getRepObjectSpecFromSelector(server, workspaceInfo.getSelector());

        return getFileContent(server, serverFile, repObjectSpec);
    }

    private static DeleteOnCloseFileInputStream getFileContent(
        Server server, String serverFile, String repObjectSpec) throws IOException, InterruptedException {
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");

        String serverPathRevSpec = "serverpath:" + serverFile + "#" + repObjectSpec;
        server.getFile(serverPathRevSpec, tempFile.getPath());

        return new DeleteOnCloseFileInputStream(tempFile);
    }

    private static String getRepObjectSpecFromSelector(
        Server server, String selectorText) throws IOException, InterruptedException {
        File tempFile = null;
        BufferedWriter out = null;
        try {
            tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");

            out = new BufferedWriter(new FileWriter(tempFile));
            out.write(selectorText);
            out.close();
            out = null;

            WorkspaceInfo workspaceInfo = server.getSelectorSpec(tempFile.getPath());
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
        if (workspaceName.equals(scm.getFirstWorkspace().getWorkspaceName()))
            return scm.getFirstWorkspace();

        for (PlasticSCM.WorkspaceInfo additionalWorkspace : scm.getAdditionalWorkspaces()) {
            if (workspaceName.equals(additionalWorkspace.getWorkspaceName()))
                return additionalWorkspace;
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

    private final PlasticSCMFileSystem fs;
    private final boolean isDir;

    private static final String SEPARATOR = "/";
    private static final Logger logger = Logger.getLogger(PlasticSCMFile.class.getName());
}
