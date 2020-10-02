package com.codicesoftware.plugins.hudson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutputTempFile {
    private static final Logger LOGGER = Logger.getLogger(OutputTempFile.class.getName());

    private OutputTempFile() { }

    public static String getPathForXml() {
        return Paths
            .get(System.getProperty("java.io.tmpdir"))
            .resolve(UUID.randomUUID().toString() + ".xml")
            .toString();
    }

    public static void safeDelete(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            LOGGER.log(
                Level.SEVERE, String.format("Unable to remove file '%s'", path), e);
        }
    }
}
