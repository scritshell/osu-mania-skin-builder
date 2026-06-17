package com.osumania.skinbuilder.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Canvas que dibuja y anima una constelación de partículas flotantes conectadas
 * por líneas semitransparentes. Se usa como fondo decorativo en GeneralTab y KeymodeTab.
 *
 * <p>Se redimensiona automáticamente al contenedor gracias a {@link #isResizable()}.</p>
 */
public class ConstellationCanvas extends Canvas {

    // ── Config ────────────────────────────────────────────────────────────────
    private final int    count;
    private final double maxDist;
    private final double maxLineAlpha;
    private final double speed;

    // ── State ─────────────────────────────────────────────────────────────────
    private final double[] px, py, vx, vy;
    private final AnimationTimer timer;
    private final Random rng;
    private long lastNs = 0;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor completo.
     *
     * @param count        Número de partículas (ej. 55 para GeneralTab, 28 para KeymodeTab)
     * @param maxDist      Distancia máxima para trazar línea entre partículas
     * @param maxLineAlpha Opacidad máxima de las líneas (0.0–1.0)
     * @param speed        Velocidad de desplazamiento en px/ms
     * @param seed         Semilla para reproducibilidad; usa {@code System.nanoTime()} para aleatoriedad
     */
    public ConstellationCanvas(int count, double maxDist, double maxLineAlpha,
                               double speed, long seed) {
        this.count        = count;
        this.maxDist      = maxDist;
        this.maxLineAlpha = maxLineAlpha;
        this.speed        = speed;

        px  = new double[count];
        py  = new double[count];
        vx  = new double[count];
        vy  = new double[count];
        rng = new Random(seed);

        scatter(900, 640);

        widthProperty().addListener((o, oldW, newW) -> clamp());
        heightProperty().addListener((o, oldH, newH) -> clamp());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNs == 0) { lastNs = now; return; }
                double dt = Math.min((now - lastNs) / 1_000_000.0, 50.0);
                lastNs = now;
                tick(dt);
                draw();
            }
        };
        timer.start();
    }

    /** Variante sin semilla fija (usa System.nanoTime). */
    public ConstellationCanvas(int count, double maxDist, double maxLineAlpha, double speed) {
        this(count, maxDist, maxLineAlpha, speed, System.nanoTime());
    }

    /** Variante con velocidad por defecto (0.22 px/ms). */
    public ConstellationCanvas(int count, double maxDist, double maxLineAlpha) {
        this(count, maxDist, maxLineAlpha, 0.22, System.nanoTime());
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public void stopAnimation()  { timer.stop(); }
    public void startAnimation() { lastNs = 0; timer.start(); }

    // =========================================================================
    // Partículas
    // =========================================================================

    private void scatter(double w, double h) {
        for (int i = 0; i < count; i++) {
            px[i] = rng.nextDouble() * w;
            py[i] = rng.nextDouble() * h;
            double angle = rng.nextDouble() * 2 * Math.PI;
            vx[i] = Math.cos(angle) * speed;
            vy[i] = Math.sin(angle) * speed;
        }
    }

    private void clamp() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < count; i++) {
            if (px[i] > w) px[i] = rng.nextDouble() * w;
            if (py[i] > h) py[i] = rng.nextDouble() * h;
        }
    }

    private void tick(double dt) {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < count; i++) {
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            if (px[i] < 0) { px[i] = 0; vx[i] =  Math.abs(vx[i]); }
            if (px[i] > w) { px[i] = w; vx[i] = -Math.abs(vx[i]); }
            if (py[i] < 0) { py[i] = 0; vy[i] =  Math.abs(vy[i]); }
            if (py[i] > h) { py[i] = h; vy[i] = -Math.abs(vy[i]); }
        }
    }

    // =========================================================================
    // Dibujo
    // =========================================================================

    private void draw() {
        double w = getWidth(), h = getHeight();
        if (w < 1 || h < 1) return;

        GraphicsContext g = getGraphicsContext2D();

        // Fondo oscuro
        g.setFill(Color.rgb(6, 9, 18));
        g.fillRect(0, 0, w, h);

        // Líneas entre partículas cercanas
        g.setLineWidth(0.65);
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                double dx = px[i] - px[j];
                double dy = py[i] - py[j];
                double d  = Math.sqrt(dx * dx + dy * dy);
                if (d < maxDist) {
                    double a = maxLineAlpha * (1.0 - d / maxDist);
                    g.setStroke(Color.color(0.22, 0.52, 0.90, a));
                    g.strokeLine(px[i], py[i], px[j], py[j]);
                }
            }
        }

        // Partículas con efecto glow en 3 capas
        for (int i = 0; i < count; i++) {
            double x = px[i], y = py[i];
            g.setFill(Color.color(0.24, 0.52, 1.0, 0.07));
            g.fillOval(x - 9,   y - 9,   18,  18);
            g.setFill(Color.color(0.38, 0.66, 1.0, 0.22));
            g.fillOval(x - 3.8, y - 3.8,  7.6, 7.6);
            g.setFill(Color.color(0.80, 0.92, 1.0, 0.94));
            g.fillOval(x - 1.7, y - 1.7,  3.4, 3.4);
        }
    }

    // =========================================================================
    // Layout — el canvas se redimensiona automáticamente con el contenedor
    // =========================================================================

    @Override public boolean isResizable()              { return true; }
    @Override public double  prefWidth(double h)        { return 400;  }
    @Override public double  prefHeight(double w)       { return 300;  }
    @Override public void    resize(double w, double h) { setWidth(w); setHeight(h); }
}