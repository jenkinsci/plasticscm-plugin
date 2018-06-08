package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.PlasticTool;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class CommandRunner {
    public static Reader execute(PlasticTool tool, Command cmd) throws IOException, InterruptedException {
        return execute(tool, cmd, null);
    }

    public static Reader execute(PlasticTool tool, Command cmd, String executionPath) throws IOException, InterruptedException {
        return tool.execute(
                cmd.getArguments().toCommandArray(),
                executionPath==null ? null : new FilePath(Computer.currentComputer().getChannel(),executionPath));
    }

    public static <T> T executeAndRead(PlasticTool tool, Command command, ParseableCommand<T> parser)
            throws IOException, InterruptedException, ParseException {
        return executeAndRead(tool, command, parser, null);
    }

    public static <T> T executeAndRead(
            PlasticTool tool,
            Command command,
            ParseableCommand<T> parser,
            String executionPath)
            throws IOException, InterruptedException, ParseException {
        Reader reader = null;
        try {
            reader = execute(tool, command, executionPath);
            return parser.parse(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
