package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class GetWorkspaceFromPathCommand extends AbstractCommand implements ParseableCommand<String> {
    private String mWkPath;

    public GetWorkspaceFromPathCommand(ServerConfigurationProvider provider, String wkPath) {
        super(provider);
        mWkPath = wkPath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("gwp");
        arguments.add(mWkPath);
        arguments.add("--format={1}");

        return arguments;
    }

    public String parse(Reader r) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(r);
        return reader.readLine();
    }
}