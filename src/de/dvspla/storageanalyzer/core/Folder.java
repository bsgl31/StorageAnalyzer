package de.dvspla.storageanalyzer.core;

import java.io.File;

/**
 * Ein Wrapper für eine {@link File}, hauptsächlich um Dateien und Ordner im Code leichter zu unterscheiden,
 * und die {@link #toString()} Methode zu überschreiben, damit der Pfad richtig im GUI angezeigt wird.
 */
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
