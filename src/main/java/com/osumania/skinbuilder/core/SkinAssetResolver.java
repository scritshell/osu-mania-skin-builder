package com.osumania.skinbuilder.core;

import com.osumania.skinbuilder.image.ImageTinter;

import java.awt.Color;
import java.util.*;
import java.util.logging.Logger;

/**
 * Resuelve los nombres de archivo reales dentro de un .osk para los nombres de imagen
 * referenciados en el skin.ini, y calcula los "planes de tintado" para keymodes nuevos.
 *
 * <h2>Convención de nombres de osu!</h2>
 * El skin.ini referencia imágenes <em>sin extensión</em>:
 * <pre>NoteImage0: mania-note1</pre>
 * osu! carga automáticamente:
 * <ul>
 *   <li>{@code mania-note1.png}     — resolución estándar (SD)</li>
 *   <li>{@code mania-note1@2x.png}  — alta resolución (HD), si existe</li>
 * </ul>
 *
 * <h2>Cómo funciona el plan de tintado</h2>
 * Al generar un keymode nuevo (p.e. 7K) a partir de una skin 4K:
 * <ol>
 *   <li>Se busca la imagen base en el .osk (p.e. {@code mania-note1.png}).</li>
 *   <li>Se crea una {@link TintOperation} que describe cómo tintar esa imagen.</li>
 *   <li>Los nombres de destino siguen el patrón {@code mania-note-7k-col0.png}.</li>
 *   <li>Los campos {@code noteImageXxx} de cada {@link ManiaKeyConfig.ColumnConfig}
 *       se actualizan <em>in-place</em> para que {@link SkinIniWriter} genere el
 *       skin.ini correcto apuntando a los nuevos archivos.</li>
 * </ol>
 */
public final class SkinAssetResolver {

    private static final Logger LOG = Logger.getLogger(SkinAssetResolver.class.getName());

    /** Sufijo de alta resolución que osu! detecta automáticamente. */
    public static final String HD_SUFFIX = "@2x";

    private SkinAssetResolver() {}

    // =========================================================================
    // Resolución de entradas en el ZIP
    // =========================================================================

    /**
     * Construye un mapa {@code nombre_lowercase → nombre_real} de todas las
     * entradas de un ZIP, para búsquedas case-insensitive.
     *
     * <p>Ejemplo: {@code "mania-note1.png"} → {@code "Mania-Note1.PNG"}
     * si el ZIP guardó el archivo con esa capitalización.</p>
     *
     * @param entryNames Nombres tal como los devuelve {@link java.util.zip.ZipFile#entries()}
     * @return           Mapa listo para usar en {@link #findEntry} y {@link #findEntryHd}
     */
    public static Map<String, String> buildLookupMap(Collection<String> entryNames) {
        Map<String, String> map = new HashMap<>(entryNames.size() * 2);
        for (String name : entryNames) {
            map.put(name.toLowerCase(Locale.ROOT), name);
        }
        return map;
    }

    /**
     * Dado un nombre de imagen sin extensión, devuelve el nombre real del archivo
     * SD (estándar) si existe en el ZIP.
     *
     * <p>Búsqueda en orden de prioridad: {@code .png} → {@code .jpg}</p>
     *
     * @param imageName  Nombre sin extensión, p.e. {@code "mania-note1"}
     * @param lookupMap  Mapa generado por {@link #buildLookupMap}
     * @return           Nombre real dentro del ZIP, o {@link Optional#empty()} si no existe
     */
    public static Optional<String> findEntry(String imageName, Map<String, String> lookupMap) {
        for (String ext : new String[] { ".png", ".jpg" }) {
            String real = lookupMap.get((imageName + ext).toLowerCase(Locale.ROOT));
            if (real != null) return Optional.of(real);
        }
        return Optional.empty();
    }

    /**
     * Dado un nombre de imagen sin extensión, devuelve el nombre real del archivo
     * HD ({@code @2x}) si existe en el ZIP.
     *
     * @param imageName  Nombre sin extensión, p.e. {@code "mania-note1"}
     * @param lookupMap  Mapa generado por {@link #buildLookupMap}
     * @return           Nombre real del archivo HD, o {@link Optional#empty()}
     */
    public static Optional<String> findEntryHd(String imageName, Map<String, String> lookupMap) {
        for (String ext : new String[] { ".png", ".jpg" }) {
            String real = lookupMap.get((imageName + HD_SUFFIX + ext).toLowerCase(Locale.ROOT));
            if (real != null) return Optional.of(real);
        }
        return Optional.empty();
    }

    /**
     * Recoge todos los nombres de imagen únicos de notas referenciados por las
     * columnas del keymode (rice, LN head, LN body, LN tail).
     *
     * @param keyConfig  Keymode a analizar
     * @return           Conjunto ordenado de nombres (sin extensión); no modificable
     */
    public static Set<String> collectNoteImageNames(ManiaKeyConfig keyConfig) {
        Set<String> names = new LinkedHashSet<>();
        for (ManiaKeyConfig.ColumnConfig col : keyConfig.getColumns()) {
            addIfNotNull(names, col.noteImageRice);
            addIfNotNull(names, col.noteImageLnHead);
            addIfNotNull(names, col.noteImageLnBody);
            addIfNotNull(names, col.noteImageLnTail);
        }
        return Collections.unmodifiableSet(names);
    }

    /**
     * Recoge todos los nombres de imagen únicos de stage (hint, left, right,
     * bottom, lightingN, lightingL, warningArrow) de un keymode.
     *
     * @param keyConfig  Keymode a analizar
     * @return           Conjunto ordenado de nombres (sin extensión); no modificable
     */
    public static Set<String> collectStageImageNames(ManiaKeyConfig keyConfig) {
        Set<String> names = new LinkedHashSet<>();
        addIfNotNull(names, keyConfig.getStageHintImage());
        addIfNotNull(names, keyConfig.getStageLeftImage());
        addIfNotNull(names, keyConfig.getStageRightImage());
        addIfNotNull(names, keyConfig.getStageBottomImage());
        addIfNotNull(names, keyConfig.getWarningArrowImage());
        addIfNotNull(names, keyConfig.getLightingNImage());
        addIfNotNull(names, keyConfig.getLightingLImage());
        return Collections.unmodifiableSet(names);
    }

    // =========================================================================
    // Plan de tintado
    // =========================================================================

    /**
     * Representa una operación atómica de tintado: leer una imagen del .osk de
     * entrada y generar una imagen de destino en el .osk de salida.
     */
    public static final class TintOperation {

        /**
         * Nombre de la entrada fuente en el ZIP (con extensión),
         * p.e. {@code "mania-note1.png"} o {@code "mania-note1@2x.png"}.
         */
        public final String sourceEntry;

        /**
         * Nombre de la entrada de destino en el ZIP de salida (con extensión),
         * p.e. {@code "mania-note-7k-col0.png"}.
         */
        public final String targetEntry;

        /**
         * Nombre de imagen de destino <em>sin extensión</em> (para skin.ini),
         * p.e. {@code "mania-note-7k-col0"}.
         */
        public final String targetImageName;

        /** Color de tinte a aplicar (multiplicación RGB). */
        public final Color tintColor;

        /** Multiplicador de alpha global [0–255]. */
        public final int globalAlpha;

        /**
         * Si {@code true}, el tinte es neutro (blanco + alpha 255) y la imagen puede
         * copiarse directamente sin procesar píxeles, ahorrando tiempo de CPU.
         */
        public final boolean isCopyOnly;

        TintOperation(String sourceEntry, String targetEntry, String targetImageName,
                      Color tintColor, int globalAlpha) {
            this.sourceEntry     = sourceEntry;
            this.targetEntry     = targetEntry;
            this.targetImageName = targetImageName;
            this.tintColor       = tintColor;
            this.globalAlpha     = globalAlpha;
            this.isCopyOnly      = ImageTinter.isNeutralTint(tintColor, globalAlpha);
        }

        @Override
        public String toString() {
            return String.format("%s → %s [tint=%s alpha=%d%s]",
                    sourceEntry, targetEntry,
                    colorHex(tintColor), globalAlpha,
                    isCopyOnly ? " COPY" : "");
        }

        private static String colorHex(Color c) {
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }
    }

    /**
     * Calcula el plan de tintado completo para un keymode nuevo.
     *
     * <p>Para cada columna genera operaciones para rice, LN head, LN body y (si aplica)
     * LN tail, tanto en resolución SD como HD. Los campos {@code noteImageXxx} de cada
     * {@link ManiaKeyConfig.ColumnConfig} se actualizan <em>in-place</em> con los nuevos
     * nombres, de modo que al llamar a {@link SkinIniWriter#generate} el skin.ini ya
     * contendrá las referencias correctas.</p>
     *
     * <h2>Convención de nombres generados</h2>
     * <ul>
     *   <li>Rice de columna 0 del 7K  →  {@code mania-note-7k-col0}</li>
     *   <li>LN head de columna 0      →  {@code mania-note-7k-col0H}</li>
     *   <li>LN body de columna 0      →  {@code mania-note-7k-col0L}</li>
     *   <li>LN tail de columna 0      →  {@code mania-note-7k-col0T}</li>
     * </ul>
     *
     * @param keyConfig  Keymode a procesar. Sus {@link ManiaKeyConfig.ColumnConfig} se
     *                   modifican in-place.
     * @param lookupMap  Entradas disponibles en el .osk de entrada (de
     *                   {@link #buildLookupMap})
     * @return           Lista de {@link TintOperation} lista para ejecutar en
     *                   {@link com.osumania.skinbuilder.core.OskPackager}
     */
    public static List<TintOperation> buildTintingPlan(ManiaKeyConfig keyConfig,
                                                       Map<String, String> lookupMap) {
        List<TintOperation> ops = new ArrayList<>();
        int keys        = keyConfig.getKeys();
        int globalAlpha = keyConfig.isUseGlobalTransparency()
                ? keyConfig.getGlobalAlpha()
                : 255;

        for (int col = 0; col < keyConfig.getColumns().size(); col++) {
            ManiaKeyConfig.ColumnConfig colCfg = keyConfig.getColumn(col);

            // Colores a aplicar (fallback a blanco si no se configuró nada)
            Color riceColor = colCfg.riceColor != null ? colCfg.riceColor : Color.WHITE;
            Color lnColor   = (keyConfig.isUseSeparateLnColor() && colCfg.lnColor != null)
                    ? colCfg.lnColor
                    : riceColor;

            // Prefijo de los nombres generados, p.e. "mania-note-7k-col0"
            String prefix = "mania-note-" + keys + "k-col" + col;

            // --- Rice ---
            String newRice = prefix;
            addTintOps(ops, lookupMap, colCfg.noteImageRice, newRice, riceColor, globalAlpha);
            colCfg.noteImageRice = newRice;

            // --- LN Head ---
            String newHead = prefix + "H";
            addTintOps(ops, lookupMap, colCfg.noteImageLnHead, newHead, lnColor, globalAlpha);
            colCfg.noteImageLnHead = newHead;

            // --- LN Body ---
            String newBody = prefix + "L";
            addTintOps(ops, lookupMap, colCfg.noteImageLnBody, newBody, lnColor, globalAlpha);
            colCfg.noteImageLnBody = newBody;

            // --- LN Tail (solo si useSeparateLnTail y tiene imagen propia) ---
            if (keyConfig.isUseSeparateLnTail() && colCfg.noteImageLnTail != null) {
                String newTail = prefix + "T";
                addTintOps(ops, lookupMap, colCfg.noteImageLnTail, newTail, lnColor, globalAlpha);
                colCfg.noteImageLnTail = newTail;
            }
        }

        LOG.info(String.format("[%dK] Plan de tintado listo: %d operaciones para %d columnas",
                keyConfig.getKeys(), ops.size(), keyConfig.getColumns().size()));
        return ops;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Añade una operación SD y (si existe) una operación HD para una imagen dada.
     * Las imágenes de destino se generan siempre como PNG, independientemente del
     * formato fuente (PNG/JPG), porque PNG soporta canal alpha completo.
     *
     * @param ops             Lista donde añadir las operaciones
     * @param lookupMap       Entradas del .osk de entrada
     * @param sourceImageName Nombre de imagen fuente sin extensión
     * @param targetImageName Nombre de imagen destino sin extensión
     * @param color           Color de tinte
     * @param alpha           Multiplicador de alpha [0–255]
     */
    private static void addTintOps(List<TintOperation> ops,
                                   Map<String, String> lookupMap,
                                   String sourceImageName,
                                   String targetImageName,
                                   Color color,
                                   int alpha) {
        if (sourceImageName == null || sourceImageName.isBlank()) return;

        Optional<String> sd = findEntry(sourceImageName, lookupMap);
        Optional<String> hd = findEntryHd(sourceImageName, lookupMap);

        if (sd.isEmpty() && hd.isEmpty()) {
            LOG.warning("Imagen base no encontrada en el .osk: '" + sourceImageName
                    + "' (se omitirá)");
            return;
        }

        // SD → destino siempre .png
        sd.ifPresent(src -> ops.add(
                new TintOperation(src, targetImageName + ".png", targetImageName, color, alpha)
        ));

        // HD → destino siempre @2x.png
        hd.ifPresent(src -> ops.add(
                new TintOperation(src, targetImageName + "@2x.png", targetImageName, color, alpha)
        ));
    }

    private static void addIfNotNull(Set<String> set, String value) {
        if (value != null && !value.isBlank()) set.add(value);
    }
}