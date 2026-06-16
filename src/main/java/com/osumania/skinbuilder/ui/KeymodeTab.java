package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.image.PreviewAssetManager;
import com.osumania.skinbuilder.image.GifFrameExtractor;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Pestaña de edición para un keymode concreto (4K, 7K, etc.).
 * Usa {@link PreviewPane} en lugar de {@link PreviewCanvas} para evitar
 * la race condition de Prism en gc.drawImage().
 */
public class KeymodeTab extends SplitPane {

    private final ManiaKeyConfig config;
    private final PreviewAssetManager assetManager;
    private final PreviewPane previewPane;   // ← cambiado de PreviewCanvas

    // Listas para actualizar la UI dinámicamente al aplicar paletas
    private final List<ColorPicker> ricePickers = new ArrayList<>();
    private final List<ColorPicker> lnPickers   = new ArrayList<>();
    private boolean isUpdatingPalette = false;

    // =========================================================================
    // Paletas de gamemode
    // =========================================================================

    private record GamemodePalette(
            String name,
            IntPredicate isSpecialFn,
            java.awt.Color normalRice,  java.awt.Color specialRice,
            java.awt.Color normalLn,    java.awt.Color specialLn) {}

    private static final Map<Integer, List<GamemodePalette>> GAMEMODE_PALETTES = Map.of(
            6, List.of(
                    new GamemodePalette("6K Normal",   i -> false,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED),
                    new GamemodePalette("5K1S (BMS)", i -> i == 5,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED)
            ),
            8, List.of(
                    new GamemodePalette("4K4K",        i -> false,
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255),
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255)),
                    new GamemodePalette("7K1S (BMS)", i -> i == 7,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED)
            ),
            12, List.of(
                    new GamemodePalette("10K2S (BMS)", i -> i == 5 || i == 6,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED),
                    new GamemodePalette("6K6K",        i -> false,
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255),
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255))
            ),
            14, List.of(
                    new GamemodePalette("7K7K (sin scratch)", i -> false,
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255),
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255)),
                    new GamemodePalette("EZ2AC 5K4K5K", i -> i == 4 || i == 9,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED)
            ),
            16, List.of(
                    new GamemodePalette("7K1S DP (BMS)", i -> i == 7 || i == 15,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED),
                    new GamemodePalette("EZ2AC 5K4K5K Scratch", i -> i == 4 || i == 9 || i == 0 || i == 15,
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED)
            ),
            18, List.of(
                    new GamemodePalette("10K8K", i -> false,
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255),
                            java.awt.Color.WHITE, new java.awt.Color(0,100,255)),
                    new GamemodePalette("9K9K",  i -> false,
                            java.awt.Color.WHITE, new java.awt.Color(255,212,0),
                            java.awt.Color.WHITE, new java.awt.Color(255,212,0))
            )
    );

    // =========================================================================
    // Constructor
    // =========================================================================

    public KeymodeTab(ManiaKeyConfig config, PreviewAssetManager assetManager) {
        this.config       = config;
        this.assetManager = assetManager;

        // Preview (derecha)
        this.previewPane = new PreviewPane();
        this.previewPane.setAssetManager(assetManager);

        // Controles (izquierda)
        VBox leftContent = new VBox(20);
        leftContent.setPadding(new Insets(20, 30, 20, 30));
        leftContent.getChildren().addAll(
                buildHeader(),
                buildGeneralOptions(),
                buildDecorationPanel(),
                buildPalettePanel(),
                buildColumnsPanel()
        );

        ScrollPane scrollPane = new ScrollPane(leftContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f0f0f0;");

        StackPane rightContent = new StackPane(previewPane);
        rightContent.setStyle("-fx-background-color: #111318;");
        rightContent.setPadding(new Insets(20));
        VBox.setVgrow(previewPane, Priority.ALWAYS);
        HBox.setHgrow(previewPane, Priority.ALWAYS);

        getItems().addAll(scrollPane, rightContent);
        setDividerPositions(0.60);

        requestRedraw();
    }

    private void requestRedraw() {
        if (previewPane != null) previewPane.drawPreview(config);
    }

    // =========================================================================
    // Header
    // =========================================================================

    private HBox buildHeader() {
        Label title = new Label(config.getDisplayName() + " — Editando skin.ini");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: #333;");

        Label sub = new Label(
                config.getColumns().size() + " columnas  |  " +
                        "HitPos=" + config.getHitPosition() + "  |  " +
                        "ColStart=" + config.getColumnStart()
        );
        sub.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        VBox labels = new VBox(3, title, sub);
        HBox header = new HBox(labels);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // =========================================================================
    // Opciones generales
    // =========================================================================

    private TitledPane buildGeneralOptions() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        int row = 0;

        grid.add(label("HitPosition:"),  0, row);
        grid.add(intField(config.getHitPosition(),  0, 480, config::setHitPosition),  1, row++);

        grid.add(label("ColumnStart:"),  0, row);
        grid.add(intField(config.getColumnStart(),  0, 640, config::setColumnStart),  1, row++);

        grid.add(label("ScorePosition:"), 0, row);
        grid.add(intField(config.getScorePosition(), 0, 480, config::setScorePosition), 1, row++);

        grid.add(label("ComboPosition:"), 0, row);
        grid.add(intField(config.getComboPosition(), 0, 480, config::setComboPosition), 1, row++);

        Separator sep = new Separator();
        grid.add(sep, 0, row++, 4, 1);

        // Checkboxes izquierda
        CheckBox upsideDown  = check("UpsideDown",           config.isUpsideDown(),       config::setUpsideDown);
        CheckBox judgement   = check("JudgementLine",        config.isJudgementLine(),    config::setJudgementLine);
        CheckBox keysUnder   = check("KeysUnderNotes",       config.isKeysUnderNotes(),   config::setKeysUnderNotes);
        CheckBox splitStages = check("SplitStages (10K+)",   config.isSplitStages(),      config::setSplitStages);
        splitStages.setDisable(config.getKeys() < 10);

        VBox leftChecks = new VBox(8, upsideDown, judgement, keysUnder, splitStages);
        grid.add(leftChecks, 0, row, 2, 1);

        // Controles derecha
        CheckBox separateLn   = check("Colores LN separados",      config.isUseSeparateLnColor(),      config::setUseSeparateLnColor);
        CheckBox separateTail = check("LN tail propia (NoteImageXT)", config.isUseSeparateLnTail(),   config::setUseSeparateLnTail);

        Label percySizeLbl = new Label("Percy Size (px):");
        percySizeLbl.setStyle("-fx-font-size: 12px;");
        Spinner<Integer> percySpinner = new Spinner<>(0, 400, config.getPercySize(), 10);
        percySpinner.setEditable(true);
        percySpinner.setPrefWidth(90);
        percySpinner.valueProperty().addListener((obs, old, val) -> { config.setPercySize(val); requestRedraw(); });
        HBox percySizeRow = new HBox(8, percySizeLbl, percySpinner);
        percySizeRow.setAlignment(Pos.CENTER_LEFT);

        Label percyShapeLbl = new Label("Forma punta:");
        percyShapeLbl.setStyle("-fx-font-size: 12px;");
        ComboBox<ManiaKeyConfig.PercyShape> percyShapeBox = new ComboBox<>();
        percyShapeBox.getItems().setAll(ManiaKeyConfig.PercyShape.values());
        percyShapeBox.setValue(config.getPercyShape());
        percyShapeBox.valueProperty().addListener((obs, old, val) -> { config.setPercyShape(val); requestRedraw(); });
        HBox percyShapeRow = new HBox(8, percyShapeLbl, percyShapeBox);
        percyShapeRow.setAlignment(Pos.CENTER_LEFT);

        Label offsetLbl = new Label("Receptor Offset (Y):");
        offsetLbl.setStyle("-fx-font-size: 12px;");
        Spinner<Integer> offsetSpinner = new Spinner<>(-200, 200, config.getReceptorOffset(), 1);
        offsetSpinner.setEditable(true);
        offsetSpinner.setPrefWidth(90);
        offsetSpinner.valueProperty().addListener((obs, old, val) -> { config.setReceptorOffset(val); requestRedraw(); });
        HBox offsetRow = new HBox(8, offsetLbl, offsetSpinner);
        offsetRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox transpCheck = check("Transparencia global", config.isUseGlobalTransparency(), config::setUseGlobalTransparency);
        Slider alphaSlider = new Slider(0, 255, config.getGlobalAlpha());
        alphaSlider.setShowTickLabels(true);
        alphaSlider.setMajorTickUnit(64);
        alphaSlider.setPrefWidth(160);
        alphaSlider.setDisable(!config.isUseGlobalTransparency());

        Label alphaValue = new Label(String.valueOf(config.getGlobalAlpha()));
        alphaValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        transpCheck.selectedProperty().addListener((obs, old, val) -> { alphaSlider.setDisable(!val); requestRedraw(); });
        alphaSlider.valueProperty().addListener((obs, old, val) -> {
            config.setGlobalAlpha(val.intValue());
            alphaValue.setText(String.valueOf(val.intValue()));
            requestRedraw();
        });

        HBox alphaRow = new HBox(8, alphaSlider, alphaValue);
        alphaRow.setAlignment(Pos.CENTER_LEFT);

        VBox rightChecks = new VBox(8, separateLn, separateTail, percySizeRow, percyShapeRow, offsetRow, transpCheck, alphaRow);
        rightChecks.setMinWidth(320);
        grid.add(rightChecks, 2, row, 2, 1);

        TitledPane pane = new TitledPane("Opciones generales", grid);
        pane.setCollapsible(true);
        pane.setStyle("-fx-font-weight: bold;");
        return pane;
    }

    // =========================================================================
    // Panel de Decoración del Stage — con botones de carga de imagen/GIF
    // =========================================================================

    private TitledPane buildDecorationPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));

        Label tip = new Label(
                "💡 Carga imágenes locales (.png / .gif) para ver el stage con tus assets antes de exportar."
        );
        tip.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        tip.setWrapText(true);
        box.getChildren().add(tip);

        box.getChildren().addAll(
                buildStageImageRow("StageLeftImage:",   config.getStageLeftImage(),   config::setStageLeftImage),
                buildStageImageRow("StageRightImage:",  config.getStageRightImage(),  config::setStageRightImage),
                buildStageImageRow("StageBottomImage:", config.getStageBottomImage(), config::setStageBottomImage),
                buildStageImageRow("StageHintImage:",   config.getStageHintImage(),   config::setStageHintImage)
        );

        TitledPane pane = new TitledPane("Decoración del Stage", box);
        pane.setCollapsible(true);
        pane.setExpanded(false);
        pane.setStyle("-fx-font-weight: bold;");
        return pane;
    }

    private HBox buildStageImageRow(String labelText,
                                    String currentVal,
                                    java.util.function.Consumer<String> setter) {
        Label lbl = new Label(labelText);
        lbl.setPrefWidth(130);
        lbl.setStyle("-fx-font-size: 13px;");

        TextField tf = new TextField(currentVal == null ? "" : currentVal);
        tf.setPrefWidth(140);
        tf.textProperty().addListener((obs, old, val) -> {
            setter.accept(val.trim().isEmpty() ? null : val.trim());
            requestRedraw();
        });

        Button btnLoad = new Button("📂 Cargar");
        btnLoad.setStyle("-fx-cursor: hand; -fx-font-size: 11px;");
        btnLoad.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Cargar imagen para " + labelText.replace(":", ""));
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.gif")
            );
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file == null) return;

            String fileName = file.getName();
            tf.setText(fileName);
            setter.accept(fileName);

            if (fileName.toLowerCase().endsWith(".gif")) {
                loadGifAsync(file, fileName);
            } else {
                try {
                    java.awt.image.BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        assetManager.putStageImage(fileName, img);
                        requestRedraw();
                    }
                } catch (Exception ex) {
                    System.err.println("Error leyendo PNG: " + ex.getMessage());
                }
            }
        });

        HBox row = new HBox(10, lbl, tf, btnLoad);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void loadGifAsync(File file, String assetName) {
        Task<List<GifFrameExtractor.GifFrame>> task = new Task<>() {
            @Override
            protected List<GifFrameExtractor.GifFrame> call() throws Exception {
                return GifFrameExtractor.extract(file);
            }
        };
        task.setOnSucceeded(e -> {
            List<GifFrameExtractor.GifFrame> frames = task.getValue();
            if (frames != null && !frames.isEmpty()) {
                assetManager.putStageGif(assetName, frames);
                previewPane.enableGifAnimation(true);
                requestRedraw();
            }
        });
        task.setOnFailed(e ->
                System.err.println("Error extrayendo GIF: " + task.getException()));
        Thread t = new Thread(task, "gif-loader");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Panel de paletas de gamemode
    // =========================================================================

    private VBox buildPalettePanel() {
        int keys = config.getKeys();
        Label title = new Label("Paletas de Gamemode — " + keys + "K");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

        FlowPane buttonsBox = new FlowPane(10, 6);   // hGap=10, vGap=6
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        List<GamemodePalette> palettes = GAMEMODE_PALETTES.get(keys);

        // Fallback genérico si el keymode no tiene paletas específicas
        if (palettes == null || palettes.isEmpty()) {
            palettes = List.of(
                    new GamemodePalette("IIDX Genérico",
                            i -> config.isSpecialColumn(i, keys),
                            java.awt.Color.WHITE, java.awt.Color.RED,
                            java.awt.Color.WHITE, java.awt.Color.RED),
                    new GamemodePalette("Reset a Blanco",
                            i -> false,
                            java.awt.Color.WHITE, java.awt.Color.WHITE,
                            java.awt.Color.WHITE, java.awt.Color.WHITE)
            );
        }

        for (GamemodePalette p : palettes) {
            Button btn = new Button(p.name());
            btn.setStyle("-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 14; " +
                    "-fx-background-radius: 6;");
            btn.setOnAction(e -> applyPalette(p));
            buttonsBox.getChildren().add(btn);
        }

        // Botón "Reset a Blanco" siempre disponible
        Button resetBtn = new Button("⬜  Reset a Blanco");
        resetBtn.setStyle("-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 14; " +
                "-fx-background-radius: 6; -fx-background-color: #e0e0e0;");
        resetBtn.setOnAction(e -> applyPalette(new GamemodePalette("Reset",
                i -> false,
                java.awt.Color.WHITE, java.awt.Color.WHITE,
                java.awt.Color.WHITE, java.awt.Color.WHITE)));
        buttonsBox.getChildren().add(resetBtn);

        VBox root = new VBox(10, title, buttonsBox);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;");
        return root;
    }

    private void applyPalette(GamemodePalette p) {
        isUpdatingPalette = true;
        try {
            int keys = config.getKeys();
            for (int i = 0; i < keys; i++) {
                ManiaKeyConfig.ColumnConfig col = config.getColumn(i);
                boolean isSpecial = p.isSpecialFn().test(i);

                col.riceColor = isSpecial ? p.specialRice() : p.normalRice();
                col.lnColor   = isSpecial ? p.specialLn()   : p.normalLn();

                if (i < ricePickers.size()) ricePickers.get(i).setValue(toFx(col.riceColor));
                if (i < lnPickers.size())   lnPickers.get(i).setValue(toFx(col.lnColor));
            }
        } finally {
            isUpdatingPalette = false;
        }
        requestRedraw();
    }

    // =========================================================================
    // Panel de columnas
    // =========================================================================

    private TitledPane buildColumnsPanel() {
        int keys = config.getKeys();
        ricePickers.clear();
        lnPickers.clear();

        HBox columnsRow = new HBox(10);
        columnsRow.setPadding(new Insets(14));
        columnsRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < keys; i++) columnsRow.getChildren().add(buildColumnCard(i));

        ScrollPane scroll = new ScrollPane(columnsRow);
        scroll.setFitToHeight(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        scroll.setPrefHeight(380);

        TitledPane pane = new TitledPane("Columnas (" + keys + "K)", scroll);
        pane.setCollapsible(true);
        pane.setExpanded(true);
        pane.setStyle("-fx-font-weight: bold;");
        return pane;
    }

    private VBox buildColumnCard(int colIndex) {
        ManiaKeyConfig.ColumnConfig col = config.getColumn(colIndex);
        boolean isSpecial = config.isSpecialColumn(colIndex, config.getKeys());

        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMinWidth(130);
        card.setPrefWidth(130);

        String borderColor = isSpecial ? "#c0a000" : "#c4c4c4";
        String bgColor     = isSpecial ? "#fffbea" : "#ffffff";
        card.setStyle(
                "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 9;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 9;" +
                        "-fx-padding: 14 10;"
        );

        String titleText = "Col " + (colIndex + 1) + (isSpecial ? "  ★" : "");
        Label title = new Label(titleText);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;"
                + (isSpecial ? " -fx-text-fill: #c0a000;" : ""));

        HBox widthRow = new HBox(6);
        widthRow.setAlignment(Pos.CENTER);
        Label widthLbl = new Label("Width:");
        widthLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField widthField = intField(col.columnWidth, 0, 500, val -> col.columnWidth = val);
        widthField.setPrefWidth(52);
        widthRow.getChildren().addAll(widthLbl, widthField);

        // Rice color
        VBox riceBox = new VBox(4);
        riceBox.setAlignment(Pos.CENTER);
        Label riceLbl = new Label("Rice");
        riceLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        ColorPicker ricePicker = new ColorPicker(toFx(col.riceColor));
        ricePicker.setPrefWidth(115);
        ricePicker.setOnAction(e -> {
            if (!isUpdatingPalette) { col.riceColor = toAwt(ricePicker.getValue()); requestRedraw(); }
        });
        ricePickers.add(ricePicker);
        riceBox.getChildren().addAll(riceLbl, ricePicker);

        // LN color
        VBox lnBox = new VBox(4);
        lnBox.setAlignment(Pos.CENTER);
        Label lnLbl = new Label("LN");
        lnLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        ColorPicker lnPicker = new ColorPicker(toFx(col.lnColor));
        lnPicker.setPrefWidth(115);
        lnPicker.setOnAction(e -> {
            if (!isUpdatingPalette) { col.lnColor = toAwt(lnPicker.getValue()); requestRedraw(); }
        });
        lnPickers.add(lnPicker);
        lnBox.getChildren().addAll(lnLbl, lnPicker);

        // ColourLight
        VBox lightBox = new VBox(4);
        lightBox.setAlignment(Pos.CENTER);
        Label lightLbl = new Label("ColourLight");
        lightLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        ColorPicker lightPicker = new ColorPicker(toFx(col.lightColor));
        lightPicker.setPrefWidth(115);
        lightPicker.setOnAction(e -> { col.lightColor = toAwt(lightPicker.getValue()); requestRedraw(); });
        lightBox.getChildren().addAll(lightLbl, lightPicker);

        Label noteImgLbl = new Label(truncate(col.noteImageRice, 15));
        noteImgLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        card.getChildren().addAll(title, widthRow, riceBox, lnBox, lightBox, noteImgLbl);
        return card;
    }

    // =========================================================================
    // Helpers conversión de color
    // =========================================================================

    private static javafx.scene.paint.Color toFx(java.awt.Color c) {
        if (c == null) return javafx.scene.paint.Color.WHITE;
        return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
    }

    private static java.awt.Color toAwt(javafx.scene.paint.Color c) {
        return new java.awt.Color((float) c.getRed(), (float) c.getGreen(),
                (float) c.getBlue(), (float) c.getOpacity());
    }

    // =========================================================================
    // Helpers UI
    // =========================================================================

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px;");
        return l;
    }

    private TextField intField(int initial, int min, int max,
                               java.util.function.IntConsumer setter) {
        TextField tf = new TextField(String.valueOf(initial));
        tf.setPrefWidth(80);
        tf.setStyle("-fx-font-size: 13px;");
        Runnable apply = () -> {
            try {
                int val = Math.max(min, Math.min(max, Integer.parseInt(tf.getText().trim())));
                setter.accept(val);
                tf.setText(String.valueOf(val));
                requestRedraw();
            } catch (NumberFormatException ignored) {
                tf.setText(String.valueOf(initial));
            }
        };
        tf.setOnAction(e -> apply.run());
        tf.focusedProperty().addListener((obs, was, is) -> { if (!is) apply.run(); });
        return tf;
    }

    private CheckBox check(String text, boolean selected,
                           java.util.function.Consumer<Boolean> onChange) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setStyle("-fx-font-size: 12px;");
        cb.selectedProperty().addListener((obs, old, val) -> { onChange.accept(val); requestRedraw(); });
        return cb;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}