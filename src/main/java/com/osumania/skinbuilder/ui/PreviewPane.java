package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.image.PreviewAssetManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Preview del stage de osu!mania usando el scene graph de JavaFX (Pane + nodos).
 *
 * Extiende StackPane — NO sobreescribe prefWidth/prefHeight porque son final en Region.
 * El tamaño preferido se establece con setPrefSize() en el constructor.
 */
public class PreviewPane extends StackPane {

    private static final double OSU_PLAYFIELD_HEIGHT = 480.0;
    private static final double SIDE_RESERVE         = 58.0;
    private static final double PREFERRED_W          = 420.0;
    private static final double PREFERRED_H          = 560.0;

    private ManiaKeyConfig      lastConfig;
    private PreviewAssetManager assetManager;

    private final Pane stagePlane = new Pane();

    private final AnimationTimer animationTimer;
    private boolean gifAnimationEnabled = false;
    private long    lastAnimFrameMs     = 0;

    private ImageView stageLeftView;
    private ImageView stageRightView;
    private ImageView stageHintView;
    private ImageView stageBottomView;

    // =========================================================================
    // Constructor
    // =========================================================================

    public PreviewPane() {
        setStyle("-fx-background-color: #111318;");
        setPrefSize(PREFERRED_W, PREFERRED_H);   // correcto: setter, no override
        setMinSize(160, 200);

        getChildren().add(stagePlane);
        StackPane.setAlignment(stagePlane, Pos.TOP_LEFT);

        widthProperty().addListener((obs, o, n)  -> redraw());
        heightProperty().addListener((obs, o, n) -> redraw());

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long nowNs) {
                long nowMs = nowNs / 1_000_000L;
                if (gifAnimationEnabled && nowMs - lastAnimFrameMs >= 40) {
                    lastAnimFrameMs = nowMs;
                    if (lastConfig != null) updateGifFrames();
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

    public void enableGifAnimation(boolean enable) {
        this.gifAnimationEnabled = enable;
    }

    public void drawPreview(ManiaKeyConfig config) {
        this.lastConfig = config;
        Platform.runLater(this::rebuild);
    }

    // =========================================================================
    // Layout
    // =========================================================================

    private void redraw() {
        if (lastConfig != null) rebuild();
    }

    private void rebuild() {
        stagePlane.getChildren().clear();
        stageLeftView = stageRightView = stageHintView = stageBottomView = null;

        ManiaKeyConfig config = lastConfig;
        if (config == null || config.getColumns().isEmpty()) return;

        double paneW = Math.max(40,  getWidth());
        double paneH = Math.max(120, getHeight());

        double availableW  = Math.max(40, paneW - SIDE_RESERVE * 2);
        double stageHeight = paneH;
        double stageTop    = 0;

        double stageContentW = calculateStageWidth(config);
        double scale         = Math.min(availableW / stageContentW, 4.0);
        double scaledStageW  = stageContentW * scale;
        double stageX        = SIDE_RESERVE + (availableW - scaledStageW) / 2.0;

        double hitYRatio = clamp(config.getHitPosition() / OSU_PLAYFIELD_HEIGHT, 0.0, 1.0);
        double hitY      = stageTop + hitYRatio * stageHeight;

        stagePlane.getChildren().add(rect(stageX, stageTop, scaledStageW, stageHeight, Color.BLACK, null));
        addStageBorderNodes(config, stageX, stageTop, stageHeight, scaledStageW, paneW);
        addColumnNodes(config, stageX, stageTop, stageHeight, scale, hitY);
        addStageHintNode(config, stageX, hitY, scaledStageW);
        addHitLine(config, stageX, hitY, scaledStageW, scale);
    }

    // =========================================================================
    // Stage images
    // =========================================================================

    private void addStageBorderNodes(ManiaKeyConfig config,
                                     double stageX, double stageTop,
                                     double stageHeight, double scaledStageW,
                                     double paneW) {
        if (assetManager == null) return;
        double rightEdge = stageX + scaledStageW;

        Image leftImg = getStageFrame(config.getStageLeftImage());
        if (leftImg != null && stageX > 1) {
            double aspect = safeDivide(leftImg.getWidth(), leftImg.getHeight());
            double drawW  = Math.min(stageX, stageHeight * aspect);
            stageLeftView = imageView(leftImg, stageX - drawW, stageTop, drawW, stageHeight);
            stagePlane.getChildren().add(stageLeftView);
        }

        Image rightImg = getStageFrame(config.getStageRightImage());
        if (rightImg != null && rightEdge < paneW - 1) {
            double aspect = safeDivide(rightImg.getWidth(), rightImg.getHeight());
            double drawW  = Math.min(paneW - rightEdge, stageHeight * aspect);
            stageRightView = imageView(rightImg, rightEdge, stageTop, drawW, stageHeight);
            stagePlane.getChildren().add(stageRightView);
        }

        String bottomName = config.getStageBottomImage();
        if (bottomName != null) {
            Image bottomImg = getStageFrame(bottomName);
            if (bottomImg != null) {
                double aspect = safeDivide(bottomImg.getWidth(), bottomImg.getHeight());
                double drawW  = scaledStageW;
                double drawH  = Math.min(safeDivide(drawW, aspect), stageHeight * 0.15);
                stageBottomView = imageView(bottomImg, stageX, stageTop + stageHeight - drawH, drawW, drawH);
                stagePlane.getChildren().add(stageBottomView);
            }
        }
    }

    private void addStageHintNode(ManiaKeyConfig config,
                                  double stageX, double hitY, double scaledStageW) {
        if (assetManager == null) return;
        Image hintImg = getStageFrame(config.getStageHintImage());
        if (hintImg == null || hintImg.getWidth() == 0) return;

        double aspect = safeDivide(hintImg.getWidth(), hintImg.getHeight());
        double drawW  = scaledStageW;
        double drawH  = Math.min(safeDivide(drawW, aspect), 48);
        stageHintView = imageView(hintImg, stageX, hitY - drawH / 2.0, drawW, drawH);
        stageHintView.setOpacity(0.88);
        stagePlane.getChildren().add(stageHintView);
    }

    private Image getStageFrame(String imageName) {
        if (assetManager == null || imageName == null) return null;
        List<Image> frames = assetManager.getStageGifFrames(imageName);
        if (frames != null && !frames.isEmpty()) {
            int  fps     = Math.max(1, assetManager.getStageGifFps(imageName));
            long frameMs = 1000L / fps;
            int  idx     = (int) ((System.currentTimeMillis() / frameMs) % frames.size());
            return frames.get(idx);
        }
        return assetManager.getStageImage(imageName);
    }

    private void updateGifFrames() {
        updateGifView(stageLeftView,   lastConfig.getStageLeftImage());
        updateGifView(stageRightView,  lastConfig.getStageRightImage());
        updateGifView(stageHintView,   lastConfig.getStageHintImage());
        updateGifView(stageBottomView, lastConfig.getStageBottomImage());
    }

    private void updateGifView(ImageView view, String imageName) {
        if (view == null || imageName == null || assetManager == null) return;
        List<Image> frames = assetManager.getStageGifFrames(imageName);
        if (frames == null || frames.isEmpty()) return;
        int  fps     = Math.max(1, assetManager.getStageGifFps(imageName));
        long frameMs = 1000L / fps;
        int  idx     = (int) ((System.currentTimeMillis() / frameMs) % frames.size());
        view.setImage(frames.get(idx));
    }

    // =========================================================================
    // Columnas y notas
    // =========================================================================

    private void addColumnNodes(ManiaKeyConfig config,
                                double stageX, double stageTop,
                                double stageHeight, double scale, double hitY) {
        List<ManiaKeyConfig.ColumnConfig> cols = config.getColumns();
        int    keys       = cols.size();
        int[]  lineWidths = buildLineWidths(config);
        int    splitIndex = keys / 2;
        double splitGap   = getSplitGap(config) * scale;
        double x          = stageX;

        double leftBorderW = lineWidths[0] * scale;
        if (leftBorderW >= 1) {
            stagePlane.getChildren().add(rect(x, stageTop, leftBorderW, stageHeight, Color.rgb(210,210,210,0.9), null));
            x += leftBorderW;
        }

        for (int i = 0; i < keys; i++) {
            if (config.isSplitStages() && i == splitIndex) {
                stagePlane.getChildren().add(rect(x, stageTop, splitGap, stageHeight, Color.rgb(12,14,18), null));
                Line sep = new Line(x + splitGap / 2, stageTop, x + splitGap / 2, stageTop + stageHeight);
                sep.setStroke(Color.rgb(160,168,190,0.35));
                sep.setStrokeWidth(1.0);
                stagePlane.getChildren().add(sep);
                x += splitGap;
                if (leftBorderW >= 1) {
                    stagePlane.getChildren().add(rect(x, stageTop, leftBorderW, stageHeight, Color.rgb(210,210,210,0.9), null));
                    x += leftBorderW;
                }
            }

            ManiaKeyConfig.ColumnConfig col = cols.get(i);
            double colW = Math.max(1, col.columnWidth * scale);

            stagePlane.getChildren().add(rect(x, stageTop, colW, stageHeight, Color.BLACK, null));
            addLnNote(config, col, x, colW, hitY, stageTop, stageHeight, scale);
            addRiceNote(config, col, i, x, colW, stageTop, stageHeight, hitY);

            x += colW;

            double sepW = lineWidths[i + 1] * scale;
            if (sepW >= 1) {
                stagePlane.getChildren().add(rect(x, stageTop, sepW, stageHeight, Color.rgb(210,210,210,0.9), null));
                x += sepW;
            }
        }
    }

    private void addRiceNote(ManiaKeyConfig config,
                             ManiaKeyConfig.ColumnConfig column,
                             int colIndex,
                             double colX, double colW,
                             double stageTop, double stageH, double hitY) {
        double pad   = Math.max(2, colW * 0.12);
        double noteH = Math.max(8, Math.min(22, stageH * 0.045));
        double noteW = colW - pad * 2;
        double dist  = stageH * (0.22 + (colIndex % 5) * 0.08);
        double y     = Math.max(stageTop + 4, hitY - dist);

        if (assetManager != null && column.noteImageRice != null) {
            int            alpha = config.isUseGlobalTransparency() ? config.getGlobalAlpha() : 255;
            java.awt.Color tint  = column.riceColor != null ? column.riceColor : java.awt.Color.WHITE;
            Image img = assetManager.getTintedImage(column.noteImageRice, tint, alpha);
            if (img != null) {
                double imgH  = safeDivide(img.getHeight(), img.getWidth()) * noteW;
                double drawH = Math.min(noteH, imgH);
                stagePlane.getChildren().add(imageView(img, colX + pad, y + (noteH - drawH) / 2.0, noteW, drawH));
                return;
            }
        }
        stagePlane.getChildren().add(rect(colX + pad, y, noteW, noteH,
                toFxColor(column.riceColor, 0.95), Color.rgb(255,255,255,0.28)));
    }

    private void addLnNote(ManiaKeyConfig config,
                           ManiaKeyConfig.ColumnConfig column,
                           double colX, double colW,
                           double hitY, double stageTop, double stageH, double scale) {
        double pad      = Math.max(2, colW * 0.18);
        double bodyH    = Math.max(55, stageH * 0.24);
        double headH    = Math.max(8, Math.min(20, stageH * 0.04));
        double bodyY    = Math.max(stageTop + 12, hitY - bodyH);
        double bodyW    = colW - pad * 2;
        double percyGap = config.getPercySize() > 0
                ? Math.min(bodyH - headH, config.getPercySize() * Math.max(0.25, scale)) : 0;
        double bodyStartY = bodyY + headH;
        double headY    = bodyY + bodyH - headH;
        double visibleY = bodyY + percyGap;
        double visibleH = Math.max(headH, bodyH - percyGap);

        int            alpha  = config.isUseGlobalTransparency() ? config.getGlobalAlpha() : 255;
        java.awt.Color lnTint = (config.isUseSeparateLnColor() && column.lnColor != null)
                ? column.lnColor : (column.riceColor != null ? column.riceColor : java.awt.Color.WHITE);

        boolean usedImages = false;
        if (assetManager != null) {
            String tailName = config.isUseSeparateLnTail() && column.noteImageLnTail != null
                    ? column.noteImageLnTail : column.noteImageLnBody;

            if (tailName != null) {
                Image img = assetManager.getTintedImage(tailName, lnTint, alpha);
                if (img != null) {
                    double h = safeDivide(img.getHeight(), img.getWidth()) * bodyW;
                    stagePlane.getChildren().add(imageView(img, colX + pad, visibleY, bodyW, Math.min(headH, h)));
                    usedImages = true;
                }
            }
            if (column.noteImageLnBody != null) {
                Image img = assetManager.getTintedImage(column.noteImageLnBody, lnTint, alpha);
                if (img != null) {
                    stagePlane.getChildren().add(imageView(img, colX + pad, bodyStartY, bodyW, Math.max(1, bodyH - 2 * headH)));
                    usedImages = true;
                }
            }
            if (column.noteImageLnHead != null) {
                Image img = assetManager.getTintedImage(column.noteImageLnHead, lnTint, alpha);
                if (img != null) {
                    double h = safeDivide(img.getHeight(), img.getWidth()) * bodyW;
                    stagePlane.getChildren().add(imageView(img, colX + pad, headY, bodyW, Math.min(headH, h)));
                    usedImages = true;
                }
            }
        }

        if (!usedImages) {
            stagePlane.getChildren().add(rect(colX + pad, visibleY, bodyW, visibleH, toFxColor(column.lnColor, 0.58), null));
            stagePlane.getChildren().add(rect(colX + pad, headY, bodyW, headH, toFxColor(column.lnColor, 0.95), Color.rgb(255,255,255,0.22)));
            Rectangle border = new Rectangle(colX + pad, visibleY, bodyW, visibleH);
            border.setFill(Color.TRANSPARENT);
            border.setStroke(Color.rgb(255,255,255,0.22));
            border.setStrokeWidth(1);
            stagePlane.getChildren().add(border);
        }
    }

    private void addHitLine(ManiaKeyConfig config,
                            double stageX, double hitY,
                            double stageWidth, double scale) {
        boolean hasHint = assetManager != null
                && assetManager.getStageImage(config.getStageHintImage()) != null;
        double adjustedY = hitY + config.getReceptorOffset() * scale;
        Line line = new Line(stageX, adjustedY, stageX + stageWidth, adjustedY);
        line.setStroke(hasHint ? Color.rgb(255,80,80,0.35) : Color.rgb(255,68,68,0.85));
        line.setStrokeWidth(hasHint ? 1.0 : 2.0);
        stagePlane.getChildren().add(line);
    }

    // =========================================================================
    // Factory helpers
    // =========================================================================

    private static Rectangle rect(double x, double y, double w, double h,
                                  Color fill, Color stroke) {
        Rectangle r = new Rectangle(x, y, Math.max(0, w), Math.max(0, h));
        r.setFill(fill != null ? fill : Color.TRANSPARENT);
        if (stroke != null) { r.setStroke(stroke); r.setStrokeWidth(1); }
        return r;
    }

    private static ImageView imageView(Image img, double x, double y, double w, double h) {
        if (img == null || w < 1 || h < 1) return new ImageView();
        ImageView iv = new ImageView(img);
        iv.setLayoutX(x);
        iv.setLayoutY(y);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        return iv;
    }

    // =========================================================================
    // Helpers matemáticos / layout
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
        if (c == null) return Color.rgb(255,255,255,opacity);
        return Color.rgb(c.getRed(), c.getGreen(), c.getBlue(),
                clamp((c.getAlpha() / 255.0) * opacity, 0.0, 1.0));
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double safeDivide(double a, double b) {
        return (b == 0) ? 1.0 : a / b;
    }
}