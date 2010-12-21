package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

public class GetSelectorCommand extends AbstractCommand implements ParseableCommand<String> {
    private final String workspaceName;

    public GetSelectorCommand(ServerConfigurationProvider provider, String workspaceName) {
        super(provider);
        this.workspaceName = workspaceName;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("ss");
        arguments.add("wk:" + workspaceName);

        return arguments;
    }

    public String parse(Reader r) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(r);
        StringBuilder builder = new StringBuilder();
        String line = reader.readLine();

        /* Ignore the first line, it just says "Selector for workspace foo:" */
        line = reader.readLine();
        while (line != null) {
            builder.append(line);
            line = reader.readLine();
        }

        return builder.toString();
    }
}