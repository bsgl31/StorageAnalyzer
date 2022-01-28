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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import javax.swing.JOptionPane;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Der Controller für das gesamte GUI.
 */
public class AnalyzerPanelController implements Initializable {

    // @FXML kann weggelassen werden, wenn die Attribute public sind.

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
        // Die Pfade werden geladen und in der Liste links hinzugefügt
        settingsLoader = new SettingsLoader();
        settingsLoader.loadSettings();
        tablePathlist.setItems(folderList = new FolderList(settingsLoader.getLoadedFolders()));

        // Die Factorys beschreiben, welche Attribute wo im Table sein sollen. Der String Paramter ist der exakte Attribut-Name des SearchItems
        // Der Table hat eine unsichtbare Spalte "bytes", nach der beim Klick auf den "Sort" Button sortiert wird.
        clmPath.setCellValueFactory(new PropertyValueFactory<>("path"));
        clmStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        columnSearchPath.setCellValueFactory(new TreeItemPropertyValueFactory<>("path"));
        columnSearchSize.setCellValueFactory(new TreeItemPropertyValueFactory<>("size"));
        columnSearchUsage.setCellValueFactory(new TreeItemPropertyValueFactory<>("usage"));
        columnSearchBytes.setCellValueFactory(new TreeItemPropertyValueFactory<>("bytes"));

        // Was passieren soll, wenn eine neue Reihe zum Table hinzugefügt wird
        mainSearch.setRowFactory(param -> {
            final TreeTableRow<SearchItem> row = new TreeTableRow<>();

            // Da wir beim Erstellen der Row noch keinen Zugriff auf das SearchItem haben, da es erst später gesetzt wird,
            // wird ein Listener auf die Item Property hinzugefügt, der ausgeführt wird, sobald dann ein Item gesetzt wird.
            row.itemProperty().addListener((observable, oldValue, item) -> {
                // Erstellt das Context Menü der Items in der TableView
                final ContextMenu rowMenu = new ContextMenu();

                if (item == null || item.getFile() == null) {
                    row.setContextMenu(null);
                    return;
                }

                MenuItem openInExplorer = new MenuItem("Select in Explorer");
                openInExplorer.setOnAction(event -> Utils.selectFileInExplorer(row.getItem().getFile()));

                MenuItem open, delete, hide = null;
                if (item.getFile().isDirectory()) {
                    open = new MenuItem("Open in Explorer");
                    open.setOnAction(event -> Utils.openFileInExplorer(item.getFile()));

                    delete = new MenuItem("Delete Directory");
                    delete.setOnAction(event -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Delete directory " + item.getFile().getName() + "?", ButtonType.YES, ButtonType.NO);
                        Optional<ButtonType> type = alert.showAndWait();
                        if (type.isPresent() && type.get().equals(ButtonType.YES)) {
                            // Wenn die Löschung bestätigt wurde, wird das Item aus der TableView entfernt und die Größe geupdated (siehe removeSelectedAndUpdateSize Methode)
                            // und der Ordner und alle Unterordner/Dateien gelöscht
                            Utils.removeSelectedAndUpdateSize(mainSearch);
                            Utils.deleteDirectory(item.getFile()); // TODO SHOW WINDOW UNTIL DELETED & ASYNC
                        }
                    });

                    // Versteckt den Ordner nur in der TableView
                    hide = new MenuItem("Hide Directory");
                    hide.setOnAction(event -> Utils.removeSelectedAndUpdateSize(mainSearch));

                } else {
                    open = new MenuItem("Open File");
                    open.setOnAction(event -> Utils.openFile(item.getFile()));

                    delete = new MenuItem("Delete File");
                    delete.setOnAction(event -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Delete file " + item.getFile().getName() + "?", ButtonType.YES, ButtonType.NO);
                        Optional<ButtonType> type = alert.showAndWait();
                        if (type.isPresent() && type.get().equals(ButtonType.YES)) {
                            // Wenn die Löschung bestätigt wurde, wird das Item aus der TableView entfernt und die Größe geupdated (siehe removeSelectedAndUpdateSize Methode)
                            // und die Datei endgültig gelöscht
                            Utils.removeSelectedAndUpdateSize(mainSearch);
                            Utils.deleteFile(item.getFile());
                        }
                    });

                }

                // Fügt die ganzen Reihen zum Context-Menu hinzu
                rowMenu.getItems().addAll(openInExplorer, open, delete);
                if (hide != null) {
                    rowMenu.getItems().add(hide);
                }
                row.setContextMenu(rowMenu);
            });

            return row;
        });

        // Setzt die Context Menus für die Pfadliste auf der linken Seite des GUIs
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

            // Sort dafür, dass das Context-Menu bei leeren Items in der Liste nicht gesetzt wird.
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu));
            return row;
        });
    }

    @FXML
    public void onClick(ActionEvent event) {
        // Verwaltet den Klick aller Buttons im GUI
        if (event.getSource() == btnLoadDefaultPaths) {
            settingsLoader.saveDefaultSettings();
            settingsLoader.loadSettings();
            tablePathlist.setItems(folderList = new FolderList(settingsLoader.getLoadedFolders()));
        } else if (event.getSource() == btnAdd) {
            DirectoryChooser chooser = new DirectoryChooser();
            File f = chooser.showDialog(StorageAnalyzerGUI.STAGE);

            if (f == null) {
                return;
            }
            if (!f.isDirectory()) {
                return;
            }

            // Sort dafür, dass sich keine Ordner überschneiden
            // Bsp. 1: Ordner "D:\Daten\" ist bereits in der Liste
            //         --> "D:\Daten\Spiele\" wird nicht hinzugefügt, da der Überordner schon in der Liste ist
            // Bsp. 2: Ordner "D:\Daten\Spiele\" ist bereits in der Liste
            //         --> "D:\Daten\" wird hinzugefügt, "D:\Daten\Spiele" aber entfernt, da es bei "D:\Daten\" schon dabei wäre
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
            // Sortiert den TableView nach der unsichtbaren Spalte "bytes"
            // (wenn man nach der "Size" Spalte sortieren würde, müsste MB, GB, etc. berücksichtigt werden)
            columnSearchBytes.setSortType(TreeTableColumn.SortType.DESCENDING);
            mainSearch.getSortOrder().clear();
            mainSearch.getSortOrder().add(columnSearchBytes);
            mainSearch.sort();
        }
    }

    private void load(List<Folder> folders) {
        // Bricht ab, wenn bereits Ordner geladen werden (die Progress wird beim Start auf -1 gesetzt, damit es
        // die Animation mit den blauen Punkten hat)
        if (pbLoadingAnimation.getProgress() == -1) {
            return;
        }

        // Das Root Item ist das "höchste" Item in der TableView, wird aber versteckt.
        // Hier werden alle Ordner als Child hinzugefügt, die geladen werden sollen
        TreeItem<SearchItem> root = new TreeItem<>(new SearchItem());

        labelLoadingInfo.setText("0%");

        pbLoadingAnimation.setVisible(true);
        pbLoadingAnimation.setProgress(-1);
        pbLoading.setProgress(0);

        new Thread(() -> {
            long amountBytes = 0;
            // Die Größe aller Ordner in der Liste wird geladen, ohne die Dateien zu laden, damit die Progressbar richtig angezeigt werden kann
            for (Folder folder : folders) {
                if (folder.getFile().exists()) {
                    amountBytes += Utils.getFileSize(folder.getFile());
                }
            }

            // /* Siehe Dokumentation in SearchItemLoader
            SearchItemLoader.AIM_BYTES.set(amountBytes);
            SearchItemLoader.CUR_BYTES.set(0);
            SearchItemLoader.LOADED.set(0);
            SearchItemLoader.LOADING.set(0);

            SearchItemLoader.progressBar(pbLoading, pbLoadingAnimation, labelLoadingInfo);
            // */

            // Alle Ordner in der Liste links im GUI werden überprüft, ob sie existieren.
            // Wenn ja wird ein Thread gestartet, um den Unterordner zu laden
            List<Folder> toRemove = new ArrayList<>();
            for (Folder folder : folders) {
                if (folder.getFile() == null || !folder.getFile().exists()) {
                    toRemove.add(folder);
                }
                new Thread(() -> root.getChildren().add(new SearchItemLoader(folder.getFile()).getMainItem())).start();
            }
            // Wenn Ordner im GUI links gefunden wurden, die nicht mehr existieren, werden sie aus der Liste entfernt
            // und der Benutzer bekommt eine Nachricht
            if (toRemove.size() > 0) {
                folderList.removeAll(toRemove);
                new Thread(() -> JOptionPane.showMessageDialog(null, "Removed " + toRemove.size() + " unknown folder(s)", "Information", JOptionPane.INFORMATION_MESSAGE)).start();
            }

            // Auf dem Thread von JavaFX wird die Root auf die oben erstellte Root gesetzt
            // (die Children der Root sind die geladenen Ordner)
            Platform.runLater(() -> mainSearch.setRoot(root));
        }).start();
    }

}
