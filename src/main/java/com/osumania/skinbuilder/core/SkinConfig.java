package com.osumania.skinbuilder.core;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modelo completo de una skin de osu!.
 * Contiene la sección [General], [Colours], [Fonts] y la lista de [Mania] por keymode.
 */
public class SkinConfig {

    // -------------------------------------------------------------------------
    // [General]
    // -------------------------------------------------------------------------

    private String skinName    = "MySkin";
    private String skinAuthor  = "Author";
    private String skinVersion = "latest";

    // Opciones generales que se escriben en [General]
    private boolean cursorRotate       = false;
    private boolean cursorExpand       = false;
    private boolean cursorCentre       = true;
    private boolean animationFramerate = false; // AnimationFramerate: 0/1

    // -------------------------------------------------------------------------
    // [Colours] — sección estándar (no mania)
    // -------------------------------------------------------------------------

    // null = no incluir esa línea en el output
    private int[] combo1  = null;
    private int[] combo2  = null;
    private int[] sliderBorder = null;
    private int[] sliderTrackOverride = null;
    private int[] menuGlow = null;
    private int[] inputOverlayText = null;
    private int[] spinnerBackground = null;

    // -------------------------------------------------------------------------
    // [Fonts]
    // -------------------------------------------------------------------------

    private String hitCirclePrefix  = "default";
    private int    hitCircleOverlap = 19;
    private String scorePrefix      = "score";
    private int    scoreOverlap     = 3;
    private String comboPrefix      = "combo";
    private int    comboOverlap     = 2;

    // -------------------------------------------------------------------------
    // Keymodes
    // -------------------------------------------------------------------------

    private List<ManiaKeyConfig> keymodes = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructor — crea una skin vacía con valores por defecto sensatos
    // -------------------------------------------------------------------------

    public SkinConfig() {}

    public SkinConfig(String name, String author) {
        this.skinName   = name;
        this.skinAuthor = author;
    }

    // -------------------------------------------------------------------------
    // Gestión de keymodes
    // -------------------------------------------------------------------------

    /**
     * Añade un keymode si no existe ya uno con ese número de teclas.
     */
    public ManiaKeyConfig addKeymode(int keys) {
        if (getKeymode(keys).isPresent()) {
            throw new IllegalArgumentException("Ya existe un keymode para " + keys + "K");
        }
        ManiaKeyConfig cfg = new ManiaKeyConfig(keys);
        keymodes.add(cfg);
        keymodes.sort((a, b) -> Integer.compare(a.getKeys(), b.getKeys()));
        return cfg;
    }

    /**
     * Elimina el keymode con ese número de teclas.
     */
    public void removeKeymode(int keys) {
        keymodes.removeIf(k -> k.getKeys() == keys);
    }

    /**
     * Devuelve el keymode con ese número de teclas, si existe.
     */
    public Optional<ManiaKeyConfig> getKeymode(int keys) {
        return keymodes.stream().filter(k -> k.getKeys() == keys).findFirst();
    }

    /**
     * Devuelve o crea el keymode con ese número de teclas.
     */
    public ManiaKeyConfig getOrCreateKeymode(int keys) {
        return getKeymode(keys).orElseGet(() -> addKeymode(keys));
    }

    /**
     * Lista solo los keymodes activos (enabled = true).
     */
    public List<ManiaKeyConfig> getEnabledKeymodes() {
        List<ManiaKeyConfig> result = new ArrayList<>();
        for (ManiaKeyConfig k : keymodes) {
            if (k.isEnabled()) result.add(k);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getSkinName() { return skinName; }
    public void setSkinName(String skinName) { this.skinName = skinName; }

    public String getSkinAuthor() { return skinAuthor; }
    public void setSkinAuthor(String skinAuthor) { this.skinAuthor = skinAuthor; }

    public String getSkinVersion() { return skinVersion; }
    public void setSkinVersion(String skinVersion) { this.skinVersion = skinVersion; }

    public boolean isCursorRotate() { return cursorRotate; }
    public void setCursorRotate(boolean cursorRotate) { this.cursorRotate = cursorRotate; }

    public boolean isCursorExpand() { return cursorExpand; }
    public void setCursorExpand(boolean cursorExpand) { this.cursorExpand = cursorExpand; }

    public boolean isCursorCentre() { return cursorCentre; }
    public void setCursorCentre(boolean cursorCentre) { this.cursorCentre = cursorCentre; }

    public int[] getCombo1() { return combo1; }
    public void setCombo1(int[] combo1) { this.combo1 = combo1; }

    public int[] getCombo2() { return combo2; }
    public void setCombo2(int[] combo2) { this.combo2 = combo2; }

    public int[] getSliderBorder() { return sliderBorder; }
    public void setSliderBorder(int[] sliderBorder) { this.sliderBorder = sliderBorder; }

    public int[] getSliderTrackOverride() { return sliderTrackOverride; }
    public void setSliderTrackOverride(int[] v) { this.sliderTrackOverride = v; }

    public int[] getMenuGlow() { return menuGlow; }
    public void setMenuGlow(int[] menuGlow) { this.menuGlow = menuGlow; }

    public int[] getInputOverlayText() { return inputOverlayText; }
    public void setInputOverlayText(int[] v) { this.inputOverlayText = v; }

    public int[] getSpinnerBackground() { return spinnerBackground; }
    public void setSpinnerBackground(int[] v) { this.spinnerBackground = v; }

    public String getHitCirclePrefix() { return hitCirclePrefix; }
    public void setHitCirclePrefix(String v) { this.hitCirclePrefix = v; }

    public int getHitCircleOverlap() { return hitCircleOverlap; }
    public void setHitCircleOverlap(int v) { this.hitCircleOverlap = v; }

    public String getScorePrefix() { return scorePrefix; }
    public void setScorePrefix(String v) { this.scorePrefix = v; }

    public int getScoreOverlap() { return scoreOverlap; }
    public void setScoreOverlap(int v) { this.scoreOverlap = v; }

    public String getComboPrefix() { return comboPrefix; }
    public void setComboPrefix(String v) { this.comboPrefix = v; }

    public int getComboOverlap() { return comboOverlap; }
    public void setComboOverlap(int v) { this.comboOverlap = v; }

    public List<ManiaKeyConfig> getKeymodes() { return keymodes; }
    public void setKeymodes(List<ManiaKeyConfig> keymodes) { this.keymodes = keymodes; }
}