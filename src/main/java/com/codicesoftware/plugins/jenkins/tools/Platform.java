package com.codicesoftware.plugins.jenkins.tools;

import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;

import java.io.IOException;
import java.util.Locale;

public enum Platform {
    LINUX("cm", "linux.tar.gz"),
    WINDOWS("cm.exe", "windows.zip"),
    MACOS("cm", "macos.tar.gz"),
    OTHER("", "");

    private final String toolName;
    private final String downloadPlatform;

    Platform(String toolName, String downloadPlatform) {
        this.toolName = toolName;
        this.downloadPlatform = downloadPlatform;
    }

    public static Platform of(Node node) throws IOException, InterruptedException {
        VirtualChannel channel = node.getChannel();
        if (channel == null) {
            throw new IOException("Unable to open channel to node " + node.getDisplayName());
        }
        return channel.call(new Platform.GetCurrentPlatform());
    }

    public static Platform current() {
        String arch = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (arch.contains("linux")) {
            return LINUX;
        }

        if (arch.contains("windows")) {
            return WINDOWS;
        }

        if (arch.contains("mac")) {
            return MACOS;
        }

        return OTHER;
    }

    public String getToolName() {
        return toolName;
    }

    public String getDownloadPlatform() {
        return downloadPlatform;
    }

    public boolean isUnix() {
        return this == LINUX || this == MACOS;
    }

    static class GetCurrentPlatform extends MasterToSlaveCallable<Platform, InterruptedException> {
        private static final long serialVersionUID = 1L;

        public Platform call() {
            return current();
        }
    }
}
