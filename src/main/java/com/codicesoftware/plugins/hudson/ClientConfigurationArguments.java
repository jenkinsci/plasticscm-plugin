package com.codicesoftware.plugins.hudson;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClientConfigurationArguments {
    @Nullable
    private final WorkingMode workingMode;
    @Nullable
    private final StandardUsernamePasswordCredentials credentials;
    @Nullable
    private final String server;

    public ClientConfigurationArguments(
            @Nullable WorkingMode workingMode,
            @Nullable StandardUsernamePasswordCredentials credentials,
            @Nullable String server) {
        this.workingMode = workingMode;
        this.credentials = credentials;
        this.server = Util.fixEmpty(server);
    }

    @Nonnull
    public ArgumentListBuilder fillParameters(@Nonnull ArgumentListBuilder args) {
        if (hasServerValue()) {
            args.add(getServerParam());
        }

        if (hasWorkingModeManualValues()) {
            args.add(getWorkingModeParam());
            args.add(getUserParam());
            args.addMasked(getPasswordParam());
        }
        return args;
    }

    @Nonnull
    String getServerParam() {
        return "-wks=" + server;
    }

    @Nonnull
    String getWorkingModeParam() {
        return "--workingmode=" + workingMode.getPlasticWorkingMode();
    }

    @Nonnull
    String getUserParam() {
        return "--username=" + credentials.getUsername();
    }

    @Nonnull
    String getPasswordParam() {
        return "--password=" + Secret.toString(credentials.getPassword());
    }

    boolean hasWorkingModeManualValues() {
        return workingMode != WorkingMode.NONE && credentials != null;
    }

    boolean hasServerValue() {
        return server != null;
    }
}
