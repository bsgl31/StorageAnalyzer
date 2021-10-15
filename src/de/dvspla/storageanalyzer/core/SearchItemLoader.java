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

public class SearchItemLoader {

    public static final AtomicInteger LOADING = new AtomicInteger(0);
    public static final AtomicInteger LOADED = new AtomicInteger(0);
    public static final AtomicLong AIM_BYTES = new AtomicLong(0);
    public static final AtomicLong CUR_BYTES = new AtomicLong(0);

    public static void progessBar(ProgressBar progressBar, ProgressBar progressBarAnimation, Label labelLoadingInfo) {
        new Thread(() -> {
            long same = 0;
            while (AIM_BYTES.get() > CUR_BYTES.get() || LOADED.get() < LOADING.get()) {
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
                progressBar.setProgress(Math.min(((double) cur) / aim, 0.99));
                Platform.runLater(() -> labelLoadingInfo.setText(((int) (progressBar.getProgress() * 100)) + "%"));
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            progressBarAnimation.setProgress(0);
            progressBarAnimation.setVisible(false);

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
        this.mainItem = new TreeItem<>(new SearchItem(file, true), getFolderIcon());

        long bytes;

        File[] files = file.listFiles();
        if(files == null || files.length == 0) {
            bytes = file.length();
            mainItem.setGraphic(getEmptyIcon());
        } else {
            AtomicLong size = new AtomicLong(0);
            Set<TreeItem<SearchItem>> treeItems = ConcurrentHashMap.newKeySet();

            for(File f : files) {
                if(f.isDirectory()) {
                    LOADING.getAndIncrement();
                    new Thread(() -> {
                        SearchItem searchItem = new SearchItem(f);
                        TreeItem<SearchItem> item = new TreeItem<>(searchItem, getFolderIcon());
                        treeItems.add(item);
                        long folderSize = loadFiles(item, f);
                        item.getValue().setSize(folderSize);
                        size.getAndAdd(folderSize);
                        LOADED.getAndIncrement();
                    }).start();
                    continue;
                }
                treeItems.add(new TreeItem<>(new SearchItem(f), getFileIcon()));
                size.addAndGet(f.length());
                CUR_BYTES.addAndGet(f.length());
            }

            while(LOADED.get() < LOADING.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Platform.runLater(() -> {
                for(TreeItem<SearchItem> treeItem : treeItems) {
                    mainItem.getChildren().add(treeItem);
                }
            });
            bytes = size.get();
        }
        mainItem.getValue().setSize(bytes);
    }

    private long loadFiles(TreeItem<SearchItem> parent, File file) {
        File[] files = file.listFiles();
        if (files == null) return file.length();
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
