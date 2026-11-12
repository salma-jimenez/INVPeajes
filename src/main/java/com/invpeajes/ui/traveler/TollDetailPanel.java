package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.model.TollRate;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Pill;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * Ficha de un peaje: datos, concesionario, teléfonos (incluido el de la grúa)
 * y tarifas por categoría vehicular.
 *
 * @author Salma Jiménez Vega
 * @version 2.0
 */
public class TollDetailPanel extends JPanel implements Scrollable {

    private final TravelerService service;
    private final JPanel body = new JPanel();
    private Catalog catalog;
    private int requestId;

    /**
     * @param service servicio del viajero (para cargar las tarifas)
     */
    public TollDetailPanel(TravelerService service) {
        super(new BorderLayout());
        this.service = service;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        add(body, BorderLayout.NORTH);
        showToll(null);
    }

    /**
     * @param catalog catálogo con los nombres de departamentos, rutas y concesionarios
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Muestra la ficha de un peaje.
     *
     * @param toll peaje, o null para mostrar la ayuda
     */
    public void showToll(Toll toll) {
        body.removeAll();
        requestId++;
        if (toll == null) {
            body.add(left(Theme.title("Ficha del peaje")));
            body.add(Box.createVerticalStrut(8));
            body.add(left(TravelerUi.label("<html>Haz clic en un peaje del mapa para ver sus datos,"
                    + " teléfonos y tarifas.</html>", Font.PLAIN, 13, Theme.MUTED)));
            refresh();
            return;
        }
        JLabel name = TravelerUi.label("<html>" + toll.getName() + "</html>", Font.BOLD, 18, Theme.TEXT);
        body.add(left(name));
        body.add(Box.createVerticalStrut(8));
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pills.setOpaque(false);
        Color level = TravelerUi.levelColor(toll.getLevel());
        pills.add(new Pill(TravelerUi.levelText(toll.getLevel()), level, Color.WHITE));
        pills.add(Box.createHorizontalStrut(6));
        boolean operating = TravelerUi.isOperating(toll);
        pills.add(new Pill(TravelerUi.pretty(toll.getOperationalStatus()),
                operating ? new Color(0xDCFCE7) : new Color(0xFEE2E2),
                operating ? new Color(0x15803D) : new Color(0xB91C1C)));
        if ("S".equals(toll.getAcceptsColpass())) {
            pills.add(Box.createHorizontalStrut(6));
            pills.add(new Pill("COLPASS", Theme.PRIMARY_SOFT, Theme.PRIMARY_DARK));
        }
        body.add(left(pills));
        body.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        int row = 0;
        row = addRow(grid, row, "Departamento", catalog == null ? toll.getDepartmentId()
                : catalog.departmentName(toll.getDepartmentId()));
        row = addRow(grid, row, "Ruta", catalog == null ? toll.getRouteId() : catalog.routeName(toll.getRouteId()));
        row = addRow(grid, row, "Kilómetro", toll.getKilometer());
        row = addRow(grid, row, "Sentido", toll.getDirection());
        row = addRow(grid, row, "Ubicación", toll.getLocationDescription());
        row = addRow(grid, row, "Concesionario", catalog == null ? "—"
                : catalog.concessionaireName(toll.getConcessionaireId()));
        row = addRow(grid, row, "Teléfono", toll.getPhone());
        addRow(grid, row, "Teléfono grúa", toll.getTowPhone());
        body.add(left(grid));
        body.add(Box.createVerticalStrut(16));
        body.add(left(Theme.title("Tarifas por categoría")));
        body.add(Box.createVerticalStrut(6));
        JPanel rates = new JPanel();
        rates.setOpaque(false);
        rates.setLayout(new BoxLayout(rates, BoxLayout.Y_AXIS));
        rates.add(left(Theme.muted("Cargando tarifas...")));
        body.add(left(rates));
        refresh();

        int myRequest = requestId;
        Async.run(() -> service.rates(toll.getTollId()), list -> {
            if (myRequest == requestId) {
                fillRates(rates, list);
            }
        }, error -> {
            if (myRequest == requestId) {
                rates.removeAll();
                rates.add(left(TravelerUi.label("No se pudieron cargar las tarifas.", Font.PLAIN, 13, Theme.DANGER)));
                refresh();
            }
        });
    }

    private void fillRates(JPanel rates, List<TollRate> list) {
        rates.removeAll();
        if (list.isEmpty()) {
            rates.add(left(Theme.muted("Este peaje no tiene tarifas registradas.")));
        }
        for (TollRate rate : list) {
            JPanel line = new JPanel(new BorderLayout(10, 0));
            line.setOpaque(false);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            line.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(6, 0, 6, 0)));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            line.add(TravelerUi.label(categoryName(rate.getCategoryId()), Font.PLAIN, 13, Theme.TEXT),
                    BorderLayout.CENTER);
            line.add(TravelerUi.label(Theme.money(rate.getPrice()), Font.BOLD, 13, Theme.TEXT), BorderLayout.EAST);
            rates.add(line);
        }
        refresh();
    }

    private String categoryName(int categoryId) {
        if (catalog != null) {
            for (VehicleCategory c : catalog.getCategories()) {
                if (c.getCategoryId() == categoryId) {
                    return TravelerUi.categoryText(c);
                }
            }
        }
        return "Categoría " + categoryId;
    }

    private static int addRow(JPanel grid, int row, String caption, String value) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 6, 12);
        grid.add(TravelerUi.label(caption, Font.BOLD, 12, Theme.MUTED), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);
        grid.add(TravelerUi.label("<html>" + TravelerUi.orDash(value) + "</html>", Font.PLAIN, 13, Theme.TEXT), c);
        return row + 1;
    }

    private static Component left(java.awt.Component c) {
        if (c instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        return c;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
        return visible.height - 32;
    }

    /** La ficha toma el ancho del panel lateral para que los textos largos bajen de línea. */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private void refresh() {
        body.revalidate();
        body.repaint();
    }
}
