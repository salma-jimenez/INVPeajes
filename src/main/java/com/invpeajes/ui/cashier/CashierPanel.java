package com.invpeajes.ui.cashier;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.CategoryRate;
import com.invpeajes.model.ChargeReceipt;
import com.invpeajes.service.CashierService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Pantalla de cobro de la caseta, pensada también para pantalla táctil:
 * 1) el cajero elige la categoría, 2) escribe la placa y el medio de pago y
 * 3) en efectivo marca el valor recibido con el teclado numérico; el sistema calcula el cambio.
 * Al confirmar, guarda el tiquete y solo entonces lo muestra para imprimir.
 *
 * Atajos: F1–F7 eligen la categoría, F12 cobra y Esc limpia.
 *
 * @author Luis Guerrero
 * @version 1.1
 */
public class CashierPanel extends JPanel {

    static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII"};
    static final Map<String, String> METHOD_LABELS = new LinkedHashMap<>();
    private static final Color CHANGE_OK_BG = new Color(0xECFDF3);
    private static final Color CHANGE_BAD_BG = new Color(0xFEF2F2);
    private static final int MAX_DIGITS = 9;

    static {
        METHOD_LABELS.put(CashierService.CASH, "Efectivo");
        METHOD_LABELS.put(CashierService.CARD, "Tarjeta");
        METHOD_LABELS.put(CashierService.TAG, "TAG Colpass");
    }

    private final CashierService service;
    private final CashierSession session;
    private final ShiftLog log;
    private final List<String> allowedMethods;
    private final Map<Integer, CategoryRate> rates = new HashMap<>();
    private CategoryRate selected;
    private String method;

    private final FlatButton[] tiles = new FlatButton[ROMAN.length];
    private final Map<String, FlatButton> methodButtons = new LinkedHashMap<>();
    private final List<FlatButton> cashButtons = new java.util.ArrayList<>();
    private final JTextField plateField = Theme.textField();
    private final JTextField tagField = Theme.textField();
    private final JTextField receivedField = Theme.textField();
    private final JLabel amountLabel = new JLabel("$0", SwingConstants.RIGHT);
    private final JLabel categoryLabel = Theme.muted("Seleccione una categoría");
    private final JPanel changeBox = new JPanel(new BorderLayout());
    private final JLabel changeTitle = new JLabel("Cambio");
    private final JLabel changeLabel = new JLabel("$0", SwingConstants.RIGHT);
    private final JLabel statusLabel = Theme.muted(" ");
    private final JLabel cashInBoxLabel = new JLabel();
    private final FlatButton chargeButton = Theme.primaryButton("Cobrar  (F12)");
    private final FlatButton clearButton = Theme.secondaryButton("Limpiar  (Esc)");

    /**
     * @param service Servicio de la caja.
     * @param session Turno abierto (con caseta).
     * @param log Registro de cobros del turno, compartido con "Mis cobros del turno".
     */
    public CashierPanel(CashierService service, CashierSession session, ShiftLog log) {
        this.service = service;
        this.session = session;
        this.log = log;
        this.allowedMethods = service.allowedMethods(session.getBooth());

        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 0.58;
        c.insets = new Insets(0, 0, 14, 14);
        c.gridy = 0;
        add(buildCategoryCard(), c);
        c.gridy = 1;
        add(buildVehicleCard(), c);
        c.gridy = 2;
        c.weighty = 1;
        c.insets = new Insets(0, 2, 0, 14);
        JLabel hint = Theme.muted("<html>Atajos de teclado: F1–F7 categoría · Enter pasa de la placa al recibido · "
                + "F12 cobrar · Esc limpiar.<br>El valor recibido se digita con el teclado del computador "
                + "o con el teclado de la pantalla.</html>");
        hint.setVerticalAlignment(SwingConstants.TOP);
        add(hint, c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 3;
        c.weightx = 0.42;
        c.insets = new Insets(0, 0, 0, 0);
        add(buildPayCard(), c);

        bindKeys();
        selectMethod(allowedMethods.get(0));
        loadRates();
    }

    // ------------------------------------------------------------------
    // Construcción de la pantalla
    // ------------------------------------------------------------------

    private JPanel buildCategoryCard() {
        JPanel card = Theme.card();
        card.add(Theme.title("1. Categoría del vehículo"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 10, 10));
        grid.setOpaque(false);
        for (int i = 0; i < ROMAN.length; i++) {
            final int categoryId = i + 1;
            FlatButton tile = new FlatButton("");
            tile.setHorizontalAlignment(SwingConstants.LEFT);
            tile.setVerticalAlignment(SwingConstants.TOP);
            tile.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
            tile.setPreferredSize(new Dimension(140, 100));
            tile.setFocusable(false);
            tile.addActionListener(e -> selectCategory(categoryId));
            tiles[i] = tile;
            paintTile(categoryId);
            grid.add(tile);
        }
        JLabel legend = new JLabel("<html><div style='text-align:center'>F1 – F7<br>para elegir<br>rápido</div></html>",
                SwingConstants.CENTER);
        legend.setFont(Theme.font(Font.PLAIN, 12));
        legend.setForeground(Theme.MUTED);
        legend.setBorder(BorderFactory.createDashedBorder(Theme.INPUT_BORDER, 4, 3));
        grid.add(legend);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildVehicleCard() {
        JPanel card = Theme.card();
        card.add(Theme.title("2. Vehículo y medio de pago"), BorderLayout.NORTH);

        ((AbstractDocument) plateField.getDocument()).setDocumentFilter(new UpperCaseFilter(8));
        plateField.setFont(Theme.font(Font.BOLD, 22));
        ((AbstractDocument) tagField.getDocument()).setDocumentFilter(new UpperCaseFilter(30));

        JPanel methods = new JPanel(new GridLayout(1, 3, 8, 0));
        methods.setOpaque(false);
        for (Map.Entry<String, String> entry : METHOD_LABELS.entrySet()) {
            FlatButton b = new FlatButton(entry.getValue());
            b.setFont(Theme.font(Font.BOLD, 13));
            b.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
            b.setFocusable(false);
            b.addActionListener(e -> selectMethod(entry.getKey()));
            if (!allowedMethods.contains(entry.getKey())) {
                b.setEnabled(false);
                b.setToolTipText("La caseta es " + session.getBooth().getPaymentType());
            }
            methodButtons.put(entry.getKey(), b);
            methods.add(b);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 4, 14);
        c.gridx = 0;
        c.weightx = 0.42;
        form.add(Theme.muted("Placa"), c);
        c.gridx = 1;
        c.weightx = 0.58;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(Theme.muted("Medio de pago"), c);
        c.gridy = 1;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 14);
        form.add(plateField, c);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(methods, c);
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(12, 0, 4, 0);
        form.add(Theme.muted("Código TAG (solo con TAG Colpass)"), c);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(tagField, c);
        card.add(form, BorderLayout.CENTER);

        plateField.addActionListener(e -> {
            if (CashierService.CASH.equals(method)) {
                receivedField.requestFocusInWindow();
            } else if (CashierService.TAG.equals(method)) {
                tagField.requestFocusInWindow();
            } else {
                charge();
            }
        });
        tagField.addActionListener(e -> charge());
        return card;
    }

    private JPanel buildPayCard() {
        JPanel card = Theme.card();
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("3. Pago"), BorderLayout.WEST);
        cashInBoxLabel.setFont(Theme.font(Font.BOLD, 15));
        cashInBoxLabel.setForeground(new Color(0xB45309));
        cashInBoxLabel.setOpaque(true);
        cashInBoxLabel.setBackground(new Color(0xFEF3C7));
        cashInBoxLabel.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        cashInBoxLabel.setToolTipText("Dinero en efectivo cobrado en este turno: debe coincidir con la caja");
        header.add(cashInBoxLabel, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        updateCashInBox();
        log.addListener(this::updateCashInBox);

        amountLabel.setFont(Theme.font(Font.BOLD, 36));
        amountLabel.setForeground(Theme.PRIMARY);
        receivedField.setFont(Theme.font(Font.BOLD, 26));
        receivedField.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) receivedField.getDocument()).setDocumentFilter(new MoneyFilter());

        changeTitle.setFont(Theme.font(Font.BOLD, 15));
        changeLabel.setFont(Theme.font(Font.BOLD, 32));
        changeBox.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        changeBox.add(changeTitle, BorderLayout.WEST);
        changeBox.add(changeLabel, BorderLayout.EAST);

        JPanel amountRow = new JPanel(new BorderLayout());
        amountRow.setOpaque(false);
        JPanel amountText = new JPanel(new GridLayout(2, 1));
        amountText.setOpaque(false);
        amountText.add(Theme.muted("Valor a pagar"));
        amountText.add(categoryLabel);
        amountRow.add(amountText, BorderLayout.WEST);
        amountRow.add(amountLabel, BorderLayout.EAST);

        JPanel receivedRow = new JPanel(new BorderLayout(12, 0));
        receivedRow.setOpaque(false);
        receivedRow.add(Theme.muted("Recibido"), BorderLayout.WEST);
        receivedRow.add(receivedField, BorderLayout.CENTER);

        JPanel keypad = new JPanel(new GridLayout(4, 3, 8, 8));
        keypad.setOpaque(false);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "000", "0", "Borrar"};
        for (String key : keys) {
            FlatButton b = Theme.secondaryButton(key);
            b.setFont(Theme.font(Font.BOLD, key.length() == 1 ? 22 : 15));
            b.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
            b.setFocusable(false);
            b.addActionListener(e -> pressKey(key));
            cashButtons.add(b);
            keypad.add(b);
        }

        chargeButton.setFont(Theme.font(Font.BOLD, 18));
        chargeButton.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        chargeButton.setColors(Theme.SUCCESS, new Color(0x15803D), Color.WHITE, null);
        clearButton.setFont(Theme.font(Font.BOLD, 13));

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints a = new GridBagConstraints();
        a.fill = GridBagConstraints.BOTH;
        a.weightx = 0.32;
        a.insets = new Insets(0, 0, 0, 8);
        actions.add(clearButton, a);
        a.weightx = 0.68;
        a.insets = new Insets(0, 0, 0, 0);
        actions.add(chargeButton, a);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int y = 0;
        c.gridy = y++;
        body.add(amountRow, c);
        c.gridy = y++;
        c.insets = new Insets(10, 0, 0, 0);
        body.add(receivedRow, c);
        c.gridy = y++;
        c.insets = new Insets(10, 0, 0, 0);
        body.add(changeBox, c);
        c.gridy = y++;
        c.insets = new Insets(12, 0, 0, 0);
        body.add(keypad, c);
        c.gridy = y++;
        c.weighty = 1;
        body.add(new JLabel(), c);
        c.weighty = 0;
        c.gridy = y++;
        c.insets = new Insets(4, 0, 6, 0);
        body.add(statusLabel, c);
        c.gridy = y;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(actions, c);
        card.add(body, BorderLayout.CENTER);

        receivedField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateChange(); }

            @Override
            public void removeUpdate(DocumentEvent e) { updateChange(); }

            @Override
            public void changedUpdate(DocumentEvent e) { updateChange(); }
        });
        receivedField.addActionListener(e -> charge());
        chargeButton.addActionListener(e -> charge());
        clearButton.addActionListener(e -> clearForm());
        return card;
    }

    private void bindKeys() {
        for (int i = 0; i < ROMAN.length; i++) {
            final int categoryId = i + 1;
            bind(KeyStroke.getKeyStroke(KeyEvent.VK_F1 + i, 0), "cat" + categoryId,
                    () -> selectCategory(categoryId));
        }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "charge", this::charge);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear", this::clearForm);
    }

    private void bind(KeyStroke key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) {
                    action.run();
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Comportamiento
    // ------------------------------------------------------------------

    private void loadRates() {
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setText("Cargando tarifas del peaje…");
        Async.run(() -> service.rates(session), list -> {
            rates.clear();
            list.forEach(r -> rates.put(r.getCategoryId(), r));
            for (int i = 1; i <= ROMAN.length; i++) {
                paintTile(i);
            }
            statusLabel.setText(list.isEmpty() ? "Este peaje no tiene tarifas registradas" : " ");
            plateField.requestFocusInWindow();
        }, error -> showError(error.getMessage()));
    }

    private void selectCategory(int categoryId) {
        CategoryRate rate = rates.get(categoryId);
        if (rate == null) {
            return;
        }
        selected = rate;
        for (int i = 1; i <= ROMAN.length; i++) {
            paintTile(i);
        }
        amountLabel.setText(Theme.money(rate.getPrice()));
        categoryLabel.setText("Categoría " + rate.getCode());
        statusLabel.setText(" ");
        updateChange();
        if (plateField.getText().isEmpty()) {
            plateField.requestFocusInWindow();
        } else if (CashierService.CASH.equals(method)) {
            receivedField.requestFocusInWindow();
        }
    }

    private void selectMethod(String newMethod) {
        method = newMethod;
        methodButtons.forEach((key, button) -> {
            if (key.equals(newMethod)) {
                button.setColors(Theme.PRIMARY, Theme.PRIMARY_DARK, Color.WHITE, null);
            } else {
                button.setColors(Color.WHITE, Theme.PRIMARY_SOFT, Theme.TEXT, Theme.INPUT_BORDER);
            }
        });
        boolean cash = CashierService.CASH.equals(newMethod);
        receivedField.setEnabled(cash);
        if (cash && !plateField.getText().isEmpty()) {
            receivedField.requestFocusInWindow();
        } else if (CashierService.TAG.equals(newMethod)) {
            tagField.requestFocusInWindow();
        }
        cashButtons.forEach(b -> b.setEnabled(cash));
        if (!cash) {
            receivedField.setText("");
        }
        tagField.setEnabled(CashierService.TAG.equals(newMethod));
        if (!tagField.isEnabled()) {
            tagField.setText("");
        }
        updateChange();
    }

    private void updateChange() {
        BigDecimal received = parseMoney(receivedField.getText());
        if (!CashierService.CASH.equals(method)) {
            showChange("Cambio", "No aplica", Theme.MUTED, Theme.BACKGROUND);
        } else if (selected == null || received == null) {
            showChange("Cambio", "$0", Theme.MUTED, Theme.BACKGROUND);
        } else {
            BigDecimal diff = received.subtract(selected.getPrice());
            if (diff.signum() < 0) {
                showChange("Falta", Theme.money(diff.negate()), Theme.DANGER, CHANGE_BAD_BG);
            } else {
                showChange("Cambio", Theme.money(diff), Theme.SUCCESS, CHANGE_OK_BG);
            }
        }
    }

    private void showChange(String title, String value, Color color, Color background) {
        changeTitle.setText(title);
        changeTitle.setForeground(color);
        changeLabel.setText(value);
        changeLabel.setForeground(color);
        changeBox.setBackground(background);
    }

    private void updateCashInBox() {
        cashInBoxLabel.setText("Efectivo en caja: " + Theme.money(log.totalBy(CashierService.CASH)));
    }

    private void pressKey(String key) {
        String digits = receivedField.getText().replaceAll("[^0-9]", "");
        if ("Borrar".equals(key)) {
            digits = digits.isEmpty() ? "" : digits.substring(0, digits.length() - 1);
        } else if (!(digits.isEmpty() && key.startsWith("0"))) {
            digits = digits + key;
        }
        receivedField.setText(digits);
    }

    private void charge() {
        if (!chargeButton.isEnabled()) {
            return;
        }
        if (selected == null) {
            showError("Seleccione la categoría del vehículo");
            return;
        }
        int categoryId = selected.getCategoryId();
        String plate = plateField.getText();
        String tag = tagField.getText();
        String paymentMethod = method;
        BigDecimal received = parseMoney(receivedField.getText());

        if (!confirmIfRepeated(plate)) {
            return;
        }

        chargeButton.setEnabled(false);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setText("Registrando cobro…");
        Async.run(() -> service.charge(session, categoryId, plate, paymentMethod, tag, received),
                receipt -> {
                    chargeButton.setEnabled(true);
                    log.add(receipt);
                    clearForm();
                    new ReceiptDialog(SwingUtilities.getWindowAncestor(this), receipt, false).setVisible(true);
                    plateField.requestFocusInWindow();
                },
                error -> {
                    chargeButton.setEnabled(true);
                    showError(error.getMessage());
                });
    }

    /**
     * Si la misma placa se cobró en esta caseta hace menos de 2 minutos, pide confirmación.
     * Es un aviso, no un bloqueo: el cajero decide.
     * @param plate Placa escrita.
     * @return true si se puede seguir con el cobro.
     */
    private boolean confirmIfRepeated(String plate) {
        Optional<ChargeReceipt> previous = service.findRecentDuplicate(
                log.getReceipts(), plate, session.getBooth(), LocalDateTime.now());
        if (!previous.isPresent()) {
            return true;
        }
        ChargeReceipt p = previous.get();
        long seconds = Math.max(0, Duration.between(p.getDateTime(), LocalDateTime.now()).getSeconds());
        String ago = seconds < 60 ? seconds + " segundos" : (seconds / 60) + " min " + (seconds % 60) + " s";
        String message = "<html><b>Posible cobro repetido</b><br><br>"
                + "La placa <b>" + p.getPlate() + "</b> ya pagó en esta caseta hace " + ago + "<br>"
                + "Tiquete " + p.getTicketNumber() + " · Cat. " + p.getCategory().getCode()
                + " · " + Theme.money(p.getAmount()) + " · " + METHOD_LABELS.getOrDefault(p.getPaymentMethod(), p.getPaymentMethod())
                + "<br><br>¿Desea cobrar de nuevo?</html>";
        Object[] options = {"Sí, cobrar", "No"};
        int answer = JOptionPane.showOptionDialog(this, message, "Cobro repetido",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (answer != JOptionPane.YES_OPTION) {
            statusLabel.setForeground(Theme.MUTED);
            statusLabel.setText("Cobro cancelado: la placa " + p.getPlate() + " ya había pagado");
            return false;
        }
        return true;
    }

    private void clearForm() {
        selected = null;
        for (int i = 1; i <= ROMAN.length; i++) {
            paintTile(i);
        }
        plateField.setText("");
        tagField.setText("");
        receivedField.setText("");
        amountLabel.setText("$0");
        categoryLabel.setText("Seleccione una categoría");
        statusLabel.setText(" ");
        updateChange();
        plateField.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private void paintTile(int categoryId) {
        FlatButton tile = tiles[categoryId - 1];
        CategoryRate rate = rates.get(categoryId);
        boolean isSelected = selected != null && selected.getCategoryId() == categoryId;
        if (isSelected) {
            tile.setColors(Theme.PRIMARY, Theme.PRIMARY_DARK, Color.WHITE, null);
        } else {
            tile.setColors(Color.WHITE, Theme.PRIMARY_SOFT, Theme.TEXT, Theme.INPUT_BORDER);
        }
        tile.setEnabled(rate != null);

        String price = rate != null ? Theme.money(rate.getPrice()) : (rates.isEmpty() ? "…" : "No aplica");
        String desc = rate != null && rate.getDescription() != null ? rate.getDescription() : "";
        String descColor = isSelected ? "#DCE4F2" : "#6B7280";
        tile.setToolTipText(desc.isEmpty() ? null : desc + " (F" + categoryId + ")");
        tile.setText("<html><div style='width:112px'>"
                + "<span style='font-size:19pt'><b>" + ROMAN[categoryId - 1] + "</b></span><br>"
                + "<span style='font-size:12pt'><b>" + price + "</b></span><br>"
                + "<span style='font-size:8.5pt;color:" + (rate != null ? descColor : "#9CA3AF") + "'>"
                + desc + "</span></div></html>");
    }

    private void showError(String message) {
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setText("<html>" + message + "</html>");
    }

    static BigDecimal parseMoney(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : new BigDecimal(digits);
    }

    /** Convierte a mayúsculas lo que se escribe y limita la longitud. */
    private static final class UpperCaseFilter extends DocumentFilter {
        private final int max;

        UpperCaseFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String upper = text == null ? "" : text.toUpperCase(Locale.ROOT);
            int room = max - (fb.getDocument().getLength() - length);
            if (room <= 0) {
                return;
            }
            super.replace(fb, offset, length, upper.length() > room ? upper.substring(0, room) : upper, attrs);
        }
    }

    /** Solo deja escribir números y los muestra con puntos de miles (ej. 50.000). */
    private static final class MoneyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            replace(fb, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset) + (text == null ? "" : text)
                    + current.substring(offset + length);
            String digits = next.replaceAll("[^0-9]", "").replaceFirst("^0+(?=\\d)", "");
            if (digits.length() > MAX_DIGITS) {
                return;
            }
            String formatted = digits.isEmpty() ? "" : Theme.money(new BigDecimal(digits)).substring(1);
            super.replace(fb, 0, fb.getDocument().getLength(), formatted, attrs);
        }
    }
}
