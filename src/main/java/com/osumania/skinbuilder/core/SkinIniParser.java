package com.osumania.skinbuilder.core;
import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Lee un skin.ini (o extrae el skin.ini de un .osk) y construye un SkinConfig.
 *
 * Uso:
 *   SkinConfig cfg = SkinIniParser.parseFile(Path.of("skin.ini"));
 *   SkinConfig cfg = SkinIniParser.parseOsk(Path.of("myskin.osk"));
 */
public class SkinIniParser {

    private static final Logger LOG = Logger.getLogger(SkinIniParser.class.getName());

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Parsea un skin.ini desde disco.
     */
    public static SkinConfig parseFile(Path skinIniPath) throws IOException {
        String content = Files.readString(skinIniPath, StandardCharsets.UTF_8);
        return parse(content);
    }

    /**
     * Extrae skin.ini de un .osk y lo parsea.
     */
    public static SkinConfig parseOsk(Path oskPath) throws IOException {
        try (ZipFile zip = new ZipFile(oskPath.toFile())) {
            ZipEntry entry = zip.getEntry("skin.ini");
            if (entry == null) {
                // Buscar case-insensitive
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.getName().equalsIgnoreCase("skin.ini")) {
                        entry = e;
                        break;
                    }
                }
            }
            if (entry == null) {
                throw new IOException("El archivo .osk no contiene un skin.ini");
            }
            String content = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            return parse(content);
        }
    }

    /**
     * Parsea el contenido de un skin.ini como String.
     */
    public static SkinConfig parse(String content) {
        SkinConfig config = new SkinConfig();
        List<String> lines = Arrays.asList(content.split("\\r?\\n"));

        String currentSection = null;
        ManiaKeyConfig currentMania = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // Ignorar comentarios y líneas vacías
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                continue;
            }

            // Detectar sección
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).trim();
                if ("Mania".equalsIgnoreCase(currentSection)) {
                    // Nuevo bloque [Mania] — se crea cuando leamos Keys:
                    currentMania = null;
                }
                continue;
            }

            // Parsear clave: valor
            int colonIdx = line.indexOf(':');
            if (colonIdx < 0) continue;

            String key   = line.substring(0, colonIdx).trim();
            String value = line.substring(colonIdx + 1).trim();

            if (currentSection == null) continue;

            switch (currentSection) {
                case "General":
                    parseGeneral(config, key, value);
                    break;
                case "Colours":
                    parseColours(config, key, value);
                    break;
                case "Fonts":
                    parseFonts(config, key, value);
                    break;
                case "Mania":
                    // El primer campo que leemos de un bloque [Mania] debería ser Keys
                    if ("Keys".equalsIgnoreCase(key) && currentMania == null) {
                        int keys = parseInt(value, -1);
                        if (keys > 0) {
                            currentMania = config.getOrCreateKeymode(keys);
                        }
                    } else if (currentMania != null) {
                        parseMania(currentMania, key, value);
                    } else {
                        LOG.warning("Línea [Mania] antes de Keys: " + rawLine);
                    }
                    break;
                default:
                    break;
            }
        }

        return config;
    }

    // -------------------------------------------------------------------------
    // Parseo de secciones
    // -------------------------------------------------------------------------

    private static void parseGeneral(SkinConfig cfg, String key, String value) {
        switch (key) {
            case "Name":    cfg.setSkinName(value);   break;
            case "Author":  cfg.setSkinAuthor(value); break;
            case "Version": cfg.setSkinVersion(value); break;
            case "CursorRotate":  cfg.setCursorRotate(parseBool(value));  break;
            case "CursorExpand":  cfg.setCursorExpand(parseBool(value));  break;
            case "CursorCentre":  cfg.setCursorCentre(parseBool(value));  break;
        }
    }

    private static void parseColours(SkinConfig cfg, String key, String value) {
        int[] rgba = parseColorArray(value);
        switch (key) {
            case "Combo1":             cfg.setCombo1(rgba);             break;
            case "Combo2":             cfg.setCombo2(rgba);             break;
            case "SliderBorder":       cfg.setSliderBorder(rgba);       break;
            case "SliderTrackOverride":cfg.setSliderTrackOverride(rgba); break;
            case "MenuGlow":           cfg.setMenuGlow(rgba);           break;
            case "InputOverlayText":   cfg.setInputOverlayText(rgba);   break;
            case "SpinnerBackground":  cfg.setSpinnerBackground(rgba);  break;
        }
    }

    private static void parseFonts(SkinConfig cfg, String key, String value) {
        switch (key) {
            case "HitCirclePrefix":  cfg.setHitCirclePrefix(value);             break;
            case "HitCircleOverlap": cfg.setHitCircleOverlap(parseInt(value,19)); break;
            case "ScorePrefix":      cfg.setScorePrefix(value);                 break;
            case "ScoreOverlap":     cfg.setScoreOverlap(parseInt(value, 3));   break;
            case "ComboPrefix":      cfg.setComboPrefix(value);                 break;
            case "ComboOverlap":     cfg.setComboOverlap(parseInt(value, 2));   break;
        }
    }

    private static void parseMania(ManiaKeyConfig m, String key, String value) {
        List<ManiaKeyConfig.ColumnConfig> cols = m.getColumns();
        int keys = m.getKeys();

        switch (key) {
            // --- Posicionamiento ---
            case "ColumnStart":         m.setColumnStart(parseInt(value, m.getColumnStart()));       break;
            case "HitPosition":         m.setHitPosition(parseInt(value, m.getHitPosition()));       break;
            case "ScorePosition":       m.setScorePosition(parseInt(value, m.getScorePosition()));   break;
            case "ComboPosition":       m.setComboPosition(parseInt(value, m.getComboPosition()));   break;
            case "LightPosition":       m.setLightPosition(parseInt(value, m.getLightPosition()));   break;
            case "LightFramePerSecond": m.setLightFramePerSecond(parseInt(value, 25));               break;
            case "BarlineHeight":       m.setBarlineHeight(parseInt(value, 0));                      break;

            // --- Comportamiento ---
            case "SpecialStyle":
                if ("1".equals(value)) m.setSpecialStyle(ManiaKeyConfig.SpecialStyle.LEFT);
                else if ("2".equals(value)) m.setSpecialStyle(ManiaKeyConfig.SpecialStyle.RIGHT);
                else m.setSpecialStyle(ManiaKeyConfig.SpecialStyle.NONE);
                break;
            case "UpsideDown":               m.setUpsideDown(parseBool(value));              break;
            case "JudgementLine":            m.setJudgementLine(parseBool(value));           break;
            case "KeysUnderNotes":           m.setKeysUnderNotes(parseBool(value));          break;
            case "NoteBodyStyle":
                int nbs = parseInt(value, 0);
                for (ManiaKeyConfig.NoteBodyStyle s : ManiaKeyConfig.NoteBodyStyle.values()) {
                    if (s.value == nbs) { m.setNoteBodyStyle(s); break; }
                }
                break;
            case "NoteFlipWhenUpsideDown":   m.setNoteFlipWhenUpsideDown(parseBool(value));  break;
            case "NoteFlipWhenUpsideDownH":  m.setNoteFlipWhenUpsideDownH(parseBool(value)); break;
            case "NoteFlipWhenUpsideDownT":  m.setNoteFlipWhenUpsideDownT(parseBool(value)); break;

            // --- Split stages ---
            case "SplitStages":    m.setSplitStages(parseBool(value));              break;
            case "StageSeparation":m.setStageSeparation(parseInt(value, 40));       break;
            case "SeparateScore":  m.setSeparateScore(parseBool(value));            break;

            // --- Dimensiones ---
            case "ColumnWidth": {
                int[] widths = parseIntArray(value);
                for (int i = 0; i < Math.min(widths.length, cols.size()); i++) {
                    cols.get(i).columnWidth = widths[i];
                }
                break;
            }
            case "WidthForNoteHeightScale":
                m.setWidthForNoteHeightScale(parseInt(value, 0));
                break;
            case "ColumnLineWidth": {
                // osu! usa keys+1 valores; el [0] y [keys] son los bordes del stage
                int[] lineWidths = parseIntArray(value);
                if (lineWidths.length >= 1) cols.get(0).columnLineWidth = lineWidths[0];
                if (lineWidths.length >= keys + 1) {
                    cols.get(keys - 1).columnLineWidth = lineWidths[keys];
                }
                break;
            }

            // --- Colores globales ---
            case "ColourBarline": m.setColourBarline(parseColor(value, m.getColourBarline())); break;
            case "ColourHold":    m.setColourHold(parseColor(value, m.getColourHold()));       break;
            case "FontCombo":     m.setFontCombo(value);                                       break;

            // --- ColourLight por columna ---
            default:
                if (key.startsWith("ColourLight")) {
                    String idx = key.substring("ColourLight".length());
                    int colIdx = parseInt(idx, -1) - 1;
                    if (colIdx >= 0 && colIdx < cols.size()) {
                        cols.get(colIdx).lightColor = parseColor(value, cols.get(colIdx).lightColor);
                    }
                }
                // --- NoteImage ---
                else if (key.startsWith("NoteImage")) {
                    parseNoteImage(m, key, value);
                }
                // --- KeyImage (solo guardamos referencia, no modificamos) ---
                else if (key.startsWith("KeyImage")) {
                    // Los KeyImage se manejan separadamente al exportar
                    // pero podríamos almacenarlos si fuera necesario
                }
                // --- Stage images ---
                else if (key.equals("StageHint"))    m.setStageHintImage(value);
                else if (key.equals("StageLeft"))    m.setStageLeftImage(value);
                else if (key.equals("StageRight"))   m.setStageRightImage(value);
                else if (key.equals("StageBottom"))  m.setStageBottomImage(value);
                else if (key.equals("WarningArrow")) m.setWarningArrowImage(value);
                else if (key.equals("LightingN"))    m.setLightingNImage(value);
                else if (key.equals("LightingL"))    m.setLightingLImage(value);
                else if (key.equals("LightingNWidth")) m.setLightingNWidth(parseIntArray(value));
                else if (key.equals("LightingLWidth")) m.setLightingLWidth(parseIntArray(value));
                break;
        }
    }

    /**
     * Parsea las líneas NoteImageXY del bloque [Mania].
     * Formatos soportados: NoteImage0, NoteImage0H, NoteImage0L, NoteImage0T
     */
    private static void parseNoteImage(ManiaKeyConfig m, String key, String value) {
        // Quitar prefijo "NoteImage"
        String rest = key.substring("NoteImage".length());

        // Detectar sufijo H / L / T
        char suffix = 0;
        String numStr = rest;
        if (rest.endsWith("H")) { suffix = 'H'; numStr = rest.substring(0, rest.length() - 1); }
        else if (rest.endsWith("L")) { suffix = 'L'; numStr = rest.substring(0, rest.length() - 1); }
        else if (rest.endsWith("T")) { suffix = 'T'; numStr = rest.substring(0, rest.length() - 1); }

        int colIdx = parseInt(numStr, -1);
        if (colIdx < 0 || colIdx >= m.getColumns().size()) return;

        ManiaKeyConfig.ColumnConfig col = m.getColumn(colIdx);
        switch (suffix) {
            case 'H': col.noteImageLnHead = value; break;
            case 'L': col.noteImageLnBody = value; break;
            case 'T': col.noteImageLnTail = value; m.setUseSeparateLnTail(true); break;
            default:  col.noteImageRice   = value; break;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de parseo
    // -------------------------------------------------------------------------

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static boolean parseBool(String s) {
        return "1".equals(s.trim());
    }

    /**
     * Parsea "R,G,B" o "R,G,B,A" a Color de Java.
     */
    static Color parseColor(String s, Color fallback) {
        try {
            int[] parts = parseIntArray(s);
            if (parts.length == 3) return new Color(parts[0], parts[1], parts[2]);
            if (parts.length == 4) return new Color(parts[0], parts[1], parts[2], parts[3]);
        } catch (Exception e) {
            LOG.warning("No se pudo parsear color: " + s);
        }
        return fallback;
    }

    /**
     * Parsea "R,G,B" o "R,G,B,A" a int[].
     */
    static int[] parseColorArray(String s) {
        return parseIntArray(s);
    }

    /**
     * Parsea "a,b,c,d" a int[].
     */
    static int[] parseIntArray(String s) {
        String[] parts = s.trim().split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseInt(parts[i].trim(), 0);
        }
        return result;
    }
}