package de.dvspla.storageanalyzer.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SearchItem {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final File file;
    private final String path, usage;
    private String size;
    private long bytes;

    public SearchItem() {
        this.file = null;
        this.path = "(empty)";
        this.size = "";
        this.usage = "";
    }

    public SearchItem(File file) {
        this(file, false);
    }

    public SearchItem(File file, boolean checkPar) {
        this.file = file;
        if(checkPar) {
            String pathString = file.getAbsolutePath();
            if(pathString.endsWith("\\")) {
                pathString = pathString.substring(0, pathString.length()-1);
            }
            this.path = pathString;
        } else {
            this.path = file.getName();
        }
        setSize(file.length());

        String usage;
        try {
            BasicFileAttributes attrs = Files.readAttributes(Paths.get(file.toURI()), BasicFileAttributes.class);
            FileTime time = attrs.lastAccessTime();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
            usage = localDateTime.format(FORMATTER);
        } catch (Exception ex) {
            usage = "?";
        }
        this.usage = usage;
    }

    public File getFile() {
        return file;
    }

    public void setSize(long bytes) {
        this.bytes = bytes;
        if(bytes < 1024) {
            this.size = bytes + " B";
        } else if (bytes < 1_048_576) {
            this.size = bytes/1024 + " KB";
        } else if (bytes < 1_073_741_824) {
            this.size = Math.round(bytes/1_048_576.0 * 100) / 100.0 + " MB";
        } else {
            this.size = Math.round(bytes/1_073_741_824.0 * 100) / 100.0 + " GB";
        }
    }

    public String getPath() {
        return path;
    }

    public String getSize() {
        return size;
    }

    public String getUsage() {
        return usage;
    }

    public long getBytes() {
        return bytes;
    }

}
