package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.jenkins.tools.CmTool;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class that encapsulates the Plastic SCM command client.
 */
public class PlasticTool {

    private static final Logger LOGGER = Logger.getLogger(PlasticTool.class.getName());

    private static final int MAX_RETRIES = 3;
    private static final int TIME_BETWEEN_RETRIES = 1000;

    @CheckForNull
    private final CmTool tool;
    @Nonnull
    private final Launcher launcher;
    @Nonnull
    private final TaskListener listener;
    @CheckForNull
    private final FilePath workspace;
    @Nonnull
    private final ClientConfigurationArguments clientConfigurationArguments;

    public PlasticTool(
        @CheckForNull CmTool tool,
        @Nonnull Launcher launcher,
        @Nonnull TaskListener listener,
        @CheckForNull FilePath workspace,
        @Nonnull ClientConfigurationArguments clientConfigurationArguments) {
        this.tool = tool;
        this.launcher = launcher;
        this.listener = listener;
        this.workspace = workspace;
        this.clientConfigurationArguments = clientConfigurationArguments;
    }

    /**
     * Execute the arguments, and return the console output as a Reader
     *
     * @param arguments arguments to send to the command-line client.
     * @return a Reader containing the console output
     * @throws IOException Operation error
     * @throws InterruptedException Process has been interrupted
     */
    @Nonnull
    public Reader execute(@Nonnull String[] arguments) throws IOException, InterruptedException {
        return execute(arguments, null, true);
    }

    @Nonnull
    public Reader execute(
            @Nonnull String[] arguments,
            @CheckForNull FilePath executionPath,
            boolean printOutput) throws IOException, InterruptedException {
        if (tool == null) {
            throw new InterruptedException("You need to specify a Plastic SCM tool");
        }

        ArgumentListBuilder cmdArgs = getToolArguments(arguments, clientConfigurationArguments);
        String cliLine = cmdArgs.toString();

        int retries = 0;
        while (retries < MAX_RETRIES) {
            Reader result = tryExecute(cmdArgs, executionPath, printOutput);
            if (result != null) {
                return result;
            }

            retries++;
            LOGGER.warning(String.format(
                    "The cm command '%s' failed. Retrying after %d ms... (%d)",
                    cliLine, TIME_BETWEEN_RETRIES, retries));
            Thread.sleep(TIME_BETWEEN_RETRIES);
        }

        String errorMessage = String.format(
                "The cm command '%s' failed after %d retries", cliLine, MAX_RETRIES);
        listener.fatalError(errorMessage);
        throw new AbortException(errorMessage);
    }

    @Nonnull
    private ArgumentListBuilder getToolArguments(
            @Nonnull String[] cmArgs,
            @Nonnull ClientConfigurationArguments clientConfigurationArguments) {
        if (tool == null) {
            return new ArgumentListBuilder();
        }
        ArgumentListBuilder result = new ArgumentListBuilder(tool.getCmPath());

        result.add(cmArgs);
        return clientConfigurationArguments.fillParameters(result);
    }

    @Nullable
    private Reader tryExecute(
            ArgumentListBuilder args,
            FilePath executionPath,
            boolean printOutput) throws IOException, InterruptedException {
        if (tool == null) {
            return null;
        }

        if (executionPath == null) {
            executionPath = workspace;
        }
        ByteArrayOutputStream consoleStream = new ByteArrayOutputStream();

        Launcher.ProcStarter procL = launcher.launch()
            .cmds(args)
            .stdout(printOutput ? new ForkOutputStream(consoleStream, listener.getLogger()) : consoleStream)
            .pwd(executionPath);

        if (tool.isUseInvariantCulture()) {
            Map<String, String> envsMap = new HashMap<>();
            envsMap.put("DOTNET_SYSTEM_GLOBALIZATION_INVARIANT", "1");
            procL.envs(envsMap);
        }

        Proc proc = procL.start();
        consoleStream.close();

        if (proc.join() == 0) {
            LOGGER.fine("Command succeeded: " + args);
            return new InputStreamReader(new ByteArrayInputStream(consoleStream.toByteArray()), StandardCharsets.UTF_8);
        } else {
            LOGGER.fine("Command failed: " + args);
            return null;
        }
    }
}
