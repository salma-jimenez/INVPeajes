package com.invpeajes.ui.login;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.Toll;
import com.invpeajes.model.TollBooth;
import com.invpeajes.model.Traveler;
import com.invpeajes.service.AuthService;
import com.invpeajes.service.CashierService;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.cashier.CashierFrame;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.station.StationFrame;
import com.invpeajes.ui.station.StationTexts;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Ventana de inicio de RutaPeaje. A la izquierda la franja azul ilustrada ({@link BrandPanel}) y a
 * la derecha los pasos del inicio de sesión:
 *
 * <ul>
 *   <li><b>Bienvenida</b>: "Soy viajero" o "Soy empleado", y el enlace para entrar como invitado.</li>
 *   <li><b>Empleados</b>: cédula y contraseña. Según el cargo, el operador de caseta escoge su caseta
 *       y abre la Caja; el jefe de estación ve el resumen de su peaje y entra a su panel; el
 *       administrador ve su saludo (su panel está en construcción).</li>
 *   <li><b>Viajeros</b>: iniciar sesión, crear cuenta, entrar como invitado y consultar una
 *       solicitud de asistencia con su número y teléfono.</li>
 * </ul>
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class LoginFrame extends JFrame {

    private static final String WELCOME = "welcome";
    private static final String EMPLOYEE = "employee";
    private static final String BOOTH = "booth";
    private static final String GREETING = "greeting";
    private static final String TRAVELER = "traveler";
    private static final String GUEST = "guest";
    private static final String TRACK = "track";
    private static final String LOGIN_TAB = "login";
    private static final String REGISTER_TAB = "register";
    private static final String PREF_EMPLOYEE_ID = "employeeId";
    private static final int CARD_WIDTH = 440;

    private final AuthService auth;
    private final CashierService cashier;
    private final StationManagerService station;
    private final Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private CashierSession employee;

    private final JTextField employeeId = Theme.textField();
    private final JPasswordField employeePassword = passwordField();
    private final JCheckBox rememberId = checkBox("Recordar mi cédula");
    private final JLabel employeeError = errorLabel();
    private final FlatButton employeeButton = wide(Theme.primaryButton("Ingresar"));

    private final JLabel boothHello = h2("");
    private final JLabel boothRole = sub("");
    private final JComboBox<TollBooth> boothCombo = new JComboBox<>();
    private final JLabel boothError = errorLabel();
    private final FlatButton boothButton = wide(green(Theme.primaryButton("Iniciar turno")));

    private final JLabel greetHello = h2("");
    private final JLabel greetRole = sub("");
    private final JPanel greetBody = new JPanel(new BorderLayout());
    private final FlatButton greetButton = wide(Theme.primaryButton("Ir al Centro de asistencia"));
    private final JLabel greetNote = sub("");

    private final CardLayout travelerTabs = new CardLayout();
    private final JPanel travelerTabPanel = new JPanel(travelerTabs) {
        @Override
        public Dimension getPreferredSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    return c.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }
    };
    private final JLabel loginTab = tab("Iniciar sesión");
    private final JLabel registerTab = tab("Registrarme");
    private final JTextField travelerEmail = Theme.textField();
    private final JPasswordField travelerPassword = passwordField();
    private final JLabel travelerError = errorLabel();
    private final FlatButton travelerButton = wide(Theme.primaryButton("Iniciar sesión"));
    private final JTextField regFirst = Theme.textField();
    private final JTextField regLast = Theme.textField();
    private final JTextField regEmail = Theme.textField();
    private final JTextField regPhone = Theme.textField();
    private final JPasswordField regPassword = passwordField();
    private final JPasswordField regConfirm = passwordField();
    private final JTextField regPlate = Theme.textField();
    private final JLabel regError = errorLabel();
    private final FlatButton regButton = wide(Theme.primaryButton("Crear cuenta"));

    private final JTextField trackNumber = Theme.textField();
    private final JTextField trackPhone = Theme.textField();
    private final JLabel trackError = errorLabel();
    private final FlatButton trackButton = wide(Theme.primaryButton("Consultar"));
    private final JPanel trackResult = new JPanel(new BorderLayout(0, 6));

    /**
     * Crea la ventana con los servicios indicados.
     * @param auth Servicio de inicio de sesión.
     * @param cashier Servicio de la caja (casetas y turno del operador).
     * @param station Servicio del jefe de estación (resumen y consulta de solicitudes).
     */
    public LoginFrame(AuthService auth, CashierService cashier, StationManagerService station) {
        super("RutaPeaje Colombia");
        this.auth = auth;
        this.cashier = cashier;
        this.station = station;

        content.setBackground(Theme.BACKGROUND);
        content.add(center(buildWelcome()), WELCOME);
        content.add(center(buildEmployee()), EMPLOYEE);
        content.add(center(buildBooth()), BOOTH);
        content.add(center(buildGreeting()), GREETING);
        content.add(center(buildTraveler()), TRAVELER);
        content.add(center(buildGuest()), GUEST);
        content.add(center(buildTrack()), TRACK);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new BrandPanel(), BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        show(WELCOME);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1366, 768);
        setLocationRelativeTo(null);
    }

    /** Abre la ventana de inicio con los servicios de la base de datos. */
    public static void open() {
        new LoginFrame(new AuthService(), new CashierService(), new StationManagerService()).setVisible(true);
    }

    /**
     * Punto de entrada de RutaPeaje.
     * @param args No se usan.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Si no se puede usar el estilo del sistema, Swing usa el suyo por defecto.
        }
        SwingUtilities.invokeLater(LoginFrame::open);
    }

    /**
     * Muestra un paso del inicio de sesión.
     * @param card Nombre del paso.
     */
    private void show(String card) {
        cards.show(content, card);
        switch (card) {
            case EMPLOYEE:
                getRootPane().setDefaultButton(employeeButton);
                SwingUtilities.invokeLater(() -> (employeeId.getText().isEmpty() ? employeeId : employeePassword)
                        .requestFocusInWindow());
                break;
            case BOOTH:
                getRootPane().setDefaultButton(boothButton);
                break;
            case GREETING:
                getRootPane().setDefaultButton(greetButton);
                break;
            case TRAVELER:
                getRootPane().setDefaultButton(travelerButton);
                break;
            case TRACK:
                getRootPane().setDefaultButton(trackButton);
                break;
            default:
                getRootPane().setDefaultButton(null);
                break;
        }
    }

    // ------------------------------------------------------------------ Bienvenida

    private JComponent buildWelcome() {
        JPanel panel = column();
        panel.setOpaque(false);
        JLabel title = new JLabel("¡Bienvenido!");
        title.setFont(Theme.font(Font.BOLD, 26));
        title.setForeground(Theme.TEXT);
        panel.add(left(title));
        panel.add(left(sub("¿Cómo quieres entrar?")));
        panel.add(Box.createVerticalStrut(24));

        JPanel choices = new JPanel(new GridLayout(1, 2, 22, 0));
        choices.setOpaque(false);
        FlatButton traveler = wide(Theme.primaryButton("Entrar como viajero"));
        traveler.addActionListener(e -> {
            selectTravelerTab(LOGIN_TAB);
            show(TRAVELER);
        });
        FlatButton staff = wide(Theme.secondaryButton("Entrar como empleado"));
        staff.addActionListener(e -> show(EMPLOYEE));
        choices.add(choice(Icon.CAR, Theme.PRIMARY, "Soy viajero", "Para quien va por la vía",
                new String[]{"Peajes, tarifas y mapa", "Costo de una ruta", "Pedir asistencia y ver su estado"},
                traveler, true));
        choices.add(choice(Icon.BADGE, Theme.SIDEBAR, "Soy empleado", "Cajeros, jefes de estación y administración",
                new String[]{"Caja de cobro (operador de caseta)", "Centro de asistencia y recursos",
                    "Estadísticas y administración"}, staff, false));
        panel.add(left(choices));
        panel.add(Box.createVerticalStrut(22));

        JPanel guest = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        guest.setOpaque(false);
        guest.add(sub("¿No quieres crear cuenta? Puedes "));
        guest.add(link("entrar como invitado", () -> show(GUEST)));
        guest.add(sub("."));
        guest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        panel.add(left(guest));
        return panel;
    }

    private JPanel choice(Icon icon, Color color, String title, String subtitle, String[] items,
                          FlatButton button, boolean highlighted) {
        JPanel card = new RoundedCard(highlighted ? Theme.PRIMARY : Theme.BORDER, highlighted ? 2 : 1);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));
        card.setPreferredSize(new Dimension(330, 320));
        card.add(left(new IconBox(icon, color)));
        card.add(Box.createVerticalStrut(16));
        JLabel name = new JLabel(title);
        name.setFont(Theme.font(Font.BOLD, 21));
        name.setForeground(Theme.TEXT);
        card.add(left(name));
        card.add(left(sub(subtitle)));
        card.add(Box.createVerticalStrut(8));
        for (String item : items) {
            JLabel line = new JLabel("<html><span style='color:#16A34A'>✓</span>&nbsp; " + item + "</html>");
            line.setFont(Theme.font(Font.PLAIN, 14));
            line.setForeground(Theme.TEXT);
            line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            card.add(left(line));
        }
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(16));
        card.add(left(button));
        return card;
    }

    // ------------------------------------------------------------------ Empleados

    private JComponent buildEmployee() {
        JPanel card = formCard();
        card.add(left(back(() -> show(WELCOME))));
        card.add(Box.createVerticalStrut(12));
        card.add(left(h2("Ingreso de empleados")));
        card.add(left(sub("El sistema te lleva a tu pantalla según tu cargo.")));
        addField(card, "Cédula", employeeId);
        addField(card, "Contraseña", withToggle(employeePassword));

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(rememberId, BorderLayout.WEST);
        row.add(link("¿Olvidaste tu contraseña?", () -> JOptionPane.showMessageDialog(this,
                "Pídele al jefe de estación de tu peaje o al administrador que restablezca tu contraseña.",
                "Recuperar contraseña", JOptionPane.INFORMATION_MESSAGE)), BorderLayout.EAST);
        card.add(Box.createVerticalStrut(12));
        card.add(left(row));
        card.add(left(employeeError));
        card.add(Box.createVerticalStrut(10));
        card.add(left(employeeButton));
        card.add(Box.createVerticalStrut(18));
        card.add(left(hint("<b>Operador de caseta</b> → elige su caseta y abre la Caja.<br>"
                + "<b>Jefe de estación</b> → Centro de asistencia, Recursos y Estadísticas.<br>"
                + "<b>Administrador</b> → peajes, tarifas, casetas y empleados.")));

        String saved = prefs.get(PREF_EMPLOYEE_ID, "");
        employeeId.setText(saved);
        rememberId.setSelected(!saved.isEmpty());
        employeeButton.addActionListener(e -> employeeLogin());
        return card;
    }

    private void employeeLogin() {
        String id = employeeId.getText().trim();
        char[] password = employeePassword.getPassword();
        employeeError.setText(" ");
        employeeButton.setEnabled(false);
        employeeButton.setText("Verificando...");
        Async.run(() -> auth.employeeLogin(id, password), session -> {
            employeeButton.setEnabled(true);
            employeeButton.setText("Ingresar");
            employeePassword.setText("");
            if (rememberId.isSelected()) {
                prefs.put(PREF_EMPLOYEE_ID, id);
            } else {
                prefs.remove(PREF_EMPLOYEE_ID);
            }
            employee = session;
            if (AuthService.ROLE_OPERATOR.equals(session.getRole())) {
                openBoothStep(session);
            } else {
                openGreeting(session);
            }
        }, error -> {
            employeeButton.setEnabled(true);
            employeeButton.setText("Ingresar");
            employeePassword.setText("");
            employeeError.setText(error.getMessage());
            employeePassword.requestFocusInWindow();
        });
    }

    // ------------------------------------------------------------------ Operador: caseta del turno

    private JComponent buildBooth() {
        JPanel card = formCard();
        card.add(left(back(() -> show(EMPLOYEE))));
        card.add(Box.createVerticalStrut(12));
        card.add(left(boothHello));
        card.add(left(boothRole));
        boothCombo.setFont(Theme.font(Font.PLAIN, 15));
        boothCombo.setBackground(Color.WHITE);
        boothCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof TollBooth) {
                    setText(boothText((TollBooth) value));
                }
                setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return this;
            }
        });
        addField(card, "Caseta del turno", boothCombo);
        card.add(left(boothError));
        card.add(Box.createVerticalStrut(10));
        card.add(left(boothButton));
        card.add(Box.createVerticalStrut(14));
        card.add(left(sub("Solo aparecen las casetas activas de tu peaje.")));
        boothButton.addActionListener(e -> startShift());
        return card;
    }

    private void openBoothStep(CashierSession session) {
        boothHello.setText("Hola, " + session.getOperatorName());
        boothRole.setText("Operador de caseta · Peaje " + session.getTollName());
        boothError.setText(" ");
        boothCombo.removeAllItems();
        boothButton.setEnabled(false);
        show(BOOTH);
        Async.run(() -> {
            CashierSession checked = cashier.login(session.getOperatorId());
            return cashier.availableBooths(checked);
        }, booths -> {
            booths.forEach(boothCombo::addItem);
            boothButton.setEnabled(!booths.isEmpty());
            if (booths.isEmpty()) {
                boothError.setText("El peaje no tiene casetas activas");
            }
        }, error -> boothError.setText(error.getMessage()));
    }

    private void startShift() {
        try {
            CashierSession shift = cashier.openShift(employee, (TollBooth) boothCombo.getSelectedItem());
            dispose();
            new CashierFrame(cashier, shift).setVisible(true);
        } catch (RuntimeException e) {
            boothError.setText(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ Jefe y administrador: saludo

    private JComponent buildGreeting() {
        JPanel card = formCard();
        card.add(centered(new CheckBadge()));
        card.add(Box.createVerticalStrut(16));
        greetHello.setHorizontalAlignment(SwingConstants.CENTER);
        greetRole.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(stretch(greetHello));
        card.add(stretch(greetRole));
        card.add(Box.createVerticalStrut(20));
        greetBody.setOpaque(false);
        card.add(left(greetBody));
        card.add(Box.createVerticalStrut(18));
        card.add(left(greetButton));
        card.add(Box.createVerticalStrut(10));
        greetNote.setHorizontalAlignment(SwingConstants.CENTER);
        greetNote.setFont(Theme.font(Font.PLAIN, 12));
        card.add(stretch(greetNote));
        card.add(Box.createVerticalStrut(6));
        JPanel out = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        out.setOpaque(false);
        out.add(link("No soy yo, volver", () -> {
            employee = null;
            show(EMPLOYEE);
        }));
        card.add(left(out));
        greetButton.addActionListener(e -> {
            if (employee != null && AuthService.ROLE_MANAGER.equals(employee.getRole())) {
                dispose();
                new StationFrame(station, employee).setVisible(true);
            }
        });
        return card;
    }

    private void openGreeting(CashierSession session) {
        greetHello.setText("Hola, " + session.getOperatorName());
        greetBody.removeAll();
        boolean admin = AuthService.ROLE_ADMIN.equals(session.getRole());
        if (admin) {
            greetRole.setText("Administrador · Todos los peajes");
            greetBody.add(hint("Desde Administración se registran, modifican y eliminan <b>peajes</b>, "
                    + "<b>tarifas</b>, <b>casetas</b> y <b>empleados</b>."), BorderLayout.CENTER);
            greetButton.setText("Ir a Administración");
            greetButton.setEnabled(false);
            greetNote.setText("El panel de Administración está en construcción.");
        } else {
            greetRole.setText("Jefe de estación · Peaje " + session.getTollName());
            JLabel unattended = value("…", Theme.DANGER);
            JLabel resources = value("…", Theme.WARNING);
            JLabel revenue = value("…", Theme.SUCCESS);
            JPanel box = new RoundedCard(Theme.BORDER, 1);
            box.setLayout(new GridLayout(3, 2, 0, 8));
            box.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            box.add(plain("Solicitudes sin atender"));
            box.add(unattended);
            box.add(plain("Recursos disponibles"));
            box.add(resources);
            box.add(plain("Recaudado hoy"));
            box.add(revenue);
            greetBody.add(box, BorderLayout.CENTER);
            greetButton.setText("Ir al Centro de asistencia");
            greetButton.setEnabled(true);
            greetNote.setText("Los jefes no escogen caseta: entran directo a su panel.");
            Async.run(() -> station.summary(session.getTollId()), s -> {
                unattended.setText(String.valueOf(s.getUnattended()));
                resources.setText(s.getAvailableResources() + " de " + s.getTotalResources());
                revenue.setText(Theme.money(s.getRevenueToday()));
            }, error -> {
                unattended.setText("—");
                resources.setText("—");
                revenue.setText("—");
            });
        }
        greetBody.revalidate();
        show(GREETING);
    }

    // ------------------------------------------------------------------ Viajero: iniciar sesión y registro

    private JComponent buildTraveler() {
        JPanel card = formCard();
        card.add(left(back(() -> show(WELCOME))));
        card.add(Box.createVerticalStrut(12));
        card.add(left(h2("Viajero")));
        card.add(Box.createVerticalStrut(12));

        JPanel tabs = new JPanel(new GridLayout(1, 2));
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.BORDER));
        tabs.add(loginTab);
        tabs.add(registerTab);
        loginTab.addMouseListener(click(() -> selectTravelerTab(LOGIN_TAB)));
        registerTab.addMouseListener(click(() -> selectTravelerTab(REGISTER_TAB)));
        card.add(left(tabs));

        JPanel login = column();
        addField(login, "Correo", travelerEmail);
        addField(login, "Contraseña", withToggle(travelerPassword));
        JPanel forgot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        forgot.setOpaque(false);
        forgot.add(link("¿Olvidaste tu contraseña?", () -> JOptionPane.showMessageDialog(this,
                "La recuperación de contraseña por correo estará disponible próximamente.",
                "Recuperar contraseña", JOptionPane.INFORMATION_MESSAGE)));
        login.add(left(forgot));
        login.add(left(travelerError));
        login.add(Box.createVerticalStrut(6));
        login.add(left(travelerButton));
        login.add(Box.createVerticalStrut(16));
        login.add(left(separator()));
        login.add(Box.createVerticalStrut(16));
        FlatButton guest = wide(Theme.secondaryButton("Entrar como invitado"));
        guest.addActionListener(e -> show(GUEST));
        login.add(left(guest));
        login.add(Box.createVerticalStrut(10));
        JLabel note = sub("<html><div style='text-align:center'>Como invitado puedes ver peajes y tarifas y pedir "
                + "asistencia.<br>Con cuenta guardas tus placas y ves todas tus solicitudes.</div></html>");
        note.setFont(Theme.font(Font.PLAIN, 12));
        note.setHorizontalAlignment(SwingConstants.CENTER);
        login.add(stretch(note));
        travelerButton.addActionListener(e -> travelerLogin());

        JPanel register = column();
        addField(register, "Nombre", "Apellido", regFirst, regLast);
        addField(register, "Correo", regEmail);
        addField(register, "Teléfono", regPhone);
        addField(register, "Contraseña", "Confirmar", regPassword, regConfirm);
        addField(register, "Placa de tu vehículo (opcional)", regPlate);
        register.add(left(regError));
        register.add(Box.createVerticalStrut(6));
        register.add(left(regButton));
        regButton.addActionListener(e -> registerTraveler());

        travelerTabPanel.setOpaque(false);
        travelerTabPanel.add(login, LOGIN_TAB);
        travelerTabPanel.add(register, REGISTER_TAB);
        card.add(left(travelerTabPanel));
        selectTravelerTab(LOGIN_TAB);
        return card;
    }

    private void selectTravelerTab(String tabName) {
        boolean loginSelected = LOGIN_TAB.equals(tabName);
        travelerTabs.show(travelerTabPanel, tabName);
        styleTab(loginTab, loginSelected);
        styleTab(registerTab, !loginSelected);
        travelerTabPanel.revalidate();
        getRootPane().setDefaultButton(loginSelected ? travelerButton : regButton);
    }

    private void travelerLogin() {
        String email = travelerEmail.getText();
        char[] password = travelerPassword.getPassword();
        travelerError.setText(" ");
        travelerButton.setEnabled(false);
        Async.run(() -> auth.travelerLogin(email, password), traveler -> {
            travelerButton.setEnabled(true);
            travelerPassword.setText("");
            enterAsTraveler(traveler);
        }, error -> {
            travelerButton.setEnabled(true);
            travelerPassword.setText("");
            travelerError.setText(error.getMessage());
        });
    }

    private void registerTraveler() {
        regError.setText(" ");
        regButton.setEnabled(false);
        String first = regFirst.getText();
        String last = regLast.getText();
        String email = regEmail.getText();
        String phone = regPhone.getText();
        String plate = regPlate.getText();
        char[] password = regPassword.getPassword();
        char[] confirm = regConfirm.getPassword();
        Async.run(() -> auth.registerTraveler(first, last, email, phone, plate, password, confirm), traveler -> {
            regButton.setEnabled(true);
            regPassword.setText("");
            regConfirm.setText("");
            JOptionPane.showMessageDialog(this, "¡Cuenta creada, " + traveler.getFirstName() + "!",
                    "Registro", JOptionPane.INFORMATION_MESSAGE);
            enterAsTraveler(traveler);
        }, error -> {
            regButton.setEnabled(true);
            regError.setText(error.getMessage());
        });
    }

    /**
     * Entra a la pantalla del viajero (con cuenta o como invitado).
     * La pantalla principal del viajero todavía no existe; por ahora se avisa.
     * @param traveler Viajero, o null si entra como invitado.
     */
    private void enterAsTraveler(Traveler traveler) {
        String who = traveler == null ? "invitado" : traveler.getFirstName();
        JOptionPane.showMessageDialog(this, "Hola, " + who + ".\nLa pantalla del viajero (peajes, tarifas, "
                + "mapa y asistencia) está en construcción.", "RutaPeaje", JOptionPane.INFORMATION_MESSAGE);
        show(WELCOME);
    }

    // ------------------------------------------------------------------ Invitado y seguimiento

    private JComponent buildGuest() {
        JPanel card = formCard();
        card.add(left(back(() -> show(WELCOME))));
        card.add(Box.createVerticalStrut(12));
        card.add(left(h2("Entrar como invitado")));
        card.add(left(sub("Puedes usar RutaPeaje sin crear cuenta.")));
        card.add(Box.createVerticalStrut(18));
        JPanel compare = new JPanel(new GridLayout(1, 2, 12, 0));
        compare.setOpaque(false);
        compare.add(list("Sin cuenta", Theme.TEXT, Color.WHITE, Theme.BORDER, new String[]{
            "Peajes, tarifas y mapa", "Costo de una ruta", "Pedir asistencia", "Ver tu solicitud con el número"}));
        compare.add(list("Con cuenta, además", Theme.PRIMARY_DARK, new Color(0xF5F9FF), new Color(0xBFDBFE),
                new String[]{"Tus placas guardadas", "Todas tus solicitudes", "Rutas favoritas"}));
        card.add(left(compare));
        card.add(Box.createVerticalStrut(20));
        FlatButton go = wide(Theme.primaryButton("Continuar como invitado"));
        go.addActionListener(e -> enterAsTraveler(null));
        card.add(left(go));
        card.add(Box.createVerticalStrut(14));
        JPanel links = new JPanel(new BorderLayout());
        links.setOpaque(false);
        links.add(link("Ya pedí asistencia: consultar mi solicitud", () -> {
            trackResult.setVisible(false);
            trackError.setText(" ");
            show(TRACK);
        }), BorderLayout.WEST);
        links.add(link("Crear cuenta", () -> {
            selectTravelerTab(REGISTER_TAB);
            show(TRAVELER);
        }), BorderLayout.EAST);
        card.add(left(links));
        return card;
    }

    private JComponent buildTrack() {
        JPanel card = formCard();
        card.add(left(back(() -> show(GUEST))));
        card.add(Box.createVerticalStrut(12));
        card.add(left(h2("Consultar mi solicitud")));
        card.add(left(sub("Para quien pidió asistencia sin registrarse.")));
        addField(card, "Número de solicitud", trackNumber);
        addField(card, "Teléfono con el que la pediste", trackPhone);
        card.add(left(trackError));
        card.add(Box.createVerticalStrut(6));
        card.add(left(trackButton));
        card.add(Box.createVerticalStrut(16));
        trackResult.setOpaque(false);
        trackResult.setVisible(false);
        card.add(left(trackResult));
        trackButton.addActionListener(e -> trackRequest());
        return card;
    }

    private void trackRequest() {
        trackError.setText(" ");
        trackResult.setVisible(false);
        String number = trackNumber.getText().trim();
        String phone = trackPhone.getText().replaceAll("\\D", "");
        if (!number.matches("\\d{1,9}")) {
            trackError.setText("Escriba el número de la solicitud");
            return;
        }
        Async.run(() -> {
            Assistance a = station.assistances().get(Integer.parseInt(number));
            String saved = a.getContactPhone() == null ? "" : a.getContactPhone().replaceAll("\\D", "");
            if (!saved.equals(phone)) {
                throw new IllegalArgumentException("El número y el teléfono no coinciden");
            }
            Toll toll = station.toll(a.getTollId());
            AssistanceResource resource = a.getResourceId() == null ? null
                    : station.resources().get(a.getResourceId());
            return new Object[]{a, toll, resource};
        }, data -> showTrackResult((Assistance) data[0], (Toll) data[1], (AssistanceResource) data[2]),
                error -> trackError.setText(error instanceof IllegalArgumentException
                        ? error.getMessage() : "No se encontró la solicitud " + number));
    }

    private void showTrackResult(Assistance a, Toll toll, AssistanceResource resource) {
        trackResult.removeAll();
        JPanel box = new RoundedCard(Theme.BORDER, 1);
        box.setLayout(new BorderLayout(0, 6));
        box.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("#" + a.getAssistanceId() + " · " + StationTexts.type(a.getType())
                + (a.getPlate() == null ? "" : " · " + a.getPlate()));
        title.setFont(Theme.font(Font.BOLD, 14));
        title.setForeground(Theme.TEXT);
        top.add(title, BorderLayout.WEST);
        top.add(StationTexts.statusPill(a.getCurrentStatus()), BorderLayout.EAST);
        box.add(top, BorderLayout.NORTH);
        String where = toll == null ? "Peaje " + a.getTollId() : "Peaje " + toll.getName();
        String who = resource == null ? "Aún sin recurso asignado" : resource.getIdentification() + " asignado";
        box.add(sub(where + " · " + who), BorderLayout.CENTER);
        trackResult.add(box, BorderLayout.CENTER);
        trackResult.setVisible(true);
        trackResult.revalidate();
        trackResult.repaint();
    }

    // ------------------------------------------------------------------ Ayudas de diseño

    private static String boothText(TollBooth booth) {
        return "Caseta " + booth.getBoothNumber() + " · " + booth.getPaymentType()
                + (booth.getDirection() == null ? "" : " · " + direction(booth.getDirection()));
    }

    private static String direction(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(" → ");
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static JPanel center(JComponent component) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.BACKGROUND);
        wrapper.add(component);
        return wrapper;
    }

    private static JPanel formCard() {
        JPanel card = new RoundedCard(Theme.BORDER, 1);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));
        ((RoundedCard) card).fixedWidth = CARD_WIDTH + 72;
        return card;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent stretch(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private static void addField(JPanel panel, String label, JComponent input) {
        panel.add(Box.createVerticalStrut(14));
        panel.add(left(fieldLabel(label)));
        panel.add(Box.createVerticalStrut(6));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        input.setPreferredSize(new Dimension(CARD_WIDTH, 42));
        panel.add(left(input));
    }

    private static void addField(JPanel panel, String label1, String label2, JComponent in1, JComponent in2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        JPanel a = column();
        addField(a, label1, in1);
        JPanel b = column();
        addField(b, label2, in2);
        row.add(a);
        row.add(b);
        panel.add(left(row));
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 13));
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static JLabel h2(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 24));
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static JLabel sub(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static JLabel plain(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static JLabel value(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(Theme.font(Font.BOLD, 14));
        label.setForeground(color);
        return label;
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(Theme.font(Font.PLAIN, 13));
        label.setForeground(Theme.DANGER);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return label;
    }

    private static JLabel tab(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        return label;
    }

    private static void styleTab(JLabel tab, boolean selected) {
        tab.setFont(Theme.font(Font.BOLD, 14));
        tab.setForeground(selected ? Theme.PRIMARY : Theme.MUTED);
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, selected ? 3 : 0, 0, Theme.PRIMARY),
                BorderFactory.createEmptyBorder(10, 0, selected ? 7 : 10, 0)));
    }

    private static JLabel link(String text, Runnable action) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 14));
        label.setForeground(Theme.PRIMARY);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(click(action));
        return label;
    }

    private static JLabel back(Runnable action) {
        JLabel label = link("← Volver", action);
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static MouseAdapter click(Runnable action) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        };
    }

    private static JLabel hint(String html) {
        JLabel label = new JLabel("<html><div style='width:330px'>" + html + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, 13));
        label.setForeground(new Color(0x1E40AF));
        label.setOpaque(true);
        label.setBackground(new Color(0xEFF6FF));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        return label;
    }

    private static JPanel list(String title, Color titleColor, Color background, Color border, String[] items) {
        JPanel box = new RoundedCard(border, 1, background);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel name = new JLabel(title);
        name.setFont(Theme.font(Font.BOLD, 14));
        name.setForeground(titleColor);
        box.add(left(name));
        for (String item : items) {
            JLabel line = new JLabel("<html><span style='color:#16A34A'>✓</span>&nbsp; " + item + "</html>");
            line.setFont(Theme.font(Font.PLAIN, 13));
            line.setForeground(Theme.TEXT);
            line.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            box.add(left(line));
        }
        return box;
    }

    private static JComponent separator() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(line(), BorderLayout.WEST);
        JLabel o = new JLabel("o", SwingConstants.CENTER);
        o.setForeground(new Color(0x9CA3AF));
        row.add(o, BorderLayout.CENTER);
        row.add(line(), BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        return row;
    }

    private static JComponent line() {
        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        line.setOpaque(false);
        line.setPreferredSize(new Dimension(CARD_WIDTH / 2 - 20, 18));
        return line;
    }

    private static FlatButton wide(FlatButton button) {
        button.setFont(Theme.font(Font.BOLD, 15));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(CARD_WIDTH, 44));
        return button;
    }

    private static FlatButton green(FlatButton button) {
        button.setColors(Theme.SUCCESS, new Color(0x15803D), Color.WHITE, null);
        return button;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.font(Font.PLAIN, 15));
        field.setForeground(Theme.TEXT);
        field.setBorder(Theme.inputBorder());
        return field;
    }

    private static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setOpaque(false);
        box.setFont(Theme.font(Font.PLAIN, 14));
        box.setForeground(Theme.TEXT);
        box.setFocusPainted(false);
        return box;
    }

    /** Campo de contraseña con el botón "Mostrar" / "Ocultar" dentro del recuadro. */
    private static JComponent withToggle(JPasswordField field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(Theme.inputBorder());
        field.setBorder(BorderFactory.createEmptyBorder());
        char echo = field.getEchoChar();
        JLabel toggle = new JLabel("Mostrar");
        toggle.setFont(Theme.font(Font.BOLD, 12));
        toggle.setForeground(Theme.MUTED);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addMouseListener(click(() -> {
            boolean hidden = field.getEchoChar() != 0;
            field.setEchoChar(hidden ? (char) 0 : echo);
            toggle.setText(hidden ? "Ocultar" : "Mostrar");
        }));
        panel.add(field, BorderLayout.CENTER);
        panel.add(toggle, BorderLayout.EAST);
        return panel;
    }

    /** Panel blanco con esquinas redondeadas y borde. */
    private static class RoundedCard extends JPanel {
        private final Color border;
        private final int thickness;
        private final Color fill;
        private int fixedWidth;

        RoundedCard(Color border, int thickness) {
            this(border, thickness, Color.WHITE);
        }

        RoundedCard(Color border, int thickness, Color fill) {
            this.border = border;
            this.thickness = thickness;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return fixedWidth > 0 ? new Dimension(fixedWidth, d.height) : d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
            g2.setColor(border);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Double(thickness / 2.0, thickness / 2.0,
                    getWidth() - 1 - thickness, getHeight() - 1 - thickness, 18, 18));
            g2.dispose();
        }
    }

    /** Íconos dibujados para las tarjetas de la bienvenida. */
    private enum Icon { CAR, BADGE }

    /** Cuadro de color con un ícono blanco dibujado. */
    private static class IconBox extends JComponent {
        private final Icon icon;
        private final Color color;

        IconBox(Icon icon, Color color) {
            this.icon = icon;
            this.color = color;
            Dimension size = new Dimension(58, 58);
            setPreferredSize(size);
            setMaximumSize(size);
            setMinimumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fill(new RoundRectangle2D.Double(0, 0, 58, 58, 16, 16));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (icon == Icon.CAR) {
                Path2D body = new Path2D.Double();
                body.moveTo(14, 34);
                body.lineTo(18, 24);
                body.quadTo(19.5, 21, 23, 21);
                body.lineTo(35, 21);
                body.quadTo(38.5, 21, 40, 24);
                body.lineTo(44, 34);
                body.lineTo(44, 40);
                body.lineTo(14, 40);
                body.closePath();
                g2.draw(body);
                g2.drawLine(14, 34, 44, 34);
                g2.fillOval(18, 38, 7, 7);
                g2.fillOval(33, 38, 7, 7);
            } else {
                g2.draw(new RoundRectangle2D.Double(13, 17, 32, 24, 5, 5));
                g2.drawOval(18, 22, 7, 7);
                g2.drawArc(16, 30, 11, 9, 0, 180);
                g2.drawLine(30, 25, 40, 25);
                g2.drawLine(30, 31, 37, 31);
            }
            g2.dispose();
        }
    }

    /** Círculo verde con un chulo, para el saludo del jefe y el administrador. */
    private static class CheckBadge extends JComponent {
        CheckBadge() {
            Dimension size = new Dimension(64, 64);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xDCFCE7));
            g2.fillOval(0, 0, 64, 64);
            g2.setColor(Theme.SUCCESS);
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{20, 29, 45}, new int[]{33, 42, 24}, 3);
            g2.dispose();
        }
    }

    private static JComponent centered(CheckBadge badge) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.add(badge);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        return row;
    }
}
