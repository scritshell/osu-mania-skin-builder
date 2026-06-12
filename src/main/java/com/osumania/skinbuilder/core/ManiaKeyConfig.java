package com.osumania.skinbuilder.core;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a full [Mania] block for a specific key count in skin.ini.
 * Each instance maps to one "Keys: N" section.
 */
public class ManiaKeyConfig {

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    public enum PercyShape {
        FLAT,
        ROUNDED,
        TRIANGLE,
        FADE
    }

    public enum SpecialStyle {
        NONE,
        LEFT,
        RIGHT
    }

    public enum NoteBodyStyle {
        STRETCH(0),
        CASCADE_TOP(1),
        CASCADE_BOTTOM(2);

        public final int value;
        NoteBodyStyle(int v) { this.value = v; }
    }

    /**
     * Per-column visual configuration.
     * Tracks colours, note images, and key/receptor images for each column.
     */
    public static class ColumnConfig {
        // Note images
        public String noteImageRice     = "mania-note1";
        public String noteImageLnHead   = "mania-note1H";
        public String noteImageLnBody   = "mania-note1L";
        public String noteImageLnTail   = null;

        // ---- TASK 1: key/receptor images per column ----
        /** Base key image (idle state). e.g. "mania-key1" */
        public String keyImage          = "mania-key1";
        /** Pressed key image. e.g. "mania-key1D" */
        public String keyImageDown      = "mania-key1D";

        // Colours
        public Color riceColor          = Color.WHITE;
        public Color lnColor            = Color.WHITE;
        public Color lightColor         = new Color(255, 255, 255, 255);

        // Layout
        public int columnWidth          = 64;
        public int columnLineWidth      = 0;

        public ColumnConfig() {}

        public ColumnConfig(ColumnConfig other) {
            this.noteImageRice   = other.noteImageRice;
            this.noteImageLnHead = other.noteImageLnHead;
            this.noteImageLnBody = other.noteImageLnBody;
            this.noteImageLnTail = other.noteImageLnTail;
            this.keyImage        = other.keyImage;
            this.keyImageDown    = other.keyImageDown;
            this.riceColor       = other.riceColor;
            this.lnColor         = other.lnColor;
            this.lightColor      = other.lightColor;
            this.columnWidth     = other.columnWidth;
            this.columnLineWidth = other.columnLineWidth;
        }
    }

    // -------------------------------------------------------------------------
    // Main [Mania] block fields
    // -------------------------------------------------------------------------

    private int keys;
    private boolean enabled = true;

    // Positioning
    private int columnStart          = 250;
    private int hitPosition          = 410;
    private int scorePosition        = 210;
    private int comboPosition        = 210;
    private int lightPosition        = 435;
    private int lightFramePerSecond  = 25;
    private int barlineHeight        = 0;

    // ---- TASK 1: receptor offset (Y px, -200..200) ----
    /**
     * Visual-only Y offset applied to the receptor/key images.
     * Positive = shift receptor downward (transparent rows added on top).
     * Negative = shift receptor upward (transparent rows added on bottom).
     * Range clamped to [-200, 200]. Does NOT affect HitPosition in skin.ini.
     */
    private int receptorOffset = 0;

    // Behaviour
    private SpecialStyle specialStyle               = SpecialStyle.NONE;
    private boolean upsideDown                      = false;
    private boolean judgementLine                   = true;
    private NoteBodyStyle noteBodyStyle             = NoteBodyStyle.STRETCH;
    private boolean noteFlipWhenUpsideDown          = true;
    private boolean noteFlipWhenUpsideDownH         = true;
    private boolean noteFlipWhenUpsideDownT         = true;
    private boolean keysUnderNotes                  = false;

    // Split stages (10K+)
    private boolean splitStages   = false;
    private int stageSeparation   = 40;
    private boolean separateScore = true;

    // Global visuals
    private Color colourBarline             = new Color(255, 255, 255, 255);
    private Color colourHold                = new Color(255, 255, 255, 255);
    private String fontCombo                = "combo";
    private int widthForNoteHeightScale     = 0;

    // Stage images
    private String stageHintImage    = "mania-stage-hint";
    private String stageLeftImage    = "mania-stage-left";
    private String stageRightImage   = "mania-stage-right";
    private String stageBottomImage  = null;
    private String warningArrowImage = null;
    private String lightingNImage    = "lightingN";
    private String lightingLImage    = "lightingL";

    private int[] lightingNWidth = null;
    private int[] lightingLWidth = null;

    // Percy
    private int percySize                = 0;
    private PercyShape percyShape        = PercyShape.FLAT;
    private boolean useSeparateLnTail    = false;

    // Global transparency
    private boolean useGlobalTransparency = false;
    private int globalAlpha               = 255;

    private boolean useSeparateLnColor = false;

    // HD/FI overlay
    public enum HdFiMode { NONE, HD, FI, BOTH }
    private HdFiMode hdFiMode = HdFiMode.NONE;

    // Per-column configuration
    private List<ColumnConfig> columns = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ManiaKeyConfig(int keys) {
        this.keys = keys;
        initDefaultColumns(keys);
    }

    private void initDefaultColumns(int keys) {
        columns.clear();
        for (int i = 0; i < keys; i++) {
            ColumnConfig col     = new ColumnConfig();
            col.columnWidth      = defaultColumnWidth(keys);
            String noteType      = resolveDefaultNoteType(i, keys);
            col.noteImageRice    = "mania-" + noteType;
            col.noteImageLnHead  = "mania-" + noteType + "H";
            col.noteImageLnBody  = "mania-" + noteType + "L";
            col.lightColor       = defaultLightColor(i, keys);
            col.columnLineWidth  = defaultLineWidth(i, keys);

            // Default key images follow the same 1/2/S pattern as notes
            if (isSpecialColumn(i, keys)) {
                col.keyImage     = "mania-keyS";
                col.keyImageDown = "mania-keySd";
            } else {
                String kt        = (i % 2 == 0) ? "key1" : "key2";
                col.keyImage     = "mania-" + kt;
                col.keyImageDown = "mania-" + kt + "d";
            }
            columns.add(col);
        }
    }

    // -------------------------------------------------------------------------
    // Default value helpers
    // -------------------------------------------------------------------------

    private int defaultColumnWidth(int keys) {
        if (keys <= 4)  return 75;
        if (keys <= 5)  return 60;
        if (keys <= 6)  return 58;
        if (keys <= 7)  return 52;
        if (keys <= 8)  return 50;
        if (keys <= 9)  return 46;
        if (keys <= 10) return 44;
        if (keys <= 12) return 40;
        if (keys <= 14) return 38;
        if (keys <= 16) return 35;
        return 30;
    }

    private String resolveDefaultNoteType(int col, int keys) {
        if (isSpecialColumn(col, keys)) return "noteS";
        return (col % 2 == 0) ? "note1" : "note2";
    }

    public boolean isSpecialColumn(int col, int keys) {
        switch (keys) {
            case 5:  return col == 2;
            case 7:  return col == 3;
            case 8:  return col == 7;
            case 9:  return col == 0 || col == 8;
            case 12: return col == 0 || col == 6;
            case 14: return col == 0 || col == 13;
            case 16: return col == 0 || col == 15;
            default: return false;
        }
    }

    private Color defaultLightColor(int col, int keys) {
        int pos = col % 4;
        switch (pos) {
            case 0:  return new Color(255, 0, 0, 255);
            case 1:  return new Color(0, 0, 255, 255);
            case 2:  return new Color(255, 212, 0, 255);
            case 3:  return new Color(0, 0, 255, 255);
            default: return Color.WHITE;
        }
    }

    private int defaultLineWidth(int col, int keys) {
        return (col == 0 || col == keys - 1) ? 2 : 0;
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    public void autoCenterStage() {
        int totalWidth = columns.stream().mapToInt(c -> c.columnWidth).sum();
        this.columnStart = 256 - (totalWidth / 2);
        if (this.columnStart < 0) this.columnStart = 0;
    }

    public void setUniformColumnWidth(int width) {
        columns.forEach(c -> c.columnWidth = width);
    }

    public void setUniformRiceColor(Color color) {
        columns.forEach(c -> c.riceColor = color);
    }

    public void setUniformLnColor(Color color) {
        columns.forEach(c -> c.lnColor = color);
    }

    public String getDisplayName() {
        switch (keys) {
            case 10: return "10K";
            case 12: return "10K2S (12K)";
            case 14: return "DP/EZ2AC (14K)";
            case 16: return "DP/EZ2AC 2S (16K)";
            case 18: return "10K8K / 9K9K (18K)";
            default: return keys + "K";
        }
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getKeys() { return keys; }
    public void setKeys(int keys) {
        this.keys = keys;
        while (columns.size() < keys) {
            ColumnConfig last = columns.isEmpty() ? new ColumnConfig() : columns.get(columns.size() - 1);
            columns.add(new ColumnConfig(last));
        }
        while (columns.size() > keys) {
            columns.remove(columns.size() - 1);
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getColumnStart() { return columnStart; }
    public void setColumnStart(int columnStart) { this.columnStart = columnStart; }

    public int getHitPosition() { return hitPosition; }
    public void setHitPosition(int hitPosition) { this.hitPosition = hitPosition; }

    public int getScorePosition() { return scorePosition; }
    public void setScorePosition(int scorePosition) { this.scorePosition = scorePosition; }

    public int getComboPosition() { return comboPosition; }
    public void setComboPosition(int comboPosition) { this.comboPosition = comboPosition; }

    public int getLightPosition() { return lightPosition; }
    public void setLightPosition(int lightPosition) { this.lightPosition = lightPosition; }

    public int getLightFramePerSecond() { return lightFramePerSecond; }
    public void setLightFramePerSecond(int fps) { this.lightFramePerSecond = fps; }

    public int getBarlineHeight() { return barlineHeight; }
    public void setBarlineHeight(int barlineHeight) { this.barlineHeight = barlineHeight; }

    // ---- TASK 1: receptorOffset getter/setter ----
    /**
     * Returns the visual Y offset applied to receptor/key images.
     * Range: [-200, 200]. Does NOT affect skin.ini HitPosition.
     */
    public int getReceptorOffset() { return receptorOffset; }

    /**
     * Sets the visual Y offset for receptor/key images, clamped to [-200, 200].
     */
    public void setReceptorOffset(int offset) {
        this.receptorOffset = Math.max(-200, Math.min(200, offset));
    }

    public SpecialStyle getSpecialStyle() { return specialStyle; }
    public void setSpecialStyle(SpecialStyle specialStyle) { this.specialStyle = specialStyle; }

    public boolean isUpsideDown() { return upsideDown; }
    public void setUpsideDown(boolean upsideDown) { this.upsideDown = upsideDown; }

    public boolean isJudgementLine() { return judgementLine; }
    public void setJudgementLine(boolean judgementLine) { this.judgementLine = judgementLine; }

    public NoteBodyStyle getNoteBodyStyle() { return noteBodyStyle; }
    public void setNoteBodyStyle(NoteBodyStyle noteBodyStyle) { this.noteBodyStyle = noteBodyStyle; }

    public boolean isNoteFlipWhenUpsideDown() { return noteFlipWhenUpsideDown; }
    public void setNoteFlipWhenUpsideDown(boolean v) { this.noteFlipWhenUpsideDown = v; }

    public boolean isNoteFlipWhenUpsideDownH() { return noteFlipWhenUpsideDownH; }
    public void setNoteFlipWhenUpsideDownH(boolean v) { this.noteFlipWhenUpsideDownH = v; }

    public boolean isNoteFlipWhenUpsideDownT() { return noteFlipWhenUpsideDownT; }
    public void setNoteFlipWhenUpsideDownT(boolean v) { this.noteFlipWhenUpsideDownT = v; }

    public boolean isKeysUnderNotes() { return keysUnderNotes; }
    public void setKeysUnderNotes(boolean keysUnderNotes) { this.keysUnderNotes = keysUnderNotes; }

    public boolean isSplitStages() { return splitStages; }
    public void setSplitStages(boolean splitStages) { this.splitStages = splitStages; }

    public int getStageSeparation() { return stageSeparation; }
    public void setStageSeparation(int stageSeparation) { this.stageSeparation = stageSeparation; }

    public boolean isSeparateScore() { return separateScore; }
    public void setSeparateScore(boolean separateScore) { this.separateScore = separateScore; }

    public Color getColourBarline() { return colourBarline; }
    public void setColourBarline(Color colourBarline) { this.colourBarline = colourBarline; }

    public Color getColourHold() { return colourHold; }
    public void setColourHold(Color colourHold) { this.colourHold = colourHold; }

    public String getFontCombo() { return fontCombo; }
    public void setFontCombo(String fontCombo) { this.fontCombo = fontCombo; }

    public int getWidthForNoteHeightScale() { return widthForNoteHeightScale; }
    public void setWidthForNoteHeightScale(int w) { this.widthForNoteHeightScale = w; }

    public String getStageHintImage() { return stageHintImage; }
    public void setStageHintImage(String stageHintImage) { this.stageHintImage = stageHintImage; }

    public String getStageLeftImage() { return stageLeftImage; }
    public void setStageLeftImage(String stageLeftImage) { this.stageLeftImage = stageLeftImage; }

    public String getStageRightImage() { return stageRightImage; }
    public void setStageRightImage(String stageRightImage) { this.stageRightImage = stageRightImage; }

    public String getStageBottomImage() { return stageBottomImage; }
    public void setStageBottomImage(String stageBottomImage) { this.stageBottomImage = stageBottomImage; }

    public String getWarningArrowImage() { return warningArrowImage; }
    public void setWarningArrowImage(String warningArrowImage) { this.warningArrowImage = warningArrowImage; }

    public String getLightingNImage() { return lightingNImage; }
    public void setLightingNImage(String lightingNImage) { this.lightingNImage = lightingNImage; }

    public String getLightingLImage() { return lightingLImage; }
    public void setLightingLImage(String lightingLImage) { this.lightingLImage = lightingLImage; }

    public int[] getLightingNWidth() { return lightingNWidth; }
    public void setLightingNWidth(int[] lightingNWidth) { this.lightingNWidth = lightingNWidth; }

    public int[] getLightingLWidth() { return lightingLWidth; }
    public void setLightingLWidth(int[] lightingLWidth) { this.lightingLWidth = lightingLWidth; }

    public int getPercySize() { return percySize; }
    public void setPercySize(int percySize) { this.percySize = Math.max(0, Math.min(400, percySize)); }

    public PercyShape getPercyShape() { return percyShape; }
    public void setPercyShape(PercyShape percyShape) {
        this.percyShape = percyShape == null ? PercyShape.FLAT : percyShape;
    }

    public boolean isUseSeparateLnTail() { return useSeparateLnTail; }
    public void setUseSeparateLnTail(boolean useSeparateLnTail) { this.useSeparateLnTail = useSeparateLnTail; }

    public boolean isUseGlobalTransparency() { return useGlobalTransparency; }
    public void setUseGlobalTransparency(boolean useGlobalTransparency) { this.useGlobalTransparency = useGlobalTransparency; }

    public int getGlobalAlpha() { return globalAlpha; }
    public void setGlobalAlpha(int globalAlpha) { this.globalAlpha = Math.max(0, Math.min(255, globalAlpha)); }

    public boolean isUseSeparateLnColor() { return useSeparateLnColor; }
    public void setUseSeparateLnColor(boolean useSeparateLnColor) { this.useSeparateLnColor = useSeparateLnColor; }

    public HdFiMode getHdFiMode() { return hdFiMode; }
    public void setHdFiMode(HdFiMode hdFiMode) { this.hdFiMode = hdFiMode; }

    public List<ColumnConfig> getColumns() { return columns; }
    public ColumnConfig getColumn(int index) { return columns.get(index); }
    public void setColumns(List<ColumnConfig> columns) { this.columns = columns; }
}