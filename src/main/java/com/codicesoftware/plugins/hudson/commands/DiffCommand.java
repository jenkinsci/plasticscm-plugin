package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.model.ChangeSet;
import com.codicesoftware.plugins.hudson.model.ObjectSpec;
import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiffCommand implements ParseableCommand<List<ChangeSet.Item>> {

    private static final String startOfLine = ">>>";
    private static final String endOfLine = "<<<";
    private static final String separator = "@sep@";
    private static final Pattern linePattern = Pattern.compile(
            String.format("^%1$s(.*)%2$s([0-9]+)%2$s(-?[0-9]+)%2$s([ACDM])%3$s$", startOfLine, separator, endOfLine),
            Pattern.UNICODE_CHARACTER_CLASS);

    @Nonnull
    private final ObjectSpec objectSpec;

    public DiffCommand(@Nonnull final ObjectSpec objectSpec) {
        this.objectSpec = objectSpec;
    }

    @Nonnull
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("diff");
        arguments.add(objectSpec.getFullSpec());
        arguments.add("--repositorypaths");
        arguments.add(String.format(
            "--format=%1$s{path}%2$s{revid}%2$s{parentrevid}%2$s{status}%3$s", startOfLine, separator, endOfLine));

        return arguments;
    }

    @Override
    @Nonnull
    public List<ChangeSet.Item> parse(@Nonnull final Reader r) throws IOException {
        ArrayList<ChangeSet.Item> result = new ArrayList<>();

        BufferedReader reader = new BufferedReader(r);
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = linePattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            result.add(new ChangeSet.Item(
                matcher.group(1), matcher.group(2), matcher.group(3), convertStatus(matcher.group(4))));
        }

        return result;
    }

    @Nonnull
    private static String convertStatus(@Nonnull final String lineStatus) {
        switch (lineStatus) {
            case "A":
                return "added";
            case "C":
                return "changed";
            case "D":
                return "deleted";
            case "M":
                return "moved";
            default:
                return "unknown";
        }
    }
}
