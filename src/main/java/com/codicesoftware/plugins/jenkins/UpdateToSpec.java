package com.codicesoftware.plugins.jenkins;

import hudson.AbortException;
import hudson.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateToSpec {

    private static final Pattern specPattern = Pattern.compile(
        "^((lb|cs|sh|br):|)([^@]+)@(rep:)?([^@]+)@(repserver:)?(.+)$");

    private final ObjectSpecType specObjectType;
    @Nonnull
    private final String objectName;
    @Nonnull
    private final String repName;
    @Nonnull
    private final String repServer;

    public String getFullObjectSpec() {
        return String.format("%s:%s@%s@%s", specObjectType.toSpecObject(), objectName, repName, repServer);
    }

    @Nonnull
    public String getObjectName() {
        return objectName;
    }

    @Nonnull
    public String getRepName() {
        return repName;
    }

    @Nonnull
    public String getRepServer() {
        return repServer;
    }

    public ObjectSpecType getObjectType() {
        return specObjectType;
    }

    @Nullable
    public static UpdateToSpec parse(@Nullable final String updateToSpecString) throws AbortException {
        if (updateToSpecString == null || updateToSpecString.isEmpty()) {
            return null;
        }

        Matcher matcher = specPattern.matcher(updateToSpecString);
        if (!matcher.matches()) {
            throw new AbortException("Invalid object spec: " + updateToSpecString);
        }

        return new UpdateToSpec(
            parseSpecObjectType(matcher.group(2)), matcher.group(3), matcher.group(5), matcher.group(7));
    }

    private UpdateToSpec(
            @Nonnull final ObjectSpecType specObjectType,
            @Nonnull final String objectName,
            @Nonnull final String repName,
            @Nonnull final String repServer) {
        this.specObjectType = specObjectType;
        this.objectName = objectName;
        this.repName = repName;
        this.repServer = repServer;
    }

    private static ObjectSpecType parseSpecObjectType(final String objectName) {
        String nonNullObjectType = Util.fixNull(objectName);
        if (nonNullObjectType.isEmpty() || nonNullObjectType.equals("br")) {
            return ObjectSpecType.Branch;
        }

        if (nonNullObjectType.equals("cs")) {
            return ObjectSpecType.Changeset;
        }

        if (nonNullObjectType.equals("lb")) {
            return ObjectSpecType.Label;
        }

        if (nonNullObjectType.equals("sh")) {
            return ObjectSpecType.Shelve;
        }

        throw new IllegalArgumentException("Invalid object type: " + objectName);
    }
}
