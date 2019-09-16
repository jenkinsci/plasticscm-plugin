package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.util.FormChecker;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nonnull;

public class PlasticSCMStep extends SCMStep {

    public static final String SELECTOR_BRANCH_FORMAT = "repository \"%s@%s\"%n  path \"/\"%n    smartbranch \"%s\"";
    public static final String SELECTOR_CHANGESET_FORMAT = "repository \"%s@%s\"%n  path \"/\"%n    smartbranch \"%s\" changeset \"%s\"";

    private String branch = DescriptorImpl.defaultBranch;
    private String changeset = "";
    private String repository = "";
    private String server = "";
    private boolean useUpdate = true;
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

    public boolean isUseUpdate() {
        return useUpdate;
    }

    @DataBoundSetter
    public void setUseUpdate(boolean useUpdate) {
        this.useUpdate = useUpdate;
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
            buildSelector(), useUpdate, false, null, directory);
    }

    private String buildSelector() {
        if (Util.fixEmptyAndTrim(changeset) == null) {
            return String.format(SELECTOR_BRANCH_FORMAT, repository, server, branch);
        } else {
            return String.format(SELECTOR_CHANGESET_FORMAT, repository, server, branch, changeset);
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
    }
}
