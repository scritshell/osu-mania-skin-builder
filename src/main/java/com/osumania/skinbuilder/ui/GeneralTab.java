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
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Pestaña General — rediseño limpio.
 *
 * <p>La constelación es la capa más baja del StackPane; el contenido
 * (tarjeta semitransparente centrada) está encima y nunca se solapa.</p>
 */
public class GeneralTab extends StackPane {

    // ── Paleta ────────────────────────────────────────────────────────────────
    // Fondo panel: rgba(10,16,32,0.88)  Borde: #1A3060  Acento: #4A8FFF
    // Texto primario: #C8DCFF  Texto secundario: #4A6890  Éxito: #3EC87A  Error: #E05050

    private static final String PANEL_BG     = "rgba(10,16,32,0.90)";
    private static final String PANEL_BORDER  = "#182848";
    private static final String TEXT_PRIMARY  = "#C8DCFF";
    private static final String TEXT_SECONDARY= "#4A6890";
    private static final String TEXT_ACCENT   = "#4A8FFF";
    private static final String BTN_IMPORT    = "linear-gradient(to bottom, #1E4AD0, #1638A0)";
    private static final String BTN_FOLDER    = "linear-gradient(to bottom, #1A3880, #122868)";
    private static final String BTN_EXPORT    = "linear-gradient(to bottom, #1A5038, #123828)";
    private static final String BTN_ADD       = "linear-gradient(to bottom, #1A4848, #0E3030)";

    // ── Refs UI ───────────────────────────────────────────────────────────────
    private final MainWindow mainWindow;

    private final VBox welcomeSection;
    private final VBox skinInfoSection;
    private final VBox addKeymodeSection;
    private final VBox exportSection;

    private final TextField skinNameField        = new TextField();
    private final TextField skinAuthorField      = new TextField();
    private final Label     skinVersionLabel     = new Label("—");
    private final Label     detectedKeymodesLabel = new Label("—");
    private final ComboBox<String> keymodeCombo  = new ComboBox<>();

    private Label       exportStatusLabel;
    private ProgressBar exportProgressBar;
    private Task<Void>  exportTask;

    private static final int[] AVAILABLE_KEYS = {1,2,3,4,5,6,7,8,9,10,12,14,16,18};

    // =========================================================================
    // Constructor
    // =========================================================================

    public GeneralTab(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        // ── Capa 0: constelaciones ────────────────────────────────────────────
        // isResizable()=true → el StackPane llama a resize() automáticamente.
        // NO usar bind(): colisiona con resize() y lanza BoundValue exception.
        ConstellationCanvas stars = new ConstellationCanvas(50, 145, 0.25, 0.14);
        stars.setMouseTransparent(true);

        // ── Capa 1: contenido desplazable ─────────────────────────────────────
        welcomeSection    = buildWelcomeSection();
        skinInfoSection   = buildSkinInfoSection();
        addKeymodeSection = buildAddKeymodeSection();
        exportSection     = buildExportSection();

        hide(skinInfoSection);
        hide(addKeymodeSection);
        hide(exportSection);

        VBox content = new VBox(20);
        content.setPadding(new Insets(32, 40, 32, 40));
        content.getChildren().addAll(
                welcomeSection, skinInfoSection, addKeymodeSection, exportSection);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;");

        getChildren().addAll(stars, scroll);
    }

    // =========================================================================
    // Sección bienvenida
    // =========================================================================

    private VBox buildWelcomeSection() {
        // Tarjeta central con glassmorphism leve
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(500);
        card.setStyle(
                "-fx-background-color: rgba(8,14,30,0.88);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #1A3060;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 40 44 36 44;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,8,30,0.75), 32, 0, 0, 8);");

        // Título principal
        Label title = new Label("osu!mania Skin Builder");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");

        Label subtitle = new Label("Crea y exporta skins para todos los keymodes");
        subtitle.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");

        // Separador
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #182848;");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));

        Label instructions = new Label(
                "Importa un archivo .osk o una carpeta para empezar.\n" +
                        "El skin.ini se leerá automáticamente.");
        instructions.setWrapText(true);
        instructions.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px; -fx-text-alignment: center;");

        // Botones de importar
        Button importFileBtn   = buildBtn("📦  Importar .osk", BTN_IMPORT);
        Button importFolderBtn = buildBtn("📁  Importar carpeta", BTN_FOLDER);
        importFileBtn.setOnAction(e   -> mainWindow.importOsk());
        importFolderBtn.setOnAction(e -> mainWindow.importFolder());

        HBox btnRow = new HBox(12, importFileBtn, importFolderBtn);
        btnRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, subtitle, sep, instructions, btnRow);

        HBox centered = new HBox(card);
        centered.setAlignment(Pos.CENTER);
        VBox outer = new VBox(centered);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setPadding(new Insets(32, 0, 0, 0));
        return outer;
    }

    // =========================================================================
    // Sección info de la skin
    // =========================================================================

    private VBox buildSkinInfoSection() {
        VBox section = new VBox(10);
        section.setMaxWidth(580);

        section.getChildren().add(sectionHeader("Skin importada"));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        grid.setStyle(panelStyle());

        skinNameField.setStyle(fieldStyle());
        skinNameField.setPrefWidth(300);
        skinNameField.textProperty().addListener((obs, old, val) -> {
            SkinConfig s = mainWindow.getCurrentSkin();
            if (s != null) s.setSkinName(val);
        });

        skinAuthorField.setStyle(fieldStyle());
        skinAuthorField.setPrefWidth(300);
        skinAuthorField.textProperty().addListener((obs, old, val) -> {
            SkinConfig s = mainWindow.getCurrentSkin();
            if (s != null) s.setSkinAuthor(val);
        });

        skinVersionLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");
        detectedKeymodesLabel.setStyle(
                "-fx-text-fill: #3EC87A; -fx-font-size: 13px; -fx-font-weight: bold;");
        detectedKeymodesLabel.setWrapText(true);

        int row = 0;
        grid.add(fieldLabel("Nombre:"),         0, row); grid.add(skinNameField,         1, row++);
        grid.add(fieldLabel("Autor:"),           0, row); grid.add(skinAuthorField,        1, row++);
        grid.add(fieldLabel("Versión:"),         0, row); grid.add(skinVersionLabel,       1, row++);
        grid.add(fieldLabel("Keymodes:"),        0, row); grid.add(detectedKeymodesLabel,  1, row);

        section.getChildren().add(grid);
        return section;
    }

    // =========================================================================
    // Sección añadir keymode
    // =========================================================================

    private VBox buildAddKeymodeSection() {
        VBox section = new VBox(10);
        section.setMaxWidth(580);
        section.getChildren().add(sectionHeader("Añadir keymode"));

        Label desc = new Label(
                "Crea una pestaña de edición para un keymode nuevo basado en los assets importados.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");

        for (int k : AVAILABLE_KEYS) keymodeCombo.getItems().add(k + "K");
        keymodeCombo.setPromptText("Seleccionar…");
        keymodeCombo.setPrefWidth(150);
        keymodeCombo.setStyle(comboStyle());

        Button addBtn = buildBtn("＋  Añadir pestaña", BTN_ADD);
        addBtn.setOnAction(e -> addSelectedKeymode());

        HBox row = new HBox(10, keymodeCombo, addBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, desc, row);
        card.setPadding(new Insets(18));
        card.setStyle(panelStyle());
        section.getChildren().add(card);
        return section;
    }

    // =========================================================================
    // Sección exportar
    // =========================================================================

    private VBox buildExportSection() {
        VBox section = new VBox(10);
        section.setMaxWidth(580);
        section.getChildren().add(sectionHeader("Exportar skin"));

        Label desc = new Label(
                "Genera un .osk completo con las imágenes tintadas y el skin.ini actualizado.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");

        Button exportBtn = buildBtn("💾  Exportar skin (.osk)", BTN_EXPORT);
        exportBtn.setOnAction(e -> exportSkinOsk());

        exportStatusLabel = new Label("");
        exportStatusLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");

        exportProgressBar = new ProgressBar(0);
        exportProgressBar.setPrefWidth(Double.MAX_VALUE);
        exportProgressBar.setVisible(false);
        exportProgressBar.setStyle(
                "-fx-accent: #4A8FFF;" +
                        "-fx-background-color: #0A1428;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-color: #1A3060;" +
                        "-fx-border-radius: 4;");

        VBox card = new VBox(12, desc, exportBtn, exportStatusLabel, exportProgressBar);
        card.setPadding(new Insets(18));
        card.setStyle(panelStyle());
        section.getChildren().add(card);
        return section;
    }

    // =========================================================================
    // Lógica pública
    // =========================================================================

    public void onSkinLoaded(SkinConfig config) {
        skinNameField.setText(config.getSkinName());
        skinAuthorField.setText(config.getSkinAuthor());
        skinVersionLabel.setText(config.getSkinVersion());

        String list = config.getKeymodes().stream()
                .map(ManiaKeyConfig::getDisplayName)
                .collect(Collectors.joining("  ·  "));
        detectedKeymodesLabel.setText(list.isEmpty() ? "Ninguno detectado" : list);

        keymodeCombo.getItems().clear();
        for (int k : AVAILABLE_KEYS) {
            boolean present = config.getKeymode(k).isPresent();
            keymodeCombo.getItems().add(k + "K" + (present ? " ✓" : ""));
        }

        show(skinInfoSection);
        show(addKeymodeSection);
        show(exportSection);
    }

    // =========================================================================
    // Lógica interna
    // =========================================================================

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
        if (skin == null) { showError("Sin skin", "Importa una skin primero."); return; }

        FileChooser outFc = new FileChooser();
        outFc.setTitle("Exportar skin como .osk");
        outFc.setInitialFileName("skin-export.osk");
        outFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("osu! skin", "*.osk"));
        File outFile = outFc.showSaveDialog(mainWindow.getStage());
        if (outFile == null) return;

        FileChooser baseFc = new FileChooser();
        baseFc.setTitle("Skin base para extraer assets (.osk)");
        baseFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("osu! skin", "*.osk"));
        File baseFile = baseFc.showOpenDialog(mainWindow.getStage());
        if (baseFile == null) return;

        Path baseOskPath   = baseFile.toPath();
        Path outputOskPath = outFile.toPath();

        exportTask = new Task<>() {
            @Override protected Void call() throws Exception {
                updateMessage("Empaquetando…");
                updateProgress(0.2, 1.0);
                OskPackager.packSkin(skin, baseOskPath, outputOskPath);
                updateProgress(1.0, 1.0);
                updateMessage("Exportación completada");
                return null;
            }
        };

        exportProgressBar.setVisible(true);
        exportProgressBar.progressProperty().bind(exportTask.progressProperty());
        exportStatusLabel.textProperty().bind(exportTask.messageProperty());

        exportTask.setOnSucceeded(e -> {
            exportProgressBar.setVisible(false);
            exportStatusLabel.textProperty().unbind();
            exportStatusLabel.setStyle("-fx-text-fill: #3EC87A; -fx-font-size: 12px;");
            exportStatusLabel.setText("✓ Guardada en: " + outFile.getName());
            new Alert(Alert.AlertType.INFORMATION,
                    "Skin exportada:\n" + outFile.getAbsolutePath(), ButtonType.OK).showAndWait();
        });

        exportTask.setOnFailed(e -> {
            exportProgressBar.setVisible(false);
            exportStatusLabel.textProperty().unbind();
            exportStatusLabel.setStyle("-fx-text-fill: #E05050; -fx-font-size: 12px;");
            exportStatusLabel.setText("✗ Error: " + exportTask.getException().getMessage());
        });

        Thread t = new Thread(exportTask, "osk-export");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Helpers de estilo
    // =========================================================================

    private static String panelStyle() {
        return  "-fx-background-color: rgba(8,14,30,0.88);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #182848;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;";
    }

    private static String fieldStyle() {
        return  "-fx-background-color: #060C1C;" +
                "-fx-text-fill: #A8C8F0;" +
                "-fx-border-color: #1A3060;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-font-size: 13px;";
    }

    private static String comboStyle() {
        return  "-fx-background-color: #060C1C;" +
                "-fx-border-color: #1A3060;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;";
    }

    private static Button buildBtn(String text, String gradient) {
        Button btn = new Button(text);
        String base = "-fx-background-color: " + gradient + ";" +
                "-fx-text-fill: #C8DCFF;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 24;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;";
        btn.setStyle(base);
        // hover: aumentar brillo con opacidad
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private static Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 14));
        l.setStyle("-fx-text-fill: #4A8FFF; -fx-padding: 0 0 2 0;");
        return l;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(80);
        l.setStyle("-fx-text-fill: #3A5880; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private static void hide(Region r) { r.setVisible(false); r.setManaged(false); }
    private static void show(Region r) { r.setVisible(true);  r.setManaged(true); }

    private void showError(String h, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR, m, ButtonType.OK);
        a.setHeaderText(h);
        a.showAndWait();
    }
}