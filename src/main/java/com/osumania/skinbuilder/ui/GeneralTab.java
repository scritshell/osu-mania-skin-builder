package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

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

    // Campos de metadatos (rellenados al importar)
    private final TextField skinNameField   = new TextField();
    private final TextField skinAuthorField = new TextField();
    private final Label skinVersionLabel    = new Label("—");
    private final Label detectedKeymodesLabel = new Label("—");

    // Selector de keymode para añadir
    private final ComboBox<String> keymodeCombo = new ComboBox<>();

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

        // Solo visible antes de importar
        skinInfoSection.setVisible(false);
        skinInfoSection.setManaged(false);
        addKeymodeSection.setVisible(false);
        addKeymodeSection.setManaged(false);

        VBox root = new VBox(24);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.getChildren().addAll(welcomeSection, skinInfoSection, addKeymodeSection);

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

        Button importBtn = new Button("📂   Importar skin (.osk)");
        importBtn.setStyle(
                "-fx-background-color: #e96d8a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 28;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        importBtn.setOnAction(e -> mainWindow.importOsk());

        Label hint = new Label("También puedes usar Archivo → Importar skin…");
        hint.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        box.getChildren().addAll(title, subtitle, importBtn, hint);

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