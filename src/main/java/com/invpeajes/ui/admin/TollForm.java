package com.invpeajes.ui.admin;

import com.invpeajes.model.Concessionaire;
import com.invpeajes.model.ManualToll;
import com.invpeajes.model.Toll;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.Theme;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Datos de un peaje para registrarlo o editarlo: nombre, código, departamento, ruta, concesionario,
 * kilómetro, coordenadas, sentido, estado, teléfonos, descripción de la ubicación y si tiene
 * carril COLPASS. La entidad no aparece: es siempre la del administrador.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class TollForm extends JPanel {

    private final JTextField name = Forms.text("");
    private final JTextField code = Forms.text("");
    private final JComboBox<String> department;
    private final JComboBox<String> route;
    private final JComboBox<Integer> concessionaire;
    private final JTextField kilometer = Forms.text("");
    private final JTextField latitude = Forms.text("");
    private final JTextField longitude = Forms.text("");
    private final JComboBox<String> direction = Forms.combo(Labels::direction, list(Labels.DIRECTIONS, true));
    private final JComboBox<String> status = Forms.combo(Labels::tollStatus, list(Labels.TOLL_STATUSES, false));
    private final JTextField phone = Forms.text("");
    private final JTextField towPhone = Forms.text("");
    private final JTextField location = Forms.text("");
    private final JCheckBox colpass = new JCheckBox("Tiene carril COLPASS (cobro con TAG)");
    private final boolean registering;

    /**
     * @param catalog Catálogo (departamentos, rutas y concesionarios).
     * @param registering true para registrar (sin estado: nace "Sin tarifa" y sin coordenadas en el
     *                    formulario, porque se marcan en el mapa).
     */
    public TollForm(Catalog catalog, boolean registering) {
        this.registering = registering;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        department = Forms.combo(id -> catalog.departmentName(id), new ArrayList<>(catalog.getDepartments().keySet()));
        List<String> routes = new ArrayList<>();
        routes.add(null);
        routes.addAll(catalog.getRoutes().keySet());
        route = Forms.combo(id -> id == null ? "—" : id + " · " + catalog.routeName(id), routes);
        List<Integer> concessionaires = new ArrayList<>();
        concessionaires.add(null);
        List<Concessionaire> sorted = new ArrayList<>(catalog.getConcessionaires().values());
        sorted.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        sorted.forEach(c -> concessionaires.add(c.getConcessionaireId()));
        concessionaire = Forms.combo(id -> id == null ? "—" : catalog.concessionaireName(id), concessionaires);
        colpass.setOpaque(false);
        colpass.setFont(Theme.font(Font.PLAIN, 13));
        colpass.setFocusPainted(false);

        add(Forms.row(Forms.field("Nombre *", name), Forms.field("Código", code)));
        gap();
        add(Forms.row(Forms.field("Departamento *", department), Forms.field("Ruta", route)));
        gap();
        add(Forms.row(Forms.field("Concesionario", concessionaire), Forms.field("Kilómetro (PR)", kilometer)));
        gap();
        if (!registering) {
            add(Forms.row(Forms.field("Latitud *", latitude), Forms.field("Longitud *", longitude)));
            gap();
            add(Forms.row(Forms.field("Sentido", direction), Forms.field("Estado", status)));
        } else {
            add(Forms.field("Sentido", direction));
        }
        gap();
        add(Forms.row(Forms.field("Teléfono", phone), Forms.field("Teléfono grúa", towPhone)));
        gap();
        add(Forms.field("Ubicación (descripción)", location));
        add(Box.createVerticalStrut(8));
        colpass.setAlignmentX(LEFT_ALIGNMENT);
        add(colpass);
    }

    private void gap() {
        add(Box.createVerticalStrut(10));
    }

    /**
     * Muestra los datos de un peaje.
     * @param toll Peaje.
     */
    public void load(Toll toll) {
        name.setText(orEmpty(toll.getName()));
        code.setText(orEmpty(toll.getCode()));
        department.setSelectedItem(toll.getDepartmentId());
        route.setSelectedItem(toll.getRouteId());
        concessionaire.setSelectedItem(toll.getConcessionaireId());
        kilometer.setText(orEmpty(toll.getKilometer()));
        setLocation(toll.getLatitude(), toll.getLongitude());
        direction.setSelectedItem(toll.getDirection());
        status.setSelectedItem(toll.getOperationalStatus());
        phone.setText(orEmpty(toll.getPhone()));
        towPhone.setText(orEmpty(toll.getTowPhone()));
        location.setText(orEmpty(toll.getLocationDescription()));
        colpass.setSelected("S".equals(toll.getAcceptsColpass()));
    }

    /**
     * Cambia las coordenadas (ej. al hacer clic en el mapa).
     * @param lat Latitud.
     * @param lon Longitud.
     */
    public void setLocation(double lat, double lon) {
        latitude.setText(String.format(Locale.ROOT, "%.6f", lat));
        longitude.setText(String.format(Locale.ROOT, "%.6f", lon));
    }

    /**
     * Lee lo escrito. El ID, la entidad y la fecha los pone el servicio.
     * @param tollId ID del peaje (0 si es nuevo).
     * @return Peaje con los datos del formulario.
     * @throws IllegalArgumentException Si las coordenadas no son números.
     */
    public Toll read(int tollId) {
        double lat;
        double lon;
        try {
            lat = Double.parseDouble(latitude.getText().trim().replace(',', '.'));
            lon = Double.parseDouble(longitude.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(registering ? "Marca la ubicación del peaje en el mapa"
                    : "Escribe la latitud y la longitud con números (ej. 6.293192)");
        }
        String st = registering ? "SIN_TARIFA" : (String) status.getSelectedItem();
        return new ManualToll(tollId, emptyToNull(code.getText()), null, name.getText().trim(),
                emptyToNull(kilometer.getText()), lat, lon, emptyToNull(location.getText()),
                (String) direction.getSelectedItem(), st, null, null, emptyToNull(phone.getText()),
                emptyToNull(towPhone.getText()), null, (String) route.getSelectedItem(),
                (Integer) concessionaire.getSelectedItem(), null, (String) department.getSelectedItem());
    }

    /** @return true si tiene carril COLPASS. */
    public boolean acceptsColpass() {
        return colpass.isSelected();
    }

    /** @return Ruta escogida, o null. */
    public String selectedRoute() {
        return (String) route.getSelectedItem();
    }

    /**
     * @param listener Qué hacer cuando cambia la ruta (para volver a revisar peajes cercanos).
     */
    public void onRouteChange(Runnable listener) {
        route.addActionListener(e -> listener.run());
    }

    private static List<String> list(String[] values, boolean withNull) {
        List<String> list = new ArrayList<>();
        if (withNull) {
            list.add(null);
        }
        list.addAll(List.of(values));
        return list;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
