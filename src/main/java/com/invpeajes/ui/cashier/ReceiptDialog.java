package com.invpeajes.ui.cashier;

import com.invpeajes.model.ChargeReceipt;
import com.invpeajes.service.ReceiptFormatter;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
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
 * @version 1.2
 */
public class ReceiptDialog extends JDialog {

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
     * El texto lo arma {@link ReceiptFormatter}, que también usa el recibo por correo.
     * @param r Cobro guardado.
     * @param copy true si es una reimpresión.
     * @return Texto del tiquete.
     */
    public static String format(ChargeReceipt r, boolean copy) {
        return ReceiptFormatter.format(r, copy);
    }

    private void print() {
        try {
            area.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + e.getMessage(),
                    "Impresión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
