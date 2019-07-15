package com.codicesoftware.plugins.hudson.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

public class DeleteOnCloseFileInputStream extends FileInputStream {
    public DeleteOnCloseFileInputStream(String fileName) throws FileNotFoundException{
        this(new File(fileName));
    }

    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException{
        super(file);
        this.file = file;
    }

    public void close() throws IOException {
        if (file == null)
            return;

        try {
            super.close();
        } finally {
            if (file != null) {
                Files.delete(file.toPath());
                file = null;
            }
        }
    }

    private File file;
}