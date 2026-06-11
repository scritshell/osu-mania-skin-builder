package com.osumania.skinbuilder.core;

import com.osumania.skinbuilder.image.ImageTinter;
import com.osumania.skinbuilder.image.PercyBuilder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class OskPackager {

    private static final String SKIN_INI_ENTRY = "skin.ini";

    private OskPackager() {
    }

    public static void packSkin(SkinConfig config, Path baseOskPath, Path outputOskPath) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("config no puede ser null");
        }
        if (baseOskPath == null) {
            throw new IllegalArgumentException("baseOskPath no puede ser null");
        }
        if (outputOskPath == null) {
            throw new IllegalArgumentException("outputOskPath no puede ser null");
        }
        if (baseOskPath.toAbsolutePath().normalize().equals(outputOskPath.toAbsolutePath().normalize())) {
            throw new IOException("La skin base y la skin exportada deben ser archivos distintos");
        }

        Path outputParent = outputOskPath.toAbsolutePath().getParent();
        if (outputParent != null) {
            Files.createDirectories(outputParent);
        }

        List<ColumnImageSnapshot> snapshots = snapshotColumnImageNames(config);

        try (ZipFile baseZip = new ZipFile(baseOskPath.toFile());
             ZipOutputStream outputZip = new ZipOutputStream(Files.newOutputStream(outputOskPath), StandardCharsets.UTF_8)) {

            List<String> entryNames = baseZip.stream()
                    .map(ZipEntry::getName)
                    .toList();
            Map<String, String> lookupMap = SkinAssetResolver.buildLookupMap(entryNames);
            Set<String> writtenEntries = new HashSet<>();

            for (ManiaKeyConfig keymode : config.getEnabledKeymodes()) {
                List<SkinAssetResolver.TintOperation> tintingPlan =
                        SkinAssetResolver.buildTintingPlan(keymode, lookupMap);

                for (SkinAssetResolver.TintOperation op : tintingPlan) {
                    if (isAlreadyWritten(writtenEntries, op.targetEntry)) {
                        continue;
                    }

                    ZipEntry sourceEntry = baseZip.getEntry(op.sourceEntry);
                    if (sourceEntry == null || sourceEntry.isDirectory()) {
                        continue;
                    }

                    try (InputStream inputStream = baseZip.getInputStream(sourceEntry)) {
                        BufferedImage outputImage = ImageTinter.tintFromStream(inputStream, op.tintColor, op.globalAlpha);
                        if (op.applyPercy) {
                            int percySize = op.highResolution ? op.percySize * 2 : op.percySize;
                            outputImage = PercyBuilder.applyPercy(outputImage, percySize, op.percyShape);
                        }
                        byte[] pngBytes = ImageTinter.toPngBytes(outputImage);
                        writeBytes(outputZip, op.targetEntry, pngBytes, writtenEntries);
                    }
                }
            }

            byte[] skinIniBytes = SkinIniWriter.generate(config).getBytes(StandardCharsets.UTF_8);
            writeBytes(outputZip, SKIN_INI_ENTRY, skinIniBytes, writtenEntries);

            for (ZipEntry baseEntry : baseZip.stream().toList()) {
                if (baseEntry.isDirectory()) {
                    continue;
                }
                if (isAlreadyWritten(writtenEntries, baseEntry.getName())) {
                    continue;
                }
                if (SKIN_INI_ENTRY.equals(normalizeEntryName(baseEntry.getName()))) {
                    continue;
                }

                copyEntry(baseZip, outputZip, baseEntry, writtenEntries);
            }
        } finally {
            restoreColumnImageNames(snapshots);
        }
    }

    private static void writeBytes(ZipOutputStream outputZip,
                                   String entryName,
                                   byte[] bytes,
                                   Set<String> writtenEntries) throws IOException {
        ZipEntry outputEntry = new ZipEntry(entryName);
        outputZip.putNextEntry(outputEntry);
        outputZip.write(bytes);
        outputZip.closeEntry();
        writtenEntries.add(normalizeEntryName(entryName));
    }

    private static void copyEntry(ZipFile baseZip,
                                  ZipOutputStream outputZip,
                                  ZipEntry baseEntry,
                                  Set<String> writtenEntries) throws IOException {
        ZipEntry outputEntry = new ZipEntry(baseEntry.getName());
        outputEntry.setTime(baseEntry.getTime());

        outputZip.putNextEntry(outputEntry);
        try (InputStream inputStream = baseZip.getInputStream(baseEntry)) {
            inputStream.transferTo(outputZip);
        }
        outputZip.closeEntry();
        writtenEntries.add(normalizeEntryName(baseEntry.getName()));
    }

    private static boolean isAlreadyWritten(Set<String> writtenEntries, String entryName) {
        return writtenEntries.contains(normalizeEntryName(entryName));
    }

    private static String normalizeEntryName(String entryName) {
        return entryName.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static List<ColumnImageSnapshot> snapshotColumnImageNames(SkinConfig config) {
        List<ColumnImageSnapshot> snapshots = new ArrayList<>();

        for (ManiaKeyConfig keymode : config.getEnabledKeymodes()) {
            for (ManiaKeyConfig.ColumnConfig column : keymode.getColumns()) {
                snapshots.add(new ColumnImageSnapshot(
                        column,
                        column.noteImageRice,
                        column.noteImageLnHead,
                        column.noteImageLnBody,
                        column.noteImageLnTail
                ));
            }
        }

        return snapshots;
    }

    private static void restoreColumnImageNames(List<ColumnImageSnapshot> snapshots) {
        for (ColumnImageSnapshot snapshot : snapshots) {
            snapshot.column.noteImageRice = snapshot.noteImageRice;
            snapshot.column.noteImageLnHead = snapshot.noteImageLnHead;
            snapshot.column.noteImageLnBody = snapshot.noteImageLnBody;
            snapshot.column.noteImageLnTail = snapshot.noteImageLnTail;
        }
    }

    private static final class ColumnImageSnapshot {
        private final ManiaKeyConfig.ColumnConfig column;
        private final String noteImageRice;
        private final String noteImageLnHead;
        private final String noteImageLnBody;
        private final String noteImageLnTail;

        private ColumnImageSnapshot(ManiaKeyConfig.ColumnConfig column,
                                    String noteImageRice,
                                    String noteImageLnHead,
                                    String noteImageLnBody,
                                    String noteImageLnTail) {
            this.column = column;
            this.noteImageRice = noteImageRice;
            this.noteImageLnHead = noteImageLnHead;
            this.noteImageLnBody = noteImageLnBody;
            this.noteImageLnTail = noteImageLnTail;
        }
    }
}
