package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.service.RouteService;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.service.TravelerService.TripPlan;
import com.invpeajes.service.TravelerService.TripStop;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.traveler.TravelerUi.City;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Planificar un viaje: el viajero escoge origen, destino (y si quiere una ruta) y la
 * categoría de su vehículo; el programa muestra el total a pagar, los peajes que va a
 * pasar en orden (como un itinerario) y el recorrido en el mapa.
 *
 * <p>Distribución: a la izquierda una columna de 340 px (formulario, resumen e
 * itinerario) y a la derecha el mapa a toda la altura, igual que en "Mapa de peajes".
 * Al escoger un peaje (en la lista o en el mapa) aparece su ficha sobre el mapa.</p>
 *
 * @author Salma Jiménez Vega
 * @version 3.0
 */
public class TripPanel extends JPanel {

    /**
     * Sin ruta por carretera: distancia máxima a la línea recta origen–destino para que un
     * peaje cuente como "del camino".
     */
    private static final double CORRIDOR_KM = 25;
    /** Con ruta por carretera: distancia máxima del peaje a la vía, en metros. */
    private static final double ROAD_MATCH_METERS = 300;
    private static final String NOTE_INITIAL = "<html>La ruta se calcula por carretera con OpenStreetMap."
            + " Si el servicio de rutas no responde, se usa una ruta aproximada en línea recta.</html>";
    private static final String NOTE_ROAD = "<html>Ruta por carretera calculada con OpenStreetMap (OSRM)."
            + " La duración es estimada y no tiene en cuenta el tráfico.</html>";
    private static final String NOTE_APPROX = "<html>El servicio de rutas no respondió:"
            + " se tomaron los peajes cercanos a la línea entre origen y destino, así que el resultado"
            + " es aproximado.</html>";
    private static final int RADIUS = 12;

    private final TravelerService service;
    private final Consumer<Toll> onShowOnMap;
    private final JComboBox<City> origin = TravelerUi.combo(City.MAIN);
    private final JComboBox<City> destination = TravelerUi.combo(City.MAIN);
    private final JComboBox<Object> route = new JComboBox<>();
    private final JComboBox<VehicleCategory> category = new JComboBox<>();
    private final FlatButton calculate = Theme.primaryButton("Calcular viaje");
    private final TollMap map = new TollMap();

    // Resumen
    private final JLabel summaryCaption = label("Total a pagar", Font.PLAIN, 12, Theme.SIDEBAR_MUTED);
    private final JLabel summaryTotal = label("—", Font.BOLD, 30, Color.WHITE);
    private final JLabel summaryCount = label(" ", Font.PLAIN, 13, Theme.SIDEBAR_TEXT);
    private final JLabel summaryKm = label(" ", Font.PLAIN, 13, Theme.SIDEBAR_TEXT);

    // Itinerario
    private final JPanel itinerary = new JPanel();
    private final JLabel note = label(" ", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel methodNote = label(NOTE_INITIAL, Font.PLAIN, 11, Theme.MUTED);
    private final List<StopRow> rows = new ArrayList<>();

    // Ficha flotante del peaje escogido
    private final RoundPanel tollCard = new RoundPanel(Color.WHITE, Theme.BORDER);
    private final JLabel cardName = label(" ", Font.BOLD, 15, Theme.TEXT);
    private final JLabel cardInfo = label(" ", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel cardCategory = label(" ", Font.PLAIN, 13, Theme.TEXT);
    private final JLabel cardPrice = label(" ", Font.BOLD, 13, Theme.TEXT);
    private Toll cardToll;

    private Catalog catalog;
    private TripPlan plan;
    private VehicleCategory planCategory;

    /**
     * @param service     servicio del viajero
     * @param onShowOnMap abre un peaje en la pantalla "Mapa de peajes" (botón de la ficha)
     */
    public TripPanel(TravelerService service, Consumer<Toll> onShowOnMap) {
        super(new BorderLayout(14, 0));
        this.service = service;
        this.onShowOnMap = onShowOnMap == null ? t -> { } : onShowOnMap;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        destination.setSelectedItem(City.MAIN.get(9)); // Medellín
        origin.setSelectedItem(City.MAIN.get(2));      // Bogotá
        TravelerUi.styleCombo(route);
        TravelerUi.styleCombo(category);
        useShortCategoryText(category);
        calculate.addActionListener(e -> calculate());

        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(340, 0));
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(buildForm(), BorderLayout.NORTH);
        top.add(buildSummary(), BorderLayout.CENTER);
        side.add(top, BorderLayout.NORTH);
        side.add(buildItinerary(), BorderLayout.CENTER);

        map.setOnSelect(this::selectToll);
        add(side, BorderLayout.WEST);
        add(buildMapArea(), BorderLayout.CENTER);
        fillItinerary();
    }

    // ------------------------------------------------------------------ Partes de la pantalla

    /** Formulario compacto: origen y destino en una fila, ruta y categoría en otra. */
    private JComponent buildForm() {
        RoundPanel form = new RoundPanel(Color.WHITE, Theme.BORDER);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        form.add(pair(TravelerUi.field("Origen", origin), TravelerUi.field("Destino", destination)));
        form.add(Box.createVerticalStrut(10));
        form.add(pair(TravelerUi.field("Ruta (opcional)", route), TravelerUi.field("Categoría", category)));
        form.add(Box.createVerticalStrut(12));
        calculate.setAlignmentX(LEFT_ALIGNMENT);
        calculate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(calculate);
        return form;
    }

    /** Tarjeta azul oscuro con el total grande y, a la derecha, peajes y km. */
    private JComponent buildSummary() {
        RoundPanel card = new RoundPanel(Theme.SIDEBAR, null);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(summaryCaption, BorderLayout.NORTH);
        left.add(summaryTotal, BorderLayout.CENTER);
        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        summaryCount.setHorizontalAlignment(SwingConstants.RIGHT);
        summaryKm.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(summaryCount);
        right.add(summaryKm);
        JPanel rightHolder = new JPanel(new BorderLayout());
        rightHolder.setOpaque(false);
        rightHolder.add(right, BorderLayout.SOUTH);
        card.add(left, BorderLayout.CENTER);
        card.add(rightHolder, BorderLayout.EAST);
        return card;
    }

    /** Lista del viaje: origen, cada peaje con su precio y destino. */
    private JComponent buildItinerary() {
        itinerary.setLayout(new BoxLayout(itinerary, BoxLayout.Y_AXIS));
        itinerary.setBackground(Color.WHITE);
        itinerary.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Color.WHITE);
        holder.add(itinerary, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        RoundPanel box = new RoundPanel(Color.WHITE, Theme.BORDER);
        box.setLayout(new BorderLayout());
        box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        box.add(scroll, BorderLayout.CENTER);

        JPanel notes = new JPanel();
        notes.setLayout(new BoxLayout(notes, BoxLayout.Y_AXIS));
        notes.setOpaque(false);
        notes.add(left(note));
        notes.add(left(methodNote));

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(box, BorderLayout.CENTER);
        panel.add(notes, BorderLayout.SOUTH);
        return panel;
    }

    /** El mapa ocupa todo el espacio; la ficha del peaje flota arriba a la derecha. */
    private JComponent buildMapArea() {
        tollCard.setLayout(new BoxLayout(tollCard, BoxLayout.Y_AXIS));
        tollCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel priceRow = new JPanel(new BorderLayout());
        priceRow.setOpaque(false);
        priceRow.add(cardCategory, BorderLayout.WEST);
        priceRow.add(cardPrice, BorderLayout.EAST);
        priceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        FlatButton open = Theme.secondaryButton("Ver en Mapa de peajes →");
        open.setFont(Theme.font(Font.BOLD, 13));
        open.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        open.addActionListener(e -> {
            if (cardToll != null) {
                onShowOnMap.accept(cardToll);
            }
        });
        JLabel close = label("✕", Font.PLAIN, 13, Theme.MUTED);
        close.setToolTipText("Cerrar");
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectToll(null);
            }
        });
        JPanel head = new JPanel(new BorderLayout(8, 0));
        head.setOpaque(false);
        head.add(cardName, BorderLayout.CENTER);
        head.add(close, BorderLayout.EAST);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        tollCard.add(left(head));
        tollCard.add(left(cardInfo));
        tollCard.add(Box.createVerticalStrut(8));
        tollCard.add(left(priceRow));
        tollCard.add(Box.createVerticalStrut(8));
        tollCard.add(left(open));
        tollCard.setVisible(false);

        JLayeredPane layers = new JLayeredPane() {
            @Override
            public void doLayout() {
                map.setBounds(0, 0, getWidth(), getHeight());
                Dimension d = tollCard.getPreferredSize();
                int w = Math.max(250, d.width);
                tollCard.setBounds(getWidth() - w - 14, 14, w, d.height);
            }
        };
        layers.add(map, JLayeredPane.DEFAULT_LAYER);
        layers.add(tollCard, JLayeredPane.PALETTE_LAYER);
        return layers;
    }

    // ------------------------------------------------------------------ Datos

    /**
     * @param catalog catálogo cargado
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        category.removeAllItems();
        for (VehicleCategory c : catalog.getCategories()) {
            category.addItem(c);
        }
        route.removeAllItems();
        route.addItem("Cualquiera");
        List<String> used = new ArrayList<>();
        for (Toll t : catalog.getTolls()) {
            if (t.getRouteId() != null && !used.contains(t.getRouteId())) {
                used.add(t.getRouteId());
            }
        }
        List<RouteOption> options = new ArrayList<>();
        for (String id : used) {
            options.add(new RouteOption(id, catalog.routeName(id)));
        }
        options.sort((a, b) -> TravelerUi.normalize(a.name).compareTo(TravelerUi.normalize(b.name)));
        options.forEach(route::addItem);
        map.showTolls(catalog.getTolls());
    }

    /**
     * Escoge la categoría (por ejemplo, la del vehículo del viajero).
     *
     * @param categoryId categoría
     */
    public void selectCategory(int categoryId) {
        for (int i = 0; i < category.getItemCount(); i++) {
            if (category.getItemAt(i).getCategoryId() == categoryId) {
                category.setSelectedIndex(i);
                return;
            }
        }
    }

    private void calculate() {
        City from = (City) origin.getSelectedItem();
        City to = (City) destination.getSelectedItem();
        VehicleCategory cat = (VehicleCategory) category.getSelectedItem();
        if (catalog == null || from == null || to == null || cat == null) {
            return;
        }
        if (from == to) {
            summaryCaption.setText("El origen y el destino son iguales");
            return;
        }
        Object r = route.getSelectedItem();
        String routeId = r instanceof RouteOption ? ((RouteOption) r).id : null;
        calculate.setEnabled(false);
        calculate.setText("Calculando...");
        Async.run(() -> {
            Map<Integer, BigDecimal> prices = service.pricesFor(cat.getCategoryId());
            // Primero la ruta real por carretera; si el servicio no responde, el cálculo aproximado
            RouteService.RoadRoute road = RouteService.findRoute(from.getLatitude(), from.getLongitude(),
                    to.getLatitude(), to.getLongitude());
            if (road != null) {
                return TravelerService.planTripOnRoad(catalog.getTolls(), prices, road, ROAD_MATCH_METERS, routeId);
            }
            return TravelerService.planTrip(catalog.getTolls(), prices, from.getLatitude(), from.getLongitude(),
                    to.getLatitude(), to.getLongitude(), CORRIDOR_KM, routeId);
        }, result -> {
            calculate.setEnabled(true);
            calculate.setText("Calcular viaje");
            show(result, cat, from, to);
        }, error -> {
            calculate.setEnabled(true);
            calculate.setText("Calcular viaje");
            summaryCaption.setText("No se pudo calcular: " + error.getMessage());
        });
    }

    private void show(TripPlan result, VehicleCategory cat, City from, City to) {
        this.plan = result;
        this.planCategory = cat;
        int n = result.getStops().size();
        summaryCaption.setText("Total a pagar · " + from.getName() + " → " + to.getName());
        summaryTotal.setText(Theme.money(result.getTotal()));
        summaryCount.setText("<html><b style='color:#ffffff'>" + n + "</b> " + (n == 1 ? "peaje" : "peajes")
                + "</html>");
        if (result.isOnRoad()) {
            summaryKm.setText("<html><b style='color:#ffffff'>" + TravelerUi.km(result.getDistanceKm())
                    + "</b> · " + result.getRoad().durationText() + "</html>");
            methodNote.setText(NOTE_ROAD);
        } else {
            summaryKm.setText("<html>≈ <b style='color:#ffffff'>" + TravelerUi.km(result.getDistanceKm())
                    + "</b></html>");
            methodNote.setText(NOTE_APPROX);
        }
        note.setText(result.getWithoutRate() == 0 ? " "
                : "<html>" + result.getWithoutRate() + " peaje(s) no tienen tarifa para esta categoría"
                + " y no suman al total.</html>");
        fillItinerary();

        List<Toll> shown = new ArrayList<>();
        for (TripStop s : result.getStops()) {
            shown.add(s.getToll());
        }
        List<GeoPosition> path = new ArrayList<>();
        if (result.isOnRoad()) {
            // Todos los puntos de la vía: la línea sigue la carretera
            for (double[] p : result.getRoad().getPoints()) {
                path.add(new GeoPosition(p[0], p[1]));
            }
        } else {
            // Aproximado: origen, cada peaje y destino unidos en línea recta
            path.add(new GeoPosition(from.getLatitude(), from.getLongitude()));
            for (Toll t : shown) {
                path.add(new GeoPosition(t.getLatitude(), t.getLongitude()));
            }
            path.add(new GeoPosition(to.getLatitude(), to.getLongitude()));
        }
        map.showTolls(shown);
        map.showRoute(path);
        selectToll(null);
    }

    /** Arma la lista del viaje (o un mensaje si todavía no se ha calculado). */
    private void fillItinerary() {
        itinerary.removeAll();
        rows.clear();
        if (plan == null) {
            JLabel empty = TravelerUi.emptyMessage("Escoja origen y destino y pulse<br><b>Calcular viaje</b>.");
            empty.setBorder(BorderFactory.createEmptyBorder(30, 16, 30, 16));
            empty.setAlignmentX(LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            itinerary.add(empty);
        } else {
            City from = (City) origin.getSelectedItem();
            City to = (City) destination.getSelectedItem();
            itinerary.add(new EndRow(from == null ? "Origen" : from.getName()));
            int i = 1;
            for (TripStop s : plan.getStops()) {
                StopRow row = new StopRow(i++, s);
                rows.add(row);
                itinerary.add(row);
            }
            if (plan.getStops().isEmpty()) {
                JLabel none = label("No se encontraron peajes en el camino.", Font.PLAIN, 13, Theme.MUTED);
                none.setBorder(BorderFactory.createEmptyBorder(8, 50, 8, 16));
                itinerary.add(left(none));
            }
            itinerary.add(new EndRow(to == null ? "Destino" : to.getName()));
        }
        itinerary.revalidate();
        itinerary.repaint();
    }

    /** Resalta un peaje en la lista y en el mapa y muestra su ficha (null la oculta). */
    private void selectToll(Toll toll) {
        cardToll = toll;
        map.select(toll, false);
        for (StopRow row : rows) {
            row.setSelected(toll != null && row.stop.getToll().getTollId() == toll.getTollId());
        }
        if (toll == null) {
            tollCard.setVisible(false);
            return;
        }
        cardName.setText("Peaje " + toll.getName());
        String routeText = toll.getRouteId() == null ? "" : " · Ruta " + toll.getRouteId();
        String dept = catalog == null ? TravelerUi.orDash(toll.getDepartmentId())
                : catalog.departmentName(toll.getDepartmentId());
        cardInfo.setText(dept + routeText + " · " + TravelerUi.pretty(toll.getOperationalStatus()));
        TripStop stop = stopOf(toll);
        if (stop != null && planCategory != null) {
            cardCategory.setText("Cat. " + planCategory.getCategoryCode());
            cardPrice.setText(stop.getPrice() == null ? "Sin tarifa" : Theme.money(stop.getPrice()));
        } else {
            cardCategory.setText(TravelerUi.levelText(toll.getLevel()));
            cardPrice.setText(" ");
        }
        tollCard.setVisible(true);
        tollCard.revalidate();
        tollCard.getParent().doLayout();
        tollCard.repaint();
    }

    private TripStop stopOf(Toll toll) {
        if (plan == null) {
            return null;
        }
        for (TripStop s : plan.getStops()) {
            if (s.getToll().getTollId() == toll.getTollId()) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Ayudas

    private static JLabel label(String text, int style, int size, Color color) {
        return TravelerUi.label(text, style, size, color);
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private static JPanel pair(JComponent a, JComponent b) {
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(a);
        row.add(b);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, a.getPreferredSize().height));
        return row;
    }

    /** En el combo cerrado muestra "Cat. I"; en la lista desplegada, el texto completo. */
    private static void useShortCategoryText(JComboBox<VehicleCategory> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof VehicleCategory) {
                    VehicleCategory c = (VehicleCategory) value;
                    setText(index < 0 ? "Cat. " + c.getCategoryCode() : TravelerUi.categoryText(c));
                }
                return this;
            }
        });
    }

    // ------------------------------------------------------------------ Componentes propios

    /** Panel con esquinas redondeadas y borde opcional. */
    private static final class RoundPanel extends JPanel {
        private final Color fill;
        private final Color outline;

        RoundPanel(Color fill, Color outline) {
            this.fill = fill;
            this.outline = outline;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Círculo con el número del peaje (naranja; azul si está seleccionado). */
    private static final class Badge extends JComponent {
        private final String text;
        private boolean selected;

        Badge(int number) {
            this.text = String.valueOf(number);
            Dimension d = new Dimension(RADIUS * 2, RADIUS * 2);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        void setSelected(boolean value) {
            selected = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selected ? Theme.PRIMARY : Theme.WARNING);
            g2.fillOval(0, 0, RADIUS * 2, RADIUS * 2);
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, RADIUS - fm.stringWidth(text) / 2, RADIUS + fm.getAscent() / 2 - 2);
            g2.dispose();
        }
    }

    /** Fila de origen o destino: anillo oscuro y nombre de la ciudad. */
    private static final class EndRow extends JPanel {
        EndRow(String city) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            JComponent ring = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.TEXT);
                    g2.setStroke(new java.awt.BasicStroke(3f));
                    int c = RADIUS;
                    g2.drawOval(c - 6, getHeight() / 2 - 6, 12, 12);
                    g2.dispose();
                }
            };
            ring.setPreferredSize(new Dimension(RADIUS * 2, RADIUS * 2));
            add(ring, BorderLayout.WEST);
            add(TravelerUi.label(city, Font.BOLD, 14, Theme.TEXT), BorderLayout.CENTER);
        }
    }

    /** Fila de un peaje del viaje: número, nombre, departamento · km y precio. */
    private final class StopRow extends JPanel {
        private final TripStop stop;
        private final Badge badge;
        private final JLabel name;

        StopRow(int number, TripStop stop) {
            super(new BorderLayout(12, 0));
            this.stop = stop;
            setBackground(Color.WHITE);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            badge = new Badge(number);
            JPanel badgeHolder = new JPanel(new java.awt.GridBagLayout());
            badgeHolder.setOpaque(false);
            badgeHolder.add(badge);

            name = TravelerUi.label(stop.getToll().getName(), Font.PLAIN, 14, Theme.TEXT);
            String dept = catalog == null ? "" : catalog.departmentName(stop.getToll().getDepartmentId());
            JLabel detail = TravelerUi.label(dept + " · km " + Math.round(stop.getKmFromOrigin()),
                    Font.PLAIN, 12, Theme.MUTED);
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(name);
            text.add(detail);

            JLabel price = TravelerUi.label(stop.getPrice() == null ? "Sin tarifa" : Theme.money(stop.getPrice()),
                    Font.BOLD, 14, stop.getPrice() == null ? Theme.MUTED : Theme.TEXT);

            add(badgeHolder, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(price, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectToll(stop.getToll());
                    map.select(stop.getToll(), true);
                }
            });
        }

        void setSelected(boolean value) {
            setBackground(value ? Theme.PRIMARY_SOFT : Color.WHITE);
            name.setFont(Theme.font(value ? Font.BOLD : Font.PLAIN, 14));
            badge.setSelected(value);
        }
    }

    /** Opción del combo de rutas. */
    private static final class RouteOption {
        private final String id;
        private final String name;

        RouteOption(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
