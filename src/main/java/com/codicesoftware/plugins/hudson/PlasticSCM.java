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

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
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
    private final List<WorkspaceInfo> workspaceInfos;

    private transient String normalizedWorkspace;

    private static final Logger logger = Logger.getLogger(PlasticSCM.class.getName());

    @DataBoundConstructor
    public PlasticSCM(List<WorkspaceInfo> workspaceInfos) {
        logger.info("Initializing PlasticSCM plugin");
        this.workspaceInfos = (workspaceInfos == null)
            ? new ArrayList<WorkspaceInfo>() : workspaceInfos;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeSetReader();
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher,
        FilePath workspace, BuildListener listener, File changelogFile)
        throws IOException, InterruptedException  {

        for (WorkspaceInfo workspaceInfo : getWorkspaceInfos()) {
            FilePath plasticWorkspace = new FilePath(workspace,
                workspaceInfo.getWorkspaceName());

            if (!plasticWorkspace.exists())
                plasticWorkspace.mkdirs();

            Server server = new Server(new PlasticTool(
                getDescriptor().getCmExecutable(), launcher, listener, plasticWorkspace));

            WorkspaceConfiguration workspaceConfiguration = new WorkspaceConfiguration(
                normalizeWorkspace(workspaceInfo.getWorkspaceName(),
                build, Computer.currentComputer()), workspaceInfo.getSelector());

            if (build.getPreviousBuild() != null) {
                BuildWorkspaceConfiguration nodeConfiguration =
                    BuildWorkspaceConfigurationRetriever.getLatestForNode(
                    workspaceInfo.getWorkspaceName(), build.getBuiltOn(), build.getPreviousBuild());

                if ((nodeConfiguration != null)
                    && nodeConfiguration.workspaceExists()
                    && (!workspaceConfiguration.equals(nodeConfiguration))) {
                        listener.getLogger().println(
                            "Deleting workspace as the configuration has changed since the last build on this computer.");
                        new RemoveWorkspaceAction(workspaceConfiguration.getWorkspaceName()).remove(server);
                        plasticWorkspace.deleteContents();
                        nodeConfiguration.setWorkspaceWasRemoved();
                        nodeConfiguration.save();
                }
            }

            build.addAction(workspaceConfiguration);
            CheckoutAction action = new CheckoutAction(
                    workspaceConfiguration.getWorkspaceName(),
                    workspaceConfiguration.getSelector(),
                    workspaceInfo.isUseUpdate());
            try {
                List<ChangeSet> list = action.checkout(
                    server, plasticWorkspace,
                    (build.getPreviousBuild() != null? build.getPreviousBuild().getTimestamp(): null),
                    build.getTimestamp());
                
                ChangeSetWriter writer = new ChangeSetWriter();
                writer.write(list, changelogFile);
            } catch (ParseException e) {
                listener.fatalError(e.getMessage());
                throw new AbortException();
            }
        }
        return true;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
            Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(AbstractProject<?,?> project,
            Launcher launcher, FilePath workspacePath, TaskListener listener, SCMRevisionState state) {

        if (project.getLastBuild() == null) {
            listener.getLogger().println("No builds detected yet!");
            return BUILD_NOW;
        }

        Run<?, ?> lastCompletedBuild = project.getLastCompletedBuild();
        for (WorkspaceInfo workspaceInfo : getWorkspaceInfos()) {    
            FilePath plasticWorkspace = new FilePath(
                    workspacePath, workspaceInfo.getWorkspaceName());

            Server server = new Server(new PlasticTool(
                    getDescriptor().getCmExecutable(), launcher, listener, plasticWorkspace));
            try {
                List<ChangeSet> changesetsFromBuild = server.getBriefHistory(
                    lastCompletedBuild.getTimestamp(), Calendar.getInstance());
                if (changesetsFromBuild.size() > 0)
                    return BUILD_NOW;
            } catch (Exception e) {
                e.printStackTrace(listener.error(workspaceInfo.getWorkspaceName()
                    + ": Unable to retrieve workspace status."));
                return BUILD_NOW;
            }
        }
        return NO_CHANGES;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    String normalizeWorkspace(String workspaceName, AbstractBuild<?,?> build, Computer computer) {
        normalizedWorkspace = workspaceName;

        if (build != null) {
            normalizedWorkspace = replaceBuildParameter(build, normalizedWorkspace);
            normalizedWorkspace = Util.replaceMacro(normalizedWorkspace, new BuildVariableResolver(build.getProject(), computer));
        }
        normalizedWorkspace = normalizedWorkspace.replaceAll("[\"/:<>\\|\\*\\?]+", "_");
        normalizedWorkspace = normalizedWorkspace.replaceAll("[\\.\\s]+$", "_");

        return normalizedWorkspace;
    }

    public List<WorkspaceInfo> getWorkspaceInfos() {
        return workspaceInfos;
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

        public boolean isUseUpdate() {
            return useUpdate;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<WorkspaceInfo> {
            private static final Pattern workspaceRegex = Pattern.compile("^[^@#/:]+$");
            private static final Pattern selectorRegex = Pattern.compile("^(\\s*(rep|repository)\\s+\"(.*)\"(\\s+mount\\s+\"(.*)\")?(\\s+path\\s+\"(.*)\"(\\s+norecursive)?(\\s+((((((branch|br)\\s+\"(.*)\")(\\s+(revno\\s+(\"\\d+\"|LAST|FIRST)|changeset\\s+\"\\S+\"))?(\\s+(label|lb)\\s+\"(.*)\")?)|(label|lb)\\s+\"(.*)\")(\\s+(checkout|co)\\s+\"(.*\"))?)|(branchpertask\\s+\"(.*)\"(\\s+baseline\\s+\"(.*)\")?)|(smartbranch\\s+\"(.*)\"))))+\\s*)+$", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
            private static final String DEFAULT_SELECTOR = "repository \"jenkins2\"\n  path \"/\"\n    br \"/main\"\n    co \"/main\"";

            @Override
            public String getDisplayName() {
                return "Workspace Info";
            }

            private FormValidation doRegexCheck(final Pattern regex, final String noMatchText,
                    final String nullText, String value) {
                value = Util.fixEmpty(value);
                if (value == null)
                    return FormValidation.error(nullText);

                if (regex.matcher(value).matches())
                    return FormValidation.ok();

                return FormValidation.error(noMatchText);
            }

            public FormValidation doCheckWorkspaceName(@QueryParameter final String value) {
                return doRegexCheck(workspaceRegex, "Workspace name should not include @, #, / or :",
                    "Workspace name is mandatory", value);
            }

            public FormValidation doCheckSelector(@QueryParameter final String value) {
                return doRegexCheck(selectorRegex, "Selector is not in valid format",
                    "Selector is mandatory", value);
            }

            public String getDefaultSelector() {
                return DEFAULT_SELECTOR;
            }

            public String nextId() {
                return String.format("PlasticSCM-Jenkins-%s", UUID.randomUUID());
            }
        }
    }
}