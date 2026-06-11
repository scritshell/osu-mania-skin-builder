package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.OskPackager;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class MainWindow {

    private final SkinConfig skinConfig = new SkinConfig();

    public void start(Stage stage) {
        stage.setTitle("osu!mania Skin Builder - v1.0");

        ManiaKeyConfig keymode4k = skinConfig.getOrCreateKeymode(4);
        ManiaKeyConfig keymode7k = skinConfig.getOrCreateKeymode(7);

        TabPane tabPane = new TabPane();

        Tab tabGeneral = new Tab("General");
        tabGeneral.setClosable(false);
        tabGeneral.setContent(new StackPane(new Label("Opciones Generales (Nombre, Autor, Importar/Exportar .osk)")));

        Tab tab4k = new Tab("4K");
        tab4k.setClosable(false);
        tab4k.setContent(new KeymodeTab(keymode4k));

        Tab tab7k = new Tab("7K");
        tab7k.setClosable(false);
        tab7k.setContent(new KeymodeTab(keymode7k));

        tabPane.getTabs().addAll(tabGeneral, tab4k, tab7k);

        Button exportButton = new Button("Exportar Skin (.osk)");
        exportButton.setOnAction(event -> exportSkin(stage, exportButton));

        HBox bottomBar = new HBox(exportButton);
        bottomBar.setPadding(new Insets(12));
        bottomBar.setStyle("-fx-background-color: #f2f2f2; -fx-border-color: #d0d0d0 transparent transparent transparent;");

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1100, 650);

        stage.setScene(scene);
        stage.show();
    }

    private void exportSkin(Stage stage, Button exportButton) {
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Guardar skin exportada");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("osu! skin (.osk)", "*.osk"));
        saveChooser.setInitialFileName(skinConfig.getSkinName() + ".osk");

        File outputFile = saveChooser.showSaveDialog(stage);
        if (outputFile == null) {
            return;
        }

        FileChooser baseChooser = new FileChooser();
        baseChooser.setTitle("Seleccionar skin base");
        baseChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("osu! skin / ZIP", "*.osk", "*.zip"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        File baseFile = baseChooser.showOpenDialog(stage);
        if (baseFile == null) {
            return;
        }

        Path baseOskPath = baseFile.toPath();
        Path outputOskPath = ensureOskExtension(outputFile).toPath();

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                OskPackager.packSkin(skinConfig, baseOskPath, outputOskPath);
                return null;
            }
        };

        exportButton.setDisable(true);

        exportTask.setOnSucceeded(event -> {
            exportButton.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportacion completada");
            alert.setHeaderText("Skin exportada correctamente");
            alert.setContentText(outputOskPath.toString());
            alert.showAndWait();
        });

        exportTask.setOnFailed(event -> {
            exportButton.setDisable(false);
            Throwable error = exportTask.getException();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al exportar");
            alert.setHeaderText("No se pudo exportar la skin");
            alert.setContentText(error == null ? "Error desconocido" : error.getMessage());
            alert.showAndWait();
        });

        Thread exportThread = new Thread(exportTask, "osk-export-task");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    private File ensureOskExtension(File file) {
        String path = file.getAbsolutePath();
        if (path.toLowerCase().endsWith(".osk")) {
            return file;
        }
        return new File(path + ".osk");
    }
}
