package de.dvspla.storageanalyzer.core;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.beans.Observable;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

public class FolderList extends ObservableListWrapper<Folder> {

    public FolderList(List<Folder> list) {
        super(list);
    }

}
