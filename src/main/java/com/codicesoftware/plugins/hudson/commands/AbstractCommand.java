package com.codicesoftware.plugins.hudson.commands;

public abstract class AbstractCommand implements Command {
    private final ServerConfigurationProvider config;

    public AbstractCommand(ServerConfigurationProvider configurationProvider) {
        this.config = configurationProvider;
    }

    public ServerConfigurationProvider getConfig() {
        return config;
    }
}