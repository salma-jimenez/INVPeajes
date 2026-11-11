package com.invpeajes.ui.login;

import com.invpeajes.ui.common.Theme;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Panel ilustrado de la derecha del inicio de sesión: un recuadro azul con esquinas redondeadas
 * que muestra un peaje en la vía (montañas, sol, plaza de peaje, barreras y un carro), la frase
 * de RutaPeaje arriba y una franja con datos del proyecto abajo.
 *
 * <p>Todo se dibuja con Java2D, sin imágenes. La escena se diseñó sobre un lienzo de 788 x 732
 * y se escala para cubrir el recuadro sin deformarse.</p>
 *
 * @author Salomejimenez
 * @version 2.0
 */
public class BrandPanel extends JPanel {

    private static final int SCENE_W = 788;
    private static final int SCENE_H = 732;
    private static final int MARGIN = 18;
    private static final int ARC = 44;
    private static final int STRIP_HEIGHT = 92;

    private static final Color NAVY = new Color(0x0F2A5F);
    private static final Color SKY_BOTTOM = new Color(0x1E40AF);
    private static final Color MOUNTAIN = new Color(0x16367A);
    private static final Color HATCH = new Color(0x3B5BA9);
    private static final Color OUTLINE = new Color(0xCBD5E1);
    private static final Color SNOW = new Color(0xE2E8F0);
    private static final Color AMBER = new Color(0xF59E0B);
    private static final Color SUN = new Color(0xFBBF24);
    private static final Color DARK = new Color(0x0F172A);

    private static final String[][] STATS = {
        {"181", "peajes registrados"},
        {"7", "categorías vehiculares"},
        {"24/7", "asistencia en la vía"},
    };

    /** Crea el panel ilustrado. */
    public BrandPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(SCENE_W + MARGIN, SCENE_H + 2 * MARGIN));
    }

    /**
     * Fuente seminegrita para los títulos (Segoe UI Semibold en Windows; si no existe, negrita).
     * @param size Tamaño en puntos.
     * @return La fuente.
     */
    static Font semibold(int size) {
        boolean available = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()).contains("Segoe UI Semibold");
        return available ? new Font("Segoe UI Semibold", Font.PLAIN, size) : Theme.font(Font.BOLD, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth() - MARGIN;
        int h = getHeight() - 2 * MARGIN;
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(0, MARGIN);
        g2.clip(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));

        // La escena cubre el recuadro: se escala por el lado que más necesite y se centra.
        double scale = Math.max((double) w / SCENE_W, (double) h / SCENE_H);
        Graphics2D scene = (Graphics2D) g2.create();
        scene.translate((w - SCENE_W * scale) / 2, (h - SCENE_H * scale) / 2);
        scene.scale(scale, scale);
        paintScene(scene);
        scene.dispose();

        paintTexts(g2, w, h);
        g2.dispose();
    }

    // ------------------------------------------------------------------ Escena

    private void paintScene(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, NAVY, 0, SCENE_H, SKY_BOTTOM));
        g.fillRect(0, 0, SCENE_W, SCENE_H);

        // Estrellas
        g.setColor(new Color(255, 255, 255, 140));
        dot(g, 560, 60, 1.6);
        dot(g, 700, 110, 1.2);
        dot(g, 430, 40, 1);
        dot(g, 740, 40, 1.4);

        // Sol con su aro
        g.setStroke(stroke(2));
        g.setColor(SUN);
        g.fill(circle(610, 210, 52));
        g.setColor(new Color(0xFDE68A));
        g.draw(circle(610, 210, 52));
        g.setColor(new Color(251, 191, 36, 64));
        g.draw(circle(610, 210, 74));

        // Nube
        Path2D cloud = new Path2D.Double();
        cloud.moveTo(120, 250);
        cloud.quadTo(138, 222, 166, 236);
        cloud.quadTo(180, 212, 208, 228);
        cloud.quadTo(238, 224, 238, 250);
        cloud.closePath();
        fillAndDraw(g, cloud, MOUNTAIN, OUTLINE);

        // Montañas con sombra rayada y nieve
        fillAndDraw(g, polygon(0, 420, 110, 300, 180, 360, 290, 230, 390, 340, 460, 290, 570, 380,
                670, 270, 788, 380, 788, 460, 0, 460), MOUNTAIN, OUTLINE);
        hatch(g, polygon(110, 300, 180, 360, 110, 360));
        hatch(g, polygon(290, 230, 390, 340, 320, 340));
        hatch(g, polygon(670, 270, 788, 380, 730, 380));
        fillAndDraw(g, polygon(290, 230, 318, 266, 302, 262, 290, 280, 278, 262, 262, 266), SNOW, SNOW);
        fillAndDraw(g, polygon(670, 270, 696, 300, 680, 296, 670, 312, 660, 296, 644, 300), SNOW, SNOW);

        // Pasto
        Path2D ground = new Path2D.Double();
        ground.moveTo(0, 470);
        ground.quadTo(200, 448, 394, 460);
        ground.quadTo(588, 472, 788, 452);
        ground.lineTo(788, 732);
        ground.lineTo(0, 732);
        ground.closePath();
        fillAndDraw(g, ground, new Color(0x14532D), new Color(74, 222, 128, 128));

        // Carretera con la línea del centro
        fillAndDraw(g, polygon(344, 460, 444, 460, 690, 732, 98, 732), new Color(0x1E293B), OUTLINE);
        g.setColor(new Color(0xFDE68A));
        g.setStroke(stroke(3));
        int[][] dashes = {{468, 486}, {504, 532}, {554, 594}, {620, 672}, {700, 740}};
        for (int[] d : dashes) {
            g.draw(new Line2D.Double(394, d[0], 394, d[1]));
        }

        paintTree(g, 110, 468, 438, 1.0);
        paintTree(g, 680, 466, 440, 0.85);

        // Poste de abscisa "PR 41"
        g.setColor(OUTLINE);
        g.setStroke(stroke(2));
        g.draw(new Line2D.Double(584, 518, 584, 462));
        fillAndDraw(g, new RoundRectangle2D.Double(566, 438, 36, 26, 6, 6), NAVY, OUTLINE);
        centered(g, "PR 41", Theme.font(Font.BOLD, 12), SNOW, 584, 456, 0);

        paintTollPlaza(g);
        paintCar(g);
    }

    private void paintTree(Graphics2D g, double x, double base, double top, double k) {
        g.setColor(OUTLINE);
        g.setStroke(stroke(2));
        g.draw(new Line2D.Double(x, base, x, top));
        Path2D crown = new Path2D.Double();
        crown.moveTo(x, 444);
        crown.quadTo(x - 26 * k, 438, x - 18 * k, 444 - 30 * k);
        crown.quadTo(x - 26 * k, 444 - 56 * k, x, 444 - 60 * k);
        crown.quadTo(x + 26 * k, 444 - 56 * k, x + 18 * k, 444 - 30 * k);
        crown.quadTo(x + 26 * k, 438, x, 444);
        crown.closePath();
        fillAndDraw(g, crown, new Color(0x166534), OUTLINE);
    }

    private void paintTollPlaza(Graphics2D g) {
        // Techo con la franja azul
        fillAndDraw(g, polygon(222, 358, 566, 358, 576, 382, 212, 382), SNOW, SNOW);
        g.setColor(Theme.PRIMARY);
        g.fill(polygon(222, 358, 566, 358, 571, 370, 217, 370));

        // Letrero "PEAJE"
        g.setColor(SNOW);
        g.setStroke(stroke(2));
        g.draw(new Line2D.Double(369, 354, 369, 358));
        g.draw(new Line2D.Double(419, 354, 419, 358));
        fillAndDraw(g, new RoundRectangle2D.Double(343, 324, 102, 30, 8, 8), NAVY, SNOW);
        centered(g, "PEAJE", Theme.font(Font.BOLD, 15), Color.WHITE, 394, 345, 0.15f);

        // Columnas
        g.setColor(SNOW);
        g.draw(new Line2D.Double(238, 382, 238, 462));
        g.draw(new Line2D.Double(550, 382, 550, 462));
        g.draw(new Line2D.Double(394, 382, 394, 460));

        // Casetas con ventana
        for (int x : new int[]{262, 468}) {
            fillAndDraw(g, new RoundRectangle2D.Double(x, 402, 58, 60, 8, 8), new Color(0xF8FAFC), SNOW);
            g.setColor(new Color(0x93C5FD));
            g.fillRect(x + 10, 412, 38, 20);
        }

        // Semáforos
        g.setColor(new Color(0x22C55E));
        g.fill(circle(380, 398, 5));
        g.setColor(new Color(0xEF4444));
        g.fill(circle(408, 398, 5));

        // Barreras: la izquierda cerrada y la derecha levantada
        barrier(g, 320, 444, 388, 444, 322, 444, 386, 444);
        barrier(g, 468, 444, 402, 398, 466, 443, 404, 399.5);
    }

    private void barrier(Graphics2D g, double x1, double y1, double x2, double y2,
                         double ix1, double iy1, double ix2, double iy2) {
        g.setColor(SNOW);
        g.setStroke(stroke(7));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        g.setColor(AMBER);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10,
                new float[]{10, 8}, 0));
        g.draw(new Line2D.Double(ix1, iy1, ix2, iy2));
    }

    private void paintCar(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(304, 512);
        g.scale(0.86, 0.86);
        g.setStroke(stroke(2));

        Path2D body = new Path2D.Double();
        body.moveTo(10, 52);
        body.quadTo(10, 30, 30, 28);
        body.lineTo(52, 6);
        body.quadTo(58, 0, 70, 0);
        body.lineTo(138, 0);
        body.quadTo(150, 0, 156, 6);
        body.lineTo(178, 28);
        body.quadTo(198, 30, 198, 52);
        body.lineTo(198, 86);
        body.quadTo(198, 94, 190, 94);
        body.lineTo(18, 94);
        body.quadTo(10, 94, 10, 86);
        body.closePath();
        fillAndDraw(g, body, Theme.PRIMARY, DARK);

        fillAndDraw(g, polygon(44, 28, 60, 10, 148, 10, 164, 28), new Color(0xBFDBFE), DARK);
        g.setColor(DARK);
        g.draw(new Line2D.Double(104, 10, 104, 28));

        fillAndDraw(g, new RoundRectangle2D.Double(22, 42, 30, 12, 8, 8), new Color(0xFEF3C7), DARK);
        fillAndDraw(g, new RoundRectangle2D.Double(156, 42, 30, 12, 8, 8), new Color(0xFEF3C7), DARK);
        fillAndDraw(g, new RoundRectangle2D.Double(72, 66, 64, 18, 6, 6), new Color(0xFACC15), DARK);
        centered(g, "KWY620", new Font(Font.SANS_SERIF, Font.BOLD, 11), new Color(0x18181B), 104, 79, 0);

        fillAndDraw(g, new RoundRectangle2D.Double(18, 94, 34, 18, 10, 10), DARK, DARK);
        fillAndDraw(g, new RoundRectangle2D.Double(156, 94, 34, 18, 10, 10), DARK, DARK);
        g.setTransform(saved);
    }

    // ------------------------------------------------------------------ Textos

    private void paintTexts(Graphics2D g, int w, int h) {
        // Frase de arriba
        g.setFont(tracked(Theme.font(Font.BOLD, 12), 0.16f));
        g.setColor(new Color(0x93C5FD));
        g.drawString("RUTAPEAJE · COLOMBIA", 44, 40 + 12);
        g.setFont(semibold(26));
        g.setColor(Color.WHITE);
        g.drawString("Conectando caminos,", 44, 92);
        g.drawString("impulsando el país.", 44, 124);

        // Franja de datos
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        g.setColor(NAVY);
        g.fillRect(0, h - STRIP_HEIGHT, w, STRIP_HEIGHT);
        g.setComposite(AlphaComposite.SrcOver);

        Font number = Theme.font(Font.BOLD, 22);
        Font label = Theme.font(Font.PLAIN, 12);
        int x = 44;
        int top = h - STRIP_HEIGHT + 20;
        for (String[] stat : STATS) {
            g.setColor(AMBER);
            g.fillRect(x, top, 2, 42);
            g.setColor(Color.WHITE);
            g.setFont(number);
            g.drawString(stat[0], x + 14, top + 22);
            g.setColor(new Color(0xBFD0F5));
            g.setFont(label);
            g.drawString(stat[1], x + 14, top + 40);
            int width = Math.max(g.getFontMetrics(number).stringWidth(stat[0]),
                    g.getFontMetrics(label).stringWidth(stat[1]));
            x += 14 + width + 34;
        }
    }

    // ------------------------------------------------------------------ Ayudas de dibujo

    private static BasicStroke stroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static Ellipse2D circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
    }

    private static void dot(Graphics2D g, double cx, double cy, double r) {
        g.fill(circle(cx, cy, r));
    }

    private static Polygon polygon(int... xy) {
        Polygon p = new Polygon();
        for (int i = 0; i < xy.length; i += 2) {
            p.addPoint(xy[i], xy[i + 1]);
        }
        return p;
    }

    private static void fillAndDraw(Graphics2D g, Shape shape, Color fill, Color line) {
        g.setColor(fill);
        g.fill(shape);
        g.setColor(line);
        g.draw(shape);
    }

    /** Rellena la figura con rayas diagonales (la sombra de las montañas). */
    private static void hatch(Graphics2D g, Shape shape) {
        Graphics2D h = (Graphics2D) g.create();
        h.clip(shape);
        h.setColor(HATCH);
        h.setStroke(new BasicStroke(1.2f));
        java.awt.Rectangle b = shape.getBounds();
        for (int x = b.x - b.height; x < b.x + b.width; x += 7) {
            h.draw(new Line2D.Double(x, b.y + b.height, x + b.height, b.y));
        }
        h.dispose();
    }

    private static Font tracked(Font font, float tracking) {
        return font.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    private static void centered(Graphics2D g, String text, Font font, Color color,
                                 double cx, double baseline, float tracking) {
        Font f = tracking > 0 ? tracked(font, tracking) : font;
        g.setFont(f);
        g.setColor(color);
        int width = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (float) (cx - width / 2.0), (float) baseline);
    }
}
