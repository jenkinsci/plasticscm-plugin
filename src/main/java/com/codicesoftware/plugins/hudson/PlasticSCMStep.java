package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import com.codicesoftware.plugins.hudson.util.FormChecker;
import com.codicesoftware.plugins.hudson.util.FormFiller;
import com.codicesoftware.plugins.jenkins.SelectorTemplates;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

public class PlasticSCMStep extends SCMStep {

    private static final Logger LOGGER = Logger.getLogger(PlasticSCMStep.class.getName());

    private String branch = DescriptorImpl.defaultBranch;
    private String changeset = "";
    private String repository = "";
    private String server = "";

    private WorkingMode workingMode = WorkingMode.NONE;
    private String credentialsId = null;
    private CleanupMethod cleanup = CleanupMethod.STANDARD;
    @Deprecated
    private transient boolean useUpdate;

    private String directory = "";

    @DataBoundConstructor
    public PlasticSCMStep() {
    }

    public String getBranch() {
        return branch;
    }

    @DataBoundSetter
    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getChangeset() {
        return changeset;
    }

    @DataBoundSetter
    public void setChangeset(String changeset) {
        this.changeset = changeset;
    }

    public String getRepository() {
        return repository;
    }

    @DataBoundSetter
    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getServer() {
        return server;
    }

    @DataBoundSetter
    public void setServer(String server) {
        this.server = server;
    }

    public WorkingMode getWorkingMode() {
        return workingMode;
    }

    @DataBoundSetter
    public void setWorkingMode(WorkingMode workingMode) {
        this.workingMode = workingMode;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public CleanupMethod getCleanup() {
        return cleanup;
    }

    @DataBoundSetter
    public void setCleanup(CleanupMethod cleanup) {
        this.cleanup = cleanup;
    }

    @Deprecated
    public void setUseUpdate(boolean useUpdate) {
        LOGGER.warning("Using deprecated 'useUpdate' field. Update job configuration.");
        this.cleanup = CleanupMethod.convertUseUpdate(useUpdate);
    }

    public String getDirectory() {
        return directory;
    }

    @DataBoundSetter
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Nonnull
    @Override
    protected SCM createSCM() {
        return new PlasticSCM(
            buildSelector(), cleanup, workingMode, credentialsId, false, null, false, directory);
    }

    private String buildSelector() {
        if (Util.fixEmptyAndTrim(changeset) == null) {
            return String.format(SelectorTemplates.BRANCH, repository, server, branch);
        } else {
            return String.format(SelectorTemplates.CHANGESET, repository, server, branch, changeset);
        }
    }

    @Extension
    public static final class DescriptorImpl extends SCMStepDescriptor {
        public static final String defaultBranch = PlasticSCM.DEFAULT_BRANCH;

        @Override
        public String getFunctionName() {
            return "cm";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Plastic SCM";
        }

        @RequirePOST
        public FormValidation doCheckBranch(@QueryParameter String value) {
            return FormChecker.doCheckBranch(value);
        }

        @RequirePOST
        public FormValidation doCheckRepository(@QueryParameter String value) {
            return FormChecker.doCheckRepository(value);
        }

        @RequirePOST
        public FormValidation doCheckServer(@QueryParameter String value) {
            return FormChecker.doCheckServer(value);
        }

        @RequirePOST
        public static FormValidation doCheckDirectory(@QueryParameter String value, @AncestorInPath Item item) {
            if (Util.fixEmpty(value) == null) {
                return FormValidation.ok();
            }
            return FormChecker.doCheckDirectory(value, item);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            return FormFiller.doFillCredentialsIdItems(item, credentialsId);
        }

        @RequirePOST
        public FormValidation doCheckCredentialsId(
            @AncestorInPath Item item,
            @QueryParameter String value,
            @QueryParameter String server,
            @QueryParameter WorkingMode workingMode
        ) throws IOException, InterruptedException {
            return FormChecker.doCheckCredentialsId(
                item,
                value,
                server,
                workingMode);
        }
    }
}
