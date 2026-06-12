package com.osumania.skinbuilder.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Utility for applying a visual Y offset to receptor/key images.
 *
 * <h2>Why this exists</h2>
 * Players sometimes need to move the visual hit receptor (the key graphic)
 * up or down without changing the gameplay {@code HitPosition} value in skin.ini.
 * The conventional technique is to add transparent padding rows to the image:
 * <ul>
 *   <li><b>offset &gt; 0</b> — receptor moves <em>down</em>:
 *       {@code offset} transparent rows are prepended to the top of the image.</li>
 *   <li><b>offset &lt; 0</b> — receptor moves <em>up</em>:
 *       {@code abs(offset)} transparent rows are appended to the bottom of the image.</li>
 *   <li><b>offset == 0</b> — the original image is returned unchanged.</li>
 * </ul>
 *
 * <h2>HD (@2x) images</h2>
 * When processing a high-resolution image the caller should double the offset
 * before passing it here, because osu! renders @2x images at 2× scale and the
 * pixel offset must compensate:
 * <pre>
 *   int effectiveOffset = op.highResolution ? keyConfig.getReceptorOffset() * 2
 *                                           : keyConfig.getReceptorOffset();
 *   ReceptorBuilder.applyOffset(image, effectiveOffset);
 * </pre>
 */
public final class ReceptorBuilder {

    private ReceptorBuilder() {} // utility class — not instantiable

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Returns a new image with the receptor shifted by {@code offset} pixels.
     *
     * <p>The output image is always {@code TYPE_INT_ARGB} regardless of the
     * source type, ensuring correct alpha handling for osu!'s PNG pipeline.</p>
     *
     * @param baseImage The original receptor/key image. Must not be {@code null}.
     * @param offset    Pixel offset in the Y axis.
     *                  <ul>
     *                    <li>{@code 0}  → return {@code baseImage} unchanged.</li>
     *                    <li>{@code > 0} → add transparent padding on top (shifts image down).</li>
     *                    <li>{@code < 0} → add transparent padding on bottom (shifts image up).</li>
     *                  </ul>
     *                  Values outside [-200, 200] are clamped silently.
     * @return A new {@link BufferedImage} with the offset applied, or {@code baseImage}
     *         itself when {@code offset == 0}.
     * @throws IllegalArgumentException if {@code baseImage} is {@code null}.
     */
    public static BufferedImage applyOffset(BufferedImage baseImage, int offset) {
        if (baseImage == null) {
            throw new IllegalArgumentException("baseImage must not be null");
        }

        // Clamp to a safe range (matches the model's [-200, 200] constraint)
        offset = Math.max(-200, Math.min(200, offset));

        if (offset == 0) {
            // Fast path — no allocation needed
            return baseImage;
        }

        int srcWidth  = baseImage.getWidth();
        int srcHeight = baseImage.getHeight();
        int absOffset = Math.abs(offset);
        int newHeight = srcHeight + absOffset;

        // Create a fully-transparent output canvas
        BufferedImage output = new BufferedImage(srcWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();

        try {
            if (offset > 0) {
                // Shift DOWN: draw original at y = offset, leaving top rows transparent
                g.drawImage(baseImage, 0, offset, null);
            } else {
                // Shift UP: draw original at y = 0, leaving bottom rows transparent
                g.drawImage(baseImage, 0, 0, null);
            }
        } finally {
            g.dispose();
        }

        return output;
    }

    // =========================================================================
    // Convenience overload
    // =========================================================================

    /**
     * Same as {@link #applyOffset(BufferedImage, int)} but also accepts the
     * {@code highResolution} flag so callers don't have to double the offset
     * themselves.
     *
     * @param baseImage      The original image.
     * @param offset         Logical Y offset (in SD pixels).
     * @param highResolution If {@code true} the offset is multiplied by 2 before
     *                       being applied, to compensate for the @2x scale factor.
     * @return A new image with the correct offset, or {@code baseImage} when the
     *         effective offset is zero.
     */
    public static BufferedImage applyOffset(BufferedImage baseImage,
                                            int offset,
                                            boolean highResolution) {
        int effectiveOffset = highResolution ? offset * 2 : offset;
        return applyOffset(baseImage, effectiveOffset);
    }
}