package com.invpeajes.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Piezas de formulario con el estilo de la aplicación: campos con su nombre arriba, filas de dos
 * campos, listas desplegables, tablas y ventanas de diálogo para registrar, editar o confirmar.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public final class Forms {

    /** Formato de fecha de los campos (ej. 12/11/2026). */
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Forms() {
    }

    /**
     * Campo con su nombre arriba.
     * @param label Nombre del campo (ej. "Cédula *").
     * @param input Campo de texto, lista, etc.
     * @return Panel con los dos.
     */
    public static JPanel field(String label, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setFont(Theme.font(Font.PLAIN, 13));
        name.setForeground(Theme.MUTED);
        panel.add(name, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    /**
     * Fila con varios campos del mismo ancho.
     * @param columns Campos (normalmente dos).
     * @return La fila.
     */
    public static JPanel row(JComponent... columns) {
        JPanel row = new JPanel(new GridLayout(1, columns.length, 12, 0));
        row.setOpaque(false);
        for (JComponent c : columns) {
            row.add(c);
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /**
     * @param text Texto inicial (puede ser null).
     * @return Campo de texto con el estilo de la aplicación.
     */
    public static JTextField text(String text) {
        JTextField field = Theme.textField();
        field.setText(text == null ? "" : text);
        return field;
    }

    /**
     * @param text Texto.
     * @return Campo gris que no se puede editar.
     */
    public static JTextField readOnly(String text) {
        JTextField field = text(text);
        field.setEditable(false);
        field.setBackground(Theme.DISABLED);
        field.setForeground(Theme.MUTED);
        return field;
    }

    /**
     * @return Campo de contraseña con el estilo de la aplicación.
     */
    public static JPasswordField password() {
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.font(Font.PLAIN, 15));
        field.setBorder(Theme.inputBorder());
        return field;
    }

    /**
     * Lista desplegable que muestra un texto propio para cada valor.
     * @param text Cómo mostrar cada valor.
     * @param values Valores.
     * @param <T> Tipo de los valores.
     * @return La lista.
     */
    public static <T> JComboBox<T> combo(java.util.function.Function<T, String> text, java.util.List<T> values) {
        JComboBox<T> combo = new JComboBox<>();
        values.forEach(combo::addItem);
        combo.setFont(Theme.font(Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                String shown = text.apply((T) value);
                setText(shown == null ? "—" : shown);
                setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                return this;
            }
        });
        combo.setPreferredSize(new Dimension(120, 36));
        return combo;
    }

    /**
     * @param text Fecha escrita como dd/MM/yyyy.
     * @return La fecha, o null si está vacía o mal escrita.
     */
    public static LocalDate parseDate(String text) {
        try {
            return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * @param date Fecha.
     * @return dd/MM/yyyy, o vacío si es null.
     */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    /**
     * Panel que se ajusta al ancho de su barra de desplazamiento (solo se desplaza hacia abajo).
     * @param content Contenido; queda arriba.
     * @return El panel, listo para meter en un JScrollPane.
     */
    public static JPanel fitWidth(JComponent content) {
        JPanel panel = new WidthPanel();
        panel.setOpaque(false);
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    /** Panel que siempre toma el ancho visible del JScrollPane. */
    private static final class WidthPanel extends JPanel implements javax.swing.Scrollable {
        WidthPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Da a una tabla el estilo de la aplicación: filas altas, sin rejilla, encabezado gris y fila
     * escogida en azul claro.
     * @param table La tabla.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(new Color(0xEFF6FF));
        table.setSelectionForeground(Theme.TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(Color.WHITE);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setFont(Theme.font(Font.BOLD, 12));
                setForeground(Theme.MUTED);
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
                return this;
            }
        });
        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, false, row, column);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF0F2F6)),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cells);
    }

    /**
     * Renderizador que muestra una pastilla de color dentro de una celda.
     * @param pill Cómo crear la pastilla a partir del valor de la celda.
     * @return El renderizador.
     */
    public static javax.swing.table.TableCellRenderer pillRenderer(java.util.function.Function<Object, Pill> pill) {
        return (table, value, selected, focus, row, column) -> {
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            cell.setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            cell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF0F2F6)));
            if (value != null) {
                cell.add(pill.apply(value));
            }
            return cell;
        };
    }

    // ------------------------------------------------------------------ Diálogos

    /**
     * Abre una ventana con un formulario. Al oprimir el botón principal ejecuta {@code action}
     * fuera del hilo de la interfaz; si falla, muestra el error en rojo sin cerrar la ventana; si
     * funciona, la cierra y llama a {@code onDone}.
     *
     * @param parent Componente de la ventana dueña.
     * @param title Título.
     * @param subtitle Texto gris debajo del título (puede ser null).
     * @param body Formulario.
     * @param okText Texto del botón principal, o null para mostrar solo "Cerrar".
     * @param danger true si el botón principal es rojo (desactivar, eliminar).
     * @param action Trabajo a hacer (ej. guardar); devuelve el resultado.
     * @param onDone Qué hacer con el resultado, ya cerrada la ventana.
     * @param <T> Tipo del resultado.
     */
    public static <T> void dialog(Component parent, String title, String subtitle, JComponent body, String okText,
                                  boolean danger, Supplier<T> action, Consumer<T> onDone) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        JPanel head = new JPanel(new BorderLayout(0, 4));
        head.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.font(Font.BOLD, 19));
        titleLabel.setForeground(Theme.TEXT);
        head.add(titleLabel, BorderLayout.NORTH);
        if (subtitle != null) {
            JLabel sub = new JLabel("<html><div style='width:" + Math.max(300, (body.getPreferredSize().width - 20) * 3 / 4)
                    + "px'>" + subtitle + "</div></html>");
            sub.setFont(Theme.font(Font.PLAIN, 13));
            sub.setForeground(Theme.MUTED);
            head.add(sub, BorderLayout.CENTER);
        }
        root.add(head, BorderLayout.NORTH);
        body.setOpaque(false);
        root.add(body, BorderLayout.CENTER);

        JLabel error = new JLabel(" ");
        error.setFont(Theme.font(Font.PLAIN, 13));
        error.setForeground(Theme.DANGER);
        FlatButton cancel = Theme.secondaryButton(okText == null ? "Cerrar" : "Cancelar");
        FlatButton ok = Theme.primaryButton(okText == null ? "Aceptar" : okText);
        ok.setVisible(okText != null);
        if (danger) {
            ok.setColors(Theme.DANGER, new Color(0xB91C1C), Color.WHITE, null);
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        JPanel south = new JPanel(new BorderLayout(10, 0));
        south.setOpaque(false);
        south.add(error, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dialog.dispose());
        ok.addActionListener(e -> {
            ok.setEnabled(false);
            error.setText(" ");
            Async.run(action, result -> {
                dialog.dispose();
                onDone.accept(result);
            }, ex -> {
                ok.setEnabled(true);
                error.setText("<html>" + ex.getMessage() + "</html>");
                dialog.pack();
            });
        });
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(okText == null ? cancel : ok);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.max(440, dialog.getWidth()), dialog.getHeight()));
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Pregunta antes de hacer algo (ej. desactivar a alguien) y lo hace si la respuesta es sí.
     * @param parent Componente de la ventana dueña.
     * @param title Pregunta (ej. "¿Desactivar a Juan Restrepo?").
     * @param message Explicación (puede llevar &lt;b&gt;).
     * @param okText Texto del botón para confirmar.
     * @param danger true si el botón es rojo.
     * @param action Trabajo a hacer.
     * @param onDone Qué hacer después.
     */
    public static void confirm(Component parent, String title, String message, String okText, boolean danger,
                               Runnable action, Runnable onDone) {
        JLabel text = new JLabel("<html><div style='width:330px'>" + message + "</div></html>");
        text.setFont(Theme.font(Font.PLAIN, 14));
        text.setForeground(Theme.TEXT);
        dialog(parent, title, null, text, okText, danger, () -> {
            action.run();
            return Boolean.TRUE;
        }, ok -> onDone.run());
    }

    /**
     * Muestra un aviso con un solo botón.
     * @param parent Componente de la ventana dueña.
     * @param title Título.
     * @param message Mensaje.
     */
    public static void info(Component parent, String title, String message) {
        javax.swing.JOptionPane.showMessageDialog(parent, message, title, javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Caja amarilla con un aviso (ej. "Este peaje no tiene jefe de estación").
     * @param html Texto (puede llevar &lt;b&gt;).
     * @return La caja.
     */
    public static JLabel note(String html) {
        JLabel note = new JLabel("<html>" + html + "</html>");
        note.setFont(Theme.font(Font.PLAIN, 12));
        note.setForeground(new Color(0x92400E));
        note.setOpaque(true);
        note.setBackground(new Color(0xFFFBEB));
        note.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFDE68A)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        return note;
    }
}
