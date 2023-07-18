package com.codicesoftware.plugins.jenkins.mergebot;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.util.StringUtil;
import com.codicesoftware.plugins.jenkins.FileContent;
import com.codicesoftware.plugins.jenkins.LastBuild;
import com.codicesoftware.plugins.jenkins.tools.CmTool;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMFile;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MergebotScmFile extends SCMFile {

    private final MergebotScmFileSystem fs;
    private final boolean isDir;

    public MergebotScmFile(@Nonnull final MergebotScmFileSystem fs) {
        this.fs = fs;
        this.isDir = true;
    }

    public MergebotScmFile(
            @Nonnull final MergebotScmFileSystem fs,
            @Nonnull final MergebotScmFile parent,
            @Nonnull final String name,
            final boolean isDir) {
        super(parent, name);
        this.fs = fs;
        this.isDir = isDir;
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull String name, boolean assumeIsDirectory) {
        return new MergebotScmFile(fs, this, name, assumeIsDirectory);
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
        Launcher launcher = fs.getLauncher();
        TaskListener taskListener = launcher.getListener();
        Run<?, ?> run = LastBuild.get(fs.getOwner());

        if (run == null) {
            throw new FileNotFoundException("No run found");
        }

        EnvVars environment = run.getEnvironment(taskListener);
        UpdateToSpec updateToSpec = UpdateToSpec.parse(environment.get(fs.getScm().getSpecAttributeName()));
        if (updateToSpec == null) {
            throw new FileNotFoundException("No update to spec found in environment variables");
        }

        try {
            PlasticTool tool = new PlasticTool(
                CmTool.get(Jenkins.getInstance(), environment, taskListener),
                launcher,
                taskListener,
                null,
                fs.getScm().buildClientConfigurationArguments(run, updateToSpec.getRepServer()));

            String serverFile = StringUtil.ensureStartsWithSlash(getPath());

            return FileContent.getFromServer(tool, serverFile, updateToSpec.getFullObjectSpec());
        } catch (AbortException e) {
            throw new FileNotFoundException(e.getMessage());

        }
    }
}
