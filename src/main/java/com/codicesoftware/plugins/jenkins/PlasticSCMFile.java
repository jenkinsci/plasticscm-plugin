package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticSCM;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.GetFileCommand;
import com.codicesoftware.plugins.hudson.commands.GetSelectorSpecCommand;
import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import com.codicesoftware.plugins.hudson.util.DeleteOnCloseFileInputStream;
import com.codicesoftware.plugins.hudson.util.SelectorParametersResolver;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import jenkins.scm.api.SCMFile;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class PlasticSCMFile extends SCMFile {

    private static final String SEPARATOR = "/";
    private static final Logger LOGGER = Logger.getLogger(PlasticSCMFile.class.getName());
    private final PlasticSCMFileSystem fs;
    private final boolean isDir;

    public PlasticSCMFile(PlasticSCMFileSystem fs) {
        this.fs = fs;
        this.isDir = true;
    }

    public PlasticSCMFile(PlasticSCMFileSystem fs, PlasticSCMFile parent, String name, boolean isDir) {
        super(parent, name);
        this.fs = fs;
        this.isDir = isDir;
    }

    private static List<ParameterValue> getLastBuildParameters(
            PlasticSCMFileSystem fs) {
        Run<?, ?> run = fs.getLastBuildFromFirstJob();
        if (run == null) {
            return null;
        }

        ParametersAction parameters = run.getAction(ParametersAction.class);
        return parameters != null ? parameters.getParameters() : null;
    }

    private static DeleteOnCloseFileInputStream getFileContent(PlasticTool tool, String serverFile, String repObjectSpec)
            throws IOException, InterruptedException {
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");

        String serverPathRevSpec = "serverpath:" + serverFile + "#" + repObjectSpec;
        getFile(tool, serverPathRevSpec, tempFile.getPath());

        return new DeleteOnCloseFileInputStream(tempFile);
    }

    private static String getRepObjectSpecFromSelector(PlasticTool tool, String selectorText)
            throws IOException, InterruptedException {
        Path tempFile = null;
        BufferedWriter out = null;
        try {
            tempFile = Files.createTempFile(UUID.randomUUID().toString(), ".tmp");

            out = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8);
            out.write(selectorText);
            out.close();
            out = null;

            WorkspaceInfo workspaceInfo = getSelectorSpec(tool, tempFile.toString());
            return workspaceInfo.getRepObjectSpec();
        } catch (AbortException e) {
            LOGGER.severe(e.getMessage());
            throw new AbortException("An error was detected while getting the selector spec. " +
                    "This usually happens with old versions of 'cm'. " +
                    "Please ensure the latest version of 'cm' is installed.");
        } catch (ParseException e) {
            LOGGER.severe(e.getMessage());
            throw new AbortException("The selector is not valid.");
        } finally {
            if (out != null) {
                out.close();
            }

            if (tempFile != null) {
                Files.delete(tempFile);
            }
        }
    }

    private static String getWorkspaceNameFromScriptPath(String scriptPath) {
        int separatorIndex = scriptPath.indexOf(SEPARATOR);

        if (separatorIndex == -1) {
            return null;
        }

        return scriptPath.substring(0, separatorIndex).trim();
    }

    private static String getServerFileFromScriptPath(String scriptPath) {
        int separatorIndex = scriptPath.indexOf(SEPARATOR);

        if (separatorIndex == -1) {
            return null;
        }

        return scriptPath.substring(separatorIndex).trim();
    }

    private static String ensureScripPathStartsBySlash(String scriptPath) {
        return scriptPath.startsWith(SEPARATOR) ? scriptPath : SEPARATOR + scriptPath;
    }

    private static void getFile(PlasticTool tool, String revSpec, String filePath)
            throws IOException, InterruptedException {
        GetFileCommand command = new GetFileCommand(revSpec, filePath);
        CommandRunner.execute(tool, command).close();
    }

    private static WorkspaceInfo getSelectorSpec(PlasticTool tool, String filePath)
            throws IOException, ParseException, InterruptedException {
        GetSelectorSpecCommand command = new GetSelectorSpecCommand(filePath);
        return CommandRunner.executeAndRead(tool, command, command);
    }

    @Nonnull
    @Override
    protected SCMFile newChild(String name, boolean assumeIsDirectory) {
        return new PlasticSCMFile(fs, this, name, assumeIsDirectory);
    }

    @Nonnull
    @Override
    public Iterable<SCMFile> children() {
        return new ArrayList<SCMFile>();
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Nonnull
    @Override
    protected Type type() {
        return isDir ? Type.DIRECTORY : Type.REGULAR_FILE;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        PlasticSCM.WorkspaceInfo workspaceInfo = fs.getSCM().getFirstWorkspace();
        String serverFile = getPath();

        if (fs.getSCM().isUseMultipleWorkspaces()) {
            String workspaceName = getWorkspaceNameFromScriptPath(getPath());
            serverFile = getServerFileFromScriptPath(getPath());

            if (workspaceName == null || serverFile == null) {
                throw new FileNotFoundException("The pipeline script path is not valid. " +
                        "Maybe you didn't use '/' as directory separator.");
            }
        }

        Launcher launcher = fs.getLauncher();
        PlasticTool tool = new PlasticTool(
                fs.getSCM().getDescriptor().getCmExecutable(),
                launcher,
                launcher.getListener(),
                null);

        try {
            String resolvedSelector = SelectorParametersResolver.resolve(
                    workspaceInfo.getSelector(),
                    getLastBuildParameters(fs));

            String repObjectSpec = getRepObjectSpecFromSelector(
                    tool, resolvedSelector);

            serverFile = ensureScripPathStartsBySlash(serverFile);

            return getFileContent(tool, serverFile, repObjectSpec);
        } catch (AbortException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
