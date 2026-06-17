package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.OskPackager;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Pestaña General con fondo de constelaciones animadas (dark theme).
 *
 * <h2>Antes de importar</h2>
 * Muestra la pantalla de bienvenida sobre el fondo de constelaciones.
 *
 * <h2>Después de importar</h2>
 * Metadatos, panel de keymodes y exportación; el fondo de constelaciones
 * sigue visible detrás del contenido.
 */
public class GeneralTab extends StackPane {

    private final MainWindow mainWindow;

    // ── Secciones ─────────────────────────────────────────────────────────────
    private final VBox welcomeSection;
    private final VBox skinInfoSection;
    private final VBox addKeymodeSection;
    private final VBox exportSection;

    // ── Campos ────────────────────────────────────────────────────────────────
    private final TextField skinNameField     = new TextField();
    private final TextField skinAuthorField   = new TextField();
    private final Label     skinVersionLabel  = new Label("—");
    private final Label     detectedKeymodesLabel = new Label("—");
    private final ComboBox<String> keymodeCombo = new ComboBox<>();

    // ── Export ────────────────────────────────────────────────────────────────
    private Label       exportStatusLabel;
    private ProgressBar exportProgressBar;
    private Task<Void>  exportTask;

    private static final int[] AVAILABLE_KEYS = {1,2,3,4,5,6,7,8,9,10,12,14,16,18};

    // =========================================================================
    // Constructor
    // =========================================================================

    public GeneralTab(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        // ── Fondo de constelaciones (capa 0) ─────────────────────────────────
        ConstellationCanvas stars = new ConstellationCanvas(60, 160, 0.55, 0.18);
        stars.setMouseTransparent(true);
        // El canvas se redimensiona con el StackPane
        stars.widthProperty().bind(widthProperty());
        stars.heightProperty().bind(heightProperty());

        // ── Contenido desplazable (capa 1) ───────────────────────────────────
        welcomeSection    = buildWelcomeSection();
        skinInfoSection   = buildSkinInfoSection();
        addKeymodeSection = buildAddKeymodeSection();
        exportSection     = buildExportSection();

        setVisible(skinInfoSection,   false);
        setVisible(addKeymodeSection, false);
        setVisible(exportSection,     false);

        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 44, 36, 44));
        content.getChildren().addAll(
                welcomeSection, skinInfoSection, addKeymodeSection, exportSection);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.getStyleClass().add("constellation-scroll");

        getChildren().addAll(stars, scroll);
    }

    // =========================================================================
    // Secciones
    // =========================================================================

    // ── Bienvenida ────────────────────────────────────────────────────────────

    private VBox buildWelcomeSection() {
        VBox card = new VBox(22);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(560);
        card.setStyle(
                "-fx-background-color: rgba(8,15,32,0.82);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #1c3060;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 44 48 40 48;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,10,40,0.7),28,0,0,6);"
        );

        // Título con gradiente simulado via estilos
        Label title = new Label("osu!mania Skin Builder");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #9ec4ff;");

        Label version = new Label("v1.0  ·  Step 3 UI");
        version.setStyle("-fx-text-fill: #3a5a90; -fx-font-size: 11px;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1a3060; -fx-padding: 0;");

        Text subtitle = new Text(
                "Importa una skin .osk para empezar.\n" +
                        "El programa detectará los keymodes y creará\n" +
                        "las pestañas de edición automáticamente."
        );
        subtitle.setFill(Color.web("#7a96c0"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setStyle("-fx-font-size: 13px;");

        // Botones de importar
        Button importFileBtn = new Button("📦  Importar archivo (.osk)");
        styleButton(importFileBtn, "#1a4abf", "#2258d8", "#d8eaff");

        Button importFolderBtn = new Button("📁  Importar carpeta");
        styleButton(importFolderBtn, "#1e3d80", "#2750a8", "#c0d8ff");

        importFileBtn.setOnAction(e -> mainWindow.importOsk());
        importFolderBtn.setOnAction(e -> mainWindow.importFolder());

        HBox btnRow = new HBox(16, importFileBtn, importFolderBtn);
        btnRow.setAlignment(Pos.CENTER);

        Label hint = new Label("También en  Archivo → Importar…");
        hint.setStyle("-fx-text-fill: #2a3d60; -fx-font-size: 11px;");

        card.getChildren().addAll(title, version, sep, subtitle, btnRow, hint);

        HBox centered = new HBox(card);
        centered.setAlignment(Pos.CENTER);
        VBox outer = new VBox(centered);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setPadding(new Insets(40, 0, 0, 0));
        return outer;
    }

    // ── Info de la skin ───────────────────────────────────────────────────────

    private VBox buildSkinInfoSection() {
        VBox section = new VBox(12);
        section.setMaxWidth(640);

        Label header = sectionHeader("Skin importada");

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));
        grid.setStyle(
                "-fx-background-color: rgba(8,15,32,0.86);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #1a2e54;" +
                        "-fx-border-radius: 12;"
        );

        skinNameField.setPrefWidth(300);
        skinNameField.textProperty().addListener((obs, old, val) -> {
            SkinConfig s = mainWindow.getCurrentSkin();
            if (s != null) s.setSkinName(val);
        });

        skinAuthorField.setPrefWidth(300);
        skinAuthorField.textProperty().addListener((obs, old, val) -> {
            SkinConfig s = mainWindow.getCurrentSkin();
            if (s != null) s.setSkinAuthor(val);
        });

        skinVersionLabel.setStyle("-fx-text-fill: #7090b8; -fx-font-size: 13px;");
        detectedKeymodesLabel.setStyle(
                "-fx-text-fill: #4aae7a; -fx-font-size: 13px; -fx-font-weight: bold;");
        detectedKeymodesLabel.setWrapText(true);

        int row = 0;
        grid.add(fieldLabel("Nombre:"),          0, row); grid.add(skinNameField,          1, row++);
        grid.add(fieldLabel("Autor:"),            0, row); grid.add(skinAuthorField,         1, row++);
        grid.add(fieldLabel("Versión:"),          0, row); grid.add(skinVersionLabel,        1, row++);
        grid.add(fieldLabel("Keymodes leídos:"),  0, row); grid.add(detectedKeymodesLabel,   1, row);

        section.getChildren().addAll(header, grid);
        return section;
    }

    // ── Añadir keymode ────────────────────────────────────────────────────────

    private VBox buildAddKeymodeSection() {
        VBox section = new VBox(12);
        section.setMaxWidth(640);

        Label header = sectionHeader("Añadir keymode");

        Label desc = new Label(
                "Añade un keymode nuevo usando los assets de la skin importada.\n" +
                        "Personaliza cada columna en la pestaña que se creará."
        );
        desc.setStyle("-fx-text-fill: #5a7898; -fx-font-size: 12px;");
        desc.setWrapText(true);

        for (int k : AVAILABLE_KEYS) keymodeCombo.getItems().add(k + "K");
        keymodeCombo.setPromptText("Selecciona keymode…");
        keymodeCombo.setPrefWidth(170);

        Button addBtn = new Button("＋  Añadir pestaña");
        styleButton(addBtn, "#1a4038", "#226050", "#a0e8d0");
        addBtn.setOnAction(e -> addSelectedKeymode());

        HBox row = new HBox(12, keymodeCombo, addBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, desc, row);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: rgba(8,15,32,0.86);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #1a2e54;" +
                        "-fx-border-radius: 12;"
        );

        section.getChildren().addAll(header, card);
        return section;
    }

    // ── Exportar ──────────────────────────────────────────────────────────────

    private VBox buildExportSection() {
        VBox section = new VBox(12);
        section.setMaxWidth(640);

        Label header = sectionHeader("Exportar skin");

        Label desc = new Label(
                "Exporta un .osk completo con las imágenes tintadas y el skin.ini regenerado."
        );
        desc.setStyle("-fx-text-fill: #5a7898; -fx-font-size: 12px;");
        desc.setWrapText(true);

        Button exportBtn = new Button("💾  Exportar skin (.osk)");
        styleButton(exportBtn, "#1a4028", "#206030", "#90e8b8");
        exportBtn.setOnAction(e -> exportSkinOsk());

        exportStatusLabel = new Label("");
        exportStatusLabel.setStyle("-fx-text-fill: #5878a0; -fx-font-size: 12px;");

        exportProgressBar = new ProgressBar(0);
        exportProgressBar.setPrefWidth(Double.MAX_VALUE);
        exportProgressBar.setVisible(false);

        VBox card = new VBox(12, desc, exportBtn, exportStatusLabel, exportProgressBar);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: rgba(8,15,32,0.86);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #1a2e54;" +
                        "-fx-border-radius: 12;"
        );

        section.getChildren().addAll(header, card);
        return section;
    }

    // =========================================================================
    // Lógica
    // =========================================================================

    public void onSkinLoaded(SkinConfig config) {
        skinNameField.setText(config.getSkinName());
        skinAuthorField.setText(config.getSkinAuthor());
        skinVersionLabel.setText(config.getSkinVersion());

        String list = config.getKeymodes().stream()
                .map(ManiaKeyConfig::getDisplayName)
                .collect(Collectors.joining(", "));
        detectedKeymodesLabel.setText(list.isEmpty() ? "Ninguno detectado" : list);

        keymodeCombo.getItems().clear();
        for (int k : AVAILABLE_KEYS) {
            boolean present = config.getKeymode(k).isPresent();
            keymodeCombo.getItems().add(k + "K" + (present ? " (ya existe)" : ""));
        }

        setVisible(skinInfoSection,   true);
        setVisible(addKeymodeSection, true);
        setVisible(exportSection,     true);
    }

    private void addSelectedKeymode() {
        String sel = keymodeCombo.getValue();
        if (sel == null) return;
        SkinConfig skin = mainWindow.getCurrentSkin();
        if (skin == null) return;
        try {
            int keys = Integer.parseInt(sel.replaceAll("\\D.*", "").trim());
            ManiaKeyConfig km = skin.getOrCreateKeymode(keys);
            km.setEnabled(true);
            mainWindow.addKeymodeTab(km);
        } catch (NumberFormatException ignored) {}
    }

    private void exportSkinOsk() {
        SkinConfig skin = mainWindow.getCurrentSkin();
        if (skin == null) {
            showError("Sin skin", "Importa una skin primero.");
            return;
        }

        FileChooser outFc = new FileChooser();
        outFc.setTitle("Exportar skin como .osk");
        outFc.setInitialFileName("skin-export.osk");
        outFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("osu! skin", "*.osk"));
        File outFile = outFc.showSaveDialog(mainWindow.getStage());
        if (outFile == null) return;

        FileChooser baseFc = new FileChooser();
        baseFc.setTitle("Selecciona la skin base (.osk)");
        baseFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("osu! skin", "*.osk"));
        File baseFile = baseFc.showOpenDialog(mainWindow.getStage());
        if (baseFile == null) return;

        Path baseOskPath   = baseFile.toPath();
        Path outputOskPath = outFile.toPath();

        exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Empaquetando skin…");
                updateProgress(0.2, 1.0);
                OskPackager.packSkin(skin, baseOskPath, outputOskPath);
                updateProgress(1.0, 1.0);
                updateMessage("¡Exportación completada!");
                return null;
            }
        };

        exportProgressBar.setVisible(true);
        exportProgressBar.progressProperty().bind(exportTask.progressProperty());
        exportStatusLabel.textProperty().bind(exportTask.messageProperty());

        exportTask.setOnSucceeded(e -> {
            exportProgressBar.setVisible(false);
            exportStatusLabel.textProperty().unbind();
            exportStatusLabel.setStyle("-fx-text-fill: #4aae7a; -fx-font-size: 12px;");
            exportStatusLabel.setText("✓ Exportada: " + outFile.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION,
                    "Skin exportada:\n" + outFile.getAbsolutePath(), ButtonType.OK).showAndWait();
        });

        exportTask.setOnFailed(e -> {
            exportProgressBar.setVisible(false);
            exportStatusLabel.textProperty().unbind();
            exportStatusLabel.setStyle("-fx-text-fill: #d04040; -fx-font-size: 12px;");
            exportStatusLabel.setText("✗ Error en la exportación");
            showError("Error de exportación", exportTask.getException().getMessage());
        });

        Thread t = new Thread(exportTask, "OskExporterThread");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void styleButton(Button btn, String bgNormal, String bgHover, String textColor) {
        String base = String.format(
                "-fx-background-color: %s;" +
                        "-fx-text-fill: %s;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 11 26;" +
                        "-fx-background-radius: 9;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;",
                bgNormal, textColor);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(bgNormal, bgHover)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
    }

    private static Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 16));
        l.setStyle("-fx-text-fill: #7aacdc;");
        return l;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #4e6888;");
        return l;
    }

    private static void setVisible(Region node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void showError(String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }
}