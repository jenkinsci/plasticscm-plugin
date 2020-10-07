package com.codicesoftware.plugins.hudson;

import hudson.FilePath;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutputTempFile {
    private static final Logger LOGGER = Logger.getLogger(OutputTempFile.class.getName());

    private OutputTempFile() { }

    public static FilePath getPathForXml(FilePath workspacePath) throws IOException, InterruptedException {
        return workspacePath.createTempFile("output_", ".xml");
    }

    public static void safeDelete(FilePath path) {
        try {
            if (path.exists()) {
                path.delete();
            }
        } catch (Exception e) {
            LOGGER.log(
                Level.SEVERE, String.format("Unable to remove file '%s'", path), e);
        }
    }
}
