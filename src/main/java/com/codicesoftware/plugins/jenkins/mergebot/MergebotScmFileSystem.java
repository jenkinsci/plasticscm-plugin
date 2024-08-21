package com.codicesoftware.plugins.jenkins.mergebot;


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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MergebotScmFileSystem extends SCMFileSystem {

    private static final Logger LOGGER = Logger.getLogger(MergebotScmFileSystem.class.getName());

    @Nonnull
    private final MergebotScm scm;
    @Nonnull
    private final Item owner;
    @Nonnull
    private final Launcher launcher;

    protected MergebotScmFileSystem(
            @Nonnull Item owner,
            @Nonnull MergebotScm scm,
            @CheckForNull SCMRevision rev) {
        super(rev);
        this.owner = owner;
        this.scm = scm;
        this.launcher = new Launcher.LocalLauncher(new LogTaskListener(LOGGER, Level.ALL));
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    public SCMFile getRoot() {
        return new MergebotScmFile(this);
    }

    @Nonnull
    public Item getOwner() {
        return owner;
    }

    @Nonnull
    public MergebotScm getScm() {
        return scm;
    }

    @Nonnull
    public Launcher getLauncher() {
        return launcher;
    }

    @Extension
    public static class BuilderImpl extends SCMFileSystem.Builder {
        @Override
        public SCMFileSystem build(
                @Nonnull Item owner,
                @Nonnull SCM scm,
                @CheckForNull SCMRevision rev) {
            if (!isMergebotScm(scm)) {
                return null;
            }

            return new MergebotScmFileSystem(owner, (MergebotScm) scm, rev);
        }

        @Override
        public boolean supports(SCM source) {
            return isMergebotScm(source);
        }

        @Override
        public boolean supports(SCMSource source) {
            return false;
        }

        @Override
        protected boolean supportsDescriptor(SCMDescriptor scmDescriptor) {
            return scmDescriptor instanceof MergebotScm.DescriptorImpl;
        }

        @Override
        protected boolean supportsDescriptor(SCMSourceDescriptor scmSourceDescriptor) {
            return false;
        }

        private static boolean isMergebotScm(SCM scm) {
            return scm instanceof MergebotScm;
        }
    }
}
