package com.osumania.skinbuilder.core;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Genera el contenido de skin.ini a partir de un SkinConfig.
 *
 * Uso:
 *   String iniContent = SkinIniWriter.generate(config);
 *   SkinIniWriter.writeToFile(config, Path.of("ruta/skin.ini"));
 */
public class SkinIniWriter {

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Genera el skin.ini completo como String.
     */
    public static String generate(SkinConfig config) {
        StringWriter sw = new StringWriter();
        PrintWriter pw  = new PrintWriter(sw);

        writeGeneralSection(pw, config);
        writeColoursSection(pw, config);
        writeFontsSection(pw, config);

        for (ManiaKeyConfig mania : config.getEnabledKeymodes()) {
            writeManiaSectionHeader(pw);
            writeManiaSection(pw, mania);
        }

        pw.flush();
        return sw.toString();
    }

    /**
     * Escribe el skin.ini en disco.
     */
    public static void writeToFile(SkinConfig config, Path outputPath) throws IOException {
        String content = generate(config);
        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Sección [General]
    // -------------------------------------------------------------------------

    private static void writeGeneralSection(PrintWriter pw, SkinConfig cfg) {
        section(pw, "General");
        kv(pw, "Name",          cfg.getSkinName());
        kv(pw, "Author",        cfg.getSkinAuthor());
        kv(pw, "Version",       cfg.getSkinVersion());
        blank(pw);
        kvBool(pw, "CursorRotate",  cfg.isCursorRotate());
        kvBool(pw, "CursorExpand",  cfg.isCursorExpand());
        kvBool(pw, "CursorCentre",  cfg.isCursorCentre());
        blank(pw);
    }

    // -------------------------------------------------------------------------
    // Sección [Colours]
    // -------------------------------------------------------------------------

    private static void writeColoursSection(PrintWriter pw, SkinConfig cfg) {
        section(pw, "Colours");
        kvColorOpt(pw, "Combo1",             cfg.getCombo1());
        kvColorOpt(pw, "Combo2",             cfg.getCombo2());
        kvColorOpt(pw, "SliderBorder",       cfg.getSliderBorder());
        kvColorOpt(pw, "SliderTrackOverride",cfg.getSliderTrackOverride());
        kvColorOpt(pw, "MenuGlow",           cfg.getMenuGlow());
        kvColorOpt(pw, "InputOverlayText",   cfg.getInputOverlayText());
        kvColorOpt(pw, "SpinnerBackground",  cfg.getSpinnerBackground());
        blank(pw);
    }

    // -------------------------------------------------------------------------
    // Sección [Fonts]
    // -------------------------------------------------------------------------

    private static void writeFontsSection(PrintWriter pw, SkinConfig cfg) {
        section(pw, "Fonts");
        kv(pw,  "HitCirclePrefix",  cfg.getHitCirclePrefix());
        kvInt(pw, "HitCircleOverlap", cfg.getHitCircleOverlap());
        blank(pw);
        kv(pw,  "ScorePrefix",  cfg.getScorePrefix());
        kvInt(pw, "ScoreOverlap", cfg.getScoreOverlap());
        blank(pw);
        kv(pw,  "ComboPrefix",  cfg.getComboPrefix());
        kvInt(pw, "ComboOverlap", cfg.getComboOverlap());
        blank(pw);
    }

    // -------------------------------------------------------------------------
    // Sección [Mania]
    // -------------------------------------------------------------------------

    private static void writeManiaSectionHeader(PrintWriter pw) {
        pw.println("[Mania]");
    }

    private static void writeManiaSection(PrintWriter pw, ManiaKeyConfig m) {
        int keys = m.getKeys();
        List<ManiaKeyConfig.ColumnConfig> cols = m.getColumns();

        // --- Info básica ---
        kvInt(pw, "Keys", keys);
        comment(pw, "KeyMode: " + m.getDisplayName());
        blank(pw);

        // --- Posicionamiento ---
        kvInt(pw, "ColumnStart",        m.getColumnStart());
        kvInt(pw, "HitPosition",        m.getHitPosition());
        kvInt(pw, "ScorePosition",      m.getScorePosition());
        kvInt(pw, "ComboPosition",      m.getComboPosition());
        kvInt(pw, "LightPosition",      m.getLightPosition());
        kvInt(pw, "LightFramePerSecond", m.getLightFramePerSecond());
        if (m.getBarlineHeight() != 0) {
            kvInt(pw, "BarlineHeight",  m.getBarlineHeight());
        }
        blank(pw);

        // --- Comportamiento ---
        kv(pw,   "SpecialStyle",        specialStyleToString(m.getSpecialStyle()));
        kvBool(pw, "UpsideDown",        m.isUpsideDown());
        kvBool(pw, "JudgementLine",     m.isJudgementLine());
        kvBool(pw, "KeysUnderNotes",    m.isKeysUnderNotes());
        kvInt(pw,  "NoteBodyStyle",     m.getNoteBodyStyle().value);

        // Flip flags (solo si difieren del default o UpsideDown activo)
        if (m.isUpsideDown()) {
            kvBool(pw, "NoteFlipWhenUpsideDown",  m.isNoteFlipWhenUpsideDown());
            kvBool(pw, "NoteFlipWhenUpsideDownH", m.isNoteFlipWhenUpsideDownH());
            kvBool(pw, "NoteFlipWhenUpsideDownT", m.isNoteFlipWhenUpsideDownT());
        }
        blank(pw);

        // --- SplitStages (solo para 10K+) ---
        if (keys >= 10) {
            kvBool(pw, "SplitStages", m.isSplitStages());
            if (m.isSplitStages()) {
                kvInt(pw, "StageSeparation",  m.getStageSeparation());
                kvBool(pw, "SeparateScore",   m.isSeparateScore());
            }
            blank(pw);
        }

        // --- Dimensiones de columnas ---
        comment(pw, "Column layout");
        kv(pw, "ColumnWidth",     intListToString(cols.stream()
                .mapToInt(c -> c.columnWidth)
                .toArray()));
        if (m.getWidthForNoteHeightScale() > 0) {
            kvInt(pw, "WidthForNoteHeightScale", m.getWidthForNoteHeightScale());
        }

        // Espaciado entre columnas (siempre 0 para columnas contiguas)
        kv(pw, "ColumnSpacing", zeroList(keys - 1));

        // Líneas de columna: una entrada por separador (keys+1 valores)
        kv(pw, "ColumnLineWidth", buildColumnLineWidths(cols));
        blank(pw);

        // --- Colores globales ---
        comment(pw, "Colours");
        kv(pw,   "ColourBarline", colorToRgba(m.getColourBarline()));
        kv(pw,   "ColourHold",   colorToRgba(m.getColourHold()));
        blank(pw);

        // --- ColourLight por columna ---
        comment(pw, "Column lights");
        for (int i = 0; i < keys; i++) {
            kv(pw, "ColourLight" + (i + 1), colorToRgba(cols.get(i).lightColor));
        }
        blank(pw);

        // --- Colour (fondo de columna) ---
        // Solo si alguna columna tiene un color distinto a transparent
        comment(pw, "Column background colours (RGBA)");
        for (int i = 0; i < keys; i++) {
            // Por defecto transparente; el usuario lo puede customizar
            pw.println("// Colour" + (i + 1) + ": 0,0,0,0");
        }
        blank(pw);

        // --- FontCombo ---
        kv(pw, "FontCombo", m.getFontCombo());
        blank(pw);

        // --- Stage images ---
        comment(pw, "Stage images");
        kv(pw, "StageLeft",   m.getStageLeftImage());
        kv(pw, "StageRight",  m.getStageRightImage());
        kv(pw, "StageHint",   m.getStageHintImage());
        if (m.getStageBottomImage() != null) {
            kv(pw, "StageBottom", m.getStageBottomImage());
        }
        if (m.getWarningArrowImage() != null) {
            kv(pw, "WarningArrow", m.getWarningArrowImage());
        }
        kv(pw, "LightingN", m.getLightingNImage());
        kv(pw, "LightingL", m.getLightingLImage());

        if (m.getLightingNWidth() != null) {
            kv(pw, "LightingNWidth", intListToString(m.getLightingNWidth()));
        }
        if (m.getLightingLWidth() != null) {
            kv(pw, "LightingLWidth", intListToString(m.getLightingLWidth()));
        }
        blank(pw);

        // --- Key images ---
        comment(pw, "Key images");
        for (int i = 0; i < keys; i++) {
            // Las key images apuntan a las imágenes base (key idle / key pressed)
            // por defecto, la skin reutiliza mania-key1, mania-key2, mania-keyS
            String keyImgName = resolveKeyImageName(i, keys, m, false);
            String keyImgDName = resolveKeyImageName(i, keys, m, true);
            kv(pw, "KeyImage"  + i,  keyImgName);
            kv(pw, "KeyImage"  + i + "D", keyImgDName);
        }
        blank(pw);

        // --- Note images ---
        comment(pw, "Note images");
        for (int i = 0; i < keys; i++) {
            ManiaKeyConfig.ColumnConfig col = cols.get(i);
            kv(pw, "NoteImage" + i,       col.noteImageRice);
            kv(pw, "NoteImage" + i + "H", col.noteImageLnHead);
            kv(pw, "NoteImage" + i + "L", col.noteImageLnBody);
            if (m.isUseSeparateLnTail() && col.noteImageLnTail != null) {
                kv(pw, "NoteImage" + i + "T", col.noteImageLnTail);
            }
        }
        blank(pw);

        // --- HD/FI overlay ---
        if (m.getHdFiMode() != ManiaKeyConfig.HdFiMode.NONE) {
            comment(pw, "HD/FI overlay");
            writeHdFiSection(pw, m);
            blank(pw);
        }

        // Separador visual entre bloques [Mania]
        pw.println();
        pw.println();
    }

    // -------------------------------------------------------------------------
    // HD / FI
    // -------------------------------------------------------------------------

    /**
     * osu!mania no tiene soporte nativo HD/FI en skin.ini.
     * La convención de skinners BMS es añadir un stage-bottom animado
     * que simula el efecto. Aquí lo documentamos como comentario de referencia.
     */
    private static void writeHdFiSection(PrintWriter pw, ManiaKeyConfig m) {
        switch (m.getHdFiMode()) {
            case HD:
                comment(pw, "HD overlay: usar mania-stage-bottom como fading top overlay");
                kv(pw, "StageBottom", "mania-stage-bottom-hd");
                break;
            case FI:
                comment(pw, "FI overlay: usar mania-stage-bottom como fading bottom overlay");
                kv(pw, "StageBottom", "mania-stage-bottom-fi");
                break;
            case BOTH:
                comment(pw, "HD+FI: requiere dos imágenes de overlay combinadas");
                kv(pw, "StageBottom", "mania-stage-bottom-hdfi");
                break;
            default:
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de formato
    // -------------------------------------------------------------------------

    /**
     * Escribe el encabezado de sección: [NombreSección]
     */
    private static void section(PrintWriter pw, String name) {
        pw.println("[" + name + "]");
    }

    /** Clave: valor */
    private static void kv(PrintWriter pw, String key, String value) {
        pw.println(key + ": " + value);
    }

    /** Clave: número entero */
    private static void kvInt(PrintWriter pw, String key, int value) {
        pw.println(key + ": " + value);
    }

    /** Clave: 0 o 1 */
    private static void kvBool(PrintWriter pw, String key, boolean value) {
        pw.println(key + ": " + (value ? 1 : 0));
    }

    /** Clave: color RGBA solo si el array no es null */
    private static void kvColorOpt(PrintWriter pw, String key, int[] rgba) {
        if (rgba != null) {
            pw.println(key + ": " + intListToString(rgba));
        }
    }

    /** Línea en blanco */
    private static void blank(PrintWriter pw) {
        pw.println();
    }

    /** Comentario */
    private static void comment(PrintWriter pw, String text) {
        pw.println("// " + text);
    }

    /**
     * Convierte un Color de Java a "R,G,B,A" (formato osu!).
     */
    public static String colorToRgba(Color c) {
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha();
    }

    /**
     * Convierte un Color de Java a "R,G,B" (sin alpha).
     */
    public static String colorToRgb(Color c) {
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    /**
     * Convierte un array de ints a "a,b,c,d".
     */
    public static String intListToString(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * Genera una cadena de N ceros separados por coma: "0,0,0".
     */
    private static String zeroList(int count) {
        if (count <= 0) return "";
        return IntStream.range(0, count).mapToObj(i -> "0").collect(Collectors.joining(","));
    }

    /**
     * Construye el array de ColumnLineWidth.
     * osu! espera keys+1 valores: el borde izquierdo del stage + un valor por separador + borde derecho.
     */
    private static String buildColumnLineWidths(List<ManiaKeyConfig.ColumnConfig> cols) {
        int[] widths = new int[cols.size() + 1];
        // Primer valor = borde izquierdo (lineWidth de la col 0)
        widths[0] = cols.get(0).columnLineWidth;
        // Valores intermedios = 0 (sin líneas entre columnas por defecto)
        for (int i = 1; i < cols.size(); i++) {
            widths[i] = 0;
        }
        // Último valor = borde derecho (lineWidth de la última col)
        widths[cols.size()] = cols.get(cols.size() - 1).columnLineWidth;
        return intListToString(widths);
    }

    /**
     * Resuelve qué imagen de key usar para una columna.
     * Sigue la convención osu!mania: key1 = tipo 1, key2 = tipo 2, keyS = especial.
     */
    private static String resolveKeyImageName(int col, int keys,
                                              ManiaKeyConfig m, boolean pressed) {
        String suffix = pressed ? "D" : "";
        if (m.isSpecialColumn(col, keys)) {
            return "mania-keyS" + suffix;
        }
        return (col % 2 == 0) ? "mania-key1" + suffix : "mania-key2" + suffix;
    }

    /**
     * Convierte SpecialStyle enum a string para skin.ini.
     */
    private static String specialStyleToString(ManiaKeyConfig.SpecialStyle style) {
        switch (style) {
            case LEFT:  return "1";
            case RIGHT: return "2";
            default:    return "0";
        }
    }
}