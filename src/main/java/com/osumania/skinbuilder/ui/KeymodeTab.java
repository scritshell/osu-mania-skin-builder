package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Pestaña de edición para un keymode concreto (4K, 7K, etc.).
 *
 * <p><b>Recibe un {@link ManiaKeyConfig} real</b> parseado del skin.ini importado.
 * No inventa valores: toda la información viene del .osk del usuario.</p>
 *
 * <h2>Estructura</h2>
 * <ol>
 * <li>Panel de opciones generales del keymode (HitPosition, ColumnStart…)</li>
 * <li>Panel de configuración de columnas (ancho, colores rice/LN, imagen de nota)</li>
 * </ol>
 *
 * <p>Cada cambio en la UI actualiza directamente el {@link ManiaKeyConfig} subyacente,
 * de modo que al exportar con {@link com.osumania.skinbuilder.core.SkinIniWriter}
 * el resultado refleja las ediciones del usuario.</p>
 */
public class KeymodeTab extends ScrollPane {

    private final ManiaKeyConfig config;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param config  Configuración real del keymode leída del skin.ini importado.
     * Se modifica in-place al editar los controles.
     */
    public KeymodeTab(ManiaKeyConfig config) {
        this.config = config;
        setFitToWidth(true);
        setStyle("-fx-background-color: #f0f0f0;");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 30, 20, 30));
        root.getChildren().addAll(
                buildHeader(),
                buildGeneralOptions(),
                buildColumnsPanel()
        );

        setContent(root);
    }

    // -------------------------------------------------------------------------
    // Header
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
    // Panel de opciones generales
    // -------------------------------------------------------------------------

    private TitledPane buildGeneralOptions() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        int row = 0;

        // HitPosition
        grid.add(label("HitPosition:"), 0, row);
        TextField hitPos = intField(config.getHitPosition(), 0, 480,
                val -> config.setHitPosition(val));
        grid.add(hitPos, 1, row++);

        // ColumnStart
        grid.add(label("ColumnStart:"), 0, row);
        TextField colStart = intField(config.getColumnStart(), 0, 640,
                val -> config.setColumnStart(val));
        grid.add(colStart, 1, row++);

        // ScorePosition
        grid.add(label("ScorePosition:"), 0, row);
        TextField scorePos = intField(config.getScorePosition(), 0, 480,
                val -> config.setScorePosition(val));
        grid.add(scorePos, 1, row++);

        // ComboPosition
        grid.add(label("ComboPosition:"), 0, row);
        TextField comboPos = intField(config.getComboPosition(), 0, 480,
                val -> config.setComboPosition(val));
        grid.add(comboPos, 1, row++);

        // Separador visual
        Separator sep = new Separator();
        grid.add(sep, 0, row++, 4, 1);

        // Checkboxes — columna izquierda
        CheckBox upsideDown = check("UpsideDown", config.isUpsideDown(),
                config::setUpsideDown);
        CheckBox judgement = check("JudgementLine", config.isJudgementLine(),
                config::setJudgementLine);
        CheckBox keysUnder = check("KeysUnderNotes", config.isKeysUnderNotes(),
                config::setKeysUnderNotes);
        CheckBox splitStages = check("SplitStages (10K+)", config.isSplitStages(),
                config::setSplitStages);
        splitStages.setDisable(config.getKeys() < 10);

        VBox leftChecks = new VBox(8, upsideDown, judgement, keysUnder, splitStages);
        grid.add(leftChecks, 0, row, 2, 1);

        // Checkboxes — columna derecha

        // Percy Size
        Label percySizeLbl = new Label("Percy Size (px):");
        percySizeLbl.setStyle("-fx-font-size: 12px;");
        Spinner<Integer> percySpinner = new Spinner<>(0, 400, config.getPercySize(), 10);
        percySpinner.setEditable(true);
        percySpinner.setPrefWidth(90);
        percySpinner.valueProperty().addListener((obs, old, val) ->
                config.setPercySize(val));
        HBox percySizeRow = new HBox(8, percySizeLbl, percySpinner);
        percySizeRow.setAlignment(Pos.CENTER_LEFT);

        // Percy Shape
        Label percyShapeLbl = new Label("Forma de punta:");
        percyShapeLbl.setStyle("-fx-font-size: 12px;");
        ComboBox<ManiaKeyConfig.PercyShape> percyShapeBox =
                new ComboBox<>();
        percyShapeBox.getItems().setAll(ManiaKeyConfig.PercyShape.values());
        percyShapeBox.setValue(config.getPercyShape());
        percyShapeBox.valueProperty().addListener((obs, old, val) ->
                config.setPercyShape(val));
        HBox percyShapeRow = new HBox(8, percyShapeLbl, percyShapeBox);
        percyShapeRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox separateLn = check("Colores LN separados por columna",
                config.isUseSeparateLnColor(),
                config::setUseSeparateLnColor);

        CheckBox separateTail = check("LN tail con imagen propia (NoteImageXT)",
                config.isUseSeparateLnTail(),
                config::setUseSeparateLnTail);

        // Transparencia global
        CheckBox transpCheck = check("Transparencia global",
                config.isUseGlobalTransparency(), config::setUseGlobalTransparency);

        Slider alphaSlider = new Slider(0, 255, config.getGlobalAlpha());
        alphaSlider.setShowTickLabels(true);
        alphaSlider.setMajorTickUnit(64);
        alphaSlider.setPrefWidth(160);
        alphaSlider.setDisable(!config.isUseGlobalTransparency());

        Label alphaValue = new Label(String.valueOf(config.getGlobalAlpha()));
        alphaValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        transpCheck.selectedProperty().addListener((obs, old, val) -> {
            config.setUseGlobalTransparency(val);
            alphaSlider.setDisable(!val);
        });
        alphaSlider.valueProperty().addListener((obs, old, val) -> {
            config.setGlobalAlpha(val.intValue());
            alphaValue.setText(String.valueOf(val.intValue()));
        });

        HBox alphaRow = new HBox(8, alphaSlider, alphaValue);
        alphaRow.setAlignment(Pos.CENTER_LEFT);

        VBox rightChecks = new VBox(8,
                separateLn, separateTail,
                percySizeRow, percyShapeRow,
                transpCheck, alphaRow);

        grid.add(rightChecks, 2, row, 2, 1);

        TitledPane pane = new TitledPane("Opciones generales", grid);
        pane.setCollapsible(true);
        pane.setExpanded(true);
        pane.setStyle("-fx-font-weight: bold;");
        return pane;
    }

    // -------------------------------------------------------------------------
    // Panel de columnas
    // -------------------------------------------------------------------------

    private TitledPane buildColumnsPanel() {
        int keys = config.getKeys();

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

        // Título
        String titleText = "Col " + (colIndex + 1) + (isSpecial ? "  ★" : "");
        Label title = new Label(titleText);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" +
                (isSpecial ? " -fx-text-fill: #c0a000;" : ""));

        // ---- Ancho de columna ----
        HBox widthRow = new HBox(6);
        widthRow.setAlignment(Pos.CENTER);
        Label widthLbl = new Label("Width:");
        widthLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField widthField = new TextField(String.valueOf(col.columnWidth));
        widthField.setPrefWidth(52);
        widthField.setStyle("-fx-font-size: 12px;");
        widthField.textProperty().addListener((obs, old, val) -> {
            try { col.columnWidth = Math.max(0, Integer.parseInt(val.trim())); }
            catch (NumberFormatException ignored) {}
        });
        widthRow.getChildren().addAll(widthLbl, widthField);

        // ---- Color Rice ----
        VBox riceBox = new VBox(4);
        riceBox.setAlignment(Pos.CENTER);
        Label riceLbl = new Label("Rice");
        riceLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        javafx.scene.control.ColorPicker ricePicker =
                new javafx.scene.control.ColorPicker(toFx(col.riceColor));
        ricePicker.setPrefWidth(115);
        ricePicker.setOnAction(e -> col.riceColor = toAwt(ricePicker.getValue()));
        riceBox.getChildren().addAll(riceLbl, ricePicker);

        // ---- Color LN ----
        VBox lnBox = new VBox(4);
        lnBox.setAlignment(Pos.CENTER);
        Label lnLbl = new Label("LN");
        lnLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        javafx.scene.control.ColorPicker lnPicker =
                new javafx.scene.control.ColorPicker(toFx(col.lnColor));
        lnPicker.setPrefWidth(115);
        lnPicker.setOnAction(e -> col.lnColor = toAwt(lnPicker.getValue()));
        lnBox.getChildren().addAll(lnLbl, lnPicker);

        // ---- Nombre de imagen de nota (solo lectura, informativo) ----
        Label noteImgLbl = new Label(truncate(col.noteImageRice, 15));
        noteImgLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        noteImgLbl.setTooltip(new Tooltip(
                "Rice: "  + nz(col.noteImageRice)   + "\n" +
                        "Head: "  + nz(col.noteImageLnHead)  + "\n" +
                        "Body: "  + nz(col.noteImageLnBody)  + "\n" +
                        "Tail: "  + nz(col.noteImageLnTail)
        ));

        // ---- Light color ----
        VBox lightBox = new VBox(4);
        lightBox.setAlignment(Pos.CENTER);
        Label lightLbl = new Label("ColourLight");
        lightLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        javafx.scene.control.ColorPicker lightPicker =
                new javafx.scene.control.ColorPicker(toFx(col.lightColor));
        lightPicker.setPrefWidth(115);
        lightPicker.setOnAction(e -> col.lightColor = toAwt(lightPicker.getValue()));
        lightBox.getChildren().addAll(lightLbl, lightPicker);

        card.getChildren().addAll(title, widthRow, riceBox, lnBox, lightBox, noteImgLbl);
        return card;
    }

    // -------------------------------------------------------------------------
    // Helpers de color
    // -------------------------------------------------------------------------

    private static javafx.scene.paint.Color toFx(java.awt.Color c) {
        if (c == null) return javafx.scene.paint.Color.WHITE;
        return javafx.scene.paint.Color.rgb(
                c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
    }

    private static java.awt.Color toAwt(javafx.scene.paint.Color c) {
        return new java.awt.Color(
                (float) c.getRed(),
                (float) c.getGreen(),
                (float) c.getBlue(),
                (float) c.getOpacity()
        );
    }

    // -------------------------------------------------------------------------
    // Helpers de UI
    // -------------------------------------------------------------------------

    private static Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px;");
        return l;
    }

    /**
     * Campo de texto numérico que actualiza el config al perder el foco o al pulsar Enter.
     */
    private static TextField intField(int initial, int min, int max,
                                      java.util.function.IntConsumer setter) {
        TextField tf = new TextField(String.valueOf(initial));
        tf.setPrefWidth(80);
        tf.setStyle("-fx-font-size: 13px;");

        Runnable apply = () -> {
            try {
                int val = Integer.parseInt(tf.getText().trim());
                val = Math.max(min, Math.min(max, val));
                setter.accept(val);
                tf.setText(String.valueOf(val));
            } catch (NumberFormatException ignored) {
                tf.setText(String.valueOf(initial));
            }
        };
        tf.setOnAction(e -> apply.run());
        tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) apply.run();
        });
        return tf;
    }

    private static CheckBox check(String text, boolean selected,
                                  java.util.function.Consumer<Boolean> onChange) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setStyle("-fx-font-size: 12px;");
        cb.selectedProperty().addListener((obs, old, val) -> onChange.accept(val));
        return cb;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static String nz(String s) {
        return s == null ? "(heredado)" : s;
    }
}