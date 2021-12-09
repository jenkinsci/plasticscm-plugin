package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticSCM;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.LogTaskListener;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlasticSCMFileSystem extends SCMFileSystem {

    private static final Logger LOGGER = Logger.getLogger(PlasticSCMFileSystem.class.getName());

    @Nonnull
    private final PlasticSCM scm;
    @Nonnull
    private final Item owner;
    @Nonnull
    private final Launcher launcher;

    protected PlasticSCMFileSystem(@Nonnull Item owner, @Nonnull PlasticSCM scm, @CheckForNull SCMRevision rev) {
        super(rev);
        this.owner = owner;
        this.scm = scm;
        this.launcher = new Launcher.LocalLauncher(new LogTaskListener(LOGGER, Level.ALL));
    }

    @CheckForNull
    public Run<?, ?> getLastBuildFromFirstJob() {
        Collection<? extends Job> jobs = owner.getAllJobs();
        for (Job job : jobs) {
            if (job == null) {
                continue;
            }
            Run<?, ?> run = job.getLastBuild();
            if (run != null) {
                return run;
            }
        }
        return null;
    }

    @Nonnull
    public Item getOwner() {
        return owner;
    }

    @Nonnull
    public PlasticSCM getSCM() {
        return scm;
    }

    @Nonnull
    public Launcher getLauncher() {
        return launcher;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    public SCMFile getRoot() {
        return new PlasticSCMFile(this);
    }

    @Extension
    public static class BuilderImpl extends SCMFileSystem.Builder {

        private static boolean isPlasticSCM(SCM scm) {
            return scm instanceof PlasticSCM;
        }

        @Override
        public SCMFileSystem build(@Nonnull Item owner,
                @Nonnull SCM scm,
                @CheckForNull SCMRevision rev) {
            if (scm == null) {
                return null;
            }

            if (!isPlasticSCM(scm)) {
                return null;
            }

            return new PlasticSCMFileSystem(owner, (PlasticSCM) scm, rev);
        }

        @Override
        public boolean supports(SCM source) {
            return isPlasticSCM(source);
        }

        @Override
        public boolean supports(SCMSource source) {
            return false;
        }

        @Override
        protected boolean supportsDescriptor(SCMDescriptor descriptor) {
            return descriptor instanceof PlasticSCM.DescriptorImpl;
        }

        @Override
        protected boolean supportsDescriptor(SCMSourceDescriptor descriptor) {
            return false;
        }

    }
}
