package de.dvspla.storageanalyzer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

/**
 * Hauptklasse, vor der JavaFX main().
 * Stellt sicher, dass das Programm nur mit Java 8 gestartet wird (wg. JavaFX), indem es versucht,
 * Java bei "C:\Program Files\Java\jdk1.8.0_291\" zu benutzen, um das Programm zu starten.
 * Wenn das fehlschlägt, muss der Benutzer einen gültigen Java 8 Pfad auswählen,
 * der dann in %appdata%\StorageAnalyzer\.javapath gespeichert wird.
 * Dann wird, wenn das Programm nicht mit Java 8 ausgeführt wurde, versucht, es nochmal mit der javaw.exe des
 * gespeicherten Pfads auszuführen.<br>
 * Wenn das Argument "java8" existiert, wird die Java Version überprüft (wenn es nicht Java 8 ist, muss man wieder
 * einen Java 8 Ordner auswählen) und die GUI main() ausgeführt. Ansonsten wird alles oben genannte mit der Überprüfung
 * des Pfads durchgeführt. Das Argument "java8" wird übergeben, wenn das Programm mit der (vorraussichtlich) gültigen
 * Java 8 Version ausgeführt wird.
 */
public class StorageAnalyzer {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("java8")) {
            Locale.setDefault(Locale.US);
            if (!System.getProperty("java.version").startsWith("1.8")) {
                JOptionPane.showMessageDialog(null, "Invalid Java JRE/JDK version. Please execute the\nprogram again and select the Java 8 directory.", "Error", JOptionPane.ERROR_MESSAGE);
                File file = new File("/" + System.getenv("APPDATA") + "/StorageAnalyzer/.javapath");
                if (file.exists()) {
                    file.delete();
                }
                return;
            }
            StorageAnalyzerGUI.main(args);
            return;
        }
        try {
            // Der aktuelle Pfad, an dem das Programm gerade ausgeführt wird, damit man es später mit der javaw.exe von Java 8 ausführen kann.
            File file = new File(StorageAnalyzer.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            String path;

            File pathFile = new File("/" + System.getenv("APPDATA") + "/StorageAnalyzer/.javapath");
            if (!pathFile.exists()) {
                if (!pathFile.getParentFile().exists()) {
                    pathFile.getParentFile().mkdirs();
                }
                pathFile.createNewFile();
                FileWriter writer = new FileWriter(pathFile);
                writer.append("C:\\Program Files\\Java\\jdk1.8.0_291\\");
                writer.close();
            }

            // Solange der Benutzer keinen gültigen Java Ordner auswählt, wird er nach dem Pfad gefragt.
            Scanner scanner;
            File javaDir = null;
            while (javaDir == null) {
                scanner = new Scanner(pathFile);
                if (scanner.hasNextLine()) {
                    String pathString = scanner.nextLine();
                    javaDir = new File(pathString);
                    if (isInvalidJavaDir(javaDir)) {
                        JOptionPane.showMessageDialog(null, "Invalid Java 8 directory.\nPlease select a Java 8 JDK/JRE directory.", "Error", JOptionPane.ERROR_MESSAGE);
                        selectJavaPath(pathFile);
                        javaDir = null;
                    }
                } else {
                    selectJavaPath(pathFile);
                    javaDir = null;
                }
                scanner.close();
            }

            path = new File(javaDir, "bin\\javaw.exe").getAbsolutePath();

            try {
                executeJava8(path, file.getAbsolutePath());
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(null, "Invalid java.exe", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | URISyntaxException ex) {
            JOptionPane.showMessageDialog(null, "An error occured while attempting to execute the program:\n" + Arrays.toString(ex.getStackTrace()), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean isInvalidJavaDir(File file) {
        if (file == null) {
            return true;
        }
        File javaCheck = new File(file, "bin\\javaw.exe");
        return !javaCheck.exists();
    }

    private static void executeJava8(String javaPath, String filePath) throws Exception {
        Runtime.getRuntime().exec("\"" + javaPath + "\"" + " -jar \"" + filePath + "\" java8");
    }

    private static void selectJavaPath(File pathFile) throws IOException {
        JFileChooser fileChooser = new JFileChooser("C:\\Program Files\\Java\\");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Java 8 Directory");
        fileChooser.setApproveButtonText("Select");

        fileChooser.showOpenDialog(null);

        File f = fileChooser.getSelectedFile();

        if (f == null || !f.exists() || !f.isDirectory()) {
            System.exit(0);
            return;
        }

        if (isInvalidJavaDir(f)) {
            return;
        }

        FileWriter writer = new FileWriter(pathFile);
        writer.append(f.getAbsolutePath());
        writer.close();
    }

}
