package de.dvspla.storageanalyzer;

import de.dvspla.storageanalyzer.core.Folder;
import de.dvspla.storageanalyzer.core.FolderList;
import de.dvspla.storageanalyzer.core.SearchItem;
import de.dvspla.storageanalyzer.core.SearchItemLoader;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.DirectoryChooser;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class AnalyzerPanelController implements Initializable {

    public TableView<Folder> tablePathlist;
    public TableColumn<String, String> clmPath = new TableColumn<>("Pfad");
    public TableColumn<String, String> clmStatus = new TableColumn<>("Status");

    public TreeTableView<SearchItem> mainSearch;
    public TreeTableColumn<SearchItem, String> columnSearchPath, columnSearchSize, columnSearchUsage;
    public TreeTableColumn<SearchItem, Long> columnSearchBytes;

    public Button btnLoad, btnAdd, btnRemove, btnSort, btnLoadDefaultPaths;

    public Label labelLoadingInfo;
    public ProgressBar pbLoading, pbLoadingAnimation;

    private FolderList folderList;
    private SettingsLoader settingsLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsLoader = new SettingsLoader();
        settingsLoader.loadSettings();
        tablePathlist.setItems(folderList = new FolderList(settingsLoader.getLoadedFolders()));

        clmPath.setCellValueFactory(new PropertyValueFactory<>("path"));
        clmStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        columnSearchPath.setCellValueFactory(new TreeItemPropertyValueFactory<>("path"));
        columnSearchSize.setCellValueFactory(new TreeItemPropertyValueFactory<>("size"));
        columnSearchUsage.setCellValueFactory(new TreeItemPropertyValueFactory<>("usage"));
        columnSearchBytes.setCellValueFactory(new TreeItemPropertyValueFactory<>("bytes"));

        mainSearch.setRowFactory(param -> {
            final TreeTableRow<SearchItem> row = new TreeTableRow<>();

            row.itemProperty().addListener((observable, oldValue, item) -> {
                final ContextMenu rowMenu = new ContextMenu();

                if(item == null || item.getFile() == null) {
                    row.setContextMenu(null);
                    return;
                }

                MenuItem openInExplorer = new MenuItem("Select in Explorer");
                openInExplorer.setOnAction(event -> Utils.selectFileInExplorer(row.getItem().getFile()));

                MenuItem open;
                if(item.getFile().isDirectory()) {
                    open = new MenuItem("Open in Explorer");
                    open.setOnAction(event -> Utils.openFileInExplorer(item.getFile()));
                } else {
                    open = new MenuItem("Open File");
                    open.setOnAction(event -> Utils.openFile(item.getFile()));
                }

                rowMenu.getItems().addAll(openInExplorer, open);
                row.setContextMenu(rowMenu);
            });

            return row;
        });

        tablePathlist.setRowFactory(param -> {
            final TableRow<Folder> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();

            MenuItem loadThis = new MenuItem("Load this Path only");
            loadThis.setOnAction(event -> load(Collections.singletonList(row.getItem())));

            MenuItem remove = new MenuItem("Remove");
            remove.setOnAction(event -> {
                tablePathlist.getItems().remove(row.getItem());
                tablePathlist.getSelectionModel().clearSelection();
            });
            rowMenu.getItems().addAll(loadThis, remove);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu));
            return row;
        });
    }

    @FXML
    public void onClick(ActionEvent event) {
        if (event.getSource() == btnLoadDefaultPaths) {
            settingsLoader.saveDefaultSettings();
            settingsLoader.loadSettings();
            tablePathlist.setItems(folderList = new FolderList(settingsLoader.getLoadedFolders()));
        } else if (event.getSource() == btnAdd) {
            DirectoryChooser chooser = new DirectoryChooser();
            File f = chooser.showDialog(StorageAnalyzerGUI.STAGE);

            if (f == null) return;
            if (!f.isDirectory()) return;

            List<Folder> toRemove = new ArrayList<>();
            for (Folder folder : folderList) {
                if (f.getAbsolutePath().startsWith(folder.getPath())) {
                    return;
                }
                if (folder.getPath().startsWith(f.getAbsolutePath())) {
                    toRemove.add(folder);
                }
            }
            folderList.add(new Folder(f));
            folderList.removeAll(toRemove);
        } else if (event.getSource() == btnRemove) {
            folderList.remove(tablePathlist.getSelectionModel().getSelectedItem());
        } else if (event.getSource() == btnLoad) {
            load(folderList);
        } else if (event.getSource() == btnSort) {
            columnSearchBytes.setSortType(TreeTableColumn.SortType.DESCENDING);
            mainSearch.getSortOrder().clear();
            mainSearch.getSortOrder().add(columnSearchBytes);
            mainSearch.sort();
        }
    }

    private void load(List<Folder> folders) {
        if (pbLoadingAnimation.getProgress() == -1) return;

        TreeItem<SearchItem> root = new TreeItem<>(new SearchItem());

        labelLoadingInfo.setText("0%");

        pbLoadingAnimation.setVisible(true);
        pbLoadingAnimation.setProgress(-1);
        pbLoading.setProgress(0);

        new Thread(() -> {
            long amountBytes = 0;
            for (Folder folder : folders) {
                if (folder.getFile().exists()) {
                    amountBytes += Utils.getFileSize(folder.getFile());
                }
            }

            SearchItemLoader.AIM_BYTES.set(amountBytes);
            SearchItemLoader.CUR_BYTES.set(0);
            SearchItemLoader.LOADED.set(0);
            SearchItemLoader.LOADING.set(0);

            SearchItemLoader.progessBar(pbLoading, pbLoadingAnimation, labelLoadingInfo);
            List<Folder> toRemove = new ArrayList<>();
            for (Folder folder : folders) {
                if (folder.getFile() == null || !folder.getFile().exists()) {
                    toRemove.add(folder);
                }
                new Thread(() -> root.getChildren().add(new SearchItemLoader(folder.getFile()).getMainItem())).start();
            }
            if(toRemove.size() > 0) {
                folderList.removeAll(toRemove);
                new Thread(() -> JOptionPane.showMessageDialog(null, "Removed " + toRemove.size() + " unknown folder(s)", "Information", JOptionPane.INFORMATION_MESSAGE)).start();
            }

            Platform.runLater(() -> mainSearch.setRoot(root));
        }).start();
    }

}
