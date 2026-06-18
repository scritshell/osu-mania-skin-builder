package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.image.GifFrameExtractor;
import com.osumania.skinbuilder.image.PreviewAssetManager;
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
 * Pestaña de edición de un keymode — rediseño limpio dark theme.
 *
 * <h2>Layout</h2>
 * <pre>
 *  ┌─────────────────────────────────────┐
 *  │ StackPane (fondo constelaciones)    │
 *  │  └─ SplitPane                       │
 *  │      ├─ ScrollPane (controles)      │
 *  │      └─ PreviewPane (preview)       │
 *  └─────────────────────────────────────┘
 * </pre>
 *
 * Las constelaciones son la capa más baja del StackPane externo.
 * El SplitPane tiene fondo semitransparente, lo que permite ver las
 * constelaciones en los bordes y zonas vacías sin tapar ningún control.
 */
public class KeymodeTab extends SplitPane {

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final String PANEL_BG    = "rgba(7,12,26,0.92)";
    private static final String CARD_BG     = "rgba(9,15,30,0.96)";
    private static final String BORDER      = "#182848";
    private static final String TEXT_PRI    = "#C8DCFF";
    private static final String TEXT_SEC    = "#3A5880";
    private static final String TEXT_MONO   = "#7AB0E8";   // valores numéricos
    private static final String ACCENT      = "#4A8FFF";
    private static final String SPECIAL_CLR = "#C8A020";

    // ── Modelo ────────────────────────────────────────────────────────────────
    private final ManiaKeyConfig    config;
    private final PreviewAssetManager assetManager;
    private final MainWindow        mainWindow;
    private final PreviewPane       previewPane;

    // Color pickers (para actualización desde paleta)
    private final List<ColorPicker> ricePickers = new ArrayList<>();
    private final List<ColorPicker> lnPickers   = new ArrayList<>();
    private boolean paletteUpdating = false;

    // =========================================================================
    // Paletas de gamemode
    // =========================================================================

    private record Palette(
            String name, IntPredicate isSpecial,
            java.awt.Color riceNormal, java.awt.Color riceSpecial,
            java.awt.Color lnNormal,   java.awt.Color lnSpecial) {}

    private static final Map<Integer, List<Palette>> PALETTES = Map.of(
            6,  List.of(
                    new Palette("6K normal",   i -> false,
                            awt(255,255,255), awt(220,50,50), awt(255,255,255), awt(220,50,50)),
                    new Palette("5K+1S BMS",   i -> i==5,
                            awt(255,255,255), awt(220,50,50), awt(255,255,255), awt(220,50,50))),
            8,  List.of(
                    new Palette("4K4K",        i -> false,
                            awt(255,255,255), awt(60,120,255), awt(255,255,255), awt(60,120,255)),
                    new Palette("7K+1S BMS",   i -> i==7,
                            awt(255,255,255), awt(220,50,50), awt(255,255,255), awt(220,50,50))),
            12, List.of(
                    new Palette("10K+2S BMS",  i -> i==5||i==6,
                            awt(255,255,255), awt(220,50,50), awt(255,255,255), awt(220,50,50)),
                    new Palette("6K6K",        i -> false,
                            awt(255,255,255), awt(60,120,255), awt(255,255,255), awt(60,120,255))),
            14, List.of(
                    new Palette("7K7K",        i -> false,
                            awt(255,255,255), awt(60,120,255), awt(255,255,255), awt(60,120,255)),
                    new Palette("EZ2AC 5K4K5K",i -> i==4||i==9,
                            awt(255,255,255), awt(220,50,50), awt(255,255,255), awt(220,50,50))),
            16, List.of(
                    new Palette("7K+1S DP BMS",i -> i==7||i==15,
                            awt(255,255,255), awt(220,50,50), awt(255,255,255), awt(220,50,50))),
            18, List.of(
                    new Palette("10K8K",       i -> false,
                            awt(255,255,255), awt(60,120,255), awt(255,255,255), awt(60,120,255)),
                    new Palette("9K9K",        i -> false,
                            awt(255,255,255), awt(255,200,0), awt(255,255,255), awt(255,200,0)))
    );

    // =========================================================================
    // Constructor
    // =========================================================================

    public KeymodeTab(ManiaKeyConfig config, PreviewAssetManager assetManager, MainWindow mainWindow) {
        this.config       = config;
        this.assetManager = assetManager;
        this.mainWindow   = mainWindow;

        // ── Panel de controles (izquierda) ────────────────────────────────────
        VBox controls = new VBox(14);
        controls.setPadding(new Insets(20, 24, 20, 24));
        controls.getChildren().addAll(
                buildHeader(),
                buildGeneralOptions(),
                buildDecorationPanel(),
                buildPalettePanel(),
                buildColumnsPanel()
        );

        ScrollPane scrollControls = new ScrollPane(controls);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;");

        // Fondo con constelaciones para el lado de controles
        // isResizable()=true → el StackPane llama a resize() automáticamente.
        // NO usar bind(): colisiona con resize() y lanza BoundValue exception.
        ConstellationCanvas starsLeft = new ConstellationCanvas(28, 120, 0.20, 0.13);
        starsLeft.setMouseTransparent(true);

        StackPane leftPane = new StackPane(starsLeft, scrollControls);
        leftPane.setStyle("-fx-background-color: #060912;");

        // ── Panel de preview (derecha) ────────────────────────────────────────
        this.previewPane = new PreviewPane();
        this.previewPane.setAssetManager(assetManager);

        StackPane rightPane = new StackPane(previewPane);
        rightPane.setStyle("-fx-background-color: #080B14;");

        // ── Botón "Ver en preview completo" ──────────────────────────────────
        Button previewBtn = new Button("↗  Preview completo");
        previewBtn.setStyle(
                "-fx-background-color: rgba(10,20,50,0.80);" +
                        "-fx-text-fill: #4A8FFF;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 5 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #1A3060;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;");
        previewBtn.setOnAction(e -> mainWindow.updatePreview(config));
        StackPane.setAlignment(previewBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(previewBtn, new Insets(10));
        rightPane.getChildren().add(previewBtn);

        // ── SplitPane ─────────────────────────────────────────────────────────
        getItems().addAll(leftPane, rightPane);
        setDividerPositions(0.58);
        setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        requestRedraw();
    }

    private void requestRedraw() {
        if (previewPane != null) previewPane.drawPreview(config);
    }

    // =========================================================================
    // Header
    // =========================================================================

    private HBox buildHeader() {
        Label title = new Label(config.getDisplayName());
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        Label stats = new Label(
                config.getColumns().size() + " columnas  ·  " +
                        "HitPos " + monoVal(config.getHitPosition()) + "  ·  " +
                        "ColStart " + monoVal(config.getColumnStart()));
        stats.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 11px;");

        VBox box = new VBox(3, title, stats);
        HBox header = new HBox(box);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));
        return header;
    }

    // =========================================================================
    // Opciones generales
    // =========================================================================

    private TitledPane buildGeneralOptions() {
        // ── Columna izquierda: campos numéricos ───────────────────────────────
        GridPane numGrid = new GridPane();
        numGrid.setHgap(10);
        numGrid.setVgap(9);

        int r = 0;
        numGrid.add(fLabel("HitPosition"),   0, r); numGrid.add(numField(config.getHitPosition(),   0, 480, config::setHitPosition),  1, r++);
        numGrid.add(fLabel("ColumnStart"),   0, r); numGrid.add(numField(config.getColumnStart(),   0, 640, config::setColumnStart),   1, r++);
        numGrid.add(fLabel("ScorePosition"), 0, r); numGrid.add(numField(config.getScorePosition(), 0, 480, config::setScorePosition), 1, r++);
        numGrid.add(fLabel("ComboPosition"), 0, r); numGrid.add(numField(config.getComboPosition(), 0, 480, config::setComboPosition), 1, r++);

        // ── Columna derecha: checkboxes + controles ───────────────────────────
        CheckBox cbUpsideDown  = chk("UpsideDown",           config.isUpsideDown(),       config::setUpsideDown);
        CheckBox cbJudgement   = chk("JudgementLine",        config.isJudgementLine(),    config::setJudgementLine);
        CheckBox cbKeysUnder   = chk("KeysUnderNotes",       config.isKeysUnderNotes(),   config::setKeysUnderNotes);
        CheckBox cbSplitStages = chk("SplitStages (10K+)",   config.isSplitStages(),      config::setSplitStages);
        cbSplitStages.setDisable(config.getKeys() < 10);
        CheckBox cbSepLn   = chk("Color LN separado",        config.isUseSeparateLnColor(), config::setUseSeparateLnColor);
        CheckBox cbSepTail = chk("LN tail propia (XT)",      config.isUseSeparateLnTail(),  config::setUseSeparateLnTail);

        // Percy
        Label percyLbl = fLabel("Percy Size (px)");
        Spinner<Integer> percySpin = spinner(0, 400, config.getPercySize(), 8,
                val -> { config.setPercySize(val); requestRedraw(); });

        Label shapeLbl = fLabel("Forma punta");
        ComboBox<ManiaKeyConfig.PercyShape> shapeBox = new ComboBox<>();
        shapeBox.getItems().setAll(ManiaKeyConfig.PercyShape.values());
        shapeBox.setValue(config.getPercyShape());
        shapeBox.setStyle(comboStyle());
        shapeBox.setPrefWidth(110);
        shapeBox.valueProperty().addListener((o, old, v) -> { config.setPercyShape(v); requestRedraw(); });

        // Receptor offset
        Label offsetLbl = fLabel("Receptor Offset (Y)");
        Spinner<Integer> offsetSpin = spinner(-200, 200, config.getReceptorOffset(), 1,
                val -> { config.setReceptorOffset(val); requestRedraw(); });

        // Global alpha
        CheckBox cbTransp = chk("Transparencia global", config.isUseGlobalTransparency(), config::setUseGlobalTransparency);
        Label alphaLbl  = fLabel("Alpha");
        Slider alphaSld = new Slider(0, 255, config.getGlobalAlpha());
        alphaSld.setPrefWidth(140);
        alphaSld.setDisable(!config.isUseGlobalTransparency());
        alphaSld.setStyle("-fx-accent: " + ACCENT + ";");
        Label alphaVal  = new Label(String.valueOf(config.getGlobalAlpha()));
        alphaVal.setStyle("-fx-text-fill: " + TEXT_MONO + "; -fx-font-family: monospace; -fx-font-size: 12px;");
        cbTransp.selectedProperty().addListener((o, old, v) -> {
            alphaSld.setDisable(!v);
            config.setUseGlobalTransparency(v);
            requestRedraw();
        });
        alphaSld.valueProperty().addListener((o, old, v) -> {
            int val = v.intValue();
            config.setGlobalAlpha(val);
            alphaVal.setText(String.valueOf(val));
            requestRedraw();
        });

        // Ensamblar columna derecha
        GridPane rightGrid = new GridPane();
        rightGrid.setHgap(10);
        rightGrid.setVgap(8);
        int rr = 0;
        rightGrid.add(cbUpsideDown,  0, rr, 2, 1); rr++;
        rightGrid.add(cbJudgement,   0, rr, 2, 1); rr++;
        rightGrid.add(cbKeysUnder,   0, rr, 2, 1); rr++;
        rightGrid.add(cbSplitStages, 0, rr, 2, 1); rr++;
        rightGrid.add(cbSepLn,       0, rr, 2, 1); rr++;
        rightGrid.add(cbSepTail,     0, rr, 2, 1); rr++;
        // separador visual
        rightGrid.add(hSep(), 0, rr, 2, 1); rr++;
        rightGrid.add(percyLbl, 0, rr); rightGrid.add(percySpin,  1, rr++);
        rightGrid.add(shapeLbl, 0, rr); rightGrid.add(shapeBox,   1, rr++);
        rightGrid.add(offsetLbl,0, rr); rightGrid.add(offsetSpin, 1, rr++);
        rightGrid.add(hSep(), 0, rr, 2, 1); rr++;
        rightGrid.add(cbTransp, 0, rr, 2, 1); rr++;
        rightGrid.add(alphaLbl, 0, rr);
        HBox alphaRow = new HBox(8, alphaSld, alphaVal);
        alphaRow.setAlignment(Pos.CENTER_LEFT);
        rightGrid.add(alphaRow, 1, rr);

        // Layout: dos columnas
        HBox columns = new HBox(32, numGrid, rightGrid);
        columns.setPadding(new Insets(16));
        columns.setStyle(cardStyle());

        TitledPane tp = titledPane("Opciones generales", columns);
        tp.setExpanded(true);
        return tp;
    }

    // =========================================================================
    // Decoración del stage
    // =========================================================================

    private TitledPane buildDecorationPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        box.setStyle(cardStyle());

        Label tip = new Label("💡  Carga imágenes locales (.png / .gif) para previsualizar antes de exportar.");
        tip.setWrapText(true);
        tip.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 11px;");
        box.getChildren().add(tip);

        box.getChildren().addAll(
                stageImageRow("StageLeft:",   config.getStageLeftImage(),   config::setStageLeftImage),
                stageImageRow("StageRight:",  config.getStageRightImage(),  config::setStageRightImage),
                stageImageRow("StageBottom:", config.getStageBottomImage(), config::setStageBottomImage),
                stageImageRow("StageHint:",   config.getStageHintImage(),   config::setStageHintImage)
        );

        TitledPane tp = titledPane("Decoración del Stage", box);
        tp.setExpanded(false);
        return tp;
    }

    private HBox stageImageRow(String label, String current, java.util.function.Consumer<String> setter) {
        Label lbl = new Label(label);
        lbl.setMinWidth(90);
        lbl.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 12px;");

        TextField tf = new TextField(current == null ? "" : current);
        tf.setPrefWidth(130);
        tf.setStyle(fieldStyle());
        tf.textProperty().addListener((o, old, v) -> {
            setter.accept(v.isBlank() ? null : v.trim());
            requestRedraw();
        });

        Button btn = new Button("Cargar");
        btn.setStyle(smallBtnStyle());
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Cargar imagen");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen", "*.png", "*.gif"));
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file == null) return;
            tf.setText(file.getName());
            setter.accept(file.getName());
            if (file.getName().toLowerCase().endsWith(".gif")) {
                loadGifAsync(file, file.getName());
            } else {
                try {
                    java.awt.image.BufferedImage img = ImageIO.read(file);
                    if (img != null) { assetManager.putStageImage(file.getName(), img); requestRedraw(); }
                } catch (Exception ex) { System.err.println("Error: " + ex.getMessage()); }
            }
        });

        HBox row = new HBox(8, lbl, tf, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void loadGifAsync(File file, String name) {
        Task<List<GifFrameExtractor.GifFrame>> task = new Task<>() {
            @Override protected List<GifFrameExtractor.GifFrame> call() throws Exception {
                return GifFrameExtractor.extract(file);
            }
        };
        task.setOnSucceeded(e -> {
            List<GifFrameExtractor.GifFrame> frames = task.getValue();
            if (frames != null && !frames.isEmpty()) {
                assetManager.putStageGif(name, frames);
                previewPane.enableGifAnimation(true);
                requestRedraw();
            }
        });
        task.setOnFailed(e -> System.err.println("GIF error: " + task.getException()));
        new Thread(task, "gif-loader").start();
    }

    // =========================================================================
    // Panel de paletas
    // =========================================================================

    private VBox buildPalettePanel() {
        Label title = new Label("Paletas rápidas — " + config.getKeys() + "K");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + ACCENT + ";");

        FlowPane btns = new FlowPane(8, 6);

        List<Palette> palettes = PALETTES.getOrDefault(config.getKeys(), List.of());

        // Fallback genérico
        if (palettes.isEmpty()) {
            palettes = List.of(
                    new Palette("IIDX genérico",
                            i -> config.isSpecialColumn(i, config.getKeys()),
                            awt(255,255,255), awt(220,50,50),
                            awt(255,255,255), awt(220,50,50)));
        }

        for (Palette p : palettes) {
            Button btn = new Button(p.name());
            btn.setStyle(smallBtnStyle());
            btn.setOnAction(e -> applyPalette(p));
            btns.getChildren().add(btn);
        }

        Button reset = new Button("Reset blanco");
        reset.setStyle(smallBtnStyle());
        reset.setOnAction(e -> applyPalette(new Palette("Reset",
                i -> false, awt(255,255,255), awt(255,255,255),
                awt(255,255,255), awt(255,255,255))));
        btns.getChildren().add(reset);

        VBox box = new VBox(10, title, btns);
        box.setPadding(new Insets(14));
        box.setStyle(cardStyle());
        return box;
    }

    private void applyPalette(Palette p) {
        paletteUpdating = true;
        try {
            for (int i = 0; i < config.getKeys(); i++) {
                ManiaKeyConfig.ColumnConfig col = config.getColumn(i);
                boolean sp = p.isSpecial().test(i);
                col.riceColor = sp ? p.riceSpecial() : p.riceNormal();
                col.lnColor   = sp ? p.lnSpecial()   : p.lnNormal();
                if (i < ricePickers.size()) ricePickers.get(i).setValue(fxColor(col.riceColor));
                if (i < lnPickers.size())   lnPickers.get(i).setValue(fxColor(col.lnColor));
            }
        } finally {
            paletteUpdating = false;
        }
        requestRedraw();
    }

    // =========================================================================
    // Panel de columnas
    // =========================================================================

    private TitledPane buildColumnsPanel() {
        ricePickers.clear();
        lnPickers.clear();

        HBox row = new HBox(8);
        row.setPadding(new Insets(12));
        for (int i = 0; i < config.getKeys(); i++) {
            row.getChildren().add(buildColumnCard(i));
        }

        ScrollPane scroll = new ScrollPane(row);
        scroll.setFitToHeight(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;");
        scroll.setPrefHeight(300);

        TitledPane tp = titledPane("Columnas (" + config.getKeys() + "K)", scroll);
        tp.setExpanded(true);
        return tp;
    }

    private VBox buildColumnCard(int idx) {
        ManiaKeyConfig.ColumnConfig col = config.getColumn(idx);
        boolean isSpecial = config.isSpecialColumn(idx, config.getKeys());

        String borderColor = isSpecial ? SPECIAL_CLR : BORDER;
        String bgColor     = isSpecial ? "rgba(22,18,6,0.96)" : "rgba(9,15,30,0.96)";

        VBox card = new VBox(9);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMinWidth(122);
        card.setPrefWidth(122);
        card.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 12 9;");

        // Título de columna
        String titleText = "Col " + (idx + 1) + (isSpecial ? "  ★" : "");
        Label title = new Label(titleText);
        title.setFont(Font.font("System", FontWeight.BOLD, 12));
        title.setStyle("-fx-text-fill: " + (isSpecial ? SPECIAL_CLR : TEXT_PRI) + ";");

        // Width
        TextField widthTf = numField(col.columnWidth, 0, 500, val -> {
            col.columnWidth = val;
            requestRedraw();
        });
        widthTf.setPrefWidth(70);
        HBox widthRow = new HBox(6, fLabel("W:"), widthTf);
        widthRow.setAlignment(Pos.CENTER_LEFT);

        // Rice picker
        ColorPicker ricePk = colorPicker(col.riceColor, c -> {
            if (!paletteUpdating) { col.riceColor = awtColor(c); requestRedraw(); }
        });
        ricePickers.add(ricePk);

        // LN picker
        ColorPicker lnPk = colorPicker(col.lnColor, c -> {
            if (!paletteUpdating) { col.lnColor = awtColor(c); requestRedraw(); }
        });
        lnPickers.add(lnPk);

        // ColourLight picker
        ColorPicker lightPk = colorPicker(col.lightColor, c -> {
            col.lightColor = awtColor(c);
            requestRedraw();
        });

        card.getChildren().addAll(
                title, widthRow,
                colPickerRow("Rice",  ricePk),
                colPickerRow("LN",    lnPk),
                colPickerRow("Light", lightPk)
        );
        return card;
    }

    private VBox colPickerRow(String label, ColorPicker picker) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 10px;");
        picker.setPrefWidth(104);
        VBox box = new VBox(2, lbl, picker);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // =========================================================================
    // Helpers de UI
    // =========================================================================

    private Label fLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(110);
        l.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 12px;");
        return l;
    }

    private TextField numField(int initial, int min, int max,
                               java.util.function.IntConsumer setter) {
        TextField tf = new TextField(String.valueOf(initial));
        tf.setPrefWidth(75);
        tf.setStyle(fieldStyle());
        Runnable apply = () -> {
            try {
                int v = Math.max(min, Math.min(max, Integer.parseInt(tf.getText().trim())));
                setter.accept(v);
                tf.setText(String.valueOf(v));
                requestRedraw();
            } catch (NumberFormatException e) {
                tf.setText(String.valueOf(initial));
            }
        };
        tf.setOnAction(e  -> apply.run());
        tf.focusedProperty().addListener((o, was, is) -> { if (!is) apply.run(); });
        return tf;
    }

    private CheckBox chk(String text, boolean selected,
                         java.util.function.Consumer<Boolean> onChange) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_PRI + ";");
        cb.selectedProperty().addListener((o, old, v) -> { onChange.accept(v); requestRedraw(); });
        return cb;
    }

    private Spinner<Integer> spinner(int min, int max, int init, int step,
                                     java.util.function.IntConsumer onChange) {
        Spinner<Integer> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true);
        sp.setPrefWidth(90);
        sp.setStyle("-fx-font-size: 12px;");
        sp.valueProperty().addListener((o, old, v) -> onChange.accept(v));
        return sp;
    }

    private ColorPicker colorPicker(java.awt.Color init,
                                    java.util.function.Consumer<javafx.scene.paint.Color> onChange) {
        ColorPicker cp = new ColorPicker(fxColor(init));
        cp.setStyle("-fx-background-color: #060C1C; -fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 5; -fx-background-radius: 5;");
        cp.setOnAction(e -> onChange.accept(cp.getValue()));
        return cp;
    }

    private TitledPane titledPane(String title, javafx.scene.Node content) {
        TitledPane tp = new TitledPane(title, content);
        tp.setCollapsible(true);
        tp.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_PRI + ";" +
                        "-fx-font-size: 13px;");
        return tp;
    }

    private Region hSep() {
        Region r = new Region();
        r.setMinHeight(1);
        r.setMaxHeight(1);
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color: " + BORDER + ";");
        GridPane.setColumnSpan(r, 2);
        VBox.setMargin(r, new Insets(2, 0, 2, 0));
        return r;
    }

    // ── Estilos de cadena ──────────────────────────────────────────────────

    private String cardStyle() {
        return  "-fx-background-color: " + CARD_BG + ";" +
                "-fx-background-radius: 9;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 9;" +
                "-fx-border-width: 1;";
    }

    private static String fieldStyle() {
        return  "-fx-background-color: #060C1C;" +
                "-fx-text-fill: #7AB0E8;" +
                "-fx-font-family: monospace;" +
                "-fx-border-color: #182848;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-font-size: 12px;";
    }

    private static String comboStyle() {
        return  "-fx-background-color: #060C1C;" +
                "-fx-border-color: #182848;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;";
    }

    private static String smallBtnStyle() {
        return  "-fx-background-color: #0C1830;" +
                "-fx-text-fill: #7AAAD8;" +
                "-fx-font-size: 11px;" +
                "-fx-padding: 5 12;" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: #182848;" +
                "-fx-border-radius: 6;" +
                "-fx-cursor: hand;";
    }

    // ── Colores ───────────────────────────────────────────────────────────────

    private static java.awt.Color awt(int r, int g, int b) {
        return new java.awt.Color(r, g, b);
    }

    private static javafx.scene.paint.Color fxColor(java.awt.Color c) {
        if (c == null) return javafx.scene.paint.Color.WHITE;
        return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
    }

    private static java.awt.Color awtColor(javafx.scene.paint.Color c) {
        return new java.awt.Color(
                (float) c.getRed(), (float) c.getGreen(),
                (float) c.getBlue(), (float) c.getOpacity());
    }

    private static String monoVal(int v) {
        return String.valueOf(v);
    }
}