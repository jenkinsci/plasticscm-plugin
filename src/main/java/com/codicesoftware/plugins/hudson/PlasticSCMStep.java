package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.util.FormChecker;
import hudson.Extension;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class PlasticSCMStep extends SCMStep {

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

    public String getWorkspaceName() {
        return useMultipleWorkspaces ? workspaceName : "";
    }

    @DataBoundSetter
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public boolean isUseMultipleWorkspaces() {
        return useMultipleWorkspaces;
    }

    @DataBoundSetter
    public void setUseMultipleWorkspaces(boolean useMultipleWorkspaces) {
        this.useMultipleWorkspaces = useMultipleWorkspaces;
    }

    @Nonnull
    @Override
    protected SCM createSCM() {
        return new PlasticSCM(
            buildSelector(), workspaceName, useUpdate, useMultipleWorkspaces, null);
    }

    String buildSelector() {
        return String.format(SELECTOR_FORMAT, repository, server, branch);
    }

    private String branch = PlasticStepDescriptor.defaultBranch;
    private String repository = PlasticStepDescriptor.defaultRepository;
    private String server = PlasticStepDescriptor.defaultServer;

    private boolean useUpdate = true;
    private boolean useMultipleWorkspaces = false;
    private String workspaceName = "";

    private static final String SELECTOR_FORMAT =
            "rep \"%s@%s\"\n  path \"/\"\n    smartbranch \"%s\"";

    @Extension
    public static final class PlasticStepDescriptor extends SCMStepDescriptor {
        public static final String defaultBranch = "/main";
        public static final String defaultRepository = "default";
        public static final String defaultServer = "localhost:8087";
        public static final String defaultWorkspaceName = "Jenkins-${JOB_NAME}-${NODE_NAME}";

        @Override
        public String getFunctionName() {
            return "cm";
        }

        @Override
        public String getDisplayName() {
            return "Plastic SCM";
        }

        public FormValidation doCheckWorkspaceName(@QueryParameter String value) {
            return FormChecker.doCheckWorkspaceName(value);
        }

        public FormValidation doCheckBranch(@QueryParameter String value) {
            return FormChecker.doCheckBranch(value);
        }

        public FormValidation doCheckRepository(@QueryParameter String value) {
            return FormChecker.doCheckRepository(value);
        }

        public FormValidation doCheckServer(@QueryParameter String value) {
            return FormChecker.doCheckServer(value);
        }
    }
}
