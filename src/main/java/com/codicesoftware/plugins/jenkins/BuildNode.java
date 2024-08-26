package com.codicesoftware.plugins.jenkins;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;

public class BuildNode {

    private BuildNode() {
    }

    @Nonnull
    public static Node getFromWorkspacePath(@Nonnull final FilePath workspacePath) {
        Jenkins jenkins = Jenkins.get();

        if (!workspacePath.isRemote()) {
            return jenkins;
        }

        for (Computer computer : jenkins.getComputers()) {
            if (computer.getChannel() != workspacePath.getChannel()) {
                continue;
            }

            Node node = computer.getNode();
            if (node != null) {
                return node;
            }
        }
        return jenkins;
    }
}
