package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

/**
 * Mapa de Colombia (OpenStreetMap, con JXMapViewer2) con un marcador por peaje.
 *
 * <ul>
 *   <li>Arrastrar mueve el mapa; la rueda o los botones +/− hacen zoom.</li>
 *   <li>El color del marcador dice quién administra el peaje (ANI, INVÍAS o territorial);
 *       el borde rojo indica que no está operativo.</li>
 *   <li>Clic en un marcador avisa a {@link #setOnSelect}.</li>
 *   <li>Con {@link #setOnPick} activo, un clic fuera de los marcadores marca un punto
 *       (se usa para decir dónde está el viajero al pedir asistencia).</li>
 *   <li>{@link #showRoute} dibuja la línea de un viaje.</li>
 * </ul>
 *
 * @author Salma Jiménez Vega
 * @version 2.0
 */
public class TollMap extends JPanel {

    /** Centro aproximado de Colombia. */
    private static final GeoPosition COLOMBIA = new GeoPosition(4.6, -74.1);
    /** En JXMapViewer un número de zoom MAYOR es más lejos. */
    private static final int START_ZOOM = 12;
    private static final int DETAIL_ZOOM = 8;
    private static final int RADIUS = 7;
    private static final Color ROUTE = new Color(0x0F2A5F);

    private final JXMapViewer viewer;
    private final WaypointPainter<TollPoint> painter = new WaypointPainter<>();
    private final List<TollPoint> points = new ArrayList<>();
    private final List<GeoPosition> route = new ArrayList<>();
    private final JLabel counter = TravelerUi.label(" ", Font.PLAIN, 11, Theme.MUTED);
    private Toll selected;
    private GeoPosition picked;
    private Consumer<Toll> onSelect = t -> { };
    private Consumer<GeoPosition> onPick;
    /** true si se pidió encuadrar antes de que el mapa tuviera tamaño. */
    private boolean fitPending;

    /** Crea el mapa centrado en Colombia. */
    public TollMap() {
        super(new BorderLayout());
        viewer = createViewer();
        add(viewer, BorderLayout.CENTER);
        setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        viewer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (fitPending && viewer.getWidth() > 0) {
                    fitPending = false;
                    if (route.size() >= 2) {
                        fit(new HashSet<>(route));
                    } else {
                        fitAll();
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private JXMapViewer createViewer() {
        JXMapViewer map = new JXMapViewer() {
            @Override
            public String getToolTipText(MouseEvent e) {
                TollPoint p = pointAt(e.getPoint());
                return p == null ? null : p.toll.getName() + " · " + TravelerUi.levelText(p.toll.getLevel())
                        + " · " + TravelerUi.pretty(p.toll.getOperationalStatus());
            }

            @Override
            public void doLayout() {
                for (Component c : getComponents()) {
                    Dimension d = c.getPreferredSize();
                    if ("zoom".equals(c.getName())) {
                        c.setBounds(12, 12, d.width, d.height);
                    } else if ("legend".equals(c.getName())) {
                        c.setBounds(12, getHeight() - d.height - 12, d.width, d.height);
                    }
                }
            }
        };
        map.setLayout(null);
        map.setToolTipText("");

        // Teselas de OpenStreetMap, guardadas en disco para no descargarlas cada vez
        DefaultTileFactory tiles = new DefaultTileFactory(new OSMTileFactoryInfo());
        tiles.setThreadPoolSize(4);
        tiles.setUserAgent("RutaPeaje-ProyectoAula/1.0 (Java Swing; JXMapViewer2)");
        File cache = new File(System.getProperty("user.home"), ".rutapeaje" + File.separator + "teselas");
        tiles.setLocalCache(new FileBasedLocalCache(cache, false));
        map.setTileFactory(tiles);
        map.setAddressLocation(COLOMBIA);
        map.setZoom(START_ZOOM);

        MouseInputListener pan = new PanMouseInputListener(map);
        map.addMouseListener(pan);
        map.addMouseMotionListener(pan);
        map.addMouseWheelListener(new ZoomMouseWheelListenerCursor(map));
        map.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                TollPoint p = pointAt(e.getPoint());
                if (p != null) {
                    select(p.toll, false);
                    onSelect.accept(p.toll);
                } else if (onPick != null) {
                    Rectangle view = map.getViewportBounds();
                    GeoPosition pos = map.getTileFactory().pixelToGeo(
                            new Point2D.Double(view.x + e.getX(), view.y + e.getY()), map.getZoom());
                    setPicked(pos);
                    onPick.accept(pos);
                }
            }
        });

        painter.setRenderer((Graphics2D g, JXMapViewer m, TollPoint point) -> {
            Point2D px = m.getTileFactory().geoToPixel(point.getPosition(), m.getZoom());
            int x = (int) px.getX();
            int y = (int) px.getY();
            boolean isSelected = selected != null && selected.getTollId() == point.toll.getTollId();
            int r = isSelected ? RADIUS + 4 : RADIUS;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected) {
                g.setColor(new Color(37, 99, 235, 70));
                g.fillOval(x - r - 6, y - r - 6, (r + 6) * 2, (r + 6) * 2);
            }
            g.setColor(TravelerUi.levelColor(point.toll.getLevel()));
            g.fillOval(x - r, y - r, r * 2, r * 2);
            boolean operating = TravelerUi.isOperating(point.toll);
            g.setColor(operating ? Color.WHITE : Theme.DANGER);
            g.setStroke(new BasicStroke(operating ? 2f : 3f));
            g.drawOval(x - r, y - r, r * 2, r * 2);
        });
        // Primero la ruta y el punto marcado, encima los peajes
        Painter<JXMapViewer> extras = (Graphics2D g, JXMapViewer m, int w, int h) -> paintExtras(g, m);
        map.setOverlayPainter(new CompoundPainter<>(extras, painter));

        map.add(zoomButtons(map));
        map.add(legend());
        return map;
    }

    private JPanel zoomButtons(JXMapViewer map) {
        JPanel zoom = new JPanel(new GridLayout(2, 1, 0, 4));
        zoom.setName("zoom");
        zoom.setOpaque(false);
        FlatButton in = Theme.secondaryButton("+");
        FlatButton out = Theme.secondaryButton("−");
        for (FlatButton b : new FlatButton[]{in, out}) {
            b.setFont(Theme.font(Font.BOLD, 16));
            b.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            b.setPreferredSize(new Dimension(36, 34));
        }
        in.setToolTipText("Acercar");
        out.setToolTipText("Alejar");
        in.addActionListener(e -> map.setZoom(Math.max(1, map.getZoom() - 1)));
        out.addActionListener(e -> map.setZoom(Math.min(15, map.getZoom() + 1)));
        zoom.add(in);
        zoom.add(out);
        return zoom;
    }

    private JPanel legend() {
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setName("legend");
        legend.setBackground(new Color(255, 255, 255, 235));
        legend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 12)));
        legend.add(legendItem(TravelerUi.COLOR_ANI, "ANI"));
        legend.add(legendItem(TravelerUi.COLOR_INVIAS, "INVÍAS"));
        legend.add(legendItem(TravelerUi.COLOR_TERRITORIAL, "Departamental / territorial"));
        legend.add(legendItem(TravelerUi.COLOR_UNKNOWN, "Sin dato"));
        legend.add(TravelerUi.label("<html><span style='color:#dc2626'>○</span>&nbsp;Borde rojo: no operativo</html>",
                Font.PLAIN, 11, Theme.TEXT));
        legend.add(counter);
        return legend;
    }

    private JLabel legendItem(Color color, String text) {
        return TravelerUi.label("<html><span style='color:" + String.format("#%06x", color.getRGB() & 0xFFFFFF)
                + "'>●</span>&nbsp;" + text + "</html>", Font.PLAIN, 11, Theme.TEXT);
    }

    // ------------------------------------------------------------------ API

    /**
     * @param action qué hacer cuando se hace clic en un peaje
     */
    public void setOnSelect(Consumer<Toll> action) {
        this.onSelect = action == null ? t -> { } : action;
    }

    /**
     * Activa el modo "marcar un punto": un clic fuera de los peajes marca la posición.
     *
     * @param action qué hacer con el punto marcado, o null para desactivarlo
     */
    public void setOnPick(Consumer<GeoPosition> action) {
        this.onPick = action;
    }

    /**
     * @param tolls peajes que se dibujan (por ejemplo, los que pasan los filtros)
     */
    public void showTolls(List<Toll> tolls) {
        points.clear();
        for (Toll t : tolls) {
            points.add(new TollPoint(t));
        }
        painter.setWaypoints(new HashSet<>(points));
        counter.setText(tolls.size() + (tolls.size() == 1 ? " peaje en el mapa" : " peajes en el mapa"));
        viewer.repaint();
    }

    /** Ajusta el zoom para ver todos los peajes dibujados. */
    public void fitAll() {
        if (viewer.getWidth() == 0) {
            fitPending = true;
            return;
        }
        if (points.isEmpty()) {
            viewer.setAddressLocation(COLOMBIA);
            viewer.setZoom(START_ZOOM);
            return;
        }
        Set<GeoPosition> positions = new HashSet<>();
        for (TollPoint p : points) {
            positions.add(p.getPosition());
        }
        fit(positions);
    }

    /**
     * Escoge el zoom más cercano en el que caben todos los puntos y centra el mapa en ellos.
     * (Se calcula aquí porque zoomToBestFit de la librería no siempre acierta.)
     */
    private void fit(Set<GeoPosition> positions) {
        if (positions.size() == 1) {
            viewer.setZoom(DETAIL_ZOOM);
            viewer.setAddressLocation(positions.iterator().next());
            return;
        }
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (GeoPosition p : positions) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }
        GeoPosition corner1 = new GeoPosition(maxLat, minLon);
        GeoPosition corner2 = new GeoPosition(minLat, maxLon);
        double usableW = viewer.getWidth() * 0.95;
        double usableH = viewer.getHeight() * 0.95;
        int best = START_ZOOM + 3;
        for (int zoom = 1; zoom <= START_ZOOM + 3; zoom++) {
            Point2D a = viewer.getTileFactory().geoToPixel(corner1, zoom);
            Point2D b = viewer.getTileFactory().geoToPixel(corner2, zoom);
            if (Math.abs(b.getX() - a.getX()) <= usableW && Math.abs(b.getY() - a.getY()) <= usableH) {
                best = zoom;
                break;
            }
        }
        viewer.setZoom(best);
        viewer.setAddressLocation(new GeoPosition((minLat + maxLat) / 2, (minLon + maxLon) / 2));
    }

    /**
     * Resalta un peaje.
     *
     * @param toll   peaje, o null para quitar el resaltado
     * @param center true para mover el mapa hasta él
     */
    public void select(Toll toll, boolean center) {
        this.selected = toll;
        if (toll != null && center) {
            viewer.setZoom(Math.min(viewer.getZoom(), DETAIL_ZOOM));
            viewer.setAddressLocation(new GeoPosition(toll.getLatitude(), toll.getLongitude()));
        }
        viewer.repaint();
    }

    /**
     * Marca (o quita) el punto donde está el viajero.
     *
     * @param position posición, o null para quitarla
     */
    public void setPicked(GeoPosition position) {
        this.picked = position;
        viewer.repaint();
    }

    /**
     * Dibuja un viaje como una línea entre los puntos (origen, peajes y destino).
     *
     * @param path puntos en orden; vacía para quitar la línea
     */
    public void showRoute(List<GeoPosition> path) {
        route.clear();
        route.addAll(path);
        if (route.size() >= 2) {
            if (viewer.getWidth() == 0) {
                fitPending = true;
            } else {
                fit(new HashSet<>(route));
            }
        }
        viewer.repaint();
    }

    private void paintExtras(Graphics2D g0, JXMapViewer m) {
        Graphics2D g = (Graphics2D) g0.create();
        Rectangle view = m.getViewportBounds();
        g.translate(-view.x, -view.y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (route.size() >= 2) {
            Path2D line = new Path2D.Double();
            for (int i = 0; i < route.size(); i++) {
                Point2D p = m.getTileFactory().geoToPixel(route.get(i), m.getZoom());
                if (i == 0) {
                    line.moveTo(p.getX(), p.getY());
                } else {
                    line.lineTo(p.getX(), p.getY());
                }
            }
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
            g.setColor(ROUTE);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
            for (GeoPosition end : new GeoPosition[]{route.get(0), route.get(route.size() - 1)}) {
                Point2D p = m.getTileFactory().geoToPixel(end, m.getZoom());
                int x = (int) p.getX();
                int y = (int) p.getY();
                g.setColor(ROUTE);
                g.fillRect(x - 9, y - 9, 18, 18);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.5f));
                g.drawRect(x - 9, y - 9, 18, 18);
            }
        }
        if (picked != null) {
            Point2D p = m.getTileFactory().geoToPixel(picked, m.getZoom());
            int x = (int) p.getX();
            int y = (int) p.getY();
            // Alfiler: círculo rojo con punta
            Path2D pin = new Path2D.Double();
            pin.moveTo(x, y);
            pin.lineTo(x - 7, y - 14);
            pin.lineTo(x + 7, y - 14);
            pin.closePath();
            g.setColor(Theme.DANGER);
            g.fill(pin);
            g.fillOval(x - 10, y - 30, 20, 20);
            g.setColor(Color.WHITE);
            g.fillOval(x - 4, y - 24, 8, 8);
        }
        g.dispose();
    }

    /** Peaje más cercano a un punto de la pantalla, o null si no hay ninguno cerca. */
    private TollPoint pointAt(Point screen) {
        Rectangle view = viewer.getViewportBounds();
        TollPoint nearest = null;
        double best = (RADIUS + 4) * (RADIUS + 4);
        for (TollPoint p : points) {
            Point2D px = viewer.getTileFactory().geoToPixel(p.getPosition(), viewer.getZoom());
            double dx = px.getX() - view.getX() - screen.x;
            double dy = px.getY() - view.getY() - screen.y;
            double d = dx * dx + dy * dy;
            if (d <= best) {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    /** Adaptador entre un peaje del modelo y un Waypoint de JXMapViewer. */
    private static final class TollPoint implements Waypoint {
        private final Toll toll;
        private final GeoPosition position;

        TollPoint(Toll toll) {
            this.toll = toll;
            this.position = new GeoPosition(toll.getLatitude(), toll.getLongitude());
        }

        @Override
        public GeoPosition getPosition() {
            return position;
        }
    }
}
