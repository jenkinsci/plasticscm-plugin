package com.codicesoftware.plugins.hudson;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.util.ForkOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Class that encapsulates the Plastic SCM command client.
 */
public class PlasticTool {

    private static final Logger LOGGER = Logger.getLogger(PlasticTool.class.getName());

    private static final int MAX_RETRIES = 3;
    private static final int TIME_BETWEEN_RETRIES = 1000;

    private String executable;
    private Launcher launcher;
    private TaskListener listener;
    private FilePath workspace;

    public PlasticTool(String executable, Launcher launcher, TaskListener listener, FilePath workspace) {
        this.executable = executable;
        this.launcher = launcher;
        this.listener = listener;
        this.workspace = workspace;
    }

    /**
     * Execute the arguments, and return the console output as a Reader
     *
     * @param arguments arguments to send to the command-line client.
     * @return a Reader containing the console output
     * @throws IOException
     * @throws InterruptedException
     */
    public Reader execute(String[] arguments) throws IOException, InterruptedException {
        return execute(arguments, null, true);
    }

    public Reader execute(String[] arguments, FilePath executionPath, boolean printOutput) throws IOException, InterruptedException {
        String[] cmdArgs = getToolArguments(arguments);
        String cliLine = getCliLine(cmdArgs);

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

    private String[] getToolArguments(String[] cmArgs) {
        String[] result = new String[cmArgs.length + 1];
        result[0] = executable;
        System.arraycopy(cmArgs, 0, result, 1, cmArgs.length);
        return result;
    }

    private String getCliLine(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append(arg);
        }
        return builder.toString();
    }

    private Reader tryExecute(String[] cmdArgs, FilePath executionPath, boolean printOutput)
            throws IOException, InterruptedException {
        if (executionPath == null) {
            executionPath = workspace;
        }
        ByteArrayOutputStream consoleStream = new ByteArrayOutputStream();
        Proc proc = launcher.launch().cmds(cmdArgs)
                .stdout(printOutput ? new ForkOutputStream(consoleStream, listener.getLogger()) : consoleStream )
                .pwd(executionPath).start();
        consoleStream.close();

        if (proc.join() == 0) {
            LOGGER.fine("Command succeeded: " + String.join(" ", cmdArgs));
            return new InputStreamReader(new ByteArrayInputStream(consoleStream.toByteArray()), StandardCharsets.UTF_8);
        } else {
            LOGGER.fine("Command failed: " + String.join(" ", cmdArgs));
            return null;
        }
    }
}
