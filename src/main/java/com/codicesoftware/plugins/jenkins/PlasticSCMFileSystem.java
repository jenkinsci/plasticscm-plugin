package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticSCM;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.LogTaskListener;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.impl.SingleSCMSource;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
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
            if (source instanceof SingleSCMSource) {
                return isPlasticSCM(((SingleSCMSource) source).getScm());
            }
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
