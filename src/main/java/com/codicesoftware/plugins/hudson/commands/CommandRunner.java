package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.PlasticTool;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class CommandRunner {

    private CommandRunner() { }

    public static Reader execute(PlasticTool tool, Command command) throws IOException, InterruptedException {
        return execute(tool, command, null, true);
    }

    public static Reader execute(PlasticTool tool, Command command, FilePath executionPath)
            throws IOException, InterruptedException {
        return tool.execute(command.getArguments().toCommandArray(), executionPath, true);
    }

    public static Reader execute(PlasticTool tool, Command command, FilePath executionPath, boolean printOutput)
            throws IOException, InterruptedException {
        return tool.execute(command.getArguments().toCommandArray(), executionPath, printOutput);
    }

    public static <T> T executeAndRead(PlasticTool tool, ParseableCommand<T> command)
            throws IOException, InterruptedException, ParseException {
        return executeAndRead(tool, command, null, true);
    }

    public static <T> T executeAndRead(PlasticTool tool, ParseableCommand<T> command, boolean printOutput)
            throws IOException, InterruptedException, ParseException {
        return executeAndRead(tool, command, null, printOutput);
    }

    public static <T> T executeAndRead(PlasticTool tool, ParseableCommand<T> command, FilePath executionPath, boolean printOutput)
            throws IOException, InterruptedException, ParseException {
        Reader reader = null;
        try {
            reader = execute(tool, command, executionPath, printOutput);
            return command.parse(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
