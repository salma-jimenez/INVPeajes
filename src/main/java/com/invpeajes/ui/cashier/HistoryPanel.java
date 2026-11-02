package com.invpeajes.ui.cashier;

import com.invpeajes.model.ChargeReceipt;
import com.invpeajes.service.CashierService;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * Pantalla "Mis cobros del turno": indicadores del turno, tabla de cobros con búsqueda por placa
 * y reimpresión del tiquete, y totales por medio de pago.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int COL_PLATE = 2;
    private static final int COL_PAY = 4;
    private static final int COL_REPRINT = 6;

    private final ShiftLog log;
    private final JLabel countValue = new JLabel("0");
    private final JLabel totalValue = new JLabel("$0");
    private final JLabel cashValue = new JLabel("$0");
    private final JLabel emptyLabel = Theme.muted("Todavía no hay cobros en este turno.");
    private final JTextField searchField = Theme.textField();
    private final DefaultTableModel model =
            new DefaultTableModel(new Object[] {"Hora", "Tiquete", "Placa", "Cat.", "Pago", "Valor", ""}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final MethodRow cashRow = new MethodRow("Efectivo", Theme.WARNING);
    private final MethodRow cardRow = new MethodRow("Tarjeta", Theme.PRIMARY);
    private final MethodRow tagRow = new MethodRow("TAG Colpass", Theme.PURPLE);

    /**
     * @param log Registro de cobros del turno.
     */
    public HistoryPanel(ShiftLog log) {
        this.log = log;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.add(Theme.statCard("Cobros del turno", countValue, Theme.PRIMARY));
        stats.add(Theme.statCard("Recaudado del turno", totalValue, Theme.SUCCESS));
        stats.add(Theme.statCard("Efectivo en caja", cashValue, Theme.WARNING));
        add(stats, BorderLayout.NORTH);

        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 14);
        split.add(buildTableCard(), c);
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        split.add(buildMethodsCard(), c);
        add(split, BorderLayout.CENTER);

        log.addListener(this::refresh);
        refresh();
    }

    private JPanel buildTableCard() {
        JPanel card = Theme.card();
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.add(Theme.title("Cobros del turno"), BorderLayout.WEST);
        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setOpaque(false);
        search.add(Theme.muted("Buscar placa"), BorderLayout.WEST);
        searchField.setColumns(10);
        searchField.setFont(Theme.font(Font.PLAIN, 13));
        search.add(searchField, BorderLayout.CENTER);
        header.add(search, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setRowSorter(sorter);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setForeground(Theme.TEXT);
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12));
        table.getTableHeader().setForeground(Theme.MUTED);
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        table.getColumnModel().getColumn(COL_PAY).setCellRenderer(new PillRenderer());
        DefaultTableCellRenderer link = new DefaultTableCellRenderer();
        link.setForeground(Theme.PRIMARY);
        link.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COL_REPRINT).setCellRenderer(link);
        table.getColumnModel().getColumn(COL_REPRINT).setPreferredWidth(90);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0 && table.columnAtPoint(e.getPoint()) == COL_REPRINT) {
                    ChargeReceipt receipt = log.getReceipts().get(table.convertRowIndexToModel(viewRow));
                    new ReceiptDialog(SwingUtilities.getWindowAncestor(HistoryPanel.this), receipt, true)
                            .setVisible(true);
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean onLink = table.rowAtPoint(e.getPoint()) >= 0
                        && table.columnAtPoint(e.getPoint()) == COL_REPRINT;
                table.setCursor(Cursor.getPredefinedCursor(onLink ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }

            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }

            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);
        card.add(emptyLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMethodsCard() {
        JPanel card = Theme.card();
        card.setPreferredSize(new Dimension(300, 0));
        card.add(Theme.title("Por medio de pago"), BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 12));
        rows.setOpaque(false);
        rows.add(cashRow);
        rows.add(cardRow);
        rows.add(tagRow);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(rows, BorderLayout.NORTH);
        card.add(wrapper, BorderLayout.CENTER);
        return card;
    }

    private void refresh() {
        model.setRowCount(0);
        for (ChargeReceipt r : log.getReceipts()) {
            model.addRow(new Object[] {
                r.getDateTime().format(TIME), r.getTicketNumber(), r.getPlate(), r.getCategory().getCode(),
                r.getPaymentMethod(), Theme.money(r.getAmount()), "Reimprimir"
            });
        }
        BigDecimal total = log.total();
        countValue.setText(String.valueOf(log.count()));
        totalValue.setText(Theme.money(total));
        cashValue.setText(Theme.money(log.totalBy(CashierService.CASH)));
        cashRow.setValues(log.totalBy(CashierService.CASH), total);
        cardRow.setValues(log.totalBy(CashierService.CARD), total);
        tagRow.setValues(log.totalBy(CashierService.TAG), total);
        emptyLabel.setVisible(log.count() == 0);
    }

    private void applyFilter() {
        String text = searchField.getText().trim().toUpperCase(Locale.ROOT);
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter(
                java.util.regex.Pattern.quote(text), COL_PLATE));
    }

    /** Fila de "Por medio de pago": nombre, total y barra proporcional. */
    private static final class MethodRow extends JPanel {
        private final JLabel amount = new JLabel("$0", SwingConstants.RIGHT);
        private final Bar bar;

        MethodRow(String name, Color color) {
            super(new BorderLayout(0, 6));
            setOpaque(false);
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            JLabel label = new JLabel(name);
            label.setFont(Theme.font(Font.PLAIN, 14));
            label.setForeground(Theme.TEXT);
            amount.setFont(Theme.font(Font.BOLD, 14));
            amount.setForeground(Theme.TEXT);
            top.add(label, BorderLayout.WEST);
            top.add(amount, BorderLayout.EAST);
            bar = new Bar(color);
            add(top, BorderLayout.NORTH);
            add(bar, BorderLayout.CENTER);
        }

        void setValues(BigDecimal value, BigDecimal total) {
            amount.setText(Theme.money(value));
            bar.ratio = total.signum() == 0 ? 0
                    : value.divide(total, 4, RoundingMode.HALF_UP).doubleValue();
            bar.repaint();
        }
    }

    /** Barra horizontal con el porcentaje de un medio de pago. */
    private static final class Bar extends JComponent {
        private final Color color;
        private double ratio;

        Bar(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(100, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 8;
            int y = (getHeight() - h) / 2;
            g2.setColor(Theme.BORDER);
            g2.fillRoundRect(0, y, getWidth(), h, h, h);
            g2.setColor(color);
            g2.fillRoundRect(0, y, (int) Math.round(getWidth() * ratio), h, h, h);
            g2.dispose();
        }
    }

    /** Muestra el medio de pago como una etiqueta redondeada de color. */
    private static final class PillRenderer extends JComponent implements TableCellRenderer {
        private String text = "";
        private Color background;
        private Color foreground;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String method = String.valueOf(value);
            text = CashierPanel.METHOD_LABELS.getOrDefault(method, method);
            switch (method) {
                case CashierService.CASH:
                    background = new Color(0xFEF3C7);
                    foreground = new Color(0xB45309);
                    break;
                case CashierService.CARD:
                    background = new Color(0xDBEAFE);
                    foreground = new Color(0x1D4ED8);
                    break;
                default:
                    background = new Color(0xEDE9FE);
                    foreground = new Color(0x6D28D9);
                    break;
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.font(Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 18;
            int h = fm.getHeight() + 4;
            int y = (getHeight() - h) / 2;
            g2.setColor(background);
            g2.fillRoundRect(2, y, w, h, h, h);
            g2.setColor(foreground);
            g2.drawString(text, 11, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
