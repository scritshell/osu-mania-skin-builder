package com.osumania.skinbuilder.core;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;

/**
 * Test rápido de consola para verificar el parser y el writer.
 * No requiere JUnit — compila y ejecuta directamente.
 */
public class SkinIniRoundTripTest {

    public static void main(String[] args) throws Exception {

        System.out.println("=== osu!ManiaSkinBuilder — Test Paso 1 ===\n");

        // ---- 1. Parsear el skin.ini de ejemplo ----
        Path inputPath = Path.of(args.length > 0 ? args[0] : "skin.ini");
        System.out.println("Parseando: " + inputPath.toAbsolutePath());

        SkinConfig parsed = SkinIniParser.parseFile(inputPath);

        System.out.println("Nombre:  " + parsed.getSkinName());
        System.out.println("Autor:   " + parsed.getSkinAuthor());
        System.out.println("Version: " + parsed.getSkinVersion());
        System.out.println();

        // ---- 2. Mostrar keymodes detectados ----
        List<ManiaKeyConfig> keymodes = parsed.getKeymodes();
        System.out.println("Keymodes detectados: " + keymodes.size());
        for (ManiaKeyConfig k : keymodes) {
            System.out.printf("  [Mania] Keys=%2d | ColumnStart=%d | HitPosition=%d | Cols=%d | SplitStages=%b%n",
                    k.getKeys(),
                    k.getColumnStart(),
                    k.getHitPosition(),
                    k.getColumns().size(),
                    k.isSplitStages()
            );
            System.out.print("    ColWidths: ");
            for (ManiaKeyConfig.ColumnConfig col : k.getColumns()) {
                System.out.print(col.columnWidth + " ");
            }
            System.out.println();
            System.out.print("    NoteRice:  ");
            for (ManiaKeyConfig.ColumnConfig col : k.getColumns()) {
                System.out.print(col.noteImageRice + " ");
            }
            System.out.println();
        }
        System.out.println();

        // ---- 3. Prueba de creación manual de un 4K desde cero ----
        System.out.println("--- Prueba creación manual 4K ---");
        SkinConfig manual = new SkinConfig("TestSkin", "TestAuthor");
        ManiaKeyConfig k4 = manual.addKeymode(4);
        k4.setHitPosition(420);
        k4.setColumnStart(250);
        k4.setUniformColumnWidth(75);
        k4.setUniformRiceColor(new Color(100, 200, 255, 255));
        k4.setUniformLnColor(new Color(255, 100, 150, 255));
        k4.setJudgementLine(false);
        k4.setPercySize(24);
        k4.setPercyShape(ManiaKeyConfig.PercyShape.ROUNDED);
        k4.setUseGlobalTransparency(true);
        k4.setGlobalAlpha(200);

        System.out.println("4K config creado. Generando skin.ini...\n");

        String ini4k = SkinIniWriter.generate(manual);
        System.out.println(ini4k);

        // ---- 4. Round-trip: parsear → regenerar → comparar keymodes ----
        System.out.println("\n--- Round-trip del skin.ini original ---");
        String regenerated = SkinIniWriter.generate(parsed);

        // Volvemos a parsear lo regenerado para verificar que es consistente
        SkinConfig reparsed = SkinIniParser.parse(regenerated);
        System.out.println("Keymodes tras round-trip: " + reparsed.getKeymodes().size());

        boolean allMatch = true;
        for (ManiaKeyConfig original : keymodes) {
            var maybeReparsed = reparsed.getKeymode(original.getKeys());
            if (maybeReparsed.isEmpty()) {
                System.err.println("  FALLO: Keymode " + original.getKeys() + "K no encontrado tras round-trip");
                allMatch = false;
                continue;
            }
            ManiaKeyConfig rp = maybeReparsed.get();
            boolean hit  = rp.getHitPosition() == original.getHitPosition();
            boolean cols = rp.getColumns().size() == original.getColumns().size();
            System.out.printf("  %2dK → HitPos: %s | Cols: %s%n",
                    original.getKeys(),
                    hit  ? "OK" : "FALLO (exp=" + original.getHitPosition() + " got=" + rp.getHitPosition() + ")",
                    cols ? "OK" : "FALLO (exp=" + original.getColumns().size() + " got=" + rp.getColumns().size() + ")"
            );
            if (!hit || !cols) allMatch = false;
        }

        System.out.println();
        System.out.println(allMatch ? "✓ Round-trip COMPLETO sin errores." : "✗ Hay fallos en el round-trip.");

        // ---- 5. Escribir el resultado a disco ----
        Path outPath = Path.of("skin_output.ini");
        SkinIniWriter.writeToFile(parsed, outPath);
        System.out.println("\nArchivo generado: " + outPath.toAbsolutePath());
    }
}
