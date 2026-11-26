package com.invpeajes.ui.admin;

import com.invpeajes.model.Toll;
import com.invpeajes.service.AdminService;
import com.invpeajes.service.AdminService.ClosePair;
import com.invpeajes.service.RouteService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.traveler.TollMap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * "Mapa" del administrador: el mismo mapa del viajero ({@link TollMap}) con los peajes de su
 * entidad (o todos, solo para ver) y a la derecha los "Peajes muy cercanos": pares de peajes de una
 * misma ruta que están a menos de la distancia escogida. Al escoger un par, el mapa los une por
 * la carretera (o en línea recta si no hay internet) y muestra los km por vía; al escoger un peaje
 * propio en el mapa, se abre para editarlo.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AdminMapPanel extends JPanel implements AdminPage {

    private static final String MINE = "Solo mis peajes";
    private static final String ALL = "Todos los peajes (los demás solo para ver)";

    private final TollMap map = new TollMap();
    private final JComboBox<String> scope = Forms.combo(s -> s, List.of(MINE, ALL));
    private final JComboBox<Integer> distance = Forms.combo(km -> "Menos de " + km + " km", List.of(20, 40, 60, 100));
    private final JPanel pairs = new JPanel();
    private final JLabel pairsNote = Theme.muted(" ");
    private AdminContext context;
    /** Cuenta las rutas pedidas, para ignorar una respuesta que llega cuando ya se escogió otra cosa. */
    private int routeRequest;

    /**
     * @param openToll Qué hacer al escoger un peaje propio en el mapa (abrirlo en Peajes).
     */
    public AdminMapPanel(Consumer<Toll> openToll) {
        super(new BorderLayout(14, 0));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        distance.setSelectedItem((int) AdminService.CLOSE_KM);
        scope.addActionListener(e -> showTolls());
        distance.addActionListener(e -> fillPairs());
        map.setOnSelect(toll -> {
            if (context != null && context.mineById(toll.getTollId()) != null) {
                openToll.accept(toll);
            }
        });

        JPanel mapCard = Theme.card();
        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        scope.setPreferredSize(new Dimension(320, 36));
        tools.add(scope, BorderLayout.WEST);
        tools.add(Theme.muted("Clic en uno de tus peajes para editarlo"), BorderLayout.EAST);
        mapCard.add(tools, BorderLayout.NORTH);
        mapCard.add(map, BorderLayout.CENTER);

        JPanel side = Theme.card();
        side.setPreferredSize(new Dimension(340, 0));
        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);
        head.add(Theme.title("Peajes muy cercanos"), BorderLayout.NORTH);
        head.add(Theme.muted("Pares sobre la misma ruta"), BorderLayout.CENTER);
        head.add(distance, BorderLayout.SOUTH);
        side.add(head, BorderLayout.NORTH);
        pairs.setLayout(new BoxLayout(pairs, BoxLayout.Y_AXIS));
        pairs.setOpaque(false);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(pairs, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        side.add(scroll, BorderLayout.CENTER);
        side.add(pairsNote, BorderLayout.SOUTH);

        add(mapCard, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
    }

    @Override
    public void setContext(AdminContext context) {
        this.context = context;
        showTolls();
        fillPairs();
    }

    @Override
    public String subtitle(AdminContext context) {
        return "El mismo mapa del viajero: el color dice qué entidad maneja cada peaje";
    }

    /**
     * Centra el mapa en un peaje.
     * @param toll Peaje.
     */
    public void focus(Toll toll) {
        clearRoute();
        map.select(toll, true);
    }

    private void showTolls() {
        if (context == null) {
            return;
        }
        map.showTolls(MINE.equals(scope.getSelectedItem()) ? context.getMine() : context.getCatalog().getTolls());
        clearRoute();
        map.fitAll();
    }

    private void fillPairs() {
        if (context == null) {
            return;
        }
        int km = (Integer) distance.getSelectedItem();
        List<ClosePair> found = AdminService.closePairs(context.getMine(), km);
        pairs.removeAll();
        for (ClosePair p : found) {
            pairs.add(pairRow(p));
        }
        if (found.isEmpty()) {
            pairs.add(Theme.muted("Ningún par de tus peajes está tan cerca."));
        }
        pairsNote.setText("<html><div style='width:225px'>" + found.size() + (found.size() == 1 ? " par" : " pares")
                + " de " + Labels.entity(context.getLevel()) + " · el proyecto de ley 226 de 2026 propone mínimo "
                + "150 km entre peajes de un mismo corredor.</div></html>");
        pairs.revalidate();
        pairs.repaint();
    }

    private JComponent pairRow(ClosePair p) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF0F2F6)),
                BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel names = new JLabel("<html><b>" + p.getFirst().getName() + " ↔ " + p.getSecond().getName()
                + "</b><br><span style='color:#6B7280'>Ruta " + p.getFirst().getRouteId() + "</span></html>");
        names.setFont(Theme.font(Font.PLAIN, 13));
        names.setForeground(Theme.TEXT);
        JLabel km = new JLabel(String.format("%.1f km", p.getKm()).replace('.', ','));
        km.setFont(Theme.font(Font.BOLD, 14));
        km.setForeground(Theme.DANGER);
        row.add(names, BorderLayout.CENTER);
        row.add(km, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPairRoute(p, km);
                map.select(p.getFirst(), false);
            }
        });
        return row;
    }

    /**
     * Une los dos peajes del par: primero en línea recta (al instante) y, cuando llega la ruta
     * por carretera, la cambia por esa y muestra los km por vía en la fila.
     */
    private void showPairRoute(ClosePair p, JLabel km) {
        Toll a = p.getFirst();
        Toll b = p.getSecond();
        map.showRoute(List.of(new GeoPosition(a.getLatitude(), a.getLongitude()),
                new GeoPosition(b.getLatitude(), b.getLongitude())));
        int request = ++routeRequest;
        Async.run(() -> RouteService.findRoute(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude()),
                road -> {
                    if (road == null || request != routeRequest) {
                        return; // sin internet se queda la línea recta; o ya se escogió otro par
                    }
                    List<GeoPosition> path = new ArrayList<>();
                    for (double[] point : road.getPoints()) {
                        path.add(new GeoPosition(point[0], point[1]));
                    }
                    map.showRoute(path);
                    km.setText(String.format(Locale.ROOT, "<html><div style='text-align:right'>%.1f km<br>"
                            + "<span style='font-size:9px;color:#6B7280'>%.1f km por vía</span></div></html>",
                            p.getKm(), road.getDistanceKm()).replace('.', ','));
                },
                error -> { });
    }

    /** Quita la línea del mapa y descarta una ruta que todavía no ha llegado. */
    private void clearRoute() {
        routeRequest++;
        map.showRoute(List.of());
    }
}