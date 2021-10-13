package de.dvspla.storageanalyzer.core;

import com.sun.javafx.collections.ObservableListWrapper;

import java.util.List;

public class FolderList extends ObservableListWrapper<Folder> {

    public FolderList(List<Folder> list) {
        super(list);
    }

}
