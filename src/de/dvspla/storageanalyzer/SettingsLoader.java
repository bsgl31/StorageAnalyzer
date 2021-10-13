package de.dvspla.storageanalyzer;

import de.dvspla.storageanalyzer.core.Folder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SettingsLoader {

    private static SettingsLoader instance;
    public static SettingsLoader getInstance() {
        return instance;
    }


    private final ArrayList<Folder> loadedFolders = new ArrayList<>();
    private final File file;

    public SettingsLoader() {
        instance = this;
        file = new File("/" + System.getenv("APPDATA") + "/StorageAnalyzer/settings");
        if (!file.exists()) {
            saveDefaultSettings();
        }
    }

    public void saveDefaultSettings() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            FileWriter fr = new FileWriter(file);
            fr.append("H:\\").append("\n").
                    append("C:\\Users\\").append(System.getProperty("user.name")).append("\\");
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void loadSettings() {
        try {
            loadedFolders.clear();
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                Folder folder = new Folder(scanner.nextLine());
                if(folder.getFile() != null && folder.getFile().exists()) {
                    loadedFolders.add(folder);
                }
            }
            scanner.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<Folder> getLoadedFolders() {
        return loadedFolders;
    }

    public void saveSettings() {
        try {
            FileWriter fr = new FileWriter(file);
            for(Folder f : loadedFolders) {
                fr.append(f.getPath()).append("\n");
            }
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
