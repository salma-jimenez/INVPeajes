package com.invpeajes.ui.traveler;

import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.Toll;
import com.invpeajes.model.Traveler;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.service.ValidationException;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.station.StationTexts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Pedir asistencia en la vía. El viajero dice qué necesita, su placa, su teléfono y qué
 * pasó, y marca en el mapa dónde está (o escoge el peaje más cercano). La solicitud le
 * llega al Centro de asistencia del jefe de estación de ese peaje.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class AssistancePanel extends JPanel {

    private final TravelerService service;
    private final Traveler traveler;
    private final Runnable onSent;
    private final JComboBox<AssistanceType> type = new JComboBox<>(new AssistanceType[]{
        AssistanceType.TOW_TRUCK, AssistanceType.MECHANICAL_AID, AssistanceType.MOBILE_WORKSHOP,
        AssistanceType.AMBULANCE});
    private final JTextField plate = Theme.textField();
    private final JComboBox<VehicleCategory> category = new JComboBox<>();
    private final JTextField phone = Theme.textField();
    private final JTextArea description = new JTextArea(4, 20);
    private final JComboBox<Object> toll = new JComboBox<>();
    private final JLabel where = TravelerUi.label("Haga clic en el mapa donde está.", Font.PLAIN, 13, Theme.MUTED);
    private final JLabel error = TravelerUi.label(" ", Font.PLAIN, 13, Theme.DANGER);
    private final FlatButton send = Theme.primaryButton("Pedir asistencia");
    private final TollMap map = new TollMap();
    private Catalog catalog;
    private GeoPosition point;

    /**
     * @param service  servicio del viajero
     * @param traveler viajero con cuenta (el invitado ve {@link GuestAssistancePanel})
     * @param onSent   qué hacer después de enviar una solicitud (por ejemplo, recargar la lista)
     */
    public AssistancePanel(TravelerService service, Traveler traveler, Runnable onSent) {
        super(new BorderLayout(14, 0));
        this.service = service;
        this.traveler = traveler;
        this.onSent = onSent;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        TravelerUi.styleCombo(type);
        type.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof AssistanceType) {
                    setText(StationTexts.type((AssistanceType) value));
                }
                return this;
            }
        });
        TravelerUi.styleCombo(category);
        TravelerUi.useCategoryText(category);
        TravelerUi.styleCombo(toll);
        description.setFont(Theme.font(Font.PLAIN, 14));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane descScroll = new JScrollPane(description);
        descScroll.setBorder(BorderFactory.createLineBorder(Theme.INPUT_BORDER));
        descScroll.setPreferredSize(new Dimension(200, 84));
        resetPersonalData();

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        form.add(left(Theme.title("¿Qué necesita?")));
        form.add(Box.createVerticalStrut(10));
        form.add(TravelerUi.field("Tipo de asistencia", type));
        form.add(Box.createVerticalStrut(10));
        form.add(TravelerUi.field("Placa del vehículo", plate));
        form.add(Box.createVerticalStrut(10));
        form.add(TravelerUi.field("Categoría (si la placa es nueva)", category));
        form.add(Box.createVerticalStrut(10));
        form.add(TravelerUi.field("Teléfono de contacto", phone));
        form.add(Box.createVerticalStrut(10));
        form.add(TravelerUi.field("¿Qué pasó?", descScroll));
        form.add(Box.createVerticalStrut(14));
        form.add(left(Theme.title("¿Dónde está?")));
        form.add(Box.createVerticalStrut(6));
        form.add(left(where));
        form.add(Box.createVerticalStrut(8));
        form.add(TravelerUi.field("Peaje que lo atiende", toll));
        form.add(Box.createVerticalStrut(14));
        send.setAlignmentX(LEFT_ALIGNMENT);
        send.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        send.addActionListener(e -> submit());
        form.add(send);
        form.add(Box.createVerticalStrut(6));
        form.add(left(error));

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(form, BorderLayout.NORTH);
        JScrollPane formScroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(Theme.BACKGROUND);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setPreferredSize(new Dimension(360, 0));

        map.setOnPick(this::pick);
        map.setOnSelect(t -> {
            toll.setSelectedItem(t);
            where.setText("Escogió el peaje " + t.getName() + ".");
        });
        JPanel mapBox = new JPanel(new BorderLayout(0, 8));
        mapBox.setOpaque(false);
        mapBox.add(TravelerUi.label("<html>Haga clic en el mapa donde está y se escoge solo el peaje más"
                + " cercano. También puede hacer clic en un peaje.</html>", Font.PLAIN, 13, Theme.MUTED),
                BorderLayout.NORTH);
        mapBox.add(map, BorderLayout.CENTER);

        add(formScroll, BorderLayout.WEST);
        add(mapBox, BorderLayout.CENTER);
    }

    /**
     * @param catalog catálogo cargado
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        category.removeAllItems();
        for (VehicleCategory c : catalog.getCategories()) {
            category.addItem(c);
        }
        toll.removeAllItems();
        toll.addItem("Escoja un peaje");
        List<Toll> sorted = new ArrayList<>(catalog.getTolls());
        sorted.sort((a, b) -> TravelerUi.normalize(a.getName()).compareTo(TravelerUi.normalize(b.getName())));
        for (Toll t : sorted) {
            toll.addItem(t);
        }
        toll.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Toll) {
                    Toll t = (Toll) value;
                    setText(t.getName() + " · " + catalog.departmentName(t.getDepartmentId()));
                }
                return this;
            }
        });
        map.showTolls(catalog.getTolls());
    }

    private void pick(GeoPosition position) {
        point = position;
        if (catalog == null) {
            return;
        }
        Toll nearest = TravelerService.nearestToll(catalog.getTolls(), position.getLatitude(), position.getLongitude());
        if (nearest != null) {
            toll.setSelectedItem(nearest);
            map.select(nearest, false);
            double km = TravelerService.distanceKm(position.getLatitude(), position.getLongitude(),
                    nearest.getLatitude(), nearest.getLongitude());
            where.setText(String.format(Locale.ROOT, "<html>Punto marcado (%.4f, %.4f).<br>Peaje más cercano: "
                    + "<b>%s</b>, a %s.</html>", position.getLatitude(), position.getLongitude(),
                    nearest.getName(), TravelerUi.km(km)));
        }
    }

    private void submit() {
        error.setText(" ");
        Object selected = toll.getSelectedItem();
        Toll chosen = selected instanceof Toll ? (Toll) selected : null;
        VehicleCategory cat = (VehicleCategory) category.getSelectedItem();
        AssistanceType kind = (AssistanceType) type.getSelectedItem();
        String plateText = plate.getText();
        String phoneText = phone.getText();
        String what = description.getText();
        Double lat = point == null ? null : point.getLatitude();
        Double lon = point == null ? null : point.getLongitude();
        send.setEnabled(false);
        send.setText("Enviando...");
        Async.run(() -> service.requestAssistance(traveler, kind, chosen == null ? 0 : chosen.getTollId(),
                plateText, cat == null ? 0 : cat.getCategoryId(), phoneText, what, lat, lon), number -> {
            send.setEnabled(true);
            send.setText("Pedir asistencia");
            String extra = "\n\nPuede seguir cómo avanza, paso a paso, en \"Mis solicitudes\".";
            JOptionPane.showMessageDialog(this, "Su solicitud fue enviada al peaje " + chosen.getName()
                    + ".\n\nNúmero de solicitud: " + number + extra, "Asistencia solicitada",
                    JOptionPane.INFORMATION_MESSAGE);
            description.setText("");
            point = null;
            map.setPicked(null);
            where.setText("Haga clic en el mapa donde está.");
            onSent.run();
        }, failure -> {
            send.setEnabled(true);
            send.setText("Pedir asistencia");
            error.setText("<html>" + (failure instanceof ValidationException ? failure.getMessage()
                    : "No se pudo enviar la solicitud. Revise su conexión e intente de nuevo.") + "</html>");
        });
    }

    private void resetPersonalData() {
        plate.setText(traveler == null || traveler.getPlate() == null ? "" : traveler.getPlate());
        phone.setText(traveler == null || traveler.getPhone() == null ? "" : traveler.getPhone());
    }

    /** Vuelve a llenar la placa y el teléfono (después de cambiarlos en "Mi cuenta"). */
    public void refreshPersonalData() {
        resetPersonalData();
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }
}
