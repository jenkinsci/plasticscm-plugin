package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.actions.CheckoutAction;
import com.codicesoftware.plugins.hudson.commands.ChangesetLogCommand;
import com.codicesoftware.plugins.hudson.commands.ChangesetRangeLogCommand;
import com.codicesoftware.plugins.hudson.commands.ChangesetsRetriever;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.FindChangesetCommand;
import com.codicesoftware.plugins.hudson.commands.GetWorkspaceStatusCommand;
import com.codicesoftware.plugins.hudson.commands.ParseableCommand;
import com.codicesoftware.plugins.hudson.model.BuildData;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.ChangeSetID;
import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.Workspace;
import com.codicesoftware.plugins.hudson.util.BuildVariableResolver;
import com.codicesoftware.plugins.hudson.util.FormChecker;
import com.codicesoftware.plugins.hudson.util.SelectorParametersResolver;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
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
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

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

    private static final Logger LOGGER = Logger.getLogger(PlasticSCM.class.getName());

    public static final String DEFAULT_BRANCH = "/main";
    public static final String DEFAULT_REPOSITORY = "default";
    public static final String DEFAULT_SERVER = "localhost:8087";
    public static final String DEFAULT_SELECTOR = "repository \"default\"\n  path \"/\"\n    smartbranch \"/main\"";

    public static final String WORKSPACE_NAME_PARAMETRIZED = "jenkins-${NODE_NAME}-${JOB_NAME}-${EXECUTOR_NUMBER}";

    private static final Pattern BRANCH_PATTERN = Pattern.compile(
            "^.*(smart)?br(anch)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
            "^.*rep(ository)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    // When the controller runs commands that require a workspace folder, it uses this folder (relative to the Jenkins root).
    // It will be created when necessary.
    private static final String CONTROLLER_WORKSPACE_FOLDER = "plastic";

    private final String selector;

    private CleanupMethod cleanup;
    @Deprecated
    private transient boolean useUpdate;

    private final List<WorkspaceInfo> additionalWorkspaces;
    private final WorkspaceInfo firstWorkspace;

    private final boolean pollOnController;
    private final String directory;
    private final boolean useWorkspaceSubdirectory;

    @DataBoundConstructor
    public PlasticSCM(
            String selector,
            CleanupMethod cleanup,
            boolean useMultipleWorkspaces,
            List<WorkspaceInfo> additionalWorkspaces,
            boolean pollOnController,
            String directory) {
        LOGGER.info("Initializing Plastic SCM plugin");
        this.selector = selector;
        this.cleanup = cleanup;
        this.useWorkspaceSubdirectory = useMultipleWorkspaces;
        this.pollOnController = pollOnController;
        this.directory = directory;
        firstWorkspace = new WorkspaceInfo(this.selector, this.cleanup, this.directory);
        if (additionalWorkspaces == null || !useMultipleWorkspaces) {
            this.additionalWorkspaces = null;
            return;
        }
        this.additionalWorkspaces = additionalWorkspaces;
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
    public boolean isUseMultipleWorkspaces() {
        return useWorkspaceSubdirectory;
    }

    @Exported
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
    public boolean isPollOnController() {
        return pollOnController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        adjustFieldsIfUsingOldConfigFormat();

        List<ChangeSet> changeLogItems = new ArrayList<>();

        ParametersAction parameters = run.getAction(ParametersAction.class);
        List<ParameterValue> parameterValues = (parameters == null) ? Collections.emptyList() : parameters.getParameters();

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces()) {

            FilePath plasticWorkspacePath = resolveWorkspacePath(workspace, workspaceInfo);
            String resolvedSelector = SelectorParametersResolver.resolve(workspaceInfo.getSelector(), parameterValues);

            PlasticTool tool = new PlasticTool(getDescriptor().getCmExecutable(), launcher, listener, plasticWorkspacePath);

            Workspace plasticWorkspace = setupWorkspace(tool, listener, plasticWorkspacePath, resolvedSelector, workspaceInfo.getCleanup());

            ChangeSetID csetId = determineCurrentChangeset(tool, listener, plasticWorkspacePath);

            ChangeSet cset = retrieveChangesetDetails(tool, workspace, listener, csetId.getId());
            cset.setRepoName(csetId.getRepository());
            cset.setRepoServer(csetId.getServer());

            ChangeSet previousCset = retrieveLastBuiltChangeset(tool, run,  workspace, cset);
            if (previousCset == null) {
                changeLogItems.add(cset);
            } else {
                List<ChangeSet> changeSetItems = retrieveMultipleChangesetDetails(
                    tool, workspace, listener, previousCset.getId(), cset.getId());
                for (ChangeSet it : changeSetItems) {
                    it.setRepoName(csetId.getRepository());
                    it.setRepoServer(csetId.getServer());
                }
                changeLogItems.addAll(changeSetItems);
            }

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

    private static boolean isSharedLibrary(@Nonnull FilePath jenkinsPath) {
        return jenkinsPath.getParent().getName().endsWith("@libs");
    }

    /**
     * Backward compatibility for jobs using old configuration format.
     */
    private void adjustFieldsIfUsingOldConfigFormat() {
        if (cleanup == null) {
            LOGGER.warning("Missing 'cleanup' field. Update job configuration.");
            cleanup = CleanupMethod.convertUseUpdate(useUpdate);
        }
    }

    private Workspace setupWorkspace(
            @Nonnull final PlasticTool tool,
            @Nonnull final TaskListener listener,
            @Nonnull final FilePath workspacePath,
            @Nonnull final String selector,
            @Nonnull final CleanupMethod cleanup) throws IOException, InterruptedException {
        try {
            if (!workspacePath.exists()) {
                workspacePath.mkdirs();
            }
            return CheckoutAction.checkout(tool, workspacePath, selector, cleanup);
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        } catch (IOException e) {
            throw buildAbortException(listener, e);
        }
    }

    /**
     * Jenkins older than 2.60
     * {@inheritDoc}
     *
     */
    @Override
    public void buildEnvVars(@Nonnull AbstractBuild<?, ?> build, @Nonnull Map<String, String> env) {
        super.buildEnvVars(build, env);
        buildEnvironment(build, env);
    }

    /**
     * Jenkins 2.60 and newer
     * {@inheritDoc}
     */
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
            @Nonnull final TaskListener listener) throws IOException, InterruptedException {
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

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces()) {
            FilePath plasticWorkspacePath = resolveWorkspacePath(workspace, workspaceInfo);
            String resolvedSelector = SelectorParametersResolver.resolve(
                    workspaceInfo.selector, parameters);
            boolean hasChanges = hasChanges(
                    launcher,
                    plasticWorkspacePath,
                    listener,
                    lastBuild.getTimestamp(),
                    getSelectorBranch(resolvedSelector),
                    getSelectorRepository(resolvedSelector));

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

    @Override
    public boolean requiresWorkspaceForPolling() {
        // If we poll on controller, we don't need a workspace
        // If we poll on an agent, we do need a workspace
        return !pollOnController;
    }

    public List<WorkspaceInfo> getAllWorkspaces() {
        List<WorkspaceInfo> result = new ArrayList<>();
        result.add(firstWorkspace);
        if (additionalWorkspaces != null) {
            result.addAll(additionalWorkspaces);
        }
        return result;
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

    private static String resolveWorkspaceNameParameters(
            Run<?, ?> build,
            FilePath workspacePath,
            String workspaceName,
            WorkspaceInfo workspaceInfo) {
        String result = workspaceName;

        if (Util.fixEmpty(result) == null) {
            result = PlasticSCM.WORKSPACE_NAME_PARAMETRIZED;
        }

        if (build != null) {
            result = replaceBuildParameter(build, result);
            BuildVariableResolver buildVariableResolver = new BuildVariableResolver(
                    build.getParent(), Computer.currentComputer(), workspacePath);
            result = Util.replaceMacro(result, buildVariableResolver);
        }

        if (Util.fixEmpty(workspaceInfo.getDirectory()) != null) {
            result += "-" + workspaceInfo.getDirectory();
        }
        result = result.replaceAll("[\"/:<>\\|\\*\\?]+", "_");
        return result.replaceAll("[\\.\\s]+$", "_");
    }

    private static String replaceBuildParameter(Run<?, ?> run, String text) {
        if (run instanceof AbstractBuild<?, ?>) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
            if (build.getAction(ParametersAction.class) != null) {
                return build.getAction(ParametersAction.class).substitute(build, text);
            }
        }

        return text;
    }

    /**
     * Returns changeset identifier for the given workspace.
     */
    private static ChangeSetID determineCurrentChangeset(
            PlasticTool tool,
            TaskListener listener,
            FilePath workspacePath)
            throws IOException, InterruptedException {
        try {
            ParseableCommand<List<ChangeSetID>> statusCommand = new GetWorkspaceStatusCommand(workspacePath.getRemote());
            List<ChangeSetID> list = CommandRunner.executeAndRead(tool, statusCommand);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
            return null;
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        }
    }

    /**
     * Finds changeset of the last completed build for the same branch as the given changeset.
     * Returns null if not found or is newer than the currently build.
     */
    private static ChangeSet retrieveLastBuiltChangeset(
            PlasticTool tool, Run<?, ?> build, FilePath workspacePath, ChangeSet cset) {
        if (cset == null || Util.fixEmpty(cset.getBranch()) == null ||
                Util.fixEmpty(cset.getRepoName()) == null || Util.fixEmpty(cset.getRepoServer()) == null) {
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
            ParseableCommand<ChangeSet> command = new FindChangesetCommand(
                    cset.getId(), cset.getBranch(), cset.getRepository(), xmlOutputPath);
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

    private static ChangeSet retrieveChangesetDetails(
            PlasticTool tool,
            FilePath workspacePath,
            TaskListener listener,
            int csetId)
            throws IOException, InterruptedException {
        FilePath xmlOutputPath = OutputTempFile.getPathForXml(workspacePath);
        try {
            ParseableCommand<ChangeSet> command = new ChangesetLogCommand(
                "cs:" + csetId, xmlOutputPath);
            return CommandRunner.executeAndRead(tool, command, false);
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        } finally {
            OutputTempFile.safeDelete(xmlOutputPath);
        }
    }

    private static List<ChangeSet> retrieveMultipleChangesetDetails(
            PlasticTool tool,
            FilePath workspacePath,
            TaskListener listener,
            int csetIdFrom,
            int csetIdTo)
            throws IOException, InterruptedException {
        FilePath xmlOutputPath = OutputTempFile.getPathForXml(workspacePath);
        try {
            ParseableCommand<List<ChangeSet>> command = new ChangesetRangeLogCommand(
                "cs:" + csetIdFrom, "cs:" + csetIdTo, xmlOutputPath);
            return CommandRunner.executeAndRead(tool, command, false);
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        } finally {
            OutputTempFile.safeDelete(xmlOutputPath);
        }
    }

    private static AbortException buildAbortException(
            TaskListener listener, Exception e) {
        listener.fatalError(e.getMessage());
        LOGGER.severe(e.getMessage());
        return new AbortException();
    }

    private void writeChangeLog(
            TaskListener listener,
            File changelogFile,
            List<ChangeSet> result) throws AbortException {
        try {
            ChangeSetWriter writer = new ChangeSetWriter();
            writer.write(result, changelogFile);
        } catch (Exception e) {
            listener.fatalError(e.getMessage());
            LOGGER.severe(e.getMessage());
            throw new AbortException();
        }
    }

    private boolean hasChanges(
            Launcher launcher,
            FilePath workspacePath,
            TaskListener listener,
            Calendar lastCompletedBuildTimestamp,
            String branchName,
            String repository) throws IOException, InterruptedException {
        if (launcher == null && workspacePath == null) {
            // hasChanges() has been invoked on the master, without any workspace
            // We will provide the plugin with a LocalLauncher and a fake workspace, since:
            // - PlasticTool needs a launcher and a workspace
            // - getChanges() needs a place to put the temp file where it captures PlasticTool's output
            launcher = new Launcher.LocalLauncher(listener);
            workspacePath = new FilePath(new FilePath(Jenkins.getInstance().getRootDir()), CONTROLLER_WORKSPACE_FOLDER);
            workspacePath.mkdirs();
        }

        PlasticTool plasticTool = new PlasticTool(getDescriptor().getCmExecutable(),
                launcher, listener, workspacePath);
        try {
            List<ChangeSet> changesetsFromBuild = ChangesetsRetriever.getChangesets(
                plasticTool,
                workspacePath, branchName,
                repository,
                lastCompletedBuildTimestamp,
                Calendar.getInstance());
            return changesetsFromBuild.size() > 0;
        } catch (Exception e) {
            e.printStackTrace(listener.error(String.format(
                "%s: Unable to retrieve workspace status.", workspacePath.getRemote())));
            return false;
        }
    }

    private List<ParameterValue> getDefaultParameterValues(Job<?, ?> project) {
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

    private String getSelectorBranch(String selector) {
        Matcher smartbranchMatcher = BRANCH_PATTERN.matcher(selector);
        if (smartbranchMatcher.matches()) {
            return smartbranchMatcher.group(3);
        }
        return null;
    }

    private String getSelectorRepository(String selector) {
        Matcher matcher = REPOSITORY_PATTERN.matcher(selector);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
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

    private static final String PLASTIC_ENV_PREFIX = "PLASTICSCM_";
    private static final String CHANGESET_ID = "CHANGESET_ID";
    private static final String CHANGESET_GUID = "CHANGESET_GUID";
    private static final String BRANCH = "BRANCH";
    private static final String AUTHOR = "AUTHOR";
    private static final String REPSPEC = "REPSPEC";

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<PlasticSCM> {
        private String cmExecutable;

        public DescriptorImpl() {
            super(PlasticSCM.class, null);
            load();
        }

        @RequirePOST
        public static FormValidation doCheckSelector(@QueryParameter String value) {
            return FormChecker.doCheckSelector(value);
        }

        @RequirePOST
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
            return PlasticSCM.DEFAULT_SELECTOR;
        }

        public String getDisplayName() {
            return "Plastic SCM";
        }

        public String getCmExecutable() {
            if (cmExecutable == null) {
                return "cm";
            } else {
                return cmExecutable;
            }
        }

        @Override
        public boolean isApplicable(Job project) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            cmExecutable = Util.fixEmpty(formData.getString("cmExecutable").trim());
            save();
            return true;
        }

        @RequirePOST
        public FormValidation doCheckExecutable(@QueryParameter("cmExecutable") String value) {
            try {
                FormValidation validation = FormValidation.validateExecutable(value);
                if (validation.kind == FormValidation.Kind.OK) {
                    validation = FormChecker.createValidationResponse("Success", false);
                } else {
                    validation = FormChecker.createValidationResponse("Failure: " + validation.getMessage(), true);
                }
                return validation;
            } catch (Exception e) {
                return FormChecker.createValidationResponse("Error: " + e.getMessage(), true);
            }
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

        @Extension
        public static class DescriptorImpl extends Descriptor<WorkspaceInfo> {

            @RequirePOST
            public static FormValidation doCheckSelector(@QueryParameter String value) {
                return FormChecker.doCheckSelector(value);
            }

            @RequirePOST
            public static FormValidation doCheckDirectory(@QueryParameter String value, @AncestorInPath Item item) {
                return FormChecker.doCheckDirectory(value, item);
            }

            public static String getDefaultSelector() {
                return PlasticSCM.DEFAULT_SELECTOR;
            }

            @Override
            public String getDisplayName() {
                return "Plastic SCM Workspace";
            }
        }
    }
}
