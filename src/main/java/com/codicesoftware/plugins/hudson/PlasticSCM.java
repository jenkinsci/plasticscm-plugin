package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.actions.CheckoutAction;
import com.codicesoftware.plugins.hudson.commands.ChangesetsRetriever;
import com.codicesoftware.plugins.hudson.model.BuildData;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.util.BuildVariableResolver;
import com.codicesoftware.plugins.hudson.util.FormChecker;
import com.codicesoftware.plugins.hudson.util.SelectorParametersResolver;
import hudson.*;
import hudson.model.*;
import hudson.scm.*;
import hudson.util.FormValidation;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.scm.PollingResult.BUILD_NOW;
import static hudson.scm.PollingResult.NO_CHANGES;

/**
 * SCM for Plastic SCM
 *
 * Based on the tfs plugin by Erik Ramfelt
 *
 * @author Dick Porter
 */
public class PlasticSCM extends SCM {

    public static final String DEFAULT_BRANCH = "/main";
    public static final String DEFAULT_REPOSITORY = "default";
    public static final String DEFAULT_SERVER = "localhost:8087";
    public static final String DEFAULT_SELECTOR = "repository \"default\"\n  path \"/\"\n    smartbranch \"/main\"";

    public static final String WORKSPACE_NAME_PARAMETRIZED = "jenkins-${NODE_NAME}-${JOB_NAME}-${EXECUTOR_NUMBER}";

    public final String selector;
    public String workspaceName = WORKSPACE_NAME_PARAMETRIZED;
    public final boolean useUpdate;
    public final boolean useWorkspaceSubdirectory;
    private final List<WorkspaceInfo> additionalWorkspaces;
    private final WorkspaceInfo firstWorkspace;
    public final String directory;

    private static final Pattern BRANCH_PATTERN = Pattern.compile(
        "^.*(smart)?br(anch)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
            "^.*rep(ository)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    private static final Logger logger = Logger.getLogger(PlasticSCM.class.getName());
    
    private PlasticSCM() {
        selector = DEFAULT_SELECTOR;
        useUpdate = true;
        firstWorkspace = null;
        additionalWorkspaces = null;
        useWorkspaceSubdirectory = false;
        directory = "";
    }

    @DataBoundConstructor
    public PlasticSCM(
            String selector,
            String workspaceName,
            boolean useUpdate,
            boolean useMultipleWorkspaces,
            List<WorkspaceInfo> additionalWorkspaces,
            String directory) {
        logger.info("Initializing PlasticSCM plugin");
        this.selector = selector;
        this.useUpdate = useUpdate;
        this.useWorkspaceSubdirectory = useMultipleWorkspaces;
        this.directory = directory;

        if (workspaceName != null && !workspaceName.equals("")) {
            this.workspaceName = WorkspaceInfo.cleanWorkspaceName(workspaceName);
        }

        firstWorkspace = new WorkspaceInfo(this.selector, this.workspaceName, this.useUpdate, this.directory);
        if(additionalWorkspaces == null || !useMultipleWorkspaces) {
            this.additionalWorkspaces = null;
            return;
        }
        this.additionalWorkspaces = additionalWorkspaces;
    }

    public boolean isUseMultipleWorkspaces() {
        return useWorkspaceSubdirectory;
    }

    public List<WorkspaceInfo> getAdditionalWorkspaces() {
        return additionalWorkspaces;
    }

    public WorkspaceInfo getFirstWorkspace() {
        return firstWorkspace;
    }

    @Override
    public String getKey() {
        return selector;
    }

    @Override
    @CheckForNull
    public RepositoryBrowser<?> guessBrowser() {
        return null;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeSetReader();
    }

    @Override
    public void checkout(
            @Nonnull final Run<?, ?> run,
            @Nonnull final Launcher launcher,
            @Nonnull final FilePath workspace,
            @Nonnull final TaskListener listener,
            @CheckForNull final File changelogFile,
            @CheckForNull final SCMRevisionState baseline) throws IOException, InterruptedException  {
        List<ChangeSet> result = new ArrayList<ChangeSet>();

        ParametersAction parameters = run.getAction(ParametersAction.class);
        List<ParameterValue> parameterValues =
                parameters == null ? null : parameters.getParameters();

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces()) {
            // shared libraries need to set the workspace name every time
            // because they use a common SCM object that isn't initialized
            // in a sane way
            if (isSharedLibrary(workspace))
                workspaceInfo.setWorkspaceName("shl-" + workspace.getRemote().hashCode());

            String plasticWorkspaceName = resolveWorkspaceNameParameters(workspace, workspaceInfo, run);
            FilePath plasticWorkspacePath = resolveWorkspacePath(workspace, workspaceInfo);

            String resolvedSelector = SelectorParametersResolver.resolve(
                    workspaceInfo.getSelector(), parameterValues);
            result.addAll(
                SetUpWorkspace(
                    run,
                    launcher,
                    listener,
                    plasticWorkspaceName,
                    plasticWorkspacePath,
                    resolvedSelector,
                    workspaceInfo.getUseUpdate())
            );
        }

        if (changelogFile != null)
            writeChangeLog(listener, changelogFile, result);
    }

    private static boolean isSharedLibrary(@Nonnull FilePath jenkinsPath) {
        return jenkinsPath.getParent().getName().endsWith("@libs");
    }

    private List<ChangeSet> SetUpWorkspace(
            @Nonnull final Run<?, ?> run,
            @Nonnull final Launcher launcher,
            @Nonnull final TaskListener listener,
            @Nonnull final String plasticWorkspaceName,
            @Nonnull final FilePath plasticWorkspacePath,
            @Nonnull final String resolvedSelector,
            @Nonnull final boolean useUpdate) throws IOException, InterruptedException {
        if (!plasticWorkspacePath.exists()) {
            plasticWorkspacePath.mkdirs();
        }

        PlasticTool tool = new PlasticTool(
                getDescriptor().getCmExecutable(), launcher, listener, plasticWorkspacePath);

        List<ChangeSet> csetsInBuild = FindCsets(
            run, tool, listener, plasticWorkspacePath,
            getSelectorBranch(resolvedSelector),
            getSelectorRepository(resolvedSelector));
        run.addAction(new BuildData(plasticWorkspaceName, getLastChangeSet(csetsInBuild)));

        checkoutWorkspace(
            plasticWorkspacePath,
            tool,
            listener,
            plasticWorkspaceName,
            resolvedSelector,
            useUpdate);

        return csetsInBuild;
    }

    // Pre Jenkins 2.60
    @Override
    public void buildEnvVars(
            @Nonnull final AbstractBuild<?, ?> build, @Nonnull final Map<String, String> env) {
        super.buildEnvVars(build, env);
        buildEnvironment(build, env);
    }

    // Post Jenkins 2.60
    public void buildEnvironment(Run<?, ?> build, Map<String, String> env) {
        List<WorkspaceInfo> allWorkspaces = getAllWorkspaces();

        for (BuildData buildData : build.getActions(BuildData.class)) {
            String wkName = buildData.getWkName();
            ChangeSet builtCset = getBuildChangeSet(build, wkName);

            if (builtCset == null)
                continue;

            publishCsetToEnvironment(
                builtCset, findWorkspaceInfoFromName(wkName, allWorkspaces), env, allWorkspaces);
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

    @Override
    public PollingResult compareRemoteRevisionWith(
            @Nonnull final  Job<?,?> project,
            @Nullable final Launcher launcher,
            @Nullable final FilePath workspace,
            @Nonnull final TaskListener listener,
            @Nonnull final SCMRevisionState baseline) {
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

            if (hasChanges)
                return BUILD_NOW;
        }
        return NO_CHANGES;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public List<WorkspaceInfo> getAllWorkspaces() {
        List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
        result.add(firstWorkspace);
        if(additionalWorkspaces != null)
            result.addAll(additionalWorkspaces);
        return result;
    }

    private FilePath resolveWorkspacePath(
            FilePath jenkinsWorkspacePath,
            WorkspaceInfo workspaceInfo) {
        if (jenkinsWorkspacePath == null || workspaceInfo == null) {
            return null;
        }
        String subdirectory = workspaceInfo.getDirectory();
        if (subdirectory == null || subdirectory.isEmpty()) {
            return jenkinsWorkspacePath;
        }
        return new FilePath(jenkinsWorkspacePath, workspaceInfo.getDirectory());
    }

//    private String generateUniqueWorkspaceName(Run<?, ?> build) {
//        String result = "jenkins-" + build.getParent().getName() + "-" + RandomStringUtils.randomAlphanumeric(12);
//        return result.replaceAll("[^A-Za-z0-9_\\-]", "_");
//    }

    private String resolveWorkspaceNameParameters(
            FilePath workspacePath,
            WorkspaceInfo workspaceInfo,
            Run<?,?> build) {
        if (workspaceName == null)
            return null;

        String result = workspaceName;

        if (build != null) {
            result = replaceBuildParameter(build, result);
            BuildVariableResolver buildVariableResolver = new BuildVariableResolver(
                build.getParent(), Computer.currentComputer(), workspacePath);
            result = Util.replaceMacro(result, buildVariableResolver);
        }

        if (!workspaceInfo.getDirectory().isEmpty()) {
            result += "-" + workspaceInfo.getDirectory();
        }
        result = result.replaceAll("[\"/:<>\\|\\*\\?]+", "_");
        return result.replaceAll("[\\.\\s]+$", "_");
    }

    private String replaceBuildParameter(Run<?,?> run, String text) {
        if (run instanceof AbstractBuild<?,?>) {
            AbstractBuild<?,?> build = (AbstractBuild<?,?>)run;
            if (build.getAction(ParametersAction.class) != null) {
                return build.getAction(ParametersAction.class).substitute(build, text);
            }
        }

        return text;
    }

    private static void checkoutWorkspace(
            FilePath workspacePath,
            PlasticTool tool,
            TaskListener listener,
            String workspaceName,
            String selector,
            boolean useUpdate) throws IOException, InterruptedException {
        try {
            CheckoutAction.checkout(
                tool, workspaceName, workspacePath, selector, useUpdate);
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        } catch (IOException e) {
            throw buildAbortException(listener, e);
        }
    }

    private static List<ChangeSet> FindCsets(
            Run<?, ?> build,
            PlasticTool tool,
            TaskListener listener,
            FilePath workspacePath,
            String branchName,
            String repository)
            throws IOException, InterruptedException {
        Calendar previousBuildDate = getPreviousBuildDate(build);
        if (previousBuildDate == null)
            return new ArrayList<ChangeSet>();

        try {
            return ChangesetsRetriever.getDetailedHistory(
                tool, workspacePath, branchName, repository,
                previousBuildDate, build.getTimestamp());
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        }
    }

    private static Calendar getPreviousBuildDate(Run<?, ?> build) {
        Run<?, ?> previousBuild = build.getPreviousBuild();
        return previousBuild == null ? null : previousBuild.getTimestamp();
    }

    private static AbortException buildAbortException(
            TaskListener listener, Exception e)
    {
        listener.fatalError(e.getMessage());
        logger.severe(e.getMessage());
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
            logger.severe(e.getMessage());
            throw new AbortException();
        }
    }

    private boolean hasChanges(
            Launcher launcher,
            FilePath workspacePath,
            TaskListener listener,
            Calendar lastCompletedBuildTimestamp,
            String branchName,
            String repository) {
        PlasticTool plasticTool = new PlasticTool(getDescriptor().getCmExecutable(),
            launcher, listener, workspacePath); 
        try {
            List<ChangeSet> changesetsFromBuild = ChangesetsRetriever.getChangesets(
                plasticTool,
                branchName,
                repository,
                lastCompletedBuildTimestamp,
                Calendar.getInstance());
            return changesetsFromBuild.size() > 0;
        } catch (Exception e) {
            e.printStackTrace(listener.error(workspacePath.getRemote()
                + ": Unable to retrieve workspace status."));
            return false;
        }
    }

    private List<ParameterValue> getDefaultParameterValues(Job<?, ?> project) {
        ParametersDefinitionProperty paramDefProp = project.getProperty(
            ParametersDefinitionProperty.class);
        if (paramDefProp == null)
            return null;

        ArrayList<ParameterValue> result = new ArrayList<ParameterValue>();

        for(ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
            ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

            if(defaultValue != null)
                result.add(defaultValue);
        }

        return result;
    }

    private String getSelectorBranch(String selector) {
        Matcher smartbranchMatcher = BRANCH_PATTERN.matcher(selector);
        if (smartbranchMatcher.matches())
            return smartbranchMatcher.group(3);
        return null;
    }

    private String getSelectorRepository(String selector) {
        Matcher matcher = REPOSITORY_PATTERN.matcher(selector);
        if (matcher.matches())
            return matcher.group(2);
        return null;
    }

    @Nullable
    private ChangeSet getLastChangeSet(@Nonnull final List<ChangeSet> csets) {
        ChangeSet result = null;
        for (ChangeSet cset : csets) {
            if (result == null || result.getDate().before(cset.getDate()))
                result = cset;
        }
        return result;
    }

    @CheckForNull
    private WorkspaceInfo findWorkspaceInfoFromName(
            @CheckForNull final String wkName, @Nonnull final List<WorkspaceInfo> wkInfos) {
        if (wkName == null)
            return getFirstWorkspace();

        for (WorkspaceInfo wkInfo : wkInfos) {
            if (wkName.equals(wkInfo.getWorkspaceName()))
                return wkInfo;
        }
        return null;
    }

    private void publishCsetToEnvironment(
            @Nonnull final ChangeSet cset,
            @CheckForNull final WorkspaceInfo wkInfo,
            @Nonnull final Map<String, String> environment,
            @CheckForNull final List<WorkspaceInfo> allWorkspaces) {
        String variablePrefix = getEnvironmentVariablePrefix(wkInfo, allWorkspaces);

        environment.put(variablePrefix + CHANGESET_ID, cset.getVersion());
        environment.put(variablePrefix + CHANGESET_GUID, cset.getGuid());
        environment.put(variablePrefix + BRANCH, cset.getBranch());
        environment.put(variablePrefix + AUTHOR, cset.getUser());
        environment.put(variablePrefix + REPSPEC, cset.getRepository());
    }

    @Nonnull
    private String getEnvironmentVariablePrefix(
            @CheckForNull final WorkspaceInfo wkInfo,
            @CheckForNull final List<WorkspaceInfo> allWorkspaces) {
        if (wkInfo == null)
            return PLASTIC_ENV_UNKNOWN_PREFIX;

        int index = allWorkspaces.indexOf(wkInfo);
        if(index == -1)
            return PLASTIC_ENV_UNKNOWN_PREFIX;

        if(index == 0)
            return PLASTIC_ENV_PREFIX;

        return PLASTIC_ENV_PREFIX + index + "_";
    }

    private static ChangeSet getBuildChangeSet(Run build, String wkName){
        while (build != null) {
            for (BuildData buildData : build.getActions(BuildData.class)) {
                if (buildData == null)
                    continue;

                if (wkName == null && buildData.getWkName() != null)
                    continue;

                if (wkName != null && !wkName.equals(buildData.getWkName()))
                    continue;

                if (buildData.getBuiltCset() == null)
                    continue;

                return buildData.getBuiltCset();
            }

            build = build.getPreviousBuild();
        }

        return null;
    }

    private static final String PLASTIC_ENV_PREFIX = "PLASTICSCM_";
    private static final String PLASTIC_ENV_UNKNOWN_PREFIX =
        PLASTIC_ENV_PREFIX + "UNKNOWN";

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

        @RequirePOST
        public static FormValidation doCheckWorkspaceName(@QueryParameter String value) {
            return FormChecker.doCheckWorkspaceName(value);
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

        public static String getDefaultWorkspaceName() {
            return PlasticSCM.WORKSPACE_NAME_PARAMETRIZED;
        }
    }

    @ExportedBean
    public static final class WorkspaceInfo extends AbstractDescribableImpl<WorkspaceInfo> implements Serializable {
        @Exported
        public final String selector;

        @Exported
        public String workspaceName;

        @Exported
        public final boolean useUpdate;

        @Exported
        public String directory;

        private static final long serialVersionUID = 1L;

        @DataBoundConstructor
        public WorkspaceInfo(
                String selector,
                String workspaceName,
                boolean useUpdate,
                String directory) {
            this.selector = selector;
            this.workspaceName = cleanWorkspaceName(workspaceName);
            this.useUpdate = useUpdate;
            this.directory = directory;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        public String getWorkspaceName() {
            return workspaceName;
        }

        public void setWorkspaceName(String workspaceName) {
            this.workspaceName = Util.fixNull(workspaceName);
        }

        public String getSelector() {
            return selector;
        }

        public boolean getUseUpdate() {
            return useUpdate;
        }

        public  static String cleanWorkspaceName(String wkName){
            if(wkName == null)
                return null;
            return wkName.replaceAll("@", "-");
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = Util.fixNull(directory);
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<WorkspaceInfo> {

            @Override
            public String getDisplayName() {
                return "Plastic SCM Workspace";
            }

            @RequirePOST
            public static FormValidation doCheckWorkspaceName(@QueryParameter String value) {
                return FormChecker.doCheckWorkspaceName(value);
            }

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

            public static String getDefaultWorkspaceName() {
                return PlasticSCM.WORKSPACE_NAME_PARAMETRIZED;
            }
        }
    }
}
