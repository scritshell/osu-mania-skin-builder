package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.SkinConfig;
import com.osumania.skinbuilder.core.SkinIniParser;
import com.osumania.skinbuilder.core.SkinIniWriter;
import com.osumania.skinbuilder.image.PreviewAssetManager;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Ventana principal de la aplicación — versión rediseñada con dark theme
 * y pestaña de preview dedicada a pantalla completa.
 *
 * <h2>Pestañas</h2>
 * <ol>
 *   <li><b>General</b> — bienvenida con constelaciones, importar/exportar, metadatos.</li>
 *   <li><b>Preview</b> — aparece tras importar; muestra {@link PreviewPane} a pantalla
 *       completa para el keymode actualmente seleccionado.</li>
 *   <li><b>nK</b> — una pestaña por cada keymode encontrado en el skin.ini.</li>
 * </ol>
 */
public class MainWindow {

    // ── UI ────────────────────────────────────────────────────────────────────
    private Stage   stage;
    private TabPane tabPane;

    private Tab         generalTab;
    private GeneralTab  generalTabContent;

    /** Pestaña de Preview dedicada (null antes de importar). */
    private Tab         previewTab;
    private PreviewPane sharedPreviewPane;   // instancia única compartida

    // ── Modelo ────────────────────────────────────────────────────────────────
    private SkinConfig          currentSkin    = null;
    private Path                currentOskPath = null;
    private final PreviewAssetManager assetManager = new PreviewAssetManager();

    // =========================================================================
    // Entrada
    // =========================================================================

    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("osu!mania Skin Builder");

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);

        generalTabContent = new GeneralTab(this);
        generalTab = new Tab("⚙  General");
        generalTab.setClosable(false);
        generalTab.setContent(generalTabContent);
        tabPane.getTabs().add(generalTab);

        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar());
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1280, 760);
        loadCss(scene);

        stage.setScene(scene);
        stage.show();
    }

    // ── CSS ───────────────────────────────────────────────────────────────────

    private void loadCss(Scene scene) {
        try {
            String css = Objects.requireNonNull(
                    getClass().getResource("/com/osumania/skinbuilder/css/dark-theme.css")
            ).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (NullPointerException e) {
            System.err.println("[MainWindow] dark-theme.css no encontrado en resources — " +
                    "asegúrate de copiar dark-theme.css a src/main/resources/com/osumania/skinbuilder/css/");
        }
    }

    // =========================================================================
    // Import / Export
    // =========================================================================

    public void importOsk() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Abrir skin de osu!mania");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("osu! skin (.osk)", "*.osk"),
                new FileChooser.ExtensionFilter("skin.ini", "skin.ini"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        try {
            Path path = file.toPath();
            SkinConfig parsed = file.getName().toLowerCase().endsWith(".osk")
                    ? SkinIniParser.parseOsk(path)
                    : SkinIniParser.parseFile(path);
            this.currentOskPath = path;
            loadSkin(parsed);
        } catch (Exception e) {
            showError("Error de importación", "No se pudo leer la skin:\n" + e.getMessage());
        }
    }

    public void importFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta de la skin");
        File dir = dc.showDialog(stage);
        if (dir == null) return;
        try {
            Path skinIniPath = dir.toPath().resolve("skin.ini");
            if (!Files.exists(skinIniPath)) {
                showError("No encontrado", "No se encontró skin.ini en la carpeta seleccionada.");
                return;
            }
            SkinConfig parsed = SkinIniParser.parseFile(skinIniPath);
            this.currentOskPath = skinIniPath;
            loadSkin(parsed);
        } catch (Exception e) {
            showError("Error de importación", "No se pudo leer la skin:\n" + e.getMessage());
        }
    }

    // ── loadSkin ──────────────────────────────────────────────────────────────

    private void loadSkin(SkinConfig config) {
        this.currentSkin = config;

        if (currentOskPath != null) {
            assetManager.loadAssetsFromOsk(currentOskPath, config);
        }

        // Eliminar pestañas de keymodes y preview anteriores (preservar General)
        tabPane.getTabs().removeIf(tab -> tab != generalTab);
        previewTab = null;

        generalTabContent.onSkinLoaded(config);
        addPreviewTab();

        for (ManiaKeyConfig km : config.getKeymodes()) {
            addKeymodeTab(km);
        }

        stage.setTitle("osu!mania Skin Builder — " + config.getSkinName());
    }

    // ── Preview tab ───────────────────────────────────────────────────────────

    /**
     * Crea la pestaña de Preview dedicada con un {@link PreviewPane} que ocupa
     * toda la pantalla. Se puede actualizar llamando a {@link #updatePreview(ManiaKeyConfig)}.
     */
    private void addPreviewTab() {
        sharedPreviewPane = new PreviewPane();
        sharedPreviewPane.setAssetManager(assetManager);

        // El PreviewPane llena el contenedor
        BorderPane previewRoot = new BorderPane(sharedPreviewPane);
        previewRoot.setStyle("-fx-background-color: #080b14;");

        previewTab = new Tab("👁  Preview");
        previewTab.setClosable(false);
        previewTab.setContent(previewRoot);

        tabPane.getTabs().add(previewTab);
    }

    /**
     * Actualiza el keymode mostrado en la pestaña de Preview
     * y la selecciona automáticamente.
     *
     * @param km Keymode a previsualizar
     */
    public void updatePreview(ManiaKeyConfig km) {
        if (sharedPreviewPane == null || km == null) return;
        sharedPreviewPane.drawPreview(km);
        if (previewTab != null) {
            tabPane.getSelectionModel().select(previewTab);
        }
    }

    // ── Keymode tabs ──────────────────────────────────────────────────────────

    public void addKeymodeTab(ManiaKeyConfig km) {
        String label = km.getDisplayName();
        for (Tab tab : tabPane.getTabs()) {
            if (label.equals(tab.getText())) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }

        Tab tab = new Tab(label);
        tab.setContent(new KeymodeTab(km, assetManager, this));
        tab.setOnClosed(e -> {
            if (currentSkin != null) {
                currentSkin.getKeymode(km.getKeys()).ifPresent(k -> k.setEnabled(false));
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    // ── Export skin.ini ───────────────────────────────────────────────────────

    private void exportSkin() {
        if (currentSkin == null) {
            showError("Sin skin", "Importa una skin primero antes de exportar.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar skin.ini");
        fc.setInitialFileName("skin.ini");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("skin.ini", "*.ini"));
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try {
            SkinIniWriter.writeToFile(currentSkin, file.toPath());
            new Alert(Alert.AlertType.INFORMATION,
                    "skin.ini exportado correctamente:\n" + file.getAbsolutePath(),
                    ButtonType.OK).showAndWait();
        } catch (Exception e) {
            showError("Error de exportación", e.getMessage());
        }
    }

    // =========================================================================
    // Menú
    // =========================================================================

    private MenuBar buildMenuBar() {
        MenuBar bar = new MenuBar();

        // ── Archivo ──────────────────────────────────────────────────────────
        Menu menuFile = new Menu("_Archivo");

        MenuItem miImportFile   = new MenuItem("Importar archivo (.osk)…");
        miImportFile.setOnAction(e -> importOsk());

        MenuItem miImportFolder = new MenuItem("Importar carpeta de skin…");
        miImportFolder.setOnAction(e -> importFolder());

        MenuItem miExport       = new MenuItem("Exportar skin.ini…");
        miExport.setOnAction(e -> exportSkin());

        MenuItem miClose        = new MenuItem("Salir");
        miClose.setOnAction(e -> stage.close());

        menuFile.getItems().addAll(
                miImportFile, miImportFolder,
                new SeparatorMenuItem(), miExport,
                new SeparatorMenuItem(), miClose);

        // ── Keymode ──────────────────────────────────────────────────────────
        Menu menuKeymode = new Menu("_Keymode");
        for (int k : new int[]{4, 7, 8, 9, 10, 12, 14, 16, 18}) {
            final int keys = k;
            MenuItem mi = new MenuItem("Añadir " + keys + "K");
            mi.setOnAction(e -> {
                if (currentSkin == null) {
                    showError("Sin skin", "Importa una skin antes de añadir keymodes.");
                    return;
                }
                ManiaKeyConfig km = currentSkin.getOrCreateKeymode(keys);
                km.setEnabled(true);
                addKeymodeTab(km);
            });
            menuKeymode.getItems().add(mi);
        }

        // ── Vista ─────────────────────────────────────────────────────────────
        Menu menuView = new Menu("_Vista");
        MenuItem miShowPreview = new MenuItem("Ir a Preview");
        miShowPreview.setOnAction(e -> {
            if (previewTab != null) tabPane.getSelectionModel().select(previewTab);
        });
        menuView.getItems().add(miShowPreview);

        bar.getMenus().addAll(menuFile, menuKeymode, menuView);
        return bar;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    public SkinConfig getCurrentSkin()  { return currentSkin; }
    public Stage      getStage()        { return stage; }
    public PreviewAssetManager getAssetManager() { return assetManager; }
}