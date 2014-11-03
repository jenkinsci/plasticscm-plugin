package com.codicesoftware.plugins.hudson;

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
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * SCM for Plastic SCM
 *
 * Based on the tfs plugin by Erik Ramfelt
 *
 * @author Dick Porter
 */
public class PlasticSCM extends SCM {
    private final String workspaceName;
    private final String selector;
    private final String workfolder;
    private final boolean useUpdate;

    private transient String normalizedWorkspace;

    private static final Logger logger = Logger.getLogger(PlasticSCM.class.getName());

    @DataBoundConstructor
    public PlasticSCM(String workspaceName, String selector, String workfolder, boolean useUpdate) {
        this.workspaceName = (Util.fixEmptyAndTrim(workspaceName) == null ? "Jenkins-${JOB_NAME}-${NODE_NAME}" : workspaceName);
        
        this.selector = selector;
        this.workfolder = workfolder;
        
        this.useUpdate = useUpdate;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getSelector() {
        return selector;
    }

    public String getWorkfolder() {
        return workfolder;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeSetReader();
    }
    
    String getWorkspace(AbstractBuild<?,?> build, Computer computer) {
        normalizedWorkspace = workspaceName;
        
        if (build != null) {
            normalizedWorkspace = substituteBuildParameter(build, normalizedWorkspace);
            normalizedWorkspace = Util.replaceMacro(normalizedWorkspace, new BuildVariableResolver(build.getProject(), computer));
        }
        normalizedWorkspace = normalizedWorkspace.replaceAll("[\"/:<>\\|\\*\\?]+", "_");
        normalizedWorkspace = normalizedWorkspace.replaceAll("[\\.\\s]+$", "_");

        return normalizedWorkspace;
    }

    private String substituteBuildParameter(Run<?,?> run, String text) {
        if (run instanceof AbstractBuild<?,?>) {
            AbstractBuild<?,?> build = (AbstractBuild<?,?>)run;
            if (build.getAction(ParametersAction.class) != null) {
                return build.getAction(ParametersAction.class).substitute(build, text);
            }
        }

        return text;
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspaceFilePath,
            BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        Server server = new Server(new PlasticTool(getDescriptor().getCmExecutable(), launcher, listener, workspaceFilePath));
        WorkspaceConfiguration workspaceConfiguration = new WorkspaceConfiguration(getWorkspace(build, Computer.currentComputer()), selector, workfolder);

        if (build.getPreviousBuild() != null) {
            BuildWorkspaceConfiguration nodeConfiguration = new BuildWorkspaceConfigurationRetriever().getLatestForNode(build.getBuiltOn(), build.getPreviousBuild());
            if ((nodeConfiguration != null) &&
                    nodeConfiguration.workspaceExists() &&
                    (!workspaceConfiguration.equals(nodeConfiguration))) {
                listener.getLogger().println("Deleting workspace as the configuration has changed since the last build on this computer.");
                new RemoveWorkspaceAction(workspaceConfiguration.getWorkspaceName()).remove(server);
                workspaceFilePath.deleteContents();
                nodeConfiguration.setWorkspaceWasRemoved();
                nodeConfiguration.save();
            }
        }

        build.addAction(workspaceConfiguration);
        CheckoutAction action = new CheckoutAction(workspaceConfiguration.getWorkspaceName(), workspaceConfiguration.getSelector(), workspaceConfiguration.getWorkfolder(), isUseUpdate());
        try {
            List<ChangeSet> list = action.checkout(server, workspaceFilePath, (build.getPreviousBuild() != null? build.getPreviousBuild().getTimestamp(): null), build.getTimestamp());
            ChangeSetWriter writer = new ChangeSetWriter();
            writer.write(list, changelogFile);
        } catch (ParseException e) {
            listener.fatalError(e.getMessage());
            throw new AbortException();
        }
        return true;
    }

    @Override
    public boolean pollChanges(AbstractProject hudsonProject, Launcher launcher,
            FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        Run<?,?> lastRun = hudsonProject.getLastBuild();
        if (lastRun == null) {
            return true;
        } else {
            Server server = new Server(new PlasticTool(getDescriptor().getCmExecutable(), launcher, listener, workspace + File.separator + workfolder));
            try {
                return (server.getBriefHistory(lastRun.getTimestamp(), Calendar.getInstance()).size() > 0);
            } catch (ParseException e) {
                listener.fatalError(e.getMessage());
                throw new AbortException();
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<PlasticSCM> {
        private static final Pattern workspaceRegex = Pattern.compile("^[^@#/:]+$");
        private static final Pattern selectorRegex = Pattern.compile("^(\\s*(rep|repository)\\s+\"(.*)\"(\\s+mount\\s+\"(.*)\")?(\\s+path\\s+\"(.*)\"(\\s+norecursive)?(\\s+((((((branch|br)\\s+\"(.*)\")(\\s+(revno\\s+(\"\\d+\"|LAST|FIRST)|changeset\\s+\"\\S+\"))?(\\s+(label|lb)\\s+\"(.*)\")?)|(label|lb)\\s+\"(.*)\")(\\s+(checkout|co)\\s+\"(.*\"))?)|(branchpertask\\s+\"(.*)\"(\\s+baseline\\s+\"(.*)\")?)|(smartbranch\\s+\"(.*)\"))))+\\s*)+$", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);

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

        private FormValidation doRegexCheck(final Pattern[] regexArray, final String noMatchText,
                final String nullText, String value) {
            value = Util.fixEmpty(value);
            if (value == null) {
                if (nullText == null) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error(nullText);
                }
            }

            for (Pattern regex : regexArray) {
                if (regex.matcher(value).matches()) {
                    return FormValidation.ok();
                }
            }

            return FormValidation.error(noMatchText);
        }

        public FormValidation doExecutableCheck(@QueryParameter final String value) {
            return FormValidation.validateExecutable(value);
        }

        public FormValidation doWorkspaceCheck(@QueryParameter final String value) {
            return doRegexCheck(new Pattern[]{workspaceRegex},
                    "Workspace name should not include @, #, / or :", null, value);
        }

        public FormValidation doSelectorCheck(@QueryParameter final String value) {
            return doRegexCheck(new Pattern[]{selectorRegex},
                    "Selector is not in valid format",
                    "Selector is mandatory", value);
        }
        
        public String getDisplayName() {
            return "Plastic SCM";
        }
    }
}
