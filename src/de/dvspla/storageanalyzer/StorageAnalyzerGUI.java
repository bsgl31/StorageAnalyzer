package de.dvspla.storageanalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;

import java.io.IOException;
import java.util.Arrays;

public class StorageAnalyzerGUI extends Application {

    public static Stage STAGE;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("AnalyzerPanel.fxml"));
        primaryStage.setTitle("Storage Analyzer");
        primaryStage.setResizable(false);

        root.getStyleClass().add(JMetroStyleClass.BACKGROUND);
        Scene scene = new Scene(root);
        JMetro jMetro = new JMetro(Style.DARK);
        jMetro.setScene(scene);

        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("core/icon/disk.png")));
        primaryStage.show();

        StorageAnalyzerGUI.STAGE = primaryStage;

        primaryStage.setOnCloseRequest(event -> SettingsLoader.getInstance().saveSettings());
    }

    public static void main(String[] args) {
        launch();
    }

}
