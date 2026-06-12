package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.OskPackager;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Pestaña General de la aplicación.
 *
 * <h2>Antes de importar</h2>
 * Muestra solo la pantalla de bienvenida con el botón "Importar Skin (.osk)".
 * No hay pestañas de keymodes, no hay preview, no hay nada inventado.
 *
 * <h2>Después de importar</h2>
 * <ul>
 *   <li>Metadatos de la skin (nombre, autor, versión, keymodes detectados).</li>
 *   <li>Panel para añadir nuevos keymodes basados en la skin importada.</li>
 *   <li>Campos editables para los metadatos generales.</li>
 * </ul>
 */
public class GeneralTab extends ScrollPane {

    private final MainWindow mainWindow;

    // Secciones que se muestran/ocultan según el estado de importación
    private final VBox welcomeSection;
    private final VBox skinInfoSection;
    private final VBox addKeymodeSection;
    private final VBox exportSection;

    // Campos de metadatos (rellenados al importar)
    private final TextField skinNameField   = new TextField();
    private final TextField skinAuthorField = new TextField();
    private final Label skinVersionLabel    = new Label("—");
    private final Label detectedKeymodesLabel = new Label("—");

    // Selector de keymode para añadir
    private final ComboBox<String> keymodeCombo = new ComboBox<>();

    // Export UI components
    private Label exportStatusLabel;
    private ProgressBar exportProgressBar;
    private Task<Void> exportTask = null;

    // Keymodes disponibles para añadir
    private static final int[] AVAILABLE_KEYS =
            {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18};

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public GeneralTab(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setFitToWidth(true);
        setStyle("-fx-background-color: #f0f0f0;");

        welcomeSection    = buildWelcomeSection();
        skinInfoSection   = buildSkinInfoSection();
        addKeymodeSection = buildAddKeymodeSection();
        exportSection     = buildExportSection();

        // Solo visible antes de importar
        skinInfoSection.setVisible(false);
        skinInfoSection.setManaged(false);
        addKeymodeSection.setVisible(false);
        addKeymodeSection.setManaged(false);
        exportSection.setVisible(false);
        exportSection.setManaged(false);

        VBox root = new VBox(24);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.getChildren().addAll(welcomeSection, skinInfoSection, addKeymodeSection, exportSection);

        setContent(root);
    }

    // -------------------------------------------------------------------------
    // Sección de bienvenida / importar
    // -------------------------------------------------------------------------

    private VBox buildWelcomeSection() {
        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(520);
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #d8d8d8;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 36 40 36 40;"
        );

        Label title = new Label("osu!mania Skin Builder");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label(
                "Importa una skin .osk para empezar.\n" +
                        "El programa leerá el skin.ini, detectará los keymodes\n" +
                        "y generará las pestañas de edición automáticamente."
        );
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(400);

        Button importFileBtn = new Button("📦 Importar archivo (.osk)");
        importFileBtn.setStyle(
                "-fx-background-color: #e96d8a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 28;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        importFileBtn.setOnAction(e -> mainWindow.importOsk());

        Button importFolderBtn = new Button("📁 Importar carpeta");
        importFolderBtn.setStyle(
                "-fx-background-color: #4a90e2;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 28;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        importFolderBtn.setOnAction(e -> mainWindow.importFolder());

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(importFileBtn, importFolderBtn);

        Label hint = new Label("También puedes usar Archivo → Importar archivo o carpeta…");
        hint.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        box.getChildren().addAll(title, subtitle, buttonBox, hint);

        // Centrar la tarjeta dentro del tab
        HBox centered = new HBox(box);
        centered.setAlignment(Pos.CENTER);
        VBox outer = new VBox(centered);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setPadding(new Insets(20, 0, 0, 0));
        return outer;
    }

    // -------------------------------------------------------------------------
    // Sección de info de la skin importada
    // -------------------------------------------------------------------------

    private VBox buildSkinInfoSection() {
        VBox section = new VBox(12);
        section.setMaxWidth(600);

        Label header = new Label("Skin importada");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setStyle("-fx-text-fill: #333;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 10;"
        );

        skinNameField.setPrefWidth(280);
        skinNameField.setStyle("-fx-font-size: 13px;");
        skinNameField.textProperty().addListener((obs, old, val) -> {
            SkinConfig skin = mainWindow.getCurrentSkin();
            if (skin != null) skin.setSkinName(val);
        });

        skinAuthorField.setPrefWidth(280);
        skinAuthorField.setStyle("-fx-font-size: 13px;");
        skinAuthorField.textProperty().addListener((obs, old, val) -> {
            SkinConfig skin = mainWindow.getCurrentSkin();
            if (skin != null) skin.setSkinAuthor(val);
        });

        skinVersionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        detectedKeymodesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a9e6b; -fx-font-weight: bold;");
        detectedKeymodesLabel.setWrapText(true);

        int row = 0;
        grid.add(bold("Nombre:"),           0, row); grid.add(skinNameField,         1, row++);
        grid.add(bold("Autor:"),            0, row); grid.add(skinAuthorField,        1, row++);
        grid.add(bold("Versión:"),          0, row); grid.add(skinVersionLabel,       1, row++);
        grid.add(bold("Keymodes leídos:"),  0, row); grid.add(detectedKeymodesLabel,  1, row);

        section.getChildren().addAll(header, grid);
        return section;
    }

    // -------------------------------------------------------------------------
    // Sección para añadir nuevos keymodes
    // -------------------------------------------------------------------------

    private VBox buildAddKeymodeSection() {
        VBox section = new VBox(12);
        section.setMaxWidth(600);

        Label header = new Label("Añadir keymode");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setStyle("-fx-text-fill: #333;");

        Label desc = new Label(
                "Añade un keymode nuevo partiendo de los assets de la skin importada.\n" +
                        "Los colores y notas se copiarán y podrás personalizar cada columna."
        );
        desc.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        desc.setWrapText(true);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        for (int k : AVAILABLE_KEYS) {
            keymodeCombo.getItems().add(k + "K");
        }
        keymodeCombo.setPromptText("Selecciona keymode…");
        keymodeCombo.setPrefWidth(160);

        Button addBtn = new Button("+ Añadir pestaña");
        addBtn.setStyle(
                "-fx-background-color: #5cad8e;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 7;" +
                        "-fx-cursor: hand;"
        );
        addBtn.setOnAction(e -> addSelectedKeymode());

        row.getChildren().addAll(keymodeCombo, addBtn);

        VBox card = new VBox(10, desc, row);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 10;"
        );

        section.getChildren().addAll(header, card);
        return section;
    }

    // -------------------------------------------------------------------------
    // Lógica
    // -------------------------------------------------------------------------

    /**
     * Llamado por {@link MainWindow} cuando una skin se ha importado con éxito.
     * Muestra los metadatos y habilita las secciones ocultas.
     */
    public void onSkinLoaded(SkinConfig config) {
        // Rellenar metadatos (sin disparar listeners de persistencia aún)
        skinNameField.setText(config.getSkinName());
        skinAuthorField.setText(config.getSkinAuthor());
        skinVersionLabel.setText(config.getSkinVersion());

        String keymodesList = config.getKeymodes().stream()
                .map(ManiaKeyConfig::getDisplayName)
                .collect(Collectors.joining(", "));
        detectedKeymodesLabel.setText(
                keymodesList.isEmpty() ? "Ninguno detectado" : keymodesList
        );

        // Filtrar del combo los keymodes que ya existen en la skin
        keymodeCombo.getItems().clear();
        for (int k : AVAILABLE_KEYS) {
            boolean alreadyPresent = config.getKeymode(k).isPresent();
            keymodeCombo.getItems().add(k + "K" + (alreadyPresent ? " (ya existe)" : ""));
        }

        // Mostrar las secciones que estaban ocultas
        setVisible(skinInfoSection, true);
        setVisible(addKeymodeSection, true);
        setVisible(exportSection, true);
    }

    private void addSelectedKeymode() {
        String sel = keymodeCombo.getValue();
        if (sel == null) return;

        SkinConfig skin = mainWindow.getCurrentSkin();
        if (skin == null) return;

        // Extraer número de la cadena "4K" o "4K (ya existe)"
        int keys;
        try {
            keys = Integer.parseInt(sel.replaceAll("\\D.*", "").trim());
        } catch (NumberFormatException e) {
            return;
        }

        ManiaKeyConfig km = skin.getOrCreateKeymode(keys);
        km.setEnabled(true);
        mainWindow.addKeymodeTab(km);
    }

    // -------------------------------------------------------------------------
    // Sección de exportación de skin
    // -------------------------------------------------------------------------

    private VBox buildExportSection() {
        VBox section = new VBox(12);
        section.setMaxWidth(600);

        Label header = new Label("Exportar skin");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setStyle("-fx-text-fill: #333;");

        Label desc = new Label(
                "Exporta una skin .osk con todas las ediciones realizadas.\n" +
                        "Se generarán las imágenes con los colores y modificaciones aplicadas."
        );
        desc.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        desc.setWrapText(true);

        // Export button
        Button exportBtn = new Button("💾   Exportar Skin (.osk)");
        exportBtn.setStyle(
                "-fx-background-color: #2d8659;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 28;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        exportBtn.setOnAction(e -> exportSkinOsk());

        // Status label
        exportStatusLabel = new Label("");
        exportStatusLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

        // Progress bar
        exportProgressBar = new ProgressBar();
        exportProgressBar.setPrefWidth(Double.MAX_VALUE);
        exportProgressBar.setProgress(0.0);
        exportProgressBar.setVisible(false);

        VBox controlsBox = new VBox(10, exportBtn, exportStatusLabel, exportProgressBar);
        controlsBox.setPadding(new Insets(16));
        controlsBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 10;"
        );

        section.getChildren().addAll(header, desc, controlsBox);
        return section;
    }

    private void exportSkinOsk() {
        SkinConfig skin = mainWindow.getCurrentSkin();
        if (skin == null) {
            showExportError("Sin skin", "Importa una skin primero antes de exportar.");
            return;
        }

        // Get base OSK path from somewhere — for now, use a placeholder
        // In a real implementation, you'd need to store the original OSK path
        // For now, we'll need to ask the user or get it from somewhere
        // Let's assume we need to get it from the file chooser

        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar skin como .osk");
        fc.setInitialFileName("skin-export.osk");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("osu! skin (.osk)", "*.osk")
        );
        File outputFile = fc.showSaveDialog(mainWindow.getStage());
        if (outputFile == null) return;

        // We need the base OSK path. For now, prompt the user for it
        // In a real scenario, this would be stored when the skin is imported
        FileChooser baseOskChooser = new FileChooser();
        baseOskChooser.setTitle("Selecciona la skin base (.osk) para extraer assets");
        baseOskChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("osu! skin (.osk)", "*.osk")
        );
        File baseOskFile = baseOskChooser.showOpenDialog(mainWindow.getStage());
        if (baseOskFile == null) return;

        Path baseOskPath = baseOskFile.toPath();
        Path outputOskPath = outputFile.toPath();

        // Create and run the export task
        exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Empaquetando skin...");
                    updateProgress(0.3, 1.0);
                    OskPackager.packSkin(skin, baseOskPath, outputOskPath);
                    updateProgress(1.0, 1.0);
                    updateMessage("¡Exportación completada!");
                    return null;
                } catch (Exception ex) {
                    throw ex;
                }
            }
        };

        exportProgressBar.setVisible(true);
        exportProgressBar.progressProperty().bind(exportTask.progressProperty());
        exportStatusLabel.textProperty().bind(exportTask.messageProperty());

        exportTask.setOnSucceeded(e -> {
            exportProgressBar.setVisible(false);
            exportStatusLabel.textProperty().unbind();
            exportStatusLabel.setText("✓ Skin exportada exitosamente en: " + outputFile.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION,
                    "Skin exportada correctamente en:\n" + outputFile.getAbsolutePath(),
                    ButtonType.OK).showAndWait();
        });

        exportTask.setOnFailed(e -> {
            exportProgressBar.setVisible(false);
            exportStatusLabel.textProperty().unbind();
            Throwable ex = exportTask.getException();
            exportStatusLabel.setText("✗ Error en la exportación");
            showExportError("Error de exportación", ex.getMessage());
        });

        Thread exportThread = new Thread(exportTask, "OskExporterThread");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    private void showExportError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");
        return l;
    }

    private static void setVisible(Region node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}