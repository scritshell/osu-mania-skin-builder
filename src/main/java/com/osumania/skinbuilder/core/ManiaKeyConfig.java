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

    public enum PercyType {
        NONE,       // Sin percy: usa la imagen de body que tengas en la skin base
        PERCY_A,    // Percy tipo A (body limpio, sin decoración)
        PERCY_B,    // Percy tipo B (con borde)
        PERCY_C,    // Percy tipo C (gradiente)
        PERCY_D     // Percy tipo D (con brillo central)
    }

    public enum SpecialStyle {
        NONE,
        LEFT,
        RIGHT
    }

    public enum NoteBodyStyle {
        STRETCH(0),     // Imagen estirada
        CASCADE_TOP(1), // Cascada desde arriba
        CASCADE_BOTTOM(2); // Cascada desde abajo

        public final int value;
        NoteBodyStyle(int v) { this.value = v; }
    }

    /**
     * Configuración visual por columna individual.
     * Permite diferenciar colores y notas entre columnas (rice vs LN vs special).
     */
    public static class ColumnConfig {
        // Imagen de nota (rice)
        public String noteImageRice     = "mania-note1";
        // Imagen de LN head
        public String noteImageLnHead   = "mania-note1H";
        // Imagen de LN body
        public String noteImageLnBody   = "mania-note1L";
        // Imagen de LN tail (null = usar el head al revés, por defecto osu!)
        public String noteImageLnTail   = null;
        // Color de columna (tinte aplicado a la nota rice)
        public Color riceColor          = Color.WHITE;
        // Color separado para LN (si useSeparateLnColor = true en el padre)
        public Color lnColor            = Color.WHITE;
        // ColourLight de esa columna
        public Color lightColor         = new Color(255, 255, 255, 255);
        // Ancho de columna en px
        public int columnWidth          = 64;
        // Grosor de la línea de columna IZQUIERDA (la derecha la define la col siguiente)
        public int columnLineWidth      = 0;

        public ColumnConfig() {}

        public ColumnConfig(ColumnConfig other) {
            this.noteImageRice   = other.noteImageRice;
            this.noteImageLnHead = other.noteImageLnHead;
            this.noteImageLnBody = other.noteImageLnBody;
            this.noteImageLnTail = other.noteImageLnTail;
            this.riceColor       = other.riceColor;
            this.lnColor         = other.lnColor;
            this.lightColor      = other.lightColor;
            this.columnWidth     = other.columnWidth;
            this.columnLineWidth = other.columnLineWidth;
        }
    }

    // -------------------------------------------------------------------------
    // Campos principales del bloque [Mania]
    // -------------------------------------------------------------------------

    /** Número de teclas: 1–18 */
    private int keys;

    /** Si este keymode está activo y se incluirá en la exportación */
    private boolean enabled = true;

    // --- Posicionamiento ---
    private int columnStart     = 250;
    private int hitPosition     = 410;
    private int scorePosition   = 210;
    private int comboPosition   = 210;
    private int lightPosition   = 435;
    private int lightFramePerSecond = 25;
    private int barlineHeight   = 0;

    // --- Comportamiento ---
    private SpecialStyle specialStyle = SpecialStyle.NONE;
    private boolean upsideDown        = false;
    private boolean judgementLine     = true;
    private NoteBodyStyle noteBodyStyle = NoteBodyStyle.STRETCH;
    private boolean noteFlipWhenUpsideDown   = true;
    private boolean noteFlipWhenUpsideDownH  = true;
    private boolean noteFlipWhenUpsideDownT  = true;
    private boolean keysUnderNotes    = false;

    // --- Split stages (10K+) ---
    private boolean splitStages       = false;
    private int stageSeparation       = 40; // px entre stages si splitStages=true
    private boolean separateScore     = true;

    // --- Visuales globales ---
    private Color colourBarline       = new Color(255, 255, 255, 255);
    private Color colourHold          = new Color(255, 255, 255, 255);
    private String fontCombo          = "combo";
    private int widthForNoteHeightScale = 0; // 0 = desactivado

    // --- Imágenes de stage ---
    private String stageHintImage     = "mania-stage-hint";
    private String stageLeftImage     = "mania-stage-left";
    private String stageRightImage    = "mania-stage-right";
    private String stageBottomImage   = null;
    private String warningArrowImage  = null;
    private String lightingNImage     = "lightingN";
    private String lightingLImage     = "lightingL";

    /** Ancho del lighting por columna (null = no especificado) */
    private int[] lightingNWidth      = null;
    private int[] lightingLWidth      = null;

    // --- Percy ---
    private PercyType percyType       = PercyType.NONE;
    private boolean useSeparateLnTail = false; // usar NoteImageXH distinto al head

    // --- Opciones de transparencia global ---
    /** Si true, aplica alpha global a todas las notas de este keymode */
    private boolean useGlobalTransparency = false;
    private int globalAlpha               = 255;

    /** Si true, las LN usan colores distintos a las rice */
    private boolean useSeparateLnColor    = false;

    // --- HD/FI overlay ---
    public enum HdFiMode { NONE, HD, FI, BOTH }
    private HdFiMode hdFiMode = HdFiMode.NONE;

    // --- Configuración por columna ---
    private List<ColumnConfig> columns = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ManiaKeyConfig(int keys) {
        this.keys = keys;
        initDefaultColumns(keys);
    }

    /**
     * Inicializa columnas con valores por defecto según el número de teclas.
     * Aplica un patrón estándar note1/note2/noteS basado en la posición.
     */
    private void initDefaultColumns(int keys) {
        columns.clear();
        for (int i = 0; i < keys; i++) {
            ColumnConfig col = new ColumnConfig();
            col.columnWidth = defaultColumnWidth(keys);
            String noteType = resolveDefaultNoteType(i, keys);
            col.noteImageRice   = "mania-" + noteType;
            col.noteImageLnHead = "mania-" + noteType + "H";
            col.noteImageLnBody = "mania-" + noteType + "L";
            col.lightColor      = defaultLightColor(i, keys);
            col.columnLineWidth = defaultLineWidth(i, keys);
            columns.add(col);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de valores por defecto
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

    /**
     * Determina qué tipo de nota usar por defecto (note1, note2, noteS)
     * según la posición en la columna y el patrón típico de ese keymode.
     */
    private String resolveDefaultNoteType(int col, int keys) {
        // Columna especial (scratch / S) = noteS
        if (isSpecialColumn(col, keys)) return "noteS";
        // Alternancia note1 / note2 por posición
        return (col % 2 == 0) ? "note1" : "note2";
    }

    /**
     * Determina si una columna es "especial" (scratch en BMS/IIDX, centro en 5K/7K).
     */
    public boolean isSpecialColumn(int col, int keys) {
        switch (keys) {
            case 5: return col == 2;           // centro
            case 7: return col == 3;           // centro
            case 8: return col == 7;           // scratch derecha
            case 9: return col == 0 || col == 8; // BMS scratches
            case 10: return false;
            case 12: return col == 0 || col == 6; // 10K2S scratches
            case 14: return col == 0 || col == 13; // DP/EZ2 scratches
            case 16: return col == 0 || col == 15;
            default: return false;
        }
    }

    private Color defaultLightColor(int col, int keys) {
        // Patrón rojo/azul/amarillo estándar
        int pos = col % 4;
        switch (pos) {
            case 0: return new Color(255, 0, 0, 255);
            case 1: return new Color(0, 0, 255, 255);
            case 2: return new Color(255, 212, 0, 255);
            case 3: return new Color(0, 0, 255, 255);
            default: return Color.WHITE;
        }
    }

    private int defaultLineWidth(int col, int keys) {
        // Línea en los bordes exteriores del stage
        if (col == 0 || col == keys - 1) return 2;
        return 0;
    }

    // -------------------------------------------------------------------------
    // Métodos utilitarios
    // -------------------------------------------------------------------------

    /**
     * Recalcula el columnStart para centrar el stage en pantalla (512px de base).
     * Pantalla osu! = 512px de ancho de campo de juego, referencia en 256.
     */
    public void autoCenterStage() {
        int totalWidth = columns.stream().mapToInt(c -> c.columnWidth).sum();
        this.columnStart = 256 - (totalWidth / 2);
        if (this.columnStart < 0) this.columnStart = 0;
    }

    /**
     * Aplica el mismo ancho a todas las columnas.
     */
    public void setUniformColumnWidth(int width) {
        columns.forEach(c -> c.columnWidth = width);
    }

    /**
     * Aplica el mismo color de rice a todas las columnas.
     */
    public void setUniformRiceColor(Color color) {
        columns.forEach(c -> c.riceColor = color);
    }

    /**
     * Aplica el mismo color de LN a todas las columnas.
     */
    public void setUniformLnColor(Color color) {
        columns.forEach(c -> c.lnColor = color);
    }

    /**
     * Devuelve una descripción legible del keymode para la UI.
     */
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
    // Getters y setters
    // -------------------------------------------------------------------------

    public int getKeys() { return keys; }
    public void setKeys(int keys) {
        this.keys = keys;
        // Redimensionar la lista de columnas si cambia el número
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

    public PercyType getPercyType() { return percyType; }
    public void setPercyType(PercyType percyType) { this.percyType = percyType; }

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