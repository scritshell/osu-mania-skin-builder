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

public final class SkinAssetResolver {

    private static final Logger LOG = Logger.getLogger(SkinAssetResolver.class.getName());
    public static final String HD_SUFFIX = "@2x";

    private SkinAssetResolver() {
    }

    public static Map<String, String> buildLookupMap(Collection<String> entryNames) {
        Map<String, String> map = new HashMap<>(entryNames.size() * 2);
        for (String name : entryNames) {
            map.put(name.toLowerCase(Locale.ROOT), name);
        }
        return map;
    }

    public static Optional<String> findEntry(String imageName, Map<String, String> lookupMap) {
        for (String ext : new String[] { ".png", ".jpg" }) {
            String real = lookupMap.get((imageName + ext).toLowerCase(Locale.ROOT));
            if (real != null) {
                return Optional.of(real);
            }
        }
        return Optional.empty();
    }

    public static Optional<String> findEntryHd(String imageName, Map<String, String> lookupMap) {
        for (String ext : new String[] { ".png", ".jpg" }) {
            String real = lookupMap.get((imageName + HD_SUFFIX + ext).toLowerCase(Locale.ROOT));
            if (real != null) {
                return Optional.of(real);
            }
        }
        return Optional.empty();
    }

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

    public static final class TintOperation {
        public final String sourceEntry;
        public final String targetEntry;
        public final String targetImageName;
        public final Color tintColor;
        public final int globalAlpha;
        public final boolean applyPercy;
        public final int percySize;
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
            this.sourceEntry = sourceEntry;
            this.targetEntry = targetEntry;
            this.targetImageName = targetImageName;
            this.tintColor = tintColor;
            this.globalAlpha = globalAlpha;
            this.applyPercy = applyPercy;
            this.percySize = Math.max(0, Math.min(400, percySize));
            this.percyShape = percyShape == null ? ManiaKeyConfig.PercyShape.FLAT : percyShape;
            this.highResolution = highResolution;
            this.isCopyOnly = ImageTinter.isNeutralTint(tintColor, globalAlpha)
                    && (!applyPercy || this.percySize == 0);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s [tint=%s alpha=%d%s%s]",
                    sourceEntry,
                    targetEntry,
                    colorHex(tintColor),
                    globalAlpha,
                    isCopyOnly ? " COPY" : "",
                    applyPercy ? " PERCY" : "");
        }

        private static String colorHex(Color c) {
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }
    }

    public static List<TintOperation> buildTintingPlan(ManiaKeyConfig keyConfig,
                                                       Map<String, String> lookupMap) {
        List<TintOperation> ops = new ArrayList<>();
        int keys = keyConfig.getKeys();
        int globalAlpha = keyConfig.isUseGlobalTransparency()
                ? keyConfig.getGlobalAlpha()
                : 255;

        for (int col = 0; col < keyConfig.getColumns().size(); col++) {
            ManiaKeyConfig.ColumnConfig colCfg = keyConfig.getColumn(col);
            Color riceColor = colCfg.riceColor != null ? colCfg.riceColor : Color.WHITE;
            Color lnColor = (keyConfig.isUseSeparateLnColor() && colCfg.lnColor != null)
                    ? colCfg.lnColor
                    : riceColor;

            String prefix = "mania-note-" + keys + "k-col" + col;

            String newRice = prefix;
            addTintOps(ops, lookupMap, colCfg.noteImageRice, newRice, riceColor, globalAlpha, false, keyConfig);
            colCfg.noteImageRice = newRice;

            String newHead = prefix + "H";
            addTintOps(ops, lookupMap, colCfg.noteImageLnHead, newHead, lnColor, globalAlpha, false, keyConfig);
            colCfg.noteImageLnHead = newHead;

            String newBody = prefix + "L";
            addTintOps(ops, lookupMap, colCfg.noteImageLnBody, newBody, lnColor, globalAlpha, true, keyConfig);
            colCfg.noteImageLnBody = newBody;

            if (keyConfig.isUseSeparateLnTail() && colCfg.noteImageLnTail != null) {
                String newTail = prefix + "T";
                addTintOps(ops, lookupMap, colCfg.noteImageLnTail, newTail, lnColor, globalAlpha, true, keyConfig);
                colCfg.noteImageLnTail = newTail;
            }
        }

        LOG.info(String.format("[%dK] Plan de tintado listo: %d operaciones para %d columnas",
                keyConfig.getKeys(), ops.size(), keyConfig.getColumns().size()));
        return ops;
    }

    private static void addTintOps(List<TintOperation> ops,
                                   Map<String, String> lookupMap,
                                   String sourceImageName,
                                   String targetImageName,
                                   Color color,
                                   int alpha,
                                   boolean applyPercy,
                                   ManiaKeyConfig keyConfig) {
        if (sourceImageName == null || sourceImageName.isBlank()) {
            return;
        }

        Optional<String> sd = findEntry(sourceImageName, lookupMap);
        Optional<String> hd = findEntryHd(sourceImageName, lookupMap);

        if (sd.isEmpty() && hd.isEmpty()) {
            LOG.warning("Imagen base no encontrada en el .osk: '" + sourceImageName + "' (se omitira)");
            return;
        }

        sd.ifPresent(src -> ops.add(new TintOperation(
                src,
                targetImageName + ".png",
                targetImageName,
                color,
                alpha,
                applyPercy,
                keyConfig.getPercySize(),
                keyConfig.getPercyShape(),
                false
        )));

        hd.ifPresent(src -> ops.add(new TintOperation(
                src,
                targetImageName + "@2x.png",
                targetImageName,
                color,
                alpha,
                applyPercy,
                keyConfig.getPercySize(),
                keyConfig.getPercyShape(),
                true
        )));
    }

    private static void addIfNotNull(Set<String> set, String value) {
        if (value != null && !value.isBlank()) {
            set.add(value);
        }
    }
}
