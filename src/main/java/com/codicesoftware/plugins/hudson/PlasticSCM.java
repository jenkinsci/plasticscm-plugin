package com.codicesoftware.plugins.hudson;

import static hudson.scm.PollingResult.BUILD_NOW;
import static hudson.scm.PollingResult.NO_CHANGES;

import com.codicesoftware.plugins.hudson.actions.CheckoutAction;
import com.codicesoftware.plugins.hudson.actions.RemoveWorkspaceAction;
import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.Server;
import com.codicesoftware.plugins.hudson.model.WorkspaceConfiguration;
import com.codicesoftware.plugins.hudson.util.BuildVariableResolver;
import com.codicesoftware.plugins.hudson.util.BuildWorkspaceConfigurationRetriever;
import com.codicesoftware.plugins.hudson.util.BuildWorkspaceConfigurationRetriever.BuildWorkspaceConfiguration;
import com.codicesoftware.plugins.hudson.util.FormChecker;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

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
    public ChangeLogParser createChangeLogParser() {
        return new ChangeSetReader();
    }

    @Override
    public boolean checkout(
            AbstractBuild<?, ?> build,
            Launcher launcher,
            FilePath workspace,
            BuildListener listener,
            File changelogFile) throws IOException, InterruptedException  {
        List<ChangeSet> result = new ArrayList<ChangeSet>();

        for (WorkspaceInfo workspaceInfo : getAllWorkspaces(build.getAction(ParametersAction.class))) {
            String normalizedWorkspaceName = normalizeWorkspace(
                workspaceInfo.getWorkspaceName(), build.getProject(), build);

            FilePath plasticWorkspace = new FilePath(workspace,
                normalizedWorkspaceName);

            if (!plasticWorkspace.exists())
                plasticWorkspace.mkdirs();

            Server server = new Server(new PlasticTool(
                getDescriptor().getCmExecutable(), launcher, listener, plasticWorkspace));

            WorkspaceConfiguration workspaceConfiguration = createWorkspaceConfiguration(
                    build, normalizedWorkspaceName, workspaceInfo.getSelector());

            if (build.getPreviousBuild() != null) {
                BuildWorkspaceConfiguration nodeConfiguration =
                    createBuildWorkspaceConfiguration(normalizedWorkspaceName, build);

                if (isWorkspaceDeleteNeeded(workspaceConfiguration, nodeConfiguration)) {
                    listener.getLogger().println(
                        "Deleting workspace as the configuration has changed since the last build on this computer.");
                    removeWorkspace(plasticWorkspace, server,
                        workspaceConfiguration, nodeConfiguration);
                }
            }

            result.addAll(checkoutWorkspace(build, plasticWorkspace, server, listener,
                workspaceConfiguration, workspaceInfo));
        }

        writeChangeLog(listener, changelogFile, result);
        return true;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
            Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(
            AbstractProject<?,?> project,
            Launcher launcher,
            FilePath workspacePath,
            TaskListener listener,
            SCMRevisionState state) {
        if (project.getLastBuild() == null) {
            listener.getLogger().println("No builds detected yet!");
            return BUILD_NOW;
        }

        Run<?, ?> lastCompletedBuild = project.getLastCompletedBuild();
        for (WorkspaceInfo workspaceInfo : getAllWorkspaces(project.getAction(ParametersAction.class))) {
            FilePath plasticWorkspace = new FilePath(workspacePath, normalizeWorkspace(
                workspaceInfo.getWorkspaceName(), project, lastCompletedBuild));
            if (HasChanges(launcher, plasticWorkspace, listener, workspaceInfo, lastCompletedBuild))
                return BUILD_NOW;
        }
        return NO_CHANGES;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    String normalizeWorkspace(
            String workspaceName,
            AbstractProject<?,?> project,
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

    private List<WorkspaceInfo> getAllWorkspaces(ParametersAction parameters) {
        List<WorkspaceInfo> result =  new ArrayList<WorkspaceInfo>();
        result.add(getFirstWorkspace());
        result.addAll(getAdditionalWorkspaces());
        return replaceBuildParameters(result, parameters);
    }

    private List<WorkspaceInfo> replaceBuildParameters(List<WorkspaceInfo> workspaces, ParametersAction parameters) {
        List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
        for (WorkspaceInfo wkInfo : workspaces) {
            if (wkInfo == null)
                continue;

            result.add(new WorkspaceInfo(
                    replaceParameters(wkInfo.getSelector(), parameters),
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

    private String replaceParameters(String selector, ParametersAction parameters) {
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
            AbstractBuild<?, ?> build,
            String normalizedWorkspaceName,
            String selector) {
        return new WorkspaceConfiguration(normalizedWorkspaceName, selector);
    }

    private BuildWorkspaceConfiguration createBuildWorkspaceConfiguration(
            String normalizedWorkspaceName,
            AbstractBuild<?, ?> build) {
        return BuildWorkspaceConfigurationRetriever.getLatestForNode(
            normalizedWorkspaceName, build.getBuiltOn(), build.getPreviousBuild());
    }

    private boolean isWorkspaceDeleteNeeded(
            WorkspaceConfiguration workspaceConfiguration,
            BuildWorkspaceConfiguration nodeConfiguration) {
        return (nodeConfiguration != null) && nodeConfiguration.workspaceExists()
            && (!workspaceConfiguration.equals(nodeConfiguration));
    }

    private void removeWorkspace(
            FilePath plasticWorkspace,
            Server server,
            WorkspaceConfiguration workspaceConfiguration,
            BuildWorkspaceConfiguration nodeConfiguration) throws IOException, InterruptedException {
        RemoveWorkspaceAction removeAction = new RemoveWorkspaceAction(
            workspaceConfiguration.getWorkspaceName());
        removeAction.remove(server);
        plasticWorkspace.deleteContents();

        nodeConfiguration.setWorkspaceWasRemoved();
        nodeConfiguration.save();
    }
    
    private List<ChangeSet> checkoutWorkspace(
            AbstractBuild<?, ?> build,
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
            BuildListener listener,
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

    private boolean HasChanges(
            Launcher launcher,
            FilePath workspacePath,
            TaskListener listener,
            WorkspaceInfo workspaceInfo,
            Run<?, ?> lastCompletedBuild) {
        PlasticTool plasticTool = new PlasticTool(getDescriptor().getCmExecutable(),
            launcher, listener, workspacePath); 
        Server server = new Server(plasticTool);
        try {
            List<ChangeSet> changesetsFromBuild = server.getBriefHistory(
                lastCompletedBuild.getTimestamp(), Calendar.getInstance());
            if (changesetsFromBuild.size() > 0)
                return true;
        } catch (Exception e) {
            e.printStackTrace(listener.error(workspaceInfo.getWorkspaceName()
                + ": Unable to retrieve workspace status."));
            return true;
        }
        return false;
    }

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