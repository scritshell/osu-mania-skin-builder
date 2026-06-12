package com.osumania.skinbuilder.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Extrae fotogramas individuales de un archivo GIF y los devuelve como
 * {@link BufferedImage} correctamente compuestos (respetando offsets de frame
 * y métodos de descarte GIF).
 *
 * <h2>Por qué es necesario este extractor</h2>
 * osu!mania no lee archivos .gif. Para animar elementos del stage (luces,
 * fondos, etc.) necesita imágenes PNG numeradas:
 * <pre>
 *   mania-stage-hint-0.png
 *   mania-stage-hint-1.png
 *   mania-stage-hint-2.png
 *   ...
 * </pre>
 * Este extractor convierte un GIF en esa secuencia.
 *
 * <h2>Composición de frames GIF</h2>
 * Un GIF puede contener sub-imágenes parciales (delta frames) ubicadas en
 * coordenadas distintas dentro del canvas. Este extractor mantiene un canvas
 * acumulado y aplica el método de descarte correcto para que cada fotograma
 * exportado sea una imagen completa y correcta.
 */
public final class GifFrameExtractor {

    private static final Logger LOG = Logger.getLogger(GifFrameExtractor.class.getName());

    private GifFrameExtractor() {}

    // =========================================================================
    // Modelo de datos
    // =========================================================================

    /**
     * Representa un fotograma ya compuesto del GIF.
     */
    public static final class GifFrame {

        /** Imagen completa y compuesta (canvas final de ese instante). */
        public final BufferedImage image;

        /** Duración de este frame en milisegundos (mínimo 100ms si el GIF declara 0). */
        public final int delayMs;

        /** Índice 0-based del frame dentro del GIF. */
        public final int index;

        GifFrame(BufferedImage image, int delayMs, int index) {
            this.image   = image;
            this.delayMs = delayMs;
            this.index   = index;
        }

        @Override
        public String toString() {
            return String.format("GifFrame[%d, delay=%dms, %dx%d]",
                    index, delayMs, image.getWidth(), image.getHeight());
        }
    }

    // =========================================================================
    // Extracción principal
    // =========================================================================

    /**
     * Extrae todos los frames de un GIF indicado por su {@link Path}.
     *
     * @param gifPath  Ruta al archivo .gif
     * @return         Lista ordenada de frames compuestos, lista para exportar
     * @throws IOException Si el archivo no existe, no es un GIF válido, o no
     *                     hay un lector GIF disponible en el classpath de Java
     */
    public static List<GifFrame> extract(Path gifPath) throws IOException {
        return extract(gifPath.toFile());
    }

    /**
     * Extrae todos los frames de un GIF indicado por un {@link File}.
     */
    public static List<GifFrame> extract(File gifFile) throws IOException {
        if (!gifFile.exists()) {
            throw new IOException("El archivo no existe: " + gifFile.getAbsolutePath());
        }

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("GIF");
        if (!readers.hasNext()) {
            throw new IOException("No hay un lector GIF disponible en este entorno Java.");
        }

        ImageReader reader = readers.next();
        List<GifFrame> frames = new ArrayList<>();

        try (ImageInputStream iis = ImageIO.createImageInputStream(gifFile)) {
            reader.setInput(iis, false); // false = forward seeking desactivado → permite releer

            int frameCount   = reader.getNumImages(true);
            int canvasWidth  = reader.getWidth(0);
            int canvasHeight = reader.getHeight(0);

            LOG.info(String.format("GIF: %d frames, canvas %dx%d — %s",
                    frameCount, canvasWidth, canvasHeight, gifFile.getName()));

            // Canvas acumulado sobre el que se componen los delta frames
            BufferedImage canvas = transparent(canvasWidth, canvasHeight);
            // Copia guardada para el método de descarte "restoreToPrevious"
            BufferedImage savedCanvas = null;

            for (int i = 0; i < frameCount; i++) {
                // Sub-imagen cruda del frame (puede ser más pequeña que el canvas)
                BufferedImage subImage = reader.read(i);
                FrameMeta meta = parseMeta(reader.getImageMetadata(i));

                // Guardar estado ANTES de dibujar si el próximo frame necesita restaurar
                if (meta.disposal == DisposalMethod.RESTORE_TO_PREVIOUS) {
                    savedCanvas = copy(canvas);
                }

                // --- Componer sub-imagen sobre el canvas actual ---
                Graphics2D g = canvas.createGraphics();
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(subImage, meta.x, meta.y, null);
                g.dispose();

                // Capturar el frame ya compuesto
                frames.add(new GifFrame(copy(canvas), meta.delayMs, i));

                // --- Aplicar método de descarte para el SIGUIENTE frame ---
                switch (meta.disposal) {
                    case RESTORE_TO_BACKGROUND:
                        // Borrar el área del sub-frame para dejar fondo transparente
                        Graphics2D g2 = canvas.createGraphics();
                        g2.setComposite(AlphaComposite.Clear);
                        g2.fillRect(meta.x, meta.y, subImage.getWidth(), subImage.getHeight());
                        g2.dispose();
                        break;

                    case RESTORE_TO_PREVIOUS:
                        canvas = (savedCanvas != null) ? copy(savedCanvas)
                                : transparent(canvasWidth, canvasHeight);
                        break;

                    case DO_NOT_DISPOSE:
                    case UNSPECIFIED:
                    default:
                        // Mantener el canvas tal cual → no hacer nada
                        break;
                }
            }
        } finally {
            reader.dispose();
        }

        return frames;
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    /**
     * Calcula el framerate recomendado para {@code LightFramePerSecond} en skin.ini
     * a partir de la duración media de los frames del GIF.
     *
     * <p>osu! usa enteros, por lo que el resultado se redondea al entero más cercano.
     * Mínimo devuelto: 1 FPS.</p>
     *
     * @param frames Lista de frames extraídos con {@link #extract}
     * @return       FPS entero recomendado para skin.ini
     */
    public static int suggestFps(List<GifFrame> frames) {
        if (frames == null || frames.isEmpty()) return 25;
        double avgDelayMs = frames.stream()
                .mapToInt(f -> f.delayMs)
                .average()
                .orElse(100.0);
        return Math.max(1, (int) Math.round(1000.0 / avgDelayMs));
    }

    /**
     * Devuelve una descripción de la duración total del GIF.
     *
     * @param frames Lista de frames
     * @return       Duración total en ms
     */
    public static int totalDurationMs(List<GifFrame> frames) {
        return frames == null ? 0 : frames.stream().mapToInt(f -> f.delayMs).sum();
    }

    // =========================================================================
    // Parseo de metadatos
    // =========================================================================

    private enum DisposalMethod { UNSPECIFIED, DO_NOT_DISPOSE, RESTORE_TO_BACKGROUND, RESTORE_TO_PREVIOUS }

    private static final class FrameMeta {
        int x         = 0;
        int y         = 0;
        int delayMs   = 100;
        DisposalMethod disposal = DisposalMethod.DO_NOT_DISPOSE;
    }

    /**
     * Extrae posición, delay y método de descarte de los metadatos de un frame GIF.
     */
    private static FrameMeta parseMeta(IIOMetadata metadata) {
        FrameMeta fm = new FrameMeta();
        if (metadata == null) return fm;

        try {
            IIOMetadataNode root = (IIOMetadataNode)
                    metadata.getAsTree("javax_imageio_gif_image_1.0");

            // Posición del sub-frame dentro del canvas
            IIOMetadataNode descriptor = child(root, "ImageDescriptor");
            if (descriptor != null) {
                fm.x = intAttr(descriptor, "imageLeftPosition", 0);
                fm.y = intAttr(descriptor, "imageTopPosition",  0);
            }

            // Delay y método de descarte
            IIOMetadataNode gce = child(root, "GraphicControlExtension");
            if (gce != null) {
                int delayCentisecs = intAttr(gce, "delayTime", 10);
                // GIF delay está en centésimas de segundo; convertir a ms
                fm.delayMs = delayCentisecs * 10;
                // Algunos GIFs ponen 0 como delay, lo que en práctica significa ~100ms
                if (fm.delayMs == 0) fm.delayMs = 100;

                String disposal = gce.getAttribute("disposalMethod");
                if (disposal != null) {
                    switch (disposal) {
                        case "restoreToBackgroundColor": fm.disposal = DisposalMethod.RESTORE_TO_BACKGROUND; break;
                        case "restoreToPrevious":        fm.disposal = DisposalMethod.RESTORE_TO_PREVIOUS;   break;
                        case "doNotDispose":             fm.disposal = DisposalMethod.DO_NOT_DISPOSE;        break;
                        default:                         fm.disposal = DisposalMethod.UNSPECIFIED;           break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("No se pudieron leer los metadatos de un frame GIF: " + e.getMessage());
        }

        return fm;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /** Crea una imagen ARGB completamente transparente del tamaño indicado. */
    private static BufferedImage transparent(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    /** Copia profunda de un BufferedImage. */
    private static BufferedImage copy(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    /** Primer hijo de {@code parent} con nombre {@code name}, o null. */
    private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
        for (int i = 0; i < parent.getLength(); i++) {
            if (name.equals(parent.item(i).getNodeName())) {
                return (IIOMetadataNode) parent.item(i);
            }
        }
        return null;
    }

    /** Lee un atributo entero de un nodo, con valor por defecto si falta. */
    private static int intAttr(IIOMetadataNode node, String attr, int fallback) {
        try {
            String val = node.getAttribute(attr);
            if (val == null || val.isBlank()) return fallback;
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}