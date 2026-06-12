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

/**
 * Ventana principal de la aplicación.
 *
 * <h2>Flujo de UX correcto</h2>
 * <ol>
 *   <li>La app arranca mostrando <b>solo</b> la pestaña General con un botón de importar.</li>
 *   <li>El usuario importa un {@code .osk} (o un {@code skin.ini}).</li>
 *   <li>{@link SkinIniParser} lee el fichero y construye un {@link SkinConfig}.</li>
 *   <li>Se generan dinámicamente pestañas {@link KeymodeTab} por cada keymode encontrado.</li>
 *   <li>El usuario puede editar los keymodes existentes y añadir nuevos desde la pestaña General.</li>
 *   <li>Al exportar, {@link SkinIniWriter} regenera el {@code skin.ini}.</li>
 * </ol>
 *
 * <p><b>No se crean pestañas 4K/7K vacías al arrancar.</b> Todo parte de la skin real importada.</p>
 */
public class MainWindow {

    private Stage stage;
    private TabPane tabPane;

    /** Skin cargada actualmente; null si aún no se ha importado ninguna. */
    private SkinConfig currentSkin = null;
    
    /** Path del .osk importado; usado para cargar assets en el preview. */
    private Path currentOskPath = null;
    
    /** Gestor de caché de imágenes para el preview. */
    private PreviewAssetManager assetManager = new PreviewAssetManager();

    private Tab generalTab;
    private GeneralTab generalTabContent;

    // -------------------------------------------------------------------------
    // Entrada
    // -------------------------------------------------------------------------

    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("osu!mania Skin Builder — v1.0");

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);

        // Solo la pestaña General al arrancar
        generalTabContent = new GeneralTab(this);
        generalTab = new Tab("General");
        generalTab.setClosable(false);
        generalTab.setContent(generalTabContent);
        tabPane.getTabs().add(generalTab);

        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar());
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1200, 700);
        stage.setScene(scene);
        stage.show();
    }

    // -------------------------------------------------------------------------
    // Import / Export
    // -------------------------------------------------------------------------

    /**
     * Abre el diálogo de importación y carga la skin seleccionada.
     * Llamado desde {@link GeneralTab} y desde el menú Archivo.
     */
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

            // Guardar la Path del OSK para cargar assets después
            this.currentOskPath = path;
            loadSkin(parsed);

        } catch (Exception e) {
            showError("Error de importación",
                    "No se pudo leer la skin:\n" + e.getMessage());
        }
    }

    /**
     * Abre el diálogo de selección de carpeta y busca un skin.ini dentro.
     * Si lo encuentra, lo parsea y lo carga.
     */
    public void importFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta de la skin");
        File dir = dc.showDialog(stage);
        if (dir == null) return;

        try {
            Path skinIniPath = dir.toPath().resolve("skin.ini");
            if (!Files.exists(skinIniPath)) {
                showError("No encontrado",
                        "No se encontró skin.ini en la carpeta seleccionada.");
                return;
            }

            // Parsear el skin.ini
            SkinConfig parsed = SkinIniParser.parseFile(skinIniPath);

            // Guardar la Path del skin.ini para cargar assets
            this.currentOskPath = skinIniPath;
            loadSkin(parsed);

        } catch (Exception e) {
            showError("Error de importación",
                    "No se pudo leer la skin:\n" + e.getMessage());
        }
    }

    /**
     * Carga un {@link SkinConfig} ya parseado en la interfaz:
     * elimina las pestañas de keymodes anteriores y crea las nuevas.
     */
    private void loadSkin(SkinConfig config) {
        this.currentSkin = config;

        // Cargar assets del OSK para el preview
        if (currentOskPath != null) {
            assetManager.loadAssetsFromOsk(currentOskPath, config);
        }

        // Eliminar todas las pestañas de keymodes (preservar General)
        tabPane.getTabs().removeIf(tab -> tab != generalTab);

        // Informar a la pestaña General para que muestre los metadatos
        generalTabContent.onSkinLoaded(config);

        // Crear una pestaña por cada keymode encontrado en el skin.ini
        for (ManiaKeyConfig km : config.getKeymodes()) {
            addKeymodeTab(km);
        }

        stage.setTitle("osu!mania Skin Builder — " + config.getSkinName());
    }

    /**
     * Añade una nueva pestaña de keymode.
     * Si ya existe una pestaña con ese keymode, la selecciona sin duplicar.
     *
     * @param km Configuración de keymode a mostrar/editar
     */
    public void addKeymodeTab(ManiaKeyConfig km) {
        String label = km.getDisplayName();

        // Evitar duplicados: si ya existe esa pestaña, seleccionarla
        for (Tab tab : tabPane.getTabs()) {
            if (label.equals(tab.getText())) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }

        Tab tab = new Tab(label);
        tab.setContent(new KeymodeTab(km, assetManager));

        // Al cerrar la pestaña, deshabilitar el keymode en el modelo
        tab.setOnClosed(e -> {
            if (currentSkin != null) {
                currentSkin.getKeymode(km.getKeys())
                        .ifPresent(k -> k.setEnabled(false));
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void exportSkin() {
        if (currentSkin == null) {
            showError("Sin skin", "Importa una skin primero antes de exportar.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar skin.ini");
        fc.setInitialFileName("skin.ini");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("skin.ini", "*.ini")
        );
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try {
            SkinIniWriter.writeToFile(currentSkin, file.toPath());
            new Alert(Alert.AlertType.INFORMATION,
                    "skin.ini exportado correctamente en:\n" + file.getAbsolutePath(),
                    ButtonType.OK).showAndWait();
        } catch (Exception e) {
            showError("Error de exportación", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Menú
    // -------------------------------------------------------------------------

    private MenuBar buildMenuBar() {
        MenuBar bar = new MenuBar();

        // --- Archivo ---
        Menu menuFile = new Menu("_Archivo");

        MenuItem miImportFile = new MenuItem("Importar archivo (.osk)…");
        miImportFile.setOnAction(e -> importOsk());

        MenuItem miImportFolder = new MenuItem("Importar carpeta de skin…");
        miImportFolder.setOnAction(e -> importFolder());

        MenuItem miExport = new MenuItem("Exportar skin.ini…");
        miExport.setOnAction(e -> exportSkin());

        MenuItem miClose = new MenuItem("Salir");
        miClose.setOnAction(e -> stage.close());

        menuFile.getItems().addAll(miImportFile, miImportFolder, new SeparatorMenuItem(), miExport,
                new SeparatorMenuItem(), miClose);

        // --- Keymode ---
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

        bar.getMenus().addAll(menuFile, menuKeymode);
        return bar;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    /** Devuelve la skin cargada actualmente, o null si no hay ninguna. */
    public SkinConfig getCurrentSkin() { return currentSkin; }

    /** Devuelve el Stage principal (necesario para FileChooser). */
    public Stage getStage() { return stage; }
}