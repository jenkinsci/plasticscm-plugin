package com.codicesoftware.plugins.jenkins.mergebot;

import com.codicesoftware.plugins.hudson.ChangeSetReader;
import com.codicesoftware.plugins.hudson.ChangeSetWriter;
import com.codicesoftware.plugins.hudson.ClientConfigurationArguments;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.WorkspaceManager;
import com.codicesoftware.plugins.hudson.model.BuildData;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.FormFiller;
import com.codicesoftware.plugins.jenkins.AbortExceptionBuilder;
import com.codicesoftware.plugins.jenkins.BuildNode;
import com.codicesoftware.plugins.jenkins.ChangesetDetails;
import com.codicesoftware.plugins.jenkins.CredentialsFinder;
import com.codicesoftware.plugins.jenkins.UpdateToSpec;
import com.codicesoftware.plugins.jenkins.tools.CmTool;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MergebotScm extends SCM {

    private static final Logger LOGGER = Logger.getLogger(MergebotScm.class.getName());

    public static final String UPDATE_TO_SPEC_PARAMETER_NAME = "PLASTICSCM_MERGEBOT_UPDATE_SPEC";

    private final WorkingMode workingMode;
    private final String credentialsId;
    private final CleanupMethod cleanup;
    private final String specAttributeName;

    @DataBoundConstructor
    public MergebotScm(
            @Nonnull final CleanupMethod cleanup,
            @Nonnull final WorkingMode workingMode,
            @Nonnull final String credentialsId,
            @Nonnull final String specAttributeName) {
        this.cleanup = cleanup;
        this.workingMode = workingMode;
        this.credentialsId = credentialsId;
        this.specAttributeName = specAttributeName;
    }

    @Exported
    public WorkingMode getWorkingMode() {
        return workingMode;
    }

    @Exported
    public String getCredentialsId() {
        return credentialsId;
    }

    @Exported
    public CleanupMethod getCleanup() {
        return cleanup;
    }

    @Exported
    public String getSpecAttributeName() {
        return specAttributeName;
    }

    @NonNull
    @Override
    public String getKey() {
        return String.format(
            "Mergebot - Plastic SCM (cleanup: %s, workingMode: %s, credentialsId: %s, specAttributeName: %s)",
            cleanup,
            workingMode,
            credentialsId,
            specAttributeName);
    }

    @Override
    public boolean supportsPolling() {
        return false;
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeSetReader();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void checkout(
            @Nonnull final Run<?, ?> run,
            @Nonnull final Launcher launcher,
            @Nonnull final FilePath workspace,
            @Nonnull final TaskListener listener,
            @CheckForNull final File changelogFile,
            @CheckForNull final SCMRevisionState baseline) throws IOException, InterruptedException {

        Node node = BuildNode.getFromWorkspacePath(workspace);

        String updateToSpecString = run.getEnvironment(listener).get(specAttributeName);

        UpdateToSpec updateToSpec = UpdateToSpec.parse(updateToSpecString);
        if (updateToSpec == null) {
            throw new AbortException("Invalid update spec: " + updateToSpecString);
        }

        PlasticTool tool = new PlasticTool(
                CmTool.get(node, run.getEnvironment(listener), listener),
                launcher,
                listener,
                workspace,
                buildClientConfigurationArguments(run, updateToSpec.getRepServer()));

        Workspace plasticWorkspace = WorkspaceManager.prepare(
                tool, listener, workspace, cleanup);

        WorkspaceManager.switchTo(tool, plasticWorkspace.getPath(), updateToSpec);

        ChangeSet cset = ChangesetDetails.forWorkspace(tool, workspace, listener);

        BuildData buildData = new BuildData(plasticWorkspace, cset);
        List<BuildData> actions = run.getActions(BuildData.class);
        if (!actions.isEmpty()) {
            buildData.setIndex(actions.size() + 1);
        }

        run.addAction(buildData);

        if (changelogFile != null) {
            writeChangeLog(listener, changelogFile, cset);
        }
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(
            @Nonnull final Run<?, ?> run,
            @Nullable final FilePath wkPath,
            @Nullable final Launcher launcher,
            @Nonnull final TaskListener listener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    @Nonnull
    public ClientConfigurationArguments buildClientConfigurationArguments(
            @Nonnull final Run<?, ?> run,
            @Nonnull final String repServer) {
        return new ClientConfigurationArguments(
                workingMode, CredentialsFinder.getFromId(credentialsId, run.getParent()), repServer);
    }

    private void writeChangeLog(
            @Nonnull final TaskListener listener,
            @Nonnull final File changelogFile,
            @Nonnull final ChangeSet buildObject) throws AbortException {
        try {
            ChangeSetWriter.write(new ArrayList<ChangeSet>() {{ add(buildObject); }}, changelogFile);
        } catch (Exception e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        }
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<MergebotScm> {
        public DescriptorImpl() {
            super(MergebotScm.class, null);
            load();
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Mergebot - Plastic SCM";
        }

        @Override
        public boolean isApplicable(Job project) {
            return true;
        }

        public static String getDefaultSpecAttributeName() {
            return MergebotScm.UPDATE_TO_SPEC_PARAMETER_NAME;
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            return FormFiller.doFillCredentialsIdItems(item, credentialsId);
        }

        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        @POST
        public static FormValidation doCheckSpeckAttributeName(@QueryParameter String value) {
            return Util.fixEmpty(value) == null
                    ? FormValidation.error("The attribute name cannot be empty")
                    : FormValidation.ok();
        }
    }
}
