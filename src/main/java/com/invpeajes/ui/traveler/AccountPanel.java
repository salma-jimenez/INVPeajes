package com.invpeajes.ui.traveler;

import com.invpeajes.model.Traveler;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.ValidationException;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * "Mi cuenta": datos del viajero y su placa. Cada viajero tiene una sola placa;
 * puede cambiarla junto con su teléfono.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class AccountPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TravelerService service;
    private final Traveler traveler;
    private final Runnable onSaved;
    private final JTextField phone = Theme.textField();
    private final JTextField plate = Theme.textField();
    private final JLabel message = TravelerUi.label(" ", Font.PLAIN, 13, Theme.MUTED);
    private final FlatButton save = Theme.primaryButton("Guardar cambios");

    /**
     * @param service  servicio del viajero
     * @param traveler viajero con cuenta
     * @param onSaved  qué hacer después de guardar (por ejemplo, actualizar otras pantallas)
     */
    public AccountPanel(TravelerService service, Traveler traveler, Runnable onSaved) {
        super(new BorderLayout());
        this.service = service;
        this.traveler = traveler;
        this.onSaved = onSaved;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.add(left(TravelerUi.label(traveler.getFullName(), Font.BOLD, 22, Theme.TEXT)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(Theme.muted(traveler.getEmail()
                + (traveler.getRegisteredAt() == null ? "" : " · Cuenta desde "
                + traveler.getRegisteredAt().format(DATE)))));
        card.add(Box.createVerticalStrut(18));
        card.add(TravelerUi.field("Correo (es su usuario, no se puede cambiar)", readOnly(traveler.getEmail())));
        card.add(Box.createVerticalStrut(12));
        card.add(TravelerUi.field("Teléfono", phone));
        card.add(Box.createVerticalStrut(12));
        card.add(TravelerUi.field("Placa de su vehículo", plate));
        card.add(Box.createVerticalStrut(4));
        card.add(left(Theme.muted("Se usa para llenar las solicitudes de asistencia. Formato ABC123 o ABC12D.")));
        card.add(Box.createVerticalStrut(18));
        save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e -> save());
        card.add(save);
        card.add(Box.createVerticalStrut(8));
        card.add(left(message));
        reset();

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setMaximumSize(new Dimension(560, Integer.MAX_VALUE));
        holder.setPreferredSize(new Dimension(560, 0));
        holder.add(card, BorderLayout.NORTH);
        add(holder, BorderLayout.WEST);
    }

    private void reset() {
        phone.setText(traveler.getPhone() == null ? "" : traveler.getPhone());
        plate.setText(traveler.getPlate() == null ? "" : traveler.getPlate());
    }

    private void save() {
        message.setText(" ");
        String newPhone = phone.getText();
        String newPlate = plate.getText();
        save.setEnabled(false);
        Async.run(() -> service.updateAccount(traveler, newPhone, newPlate), t -> {
            save.setEnabled(true);
            reset();
            message.setForeground(Theme.SUCCESS);
            message.setText("Cambios guardados.");
            onSaved.run();
        }, error -> {
            save.setEnabled(true);
            message.setForeground(Theme.DANGER);
            message.setText(error instanceof ValidationException ? error.getMessage()
                    : "No se pudieron guardar los cambios. Intente de nuevo.");
        });
    }

    private static JTextField readOnly(String text) {
        JTextField field = Theme.textField();
        field.setText(text);
        field.setEditable(false);
        field.setBackground(Theme.DISABLED);
        return field;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }
}
