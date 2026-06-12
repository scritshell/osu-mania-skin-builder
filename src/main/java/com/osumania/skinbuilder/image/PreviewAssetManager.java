package com.osumania.skinbuilder.image;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import com.osumania.skinbuilder.core.SkinAssetResolver;
import com.osumania.skinbuilder.core.SkinConfig;
import javafx.scene.image.Image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Gestor de caché de imágenes para el PreviewCanvas.
 *
 * <p>Carga imágenes PNG desde un .osk, las tinta con los colores de las columnas
 * y las almacena en caché como {@link Image} de JavaFX para renderizado rápido.</p>
 */
public class PreviewAssetManager {

    private static final Logger LOG = Logger.getLogger(PreviewAssetManager.class.getName());

    /** Caché de imágenes: nombre base (ej. "mania-note1") → Image de JavaFX */
    private final Map<String, Image> imageCache = new HashMap<>();

    // =========================================================================
    // Carga de assets
    // =========================================================================

    /**
     * Carga todos los assets (imágenes de notas y receptores) desde un .osk (ZIP) o carpeta.
     *
     * @param sourcePath Ruta del archivo .osk, archivo skin.ini, o directorio raíz de skin
     * @param config     Configuración de la skin con todos los keymodes
     */
    public void loadAssetsFromOsk(Path sourcePath, SkinConfig config) {
        if (sourcePath == null || config == null) {
            LOG.warning("sourcePath o config es null, no se cargarán assets");
            return;
        }

        imageCache.clear();

        try {
            // Detectar si es un ZIP (.osk) o una carpeta/archivo skin.ini
            if (Files.isRegularFile(sourcePath)) {
                String filename = sourcePath.getFileName().toString();
                if (filename.endsWith(".osk")) {
                    // Es un archivo ZIP
                    loadFromZip(sourcePath, config);
                } else if (filename.equals("skin.ini")) {
                    // Es un archivo skin.ini: usar el directorio padre como raíz
                    Path skinRoot = sourcePath.getParent();
                    if (skinRoot != null) {
                        loadFromDirectory(skinRoot, config);
                    } else {
                        LOG.warning("No se pudo obtener el directorio padre de: " + sourcePath);
                    }
                } else {
                    LOG.warning("Archivo no soportado (debe ser .osk o skin.ini): " + sourcePath);
                }
            } else if (Files.isDirectory(sourcePath)) {
                // Es un directorio: usarlo como raíz directamente
                loadFromDirectory(sourcePath, config);
            } else {
                LOG.warning("sourcePath no existe: " + sourcePath);
            }
        } catch (Exception e) {
            LOG.warning("Error cargando assets: " + e.getMessage());
            e.printStackTrace();
        }

        LOG.info("Assets cargados en caché: " + imageCache.size() + " imágenes");
    }

    private void loadFromZip(Path oskPath, SkinConfig config) throws IOException {
        try (ZipFile zipFile = new ZipFile(oskPath.toFile())) {
            // Usar SkinAssetResolver para construir el mapa (con normalización)
            java.util.List<String> entryNames = zipFile.stream()
                    .map(ZipEntry::getName)
                    .toList();
            Map<String, String> lookupMap = SkinAssetResolver.buildLookupMap(entryNames);

            // Cargar imágenes de cada keymode y columna
            for (ManiaKeyConfig keymode : config.getKeymodes()) {
                loadKeymodeAssetsFromZip(zipFile, keymode, lookupMap);
            }
        }
    }

    private void loadFromDirectory(Path dirPath, SkinConfig config) throws IOException {
        // Usar Files.walk para obtener todas las rutas relativas
        java.util.List<String> allFiles = new java.util.ArrayList<>();
        try (var stream = Files.walk(dirPath)) {
            stream.filter(Files::isRegularFile)
                    .map(dirPath::relativize)
                    .map(Path::toString)
                    .forEach(allFiles::add);
        }

        // Construir mapa de búsqueda con normalización
        Map<String, String> lookupMap = SkinAssetResolver.buildLookupMap(allFiles);

        // Cargar imágenes de cada keymode y columna
        for (ManiaKeyConfig keymode : config.getKeymodes()) {
            loadKeymodeAssetsFromDirectory(dirPath, keymode, lookupMap);
        }
    }

    private void loadKeymodeAssetsFromZip(ZipFile zipFile,
                                         ManiaKeyConfig keymode,
                                         Map<String, String> lookupMap) {
        int globalAlpha = keymode.isUseGlobalTransparency() ? keymode.getGlobalAlpha() : 255;

        for (int colIdx = 0; colIdx < keymode.getColumns().size(); colIdx++) {
            ManiaKeyConfig.ColumnConfig col = keymode.getColumn(colIdx);
            Color riceColor = col.riceColor != null ? col.riceColor : Color.WHITE;
            Color lnColor = (keymode.isUseSeparateLnColor() && col.lnColor != null)
                    ? col.lnColor
                    : riceColor;

            // Cargar imágenes de notas
            loadImageFromZip(zipFile, lookupMap, col.noteImageRice, riceColor, globalAlpha);
            loadImageFromZip(zipFile, lookupMap, col.noteImageLnHead, lnColor, globalAlpha);
            loadImageFromZip(zipFile, lookupMap, col.noteImageLnBody, lnColor, globalAlpha);
            if (keymode.isUseSeparateLnTail() && col.noteImageLnTail != null) {
                loadImageFromZip(zipFile, lookupMap, col.noteImageLnTail, lnColor, globalAlpha);
            }

            // Cargar imágenes de receptores/teclas
            loadImageFromZip(zipFile, lookupMap, col.keyImage, riceColor, globalAlpha);
            loadImageFromZip(zipFile, lookupMap, col.keyImageDown, riceColor, globalAlpha);
        }
    }

    private void loadKeymodeAssetsFromDirectory(Path dirPath,
                                                ManiaKeyConfig keymode,
                                                Map<String, String> lookupMap) {
        int globalAlpha = keymode.isUseGlobalTransparency() ? keymode.getGlobalAlpha() : 255;

        for (int colIdx = 0; colIdx < keymode.getColumns().size(); colIdx++) {
            ManiaKeyConfig.ColumnConfig col = keymode.getColumn(colIdx);
            Color riceColor = col.riceColor != null ? col.riceColor : Color.WHITE;
            Color lnColor = (keymode.isUseSeparateLnColor() && col.lnColor != null)
                    ? col.lnColor
                    : riceColor;

            loadImageFromDirectory(dirPath, lookupMap, col.noteImageRice, riceColor, globalAlpha);
            loadImageFromDirectory(dirPath, lookupMap, col.noteImageLnHead, lnColor, globalAlpha);
            loadImageFromDirectory(dirPath, lookupMap, col.noteImageLnBody, lnColor, globalAlpha);
            if (keymode.isUseSeparateLnTail() && col.noteImageLnTail != null) {
                loadImageFromDirectory(dirPath, lookupMap, col.noteImageLnTail, lnColor, globalAlpha);
            }

            loadImageFromDirectory(dirPath, lookupMap, col.keyImage, riceColor, globalAlpha);
            loadImageFromDirectory(dirPath, lookupMap, col.keyImageDown, riceColor, globalAlpha);
        }
    }

    private void loadImageFromZip(ZipFile zipFile,
                                  Map<String, String> lookupMap,
                                  String imageName,
                                  Color tintColor,
                                  int globalAlpha) {
        if (imageName == null || imageName.isBlank()) {
            return;
        }

        // Buscar en el ZIP usando el mapa normalizado
        Optional<String> sdEntry = SkinAssetResolver.findEntry(imageName, lookupMap);
        Optional<String> hdEntry = SkinAssetResolver.findEntryHd(imageName, lookupMap);

        // Cargar versión SD
        sdEntry.ifPresent(entry -> {
            try {
                ZipEntry ze = zipFile.getEntry(entry);
                if (ze != null && !ze.isDirectory()) {
                    try (InputStream is = zipFile.getInputStream(ze)) {
                        BufferedImage buffered = ImageTinter.tintFromStream(is, tintColor, globalAlpha);
                        Image fxImage = bufferedImageToFxImage(buffered);
                        imageCache.put(imageName, fxImage);
                        LOG.fine("Loaded SD image from ZIP: " + imageName + " (from " + entry + ")");
                    }
                }
            } catch (Exception e) {
                LOG.warning("Error loading image " + imageName + " from ZIP: " + e.getMessage());
            }
        });

        // Cargar versión HD si existe
        hdEntry.ifPresent(entry -> {
            try {
                ZipEntry ze = zipFile.getEntry(entry);
                if (ze != null && !ze.isDirectory()) {
                    try (InputStream is = zipFile.getInputStream(ze)) {
                        BufferedImage buffered = ImageTinter.tintFromStream(is, tintColor, globalAlpha);
                        Image fxImage = bufferedImageToFxImage(buffered);
                        imageCache.put(imageName + "@2x", fxImage);
                        LOG.fine("Loaded HD image from ZIP: " + imageName + "@2x (from " + entry + ")");
                    }
                }
            } catch (Exception e) {
                LOG.warning("Error loading HD image " + imageName + "@2x from ZIP: " + e.getMessage());
            }
        });
    }

    private void loadImageFromDirectory(Path dirPath,
                                        Map<String, String> lookupMap,
                                        String imageName,
                                        Color tintColor,
                                        int globalAlpha) {
        if (imageName == null || imageName.isBlank()) {
            return;
        }

        // Buscar usando el mapa normalizado
        Optional<String> sdEntry = SkinAssetResolver.findEntry(imageName, lookupMap);
        Optional<String> hdEntry = SkinAssetResolver.findEntryHd(imageName, lookupMap);

        // Cargar versión SD
        sdEntry.ifPresent(entry -> {
            try {
                Path filePath = dirPath.resolve(entry);
                if (Files.isRegularFile(filePath)) {
                    try (InputStream is = Files.newInputStream(filePath)) {
                        BufferedImage buffered = ImageTinter.tintFromStream(is, tintColor, globalAlpha);
                        Image fxImage = bufferedImageToFxImage(buffered);
                        imageCache.put(imageName, fxImage);
                        LOG.fine("Loaded SD image from directory: " + imageName + " (from " + entry + ")");
                    }
                }
            } catch (Exception e) {
                LOG.warning("Error loading image " + imageName + " from directory: " + e.getMessage());
            }
        });

        // Cargar versión HD si existe
        hdEntry.ifPresent(entry -> {
            try {
                Path filePath = dirPath.resolve(entry);
                if (Files.isRegularFile(filePath)) {
                    try (InputStream is = Files.newInputStream(filePath)) {
                        BufferedImage buffered = ImageTinter.tintFromStream(is, tintColor, globalAlpha);
                        Image fxImage = bufferedImageToFxImage(buffered);
                        imageCache.put(imageName + "@2x", fxImage);
                        LOG.fine("Loaded HD image from directory: " + imageName + "@2x (from " + entry + ")");
                    }
                }
            } catch (Exception e) {
                LOG.warning("Error loading HD image " + imageName + "@2x from directory: " + e.getMessage());
            }
        });
    }

    // =========================================================================
    // Conversión y caché
    // =========================================================================

    /**
     * Convierte un {@link BufferedImage} a {@link Image} de JavaFX.
     */
    private Image bufferedImageToFxImage(BufferedImage buffered) {
        if (buffered == null) {
            return null;
        }

        // Usar PixelWriter para convertir a Image de JavaFX
        javafx.scene.image.WritableImage writableImage =
                new javafx.scene.image.WritableImage(buffered.getWidth(), buffered.getHeight());
        javafx.scene.image.PixelWriter writer = writableImage.getPixelWriter();

        for (int y = 0; y < buffered.getHeight(); y++) {
            for (int x = 0; x < buffered.getWidth(); x++) {
                int argb = buffered.getRGB(x, y);
                writer.setArgb(x, y, argb);
            }
        }

        return writableImage;
    }

    /**
     * Recupera una imagen en caché por su nombre.
     * @param imageName Nombre base de la imagen (ej. "mania-note1")
     * @return La Image de JavaFX, o null si no se encontró en caché
     */
    public Image getImage(String imageName) {
        if (imageName == null) {
            return null;
        }
        return imageCache.get(imageName);
    }

    /**
     * Recupera una imagen HD si existe, sino devuelve la versión SD.
     */
    public Image getImageWithFallback(String imageName) {
        if (imageName == null) {
            return null;
        }
        Image hdImage = imageCache.get(imageName + "@2x");
        if (hdImage != null) {
            return hdImage;
        }
        return imageCache.get(imageName);
    }

    /**
     * Limpia la caché de imágenes.
     */
    public void clear() {
        imageCache.clear();
    }
}
