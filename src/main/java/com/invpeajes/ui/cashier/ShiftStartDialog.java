package com.invpeajes.ui.cashier;

import com.invpeajes.model.TollBooth;
import com.invpeajes.model.CashierSession;
import com.invpeajes.service.CashierService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Ventana de apertura de turno: el operador escribe su cédula, el sistema verifica que
 * sea operador de caseta y le muestra las casetas activas de su peaje para elegir una.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class ShiftStartDialog extends JDialog {

    private final CashierService service;
    private final JTextField idField = Theme.textField();
    private final JComboBox<TollBooth> boothCombo = new JComboBox<>();
    private final JLabel statusLabel = Theme.muted(" ");
    private final FlatButton verifyButton = Theme.secondaryButton("Verificar");
    private final FlatButton startButton = Theme.primaryButton("Iniciar turno");
    private CashierSession operator;
    private CashierSession result;

    /**
     * @param owner Ventana dueña (puede ser null).
     * @param service Servicio de la caja.
     */
    public ShiftStartDialog(Window owner, CashierService service) {
        super(owner, "RutaPeaje · Iniciar turno", ModalityType.APPLICATION_MODAL);
        this.service = service;
        buildContent();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /**
     * Muestra la ventana y espera a que el operador abra su turno o la cierre.
     * @return Sesión con caseta, o null si se canceló.
     */
    public CashierSession showDialog() {
        setVisible(true);
        return result;
    }

    private void buildContent() {
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(Theme.SIDEBAR);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel brand = new JLabel("RutaPeaje");
        brand.setFont(Theme.font(Font.BOLD, 24));
        brand.setForeground(java.awt.Color.WHITE);
        JLabel subtitle = new JLabel("Caja de peaje · apertura de turno");
        subtitle.setFont(Theme.font(Font.PLAIN, 13));
        subtitle.setForeground(Theme.SIDEBAR_MUTED);
        header.add(brand);
        header.add(subtitle);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CARD);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        c.gridx = 0;
        c.weightx = 1;

        c.gridy = 0;
        c.gridwidth = 2;
        form.add(Theme.muted("Cédula del operador"), c);

        c.gridy = 1;
        c.gridwidth = 1;
        idField.setColumns(16);
        form.add(idField, c);
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(4, 8, 4, 0);
        form.add(verifyButton, c);

        c.gridx = 0;
        c.weightx = 1;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 4, 0);
        c.gridy = 2;
        form.add(Theme.muted("Caseta del turno"), c);

        c.insets = new Insets(4, 0, 4, 0);
        c.gridy = 3;
        boothCombo.setFont(Theme.font(Font.PLAIN, 14));
        boothCombo.setEnabled(false);
        boothCombo.setRenderer(new BoothRenderer());
        form.add(boothCombo, c);

        c.gridy = 4;
        c.insets = new Insets(10, 0, 4, 0);
        form.add(statusLabel, c);

        c.gridy = 5;
        c.insets = new Insets(14, 0, 0, 0);
        startButton.setEnabled(false);
        form.add(startButton, c);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);

        verifyButton.addActionListener(e -> verify());
        idField.addActionListener(e -> verify());
        startButton.addActionListener(e -> start());
        getRootPane().setDefaultButton(verifyButton);
    }

    private void verify() {
        String id = idField.getText();
        setBusy(true, "Verificando…");
        Async.run(() -> {
            CashierSession session = service.login(id);
            return new Object[] {session, service.availableBooths(session)};
        }, data -> {
            operator = (CashierSession) data[0];
            @SuppressWarnings("unchecked")
            List<TollBooth> booths = (List<TollBooth>) data[1];
            boothCombo.removeAllItems();
            booths.forEach(boothCombo::addItem);
            setBusy(false, null);
            if (booths.isEmpty()) {
                showError("El peaje " + operator.getTollName() + " no tiene casetas activas");
                return;
            }
            showOk("Hola, " + operator.getOperatorName() + " · Peaje " + operator.getTollName());
            boothCombo.setEnabled(true);
            startButton.setEnabled(true);
            getRootPane().setDefaultButton(startButton);
            boothCombo.requestFocusInWindow();
        }, error -> {
            setBusy(false, null);
            operator = null;
            boothCombo.removeAllItems();
            boothCombo.setEnabled(false);
            startButton.setEnabled(false);
            showError(error.getMessage());
        });
    }

    private void start() {
        try {
            result = service.openShift(operator, (TollBooth) boothCombo.getSelectedItem());
            dispose();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void setBusy(boolean busy, String message) {
        verifyButton.setEnabled(!busy);
        idField.setEnabled(!busy);
        if (message != null) {
            statusLabel.setForeground(Theme.MUTED);
            statusLabel.setText(message);
        }
    }

    private void showError(String message) {
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setText("<html>" + message + "</html>");
        pack();
    }

    private void showOk(String message) {
        statusLabel.setForeground(Theme.SUCCESS);
        statusLabel.setText("<html>" + message + "</html>");
        pack();
    }

    /** Muestra cada caseta como "Caseta 2 · MIXTO · OCCIDENTE_ORIENTE". */
    private static final class BoothRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof TollBooth) {
                TollBooth b = (TollBooth) value;
                setText("Caseta " + b.getBoothNumber() + " · " + b.getPaymentType()
                        + (b.getDirection() != null ? " · " + b.getDirection() : ""));
            }
            return this;
        }
    }
}
