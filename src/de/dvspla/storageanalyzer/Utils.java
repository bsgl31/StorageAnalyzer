package de.dvspla.storageanalyzer;

import de.dvspla.storageanalyzer.core.SearchItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    /**
     * Berechnet die Gesamtgröße eines Pfades und dessen Dateien/Ordner rekursiv
     * @param file Zu berechnender Pfad
     * @return Gesamtgrößer des übergebenen Pfades in bytes
     */
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

    /**
     * Öffnet die übergebene Datei.
     * @param file Zu öffnende Datei
     */
    public static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            showError();
        }
    }

    /**
     * Öffnet den übergeordneten Ordner der übergebenen Datei und wählt sie im Explorer an.
     * @param file Auszuwählende Datei
     */
    public static void selectFileInExplorer(File file) {
        try {
            Runtime.getRuntime().exec("explorer.exe /select, \"" + file.getAbsolutePath() + "\"");
        } catch (IOException ex) {
            showError();
        }
    }

    /**
     * Öffnet bzw. führt die übergebene Datei aus.
     * @param file Auszuführende Datei
     */
    public static void openFileInExplorer(File file) {
        try {
            Runtime.getRuntime().exec("explorer.exe /open, \"" + file.getAbsolutePath() + "\"");
        } catch (IOException ex) {
            showError();
        }
    }

    private static void showError() {
        JOptionPane.showMessageDialog(null, "An error has occurred.", "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Löscht eine Datei aus dem Dateisystem
     * @param file
     */
    public static void deleteFile(File file) {
        if (!file.delete()) {
            showError();
        }
    }

    /**
     * Löscht den übergebenen Ordner rekursiv aus dem Dateisystem und der Dateiansicht im StorageAnalyzer.
     * @param file Zu löschender Ordner
     */
    public static void deleteDirectory(File file) {
        File[] files = file.listFiles();
        if (files == null || files.length == 0) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDirectory(f);
                continue;
            }
            f.delete();
        }
    }

    /**
     * Subtrahiert rekursiv die übergebene Dateigröße von allen übergeordneten Ordnern.
     * @param parent Übergeordneter Ordner
     * @param size Größe der zu entfernenden Datei
     */
    private static void updateSize(TreeItem<SearchItem> parent, long size) {
        parent.getValue().setSize(parent.getValue().getBytes() - size);
        if (parent.getParent() != null) {
            updateSize(parent.getParent(), size);
        }
    }

    /**
     * Entfernt ein Element aus der Liste und aktualisiert mithilfe der {@link #updateSize updateSize} Methode die Speichergrößen der übergeordneten Ordner.
     * @param view Zu entfernendes Element
     */
    public static void removeSelectedAndUpdateSize(TreeTableView<SearchItem> view) {
        TreeItem<SearchItem> item = view.getSelectionModel().getSelectedItem();
        TreeItem<SearchItem> parent = item.getParent();
        updateSize(parent, item.getValue().getBytes());
        parent.getChildren().remove(item);
    }

}
