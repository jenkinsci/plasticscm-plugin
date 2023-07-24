package com.codicesoftware.plugins.hudson.util;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.codicesoftware.plugins.hudson.ClientConfigurationArguments;
import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import com.codicesoftware.plugins.jenkins.tools.CmTool;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class FormChecker {
    private static final Logger LOGGER = Logger.getLogger(FormChecker.class.getName());

    private static final Pattern SELECTOR_REGEX = Pattern.compile(
        "^(\\s*(rep|repository)\\s+\"(.*)\"(\\s+mount\\s+\"(.*)\")?(\\s+path\\s+"
        + "\"(.*)\"(\\s+norecursive)?(\\s+((((((branch|br)\\s+\"(.*)\")(\\s+(revno\\s+"
        + "(\"\\d+\"|LAST|FIRST)|changeset\\s+\"\\S+\"))?(\\s+(label|lb)\\s+\"(.*)\")?)|"
        + "(label|lb)\\s+\"(.*)\")(\\s+(checkout|co)\\s+\"(.*)\")?)|shelve\\s+\"(.*)\"|"
        + "(branchpertask\\s+\"(.*)\"(\\s+baseline\\s+\"(.*)\")?)|(smartbranch\\s+\"(.*)\"))))+\\s*)+$",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NO_AT_CHAR_REGEX = Pattern.compile("^[^@]+$");
    private static final Pattern SERVER_REGEX = Pattern.compile("^.+:[0-9]+$");
    private static final Pattern DIRECTORY_REGEX = Pattern.compile("^[A-Za-z0-9\\-\\_]+$");

    private FormChecker() { }

    public static FormValidation doCheckSelector(String value) {
        return doRegexCheck(SELECTOR_REGEX, "Selector is not in valid format",
            "Selector is mandatory", value);
    }

    public static FormValidation doCheckBranch(String value) {
        return doRegexCheck(
            NO_AT_CHAR_REGEX,
            "Branch is not in a valid format ('@' characters are not valid)",
            "The branch name is mandatory",
            value);
    }

    public static FormValidation doCheckRepository(String value) {
        return doRegexCheck(
            NO_AT_CHAR_REGEX,
            "Repository name is not in a valid format ('@' characters are not valid)",
            "The repository name is mandatory",
            value);
    }

    public static FormValidation doCheckServer(String value) {
        return doRegexCheck(
            SERVER_REGEX,
            "Server name is not in a valid format (<address>:<port>)",
            "The server name is mandatory",
            value);
    }

    public static FormValidation doCheckDirectory(String value, Item item) {
        item.checkPermission(Item.CONFIGURE);
        return doRegexCheck(
                DIRECTORY_REGEX,
                "Directory name is not in a valid format (only A-Z, a-z, 0-9, '-' and '_' are allowed)",
                "The directory name is mandatory",
                value);
    }

    private static FormValidation doRegexCheck(
            final Pattern regex,
            final String noMatchText,
            final String nullText,
            String value) {
        value = Util.fixEmpty(value);
        if (value == null) {
            return FormValidation.error(nullText);
        }
        if (regex.matcher(value).matches()) {
            return FormValidation.ok();
        }
        return FormValidation.error(noMatchText);
    }

    public static FormValidation doCheckCredentialsId(
            Item item,
            String value,
            String server,
            WorkingMode workingMode) throws IOException, InterruptedException {
        if (item == null) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
        }
        if (Util.fixEmpty(value) == null) {
            if (workingMode == WorkingMode.NONE) {
                return FormValidation.ok();
            }
            return FormValidation.error(workingMode.getLabel() + " requires credentials");
        }
        StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                item,
                item instanceof Queue.Task
                    ? Tasks.getAuthenticationOf((Queue.Task) item)
                    : ACL.SYSTEM,
                URIRequirementBuilder.create().build()),
            CredentialsMatchers.withId(value));

        if (credentials == null) {
            return FormValidation.error("Cannot find currently selected credentials");
        }

        ClientConfigurationArguments clientConfArgs = new ClientConfigurationArguments(
            workingMode, credentials, server);

        TaskListener listener = new LogTaskListener(LOGGER, Level.INFO);
        Launcher launcher = new Launcher.LocalLauncher(listener);

        PlasticTool tool = new PlasticTool(
            CmTool.get(Jenkins.getInstance(), new EnvVars(EnvVars.masterEnvVars), listener),
            launcher,
            listener,
            Jenkins.getInstance().getRootPath(),
            clientConfArgs);

        try {
            // execute will return a Reader or throw an exception - never null
            tool.execute(new String[] {"repo", "list", server}).close();
            return FormValidation.ok();
        } catch (InterruptedException | IOException e) {
            return FormValidation.error(e.getMessage());
        }
    }
}
