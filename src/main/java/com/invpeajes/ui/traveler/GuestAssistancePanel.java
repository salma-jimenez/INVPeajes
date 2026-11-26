package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.model.Traveler;
import com.invpeajes.service.AuthService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

/**
 * "Pedir asistencia" para el invitado: la asistencia necesita una cuenta, así que aquí se
 * explica eso y se ofrece un registro corto que, al terminar, lleva directo al formulario
 * de asistencia (sin volver a iniciar sesión).
 *
 * <p>Al lado van los números para una urgencia (123 y los teléfonos del peaje escogido),
 * para que quien esté en una emergencia pueda llamar sin registrarse.</p>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class GuestAssistancePanel extends JPanel {

    private final AuthService auth;
    private final Consumer<Traveler> onRegistered;
    private final Runnable onLogin;

    private final JTextField first = Theme.textField();
    private final JTextField last = Theme.textField();
    private final JTextField email = Theme.textField();
    private final JTextField phone = Theme.textField();
    private final JTextField plate = Theme.textField();
    private final JPasswordField password = new JPasswordField();
    private final JCheckBox terms = new JCheckBox("Acepto que RutaPeaje use estos datos para atender mis solicitudes");
    private final JLabel error = TravelerUi.label(" ", Font.PLAIN, 13, Theme.DANGER);
    private final FlatButton create = Theme.primaryButton("Crear cuenta y pedir asistencia");

    private final JComboBox<Object> toll = new JComboBox<>();
    private final JLabel tollPhone = TravelerUi.label("—", Font.BOLD, 15, Theme.TEXT);
    private final JLabel towPhone = TravelerUi.label("—", Font.BOLD, 15, Theme.TEXT);
    private Catalog catalog;

    /**
     * @param auth         servicio de cuentas
     * @param onRegistered qué hacer con la cuenta recién creada (abrir la ventana del viajero)
     * @param onLogin      abrir el inicio de sesión para quien ya tiene cuenta
     */
    public GuestAssistancePanel(AuthService auth, Consumer<Traveler> onRegistered, Runnable onLogin) {
        super(new BorderLayout(18, 0));
        this.auth = auth;
        this.onRegistered = onRegistered;
        this.onLogin = onLogin;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(buildRegister(), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.add(buildEmergency(), BorderLayout.NORTH);
        side.setPreferredSize(new Dimension(360, 0));

        add(scroll, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
    }

    // ------------------------------------------------------------------ Registro corto

    private JComponent buildRegister() {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel head = new JPanel(new BorderLayout(14, 0));
        head.setOpaque(false);
        head.setAlignmentX(LEFT_ALIGNMENT);
        head.add(new LockIcon(40, Theme.PRIMARY, Theme.PRIMARY_SOFT), BorderLayout.WEST);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(TravelerUi.label("Para pedir asistencia necesita una cuenta", Font.BOLD, 20, Theme.TEXT));
        titles.add(TravelerUi.label("Créela en un minuto y quedará lista para pedir ayuda y seguirla paso a paso.",
                Font.PLAIN, 13, Theme.MUTED));
        head.add(titles, BorderLayout.CENTER);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        card.add(head);
        card.add(Box.createVerticalStrut(20));

        card.add(pair(TravelerUi.field("Nombre", first), TravelerUi.field("Apellido", last)));
        card.add(Box.createVerticalStrut(12));
        card.add(TravelerUi.field("Correo (con él inicia sesión)", email));
        card.add(Box.createVerticalStrut(12));
        card.add(pair(TravelerUi.field("Teléfono", phone), TravelerUi.field("Placa (opcional)", plate)));
        card.add(Box.createVerticalStrut(12));
        password.setFont(Theme.font(Font.PLAIN, 15));
        password.setBorder(Theme.inputBorder());
        card.add(TravelerUi.field("Contraseña (mínimo " + AuthService.MIN_PASSWORD_LENGTH + " caracteres)", password));
        card.add(Box.createVerticalStrut(12));
        terms.setOpaque(false);
        terms.setFont(Theme.font(Font.PLAIN, 13));
        terms.setForeground(Theme.TEXT);
        terms.setAlignmentX(LEFT_ALIGNMENT);
        card.add(terms);
        card.add(Box.createVerticalStrut(14));
        create.setAlignmentX(LEFT_ALIGNMENT);
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        create.addActionListener(e -> register());
        card.add(create);
        card.add(Box.createVerticalStrut(6));
        card.add(left(error));
        card.add(Box.createVerticalStrut(8));

        JLabel login = TravelerUi.label("<html>¿Ya tiene cuenta? <span style='color:#2563eb'><b>Iniciar sesión</b></span>"
                + "</html>", Font.PLAIN, 13, Theme.MUTED);
        login.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        login.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onLogin.run();
            }
        });
        card.add(left(login));
        for (JTextField f : new JTextField[]{first, last, email, phone, plate}) {
            f.addActionListener(e -> register());
        }
        password.addActionListener(e -> register());
        return card;
    }

    private void register() {
        error.setText(" ");
        if (!terms.isSelected()) {
            error.setText("Debe aceptar el uso de sus datos para crear la cuenta.");
            return;
        }
        String f = first.getText();
        String l = last.getText();
        String m = email.getText();
        String p = phone.getText();
        String pl = plate.getText();
        char[] pass = password.getPassword();
        char[] confirm = pass.clone();
        create.setEnabled(false);
        create.setText("Creando cuenta...");
        Async.run(() -> auth.registerTraveler(f, l, m, p, pl, pass, confirm), traveler -> {
            password.setText("");
            onRegistered.accept(traveler);
        }, failure -> {
            create.setEnabled(true);
            create.setText("Crear cuenta y pedir asistencia");
            error.setText("<html>" + (failure instanceof IllegalArgumentException ? failure.getMessage()
                    : "No se pudo crear la cuenta. Revise su conexión e intente de nuevo.") + "</html>");
        });
    }

    // ------------------------------------------------------------------ Urgencias

    private JComponent buildEmergency() {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.add(left(TravelerUi.label("¿Es una emergencia?", Font.BOLD, 17, Theme.TEXT)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(TravelerUi.label("<html>Si hay heridos o peligro en la vía, llame ya; no necesita cuenta.</html>",
                Font.PLAIN, 13, Theme.MUTED)));
        card.add(Box.createVerticalStrut(14));

        JPanel big = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFEE2E2));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        big.setOpaque(false);
        big.setAlignmentX(LEFT_ALIGNMENT);
        big.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        big.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        big.add(TravelerUi.label("123", Font.BOLD, 30, new Color(0xB91C1C)), BorderLayout.WEST);
        big.add(TravelerUi.label("<html>Línea nacional de<br>emergencias</html>", Font.PLAIN, 13,
                new Color(0x7F1D1D)), BorderLayout.CENTER);
        card.add(big);
        card.add(Box.createVerticalStrut(16));

        card.add(left(TravelerUi.label("Teléfonos del peaje más cercano", Font.BOLD, 13, Theme.TEXT)));
        card.add(Box.createVerticalStrut(6));
        TravelerUi.styleCombo(toll);
        toll.addItem("Escoja el peaje");
        toll.addActionListener(e -> showPhones());
        JPanel tollField = TravelerUi.field("Peaje", toll);
        card.add(tollField);
        card.add(Box.createVerticalStrut(10));
        card.add(phoneRow("Línea de grúas", towPhone));
        card.add(Box.createVerticalStrut(4));
        card.add(phoneRow("Teléfono del peaje", tollPhone));
        return card;
    }

    private void showPhones() {
        Object o = toll.getSelectedItem();
        if (o instanceof Toll) {
            Toll t = (Toll) o;
            towPhone.setText("<html>" + phones(t.getTowPhone()) + "</html>");
            tollPhone.setText("<html>" + phones(t.getPhone()) + "</html>");
        } else {
            towPhone.setText("—");
            tollPhone.setText("—");
        }
    }

    /**
     * @param catalog catálogo cargado (para la lista de peajes)
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        toll.removeAllItems();
        toll.addItem("Escoja el peaje");
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
                    setText(t.getName() + " · " + GuestAssistancePanel.this.catalog.departmentName(t.getDepartmentId()));
                }
                return this;
            }
        });
    }

    // ------------------------------------------------------------------ Ayudas

    private static JPanel card() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    /** Etiqueta arriba y número(s) debajo: así un teléfono largo nunca tapa la etiqueta. */
    private static JPanel phoneRow(String caption, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(0, 2)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        row.add(TravelerUi.label(caption, Font.PLAIN, 13, Theme.MUTED), BorderLayout.NORTH);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    /**
     * Limpia un campo de teléfonos de la base de datos: algunos peajes traen varios números
     * unidos con guiones largos, barras o comas ("3102613190----------- 3002149460").
     *
     * @param raw texto guardado
     * @return los números en HTML, uno por línea (o "—" si no hay)
     */
    public static String phones(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        StringBuilder out = new StringBuilder();
        for (String part : raw.split("\\s*(?:-{2,}|/|;|,|\\s+y\\s+)\\s*")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                if (out.length() > 0) {
                    out.append("<br>");
                }
                out.append(p);
            }
        }
        return out.length() == 0 ? "—" : out.toString();
    }

    private static JPanel pair(JComponent a, JComponent b) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 12);
        row.add(a, c);
        c.insets = new Insets(0, 0, 0, 0);
        row.add(b, c);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, a.getPreferredSize().height));
        return row;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    /** Candado dibujado (sin depender de que la fuente tenga el símbolo). */
    public static final class LockIcon extends JComponent {
        private final int size;
        private final Color color;
        private final Color background;

        /**
         * @param size       lado en píxeles
         * @param color      color del candado
         * @param background color del círculo de fondo, o null para no dibujarlo
         */
        public LockIcon(int size, Color color, Color background) {
            this.size = size;
            this.color = color;
            this.background = background;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y0 = (getHeight() - size) / 2;
            if (background != null) {
                g2.setColor(background);
                g2.fillOval(0, y0, size, size);
            }
            double s = background != null ? size * 0.5 : size;
            double x = (size - s) / 2;
            double y = y0 + (size - s) / 2;
            g2.setColor(color);
            g2.setStroke(new BasicStroke((float) Math.max(1.5, s / 9), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int bodyY = (int) (y + s * 0.45);
            g2.drawArc((int) (x + s * 0.22), (int) (y + s * 0.05), (int) (s * 0.56), (int) (s * 0.7), 0, 180);
            g2.drawLine((int) (x + s * 0.22), (int) (y + s * 0.4), (int) (x + s * 0.22), bodyY);
            g2.drawLine((int) (x + s * 0.78), (int) (y + s * 0.4), (int) (x + s * 0.78), bodyY);
            g2.fillRoundRect((int) (x + s * 0.1), bodyY, (int) (s * 0.8), (int) (s * 0.52), (int) (s * 0.15),
                    (int) (s * 0.15));
            g2.dispose();
        }
    }
}
