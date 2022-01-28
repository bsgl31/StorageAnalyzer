package de.dvspla.storageanalyzer.core;

import com.sun.javafx.collections.ObservableListWrapper;

import java.util.List;

/**
 * Ein Container für einen {@link ObservableListWrapper<Folder>}, um den Code mit dem kürzeren Namen "FolderList" verständlicher zu machen.
 */
public class FolderList extends ObservableListWrapper<Folder> {

    public FolderList(List<Folder> list) {
        super(list);
    }

}
