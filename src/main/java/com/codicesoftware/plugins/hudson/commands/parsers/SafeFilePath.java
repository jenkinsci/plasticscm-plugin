package com.codicesoftware.plugins.hudson.commands.parsers;

import hudson.FilePath;
import hudson.model.Computer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

class SafeFilePath {
    private static final Logger LOGGER = Logger.getLogger(SafeFilePath.class.getName());

    private SafeFilePath() {
    }

    static boolean exists(@Nonnull final FilePath filePath) {
        try {
            return filePath.exists();
        } catch (Exception e) {
            LOGGER.log(
                Level.SEVERE,
                String.format("Unable to determine whether file '%s' exists", getPrintableFilePath(filePath)),
                e);
            return false;
        }
    }

    @CheckForNull
    static InputStream read(@Nonnull final FilePath filePath) {
        try {
            return filePath.read();
        } catch (Exception e) {
            LOGGER.log(
                Level.SEVERE,
                String.format("Unable to open file %s' for read", getPrintableFilePath(filePath)),
                e);
            return null;
        }
    }

    @Nonnull
    private static String getPrintableFilePath(@Nonnull final FilePath path) {
        Computer computer = path.toComputer();
        return String.format(
            "'%s' in '%s'",
            path.getRemote(), computer != null ? computer.getName() : "master");
    }
}
