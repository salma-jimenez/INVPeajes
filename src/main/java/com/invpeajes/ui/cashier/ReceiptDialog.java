package com.invpeajes.ui.cashier;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.ChargeReceipt;
import com.invpeajes.service.CashierService;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Muestra el tiquete de un cobro ya confirmado y permite imprimirlo.
 * La impresión usa {@link JTextArea#print()}, que abre el diálogo de impresoras del sistema
 * (también sirve "Microsoft Print to PDF" para guardarlo como PDF).
 *
 * @author Luis Guerrero
 * @version 1.1
 */
public class ReceiptDialog extends JDialog {

    /** Ancho del tiquete en caracteres (impresora térmica de 80 mm). */
    private static final int WIDTH = 40;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    /** Línea de la Policía de Carreteras, para peajes sin teléfono registrado. */
    private static final String ROAD_EMERGENCY = "#767 Policía de Carreteras";
    private final JTextArea area = new JTextArea();

    /**
     * @param owner Ventana dueña.
     * @param receipt Cobro ya guardado.
     * @param copy true si es una reimpresión (el tiquete dice "COPIA").
     */
    public ReceiptDialog(Window owner, ChargeReceipt receipt, boolean copy) {
        super(owner, (copy ? "Copia del tiquete " : "Tiquete ") + receipt.getTicketNumber(),
                ModalityType.APPLICATION_MODAL);

        area.setText(format(receipt, copy));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setForeground(Theme.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        area.setCaretPosition(0);

        FlatButton print = Theme.secondaryButton("Imprimir");
        FlatButton next = Theme.primaryButton(copy ? "Cerrar  (Enter)" : "Nuevo cobro  (Enter)");
        print.addActionListener(e -> print());
        next.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttons.setBackground(Theme.BACKGROUND);
        buttons.add(print);
        buttons.add(next);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(next);
        pack();
        if (getHeight() > 680) {
            setSize(getWidth(), 680);
        }
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Arma el texto del tiquete con ancho fijo para impresora térmica o PDF.
     * @param r Cobro guardado.
     * @param copy true si es una reimpresión.
     * @return Texto del tiquete.
     */
    public static String format(ChargeReceipt r, boolean copy) {
        CashierSession s = r.getSession();
        String line = "-".repeat(WIDTH);
        List<String> out = new ArrayList<>();
        out.add(center("RUTAPEAJE COLOMBIA"));
        out.add(center("Peaje " + s.getTollName()));
        if (s.getConcessionaire() != null) {
            wrap(s.getConcessionaire()).forEach(l -> out.add(center(l)));
        }
        out.add(center("Caseta " + s.getBooth().getBoothNumber()));
        if (copy) {
            out.add(center("*** COPIA ***"));
        }
        out.add(line);
        out.add(pair("Tiquete:", r.getTicketNumber()));
        out.add(pair("Fecha:", r.getDateTime().format(DATE)));
        out.add(pair("Operador:", s.getOperatorName()));
        out.add(line);
        out.add(pair("Placa:", r.getPlate()));
        out.add(pair("Categoría:", r.getCategory().getCode()));
        if (r.getCategory().getDescription() != null) {
            out.addAll(wrap(r.getCategory().getDescription()));
        }
        out.add(pair("Medio de pago:", r.getPaymentMethod()));
        if (r.getTagCode() != null) {
            out.add(pair("TAG:", r.getTagCode()));
        }
        out.add(line);
        out.add(pair("VALOR PEAJE", Theme.money(r.getAmount())));
        if (CashierService.CASH.equals(r.getPaymentMethod())) {
            out.add(pair("RECIBIDO", Theme.money(r.getReceived())));
            out.add(pair("CAMBIO", Theme.money(r.getChange())));
        }
        out.add(line);
        out.add(center("Gracias por su viaje. ¡Buen camino!"));
        out.add(line);
        out.add("CONTACTO DEL PEAJE");
        boolean anyPhone = false;
        if (hasText(s.getTollPhone())) {
            out.add("Atención:");
            wrap(clean(s.getTollPhone())).forEach(l -> out.add("  " + l));
            anyPhone = true;
        }
        if (hasText(s.getTowPhone())) {
            out.add("Grúa y emergencias en la vía:");
            wrap(clean(s.getTowPhone())).forEach(l -> out.add("  " + l));
            anyPhone = true;
        }
        out.add(anyPhone ? "Línea nacional:" : "Emergencias en la vía (línea nacional):");
        out.add("  " + ROAD_EMERGENCY);
        return String.join("\n", out);
    }

    private void print() {
        try {
            area.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + e.getMessage(),
                    "Impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Une espacios repetidos y quita guiones sueltos de los teléfonos de los datos abiertos. */
    private static String clean(String phones) {
        return phones.replaceAll("-{2,}", " ").replaceAll("\\s+", " ").trim();
    }

    /** Parte un texto largo en líneas del ancho del tiquete, sin cortar palabras. */
    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            int limit = WIDTH - 2;
            if (current.length() > 0 && current.length() + 1 + word.length() > limit) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word.length() > limit ? word.substring(0, limit) : word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String center(String text) {
        String t = text.length() <= WIDTH ? text : text.substring(0, WIDTH);
        int pad = (WIDTH - t.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + t;
    }

    private static String pair(String label, String value) {
        int spaces = WIDTH - label.length() - value.length();
        return spaces >= 1 ? label + " ".repeat(spaces) + value : label + " " + value;
    }
}
