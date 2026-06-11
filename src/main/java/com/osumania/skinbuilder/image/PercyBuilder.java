package com.osumania.skinbuilder.image;

import com.osumania.skinbuilder.core.ManiaKeyConfig.PercyShape;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class PercyBuilder {

    private PercyBuilder() {
    }

    public static BufferedImage applyPercy(BufferedImage baseImage, int percySize, PercyShape shape) {
        if (baseImage == null) {
            throw new IllegalArgumentException("baseImage no puede ser null");
        }

        PercyShape safeShape = shape == null ? PercyShape.FLAT : shape;
        int safePercySize = Math.max(0, Math.min(400, percySize));

        if (safePercySize == 0) {
            return baseImage;
        }

        int firstOpaqueRow = findFirstOpaqueRow(baseImage);
        if (firstOpaqueRow < 0) {
            return new BufferedImage(baseImage.getWidth(), Math.max(1, safePercySize), BufferedImage.TYPE_INT_ARGB);
        }

        int graphicHeight = baseImage.getHeight() - firstOpaqueRow;
        int outputHeight = graphicHeight + safePercySize;
        BufferedImage output = new BufferedImage(baseImage.getWidth(), outputHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = output.createGraphics();
        try {
            g.drawImage(
                    baseImage,
                    0,
                    safePercySize,
                    baseImage.getWidth(),
                    safePercySize + graphicHeight,
                    0,
                    firstOpaqueRow,
                    baseImage.getWidth(),
                    baseImage.getHeight(),
                    null
            );
        } finally {
            g.dispose();
        }

        applyTipMask(output, safePercySize, safeShape);
        return output;
    }

    private static int findFirstOpaqueRow(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    return y;
                }
            }
        }
        return -1;
    }

    private static void applyTipMask(BufferedImage image, int tipStartY, PercyShape shape) {
        if (shape == PercyShape.FLAT || tipStartY >= image.getHeight()) {
            return;
        }

        int width = image.getWidth();
        int graphicHeight = image.getHeight() - tipStartY;
        int tipHeight = Math.max(1, Math.min(graphicHeight, Math.max(8, Math.min(width / 2, Math.max(16, tipStartY)))));

        for (int localY = 0; localY < tipHeight; localY++) {
            double progress = tipHeight == 1 ? 1.0 : (double) localY / (double) (tipHeight - 1);
            int y = tipStartY + localY;

            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                int newAlpha = alpha;
                boolean erase = false;

                switch (shape) {
                    case ROUNDED:
                        erase = isOutsideRoundedTip(width, tipHeight, x, localY);
                        break;
                    case TRIANGLE:
                        erase = isOutsideTriangleTip(width, x, progress);
                        break;
                    case FADE:
                        newAlpha = (int) Math.round(alpha * progress);
                        break;
                    case FLAT:
                    default:
                        break;
                }

                if (erase) {
                    newAlpha = 0;
                }

                image.setRGB(x, y, (argb & 0x00FFFFFF) | (newAlpha << 24));
            }
        }
    }

    private static boolean isOutsideRoundedTip(int width, int tipHeight, int x, int y) {
        double radius = Math.max(1.0, Math.min(width / 2.0, tipHeight));
        double topCurve = 1.0 - ((double) y / Math.max(1.0, tipHeight - 1.0));
        double cut = radius * topCurve * topCurve;
        return x < cut || x >= width - cut;
    }

    private static boolean isOutsideTriangleTip(int width, int x, double progress) {
        double halfVisibleWidth = (width / 2.0) * progress;
        double center = (width - 1) / 2.0;
        return Math.abs(x - center) > halfVisibleWidth;
    }
}
