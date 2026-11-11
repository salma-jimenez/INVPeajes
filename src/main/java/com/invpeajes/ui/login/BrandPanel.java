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
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/**
 * Franja azul de la izquierda del inicio de sesión: nombre de RutaPeaje, frase, datos del
 * proyecto y la ilustración del peaje (montañas, plaza de peaje, barrera y carro).
 * Todo se dibuja con Java2D, sin imágenes, en un lienzo de 520 x 768 que se escala al alto
 * de la ventana.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class BrandPanel extends JPanel {

    private static final int W = 520;
    private static final int H = 768;
    private static final Color NAVY = new Color(0x0B1F4A);

    /** Crea la franja. */
    public BrandPanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(NAVY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = Math.max(getWidth() / (double) W, getHeight() / (double) H);
        double offsetX = (getWidth() - W * scale) / 2;
        double offsetY = getHeight() - H * scale;
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        paintScene(g2);
        paintTexts(g2);
        g2.dispose();
    }

    private void paintScene(Graphics2D g) {
        g.setPaint(new LinearGradientPaint(0, 0, 0, H, new float[]{0f, 0.55f, 1f},
                new Color[]{NAVY, new Color(0x1D4ED8), new Color(0x60A5FA)}));
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(255, 255, 255, 180));
        int[][] stars = {{60, 70}, {140, 40}, {300, 90}, {430, 50}, {480, 140}, {220, 150}};
        for (int[] s : stars) {
            g.fillOval(s[0] - 1, s[1] - 1, 3, 3);
        }

        g.setPaint(new RadialGradientPaint(new Point2D.Float(380, 420), 90, new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(0xFDE68A), new Color(251, 191, 36, 230), new Color(251, 191, 36, 0)}));
        g.fillOval(290, 330, 180, 180);

        g.setColor(new Color(30, 58, 138, 217));
        g.fill(poly(0, 470, 70, 400, 130, 445, 210, 360, 290, 440, 360, 385, 440, 450, 520, 395, 520, 560, 0, 560));
        g.setColor(new Color(0x172554));
        g.fill(poly(0, 510, 90, 455, 170, 500, 250, 440, 340, 505, 420, 460, 520, 500, 520, 600, 0, 600));

        g.setColor(new Color(0x14532D));
        g.fill(hill(560, 130, 520, 260, 545, 390, 570, 520, 540));
        g.setColor(new Color(0x166534));
        g.fill(hill(580, 140, 548, 260, 568, 380, 588, 520, 562));

        g.setPaint(new GradientPaint(0, 540, new Color(0x334155), 0, H, new Color(0x1E293B)));
        g.fill(poly(232, 540, 288, 540, 470, 768, 50, 768));
        g.setColor(new Color(0xFDE68A));
        g.fill(poly(258, 540, 262, 540, 268, 580, 252, 580));
        g.fill(poly(250, 600, 270, 600, 278, 650, 242, 650));
        g.fill(poly(238, 680, 282, 680, 294, 740, 226, 740));

        paintTollPlaza(g);
        paintCar(g, 206, 640);
    }

    private void paintTollPlaza(Graphics2D g) {
        g.setColor(new Color(0xE2E8F0));
        g.fill(new RoundRectangle2D.Double(150, 512, 220, 16, 6, 6));
        g.setColor(Theme.WARNING);
        g.fill(new RoundRectangle2D.Double(150, 506, 220, 8, 6, 6));
        g.setColor(new Color(0x0F2A5F));
        g.fill(new RoundRectangle2D.Double(216, 482, 88, 22, 8, 8));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(216, 482, 88, 22, 8, 8));
        g.setFont(Theme.font(Font.BOLD, 12));
        drawCentered(g, "PEAJE", 260, 498);

        g.setColor(new Color(0xCBD5E1));
        g.fillRect(168, 528, 10, 26);
        g.fillRect(342, 528, 10, 26);
        g.fillRect(255, 528, 10, 26);
        for (int x : new int[]{185, 313}) {
            g.setColor(new Color(0xF8FAFC));
            g.fill(new RoundRectangle2D.Double(x, 534, 22, 20, 4, 4));
            g.setColor(new Color(0x93C5FD));
            g.fillRect(x + 4, 538, 14, 7);
        }
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.DANGER);
        g.drawLine(207, 548, 248, 548);
        g.setColor(Color.WHITE);
        g.drawLine(216, 548, 222, 548);
        g.drawLine(232, 548, 238, 548);
        g.setColor(new Color(0x22C55E));
        g.fillOval(244, 534, 6, 6);
        g.setColor(new Color(0xEF4444));
        g.fillOval(270, 534, 6, 6);
    }

    private void paintCar(Graphics2D g, int x, int y) {
        Graphics2D c = (Graphics2D) g.create();
        c.translate(x, y);
        c.setColor(Theme.PRIMARY);
        c.fill(new RoundRectangle2D.Double(0, 14, 108, 34, 20, 20));
        c.setColor(new Color(0x3B82F6));
        c.fill(poly(18, 14, 32, -6, 78, -6, 92, 14));
        c.setColor(new Color(0xBFDBFE));
        c.fill(poly(36, 10, 42, -1, 68, -1, 74, 10));
        c.setColor(new Color(0xFDE68A));
        c.fill(new RoundRectangle2D.Double(6, 24, 16, 8, 6, 6));
        c.fill(new RoundRectangle2D.Double(86, 24, 16, 8, 6, 6));
        c.setColor(new Color(0xFACC15));
        c.fill(new RoundRectangle2D.Double(40, 34, 28, 9, 3, 3));
        c.setColor(new Color(0x111111));
        c.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
        drawCentered(c, "KWY620", 54, 41);
        c.setColor(new Color(0x0F172A));
        c.fill(new RoundRectangle2D.Double(4, 46, 18, 10, 6, 6));
        c.fill(new RoundRectangle2D.Double(86, 46, 18, 10, 6, 6));
        c.dispose();
    }

    private void paintTexts(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(Theme.font(Font.BOLD, 44));
        g.drawString("RutaPeaje", 54, 100);
        g.setColor(Theme.SIDEBAR_MUTED);
        g.setFont(Theme.font(Font.PLAIN, 18));
        g.drawString("Colombia", 54, 130);

        g.setColor(Color.WHITE);
        g.setFont(Theme.font(Font.BOLD, 26));
        g.drawString("Todos los peajes del país", 54, 205);
        g.drawString("en un solo lugar.", 54, 239);
        g.setColor(new Color(0xC7D3EA));
        g.setFont(Theme.font(Font.PLAIN, 15));
        g.drawString("Consulta peajes, tarifas y rutas, pide asistencia en la vía y", 54, 278);
        g.drawString("gestiona la operación de cada peaje.", 54, 303);

        String[][] chips = {{"181", "peajes"}, {"7", "categorías"}, {"24/7", "asistencia"}};
        int x = 54;
        for (String[] chip : chips) {
            g.setFont(Theme.font(Font.PLAIN, 12));
            int w = Math.max(g.getFontMetrics().stringWidth(chip[1]), 40) + 30;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.14f));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(x, 331, w, 60, 22, 22));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(x, 331, w, 60, 22, 22));
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, 20));
            g.drawString(chip[0], x + 14, 361);
            g.setColor(new Color(0xDCE4F2));
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.drawString(chip[1], x + 14, 380);
            x += w + 10;
        }

        g.setColor(new Color(11, 31, 74, 190));
        g.fillRect(0, H - 40, W, 40);
        g.setColor(new Color(0xDCE4F2));
        g.setFont(Theme.font(Font.PLAIN, 13));
        g.drawString("Conectando caminos, impulsando el país", 54, H - 15);
    }

    private static Polygon poly(int... points) {
        Polygon p = new Polygon();
        for (int i = 0; i < points.length; i += 2) {
            p.addPoint(points[i], points[i + 1]);
        }
        return p;
    }

    /** Colina: una curva de dos tramos hasta el borde derecho y cerrada por abajo. */
    private static Path2D hill(double y0, double c1x, double c1y, double mx, double my,
                               double c2x, double c2y, double ex, double ey) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, y0);
        p.quadTo(c1x, c1y, mx, my);
        p.quadTo(c2x, c2y, ex, ey);
        p.lineTo(W, H);
        p.lineTo(0, H);
        p.closePath();
        return p;
    }

    private static void drawCentered(Graphics2D g, String text, int cx, int baseline) {
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
