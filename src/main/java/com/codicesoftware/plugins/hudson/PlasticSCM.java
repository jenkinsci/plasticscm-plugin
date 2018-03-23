package com.codicesoftware.plugins.hudson;

import static hudson.scm.PollingResult.BUILD_NOW;
import static hudson.scm.PollingResult.NO_CHANGES;

import com.codicesoftware.plugins.hudson.actions.CheckoutAction;
import com.codicesoftware.plugins.hudson.model.*;
import com.codicesoftware.plugins.hudson.util.BuildVariableResolver;
import com.codicesoftware.plugins.hudson.util.FormChecker;

import hudson.*;
import hudson.model.*;
import hudson.scm.*;
import hudson.util.FormValidation;

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

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SCM for Plastic SCM
 *
 * Based on the tfs plugin by Erik Ramfelt
 *
 * @author Dick Porter
 */
public class PlasticSCM extends SCM {
    public final String selector;
    public final String workspaceName;
    public final boolean useUpdate;

    private List<WorkspaceInfo> additionalWorkspaces = new ArrayList<WorkspaceInfo>();
    private WorkspaceInfo firstWorkspace;

    private static final Pattern BRANCH_PATTERN = Pattern.compile(
        "^.*(smart)?br(anch)? \"([^\"]*)\".*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    private static final Logger logger = Logger.getLogger(PlasticSCM.class.getName());
    
    private PlasticSCM() {
        selector = FormChecker.getDefaultSelector();
        workspaceName = "jenkins-default";
        useUpdate = true;
    }

    @DataBoundConstructor
    public PlasticSCM(
            String selector,
            String workspaceName,
            boolean useUpdate,
            List<WorkspaceInfo> additionalWorkspaces) {
        logger.info("Initializing PlasticSCM plugin");
        this.selector = selector;
        this.workspaceName = workspaceName;
        this.useUpdate = useUpdate;
        this.firstWorkspace = new WorkspaceInfo(selector, workspaceName, useUpdate);

        this.additionalWorkspaces = new ArrayList<WorkspaceInfo>();
        if (additionalWorkspaces != null)
            this.additionalWorkspaces.addAll(additionalWorkspaces);
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

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces(run.getAction(ParametersAction.class))) {
            String normalizedWorkspaceName = normalizeWorkspace(
                workspaceInfo.getWorkspaceName(), run.getParent(), run);

            FilePath plasticWorkspace = new FilePath(workspace,
                normalizedWorkspaceName);

            if (!plasticWorkspace.exists())
                plasticWorkspace.mkdirs();

            Server server = new Server(new PlasticTool(
                getDescriptor().getCmExecutable(), launcher, listener, plasticWorkspace));

            WorkspaceConfiguration workspaceConfiguration = createWorkspaceConfiguration(
                normalizedWorkspaceName, workspaceInfo.getSelector());

            List<ChangeSet> csets = checkoutWorkspace(
                run,
                plasticWorkspace,
                server,
                listener,
                workspaceConfiguration,
                workspaceInfo);
            result.addAll(csets);

            run.addAction(new BuildData(
                workspaceInfo.getWorkspaceName(), getLastChangeSet(csets)));
        }

        if (changelogFile != null)
            writeChangeLog(listener, changelogFile, result);
    }

    @Override
    public void buildEnvVars(
            @Nonnull final AbstractBuild<?, ?> build, @Nonnull final Map<String, String> env) {
        super.buildEnvVars(build, env);

        List<WorkspaceInfo> wkInfos = getAllWorkspaces(
            build.getAction(ParametersAction.class));

        for (BuildData buildData : build.getActions(BuildData.class)) {
            if (buildData.getBuiltCset() == null)
                continue;
            publishCsetToEnvironment(
                buildData.getBuiltCset(),
                findWorkspaceInfoFromName(buildData.getWkName(), wkInfos),
                env);
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
            @Nullable final FilePath workspacePath,
            @Nonnull final TaskListener listener,
            @Nonnull final SCMRevisionState baseline) {
        if (project.getLastBuild() == null) {
            listener.getLogger().println("No builds detected yet!");
            return BUILD_NOW;
        }

        Run<?, ?> lastBuild = project.getLastBuild();
        for (WorkspaceInfo workspaceInfo : getAllWorkspaces(project.getAction(ParametersAction.class))) {
            FilePath plasticWorkspace = new FilePath(workspacePath, normalizeWorkspace(
                workspaceInfo.getWorkspaceName(), project, lastBuild));

            String resolvedSelector = replaceParameters(
                    selector, getDefaultParameterValues(project));
            boolean hasChanges = hasChanges(
                    launcher,
                    plasticWorkspace,
                    listener,
                    lastBuild.getTimestamp(),
                    getSelectorBranch(resolvedSelector));

            if (hasChanges)
                return BUILD_NOW;
        }
        return NO_CHANGES;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private String normalizeWorkspace(
            String workspaceName,
            Job<?,?> project,
            Run<?,?> build) {
        String result = workspaceName;

        if (build != null) {
            result = replaceBuildParameter(build, result);
            BuildVariableResolver buildVariableResolver = new BuildVariableResolver(
                project, Computer.currentComputer());
            result = Util.replaceMacro(result, buildVariableResolver);
        }
        result = result.replaceAll("[\"/:<>\\|\\*\\?]+", "_");
        result = result.replaceAll("[\\.\\s]+$", "_");

        return result;
    }

    public List<WorkspaceInfo> getAdditionalWorkspaces() {
        if (additionalWorkspaces == null)
            additionalWorkspaces = new ArrayList<WorkspaceInfo>();
        return additionalWorkspaces;
    }

    @Nonnull
    private List<WorkspaceInfo> getAllWorkspaces(ParametersAction parameters) {
        List<WorkspaceInfo> result =  new ArrayList<WorkspaceInfo>();
        result.add(getFirstWorkspace());
        result.addAll(getAdditionalWorkspaces());
        return replaceBuildParameters(result, parameters);
    }

    private List<WorkspaceInfo> replaceBuildParameters(
            List<WorkspaceInfo> workspaces, ParametersAction parameters) {
        List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();

        List<ParameterValue> parameterValues = parameters == null ? null : parameters.getParameters();
        for (WorkspaceInfo wkInfo : workspaces) {
            if (wkInfo == null)
                continue;

            result.add(new WorkspaceInfo(
                    replaceParameters(wkInfo.getSelector(), parameterValues),
                    wkInfo.getWorkspaceName(),
                    wkInfo.getUseUpdate()));
        }
        return result;
    }

    private WorkspaceInfo getFirstWorkspace() {
        if (firstWorkspace == null) {
            firstWorkspace =  new WorkspaceInfo(selector, workspaceName, useUpdate);
        }
        return firstWorkspace;
    }

    private String replaceParameters(String selector, List<ParameterValue> parameters) {
        if (parameters == null)
            return selector;

        logger.info("Replacing build parameters in selector...");

        String result = selector;
        for (ParameterValue parameter : parameters) {
            if (!(parameter instanceof StringParameterValue))
                continue;

            StringParameterValue stringParameter = (StringParameterValue)parameter;
            String variable = "%" + stringParameter.getName() + "%";
            String value = stringParameter.value;
            logger.info("Replacing [" + variable + "]->[" + value + "]");
            result = result.replace(variable, value);
        }

        return result;
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

    private WorkspaceConfiguration createWorkspaceConfiguration(
            String normalizedWorkspaceName,
            String selector) {
        return new WorkspaceConfiguration(normalizedWorkspaceName, selector);
    }
    
    private List<ChangeSet> checkoutWorkspace(
            Run<?, ?> build,
            FilePath plasticWorkspace,
            Server server,
            TaskListener listener,
            WorkspaceConfiguration workspaceConfiguration,
            WorkspaceInfo workspaceInfo) throws IOException, InterruptedException{
        build.addAction(workspaceConfiguration);
        CheckoutAction action = new CheckoutAction(
            workspaceConfiguration.getWorkspaceName(),
            workspaceConfiguration.getSelector(),
            workspaceInfo.getUseUpdate());
        try {
            Calendar previousBuildDate = null;
            if (build.getPreviousBuild() != null)
                previousBuildDate = build.getPreviousBuild().getTimestamp();

            return action.checkout(
                server, plasticWorkspace, previousBuildDate, build.getTimestamp());
        } catch (ParseException e) {
            throw buildAbortException(listener, e);
        } catch (IOException e) {
            throw buildAbortException(listener, e);
        }
    }

    private AbortException buildAbortException(TaskListener listener, Exception e)
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
            String branchName) {
        PlasticTool plasticTool = new PlasticTool(getDescriptor().getCmExecutable(),
            launcher, listener, workspacePath); 
        Server server = new Server(plasticTool);
        try {
            List<ChangeSet> changesetsFromBuild = server.getBriefHistory(
                workspacePath.getRemote(),
                branchName,
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
            return null;

        for (WorkspaceInfo wkInfo : wkInfos) {
            if (wkName.equals(wkInfo.getWorkspaceName()))
                return wkInfo;
        }
        return null;
    }

    private void publishCsetToEnvironment(
            @Nonnull final ChangeSet cset,
            @CheckForNull final WorkspaceInfo wkInfo,
            @Nonnull final Map<String, String> environment) {
        String variablePrefix = getEnvironmentVariablePrefix(wkInfo);

        environment.put(variablePrefix + CHANGESET_ID, cset.getVersion());
        environment.put(variablePrefix + CHANGESET_GUID, cset.getGuid());
        environment.put(variablePrefix + BRANCH, cset.getBranch());
        environment.put(variablePrefix + AUTHOR, cset.getUser());
        environment.put(variablePrefix + REPSPEC, cset.getRepository());
    }

    @Nonnull
    private String getEnvironmentVariablePrefix(@CheckForNull final WorkspaceInfo wkInfo) {
        if (wkInfo == null)
            return PLASTIC_ENV_UNKNOWN_PREFIX;

        String wkName = wkInfo.getWorkspaceName();
        if (wkName.equals(wkInfo.getWorkspaceName()))
            return PLASTIC_ENV_PREFIX;

        List<WorkspaceInfo> additionalWorkspaces = getAdditionalWorkspaces();
        for (int i = 0; i < additionalWorkspaces.size(); i++) {
            if (wkName.equals(additionalWorkspaces.get(i).getWorkspaceName())) {
                return String.format("%s%d_", PLASTIC_ENV_PREFIX, i);
            }
        }
        return PLASTIC_ENV_UNKNOWN_PREFIX;
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
            cmExecutable = Util.fixEmpty(req.getParameter("plastic.cmExecutable").trim());
            save();
            return true;
        }

        public FormValidation doCheckExecutable(@QueryParameter final String value) {
            return FormValidation.validateExecutable(value);
        }

        public String getDisplayName() {
            return "Plastic SCM";
        }

        public static FormValidation doCheckWorkspaceName(@QueryParameter final String value) {
            return FormChecker.doCheckWorkspaceName(value);
        }

        public static FormValidation doCheckSelector(@QueryParameter final String value) {
            return FormChecker.doCheckSelector(value);
        }

        public String getDefaultSelector() {
            return FormChecker.getDefaultSelector();
        }

        public String getDefaultWorkspaceName() {
            return PlasticSCMStep.PlasticStepDescriptor.defaultWorkspaceName;
        }
    }

    @ExportedBean
    public static final class WorkspaceInfo extends AbstractDescribableImpl<WorkspaceInfo> implements Serializable {
        @Exported
        public final String selector;

        @Exported
        public final String workspaceName;

        @Exported
        public final boolean useUpdate;

        private static final long serialVersionUID = 1L;

        @DataBoundConstructor
        public WorkspaceInfo(String selector, String workspaceName, boolean useUpdate) {
            this.selector = selector;
            this.workspaceName = workspaceName;
            this.useUpdate = useUpdate;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        public String getWorkspaceName() {
            return workspaceName;
        }

        public String getSelector() {
            return selector;
        }

        public boolean getUseUpdate() {
            return useUpdate;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<WorkspaceInfo> {

            @Override
            public String getDisplayName() {
                return "Workspace Info";
            }

            public static FormValidation doCheckWorkspaceName(@QueryParameter final String value) {
                return FormChecker.doCheckWorkspaceName(value);
            }

            public static FormValidation doCheckSelector(@QueryParameter final String value) {
                return FormChecker.doCheckSelector(value);
            }

            public String getDefaultSelector() {
                return FormChecker.getDefaultSelector();
            }
        }
    }
}