package de.dvspla.storageanalyzer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    public static long getFileSize(File file) {
        final AtomicLong amount = new AtomicLong(0);

        try {
            Files.walkFileTree(Paths.get(file.toURI()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    amount.getAndAdd(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return 0;
        }

        return amount.get();
    }


    public static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            showError();
        }
    }

    public static void selectFileInExplorer(File file) {
        try {
            Runtime.getRuntime().exec("explorer.exe /select," + file.getAbsolutePath());
        } catch (IOException ex) {
            showError();
        }
    }

    public static void openFileInExplorer(File file) {
        try {
            Runtime.getRuntime().exec("explorer.exe /open," + file.getAbsolutePath());
        } catch (IOException ex) {
            showError();
        }
    }

    private static void showError() {
        JOptionPane.showMessageDialog(null, "Es ist ein Fehler aufgetreten.", "Meldung", JOptionPane.ERROR_MESSAGE);
    }

}
