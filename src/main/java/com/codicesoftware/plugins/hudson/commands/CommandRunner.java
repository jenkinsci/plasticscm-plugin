package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.PlasticTool;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class CommandRunner {

    private CommandRunner() { }

    public static Reader execute(PlasticTool tool, Command cmd) throws IOException, InterruptedException {
        return execute(tool, cmd, null);
    }

    public static Reader execute(PlasticTool tool, Command cmd, FilePath executionPath)
            throws IOException, InterruptedException {
        return tool.execute(cmd.getArguments().toCommandArray(), executionPath);
    }

    public static <T> T executeAndRead(PlasticTool tool, Command command, ParseableCommand<T> parser)
            throws IOException, InterruptedException, ParseException {
        return executeAndRead(tool, command, parser, null);
    }

    public static <T> T executeAndRead(
            PlasticTool tool,
            Command command,
            ParseableCommand<T> parser,
            FilePath executionPath)
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
