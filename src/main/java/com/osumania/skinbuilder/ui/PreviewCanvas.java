package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.image.PreviewAssetManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class PreviewCanvas extends Canvas {

    private static final double OSU_PLAYFIELD_HEIGHT = 480.0;
    private ManiaKeyConfig lastConfig;
    private PreviewAssetManager assetManager;

    public PreviewCanvas() {
        setWidth(420);
        setHeight(560);
        widthProperty().addListener((obs, oldValue, newValue) -> redraw());
        heightProperty().addListener((obs, oldValue, newValue) -> redraw());
    }

    /**
     * Establece el gestor de assets para cargar imágenes.
     */
    public void setAssetManager(PreviewAssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void drawPreview(ManiaKeyConfig config) {
        this.lastConfig = config;

        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        gc.setFill(Color.rgb(18, 20, 24));
        gc.fillRect(0, 0, width, height);

        if (config == null || config.getColumns().isEmpty()) {
            return;
        }

        double stageTop = 24;
        double stageBottom = 28;
        double stageHeight = Math.max(120, height - stageTop - stageBottom);
        double stageContentWidth = calculateStageWidth(config);
        double maxStageWidth = Math.max(80, width - 48);
        double scale = Math.min(1.0, maxStageWidth / stageContentWidth);
        double scaledStageWidth = stageContentWidth * scale;
        double stageX = (width - scaledStageWidth) / 2.0;
        double hitY = stageTop + clamp(config.getHitPosition() / OSU_PLAYFIELD_HEIGHT, 0.0, 1.0) * stageHeight;

        gc.setFill(Color.rgb(30, 34, 42));
        gc.fillRect(stageX, stageTop, scaledStageWidth, stageHeight);

        drawColumns(gc, config, stageX, stageTop, stageHeight, scale, hitY);
        drawHitLine(gc, config, stageX, hitY, scaledStageWidth, scale);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return 420;
    }

    @Override
    public double prefHeight(double width) {
        return 560;
    }

    @Override
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    private void redraw() {
        if (lastConfig != null) {
            drawPreview(lastConfig);
        }
    }

    private void drawColumns(GraphicsContext gc,
                             ManiaKeyConfig config,
                             double stageX,
                             double stageTop,
                             double stageHeight,
                             double scale,
                             double hitY) {
        double x = stageX;
        int splitIndex = config.getKeys() / 2;
        double splitGap = getSplitGap(config) * scale;

        gc.setStroke(Color.rgb(95, 102, 118, 0.9));
        gc.setLineWidth(1.0);
        gc.strokeLine(x, stageTop, x, stageTop + stageHeight);

        for (int i = 0; i < config.getColumns().size(); i++) {
            if (config.isSplitStages() && i == splitIndex) {
                drawSplitGap(gc, x, stageTop, splitGap, stageHeight);
                x += splitGap;
                gc.strokeLine(x, stageTop, x, stageTop + stageHeight);
            }

            ManiaKeyConfig.ColumnConfig column = config.getColumn(i);
            double columnWidth = Math.max(1, column.columnWidth * scale);

            drawColumnBackground(gc, i, x, stageTop, columnWidth, stageHeight);
            drawLongNote(gc, config, column, x, columnWidth, hitY, stageTop, stageHeight, scale);
            drawRiceNote(gc, column, i, x, columnWidth, stageTop, stageHeight);

            x += columnWidth;
            gc.setStroke(Color.rgb(95, 102, 118, 0.85));
            gc.setLineWidth(1.0);
            gc.strokeLine(x, stageTop, x, stageTop + stageHeight);
        }
    }

    private void drawColumnBackground(GraphicsContext gc,
                                      int columnIndex,
                                      double x,
                                      double y,
                                      double width,
                                      double height) {
        Color fill = columnIndex % 2 == 0
                ? Color.rgb(42, 47, 58, 0.72)
                : Color.rgb(35, 40, 50, 0.72);
        gc.setFill(fill);
        gc.fillRect(x, y, width, height);
    }

    private void drawRiceNote(GraphicsContext gc,
                              ManiaKeyConfig.ColumnConfig column,
                              int columnIndex,
                              double columnX,
                              double columnWidth,
                              double stageTop,
                              double stageHeight) {
        double notePadding = Math.max(2, columnWidth * 0.12);
        double noteHeight = Math.max(8, Math.min(22, stageHeight * 0.045));
        double y = stageTop + stageHeight * (0.16 + (columnIndex % 4) * 0.075);
        double noteWidth = columnWidth - notePadding * 2;

        // Intentar usar imagen
        if (assetManager != null && column.noteImageRice != null) {
            Image img = assetManager.getImageWithFallback(column.noteImageRice);
            if (img != null) {
                // Calcular alto de imagen manteniendo aspect ratio
                double imgHeight = (img.getHeight() / img.getWidth()) * noteWidth;
                double drawHeight = Math.min(noteHeight, imgHeight);
                double yOffset = (noteHeight - drawHeight) / 2.0;
                gc.drawImage(img, columnX + notePadding, y + yOffset, noteWidth, drawHeight);
                return;
            }
        }

        // Fallback: rectángulo de color
        gc.setFill(toFxColor(column.riceColor, 0.95));
        gc.fillRoundRect(columnX + notePadding, y, noteWidth, noteHeight, 4, 4);

        gc.setStroke(Color.rgb(255, 255, 255, 0.28));
        gc.setLineWidth(1);
        gc.strokeRoundRect(columnX + notePadding, y, noteWidth, noteHeight, 4, 4);
    }

    private void drawLongNote(GraphicsContext gc,
                              ManiaKeyConfig config,
                              ManiaKeyConfig.ColumnConfig column,
                              double columnX,
                              double columnWidth,
                              double hitY,
                              double stageTop,
                              double stageHeight,
                              double scale) {
        double notePadding = Math.max(2, columnWidth * 0.18);
        double bodyHeight = Math.max(55, stageHeight * 0.24);
        double headHeight = Math.max(8, Math.min(20, stageHeight * 0.04));
        double bodyY = Math.max(stageTop + 12, hitY - bodyHeight);
        double bodyWidth = columnWidth - notePadding * 2;
        double percyGap = config.getPercySize() > 0
                ? Math.min(bodyHeight - headHeight, config.getPercySize() * Math.max(0.25, scale))
                : 0;
        double tailY = bodyY;  // Tail at TOP (first visual element)
        double bodyStartY = bodyY + headHeight;  // Body starts after Tail
        double headY = bodyY + bodyHeight - headHeight;  // Head at BOTTOM (last visual element)
        double visibleY = bodyY + percyGap;
        double visibleHeight = Math.max(headHeight, bodyHeight - percyGap);

        // Intentar usar imágenes
        boolean usedImages = false;
        if (assetManager != null) {
            // Dibujar Tail (superior - primero visualmente, se suelta último en osu!mania)
            if (column.noteImageLnTail != null || column.noteImageLnBody != null) {
                String tailName = config.isUseSeparateLnTail() && column.noteImageLnTail != null 
                    ? column.noteImageLnTail 
                    : column.noteImageLnBody;
                Image tailImg = assetManager.getImageWithFallback(tailName);
                if (tailImg != null) {
                    double tailDrawHeight = (tailImg.getHeight() / tailImg.getWidth()) * bodyWidth;
                    gc.drawImage(tailImg, columnX + notePadding, visibleY, bodyWidth, Math.min(headHeight, tailDrawHeight));
                    usedImages = true;
                }
            }

            // Dibujar Body (cuerpo de la LN, estirado en el medio)
            if (column.noteImageLnBody != null) {
                Image bodyImg = assetManager.getImageWithFallback(column.noteImageLnBody);
                if (bodyImg != null) {
                    // Estirar la imagen en Y
                    gc.drawImage(bodyImg, columnX + notePadding, bodyStartY, bodyWidth, Math.max(1, bodyHeight - 2 * headHeight));
                    usedImages = true;
                }
            }

            // Dibujar Head (inferior - último visualmente, toca primero la línea de juicio)
            if (column.noteImageLnHead != null) {
                Image headImg = assetManager.getImageWithFallback(column.noteImageLnHead);
                if (headImg != null) {
                    double headDrawHeight = (headImg.getHeight() / headImg.getWidth()) * bodyWidth;
                    gc.drawImage(headImg, columnX + notePadding, headY, bodyWidth, Math.min(headHeight, headDrawHeight));
                    usedImages = true;
                }
            }
        }

        // Si no se usaron imágenes, fallback a rectángulos de color
        if (!usedImages) {
            gc.setFill(toFxColor(column.lnColor, 0.58));
            gc.fillRect(columnX + notePadding, visibleY, bodyWidth, visibleHeight);

            gc.setFill(toFxColor(column.lnColor, 0.95));
            ManiaKeyConfig.PercyShape previewShape = config.getPercySize() > 0
                    ? config.getPercyShape()
                    : ManiaKeyConfig.PercyShape.FLAT;
            // Percy tip at top (Tail position)
            drawPercyTip(gc, previewShape, columnX + notePadding, visibleY, bodyWidth, headHeight, column.lnColor);
            // Head (clickable area) at bottom
            gc.fillRoundRect(columnX + notePadding, headY, bodyWidth, headHeight, 4, 4);
        }

        gc.setStroke(Color.rgb(255, 255, 255, 0.22));
        gc.setLineWidth(1);
        gc.strokeRect(columnX + notePadding, visibleY, bodyWidth, visibleHeight);
    }

    private void drawPercyTip(GraphicsContext gc,
                              ManiaKeyConfig.PercyShape shape,
                              double x,
                              double y,
                              double width,
                              double height,
                              java.awt.Color color) {
        ManiaKeyConfig.PercyShape safeShape = shape == null ? ManiaKeyConfig.PercyShape.FLAT : shape;
        Color fill = toFxColor(color, safeShape == ManiaKeyConfig.PercyShape.FADE ? 0.65 : 0.95);
        gc.setFill(fill);

        switch (safeShape) {
            case ROUNDED:
                gc.fillRoundRect(x, y, width, height, Math.min(width, height), Math.min(width, height));
                break;
            case TRIANGLE:
                gc.fillPolygon(
                        new double[] { x + width / 2.0, x + width, x + width, x },
                        new double[] { y, y + height, y + height, y + height },
                        4
                );
                break;
            case FADE:
            case FLAT:
            default:
                gc.fillRoundRect(x, y, width, height, 4, 4);
                break;
        }
    }

    private void drawSplitGap(GraphicsContext gc,
                              double x,
                              double y,
                              double gap,
                              double height) {
        gc.setFill(Color.rgb(12, 14, 18));
        gc.fillRect(x, y, gap, height);
        gc.setStroke(Color.rgb(160, 168, 190, 0.35));
        gc.setLineWidth(1.0);
        gc.strokeLine(x + gap / 2.0, y, x + gap / 2.0, y + height);
    }

    private void drawHitLine(GraphicsContext gc, ManiaKeyConfig config, double stageX, double hitY, double stageWidth, double scale) {
        // Apply receptor Y-offset to the hit line position
        double offsetInPixels = config.getReceptorOffset() * scale;
        double adjustedHitY = hitY + offsetInPixels;
        
        gc.setStroke(Color.rgb(255, 68, 68));
        gc.setLineWidth(2.0);
        gc.strokeLine(stageX, adjustedHitY, stageX + stageWidth, adjustedHitY);
    }

    private double calculateStageWidth(ManiaKeyConfig config) {
        double totalWidth = config.getColumns().stream()
                .mapToDouble(column -> Math.max(1, column.columnWidth))
                .sum();
        return totalWidth + getSplitGap(config);
    }

    private double getSplitGap(ManiaKeyConfig config) {
        return config.isSplitStages() ? Math.max(0, config.getStageSeparation()) : 0;
    }

    private Color toFxColor(java.awt.Color color, double opacity) {
        if (color == null) {
            return Color.rgb(255, 255, 255, opacity);
        }
        return Color.rgb(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                clamp((color.getAlpha() / 255.0) * opacity, 0.0, 1.0)
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
