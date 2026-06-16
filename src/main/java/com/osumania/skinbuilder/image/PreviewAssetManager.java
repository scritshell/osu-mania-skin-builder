package com.osumania.skinbuilder.image;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.SkinAssetResolver;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.IntBuffer;
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
 */
public class PreviewAssetManager {

    private static final Logger LOG = Logger.getLogger(PreviewAssetManager.class.getName());

    private final Map<String, BufferedImage> baseBufferedCache = new HashMap<>();
    private final Map<String, Image>         tintedFxCache     = new HashMap<>();

    private final Map<String, Image>         stageImageFxCache = new HashMap<>();
    private final Map<String, List<Image>>   stageGifFxCache   = new HashMap<>();
    private final Map<String, Integer>       stageGifFpsCache  = new HashMap<>();

    public void loadAssetsFromOsk(Path sourcePath, SkinConfig config) {
        if (sourcePath == null || config == null) return;
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
        LOG.info("Assets cargados → notas base: " + baseBufferedCache.size() + "  stage: " + stageImageFxCache.size());
    }

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

    private void loadNoteAssetsFromZip(ZipFile zip, ManiaKeyConfig km, Map<String, String> lookupMap) {
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

    private void loadStageImagesFromZip(ZipFile zip, ManiaKeyConfig km, Map<String, String> lookupMap) {
        loadStageImageFromZip(zip, lookupMap, km.getStageHintImage());
        loadStageImageFromZip(zip, lookupMap, km.getStageLeftImage());
        loadStageImageFromZip(zip, lookupMap, km.getStageRightImage());
        loadStageImageFromZip(zip, lookupMap, km.getStageBottomImage());
    }

    private void loadBaseImageFromZip(ZipFile zip, Map<String, String> lookupMap, String imageName) {
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
                } catch (Exception e) {}
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
                } catch (Exception e) {}
            }
        });
    }

    private void loadStageImageFromZip(ZipFile zip, Map<String, String> lookupMap, String imageName) {
        if (imageName == null || imageName.isBlank() || stageImageFxCache.containsKey(imageName)) return;
        SkinAssetResolver.findEntry(imageName, lookupMap).ifPresent(entry -> {
            try { ZipEntry ze = zip.getEntry(entry);
                if (ze != null && !ze.isDirectory()) {
                    try (InputStream is = zip.getInputStream(ze)) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) stageImageFxCache.put(imageName, toFxImage(img));
                    }
                }
            } catch (Exception e) {}
        });
    }

    private void loadFromDirectory(Path dirPath, SkinConfig config) throws Exception {
        List<String> files = new ArrayList<>();
        try (var stream = Files.walk(dirPath)) {
            stream.filter(Files::isRegularFile).map(dirPath::relativize).map(Path::toString).forEach(files::add);
        }
        Map<String, String> lookupMap = SkinAssetResolver.buildLookupMap(files);
        for (ManiaKeyConfig km : config.getKeymodes()) {
            loadNoteAssetsFromDirectory(dirPath, km, lookupMap);
            loadStageImagesFromDirectory(dirPath, km, lookupMap);
        }
    }

    private void loadNoteAssetsFromDirectory(Path dirPath, ManiaKeyConfig km, Map<String, String> lookupMap) {
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

    private void loadStageImagesFromDirectory(Path dirPath, ManiaKeyConfig km, Map<String, String> lookupMap) {
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageHintImage());
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageLeftImage());
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageRightImage());
        loadStageImageFromDirectory(dirPath, lookupMap, km.getStageBottomImage());
    }

    private void loadBaseImageFromDirectory(Path dirPath, Map<String, String> lookupMap, String imageName) {
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
                } catch (Exception e) {}
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
                } catch (Exception e) {}
            }
        });
    }

    private void loadStageImageFromDirectory(Path dirPath, Map<String, String> lookupMap, String imageName) {
        if (imageName == null || imageName.isBlank() || stageImageFxCache.containsKey(imageName)) return;
        SkinAssetResolver.findEntry(imageName, lookupMap).ifPresent(entry -> {
            try { Path fp = dirPath.resolve(entry);
                if (Files.isRegularFile(fp)) {
                    try (InputStream is = Files.newInputStream(fp)) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) stageImageFxCache.put(imageName, toFxImage(img));
                    }
                }
            } catch (Exception e) {}
        });
    }

    public void putStageImage(String name, BufferedImage image) {
        if (name == null || image == null) return;
        stageImageFxCache.put(name, toFxImage(image));
    }

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
        stageImageFxCache.put(name, fxFrames.get(0));
    }

    public Image getStageImage(String name) { return (name == null) ? null : stageImageFxCache.get(name); }
    public List<Image> getStageGifFrames(String name) { return (name == null) ? null : stageGifFxCache.get(name); }
    public int getStageGifFps(String name) { return stageGifFpsCache.getOrDefault(name, 25); }
    public boolean hasAnimatedStageImages() { return !stageGifFxCache.isEmpty(); }

    public Image getTintedImage(String imageName, Color tintColor, int globalAlpha) {
        if (imageName == null || imageName.isBlank()) return null;
        if (tintColor == null) tintColor = Color.WHITE;

        String hdName = imageName + SkinAssetResolver.HD_SUFFIX;
        BufferedImage hdBase = baseBufferedCache.get(hdName);
        if (hdBase != null) {
            String key = hdName + "|" + colorKey(tintColor, globalAlpha);
            Image cached = tintedFxCache.get(key);
            if (cached != null) return cached;
            try {
                Image fx = toFxImage(ImageTinter.tint(hdBase, tintColor, globalAlpha));
                if (fx != null) tintedFxCache.put(key, fx);
                return fx;
            } catch (Exception e) {}
        }

        BufferedImage sdBase = baseBufferedCache.get(imageName);
        if (sdBase == null) return null;

        String key = imageName + "|" + colorKey(tintColor, globalAlpha);
        Image cached = tintedFxCache.get(key);
        if (cached != null) return cached;
        try {
            Image fx = toFxImage(ImageTinter.tint(sdBase, tintColor, globalAlpha));
            if (fx != null) tintedFxCache.put(key, fx);
            return fx;
        } catch (Exception e) { return null; }
    }

    private static String colorKey(Color c, int alpha) {
        return String.format("%06X_%d", c.getRGB() & 0xFFFFFF, alpha);
    }

    // =========================================================================
    // FIX 1 DE CLAUDE (IntBuffer.wrap + Premultiplication)
    // =========================================================================

    private Image toFxImage(BufferedImage src) {
        if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) return null;
        try {
            int w = src.getWidth();
            int h = src.getHeight();
            int[] pixels = src.getRGB(0, 0, w, h, null, 0, w);
            // Premultiply alpha — required by PixelFormat.getIntArgbPreInstance()
            for (int i = 0; i < pixels.length; i++) {
                int argb = pixels[i];
                int a = (argb >>> 24) & 0xFF;
                if (a == 255 || a == 0) continue; // no-op for opaque/fully transparent
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8)  & 0xFF;
                int b =  argb         & 0xFF;
                r = (r * a + 127) / 255;
                g = (g * a + 127) / 255;
                b = (b * a + 127) / 255;
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            IntBuffer buffer = IntBuffer.wrap(pixels);
            PixelBuffer<IntBuffer> pixelBuffer =
                    new PixelBuffer<>(w, h, buffer, PixelFormat.getIntArgbPreInstance());
            return new WritableImage(pixelBuffer);
        } catch (Exception e) {
            LOG.warning("toFxImage (PixelBuffer): " + e.getMessage());
            return null;
        }
    }

    public void clear() {
        baseBufferedCache.clear();
        tintedFxCache.clear();
        stageImageFxCache.clear();
        stageGifFxCache.clear();
        stageGifFpsCache.clear();
    }
}