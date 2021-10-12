package de.dvspla.storageanalyzer.core;

import java.io.File;

public class Folder {

    private final File file;

    public Folder(String path) {
        this.file = new File(path);
    }

    public Folder(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
