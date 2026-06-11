package com.osumania.skinbuilder.image;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Aplica tinte de color (multiplicación RGB) y ajuste de alpha a imágenes PNG.
 *
 * <p>La operación de tintado es idéntica a la que usa osu! internamente:</p>
 * <pre>
 *   outR = (srcR * tintR) / 255
 *   outG = (srcG * tintG) / 255
 *   outB = (srcB * tintB) / 255
 *   outA = (srcA * globalAlpha) / 255
 * </pre>
 *
 * <p><b>IMPORTANTE:</b> para que el tinte sea efectivo, las imágenes base deben ser
 * grises o blancas. Sobre un píxel blanco (255,255,255), el resultado es exactamente
 * el color del tinte. Sobre un píxel negro (0,0,0), el resultado es siempre negro
 * independientemente del tinte aplicado.</p>
 */
public final class ImageTinter {

    private ImageTinter() {} // clase utilitaria, no instanciable

    // =========================================================================
    // API — operaciones sobre BufferedImage
    // =========================================================================

    /**
     * Tinta una imagen con el color dado, sin modificar el alpha original.
     *
     * @param source    Imagen fuente (cualquier tipo; se convierte a ARGB internamente)
     * @param tintColor Color de tinte (se usan sus canales R, G, B como factores 0–255)
     * @return          Nueva imagen tintada (TYPE_INT_ARGB)
     */
    public static BufferedImage tint(BufferedImage source, Color tintColor) {
        return tint(source, tintColor, 255);
    }

    /**
     * Tinta una imagen aplicando también un multiplicador de alpha global.
     *
     * @param source      Imagen fuente
     * @param tintColor   Color de tinte
     * @param globalAlpha Multiplicador de alpha [0–255].
     *                    255 = sin cambio de alpha; 0 = totalmente transparente.
     * @return            Nueva imagen tintada (TYPE_INT_ARGB)
     */
    public static BufferedImage tint(BufferedImage source, Color tintColor, int globalAlpha) {
        int width  = source.getWidth();
        int height = source.getHeight();

        // Siempre creamos ARGB para preservar la transparencia
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int tr = tintColor.getRed();
        int tg = tintColor.getGreen();
        int tb = tintColor.getBlue();
        int ta = clamp(globalAlpha, 0, 255);

        // Optimización: tinte neutro → copia directa de píxeles sin operar
        if (tr == 255 && tg == 255 && tb == 255 && ta == 255) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out.setRGB(x, y, toArgb(source.getRGB(x, y)));
                }
            }
            return out;
        }

        // Tintado pixel a pixel mediante multiplicación de canales
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);

                // >>> (unsigned shift) para extraer el canal alpha correctamente
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>>  8) & 0xFF;
                int b =  argb         & 0xFF;

                // Multiplicación osu!-style: resultado = canal_src * factor_tinte / 255
                r = (r * tr) / 255;
                g = (g * tg) / 255;
                b = (b * tb) / 255;
                a = (a * ta) / 255;

                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return out;
    }

    // =========================================================================
    // API — desde InputStream / byte[]
    // =========================================================================

    /**
     * Lee una imagen de un {@link InputStream}, la tinta y devuelve la imagen resultante.
     *
     * @param inputStream  Stream con los bytes de la imagen (PNG, JPG…)
     * @param tintColor    Color de tinte
     * @param globalAlpha  Multiplicador de alpha [0–255]
     * @return             Imagen tintada como {@link BufferedImage}
     * @throws IOException Si no se puede leer o decodificar la imagen
     */
    public static BufferedImage tintFromStream(InputStream inputStream,
                                               Color tintColor,
                                               int globalAlpha) throws IOException {
        BufferedImage source = ImageIO.read(inputStream);
        if (source == null) {
            throw new IOException("No se pudo decodificar la imagen del stream " +
                    "(formato no soportado o datos corruptos)");
        }
        return tint(source, tintColor, globalAlpha);
    }

    /**
     * Lee una imagen de un {@code byte[]}, la tinta y devuelve los bytes PNG del resultado.
     * <p>Conveniente para trabajar directamente con entradas de un
     * {@link java.util.zip.ZipFile}.</p>
     *
     * @param sourceBytes  Bytes de la imagen fuente
     * @param tintColor    Color de tinte
     * @param globalAlpha  Multiplicador de alpha [0–255]
     * @return             Bytes PNG de la imagen tintada
     * @throws IOException Si no se puede procesar la imagen
     */
    public static byte[] tintToPngBytes(byte[] sourceBytes,
                                        Color tintColor,
                                        int globalAlpha) throws IOException {
        BufferedImage source = fromBytes(sourceBytes);
        return toPngBytes(tint(source, tintColor, globalAlpha));
    }

    /**
     * Tinta un {@link BufferedImage} y devuelve los bytes PNG del resultado.
     */
    public static byte[] tintToPngBytes(BufferedImage source,
                                        Color tintColor,
                                        int globalAlpha) throws IOException {
        return toPngBytes(tint(source, tintColor, globalAlpha));
    }

    // =========================================================================
    // Helpers públicos
    // =========================================================================

    /**
     * Serializa un {@link BufferedImage} como bytes PNG en memoria.
     *
     * @throws IOException Si ImageIO no puede codificar la imagen como PNG
     */
    public static byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean ok = ImageIO.write(image, "PNG", baos);
        if (!ok) {
            throw new IOException("ImageIO no pudo codificar la imagen como PNG " +
                    "(¿falta el writer de PNG en el classpath?)");
        }
        return baos.toByteArray();
    }

    /**
     * Decodifica un {@code byte[]} como {@link BufferedImage}.
     *
     * @throws IOException Si los bytes no representan una imagen válida o soportada
     */
    public static BufferedImage fromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                throw new IOException("Los bytes proporcionados no son una imagen reconocida " +
                        "(formato no soportado o datos corruptos)");
            }
            return img;
        }
    }

    /**
     * Determina si un tinte es "neutro", es decir, que no produce ningún cambio
     * visual sobre la imagen fuente (blanco puro + alpha 255).
     * <p>Útil para optimizar: si el tinte es neutro se puede copiar la imagen
     * directamente sin procesar píxeles.</p>
     *
     * @param c           Color de tinte
     * @param globalAlpha Multiplicador de alpha [0–255]
     * @return            {@code true} si el tinte no produce ningún cambio
     */
    public static boolean isNeutralTint(Color c, int globalAlpha) {
        return c.getRed()   == 255
                && c.getGreen() == 255
                && c.getBlue()  == 255
                && globalAlpha  >= 255;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /** Fuerza un valor entero al rango [min, max]. */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Garantiza que el entero ARGB tiene el canal alpha correctamente en los bits 24–31.
     * Algunas imágenes fuente usan TYPE_INT_RGB (sin canal alpha) y getRGB() devuelve
     * 0xFF en los bits superiores, pero lo hacemos explícito por seguridad.
     */
    private static int toArgb(int argb) {
        // Si el alpha ya está presente (imagen ARGB), no hacemos nada.
        // Para imágenes RGB puras, setRGB con alpha=0 daría píxeles transparentes;
        // en su lugar preservamos el alpha original tal como viene.
        return argb;
    }
}