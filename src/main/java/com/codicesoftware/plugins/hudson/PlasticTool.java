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
import java.util.logging.Logger;

/**
 * Class that encapsulates the Plastic SCM command client.
 *
 * @author Erik Ramfelt
 * @author Dick Porter
 */
public class PlasticTool {
    private final String executable;
    private Launcher launcher;
    private TaskListener listener;
    private FilePath workspace;

    private static final int MAX_RETRIES = 3;
    private static final int TIME_BETWEEN_RETRIES = 500;

    private static final Logger logger = Logger.getLogger(PlasticTool.class.getName());

    public PlasticTool(String executable, Launcher launcher, TaskListener listener,
            FilePath workspace) {
        this.executable = executable;
        this.launcher = launcher;
        this.listener = listener;
        this.workspace = workspace;
    }

    /**
     * Execute the arguments, and return the console output as a Reader
     * @param arguments arguments to send to the command-line client.
     * @return a Reader containing the console output
     * @throws IOException
     * @throws InterruptedException
     */
    public Reader execute(String[] arguments) throws IOException, InterruptedException {
        return execute(arguments, null);
    }
    public Reader execute(String[] arguments, FilePath executionPath) throws IOException, InterruptedException {
        String[] cmdArgs = getToolArguments(arguments);

        int retries = 0;
        while (retries < MAX_RETRIES) {
            Reader result = tryExecute(cmdArgs, executionPath);
            if (result != null)
                return result;

            retries++;
            logger.warning(String.format(
                "The cm command '%s' failed. Retrying after %d ms... (%d)",
                arguments[0], TIME_BETWEEN_RETRIES, retries));
            Thread.sleep(TIME_BETWEEN_RETRIES);
        }
        listener.fatalError(String.format(
            "The cm command '%s' failed after %d retries", arguments[0], MAX_RETRIES));
        throw new AbortException();
    }

    private String[] getToolArguments(String[] cmArgs) {
        String[] result = new String[cmArgs.length + 1];
        result[0] = executable;
        System.arraycopy(cmArgs, 0, result, 1, cmArgs.length);
        return result;
    }

    private Reader tryExecute(String[] cmdArgs, FilePath executionPath) throws IOException, InterruptedException {
        if(executionPath == null)
            executionPath = workspace;
        ByteArrayOutputStream consoleStream = new ByteArrayOutputStream();
        Proc proc = launcher.launch().cmds(cmdArgs)
                .stdout(new ForkOutputStream(consoleStream, listener.getLogger()))
                .pwd(executionPath).start();
        consoleStream.close();

        if (proc.join() == 0)
            return new InputStreamReader(new ByteArrayInputStream(consoleStream.toByteArray()));
        return null;
    }
}