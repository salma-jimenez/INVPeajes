package com.invpeajes.ui.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;

/**
 * Gráficas sencillas dibujadas con Java2D para las estadísticas del jefe de estación:
 * línea con área (periodo actual contra el anterior), barras verticales y barra apilada.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public final class Charts {

    private Charts() {
    }

    /**
     * Dinero en formato corto para ejes y etiquetas: $12,4 M, $850 K, $900.
     * @param value Valor.
     * @return Texto corto.
     */
    public static String shortMoney(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.forLanguageTag("es-CO"), "$%,.1f M", value / 1_000_000).replace(",0 M", " M");
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "$%.0f K", value / 1_000);
        }
        return String.format(Locale.ROOT, "$%.0f", value);
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Gráfica de línea: periodo actual (azul con área) y periodo anterior (gris punteado). */
    public static class LineChart extends JComponent {

        private List<String> labels = Collections.emptyList();
        private List<BigDecimal> current = Collections.emptyList();
        private List<BigDecimal> previous = Collections.emptyList();
        private int hover = -1;

        /** Crea la gráfica vacía. */
        public LineChart() {
            setPreferredSize(new Dimension(500, 200));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int n = labels.size();
                    if (n == 0) {
                        return;
                    }
                    double step = (getWidth() - 56.0) / Math.max(1, n - 1);
                    int index = (int) Math.round((e.getX() - 46) / step);
                    hover = Math.max(0, Math.min(n - 1, index));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            };
            addMouseMotionListener(mouse);
            addMouseListener(mouse);
        }

        /**
         * Cambia los datos.
         * @param labels Etiquetas del eje horizontal.
         * @param current Valores del periodo (null = todavía no ocurre).
         * @param previous Valores del periodo anterior.
         */
        public void setData(List<String> labels, List<BigDecimal> current, List<BigDecimal> previous) {
            this.labels = labels;
            this.current = current;
            this.previous = previous;
            hover = -1;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int n = labels.size();
            int left = 46;
            int right = 10;
            int top = 12;
            int bottom = 22;
            int w = getWidth() - left - right;
            int h = getHeight() - top - bottom;
            double max = 1;
            for (int i = 0; i < n; i++) {
                if (current.get(i) != null) {
                    max = Math.max(max, current.get(i).doubleValue());
                }
                if (previous.get(i) != null) {
                    max = Math.max(max, previous.get(i).doubleValue());
                }
            }
            max *= 1.12;
            g2.setFont(Theme.font(Font.PLAIN, 10));
            for (int k = 0; k <= 3; k++) {
                int y = top + h - h * k / 3;
                g2.setColor(new Color(0xEEF0F3));
                g2.drawLine(left, y, left + w, y);
                g2.setColor(new Color(0x9CA3AF));
                g2.drawString(shortMoney(max * k / 3), 2, y + 4);
            }
            if (n == 0) {
                g2.dispose();
                return;
            }
            double step = n > 1 ? (double) w / (n - 1) : 0;
            int labelEvery = Math.max(1, (int) Math.ceil(n / 12.0));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < n; i += labelEvery) {
                int x = (int) (left + i * step);
                String text = labels.get(i);
                g2.drawString(text, x - fm.stringWidth(text) / 2, getHeight() - 6);
            }

            Path2D prevLine = path(previous, left, top, h, step, max);
            g2.setColor(new Color(0x9CA3AF));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0f));
            g2.draw(prevLine);

            Path2D line = path(current, left, top, h, step, max);
            int last = lastIndex(current);
            if (last >= 0) {
                Path2D area = new Path2D.Double(line);
                area.lineTo(left + last * step, top + h);
                area.lineTo(left + firstIndex(current) * step, top + h);
                area.closePath();
                g2.setPaint(new GradientPaint(0, top, new Color(37, 99, 235, 90), 0, top + h, new Color(37, 99, 235, 0)));
                g2.fill(area);
            }
            g2.setColor(Theme.PRIMARY);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);

            if (hover >= 0 && hover < n) {
                int x = (int) (left + hover * step);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{2f, 3f}, 0f));
                g2.setColor(Theme.PRIMARY);
                g2.drawLine(x, top, x, top + h);
                BigDecimal value = current.get(hover);
                BigDecimal before = previous.get(hover);
                String l1 = labels.get(hover) + " · " + (value == null ? "—" : shortMoney(value.doubleValue()));
                String l2 = "Anterior: " + (before == null ? "—" : shortMoney(before.doubleValue()));
                g2.setFont(Theme.font(Font.BOLD, 11));
                int boxW = Math.max(g2.getFontMetrics().stringWidth(l1), g2.getFontMetrics().stringWidth(l2)) + 16;
                int bx = x + 8 + boxW > getWidth() ? x - 8 - boxW : x + 8;
                g2.setColor(Theme.SIDEBAR);
                g2.fillRoundRect(bx, top, boxW, 36, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(l1, bx + 8, top + 15);
                g2.setFont(Theme.font(Font.PLAIN, 10));
                g2.setColor(Theme.SIDEBAR_MUTED);
                g2.drawString(l2, bx + 8, top + 29);
            }
            g2.dispose();
        }

        private static Path2D path(List<BigDecimal> values, int left, int top, int h, double step, double max) {
            Path2D p = new Path2D.Double();
            boolean started = false;
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) == null) {
                    continue;
                }
                double x = left + i * step;
                double y = top + h - values.get(i).doubleValue() / max * h;
                if (started) {
                    p.lineTo(x, y);
                } else {
                    p.moveTo(x, y);
                    started = true;
                }
            }
            return p;
        }

        private static int lastIndex(List<BigDecimal> values) {
            for (int i = values.size() - 1; i >= 0; i--) {
                if (values.get(i) != null) {
                    return i;
                }
            }
            return -1;
        }

        private static int firstIndex(List<BigDecimal> values) {
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) != null) {
                    return i;
                }
            }
            return 0;
        }
    }

    /** Barras verticales con el porcentaje encima y la etiqueta debajo. */
    public static class BarChart extends JComponent {

        private final List<String> labels = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();
        private final List<Color> colors = new ArrayList<>();

        /** Crea la gráfica vacía. */
        public BarChart() {
            setPreferredSize(new Dimension(300, 170));
        }

        /**
         * Cambia los datos.
         * @param newLabels Etiquetas (ej. I, II, III...).
         * @param newValues Valores.
         * @param newColors Color de cada barra.
         */
        public void setData(List<String> newLabels, List<Double> newValues, List<Color> newColors) {
            labels.clear();
            labels.addAll(newLabels);
            values.clear();
            values.addAll(newValues);
            colors.clear();
            colors.addAll(newColors);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int n = labels.size();
            if (n == 0) {
                g2.dispose();
                return;
            }
            double total = values.stream().mapToDouble(Double::doubleValue).sum();
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            int gap = 10;
            int barW = (getWidth() - gap * (n - 1)) / n;
            int top = 18;
            int bottom = 18;
            int h = getHeight() - top - bottom;
            for (int i = 0; i < n; i++) {
                int x = i * (barW + gap);
                double v = values.get(i);
                int bh = max <= 0 ? 0 : (int) Math.max(v > 0 ? 3 : 2, v / max * h);
                g2.setColor(v > 0 ? colors.get(i) : new Color(0xE5E7EB));
                g2.fillRoundRect(x, top + h - bh, barW, bh + 6, 8, 8);
                g2.setColor(Theme.BACKGROUND);
                g2.fillRect(x, top + h, barW, 6);
                g2.setFont(Theme.font(Font.BOLD, 11));
                g2.setColor(Theme.TEXT);
                String pct = total <= 0 ? "0 %" : Math.round(v * 100 / total) + " %";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(pct, x + (barW - fm.stringWidth(pct)) / 2, top + h - bh - 4);
                g2.setFont(Theme.font(Font.PLAIN, 11));
                g2.setColor(Theme.MUTED);
                fm = g2.getFontMetrics();
                g2.drawString(labels.get(i), x + (barW - fm.stringWidth(labels.get(i))) / 2, getHeight() - 3);
            }
            g2.dispose();
        }
    }

    /** Barra horizontal dividida en partes de colores (reparto de un total). */
    public static class StackedBar extends JComponent {

        private final List<Double> values = new ArrayList<>();
        private final List<Color> colors = new ArrayList<>();

        /** Crea la barra vacía. */
        public StackedBar() {
            setPreferredSize(new Dimension(200, 14));
            setMinimumSize(new Dimension(50, 14));
        }

        /**
         * Cambia los datos.
         * @param newValues Valores de cada parte.
         * @param newColors Color de cada parte.
         */
        public void setData(List<Double> newValues, List<Color> newColors) {
            values.clear();
            values.addAll(newValues);
            colors.clear();
            colors.addAll(newColors);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int h = getHeight();
            g2.setColor(new Color(0xEEF0F3));
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            double total = values.stream().mapToDouble(Double::doubleValue).sum();
            if (total > 0) {
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), h, h, h));
                double x = 0;
                for (int i = 0; i < values.size(); i++) {
                    double w = values.get(i) / total * getWidth();
                    g2.setColor(colors.get(i));
                    g2.fill(new java.awt.geom.Rectangle2D.Double(x, 0, w + 0.5, h));
                    x += w;
                }
            }
            g2.dispose();
        }
    }
}
