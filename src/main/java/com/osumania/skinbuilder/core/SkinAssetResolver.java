package com.osumania.skinbuilder.core;

import com.osumania.skinbuilder.image.ImageTinter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Resolves skin asset names and builds the tinting plan for {@link OskPackager}.
 *
 * <p>Extended in Task 3 to also generate {@link TintOperation} entries for
 * receptor/key images ({@code keyImage} and {@code keyImageDown}) so that the
 * packager can apply them through the same pipeline — including the receptor
 * Y-offset step performed by {@link com.osumania.skinbuilder.image.ReceptorBuilder}.</p>
 */
public final class SkinAssetResolver {

    private static final Logger LOG = Logger.getLogger(SkinAssetResolver.class.getName());

    /** Suffix appended to image names for high-DPI (@2x) variants. */
    public static final String HD_SUFFIX = "@2x";

    /**
     * Marker prefix stored in {@link TintOperation#targetImageName} so that
     * {@link OskPackager} can recognise receptor images and apply the offset.
     */
    public static final String RECEPTOR_TARGET_PREFIX = "_receptor_";

    private SkinAssetResolver() {}

    // =========================================================================
    // Lookup map helpers
    // =========================================================================

    public static Map<String, String> buildLookupMap(Collection<String> entryNames) {
        Map<String, String> map = new HashMap<>(entryNames.size() * 2);
        for (String name : entryNames) {
            // Normalize: replace \ with /, then lowercase
            String normalized = name.replace('\\', '/').toLowerCase(Locale.ROOT);
            map.put(normalized, name);
        }
        return map;
    }

    public static Optional<String> findEntry(String imageName, Map<String, String> lookupMap) {
        // Normalize: replace \ with / in imageName
        String normalized = imageName.replace('\\', '/');
        for (String ext : new String[]{".png", ".jpg"}) {
            String key = (normalized + ext).toLowerCase(Locale.ROOT);
            String real = lookupMap.get(key);
            if (real != null) return Optional.of(real);
        }
        return Optional.empty();
    }

    public static Optional<String> findEntryHd(String imageName, Map<String, String> lookupMap) {
        // Normalize: replace \ with / in imageName
        String normalized = imageName.replace('\\', '/');
        for (String ext : new String[]{".png", ".jpg"}) {
            String key = (normalized + HD_SUFFIX + ext).toLowerCase(Locale.ROOT);
            String real = lookupMap.get(key);
            if (real != null) return Optional.of(real);
        }
        return Optional.empty();
    }

    // =========================================================================
    // Name collectors
    // =========================================================================

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
    // TintOperation model
    // =========================================================================

    /**
     * Describes a single image-processing step: read {@code sourceEntry} from
     * the base .osk, tint it (or copy if neutral), optionally apply Percy, and
     * write as {@code targetEntry}.
     *
     * <p>A {@code targetImageName} that begins with {@link #RECEPTOR_TARGET_PREFIX}
     * signals to {@link OskPackager} that it must additionally apply
     * {@link com.osumania.skinbuilder.image.ReceptorBuilder#applyOffset} on the
     * processed image.</p>
     */
    public static final class TintOperation {
        public final String sourceEntry;
        public final String targetEntry;
        public final String targetImageName;
        public final Color  tintColor;
        public final int    globalAlpha;
        public final boolean applyPercy;
        public final int    percySize;
        public final ManiaKeyConfig.PercyShape percyShape;
        public final boolean highResolution;
        public final boolean isCopyOnly;

        TintOperation(String sourceEntry,
                      String targetEntry,
                      String targetImageName,
                      Color tintColor,
                      int globalAlpha,
                      boolean applyPercy,
                      int percySize,
                      ManiaKeyConfig.PercyShape percyShape,
                      boolean highResolution) {
            this.sourceEntry      = sourceEntry;
            this.targetEntry      = targetEntry;
            this.targetImageName  = targetImageName;
            this.tintColor        = tintColor;
            this.globalAlpha      = globalAlpha;
            this.applyPercy       = applyPercy;
            this.percySize        = Math.max(0, Math.min(400, percySize));
            this.percyShape       = percyShape == null ? ManiaKeyConfig.PercyShape.FLAT : percyShape;
            this.highResolution   = highResolution;
            this.isCopyOnly       = ImageTinter.isNeutralTint(tintColor, globalAlpha)
                    && (!applyPercy || this.percySize == 0);
        }

        /** Returns {@code true} if this operation targets a receptor/key image. */
        public boolean isReceptorImage() {
            return targetImageName != null && targetImageName.startsWith(RECEPTOR_TARGET_PREFIX);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s [tint=%s alpha=%d%s%s%s]",
                    sourceEntry,
                    targetEntry,
                    colorHex(tintColor),
                    globalAlpha,
                    isCopyOnly      ? " COPY"    : "",
                    applyPercy      ? " PERCY"   : "",
                    isReceptorImage() ? " RECEPTOR" : "");
        }

        private static String colorHex(Color c) {
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }
    }

    // =========================================================================
    // buildTintingPlan  (TASK 3 — extended with receptor image operations)
    // =========================================================================

    /**
     * Builds the full list of image-processing operations for a keymode.
     *
     * <p>This includes:</p>
     * <ol>
     *   <li><b>Note images</b> — rice, LN head, LN body, LN tail (per column).</li>
     *   <li><b>Receptor/key images</b> — {@code keyImage} (idle) and
     *       {@code keyImageDown} (pressed) per column.  These are tagged with
     *       {@link #RECEPTOR_TARGET_PREFIX} so {@link OskPackager} can route
     *       them through {@code ReceptorBuilder.applyOffset}.</li>
     * </ol>
     *
     * <p>Key images are always tinted with an opaque white (neutral tint) unless
     * the column's {@code riceColor} is non-white — in that case the same tint is
     * reused, matching the feel of the notes. Adjust this heuristic if needed.</p>
     */
    public static List<TintOperation> buildTintingPlan(ManiaKeyConfig keyConfig,
                                                       Map<String, String> lookupMap) {
        List<TintOperation> ops = new ArrayList<>();
        int keys       = keyConfig.getKeys();
        int globalAlpha = keyConfig.isUseGlobalTransparency()
                ? keyConfig.getGlobalAlpha()
                : 255;

        for (int col = 0; col < keyConfig.getColumns().size(); col++) {
            ManiaKeyConfig.ColumnConfig colCfg = keyConfig.getColumn(col);

            Color riceColor = colCfg.riceColor != null ? colCfg.riceColor : Color.WHITE;
            Color lnColor   = (keyConfig.isUseSeparateLnColor() && colCfg.lnColor != null)
                    ? colCfg.lnColor
                    : riceColor;

            // ---- Note images ----
            String prefix = "mania-note-" + keys + "k-col" + col;

            String newRice = prefix;
            addTintOps(ops, lookupMap, colCfg.noteImageRice, newRice,
                    riceColor, globalAlpha, false, keyConfig);
            colCfg.noteImageRice = newRice;

            String newHead = prefix + "H";
            addTintOps(ops, lookupMap, colCfg.noteImageLnHead, newHead,
                    lnColor, globalAlpha, false, keyConfig);
            colCfg.noteImageLnHead = newHead;

            String newBody = prefix + "L";
            addTintOps(ops, lookupMap, colCfg.noteImageLnBody, newBody,
                    lnColor, globalAlpha, true, keyConfig);
            colCfg.noteImageLnBody = newBody;

            if (keyConfig.isUseSeparateLnTail() && colCfg.noteImageLnTail != null) {
                String newTail = prefix + "T";
                addTintOps(ops, lookupMap, colCfg.noteImageLnTail, newTail,
                        lnColor, globalAlpha, true, keyConfig);
                colCfg.noteImageLnTail = newTail;
            }

            // ---- TASK 3: Receptor / key images ----
            // Target names are prefixed with RECEPTOR_TARGET_PREFIX so OskPackager
            // can identify them and run ReceptorBuilder.applyOffset.
            String receptorPrefix = RECEPTOR_TARGET_PREFIX + "key-" + keys + "k-col" + col;

            String newKeyIdle    = receptorPrefix;
            String newKeyPressed = receptorPrefix + "D";

            addReceptorOps(ops, lookupMap, colCfg.keyImage,     newKeyIdle,    riceColor, globalAlpha, keyConfig);
            addReceptorOps(ops, lookupMap, colCfg.keyImageDown, newKeyPressed, riceColor, globalAlpha, keyConfig);

            // Update the column so the skin.ini writer references the new file names.
            // Strip the internal prefix from the names stored in the column
            // (the prefix is only used as an in-memory routing marker; the actual
            //  file written to the zip drops it).
            colCfg.keyImage     = stripReceptorPrefix(newKeyIdle);
            colCfg.keyImageDown = stripReceptorPrefix(newKeyPressed);
        }

        LOG.info(String.format("[%dK] Tinting plan ready: %d operations for %d columns",
                keyConfig.getKeys(), ops.size(), keyConfig.getColumns().size()));
        return ops;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Removes the internal {@link #RECEPTOR_TARGET_PREFIX} from a target name,
     * producing the file-system-safe name that will be written into the ZIP.
     */
    public static String stripReceptorPrefix(String targetImageName) {
        if (targetImageName != null && targetImageName.startsWith(RECEPTOR_TARGET_PREFIX)) {
            return targetImageName.substring(RECEPTOR_TARGET_PREFIX.length());
        }
        return targetImageName;
    }

    /** Adds SD + HD tint operations for a note image. */
    private static void addTintOps(List<TintOperation> ops,
                                   Map<String, String> lookupMap,
                                   String sourceImageName,
                                   String targetImageName,
                                   Color color,
                                   int alpha,
                                   boolean applyPercy,
                                   ManiaKeyConfig keyConfig) {
        if (sourceImageName == null || sourceImageName.isBlank()) return;

        Optional<String> sd = findEntry(sourceImageName, lookupMap);
        Optional<String> hd = findEntryHd(sourceImageName, lookupMap);

        if (sd.isEmpty() && hd.isEmpty()) {
            LOG.warning("Base image not found in .osk: '" + sourceImageName + "' — skipping");
            return;
        }

        sd.ifPresent(src -> ops.add(new TintOperation(
                src,
                targetImageName + ".png",
                targetImageName,
                color, alpha,
                applyPercy,
                keyConfig.getPercySize(),
                keyConfig.getPercyShape(),
                false)));

        hd.ifPresent(src -> ops.add(new TintOperation(
                src,
                targetImageName + HD_SUFFIX + ".png",
                targetImageName,
                color, alpha,
                applyPercy,
                keyConfig.getPercySize(),
                keyConfig.getPercyShape(),
                true)));
    }

    /**
     * Adds SD + HD tint operations for a receptor/key image.
     *
     * <p>Receptor images are NOT processed through Percy (that's a note-body
     * feature).  They always carry the {@link #RECEPTOR_TARGET_PREFIX} in
     * {@code targetImageName} so {@link OskPackager} routes them through
     * {@code ReceptorBuilder.applyOffset}.</p>
     */
    private static void addReceptorOps(List<TintOperation> ops,
                                       Map<String, String> lookupMap,
                                       String sourceImageName,
                                       String targetImageName,   // already prefixed
                                       Color  color,
                                       int    alpha,
                                       ManiaKeyConfig keyConfig) {
        if (sourceImageName == null || sourceImageName.isBlank()) return;

        Optional<String> sd = findEntry(sourceImageName, lookupMap);
        Optional<String> hd = findEntryHd(sourceImageName, lookupMap);

        if (sd.isEmpty() && hd.isEmpty()) {
            LOG.warning("Receptor base image not found in .osk: '"
                    + sourceImageName + "' — skipping");
            return;
        }

        // The actual file written to the ZIP uses the name WITHOUT the prefix
        String fileTarget = stripReceptorPrefix(targetImageName);

        sd.ifPresent(src -> ops.add(new TintOperation(
                src,
                fileTarget + ".png",
                targetImageName,   // keeps prefix so OskPackager can detect it
                color, alpha,
                false,             // no Percy for receptor images
                0,
                ManiaKeyConfig.PercyShape.FLAT,
                false)));

        hd.ifPresent(src -> ops.add(new TintOperation(
                src,
                fileTarget + HD_SUFFIX + ".png",
                targetImageName,
                color, alpha,
                false,
                0,
                ManiaKeyConfig.PercyShape.FLAT,
                true)));
    }

    private static void addIfNotNull(Set<String> set, String value) {
        if (value != null && !value.isBlank()) set.add(value);
    }
}