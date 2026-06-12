package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.image.PreviewAssetManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña de edición para un keymode concreto (4K, 7K, etc.).
 */
public class KeymodeTab extends SplitPane {

    private final ManiaKeyConfig config;
    private final PreviewAssetManager assetManager;
    private final PreviewCanvas previewCanvas;

    // Listas para actualizar la UI dinámicamente al aplicar paletas
    private final List<ColorPicker> ricePickers = new ArrayList<>();
    private final List<ColorPicker> lnPickers = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Sistema Escalable de Paletas (Java 17 Record)
    // -------------------------------------------------------------------------
    private record Palette(String name,
                           java.awt.Color oddRice, java.awt.Color evenRice, java.awt.Color specialRice,
                           java.awt.Color oddLn, java.awt.Color evenLn, java.awt.Color specialLn) {}

    private static final List<Palette> PALETTES = List.of(
            new Palette("🔴🔵 IIDX / O2Jam",
                    java.awt.Color.WHITE, new java.awt.Color(0, 100, 255), java.awt.Color.RED,
                    java.awt.Color.WHITE, new java.awt.Color(0, 100, 255), java.awt.Color.RED),
            new Palette("⚪⚫ DJMAX",
                    new java.awt.Color(200, 200, 200), new java.awt.Color(100, 100, 100), java.awt.Color.RED,
                    new java.awt.Color(200, 200, 200), new java.awt.Color(100, 100, 100), java.awt.Color.RED),
            new Palette("🎨 Reset",
                    java.awt.Color.WHITE, java.awt.Color.WHITE, java.awt.Color.WHITE,
                    java.awt.Color.WHITE, java.awt.Color.WHITE, java.awt.Color.WHITE)
    );

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public KeymodeTab(ManiaKeyConfig config, PreviewAssetManager assetManager) {
        this.config = config;
        this.assetManager = assetManager;

        // 1. Instanciar Canvas
        this.previewCanvas = new PreviewCanvas();
        this.previewCanvas.setAssetManager(assetManager);

        // 2. Lado Izquierdo (Controles)
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

        // 3. Lado Derecho (Vista Previa)
        StackPane rightContent = new StackPane(previewCanvas);
        rightContent.setStyle("-fx-background-color: #111318;");
        rightContent.setPadding(new Insets(20));

        getItems().addAll(scrollPane, rightContent);
        setDividerPositions(0.60);

        // Dibujo inicial
        requestRedraw();
    }

    // -------------------------------------------------------------------------
    // Sincronización en Tiempo Real
    // -------------------------------------------------------------------------
    private void requestRedraw() {
        if (previewCanvas != null) {
            previewCanvas.drawPreview(config);
        }
    }

    // -------------------------------------------------------------------------
    // UI: Header
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // UI: Opciones Generales
    // -------------------------------------------------------------------------

    private TitledPane buildGeneralOptions() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        int row = 0;

        grid.add(label("HitPosition:"), 0, row);
        grid.add(intField(config.getHitPosition(), 0, 480, config::setHitPosition), 1, row++);

        grid.add(label("ColumnStart:"), 0, row);
        grid.add(intField(config.getColumnStart(), 0, 640, config::setColumnStart), 1, row++);

        grid.add(label("ScorePosition:"), 0, row);
        grid.add(intField(config.getScorePosition(), 0, 480, config::setScorePosition), 1, row++);

        grid.add(label("ComboPosition:"), 0, row);
        grid.add(intField(config.getComboPosition(), 0, 480, config::setComboPosition), 1, row++);

        Separator sep = new Separator();
        grid.add(sep, 0, row++, 4, 1);

        // Checkboxes Izquierda
        CheckBox upsideDown = check("UpsideDown", config.isUpsideDown(), config::setUpsideDown);
        CheckBox judgement = check("JudgementLine", config.isJudgementLine(), config::setJudgementLine);
        CheckBox keysUnder = check("KeysUnderNotes", config.isKeysUnderNotes(), config::setKeysUnderNotes);
        CheckBox splitStages = check("SplitStages (10K+)", config.isSplitStages(), config::setSplitStages);
        splitStages.setDisable(config.getKeys() < 10);

        VBox leftChecks = new VBox(8, upsideDown, judgement, keysUnder, splitStages);
        grid.add(leftChecks, 0, row, 2, 1);

        // Controles Derecha
        CheckBox separateLn = check("Colores LN separados", config.isUseSeparateLnColor(), config::setUseSeparateLnColor);
        CheckBox separateTail = check("LN tail propia (NoteImageXT)", config.isUseSeparateLnTail(), config::setUseSeparateLnTail);

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
        rightChecks.setMinWidth(320); // Previene que los textos se corten

        grid.add(rightChecks, 2, row, 2, 1);

        TitledPane pane = new TitledPane("Opciones generales", grid);
        pane.setCollapsible(true);
        pane.setStyle("-fx-font-weight: bold;");
        return pane;
    }

    // -------------------------------------------------------------------------
    // UI: Panel Decoración del Stage
    // -------------------------------------------------------------------------

    private TitledPane buildDecorationPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        Label tip = new Label("💡 Tip: Si tienes un GIF animado para el fondo, usa la herramienta 'Conversor de GIF' en la pestaña General y escribe aquí el nombre base.");
        tip.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        tip.setWrapText(true);
        grid.add(tip, 0, 0, 2, 1);

        int row = 1;
        grid.add(label("StageLeftImage:"), 0, row);
        grid.add(stringField(config.getStageLeftImage(), config::setStageLeftImage), 1, row++);

        grid.add(label("StageRightImage:"), 0, row);
        grid.add(stringField(config.getStageRightImage(), config::setStageRightImage), 1, row++);

        grid.add(label("StageBottomImage:"), 0, row);
        grid.add(stringField(config.getStageBottomImage(), config::setStageBottomImage), 1, row++);

        grid.add(label("StageHintImage:"), 0, row);
        grid.add(stringField(config.getStageHintImage(), config::setStageHintImage), 1, row++);

        TitledPane pane = new TitledPane("Decoración del Stage", grid);
        pane.setCollapsible(true);
        pane.setExpanded(false);
        pane.setStyle("-fx-font-weight: bold;");
        return pane;
    }

    // -------------------------------------------------------------------------
    // UI: Paletas Rápidas
    // -------------------------------------------------------------------------

    private VBox buildPalettePanel() {
        Label title = new Label("Paletas Rápidas (Aplica a todas las columnas)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        for (Palette p : PALETTES) {
            Button btn = new Button(p.name());
            btn.setStyle("-fx-font-size: 12px; -fx-cursor: hand;");
            btn.setOnAction(e -> applyPalette(p));
            buttonsBox.getChildren().add(btn);
        }

        VBox root = new VBox(10, title, buttonsBox);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8;");
        return root;
    }

    private void applyPalette(Palette p) {
        int keys = config.getKeys();
        for (int i = 0; i < keys; i++) {
            ManiaKeyConfig.ColumnConfig col = config.getColumn(i);
            boolean isSpecial = config.isSpecialColumn(i, keys);
            boolean isEven = (i % 2 != 0); // i=0 (col 1) es impar. i=1 (col 2) es par.

            java.awt.Color targetRice = isSpecial ? p.specialRice() : (isEven ? p.evenRice() : p.oddRice());
            java.awt.Color targetLn = isSpecial ? p.specialLn() : (isEven ? p.evenLn() : p.oddLn());

            col.riceColor = targetRice;
            col.lnColor = targetLn;

            // Actualizar los ColorPickers de la interfaz para que reflejen el nuevo color
            if (i < ricePickers.size()) ricePickers.get(i).setValue(toFx(targetRice));
            if (i < lnPickers.size()) lnPickers.get(i).setValue(toFx(targetLn));
        }
        requestRedraw();
    }

    // -------------------------------------------------------------------------
    // UI: Columnas
    // -------------------------------------------------------------------------

    private TitledPane buildColumnsPanel() {
        int keys = config.getKeys();

        // Limpiamos listas para evitar duplicados en recargas
        ricePickers.clear();
        lnPickers.clear();

        HBox columnsRow = new HBox(10);
        columnsRow.setPadding(new Insets(14));
        columnsRow.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < keys; i++) {
            columnsRow.getChildren().add(buildColumnCard(i));
        }

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
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" + (isSpecial ? " -fx-text-fill: #c0a000;" : ""));

        // Width
        HBox widthRow = new HBox(6);
        widthRow.setAlignment(Pos.CENTER);
        Label widthLbl = new Label("Width:");
        widthLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField widthField = intField(col.columnWidth, 0, 500, val -> col.columnWidth = val);
        widthField.setPrefWidth(52);
        widthRow.getChildren().addAll(widthLbl, widthField);

        // Rice Color
        VBox riceBox = new VBox(4);
        riceBox.setAlignment(Pos.CENTER);
        Label riceLbl = new Label("Rice");
        riceLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        ColorPicker ricePicker = new ColorPicker(toFx(col.riceColor));
        ricePicker.setPrefWidth(115);
        ricePicker.setOnAction(e -> { col.riceColor = toAwt(ricePicker.getValue()); requestRedraw(); });
        ricePickers.add(ricePicker);
        riceBox.getChildren().addAll(riceLbl, ricePicker);

        // LN Color
        VBox lnBox = new VBox(4);
        lnBox.setAlignment(Pos.CENTER);
        Label lnLbl = new Label("LN");
        lnLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        ColorPicker lnPicker = new ColorPicker(toFx(col.lnColor));
        lnPicker.setPrefWidth(115);
        lnPicker.setOnAction(e -> { col.lnColor = toAwt(lnPicker.getValue()); requestRedraw(); });
        lnPickers.add(lnPicker);
        lnBox.getChildren().addAll(lnLbl, lnPicker);

        // Light Color
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

    // -------------------------------------------------------------------------
    // Helpers Conversión / UI
    // -------------------------------------------------------------------------

    private static javafx.scene.paint.Color toFx(java.awt.Color c) {
        if (c == null) return javafx.scene.paint.Color.WHITE;
        return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
    }

    private static java.awt.Color toAwt(javafx.scene.paint.Color c) {
        return new java.awt.Color((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(), (float) c.getOpacity());
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px;");
        return l;
    }

    private TextField stringField(String initial, java.util.function.Consumer<String> setter) {
        TextField tf = new TextField(initial == null ? "" : initial);
        tf.setPrefWidth(180);
        tf.textProperty().addListener((obs, old, val) -> {
            setter.accept(val.trim().isEmpty() ? null : val.trim());
            requestRedraw();
        });
        return tf;
    }

    private TextField intField(int initial, int min, int max, java.util.function.IntConsumer setter) {
        TextField tf = new TextField(String.valueOf(initial));
        tf.setPrefWidth(80);
        tf.setStyle("-fx-font-size: 13px;");

        Runnable apply = () -> {
            try {
                int val = Integer.parseInt(tf.getText().trim());
                val = Math.max(min, Math.min(max, val));
                setter.accept(val);
                tf.setText(String.valueOf(val));
                requestRedraw();
            } catch (NumberFormatException ignored) {
                tf.setText(String.valueOf(initial));
            }
        };
        tf.setOnAction(e -> apply.run());
        tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> { if (!isFocused) apply.run(); });
        return tf;
    }

    private CheckBox check(String text, boolean selected, java.util.function.Consumer<Boolean> onChange) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setStyle("-fx-font-size: 12px;");
        cb.selectedProperty().addListener((obs, old, val) -> {
            onChange.accept(val);
            requestRedraw();
        });
        return cb;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}