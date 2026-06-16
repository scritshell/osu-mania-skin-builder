package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.image.PreviewAssetManager;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

public class PreviewCanvas extends Canvas {

    private static final double OSU_PLAYFIELD_HEIGHT = 480.0;

    private ManiaKeyConfig      lastConfig;
    private PreviewAssetManager assetManager;

    // ── Animación GIF ─────────────────────────────────────────────────────────
    private final AnimationTimer animationTimer;
    private boolean gifAnimationEnabled = false;
    private long    lastAnimFrameMs     = 0;

    // =========================================================================
    // Constructor
    // =========================================================================

    public PreviewCanvas() {
        setWidth(420);
        setHeight(560);

        widthProperty().addListener((obs, o, n)  -> redraw());
        heightProperty().addListener((obs, o, n) -> redraw());

        // Timer único para animar GIFs de stage images (~25 fps max)
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long nowNs) {
                long nowMs = nowNs / 1_000_000L;
                if (gifAnimationEnabled && nowMs - lastAnimFrameMs >= 40) {
                    lastAnimFrameMs = nowMs;
                    if (lastConfig != null) drawPreview(lastConfig);
                }
            }
        };
        animationTimer.start();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public void setAssetManager(PreviewAssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /** Activa el timer de animación (llamar cuando se añade un GIF). */
    public void enableGifAnimation(boolean enable) {
        this.gifAnimationEnabled = enable;
    }

    // =========================================================================
    // Dibujo principal
    // =========================================================================

    public void drawPreview(ManiaKeyConfig config) {
        this.lastConfig = config;
        GraphicsContext gc    = getGraphicsContext2D();
        double          width = getWidth();
        double          height= getHeight();

        gc.setFill(Color.rgb(18, 20, 24));
        gc.fillRect(0, 0, width, height);

        if (config == null || config.getColumns().isEmpty()) return;

        // Reservar espacio para StageLeft/Right a ambos lados del stage
        final double SIDE_RESERVE = 58;
        final double TOP_MARGIN   = 8;
        final double BOT_MARGIN   = 8;

        double availableW  = Math.max(40, width  - SIDE_RESERVE * 2);
        double stageHeight = Math.max(120, height - TOP_MARGIN - BOT_MARGIN);
        double stageTop    = TOP_MARGIN;

        double stageContentWidth = calculateStageWidth(config);

        // Sin cap a 1.0 → el stage escala HACIA ARRIBA para llenar el espacio disponible
        double scale            = Math.min(availableW / stageContentWidth, 4.0);
        double scaledStageWidth = stageContentWidth * scale;

        // Centrar el stage dentro del área disponible (excluyendo márgenes laterales)
        double stageX = SIDE_RESERVE + (availableW - scaledStageWidth) / 2.0;
        double hitY   = stageTop
                + clamp(config.getHitPosition() / OSU_PLAYFIELD_HEIGHT, 0.0, 1.0)
                * stageHeight;

        // 1. Fondo negro puro del stage
        gc.setFill(Color.BLACK);
        gc.fillRect(stageX, stageTop, scaledStageWidth, stageHeight);

        // 2. Paneles de borde del stage (detrás de las notas)
        drawStageBorderImages(gc, config, stageX, stageTop, stageHeight, scaledStageWidth);

        // 3. Columnas + notas
        drawColumns(gc, config, stageX, stageTop, stageHeight, scale, hitY);

        // 4. StageHint encima de las notas
        drawStageHintImage(gc, config, stageX, hitY, scaledStageWidth);

        // 5. Línea de hit de referencia
        drawHitLine(gc, config, stageX, hitY, scaledStageWidth, scale);
    }

    // =========================================================================
    // Utilidad de dibujo seguro (Protección contra NPE de Prism)
    // =========================================================================

    /**
     * Wrapper seguro de gc.drawImage.
     * JavaFX lanza NPE en el hilo de renderizado (NGCanvas.handleRenderOp) si
     * se llama con width ≤ 0, height ≤ 0, o con una imagen en estado de error.
     * Este método absorbe todos esos casos antes de llegar al canvas.
     */
    private void safeDraw(GraphicsContext gc, Image img, double x, double y, double w, double h) {
        if (img == null || img.isError() || w < 1.0 || h < 1.0) return;
        gc.drawImage(img, x, y, w, h);
    }

    // =========================================================================
    // Stage images
    // =========================================================================

    private Image getCurrentStageFrame(String imageName) {
        if (assetManager == null || imageName == null) return null;

        List<Image> frames = assetManager.getStageGifFrames(imageName);
        if (frames != null && !frames.isEmpty()) {
            int  fps      = Math.max(1, assetManager.getStageGifFps(imageName));
            long frameMs  = 1000L / fps;
            int  idx      = (int) ((System.currentTimeMillis() / frameMs) % frames.size());
            return frames.get(idx);
        }
        return assetManager.getStageImage(imageName);
    }

    private void drawStageBorderImages(GraphicsContext gc,
                                       ManiaKeyConfig config,
                                       double stageX,
                                       double stageTop,
                                       double stageHeight,
                                       double scaledStageWidth) {
        if (assetManager == null) return;
        double canvasW = getWidth();

        // ── StageLeft ─────────────────────────────────────────────────────────────
        Image leftImg = getCurrentStageFrame(config.getStageLeftImage());
        if (leftImg != null && !leftImg.isError() && leftImg.getWidth() > 0 && stageX > 1) {
            double aspect = leftImg.getWidth() / leftImg.getHeight();
            double drawH  = stageHeight;
            double drawW  = Math.min(stageX, drawH * aspect);
            safeDraw(gc, leftImg, stageX - drawW, stageTop, drawW, drawH);
        }

        // ── StageRight ────────────────────────────────────────────────────────────
        Image rightImg = getCurrentStageFrame(config.getStageRightImage());
        double rightEdge = stageX + scaledStageWidth;
        if (rightImg != null && !rightImg.isError() && rightImg.getWidth() > 0 && rightEdge < canvasW - 1) {
            double aspect = rightImg.getWidth() / rightImg.getHeight();
            double drawH  = stageHeight;
            double drawW  = Math.min(canvasW - rightEdge, drawH * aspect);
            safeDraw(gc, rightImg, rightEdge, stageTop, drawW, drawH);
        }

        // ── StageBottom ───────────────────────────────────────────────────────────
        String bottomName = config.getStageBottomImage();
        if (bottomName != null) {
            Image bottomImg = getCurrentStageFrame(bottomName);
            if (bottomImg != null && !bottomImg.isError() && bottomImg.getWidth() > 0) {
                double aspect = bottomImg.getWidth() / bottomImg.getHeight();
                double drawW  = scaledStageWidth;
                double drawH  = Math.min(drawW / aspect, stageHeight * 0.15);
                safeDraw(gc, bottomImg, stageX, stageTop + stageHeight - drawH, drawW, drawH);
            }
        }
    }

    private void drawStageHintImage(GraphicsContext gc,
                                    ManiaKeyConfig config,
                                    double stageX,
                                    double hitY,
                                    double scaledStageWidth) {
        if (assetManager == null) return;
        Image hintImg = getCurrentStageFrame(config.getStageHintImage());
        if (hintImg == null || hintImg.isError() || hintImg.getWidth() == 0) return;

        double aspect = hintImg.getWidth() / hintImg.getHeight();
        double drawW  = scaledStageWidth;
        double drawH  = Math.min(drawW / aspect, 48);
        double drawY  = hitY - drawH / 2.0;

        gc.setGlobalAlpha(0.88);
        safeDraw(gc, hintImg, stageX, drawY, drawW, drawH);
        gc.setGlobalAlpha(1.0);
    }

    // =========================================================================
    // Columnas
    // =========================================================================

    private void drawColumns(GraphicsContext gc,
                             ManiaKeyConfig config,
                             double stageX,
                             double stageTop,
                             double stageHeight,
                             double scale,
                             double hitY) {
        List<ManiaKeyConfig.ColumnConfig> cols = config.getColumns();
        int    keys       = cols.size();
        int[]  lineWidths = buildLineWidths(config);
        int    splitIndex = keys / 2;
        double splitGap   = getSplitGap(config) * scale;

        double x = stageX;

        double leftBorderW = lineWidths[0] * scale;
        if (leftBorderW > 0) {
            gc.setFill(Color.rgb(210, 210, 210, 0.9));
            gc.fillRect(x, stageTop, leftBorderW, stageHeight);
            x += leftBorderW;
        }

        for (int i = 0; i < keys; i++) {
            if (config.isSplitStages() && i == splitIndex) {
                drawSplitGap(gc, x, stageTop, splitGap, stageHeight);
                x += splitGap;
                if (leftBorderW > 0) {
                    gc.setFill(Color.rgb(210, 210, 210, 0.9));
                    gc.fillRect(x, stageTop, leftBorderW, stageHeight);
                    x += leftBorderW;
                }
            }

            ManiaKeyConfig.ColumnConfig column = cols.get(i);
            double columnWidth = Math.max(1, column.columnWidth * scale);

            drawColumnBackground(gc, i, x, stageTop, columnWidth, stageHeight);
            drawLongNote(gc, config, column, x, columnWidth, hitY, stageTop, stageHeight, scale);
            drawRiceNote(gc, config, column, i, x, columnWidth, stageTop, stageHeight, hitY);

            x += columnWidth;

            double sepW = lineWidths[i + 1] * scale;
            if (sepW > 0) {
                gc.setFill(Color.rgb(210, 210, 210, 0.9));
                gc.fillRect(x, stageTop, sepW, stageHeight);
                x += sepW;
            }
        }
    }

    private void drawColumnBackground(GraphicsContext gc, int columnIndex,
                                      double x, double y, double w, double h) {
        gc.setFill(Color.BLACK);
        gc.fillRect(x, y, w, h);
    }

    private void drawRiceNote(GraphicsContext gc,
                              ManiaKeyConfig config,
                              ManiaKeyConfig.ColumnConfig column,
                              int columnIndex,
                              double columnX,
                              double columnWidth,
                              double stageTop,
                              double stageHeight,
                              double hitY) {
        double notePadding = Math.max(2, columnWidth * 0.12);
        double noteHeight  = Math.max(8, Math.min(22, stageHeight * 0.045));
        double noteWidth   = columnWidth - notePadding * 2;

        double distFromHit = stageHeight * (0.22 + (columnIndex % 5) * 0.08);
        double y = Math.max(stageTop + 4, hitY - distFromHit);

        if (assetManager != null && column.noteImageRice != null) {
            int            alpha = config.isUseGlobalTransparency() ? config.getGlobalAlpha() : 255;
            java.awt.Color tint  = column.riceColor != null ? column.riceColor : java.awt.Color.WHITE;
            Image img = assetManager.getTintedImage(column.noteImageRice, tint, alpha);

            if (img != null) {
                double imgH  = (img.getHeight() / img.getWidth()) * noteWidth;
                double drawH = Math.min(noteHeight, imgH);
                safeDraw(gc, img, columnX + notePadding, y + (noteHeight - drawH) / 2.0, noteWidth, drawH);
                return;
            }
        }
        gc.setFill(toFxColor(column.riceColor, 0.95));
        gc.fillRoundRect(columnX + notePadding, y, noteWidth, noteHeight, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 255, 0.28));
        gc.setLineWidth(1);
        gc.strokeRoundRect(columnX + notePadding, y, noteWidth, noteHeight, 4, 4);
    }

    private void drawLongNote(GraphicsContext gc,
                              ManiaKeyConfig config,
                              ManiaKeyConfig.ColumnConfig column,
                              double columnX, double columnWidth,
                              double hitY, double stageTop, double stageHeight,
                              double scale) {
        double notePadding = Math.max(2, columnWidth * 0.18);
        double bodyHeight  = Math.max(55, stageHeight * 0.24);
        double headHeight  = Math.max(8, Math.min(20, stageHeight * 0.04));
        double bodyY       = Math.max(stageTop + 12, hitY - bodyHeight);
        double bodyWidth   = columnWidth - notePadding * 2;
        double percyGap    = config.getPercySize() > 0
                ? Math.min(bodyHeight - headHeight, config.getPercySize() * Math.max(0.25, scale)) : 0;
        double bodyStartY  = bodyY + headHeight;
        double headY       = bodyY + bodyHeight - headHeight;
        double visibleY    = bodyY + percyGap;
        double visibleH    = Math.max(headHeight, bodyHeight - percyGap);

        int            alpha  = config.isUseGlobalTransparency() ? config.getGlobalAlpha() : 255;
        java.awt.Color lnTint = (config.isUseSeparateLnColor() && column.lnColor != null)
                ? column.lnColor : (column.riceColor != null ? column.riceColor : java.awt.Color.WHITE);

        boolean usedImages = false;
        if (assetManager != null) {
            String tailName = config.isUseSeparateLnTail() && column.noteImageLnTail != null
                    ? column.noteImageLnTail : column.noteImageLnBody;

            // Tail
            if (tailName != null) {
                Image tailImg = assetManager.getTintedImage(tailName, lnTint, alpha);
                if (tailImg != null) {
                    double h = (tailImg.getHeight() / tailImg.getWidth()) * bodyWidth;
                    safeDraw(gc, tailImg, columnX + notePadding, visibleY, bodyWidth, Math.min(headHeight, h));
                    usedImages = true;
                }
            }

            // Body
            if (column.noteImageLnBody != null) {
                Image bodyImg = assetManager.getTintedImage(column.noteImageLnBody, lnTint, alpha);
                if (bodyImg != null) {
                    safeDraw(gc, bodyImg, columnX + notePadding, bodyStartY, bodyWidth, Math.max(1, bodyHeight - 2 * headHeight));
                    usedImages = true;
                }
            }

            // Head
            if (column.noteImageLnHead != null) {
                Image headImg = assetManager.getTintedImage(column.noteImageLnHead, lnTint, alpha);
                if (headImg != null) {
                    double h = (headImg.getHeight() / headImg.getWidth()) * bodyWidth;
                    safeDraw(gc, headImg, columnX + notePadding, headY, bodyWidth, Math.min(headHeight, h));
                    usedImages = true;
                }
            }
        }

        if (!usedImages) {
            gc.setFill(toFxColor(column.lnColor, 0.58));
            gc.fillRect(columnX + notePadding, visibleY, bodyWidth, visibleH);
            gc.setFill(toFxColor(column.lnColor, 0.95));
            ManiaKeyConfig.PercyShape shape = config.getPercySize() > 0
                    ? config.getPercyShape() : ManiaKeyConfig.PercyShape.FLAT;
            drawPercyTip(gc, shape, columnX + notePadding, visibleY, bodyWidth, headHeight, column.lnColor);
            gc.fillRoundRect(columnX + notePadding, headY, bodyWidth, headHeight, 4, 4);
        }
        gc.setStroke(Color.rgb(255, 255, 255, 0.22));
        gc.setLineWidth(1);
        gc.strokeRect(columnX + notePadding, visibleY, bodyWidth, visibleH);
    }

    private void drawPercyTip(GraphicsContext gc, ManiaKeyConfig.PercyShape shape,
                              double x, double y, double w, double h, java.awt.Color c) {
        gc.setFill(toFxColor(c, shape == ManiaKeyConfig.PercyShape.FADE ? 0.65 : 0.95));
        switch (shape == null ? ManiaKeyConfig.PercyShape.FLAT : shape) {
            case ROUNDED  -> gc.fillRoundRect(x, y, w, h, Math.min(w, h), Math.min(w, h));
            case TRIANGLE -> gc.fillPolygon(
                    new double[]{ x + w / 2.0, x + w, x + w, x },
                    new double[]{ y, y + h, y + h, y + h }, 4);
            default       -> gc.fillRoundRect(x, y, w, h, 4, 4);
        }
    }

    private void drawSplitGap(GraphicsContext gc, double x, double y, double gap, double h) {
        gc.setFill(Color.rgb(12, 14, 18));
        gc.fillRect(x, y, gap, h);
        gc.setStroke(Color.rgb(160, 168, 190, 0.35));
        gc.setLineWidth(1.0);
        gc.strokeLine(x + gap / 2.0, y, x + gap / 2.0, y + h);
    }

    private void drawHitLine(GraphicsContext gc, ManiaKeyConfig config,
                             double stageX, double hitY, double stageWidth, double scale) {
        boolean hasHint = assetManager != null
                && assetManager.getStageImage(config.getStageHintImage()) != null;
        double adjusted = hitY + config.getReceptorOffset() * scale;
        gc.setStroke(hasHint ? Color.rgb(255, 80, 80, 0.35) : Color.rgb(255, 68, 68, 0.85));
        gc.setLineWidth(hasHint ? 1.0 : 2.0);
        gc.strokeLine(stageX, adjusted, stageX + stageWidth, adjusted);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private int[] buildLineWidths(ManiaKeyConfig config) {
        List<ManiaKeyConfig.ColumnConfig> cols = config.getColumns();
        int n = cols.size();
        if (n == 0) return new int[]{ 0 };
        int[] w = new int[n + 1];
        w[0] = cols.get(0).columnLineWidth;
        w[n] = cols.get(n - 1).columnLineWidth;
        return w;
    }

    private double calculateStageWidth(ManiaKeyConfig config) {
        double total = config.getColumns().stream().mapToDouble(c -> Math.max(1, c.columnWidth)).sum();
        for (int w : buildLineWidths(config)) total += w;
        return total + getSplitGap(config);
    }

    private double getSplitGap(ManiaKeyConfig config) {
        return config.isSplitStages() ? Math.max(0, config.getStageSeparation()) : 0;
    }

    private Color toFxColor(java.awt.Color c, double opacity) {
        if (c == null) return Color.rgb(255, 255, 255, opacity);
        return Color.rgb(c.getRed(), c.getGreen(), c.getBlue(),
                clamp((c.getAlpha() / 255.0) * opacity, 0.0, 1.0));
    }

    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private void redraw() { if (lastConfig != null) drawPreview(lastConfig); }

    @Override public boolean isResizable()            { return true; }
    @Override public double  prefWidth(double height) { return 420;  }
    @Override public double  prefHeight(double width) { return 560;  }
    @Override public void    resize(double w, double h) { setWidth(w); setHeight(h); }
}