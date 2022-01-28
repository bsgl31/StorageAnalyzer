package de.dvspla.storageanalyzer.core;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Für das Laden der Items und die Progressbar zuständig
 */
public class SearchItemLoader {

    /**
     * Wie viele Ordner in der Liste links geladen werden müssen
     */
    public static final AtomicInteger LOADING = new AtomicInteger(0);
    /**
     * Wie viele Ordner in der Liste links schon geladen wurden
     */
    public static final AtomicInteger LOADED = new AtomicInteger(0);

    /**
     * Wie viele Bytes insgesamt geladen werden müssen (wird für die Progressbar benötigt)
     */
    public static final AtomicLong AIM_BYTES = new AtomicLong(0);
    /**
     * Wie viele Bytes aktuell geladen wurden (wird für die Progressbar benötigt)
     */
    public static final AtomicLong CUR_BYTES = new AtomicLong(0);

    /**
     * Kümmert sich um die Progressbar, welche auf einem eigenständigen Thread verwaltet wird.
     * @param progressBar Die Progressbar mit den Prozentwerten
     * @param progressBarAnimation Die Progressbar mit den blauen Punkten, um anzuzeigen, dass etwas gemacht wird, auch wenn die "normale" Progressbar nicht weiter geht
     * @param labelLoadingInfo Das Label mit der Prozentzahl
     */
    public static void progressBar(ProgressBar progressBar, ProgressBar progressBarAnimation, Label labelLoadingInfo) {
        new Thread(() -> {
            long same = 0;

            // Solange die Anzahl Bytes, die erreicht werden soll, nicht erreicht wurde, oder die Anzahl der Ordner,
            // die geladen werden müssen, nicht geladen wurden, wird die Progressbar geupdated.
            while (AIM_BYTES.get() > CUR_BYTES.get() || LOADED.get() < LOADING.get()) {
                // Wenn die Zielbytes und die aktuellen Bytes zu lange gleich sind, wird die Schleife abgebrochen und
                // somit die Progressbar auf 100 % gesetzt.
                long aim = AIM_BYTES.get();
                long cur = CUR_BYTES.get();
                if (aim == cur) {
                    same++;
                    if (same == 120) {
                        break;
                    }
                } else {
                    same = 0;
                }
                // Setzt den Progress der Progressbar auf den aktuellen Progress, aber maximal 0.99
                // (damit bei Ungenauigkeiten oder wenn später Dateien hinzukommen der Progress nicht über 100 % geht)
                progressBar.setProgress(Math.min(((double) cur) / aim, 0.99));
                // Setzt die Text-Prozentanzeige auf den aktuellen Progress
                Platform.runLater(() -> labelLoadingInfo.setText(((int) (progressBar.getProgress() * 100)) + "%"));
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Wenn das Einlesen abgeschlossen ist, wird die Progressbar mit den blauen Punkten auf 0 gesetzt
            // (damit die blauen Punkte nicht mehr durchlaufen) und versteckt
            progressBarAnimation.setProgress(0);
            progressBarAnimation.setVisible(false);

            // Danach wird der Progress der "normalen" Progressbar auf 100 % gesetzt (Wert zwischen 0 und 1, 1 = 100 %)
            progressBar.setProgress(1);
            Platform.runLater(() -> labelLoadingInfo.setText("100%"));
        }).start();
    }


    private static final Image FOLDER = new Image(SearchItemLoader.class.getResourceAsStream("icon/folder.png"));
    private static final Image FILE = new Image(SearchItemLoader.class.getResourceAsStream("icon/file.png"));
    private static final Image EMPTY = new Image(SearchItemLoader.class.getResourceAsStream("icon/empty.png"));

    private final File file;
    private final TreeItem<SearchItem> mainItem;

    public SearchItemLoader(File file) {
        this.file = file;
        // Das Hauptitem im GUI, zu dem alle Dateien/Ordner hinzugefügt werden
        this.mainItem = new TreeItem<>(new SearchItem(file, true), getFolderIcon());

        long bytes;

        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            bytes = file.length();
            mainItem.setGraphic(getEmptyIcon());
        } else {
            AtomicLong size = new AtomicLong(0);
            // Ein Thread-Safe Set, zu dem alle Items hinzugefügt werden
            Set<TreeItem<SearchItem>> treeItems = ConcurrentHashMap.newKeySet();

            // Für jeden Ordner in dem Oberordner wird ein neuer Thread erstellt, die diesen rekursiv mit der loadFiles Methode lädt.
            for (File f : files) {
                if (f.isDirectory()) {
                    // Ein neuer Ordner wurde gefunden, der geladen werden muss.
                    LOADING.getAndIncrement();
                    new Thread(() -> {
                        SearchItem searchItem = new SearchItem(f);
                        TreeItem<SearchItem> item = new TreeItem<>(searchItem, getFolderIcon());
                        treeItems.add(item);
                        // Die Größe wird Rekursiv geladen und alle Child Elemente werden dem übergebenen Item hinzugefügt
                        long folderSize = loadFiles(item, f);
                        item.getValue().setSize(folderSize);
                        size.getAndAdd(folderSize);
                        // Der Ordner wurde fertig geladen
                        LOADED.getAndIncrement();
                    }).start();
                    continue;
                }
                // Items in dem Oberordner werden ohne Thread hinzugefügt.
                treeItems.add(new TreeItem<>(new SearchItem(f), getFileIcon()));
                size.addAndGet(f.length());
                CUR_BYTES.addAndGet(f.length());
            }

            // Solange noch Items geladen werden wird gewartet
            while (LOADED.get() < LOADING.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Alle geladenen Items werden zum GUI hinzugefügt
            Platform.runLater(() -> {
                for (TreeItem<SearchItem> treeItem : treeItems) {
                    mainItem.getChildren().add(treeItem);
                }
            });
            bytes = size.get();
        }
        mainItem.getValue().setSize(bytes);
    }

    /**
     * Lädt alle Dateien des angegebenen TreeItems rekursiv
     * @param parent Das TreeItem
     * @param file Der zu ladende Ordner
     * @return Die Größe aller Dateien, damit sie beim Oberordner gesetzt werden kann
     */
    private long loadFiles(TreeItem<SearchItem> parent, File file) {
        File[] files = file.listFiles();
        if (files == null) {
            return file.length();
        }
        if (files.length == 0) {
            parent.setGraphic(getEmptyIcon());
            return file.length();
        }
        long size = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                SearchItem searchItem = new SearchItem(f);
                TreeItem<SearchItem> item = new TreeItem<>(searchItem, getFolderIcon());
                parent.getChildren().add(item);
                long folderSize = loadFiles(item, f);
                item.getValue().setSize(folderSize);
                size += folderSize;
                continue;
            }
            parent.getChildren().add(new TreeItem<>(new SearchItem(f), getFileIcon()));
            size += f.length();
            CUR_BYTES.addAndGet(f.length());
        }
        parent.getValue().setSize(size);
        return size;
    }

    /**
     * @return Eine ImageView des angegebenen Bildes, da ein Bild nur einmal angezeigt werden kann (somit braucht man für jedes Item eine neue ImageView)
     */
    private ImageView getIcon(Image image) {
        ImageView folderIcon = new ImageView(image);
        folderIcon.setFitHeight(16);
        folderIcon.setFitWidth(16);
        return folderIcon;
    }

    private ImageView getFolderIcon() {
        return getIcon(FOLDER);
    }

    private ImageView getFileIcon() {
        return getIcon(FILE);
    }

    private ImageView getEmptyIcon() {
        return getIcon(EMPTY);
    }

    public TreeItem<SearchItem> getMainItem() {
        return mainItem;
    }

    public File getFile() {
        return file;
    }

}
