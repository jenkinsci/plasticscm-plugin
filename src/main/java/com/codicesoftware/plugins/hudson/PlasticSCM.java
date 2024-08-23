package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.commands.ChangesetRangeLogCommand;
import com.codicesoftware.plugins.hudson.commands.ChangesetsRetriever;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.FindChangesetCommand;
import com.codicesoftware.plugins.hudson.commands.ParseableCommand;
import com.codicesoftware.plugins.hudson.model.BuildData;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.FormChecker;
import com.codicesoftware.plugins.hudson.util.FormFiller;
import com.codicesoftware.plugins.hudson.util.SelectorParametersResolver;
import com.codicesoftware.plugins.jenkins.AbortExceptionBuilder;
import com.codicesoftware.plugins.jenkins.BuildNode;
import com.codicesoftware.plugins.jenkins.ChangesetDetails;
import com.codicesoftware.plugins.jenkins.CredentialsFinder;
import com.codicesoftware.plugins.jenkins.SelectorTemplates;
import com.codicesoftware.plugins.jenkins.ObjectSpecType;
import com.codicesoftware.plugins.jenkins.tools.CmTool;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.scm.PollingResult.BUILD_NOW;
import static hudson.scm.PollingResult.NO_CHANGES;

/**
 * SCM for Plastic SCM
 */
public class PlasticSCM extends SCM {

    // region Constants

    private static final Logger LOGGER = Logger.getLogger(PlasticSCM.class.getName());

    private static final String PLASTIC_ENV_PREFIX = "PLASTICSCM_";
    private static final String CHANGESET_ID = "CHANGESET_ID";
    private static final String CHANGESET_GUID = "CHANGESET_GUID";
    private static final String BRANCH = "BRANCH";
    private static final String AUTHOR = "AUTHOR";
    private static final String REPSPEC = "REPSPEC";

    public static final String DEFAULT_BRANCH = "/main";

    private static final Pattern BRANCH_PATTERN = Pattern.compile(
            "^.*(smart)?br(anch)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
            "^.*rep(ository)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    // When the controller runs commands that require a workspace folder, it uses this folder (relative to the Jenkins
    // root). It will be created when necessary.
    private static final String CONTROLLER_WORKSPACE_FOLDER = "plastic";

    // endregion

    // region Members

    private final String selector;

    private CleanupMethod cleanup;
    private final WorkingMode workingMode;
    @CheckForNull
    private final String credentialsId;
    @Deprecated
    private transient boolean useUpdate;

    private final List<WorkspaceInfo> additionalWorkspaces;
    private final WorkspaceInfo firstWorkspace;

    private final boolean pollOnController;
    private final String directory;
    private final boolean useWorkspaceSubdirectory;

    // endregion

    @DataBoundConstructor
    public PlasticSCM(
            String selector,
            CleanupMethod cleanup,
            WorkingMode workingMode,
            String credentialsId,
            boolean useMultipleWorkspaces,
            List<WorkspaceInfo> additionalWorkspaces,
            boolean pollOnController,
            String directory) {
        LOGGER.info("Initializing Plastic SCM plugin");
        this.selector = selector;
        this.cleanup = cleanup;
        this.workingMode = workingMode;
        this.useWorkspaceSubdirectory = useMultipleWorkspaces;
        this.pollOnController = pollOnController;
        this.directory = directory;
        this.credentialsId = credentialsId;

        firstWorkspace = new WorkspaceInfo(this.selector, this.cleanup, this.directory);
        if (additionalWorkspaces == null || !useMultipleWorkspaces) {
            this.additionalWorkspaces = null;
            return;
        }
        this.additionalWorkspaces = additionalWorkspaces;
    }

    // region Bean getters & setters

    @Exported
    public String getSelector() {
        return selector;
    }

    @Exported
    public CleanupMethod getCleanup() {
        // Field might be null if deserialized from older class version.
        return (cleanup != null) ? cleanup : CleanupMethod.convertUseUpdate(useUpdate);
    }

    @Exported
    public WorkingMode getWorkingMode() {
        // Field might be null if deserialized from older class version.
        return (workingMode != null) ? workingMode : WorkingMode.NONE;
    }

    @Exported
    public String getCredentialsId() {
        return credentialsId;
    }

    @Exported
    public boolean isUseMultipleWorkspaces() {
        return useWorkspaceSubdirectory;
    }

    @Exported
    @SuppressWarnings("unused")
    public List<WorkspaceInfo> getAdditionalWorkspaces() {
        return additionalWorkspaces;
    }

    @Exported
    public WorkspaceInfo getFirstWorkspace() {
        return firstWorkspace;
    }

    @Exported
    public String getDirectory() {
        return directory;
    }

    @Exported
    @SuppressWarnings("unused")
    public boolean isPollOnController() {
        return pollOnController;
    }

    // endregion

    // region Overridden from parent class

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String getKey() {
        StringBuilder builder = new StringBuilder("Plastic SCM");
        for (WorkspaceInfo workspace : getAllWorkspaces()) {
            builder.append(" ");
            builder.append(Util.fixNull(workspace.getSelector()).replaceAll("\\s+", " "));
        }
        return builder.toString();
    }

    @Override
    @CheckForNull
    public RepositoryBrowser<?> guessBrowser() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeSetReader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkout(
            @Nonnull final Run<?, ?> run,
            @Nonnull final Launcher launcher,
            @Nonnull final FilePath workspace,
            @Nonnull final TaskListener listener,
            @CheckForNull final File changelogFile,
            @CheckForNull final SCMRevisionState baseline) throws IOException, InterruptedException {

        Node node = BuildNode.getFromWorkspacePath(workspace);
        adjustFieldsIfUsingOldConfigFormat();

        List<ChangeSet> changeLogItems = new ArrayList<>();

        ParametersAction parameters = run.getAction(ParametersAction.class);
        List<ParameterValue> parameterValues = (parameters == null)
            ? Collections.emptyList()
            : parameters.getParameters();

        EnvVars environment = run.getEnvironment(listener);

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces()) {

            FilePath plasticWorkspacePath = resolveWorkspacePath(workspace, workspaceInfo);
            String resolvedSelector = SelectorParametersResolver.resolve(
                workspaceInfo.getSelector(), parameterValues, environment);

            if (!plasticWorkspacePath.exists()) {
                plasticWorkspacePath.mkdirs();
            }

            PlasticTool tool = new PlasticTool(
                CmTool.get(node, run.getEnvironment(listener), listener),
                launcher,
                listener,
                plasticWorkspacePath,
                buildClientConfigurationArguments(run.getParent(), resolvedSelector));

            Workspace plasticWorkspace = WorkspaceManager.prepare(
                tool, listener, plasticWorkspacePath, workspaceInfo.getCleanup());

            WorkspaceManager.setSelector(tool, plasticWorkspace.getPath(), resolvedSelector);

            ChangeSet cset = ChangesetDetails.forWorkspace(tool, plasticWorkspacePath, listener);

            ChangeSet previousCset = retrieveLastBuiltChangeset(tool, run,  plasticWorkspacePath, cset);
            changeLogItems.addAll(retrieveMultipleChangesetDetails(
                tool, plasticWorkspacePath, listener, previousCset, cset));

            BuildData buildData = new BuildData(plasticWorkspace, cset);
            List<BuildData> actions = run.getActions(BuildData.class);
            if (!actions.isEmpty()) {
                buildData.setIndex(actions.size() + 1);
            }
            run.addAction(buildData);
        }

        if (changelogFile != null) {
            writeChangeLog(listener, changelogFile, changeLogItems);
        }
    }

    @Override
    public void buildEnvironment(@Nonnull Run<?, ?> build, @Nonnull Map<String, String> env) {
        int index = 1;
        for (BuildData buildData : build.getActions(BuildData.class)) {
            ChangeSet cset = buildData.getChangeset();
            if (cset != null) {
                populateEnvironmentVariables(cset, env, PLASTIC_ENV_PREFIX);
                if (additionalWorkspaces != null) {
                    populateEnvironmentVariables(cset, env, PLASTIC_ENV_PREFIX + index + "_");
                    index++;
                }
            } else {
                LOGGER.warning("Unable to populate environment variables");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SCMRevisionState calcRevisionsFromBuild(
            @Nonnull final Run<?, ?> run,
            @Nullable final FilePath wkPath,
            @Nullable final Launcher launcher,
            @Nonnull final TaskListener listener) {
        return SCMRevisionState.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PollingResult compareRemoteRevisionWith(
            @Nonnull final Job<?, ?> project,
            @Nullable final Launcher launcher,
            @Nullable final FilePath workspace,
            @Nonnull final TaskListener listener,
            @Nonnull final SCMRevisionState baseline) throws IOException, InterruptedException {
        if (project.getLastBuild() == null) {
            listener.getLogger().println("No builds detected yet!");
            return BUILD_NOW;
        }

        List<ParameterValue> parameters = getDefaultParameterValues(project);
        Run<?, ?> lastBuild = project.getLastBuild();

        EnvVars environment = lastBuild.getEnvironment(listener);

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces()) {
            FilePath plasticWorkspacePath = resolveWorkspacePath(workspace, workspaceInfo);
            String resolvedSelector = SelectorParametersResolver.resolve(
                workspaceInfo.getSelector(), parameters, environment);

            boolean hasChanges = hasChanges(
                project,
                    launcher,
                    plasticWorkspacePath,
                    listener,
                    lastBuild.getTimestamp(),
                resolvedSelector);

            if (hasChanges) {
                return BUILD_NOW;
            }
        }
        return NO_CHANGES;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Nonnull
    public ClientConfigurationArguments buildClientConfigurationArguments(
            @Nullable Item item, @CheckForNull String selector) {
        return new ClientConfigurationArguments(
            workingMode,
            CredentialsFinder.getFromId(credentialsId, item),
            getServerFromSelector(selector));
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return !pollOnController;
    }

    // endregion

    // region Public methods

    public List<WorkspaceInfo> getAllWorkspaces() {
        List<WorkspaceInfo> result = new ArrayList<>();
        result.add(firstWorkspace);
        if (additionalWorkspaces != null) {
            result.addAll(additionalWorkspaces);
        }
        return result;
    }

    // endregion

    // region Private methods

    /**
     * Backward compatibility for jobs using old configuration format.
     */
    private void adjustFieldsIfUsingOldConfigFormat() {
        if (cleanup == null) {
            LOGGER.warning("Missing 'cleanup' field. Update job configuration.");
            cleanup = CleanupMethod.convertUseUpdate(useUpdate);
        }
    }

    private static FilePath resolveWorkspacePath(
            FilePath jenkinsWorkspacePath,
            WorkspaceInfo workspaceInfo) {
        if (jenkinsWorkspacePath == null || workspaceInfo == null) {
            return null;
        }
        String subdirectory = workspaceInfo.getDirectory();
        if (Util.fixEmpty(subdirectory) == null) {
            return jenkinsWorkspacePath;
        }
        return new FilePath(jenkinsWorkspacePath, workspaceInfo.getDirectory());
    }

    /**
     * Finds changeset of the last completed build for the same branch as the given changeset.
     * Returns null if not found or is newer than the current build.
     */
    private static ChangeSet retrieveLastBuiltChangeset(
            PlasticTool tool, Run<?, ?> build, FilePath workspacePath, ChangeSet cset) {
        if (cset == null
                || cset.getType() == ObjectSpecType.Shelve
                || Util.fixEmpty(cset.getBranch()) == null
                || Util.fixEmpty(cset.getRepoName()) == null
                || Util.fixEmpty(cset.getRepoServer()) == null) {
            return null;
        }

        while (build != null) {
            for (BuildData buildData : build.getActions(BuildData.class)) {
                ChangeSet oldCset = buildData.getChangeset();
                if (oldCset == null) {
                    continue;
                }

                if (!cset.getBranch().equals(oldCset.getBranch()) ||
                        !cset.getRepoName().equals(oldCset.getRepoName()) ||
                        !cset.getRepoServer().equals(oldCset.getRepoServer())) {
                    continue;
                }

                int oldCsetId = oldCset.getId();
                if (oldCsetId <= 0 || oldCsetId >= cset.getId()) {
                    return null;
                }

                if (isExistingChangeset(tool, workspacePath, oldCset)) {
                    return oldCset;
                }
            }
            build = build.getPreviousCompletedBuild();
        }
        return null;
    }

    private static boolean isExistingChangeset(PlasticTool tool, FilePath workspacePath, ChangeSet cset) {
        FilePath xmlOutputPath = null;
        try {
            xmlOutputPath = OutputTempFile.getPathForXml(workspacePath);
            ParseableCommand<ChangeSet> command = new FindChangesetCommand(cset.getObjectSpec(), xmlOutputPath);
            return CommandRunner.executeAndRead(tool, command) != null;
        } catch (Exception e) {
            LOGGER.log(
                Level.WARNING,
                String.format(
                    "Unable to determine whether cset cs:%d@%s@%s exists: %s",
                    cset.getId(),
                    cset.getBranch(),
                    cset.getRepository(),
                    e.getMessage()),
                e);
            return false;
        } finally {
            if (xmlOutputPath != null) {
                OutputTempFile.safeDelete(xmlOutputPath);
            }
        }
    }

    private static List<ChangeSet> retrieveMultipleChangesetDetails(
            @Nonnull final PlasticTool tool,
            @Nonnull final FilePath workspacePath,
            @Nonnull final TaskListener listener,
            @CheckForNull final ChangeSet fromCset,
            @Nonnull final ChangeSet toCset) throws IOException, InterruptedException {

        if (fromCset == null) {
            return new ArrayList<>() {{ add(toCset); }};
        }

        FilePath xmlOutputPath = OutputTempFile.getPathForXml(workspacePath);
        try {
            ParseableCommand<List<ChangeSet>> command = new ChangesetRangeLogCommand(fromCset, toCset, xmlOutputPath);
            List<ChangeSet> csets = CommandRunner.executeAndRead(tool, command, false);
            for (ChangeSet cset : csets) {
                cset.setRepoName(toCset.getRepoName());
                cset.setRepoServer(toCset.getRepoServer());
            }
            return csets;
        } catch (ParseException e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        } finally {
            OutputTempFile.safeDelete(xmlOutputPath);
        }
    }

    private void writeChangeLog(
            TaskListener listener,
            File changelogFile,
            List<ChangeSet> result) throws AbortException {
        try {
            ChangeSetWriter.write(result, changelogFile);
        } catch (Exception e) {
            throw AbortExceptionBuilder.build(LOGGER, listener, e);
        }
    }

    private boolean hasChanges(
            @Nullable Item item,
            @CheckForNull Launcher launcher,
            @CheckForNull FilePath workspacePath,
            @Nonnull TaskListener listener,
            @Nonnull Calendar lastCompletedBuildTimestamp,
            @CheckForNull String selector) throws IOException, InterruptedException {

        // hasChanges() can be invoked on the master, without any workspace
        // We will provide the plugin with a LocalLauncher and a fake workspace, since:
        // - PlasticTool needs a launcher and a workspace
        // - getChanges() needs a place to put the temp file where it captures PlasticTool's output
        if (launcher == null) {
            launcher = new Launcher.LocalLauncher(listener);
        }

        if (workspacePath == null) {
            workspacePath = new FilePath(new FilePath(Jenkins.get().getRootDir()), CONTROLLER_WORKSPACE_FOLDER);
            workspacePath.mkdirs();
        }

        String repSpec = getRepSpecFromSelector(selector);

        PlasticTool plasticTool = new PlasticTool(
            CmTool.get(BuildNode.getFromWorkspacePath(workspacePath), new EnvVars(EnvVars.masterEnvVars), listener),
            launcher,
            listener,
            workspacePath,
            buildClientConfigurationArguments(item, selector));
        try {
            List<ChangeSet> changesetsFromBuild = ChangesetsRetriever.getChangesets(
                plasticTool,
                workspacePath,
                getBranchFromSelector(selector),
                repSpec,
                lastCompletedBuildTimestamp,
                Calendar.getInstance());
            return !changesetsFromBuild.isEmpty();
        } catch (Exception e) {
            e.printStackTrace(listener.error(String.format(
                "%s: Unable to retrieve workspace status.", workspacePath.getRemote())));
            return false;
        }
    }

    private static List<ParameterValue> getDefaultParameterValues(Job<?, ?> project) {
        ParametersDefinitionProperty paramDefProp = project.getProperty(ParametersDefinitionProperty.class);
        if (paramDefProp == null) {
            return null;
        }

        ArrayList<ParameterValue> result = new ArrayList<>();
        for (ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
            ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

            if (defaultValue != null) {
                result.add(defaultValue);
            }
        }
        return result;
    }

    @CheckForNull
    private static String getBranchFromSelector(@CheckForNull String selector) {
        if (selector == null) {
            return null;
        }

        Matcher smartbranchMatcher = BRANCH_PATTERN.matcher(selector);
        if (smartbranchMatcher.matches()) {
            return smartbranchMatcher.group(3);
        }
        return null;
    }

    @CheckForNull
    private static String getRepSpecFromSelector(@CheckForNull String selector) {
        if (selector == null) {
            return null;
        }

        Matcher matcher = REPOSITORY_PATTERN.matcher(selector);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    @CheckForNull
    private static String getServerFromSelector(@CheckForNull String selector) {
        return getServerFromRepositorySpec(getRepSpecFromSelector(selector));
    }

    @CheckForNull
    private static String getServerFromRepositorySpec(@CheckForNull String repSpec) {
        String repository = Util.fixEmpty(repSpec);

        if (repository == null) {
            return null;
        }

        int atSignIndex = repository.indexOf('@');
        if (atSignIndex == -1) {
            return null;
        }

        return repository.substring(atSignIndex + 1);

    }

    private void populateEnvironmentVariables(
            @Nonnull final ChangeSet cset,
            @Nonnull final Map<String, String> environment,
            @CheckForNull final String prefix) {
        environment.put(prefix + CHANGESET_ID, cset.getVersion());
        environment.put(prefix + CHANGESET_GUID, cset.getGuid());
        environment.put(prefix + BRANCH, cset.getBranch());
        environment.put(prefix + AUTHOR, cset.getUser());
        environment.put(prefix + REPSPEC, cset.getRepository());
    }

    // endregion

    @SuppressWarnings("unused")
    @Extension
    public static class DescriptorImpl extends SCMDescriptor<PlasticSCM> {
        public DescriptorImpl() {
            super(PlasticSCM.class, null);
            load();
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Plastic SCM";
        }

        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        @POST
        public static FormValidation doCheckSelector(@QueryParameter String value) {
            return FormChecker.doCheckSelector(value);
        }

        @POST
        public static FormValidation doCheckDirectory(
                @QueryParameter String value,
                @QueryParameter boolean useMultipleWorkspaces,
                @AncestorInPath Item item) {
            if (Util.fixEmpty(value) == null && !useMultipleWorkspaces) {
                return FormValidation.ok();
            }
            return FormChecker.doCheckDirectory(value, item);
        }

        public static String getDefaultSelector() {
            return SelectorTemplates.DEFAULT;
        }

        @POST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            return FormFiller.doFillCredentialsIdItems(item, credentialsId);
        }

        @POST
        public FormValidation doCheckCredentialsId(
            @AncestorInPath Item item,
            @QueryParameter String value,
            @QueryParameter String selector,
            @QueryParameter WorkingMode workingMode
        ) throws IOException, InterruptedException {
            return FormChecker.doCheckCredentialsId(
                item,
                value,
                getServerFromSelector(selector),
                workingMode);
        }

        @Override
        public boolean isApplicable(Job project) {
            return true;
        }
    }

    @ExportedBean
    public static final class WorkspaceInfo extends AbstractDescribableImpl<WorkspaceInfo> implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String selector;

        private final CleanupMethod cleanup;
        @Deprecated
        private transient boolean useUpdate;

        private final String directory;

        @DataBoundConstructor
        public WorkspaceInfo(String selector, CleanupMethod cleanup, String directory) {
            this.selector = selector;
            this.cleanup = cleanup;
            this.directory = directory;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        @Exported
        public String getSelector() {
            return selector;
        }

        @Exported
        public CleanupMethod getCleanup() {
            // Field might be null if deserialized from older class version.
            return (cleanup != null) ? cleanup : CleanupMethod.convertUseUpdate(useUpdate);
        }

        @Exported
        public String getDirectory() {
            return directory;
        }

        @SuppressWarnings("unused")
        @Extension
        public static class DescriptorImpl extends Descriptor<WorkspaceInfo> {

            @SuppressWarnings("lgtm[jenkins/no-permission-check]")
            @POST
            public static FormValidation doCheckSelector(@QueryParameter String value) {
                return FormChecker.doCheckSelector(value);
            }

            @POST
            public static FormValidation doCheckDirectory(@QueryParameter String value, @AncestorInPath Item item) {
                return FormChecker.doCheckDirectory(value, item);
            }

            public static String getDefaultSelector() {
                return SelectorTemplates.DEFAULT;
            }

            @Override
            @Nonnull
            public String getDisplayName() {
                return "Plastic SCM Workspace";
            }
        }
    }

    private static class GetCurrentNode extends MasterToSlaveCallable<String, InterruptedException> {
        private static final long serialVersionUID = 1L;

        @Override
        public String call() {
            Node node = Computer.currentComputer().getNode();
            if (node == null) {
                return null;
            }
            return node.getNodeName();
        }
    }
}
