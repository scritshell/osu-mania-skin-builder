package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.image.GifFrameExtractor;
import com.osumania.skinbuilder.image.GifFrameExtractor.GifFrame;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Ventana flotante: Conversor de GIF → PNG numerados para osu!mania.
 *
 * <h2>Problema que resuelve</h2>
 * osu!mania no lee archivos .gif directamente. Para animar un elemento
 * (p.e. {@code mania-stage-hint}) necesita imágenes separadas numeradas:
 * <pre>
 *   mania-stage-hint-0.png
 *   mania-stage-hint-1.png
 *   mania-stage-hint-2.png
 *   ...
 * </pre>
 *
 * <h2>Flujo de uso</h2>
 * <ol>
 *   <li>El usuario pulsa "Cargar GIF" y elige un archivo .gif.</li>
 *   <li>El conversor extrae todos los frames con {@link GifFrameExtractor}.</li>
 *   <li>Se muestra una tira de miniaturas y la información del GIF.</li>
 *   <li>El usuario configura el nombre base y la carpeta de destino.</li>
 *   <li>Al pulsar "Convertir", se exportan los PNG numerados en un hilo de fondo.</li>
 *   <li>El panel de info muestra el {@code LightFramePerSecond} recomendado para skin.ini.</li>
 * </ol>
 */
public class GifConverterDialog {

    // -------------------------------------------------------------------------
    // Estado interno
    // -------------------------------------------------------------------------

    private Stage dialog;
    private List<GifFrame> loadedFrames  = null;
    private File            loadedGifFile = null;
    private File            outputDir     = null;

    // Controles referenciados desde varios métodos
    private HBox            stripBox;
    private Label           gifInfoLabel;
    private Label           fpsHintLabel;
    private TextField       baseNameField;
    private Label           outputDirLabel;
    private ProgressBar     progressBar;
    private Label           progressLabel;
    private Button          convertBtn;

    // -------------------------------------------------------------------------
    // Punto de entrada
    // -------------------------------------------------------------------------

    /**
     * Crea y muestra el diálogo de conversión.
     *
     * @param owner Ventana padre (para modalidad)
     */
    public static void show(Stage owner) {
        new GifConverterDialog().openDialog(owner);
    }

    private void openDialog(Stage owner) {
        dialog = new Stage();
        dialog.setTitle("Conversor de GIF → PNG (osu!mania)");
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);
        dialog.setMinWidth(740);
        dialog.setMinHeight(520);

        BorderPane root = new BorderPane();
        root.setTop(buildTopBar());
        root.setCenter(buildCenter());
        root.setBottom(buildBottomBar());

        Scene scene = new Scene(root, 820, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    // =========================================================================
    // Layout
    // =========================================================================

    // ---- Barra superior: carga del GIF + nombre base + carpeta destino ------

    private HBox buildTopBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(14, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Botón "Cargar GIF"
        Button loadBtn = new Button("📂  Cargar GIF…");
        loadBtn.setStyle(
                "-fx-background-color: #e96d8a; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 7; -fx-cursor: hand;"
        );
        loadBtn.setOnAction(e -> onLoadGif());

        // Nombre base de los archivos exportados
        Label baseNameLbl = new Label("Nombre base:");
        baseNameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        baseNameField = new TextField("mania-stage-hint");
        baseNameField.setPrefWidth(180);
        baseNameField.setStyle("-fx-font-size: 12px;");
        Tooltip.install(baseNameField, new Tooltip(
                "Nombre sin extensión ni número.\n" +
                        "Resultado: mania-stage-hint-0.png, mania-stage-hint-1.png, …"
        ));

        // Carpeta de destino
        Label dirLbl = new Label("Carpeta:");
        dirLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        outputDirLabel = new Label("(sin seleccionar)");
        outputDirLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        outputDirLabel.setMaxWidth(180);
        outputDirLabel.setEllipsisString("…");

        Button dirBtn = new Button("Examinar…");
        dirBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12; -fx-background-radius: 5;");
        dirBtn.setOnAction(e -> onChooseOutputDir());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(
                loadBtn, spacer,
                baseNameLbl, baseNameField,
                dirLbl, outputDirLabel, dirBtn
        );
        return bar;
    }

    // ---- Centro: tira de frames + panel de información ----------------------

    private SplitPane buildCenter() {
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.72);

        // --- Izquierda: tira de miniaturas ---
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(14));

        Label stripTitle = new Label("Fotogramas del GIF");
        stripTitle.setFont(Font.font("System", FontWeight.BOLD, 13));

        stripBox = new HBox(6);
        stripBox.setAlignment(Pos.CENTER_LEFT);
        stripBox.setPadding(new Insets(8));
        stripBox.setMinHeight(100);
        stripBox.setStyle(
                "-fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-radius: 8;"
        );

        // Placeholder mientras no hay GIF
        Label placeholder = new Label("Carga un archivo GIF para ver los frames aquí");
        placeholder.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        stripBox.getChildren().add(placeholder);

        ScrollPane stripScroll = new ScrollPane(stripBox);
        stripScroll.setFitToHeight(true);
        stripScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        stripScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        stripScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        VBox.setVgrow(stripScroll, Priority.ALWAYS);

        leftPane.getChildren().addAll(stripTitle, stripScroll);

        // --- Derecha: panel de información ---
        VBox rightPane = buildInfoPanel();

        split.getItems().addAll(leftPane, rightPane);
        return split;
    }

    private VBox buildInfoPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: #fafafa;");

        Label infoTitle = new Label("Información");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 13));

        gifInfoLabel = new Label("—");
        gifInfoLabel.setWrapText(true);
        gifInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #444;");

        Separator sep = new Separator();

        Label fpsTitle = new Label("skin.ini — LightFramePerSecond");
        fpsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        fpsHintLabel = new Label("—");
        fpsHintLabel.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-text-fill: #4a9e6b; -fx-padding: 6 10; " +
                        "-fx-background-color: #eafaf1; -fx-background-radius: 6;"
        );

        Label fpsNote = new Label(
                "Usa este valor en el bloque [Mania] de tu skin.ini\n" +
                        "para que la animación reproduzca a la velocidad correcta."
        );
        fpsNote.setWrapText(true);
        fpsNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        Separator sep2 = new Separator();

        Label namingTitle = new Label("Archivos que se crearán:");
        namingTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label namingExample = new Label(
                "{nombre}-0.png\n{nombre}-1.png\n{nombre}-2.png\n…"
        );
        namingExample.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-family: monospace;");

        panel.getChildren().addAll(
                infoTitle, gifInfoLabel, sep,
                fpsTitle, fpsHintLabel, fpsNote,
                sep2, namingTitle, namingExample
        );
        return panel;
    }

    // ---- Barra inferior: progreso + botón Convertir -------------------------

    private HBox buildBottomBar() {
        HBox bar = new HBox(14);
        bar.setPadding(new Insets(12, 18, 14, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);
        progressBar.setVisible(false);

        progressLabel = new Label("");
        progressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        progressLabel.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        convertBtn = new Button("✅  Convertir y Exportar");
        convertBtn.setStyle(
                "-fx-background-color: #5cad8e; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 9 22; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        convertBtn.setDisable(true); // habilitado solo cuando hay frames cargados
        convertBtn.setOnAction(e -> onConvert());

        bar.getChildren().addAll(progressBar, progressLabel, spacer, convertBtn);
        return bar;
    }

    // =========================================================================
    // Lógica de eventos
    // =========================================================================

    /** Abre el selector de archivo GIF y carga los frames. */
    private void onLoadGif() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona un archivo GIF");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos GIF", "*.gif", "*.GIF")
        );
        File file = fc.showOpenDialog(dialog);
        if (file == null) return;

        loadedGifFile = file;
        convertBtn.setDisable(true);
        gifInfoLabel.setText("Cargando frames…");
        stripBox.getChildren().clear();

        // Extraer en hilo de fondo para no bloquear la UI
        Task<List<GifFrame>> task = new Task<>() {
            @Override protected List<GifFrame> call() throws Exception {
                return GifFrameExtractor.extract(file);
            }
        };

        task.setOnSucceeded(e -> {
            loadedFrames = task.getValue();
            Platform.runLater(() -> onFramesLoaded());
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable err = task.getException();
            gifInfoLabel.setText("Error al leer el GIF:\n" + err.getMessage());
            showError("No se pudo cargar el GIF", err.getMessage());
        }));

        Thread t = new Thread(task, "gif-loader");
        t.setDaemon(true);
        t.start();
    }

    /** Actualiza la UI tras cargar los frames con éxito. */
    private void onFramesLoaded() {
        List<GifFrame> frames = loadedFrames;
        int count      = frames.size();
        int fps        = GifFrameExtractor.suggestFps(frames);
        int totalMs    = GifFrameExtractor.totalDurationMs(frames);
        int w          = count > 0 ? frames.get(0).image.getWidth()  : 0;
        int h          = count > 0 ? frames.get(0).image.getHeight() : 0;

        // Info textual
        gifInfoLabel.setText(String.format(
                "Archivo: %s\nFrames: %d\nDimensiones: %d × %d px\nDuración total: %d ms",
                loadedGifFile.getName(), count, w, h, totalMs
        ));
        fpsHintLabel.setText("LightFramePerSecond: " + fps);

        // Tira de miniaturas (máx 64px de alto, escaladas proporcionalmente)
        stripBox.getChildren().clear();
        for (int i = 0; i < frames.size(); i++) {
            final int idx = i;
            GifFrame frame = frames.get(i);

            ImageView thumb;
            try {
                Image fxImg = toFxImage(frame.image, 64);
                thumb = new ImageView(fxImg);
            } catch (IOException ex) {
                thumb = new ImageView();
            }

            Label indexLbl = new Label(String.valueOf(i));
            indexLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #aaa;");
            indexLbl.setAlignment(Pos.CENTER);

            VBox cell = new VBox(3, thumb, indexLbl);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setStyle("-fx-cursor: hand;");
            Tooltip.install(cell, new Tooltip(
                    "Frame " + idx + "  |  " + frame.delayMs + " ms"
            ));

            stripBox.getChildren().add(cell);
        }

        convertBtn.setDisable(false);
    }

    /** Abre el selector de carpeta de destino. */
    private void onChooseOutputDir() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Selecciona carpeta de destino");
        if (outputDir != null) dc.setInitialDirectory(outputDir);
        File dir = dc.showDialog(dialog);
        if (dir == null) return;
        outputDir = dir;
        outputDirLabel.setText(dir.getAbsolutePath());
        outputDirLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
    }

    /** Exporta los frames como PNGs numerados en un hilo de fondo. */
    private void onConvert() {
        if (loadedFrames == null || loadedFrames.isEmpty()) {
            showError("Sin frames", "Carga un GIF primero.");
            return;
        }
        if (outputDir == null) {
            showError("Sin carpeta", "Elige una carpeta de destino.");
            return;
        }

        String baseName = baseNameField.getText().trim();
        if (baseName.isEmpty()) {
            showError("Nombre vacío", "Introduce un nombre base para los archivos.");
            return;
        }

        List<GifFrame> frames = loadedFrames;
        int total = frames.size();

        progressBar.setProgress(0);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressLabel.setText("0 / " + total);
        convertBtn.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                Path outPath = outputDir.toPath();
                Files.createDirectories(outPath);

                for (int i = 0; i < frames.size(); i++) {
                    GifFrame frame = frames.get(i);
                    // Nombre: baseName-0.png, baseName-1.png, …
                    String fileName = baseName + "-" + i + ".png";
                    Path dest = outPath.resolve(fileName);

                    ImageIO.write(frame.image, "PNG", dest.toFile());

                    final int done = i + 1;
                    updateProgress(done, total);
                    Platform.runLater(() -> {
                        progressBar.setProgress((double) done / total);
                        progressLabel.setText(done + " / " + total);
                    });
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            progressLabel.setText("✓ " + total + " frames exportados");
            progressBar.setProgress(1.0);
            convertBtn.setDisable(false);
            showSuccess(total, baseName);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressLabel.setText("Error al exportar");
            progressBar.setVisible(false);
            convertBtn.setDisable(false);
            showError("Error de exportación", task.getException().getMessage());
        }));

        Thread t = new Thread(task, "gif-exporter");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Helpers de imagen
    // =========================================================================

    /**
     * Convierte un {@link BufferedImage} a una {@link Image} de JavaFX
     * sin necesitar la dependencia {@code javafx-swing}.
     * La imagen se escala a {@code maxHeight} px de alto manteniendo proporción.
     */
    private static Image toFxImage(BufferedImage src, int maxHeight) throws IOException {
        // Escalar si es necesario
        BufferedImage img = src;
        if (src.getHeight() > maxHeight) {
            double ratio  = (double) maxHeight / src.getHeight();
            int newW      = Math.max(1, (int) (src.getWidth() * ratio));
            java.awt.image.BufferedImage scaled =
                    new java.awt.image.BufferedImage(newW, maxHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, newW, maxHeight, null);
            g.dispose();
            img = scaled;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return new Image(new ByteArrayInputStream(baos.toByteArray()));
    }

    // =========================================================================
    // Alertas
    // =========================================================================

    private void showError(String header, String body) {
        Alert a = new Alert(Alert.AlertType.ERROR, body, ButtonType.OK);
        a.setHeaderText(header);
        a.initOwner(dialog);
        a.showAndWait();
    }

    private void showSuccess(int count, String baseName) {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                count + " imágenes exportadas:\n" +
                        baseName + "-0.png  →  " + baseName + "-" + (count - 1) + ".png\n\n" +
                        "Cópialas a la carpeta de tu skin y añade en skin.ini:\n" +
                        "  LightFramePerSecond: " + GifFrameExtractor.suggestFps(loadedFrames),
                ButtonType.OK);
        a.setHeaderText("¡Conversión completada!");
        a.initOwner(dialog);
        a.showAndWait();
    }
}