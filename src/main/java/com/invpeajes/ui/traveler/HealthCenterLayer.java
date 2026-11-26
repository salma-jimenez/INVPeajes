package com.invpeajes.ui.traveler;

import com.invpeajes.model.HealthCenter;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Capa del mapa que dibuja los centros de salud con un pin rosado de cruz roja.
 * La punta del pin cae exactamente en la coordenada de la sede.
 *
 * <ul>
 *   <li>Lejos (vista de país) el pin es pequeño; al acercarse crece.</li>
 *   <li>Las sedes con ubicación aproximada (centro del municipio) se ven semitransparentes.</li>
 *   <li>{@link #centerAt(JXMapViewer, Point)} dice qué sede hay bajo el mouse, para el
 *       tooltip o el clic.</li>
 * </ul>
 *
 * Se agrega al mapa junto con los peajes usando un {@code CompoundPainter}.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class HealthCenterLayer implements Painter<JXMapViewer> {

    private static final Color PIN_FILL = new Color(246, 158, 160);
    private static final Color PIN_SHINE = new Color(250, 196, 197);
    private static final Color OUTLINE = new Color(38, 30, 34);
    private static final Color CIRCLE_FILL = new Color(250, 250, 250);
    private static final Color CROSS_FILL = new Color(238, 74, 92);

    /** En JXMapViewer un zoom MAYOR es más lejos. Desde este zoom se usa el pin pequeño. */
    private static final int ZOOM_FAR = 11;
    private static final int ZOOM_MEDIUM = 8;
    private static final int SMALL_HEIGHT = 16;
    private static final int MEDIUM_HEIGHT = 24;
    private static final int LARGE_HEIGHT = 34;
    private static final float APPROX_ALPHA = 0.55f;

    private final BufferedImage smallPin = createPin(SMALL_HEIGHT);
    private final BufferedImage mediumPin = createPin(MEDIUM_HEIGHT);
    private final BufferedImage largePin = createPin(LARGE_HEIGHT);

    private final List<HealthCenter> centers = new ArrayList<>();
    private boolean visible = true;

    /**
     * Reemplaza las sedes que se dibujan.
     *
     * @param newCenters sedes a mostrar (null equivale a ninguna)
     */
    public void setCenters(List<HealthCenter> newCenters) {
        centers.clear();
        if (newCenters != null) {
            centers.addAll(newCenters);
        }
    }

    /** @return cantidad de sedes cargadas en la capa */
    public int getCount() {
        return centers.size();
    }

    /**
     * Muestra u oculta la capa sin borrar las sedes cargadas.
     *
     * @param visible true para dibujarla
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /** @return true si la capa se está dibujando */
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (!visible || centers.isEmpty()) {
            return;
        }
        BufferedImage pin = pinForZoom(map.getZoom());
        int w = pin.getWidth();
        int h = pin.getHeight();
        Rectangle view = map.getViewportBounds();

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(-view.x, -view.y);
            // Primero las aproximadas (debajo) y luego las exactas (encima)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, APPROX_ALPHA));
            drawPins(g2, map, view, pin, w, h, false);
            g2.setComposite(AlphaComposite.SrcOver);
            drawPins(g2, map, view, pin, w, h, true);
        } finally {
            g2.dispose();
        }
    }

    private void drawPins(Graphics2D g, JXMapViewer map, Rectangle view, BufferedImage pin,
                          int w, int h, boolean exact) {
        for (HealthCenter c : centers) {
            if (c.hasExactLocation() != exact) {
                continue;
            }
            Point2D px = toPixel(map, c);
            int x = (int) px.getX() - w / 2;
            int y = (int) px.getY() - h;
            if (x + w < view.x || x > view.x + view.width || y + h < view.y || y > view.y + view.height) {
                continue;
            }
            g.drawImage(pin, x, y, null);
        }
    }

    /**
     * Sede cuyo pin está bajo un punto de la pantalla.
     *
     * @param map     visor del mapa
     * @param screen punto en coordenadas del componente (por ejemplo, del MouseEvent)
     * @return la sede encontrada, o null si no hay ninguna
     */
    public HealthCenter centerAt(JXMapViewer map, Point screen) {
        if (!visible || centers.isEmpty()) {
            return null;
        }
        BufferedImage pin = pinForZoom(map.getZoom());
        int w = pin.getWidth();
        int h = pin.getHeight();
        Rectangle view = map.getViewportBounds();
        HealthCenter found = null;
        // Se recorre al revés para devolver el pin que se dibujó encima
        for (int i = centers.size() - 1; i >= 0; i--) {
            HealthCenter c = centers.get(i);
            Point2D px = toPixel(map, c);
            double x = px.getX() - view.x;
            double y = px.getY() - view.y;
            Rectangle area = new Rectangle((int) x - w / 2, (int) y - h, w, h);
            if (area.contains(screen)) {
                if (c.hasExactLocation()) {
                    return c;
                }
                if (found == null) {
                    found = c;
                }
            }
        }
        return found;
    }

    /**
     * Texto para el tooltip de una sede.
     *
     * @param c sede
     * @return texto HTML con nombre, tipo, municipio y aviso si la ubicación es aproximada
     */
    public static String tooltipText(HealthCenter c) {
        String note = c.hasExactLocation() ? "" : "<br><i>Ubicación aproximada (centro del municipio)</i>";
        return "<html><b>" + c.getName() + "</b><br>" + c.getType() + " · "
                + c.getMunicipality() + ", " + c.getDepartment() + note + "</html>";
    }

    private Point2D toPixel(JXMapViewer map, HealthCenter c) {
        return map.getTileFactory().geoToPixel(new GeoPosition(c.getLatitude(), c.getLongitude()), map.getZoom());
    }

    private BufferedImage pinForZoom(int zoom) {
        if (zoom >= ZOOM_FAR) {
            return smallPin;
        }
        return zoom >= ZOOM_MEDIUM ? mediumPin : largePin;
    }

    /**
     * Dibuja una vez el pin en una imagen para reutilizarla en cada marcador.
     * Forma: gota rosada con borde oscuro, círculo blanco y cruz roja en el centro.
     */
    private static BufferedImage createPin(int height) {
        float stroke = Math.max(1.1f, height / 22f);
        double pad = stroke;
        double r = height * 0.36;                 // radio de la cabeza del pin
        double cx = r + pad;
        double cy = r + pad;
        double tipY = height - pad;               // la punta toca el borde inferior
        int width = (int) Math.ceil(2 * (r + pad));

        // Cabeza (círculo) + triángulo tangente hasta la punta
        double d = tipY - cy;
        double alpha = Math.acos(r / d);
        Path2D cone = new Path2D.Double();
        cone.moveTo(cx - r * Math.sin(alpha), cy + r * Math.cos(alpha));
        cone.lineTo(cx, tipY);
        cone.lineTo(cx + r * Math.sin(alpha), cy + r * Math.cos(alpha));
        cone.closePath();
        Area drop = new Area(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        drop.add(new Area(cone));

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.setColor(PIN_FILL);
        g.fill(drop);
        // Brillo suave en el lado izquierdo, como en el ícono de referencia
        Area shine = new Area(drop);
        shine.subtract(new Area(new Ellipse2D.Double(cx - r * 0.75, cy - r * 1.1, r * 2.4, height)));
        g.setColor(PIN_SHINE);
        g.fill(shine);
        g.setColor(OUTLINE);
        g.draw(drop);

        // Círculo blanco
        double rc = r * 0.68;
        Ellipse2D circle = new Ellipse2D.Double(cx - rc, cy - rc, 2 * rc, 2 * rc);
        g.setColor(CIRCLE_FILL);
        g.fill(circle);
        g.setColor(OUTLINE);
        g.draw(circle);

        // Cruz roja
        double arm = rc * 0.62;
        double thickness = rc * 0.42;
        Area cross = new Area(new RoundRectangle2D.Double(cx - thickness / 2, cy - arm, thickness, 2 * arm,
                thickness * 0.3, thickness * 0.3));
        cross.add(new Area(new RoundRectangle2D.Double(cx - arm, cy - thickness / 2, 2 * arm, thickness,
                thickness * 0.3, thickness * 0.3)));
        g.setColor(CROSS_FILL);
        g.fill(cross);
        g.setStroke(new BasicStroke(stroke * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(OUTLINE);
        g.draw(cross);

        g.dispose();
        return img;
    }

    /**
     * Imagen del pin para usarla en leyendas o botones.
     *
     * @return pin de tamaño mediano
     */
    public BufferedImage getIcon() {
        return mediumPin;
    }
}
