package com.codicesoftware.plugins.jenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.util.LogTaskListener;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import com.codicesoftware.plugins.hudson.PlasticSCM;

public class PlasticSCMFileSystem extends SCMFileSystem {

    protected PlasticSCMFileSystem(@Nonnull Item owner, @Nonnull PlasticSCM scm, @CheckForNull SCMRevision rev) {
        super(rev);
        this.scm = scm;
        this.launcher = new Launcher.LocalLauncher(
            new LogTaskListener(LOGGER, Level.ALL));
    }

    public PlasticSCM getSCM() {
        return scm;
    }

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

        @Override
        public boolean supports(SCM source) {
            return isPlasticSCM(source);
        }

        @Override
        public boolean supports(SCMSource source) {
            return false;
        }

        @Override
        public SCMFileSystem build(
            @Nonnull Item owner,
            @Nonnull SCM scm,
            @CheckForNull SCMRevision rev){
            if (scm == null)
                return null;

             if (!isPlasticSCM(scm))
                 return null;

            return new PlasticSCMFileSystem(owner, (PlasticSCM)scm, rev);
        }

        private static boolean isPlasticSCM(SCM scm){
            return scm instanceof PlasticSCM;
        }
    }

    private Launcher launcher;
    private final PlasticSCM scm;

    private static final Logger LOGGER = Logger.getLogger(PlasticSCMFileSystem.class.getName());
}
