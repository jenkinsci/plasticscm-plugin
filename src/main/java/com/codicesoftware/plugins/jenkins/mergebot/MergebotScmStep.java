package com.codicesoftware.plugins.jenkins.mergebot;

import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import com.codicesoftware.plugins.hudson.util.FormFiller;
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
import org.kohsuke.stapler.verb.POST;

import javax.annotation.Nonnull;

public class MergebotScmStep extends SCMStep {

    private WorkingMode workingMode = WorkingMode.NONE;
    private String credentialsId = null;
    private CleanupMethod cleanup = CleanupMethod.STANDARD;
    private String specAttributeName = MergebotScm.UPDATE_TO_SPEC_PARAMETER_NAME;

    @DataBoundConstructor
    public MergebotScmStep() {
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

    public String getSpecAttributeName() {
        return specAttributeName;
    }

    @DataBoundSetter
    public void setSpecAttributeName(String specAttributeName) {
        this.specAttributeName = specAttributeName;
    }

    @Nonnull
    @Override
    protected SCM createSCM() {
        return new MergebotScm(cleanup, workingMode, credentialsId, specAttributeName);
    }

    @Extension
    public static final class MergebotScmStepDescriptor extends SCMStep.SCMStepDescriptor {
        @Override
        @Nonnull
        public String getFunctionName() {
            return "mergebotCheckout";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Plastic SCM Mergebot Checkout";
        }

        @POST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            return FormFiller.doFillCredentialsIdItems(item, credentialsId);
        }

        public static String getDefaultSpecAttributeName() {
            return MergebotScm.UPDATE_TO_SPEC_PARAMETER_NAME;
        }

        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        @POST
        public static FormValidation doCheckSpecAttributeName(@QueryParameter String value) {
            return Util.fixEmpty(value) == null
                ? FormValidation.error("The attribute name cannot be empty")
                : FormValidation.ok();
        }
    }
}
