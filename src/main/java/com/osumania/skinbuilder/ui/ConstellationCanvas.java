package com.osumania.skinbuilder.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Canvas de constelaciones animado — decoración ambient.
 *
 * <p>Diseñado para usarse como capa de fondo en un StackPane, con el
 * contenido real encima. Nunca debe tapar controles. Las partículas y líneas
 * son deliberadamente tenues para no competir con el UI.</p>
 *
 * <h2>Uso correcto</h2>
 * <pre>
 *   ConstellationCanvas bg = new ConstellationCanvas(45, 140, 0.28);
 *   bg.widthProperty().bind(container.widthProperty());
 *   bg.heightProperty().bind(container.heightProperty());
 *   bg.setMouseTransparent(true);
 *
 *   StackPane pane = new StackPane(bg, myContent);
 * </pre>
 */
public class ConstellationCanvas extends Canvas {

    // ── Configuración ─────────────────────────────────────────────────────────
    private final int    count;
    private final double maxDist;
    private final double maxLineAlpha;
    private final double speed;

    // ── Estado ────────────────────────────────────────────────────────────────
    private final double[] px, py, vx, vy;
    private final AnimationTimer timer;
    private final Random rng;
    private long lastNs = 0;

    // ── Color base de las partículas (azul osu!) ──────────────────────────────
    private static final double CR = 0.20;
    private static final double CG = 0.45;
    private static final double CB = 0.90;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * @param count        Número de partículas. 40–60 para pantalla completa, 20–30 para paneles.
     * @param maxDist      Distancia máxima para trazar línea (px). Recomendado: 120–160.
     * @param maxLineAlpha Opacidad máxima de líneas [0–1]. Mantener ≤ 0.30 para no tapar UI.
     * @param speed        Velocidad de movimiento en px/ms. 0.12–0.22 es casi imperceptible.
     */
    public ConstellationCanvas(int count, double maxDist, double maxLineAlpha, double speed) {
        this.count        = count;
        this.maxDist      = maxDist;
        this.maxLineAlpha = maxLineAlpha;
        this.speed        = speed;

        px  = new double[count];
        py  = new double[count];
        vx  = new double[count];
        vy  = new double[count];
        rng = new Random();

        scatter(900, 640);

        widthProperty().addListener((o, ov, nv)  -> clampPositions());
        heightProperty().addListener((o, ov, nv) -> clampPositions());

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNs == 0) { lastNs = now; return; }
                double dt = Math.min((now - lastNs) / 1_000_000.0, 50.0);
                lastNs = now;
                tick(dt);
                draw();
            }
        };
        timer.start();
    }

    /** Variante con velocidad por defecto (0.15 px/ms — muy sutil). */
    public ConstellationCanvas(int count, double maxDist, double maxLineAlpha) {
        this(count, maxDist, maxLineAlpha, 0.15);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public void stop()  { timer.stop(); }
    public void start() { lastNs = 0; timer.start(); }

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

    private void clampPositions() {
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

        // Fondo — azul noche muy oscuro
        g.setFill(Color.rgb(6, 9, 18));
        g.fillRect(0, 0, w, h);

        // Líneas entre partículas cercanas — muy tenues
        g.setLineWidth(0.6);
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                double dx = px[i] - px[j];
                double dy = py[i] - py[j];
                double d  = Math.sqrt(dx * dx + dy * dy);
                if (d < maxDist) {
                    double alpha = maxLineAlpha * (1.0 - d / maxDist);
                    g.setStroke(Color.color(CR, CG, CB, alpha));
                    g.strokeLine(px[i], py[i], px[j], py[j]);
                }
            }
        }

        // Partículas: glow suave en 3 capas
        for (int i = 0; i < count; i++) {
            double x = px[i], y = py[i];
            // capa exterior difusa
            g.setFill(Color.color(CR, CG, CB, 0.05));
            g.fillOval(x - 8, y - 8, 16, 16);
            // halo medio
            g.setFill(Color.color(CR + 0.15, CG + 0.20, CB, 0.18));
            g.fillOval(x - 3.5, y - 3.5, 7, 7);
            // núcleo brillante
            g.setFill(Color.color(0.78, 0.90, 1.0, 0.88));
            g.fillOval(x - 1.5, y - 1.5, 3, 3);
        }
    }

    // =========================================================================
    // Resizable
    // =========================================================================

    @Override public boolean isResizable()            { return true; }
    @Override public double  prefWidth(double height) { return 0; }
    @Override public double  prefHeight(double width) { return 0; }
    @Override public void    resize(double w, double h) { setWidth(w); setHeight(h); }
}