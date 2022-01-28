package de.dvspla.storageanalyzer;

import de.dvspla.storageanalyzer.core.SearchItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    /**
     * Wird vor dem eigentlichen Einlesen der Dateien benutzt, um grob die Größe aller Dateien zu erhalten, damit der Fortschrittsbalken richtig berechnet werden kann.
     * @param file Ordner
     * @return Die größer aller Dateien, rekursiv, ausgehend von dem übergebenen Ordner in bytes.
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


    public static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            showError();
        }
    }

    public static void selectFileInExplorer(File file) {
        try {
            Runtime.getRuntime().exec("explorer.exe /select, \"" + file.getAbsolutePath() + "\"");
        } catch (IOException ex) {
            showError();
        }
    }

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

    public static void deleteFile(File file) {
        if (!file.delete()) {
            showError();
        }
    }

    /**
     * Löscht rekursiv vom angegebenen Ordner ausgehend alle Dateien und Unterordner
     * @param file Ordner
     */
    public static void deleteDirectory(File file) {
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDirectory(f);
                continue;
            }
            f.delete();
        }
    }

    /**
     * Geht alle Parent-Elemente des angegebenen Items durch und zieht von der Größe die angegebene Größe ab.
     * Wird benutzt, um die Größe der Parent-Elemente zu updaten, wenn ein Item versteckt oder gelöscht wird.
     * @param parent Angeklicktes Item
     * @param size Die Größe des Items in bytes.
     */
    private static void updateSize(TreeItem<SearchItem> parent, long size) {
        parent.getValue().setSize(parent.getValue().getBytes() - size);
        if (parent.getParent() != null) {
            updateSize(parent.getParent(), size);
        }
    }

    /**
     * Nimmt das aktuell ausgewählte Item in der View, führt die Methode {@link #updateSize(TreeItem, long)} mit dem Parent
     * davon und der Größe davon aus, und entfernt das aktuelle Element in der View.
     * Wird beim Verstecken und Löschen von Dateien direkt über den StorageAnalyzer benötigt.
     * @param view TreeTableView
     */
    public static void removeSelectedAndUpdateSize(TreeTableView<SearchItem> view) {
        TreeItem<SearchItem> item = view.getSelectionModel().getSelectedItem();
        TreeItem<SearchItem> parent = item.getParent();
        updateSize(parent, item.getValue().getBytes());
        parent.getChildren().remove(item);
    }

}
