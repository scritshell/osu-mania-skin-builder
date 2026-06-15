package com.osumania.skinbuilder.image;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.SkinAssetResolver;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Gestor de caché de imágenes para PreviewCanvas.
 *
 * <h2>Estrategia de caché en dos grupos</h2>
 * <b>Notas / receptores</b>
 * <ol>
 *   <li>{@code baseBufferedCache} — imagen original sin teñir (BufferedImage).</li>
 *   <li>{@code tintedFxCache}     — versión teñida (Image JavaFX), clave {@code "nombre|RRGGBB_alpha"}.</li>
 * </ol>
 * <b>Stage images</b> (color completo, sin teñir)
 * <ol>
 *   <li>{@code stageImageFxCache} — imagen estática o primer frame de GIF.</li>
 *   <li>{@code stageGifFxCache}   — lista de frames GIF.</li>
 *   <li>{@code stageGifFpsCache}  — FPS de cada GIF.</li>
 * </ol>
 */
public class PreviewAssetManager {

    private static final Logger LOG = Logger.getLogger(PreviewAssetManager.class.getName());

    // ── Notas / receptores ────────────────────────────────────────────────────
    private final Map<String, BufferedImage> baseBufferedCache = new HashMap<>();
    private final Map<String, Image>         tintedFxCache     = new HashMap<>();

    // ── Stage images ──────────────────────────────────────────────────────────
    private final Map<String, Image>         stageImageFxCache = new HashMap<>();
    private final Map<String, List<Image>>   stageGifFxCache   = new HashMap<>();
    private final Map<String, Integer>       stageGifFpsCache  = new HashMap<>();

    // =========================================================================
    // Carga desde .osk / carpeta
    // =========================================================================

    public void loadAssetsFromOsk(Path sourcePath, SkinConfig config) {
        if (sourcePath == null || config == null) {
            LOG.warning("sourcePath o config es null");
            return;
        }
        clear();
        try {
            if (Files.isRegularFile(sourcePath)) {
                String fn = sourcePath.getFileName().toString();
                if (fn.endsWith(".osk"))         loadFromZip(sourcePath, config);
                else if (fn.equals("skin.ini")) { Path r = sourcePath.getParent(); if (r != null) loadFromDirectory(r, config); }
            } else if (Files.isDirectory(sourcePath)) {
                loadFromDirectory(sourcePath, config);
            }
        } catch (Exception e) {
            LOG.warning("Error cargando assets: " + e.getMessage());
        }
        LOG.info("Assets cargados → notas base: " + baseBufferedCache.size()
                + "  stage: " + stageImageFxCache.size());
    }

    // ---- ZIP ----------------------------------------------------------------

    private void loadFromZip(Path oskPath, SkinConfig config) throws Exception {
        try (ZipFile zip = new ZipFile(oskPath.toFile())) {
            List<String> entryNames = zip.stream().map(ZipEntry::getName).toList();
            Map<String, String> lookupMap = SkinAssetResolver.buildLookupMap(entryNames);
            for (ManiaKeyConfig km : config.getKeymodes()) {
                loadNoteAssetsFromZip(zip, km, lookupMap);
                loadStageImagesFromZip(zip, km, lookupMap);
            }
        }
    }

    private void loadNoteAssetsFromZip(ZipFile zip, ManiaKeyConfig km,
                                       Map<String, String> lookupMap) {
        for (ManiaKeyConfig.ColumnConfig col : km.getColumns()) {
            loadBaseImageFromZip(zip, lookupMap, col.noteImageRice);
            loadBaseImageFromZip(zip, lookupMap, col.noteImageLnHead);
            loadBaseImageFromZip(zip, lookupMap, col.noteImageLnBody);
            if (km.isUseSeparateLnTail() && col.noteImageLnTail != null)
                loadBaseImageFromZip(zip, lookupMap, col.noteImageLnTail);
            loadBaseImageFromZip(zip, lookupMap, col.keyImage);
            loadBaseImageFromZip(zip, lookupMap, col.keyImageDown);
        }
    }

    private void loadStageImagesFromZip(ZipFile zip, ManiaKeyConfig km,
                                        Map<String, String> lookupMap) {
        loadStageImageFromZip(zip, lookupMap, km.getStageHintImage());
        loadStageImageFromZip(zip, lookupMap, km.getStageLeftImage());
        loadStageImageFromZip(zip, lookupMap, km.getStageRightImage());
        loadStageImageFromZip(zip, lookupMap, km.getStageBottomImage());
    }

    private void loadBaseImageFromZip(ZipFile zip, Map<String, String> lookupMap,
                                      String imageName) {
        if (imageName == null || imageName.isBlank()) return;

        SkinAssetResolver.findEntry(imageName, lookupMap).ifPresent(entry -> {
            if (!baseBufferedCache.containsKey(imageName)) {
                try { ZipEntry ze = zip.getEntry(entry);
                    if (ze != null && !ze.isDirectory()) {
                        try (InputStream is = zip.getInputStream(ze)) {
                            BufferedImage img = ImageIO.read(is);
                            if (img != null) baseBufferedCache.put(imageName, img);
                        }
                    }
                } catch (Exception e) { LOG.warning("base-zip '" + imageName + "': " + e.getMessage()); }
            }
        });

        String hdName = imageName + SkinAssetResolver.HD_SUFFIX;
        SkinAssetResolver.findEntryHd(imageName, lookupMap).ifPresent(entry -> {
            if (!baseBufferedCache.containsKey(hdName)) {
                try { ZipEntry ze = zip.getEntry(entry);
                    if (ze != null && !ze.isDirectory()) {
                        try (InputStream is = zip.getInputStream(ze)) {
                            BufferedImage img = ImageIO.read(is);
                            if (img != null) baseBufferedCache.put(hdName, img);
                        }
                    }
                } catch (Exception e) { LOG.warning("HD-zip '" + hdName + "': " + e.getMessage()); }
            }
        });
    }

    private void loadStageImageFromZip(ZipFile zip, Map<String, String> lookupMap,
                                       String imageName) {
        if (imageName == null || imageName.isBlank() || stageImageFxCache.containsKey(imageName)) return;
        SkinAssetResolver.findEntry(imageName, lookupMap).ifPresent(entry -> {
            try { ZipEntry ze = zip.getEntry(entry);
                if (ze != null && !ze.isDirectory()) {
                    try (InputStream is = zip.getInputStream(ze)) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) stageImageFxCache.put(imageName, toFxImage(img));
                    }
                }
            } catch (Exception e) { LOG.warning("stage-zip '" + imageName + "': " + e.getMessage()); }
        });
    }

    // ---- Directorio ---------------------------------------------------------

    private void loadFromDirectory(Path dirPath, SkinConfig config) throws Exception {
        List<String> files = new ArrayList<>();
        try (var stream = Files.walk(dirPath)) {
            stream.filter(Files::isRegularFile)
                    .map(dirPath::relativize)
                    .map(Path::toString)
                    .forEach(files::add);
        }
        Map<String, String> lookupMap = SkinAssetResolver.buildLookupMap(files);
        for (ManiaKeyConfig km : config.getKeymodes()) {
            loadNoteAssetsFromDirectory(dirPath, km, lookupMap);
            loadStageImagesFromDirectory(dirPath, km, lookupMap);
        }
    }

    private void loadNoteAssetsFromDirectory(Path dirPath, ManiaKeyConfig km,
                                             Map<String, String> lookupMap) {
        for (ManiaKeyConfig.ColumnConfig col : km.getColumns()) {
            loadBaseImageFromDirectory(dirPath, lookupMap, col.noteImageRice);
            loadBaseImageFromDirectory(dirPath, lookupMap, col.noteImageLnHead);
            loadBaseImageFromDirectory(dirPath, lookupMap, col.noteImageLnBody);
            if (km.isUseSeparateLnTail() && col.noteImageLnTail != null)
                loadBaseImageFromDirectory(dirPath, lookupMap, col.noteImageLnTail);
            loadBaseImageFromDirectory(dirPath, lookupMap, col.keyImage);
            loadBaseImageFromDirectory(dirPath, lookupMap, col.keyImageDown);
        }
    }

    private void loadStageImagesFromDirectory(Path dirPath, ManiaKeyConfig km,
                                              Map<String, String> lookupMap) {
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageHintImage());
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageLeftImage());
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageRightImage());
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageBottomImage());
    }

    private void loadBaseImageFromDirectory(Path dirPath, Map<String, String> lookupMap,
                                            String imageName) {
        if (imageName == null || imageName.isBlank()) return;

        SkinAssetResolver.findEntry(imageName, lookupMap).ifPresent(entry -> {
            if (!baseBufferedCache.containsKey(imageName)) {
                try { Path fp = dirPath.resolve(entry);
                    if (Files.isRegularFile(fp)) {
                        try (InputStream is = Files.newInputStream(fp)) {
                            BufferedImage img = ImageIO.read(is);
                            if (img != null) baseBufferedCache.put(imageName, img);
                        }
                    }
                } catch (Exception e) { LOG.warning("base-dir '" + imageName + "': " + e.getMessage()); }
            }
        });

        String hdName = imageName + SkinAssetResolver.HD_SUFFIX;
        SkinAssetResolver.findEntryHd(imageName, lookupMap).ifPresent(entry -> {
            if (!baseBufferedCache.containsKey(hdName)) {
                try { Path fp = dirPath.resolve(entry);
                    if (Files.isRegularFile(fp)) {
                        try (InputStream is = Files.newInputStream(fp)) {
                            BufferedImage img = ImageIO.read(is);
                            if (img != null) baseBufferedCache.put(hdName, img);
                        }
                    }
                } catch (Exception e) { LOG.warning("HD-dir '" + hdName + "': " + e.getMessage()); }
            }
        });
    }

    private void loadStageImageFromDirectory(Path dirPath, Map<String, String> lookupMap,
                                             String imageName) {
        if (imageName == null || imageName.isBlank() || stageImageFxCache.containsKey(imageName)) return;
        SkinAssetResolver.findEntry(imageName, lookupMap).ifPresent(entry -> {
            try { Path fp = dirPath.resolve(entry);
                if (Files.isRegularFile(fp)) {
                    try (InputStream is = Files.newInputStream(fp)) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) stageImageFxCache.put(imageName, toFxImage(img));
                    }
                }
            } catch (Exception e) { LOG.warning("stage-dir '" + imageName + "': " + e.getMessage()); }
        });
    }

    // =========================================================================
    // Stage images — API pública para imágenes añadidas por el usuario
    // =========================================================================

    /** Inyecta un PNG de stage image (sin teñir) cargado desde disco. */
    public void putStageImage(String name, BufferedImage image) {
        if (name == null || image == null) return;
        stageImageFxCache.put(name, toFxImage(image));
    }

    /**
     * Inyecta los frames de un GIF como stage image animada.
     * El primer frame queda en {@code stageImageFxCache} para acceso estático.
     */
    public void putStageGif(String name, List<GifFrameExtractor.GifFrame> frames) {
        if (name == null || frames == null || frames.isEmpty()) return;
        List<Image> fxFrames = new ArrayList<>(frames.size());
        for (GifFrameExtractor.GifFrame frame : frames) {
            Image fx = toFxImage(frame.image);
            if (fx != null) fxFrames.add(fx);
        }
        if (fxFrames.isEmpty()) return;
        stageGifFxCache.put(name, fxFrames);
        stageGifFpsCache.put(name, GifFrameExtractor.suggestFps(frames));
        stageImageFxCache.put(name, fxFrames.get(0));   // primer frame para acceso estático
    }

    /** Imagen estática de stage (o primer frame si es GIF). */
    public Image getStageImage(String name) {
        return (name == null) ? null : stageImageFxCache.get(name);
    }

    /** Lista de frames GIF de un stage image, o null si es imagen estática. */
    public List<Image> getStageGifFrames(String name) {
        return (name == null) ? null : stageGifFxCache.get(name);
    }

    /** FPS del GIF (por defecto 25 si no se conoce). */
    public int getStageGifFps(String name) {
        if (name == null) return 25;
        return stageGifFpsCache.getOrDefault(name, 25);
    }

    /** Devuelve true si al menos un stage image tiene frames GIF. */
    public boolean hasAnimatedStageImages() {
        return !stageGifFxCache.isEmpty();
    }

    // =========================================================================
    // Tintado dinámico para notas / receptores
    // =========================================================================

    /**
     * Devuelve la imagen {@code imageName} teñida con {@code tintColor}/alpha.
     * Prefiere versión @2x. Resultado en sub-caché por color.
     */
    public Image getTintedImage(String imageName, Color tintColor, int globalAlpha) {
        if (imageName == null || imageName.isBlank()) return null;
        if (tintColor == null) tintColor = Color.WHITE;

        // HD primero
        String hdName = imageName + SkinAssetResolver.HD_SUFFIX;
        BufferedImage hdBase = baseBufferedCache.get(hdName);
        if (hdBase != null) {
            String key = hdName + "|" + colorKey(tintColor, globalAlpha);
            Image cached = tintedFxCache.get(key);
            if (cached != null) return cached;
            try {
                Image fx = toFxImage(ImageTinter.tint(hdBase, tintColor, globalAlpha));
                tintedFxCache.put(key, fx);
                return fx;
            } catch (Exception e) { LOG.warning("Tint HD '" + hdName + "': " + e.getMessage()); }
        }

        // SD
        BufferedImage sdBase = baseBufferedCache.get(imageName);
        if (sdBase == null) return null;

        String key = imageName + "|" + colorKey(tintColor, globalAlpha);
        Image cached = tintedFxCache.get(key);
        if (cached != null) return cached;
        try {
            Image fx = toFxImage(ImageTinter.tint(sdBase, tintColor, globalAlpha));
            tintedFxCache.put(key, fx);
            return fx;
        } catch (Exception e) {
            LOG.warning("Tint SD '" + imageName + "': " + e.getMessage());
            return null;
        }
    }

    private static String colorKey(Color c, int alpha) {
        return String.format("%06X_%d", c.getRGB() & 0xFFFFFF, alpha);
    }

    // =========================================================================
    // Conversión y utilidades
    // =========================================================================

    private Image toFxImage(BufferedImage src) {
        if (src == null) return null;
        javafx.scene.image.WritableImage wi =
                new javafx.scene.image.WritableImage(src.getWidth(), src.getHeight());
        javafx.scene.image.PixelWriter pw = wi.getPixelWriter();
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++)
                pw.setArgb(x, y, src.getRGB(x, y));
        return wi;
    }

    /** Limpia todas las cachés. */
    public void clear() {
        baseBufferedCache.clear();
        tintedFxCache.clear();
        stageImageFxCache.clear();
        stageGifFxCache.clear();
        stageGifFpsCache.clear();
    }
}