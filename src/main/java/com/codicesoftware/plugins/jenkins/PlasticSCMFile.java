package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticSCM;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.GetSelectorSpecCommand;
import com.codicesoftware.plugins.hudson.model.WorkspaceInfo;
import com.codicesoftware.plugins.hudson.util.SelectorParametersResolver;
import com.codicesoftware.plugins.hudson.util.StringUtil;
import com.codicesoftware.plugins.jenkins.tools.CmTool;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PlasticSCMFile extends SCMFile {

    private static final Logger LOGGER = Logger.getLogger(PlasticSCMFile.class.getName());

    @Nonnull
    private final PlasticSCMFileSystem fs;
    private final boolean isDir;

    public PlasticSCMFile(@Nonnull final PlasticSCMFileSystem fs) {
        this.fs = fs;
        this.isDir = true;
    }

    public PlasticSCMFile(
            @Nonnull final PlasticSCMFileSystem fs,
            @Nonnull final PlasticSCMFile parent,
            @Nonnull final String name,
            final boolean isDir) {
        super(parent, name);
        this.fs = fs;
        this.isDir = isDir;
    }

    @CheckForNull
    private static List<ParameterValue> getLastBuildParameters(@Nonnull final PlasticSCMFileSystem fs) {
        Run<?, ?> run = LastBuild.get(fs.getOwner());
        if (run == null) {
            return null;
        }

        ParametersAction parameters = run.getAction(ParametersAction.class);
        return parameters != null ? parameters.getParameters() : null;
    }

    @Nonnull
    private static String getRepObjectSpecFromSelector(
            @Nonnull final PlasticTool tool,
            @Nonnull final String selectorText) throws IOException, InterruptedException {
        Path tempFile = null;
        BufferedWriter out = null;
        try {
            tempFile = TempFile.create();

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

    private static String getWorkspaceNameFromScriptPath(@Nonnull final String scriptPath) {
        int separatorIndex = scriptPath.indexOf(StringUtil.SEPARATOR);

        if (separatorIndex == -1) {
            return null;
        }

        return scriptPath.substring(0, separatorIndex).trim();
    }

    private static String getServerFileFromScriptPath(@Nonnull final String scriptPath) {
        int separatorIndex = scriptPath.indexOf(StringUtil.SEPARATOR);

        if (separatorIndex == -1) {
            return null;
        }

        return scriptPath.substring(separatorIndex).trim();
    }

    private static WorkspaceInfo getSelectorSpec(
            @Nonnull final PlasticTool tool,
            @Nonnull final String filePath) throws IOException, ParseException, InterruptedException {
        GetSelectorSpecCommand command = new GetSelectorSpecCommand(filePath);
        return CommandRunner.executeAndRead(tool, command);
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull final String name, final boolean assumeIsDirectory) {
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
        TaskListener listener = launcher.getListener();
        Run<?, ?> build = LastBuild.get(fs.getOwner());

        EnvVars environment = new EnvVars(EnvVars.masterEnvVars);
        if (build != null) {
            environment = environment.overrideAll(build.getEnvironment(listener));
        }

        try {
            String resolvedSelector = SelectorParametersResolver.resolve(
                workspaceInfo.getSelector(),
                getLastBuildParameters(fs),
                environment);

            PlasticTool tool = new PlasticTool(
                CmTool.get(Jenkins.getInstance(), environment, listener),
                launcher,
                listener,
                null,
                    fs.getSCM().buildClientConfigurationArguments(fs.getOwner(), resolvedSelector));

            String repObjectSpec = getRepObjectSpecFromSelector(
                    tool, resolvedSelector);

            serverFile = StringUtil.ensureStartsWithSlash(serverFile);

            return FileContent.getFromServer(tool, serverFile, repObjectSpec);
        } catch (AbortException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
