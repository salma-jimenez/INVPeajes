package com.invpeajes.ui.login;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.TollBooth;
import com.invpeajes.model.Traveler;
import com.invpeajes.service.AuthService;
import com.invpeajes.service.CashierService;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.cashier.CashierFrame;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.station.StationFrame;
import com.invpeajes.ui.traveler.TravelerFrame;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

/**
 * Ventana de inicio de RutaPeaje. A la izquierda va la columna blanca con el formulario de cada
 * paso y a la derecha el panel ilustrado del peaje ({@link BrandPanel}).
 *
 * <ul>
 *   <li><b>Bienvenida</b>: "Soy viajero" o "Soy empleado", y el enlace para entrar como invitado.</li>
 *   <li><b>Empleados</b>: cédula y contraseña. Según el cargo, el operador de caseta escoge su caseta
 *       y abre la Caja; el jefe de estación ve el resumen de su peaje y entra a su panel; el
 *       administrador ve su saludo (su panel está en construcción).</li>
 *   <li><b>Viajeros</b>: iniciar sesión y crear cuenta en pestañas.</li>
 *   <li><b>Invitado</b>: entrar sin cuenta para ver el mapa de peajes y planificar viajes. Para
 *       pedir asistencia hace falta una cuenta (se crea desde la misma pantalla del viajero).</li>
 * </ul>
 *
 * @author Salomejimenez
 * @version 2.0
 */
public class LoginFrame extends JFrame {

    private static final String WELCOME = "welcome";
    private static final String EMPLOYEE = "employee";
    private static final String BOOTH = "booth";
    private static final String GREETING = "greeting";
    private static final String TRAVELER = "traveler";
    private static final String GUEST = "guest";
    private static final String LOGIN_TAB = "login";
    private static final String REGISTER_TAB = "register";
    private static final String PREF_EMPLOYEE_ID = "employeeId";

    /** Ancho del formulario dentro de la columna izquierda. */
    private static final int FORM_WIDTH = 384;
    private static final int COLUMN_WIDTH = 560;

    private static final Color NAVY = new Color(0x0F2A5F);
    private static final Color INK = new Color(0x18181B);
    private static final Color BODY = new Color(0x52525B);
    private static final Color LABEL = new Color(0x71717A);
    private static final Color LINE = new Color(0xD4D4D8);
    private static final Color SOFT_LINE = new Color(0xE4E4E7);
    private static final Color ARROW = new Color(0xA1A1AA);

    private final AuthService auth;
    private final CashierService cashier;
    private final StationManagerService station;
    private final Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel topBack = muted("← Volver");
    private CashierSession employee;

    private final JTextField employeeId = textField();
    private final JPasswordField employeePassword = passwordField();
    private final Field employeeIdField = new Field("Cédula", employeeId);
    private final Field employeePasswordField = new Field("Contraseña", employeePassword, toggle(employeePassword));
    private final JCheckBox rememberId = checkBox("Recordar mi cédula");
    private final JLabel employeeError = errorLabel();
    private final PillButton employeeButton = new PillButton("Ingresar", PillButton.PRIMARY);

    private final JLabel boothHello = h1("", 30);
    private final WrapText boothRole = lead("");
    private final JComboBox<TollBooth> boothCombo = new JComboBox<>();
    private final JLabel boothError = errorLabel();
    private final PillButton boothButton = new PillButton("Iniciar turno", PillButton.SUCCESS);

    private final JLabel greetHello = h1("", 30);
    private final WrapText greetRole = lead("");
    private final JPanel greetBody = new JPanel(new BorderLayout());
    private final PillButton greetButton = new PillButton("Ir al Centro de asistencia", PillButton.PRIMARY);
    private final WrapText greetNote = note("");

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
    private final JLabel registerTab = tab("Crear cuenta");
    private final WrapText travelerNote = note("Como invitado puedes ver el mapa de peajes y planificar viajes.\n"
            + "Con cuenta puedes pedir asistencia en la vía y seguirla paso a paso.");
    private final JTextField travelerEmail = textField();
    private final JPasswordField travelerPassword = passwordField();
    private final Field travelerEmailField = new Field("Correo", travelerEmail);
    private final Field travelerPasswordField = new Field("Contraseña", travelerPassword, toggle(travelerPassword));
    private final JLabel travelerError = errorLabel();
    private final PillButton travelerButton = new PillButton("Iniciar sesión", PillButton.PRIMARY);

    private final JTextField regFirst = textField();
    private final JTextField regLast = textField();
    private final JTextField regEmail = textField();
    private final JTextField regPhone = textField();
    private final JTextField regPlate = textField();
    private final JPasswordField regPassword = passwordField();
    private final JPasswordField regConfirm = passwordField();
    private final Field[] regFields = {
        new Field("Correo", regEmail),
        new Field("Teléfono", regPhone),
        new Field("Placa (opcional)", regPlate).key("placa"),
        new Field("Confirmar", regConfirm).key("coinciden"),
        new Field("Contraseña", regPassword),
        new Field("Nombre", regFirst),
        new Field("Apellido", regLast),
    };
    private final JCheckBox regTerms = checkBox("Acepto los ");
    private final JLabel regError = errorLabel();
    private final PillButton regButton = new PillButton("Crear cuenta", PillButton.PRIMARY);

    private final PillButton guestButton = new PillButton("Continuar como invitado", PillButton.PRIMARY);

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

        content.setOpaque(false);
        content.add(buildWelcome(), WELCOME);
        content.add(buildEmployee(), EMPLOYEE);
        content.add(buildBooth(), BOOTH);
        content.add(buildGreeting(), GREETING);
        content.add(buildTraveler(), TRAVELER);
        content.add(buildGuest(), GUEST);

        JPanel column = new JPanel(new BorderLayout(0, 0));
        column.setBackground(Color.WHITE);
        column.setBorder(BorderFactory.createEmptyBorder(56, 88, 36, 88));
        column.setPreferredSize(new Dimension(COLUMN_WIDTH, 768));
        column.add(buildLogoRow(), BorderLayout.NORTH);
        column.add(content, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        getContentPane().add(column, BorderLayout.WEST);
        getContentPane().add(new BrandPanel(), BorderLayout.CENTER);
        show(WELCOME);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1366, 768);
        setLocationRelativeTo(null);
    }

    /** Abre la ventana de inicio con los servicios de la base de datos. */
    public static void open() {
        new LoginFrame(new AuthService(), new CashierService(), new StationManagerService()).setVisible(true);
    }

    /** Abre la ventana de inicio directamente en "Iniciar sesión" del viajero. */
    public static void openTravelerLogin() {
        LoginFrame frame = new LoginFrame(new AuthService(), new CashierService(), new StationManagerService());
        frame.selectTravelerTab(LOGIN_TAB);
        frame.show(TRAVELER);
        frame.setVisible(true);
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
        String target = card;
        cards.show(content, target);
        topBack.setVisible(TRAVELER.equals(target));
        switch (target) {
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
                getRootPane().setDefaultButton(LOGIN_TAB.equals(currentTab()) ? travelerButton : regButton);
                break;
            case GUEST:
                getRootPane().setDefaultButton(guestButton);
                break;
            default:
                getRootPane().setDefaultButton(null);
                break;
        }
    }

    private JComponent buildLogoRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logo.setOpaque(false);
        logo.add(new LogoMark());
        logo.add(Box.createHorizontalStrut(10));
        JLabel name = new JLabel("RutaPeaje");
        name.setFont(Theme.font(Font.BOLD, 18));
        name.setForeground(NAVY);
        logo.add(name);
        logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logo.addMouseListener(click(() -> show(WELCOME)));
        row.add(logo, BorderLayout.WEST);
        topBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topBack.addMouseListener(click(() -> show(WELCOME)));
        row.add(topBack, BorderLayout.EAST);
        return row;
    }

    // ------------------------------------------------------------------ Bienvenida

    private JComponent buildWelcome() {
        JPanel top = column();
        top.add(Box.createVerticalStrut(56));
        top.add(left(h1("<html>Todos los peajes<br>de Colombia, en un<br>solo lugar.</html>", 34)));
        top.add(Box.createVerticalStrut(14));
        top.add(left(lead("Consulta tarifas y rutas, pide asistencia en la vía o ingresa a la "
                + "operación de tu peaje.")));
        top.add(Box.createVerticalStrut(28));
        top.add(left(new OptionRow(OptionRow.CAR, "Soy viajero", "Peajes, tarifas, rutas y asistencia en la vía",
                () -> {
                    selectTravelerTab(LOGIN_TAB);
                    show(TRAVELER);
                })));
        top.add(Box.createVerticalStrut(10));
        top.add(left(new OptionRow(OptionRow.BADGE, "Soy empleado", "Caja de cobro y gestión del peaje",
                () -> show(EMPLOYEE))));

        JPanel bottom = flow();
        bottom.add(small("¿Solo quieres consultar? "));
        bottom.add(link("Entrar como invitado", () -> show(GUEST)));
        return page(top, bottom);
    }

    // ------------------------------------------------------------------ Empleados

    private JComponent buildEmployee() {
        JPanel top = column();
        top.add(Box.createVerticalStrut(56));
        top.add(left(h1("Ingreso de empleados", 34)));
        top.add(Box.createVerticalStrut(14));
        top.add(left(lead("Te llevamos a la caja o al panel del peaje según tu cargo.")));
        top.add(Box.createVerticalStrut(8));
        addField(top, employeeIdField, 22);
        addField(top, employeePasswordField, 22);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(rememberId, BorderLayout.WEST);
        row.add(link("¿Olvidaste tu contraseña?", () -> JOptionPane.showMessageDialog(this,
                "Pídele al jefe de estación de tu peaje o al administrador que restablezca tu contraseña.",
                "Recuperar contraseña", JOptionPane.INFORMATION_MESSAGE)), BorderLayout.EAST);
        top.add(Box.createVerticalStrut(14));
        top.add(stretch(row));
        top.add(left(employeeError));
        top.add(Box.createVerticalStrut(18));
        top.add(left(actions(employeeButton, "← Volver", () -> show(WELCOME))));

        String saved = prefs.get(PREF_EMPLOYEE_ID, "");
        employeeId.setText(saved);
        rememberId.setSelected(!saved.isEmpty());
        employeeButton.addActionListener(e -> employeeLogin());
        return page(top, note("Operador de caseta → escoge su caseta y abre la Caja.\n"
                + "Jefe de estación y administrador → entran a su panel."));
    }

    private void employeeLogin() {
        String id = employeeId.getText().trim();
        char[] password = employeePassword.getPassword();
        employeeError.setText(" ");
        clear(employeeIdField, employeePasswordField);
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
            if (!flag(error.getMessage(), employeePasswordField, employeeIdField)) {
                employeePasswordField.setError(error.getMessage());
            }
            employeePassword.requestFocusInWindow();
        });
    }

    // ------------------------------------------------------------------ Operador: caseta del turno

    private JComponent buildBooth() {
        JPanel top = column();
        top.add(Box.createVerticalStrut(56));
        top.add(left(new Badge()));
        top.add(Box.createVerticalStrut(18));
        top.add(left(boothHello));
        top.add(Box.createVerticalStrut(8));
        top.add(left(boothRole));
        top.add(Box.createVerticalStrut(18));
        top.add(left(lead("Escoge la caseta donde vas a trabajar hoy para abrir tu turno.")));

        boothCombo.setFont(Theme.font(Font.PLAIN, 16));
        boothCombo.setForeground(INK);
        boothCombo.setBackground(Color.WHITE);
        boothCombo.setBorder(BorderFactory.createEmptyBorder());
        boothCombo.setOpaque(false);
        boothCombo.setUI(new FlatComboUI());
        boothCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof TollBooth) {
                    setText(boothText((TollBooth) value));
                }
                setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 8));
                return this;
            }
        });
        addField(top, new Field("Caseta del turno", boothCombo), 22);
        top.add(left(boothError));
        top.add(Box.createVerticalStrut(18));
        top.add(left(actions(boothButton, "← No soy yo", () -> {
            employee = null;
            show(EMPLOYEE);
        })));
        boothButton.addActionListener(e -> startShift());
        return page(top, note("Solo aparecen las casetas activas de tu peaje."));
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
        JPanel top = column();
        top.add(Box.createVerticalStrut(56));
        top.add(left(new Badge()));
        top.add(Box.createVerticalStrut(18));
        top.add(left(greetHello));
        top.add(Box.createVerticalStrut(8));
        top.add(left(greetRole));
        top.add(Box.createVerticalStrut(26));
        greetBody.setOpaque(false);
        top.add(stretch(greetBody));
        top.add(Box.createVerticalStrut(28));
        top.add(left(actions(greetButton, "← No soy yo", () -> {
            employee = null;
            show(EMPLOYEE);
        })));
        greetButton.addActionListener(e -> {
            if (employee != null && AuthService.ROLE_MANAGER.equals(employee.getRole())) {
                dispose();
                new StationFrame(station, employee).setVisible(true);
            }
        });
        return page(top, greetNote);
    }

    private void openGreeting(CashierSession session) {
        greetHello.setText("Hola, " + session.getOperatorName());
        greetBody.removeAll();
        boolean admin = AuthService.ROLE_ADMIN.equals(session.getRole());
        if (admin) {
            greetRole.setText("Administrador · Todos los peajes");
            JPanel list = column();
            list.add(left(overline("DESDE ADMINISTRACIÓN GESTIONAS")));
            list.add(Box.createVerticalStrut(6));
            for (String item : new String[]{"Peajes", "Tarifas por categoría", "Casetas", "Empleados"}) {
                list.add(stretch(summaryRow(item, valueLabel("Registrar · modificar · eliminar", LABEL, false))));
            }
            greetBody.add(list, BorderLayout.CENTER);
            greetButton.setText("Ir a Administración");
            greetButton.setEnabled(false);
            greetNote.setText("El panel de Administración está en construcción.");
        } else {
            greetRole.setText("Jefe de estación · Peaje " + session.getTollName());
            JLabel unattended = valueLabel("…", Theme.DANGER, true);
            JLabel resources = valueLabel("…", Theme.WARNING, true);
            JLabel revenue = valueLabel("…", Theme.SUCCESS, true);
            JPanel list = column();
            list.add(left(overline("HOY EN TU PEAJE")));
            list.add(Box.createVerticalStrut(6));
            list.add(stretch(summaryRow("Solicitudes sin atender", unattended)));
            list.add(stretch(summaryRow("Recursos disponibles", resources)));
            list.add(stretch(summaryRow("Recaudado hoy", revenue)));
            greetBody.add(list, BorderLayout.CENTER);
            greetButton.setText("Ir al Centro de asistencia");
            greetButton.setEnabled(true);
            greetNote.setText("Los jefes de estación no escogen caseta: entran directo a su panel.");
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
        greetBody.repaint();
        show(GREETING);
    }

    // ------------------------------------------------------------------ Viajero: iniciar sesión y crear cuenta

    private JComponent buildTraveler() {
        JPanel top = column();
        top.add(Box.createVerticalStrut(34));
        JPanel tabs = new TabBar();
        tabs.add(loginTab);
        tabs.add(Box.createHorizontalStrut(28));
        tabs.add(registerTab);
        loginTab.addMouseListener(click(() -> selectTravelerTab(LOGIN_TAB)));
        registerTab.addMouseListener(click(() -> selectTravelerTab(REGISTER_TAB)));
        top.add(stretch(tabs));

        JPanel login = column();
        login.add(Box.createVerticalStrut(30));
        login.add(left(h1("Hola de nuevo, viajero.", 34)));
        addField(login, travelerEmailField, 22);
        addField(login, travelerPasswordField, 22);
        JPanel forgot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgot.setOpaque(false);
        forgot.add(link("¿Olvidaste tu contraseña?", () -> JOptionPane.showMessageDialog(this,
                "La recuperación de contraseña por correo estará disponible próximamente.",
                "Recuperar contraseña", JOptionPane.INFORMATION_MESSAGE)));
        login.add(Box.createVerticalStrut(12));
        login.add(stretch(forgot));
        login.add(left(travelerError));
        login.add(Box.createVerticalStrut(18));
        PillButton guest = new PillButton("Entrar como invitado", PillButton.OUTLINE);
        guest.addActionListener(e -> show(GUEST));
        JPanel buttons = flow();
        buttons.add(travelerButton);
        buttons.add(Box.createHorizontalStrut(14));
        buttons.add(guest);
        login.add(left(buttons));
        travelerButton.addActionListener(e -> travelerLogin());

        JPanel register = column();
        register.add(Box.createVerticalStrut(26));
        register.add(left(h1("Viaja con todo a la mano.", 30)));
        Field first = regField("Nombre");
        Field last = regField("Apellido");
        addFields(register, first, last, 16);
        addField(register, regField("Correo"), 16);
        addFields(register, regField("Teléfono"), regField("Placa (opcional)"), 16);
        addFields(register, regField("Contraseña"), regField("Confirmar"), 16);
        JPanel terms = flow();
        terms.add(regTerms);
        terms.add(link("términos y la política de datos", () -> JOptionPane.showMessageDialog(this,
                "RutaPeaje usa tu nombre, correo, teléfono y placa solo para mostrarte tus solicitudes de\n"
                        + "asistencia y comunicarse contigo durante una atención. No se comparten con terceros.",
                "Términos y política de datos", JOptionPane.INFORMATION_MESSAGE)));
        regTerms.setIconTextGap(6);
        register.add(Box.createVerticalStrut(16));
        register.add(left(terms));
        register.add(left(regError));
        register.add(Box.createVerticalStrut(14));
        register.add(left(regButton));
        regButton.addActionListener(e -> registerTraveler());

        travelerTabPanel.setOpaque(false);
        travelerTabPanel.add(login, LOGIN_TAB);
        travelerTabPanel.add(register, REGISTER_TAB);
        top.add(stretch(travelerTabPanel));
        selectTravelerTab(LOGIN_TAB);
        return page(top, travelerNote);
    }

    private Field regField(String label) {
        for (Field f : regFields) {
            if (f.label.getText().equals(label)) {
                return f;
            }
        }
        throw new IllegalArgumentException(label);
    }

    private String currentTab() {
        return travelerTabPanel.getComponentCount() > 0 && travelerTabPanel.getComponent(0).isVisible()
                ? LOGIN_TAB : REGISTER_TAB;
    }

    private void selectTravelerTab(String tabName) {
        boolean loginSelected = LOGIN_TAB.equals(tabName);
        travelerTabs.show(travelerTabPanel, tabName);
        styleTab(loginTab, loginSelected);
        styleTab(registerTab, !loginSelected);
        travelerNote.setVisible(loginSelected);
        travelerTabPanel.revalidate();
        getRootPane().setDefaultButton(loginSelected ? travelerButton : regButton);
    }

    private void travelerLogin() {
        String email = travelerEmail.getText();
        char[] password = travelerPassword.getPassword();
        travelerError.setText(" ");
        clear(travelerEmailField, travelerPasswordField);
        travelerButton.setEnabled(false);
        Async.run(() -> auth.travelerLogin(email, password), traveler -> {
            travelerButton.setEnabled(true);
            travelerPassword.setText("");
            enterAsTraveler(traveler);
        }, error -> {
            travelerButton.setEnabled(true);
            travelerPassword.setText("");
            if (!flag(error.getMessage(), travelerPasswordField, travelerEmailField)) {
                travelerPasswordField.setError(error.getMessage());
            }
        });
    }

    private void registerTraveler() {
        regError.setText(" ");
        clear(regFields);
        if (!regTerms.isSelected()) {
            regError.setText("Debes aceptar los términos y la política de datos");
            return;
        }
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
                    "Crear cuenta", JOptionPane.INFORMATION_MESSAGE);
            enterAsTraveler(traveler);
        }, error -> {
            regButton.setEnabled(true);
            if (!flag(error.getMessage(), regFields)) {
                regError.setText(error.getMessage());
            }
        });
    }

    /**
     * Entra a la pantalla del viajero (con cuenta o como invitado).
     * @param traveler Viajero, o null si entra como invitado.
     */
    private void enterAsTraveler(Traveler traveler) {
        dispose();
        new TravelerFrame(traveler).setVisible(true);
    }

    // ------------------------------------------------------------------ Invitado

    private JComponent buildGuest() {
        JPanel top = column();
        top.add(Box.createVerticalStrut(56));
        top.add(left(h1("Entrar como invitado", 34)));
        top.add(Box.createVerticalStrut(14));
        top.add(left(lead("Sin cuenta puedes consultar el mapa de peajes y saber cuánto cuesta tu viaje.")));
        top.add(Box.createVerticalStrut(16));
        for (String item : new String[]{"Mapa de peajes con su ficha y tarifas", "Planificar un viaje y su costo"}) {
            JLabel line = new JLabel("✓   " + item);
            line.setFont(Theme.font(Font.PLAIN, 14));
            line.setForeground(new Color(0x3F3F46));
            line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            top.add(left(line));
        }
        JLabel locked = new JLabel("✕   Pedir asistencia en la vía (necesita una cuenta)");
        locked.setFont(Theme.font(Font.PLAIN, 14));
        locked.setForeground(LABEL);
        locked.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        top.add(left(locked));
        top.add(Box.createVerticalStrut(20));
        guestButton.addActionListener(e -> enterAsTraveler(null));
        top.add(left(actions(guestButton, "← Volver", () -> show(WELCOME))));
        top.add(Box.createVerticalStrut(24));
        top.add(stretch(divider()));
        top.add(Box.createVerticalStrut(22));
        JLabel already = new JLabel("¿Vas a necesitar asistencia en la vía?");
        already.setFont(Theme.font(Font.BOLD, 15));
        already.setForeground(INK);
        top.add(left(already));
        top.add(Box.createVerticalStrut(6));
        top.add(left(small("Con una cuenta la pides en segundos y ves cómo avanza.")));
        top.add(Box.createVerticalStrut(10));
        top.add(left(link("Crear una cuenta →", () -> {
            selectTravelerTab(REGISTER_TAB);
            show(TRAVELER);
        })));
        return page(top, null);
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

    /**
     * Arma un paso: el contenido arriba y, si hay, una nota pegada al fondo de la columna.
     */
    private static JPanel page(JComponent top, JComponent bottom) {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.add(top, BorderLayout.NORTH);
        if (bottom != null) {
            page.add(bottom, BorderLayout.SOUTH);
        }
        return page;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel flow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        return panel;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent stretch(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(FORM_WIDTH, Integer.MAX_VALUE));
        return c;
    }

    /** Botón principal con el enlace para devolverse a su lado. */
    private static JComponent actions(PillButton button, String backText, Runnable back) {
        JPanel row = flow();
        row.add(button);
        row.add(Box.createHorizontalStrut(20));
        JLabel label = muted(backText);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, PillButton.SHADOW, 0));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(click(back));
        row.add(label);
        return row;
    }

    private static void addField(JPanel panel, Field field, int gap) {
        panel.add(Box.createVerticalStrut(gap));
        panel.add(stretch(field));
    }

    private static void addFields(JPanel panel, Field a, Field b, int gap) {
        JPanel row = new JPanel(new GridLayout(1, 2, 24, 0));
        row.setOpaque(false);
        row.add(a);
        row.add(b);
        panel.add(Box.createVerticalStrut(gap));
        panel.add(stretch(row));
    }

    /**
     * Marca en rojo el campo del que habla el mensaje de error.
     * @return true si algún campo lo recibió.
     */
    private static boolean flag(String message, Field... fields) {
        String text = message == null ? "" : message.toLowerCase();
        for (Field f : fields) {
            if (text.contains(f.key)) {
                f.setError(message);
                return true;
            }
        }
        return false;
    }

    private static void clear(Field... fields) {
        for (Field f : fields) {
            f.setError(null);
        }
    }

    private static JLabel h1(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(BrandPanel.semibold(size));
        label.setForeground(INK);
        return label;
    }

    private static WrapText lead(String text) {
        return new WrapText(text, FORM_WIDTH, Theme.font(Font.PLAIN, 15), BODY);
    }

    private static WrapText note(String text) {
        return new WrapText(text, FORM_WIDTH, Theme.font(Font.PLAIN, 13), LABEL);
    }

    private static JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 13));
        label.setForeground(LABEL);
        return label;
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(LABEL);
        return label;
    }

    private static JLabel overline(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 11).deriveFont(Map.of(TextAttribute.TRACKING, 0.12f)));
        label.setForeground(LABEL);
        return label;
    }

    private static JLabel valueLabel(String text, Color color, boolean bold) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, bold ? 16 : 13));
        label.setForeground(color);
        return label;
    }

    /** Fila del resumen: nombre a la izquierda, valor a la derecha y una línea fina debajo. */
    private static JPanel summaryRow(String name, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, SOFT_LINE),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)));
        JLabel label = new JLabel(name);
        label.setFont(Theme.font(Font.PLAIN, 15));
        label.setForeground(INK);
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(Theme.font(Font.PLAIN, 13));
        label.setForeground(Theme.DANGER);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return label;
    }

    private static JLabel tab(String text) {
        JLabel label = new JLabel(text);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return label;
    }

    private static void styleTab(JLabel tab, boolean selected) {
        tab.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 15));
        tab.setForeground(selected ? INK : LABEL);
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, selected ? Theme.PRIMARY : new Color(0, 0, 0, 0)),
                BorderFactory.createEmptyBorder(0, 0, 10, 0)));
    }

    private static JLabel link(String text, Runnable action) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(Theme.PRIMARY);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(click(action));
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

    private static JComponent divider() {
        JPanel line = new JPanel();
        line.setOpaque(true);
        line.setBackground(SOFT_LINE);
        line.setPreferredSize(new Dimension(FORM_WIDTH, 1));
        line.setMaximumSize(new Dimension(FORM_WIDTH, 1));
        return line;
    }

    private static JTextField textField() {
        JTextField field = new JTextField();
        styleEditor(field);
        return field;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        styleEditor(field);
        return field;
    }

    private static void styleEditor(JTextComponent editor) {
        editor.setFont(Theme.font(Font.PLAIN, 16));
        editor.setForeground(INK);
        editor.setCaretColor(INK);
        editor.setOpaque(false);
        editor.setBorder(BorderFactory.createEmptyBorder());
    }

    private static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setOpaque(false);
        box.setFont(Theme.font(Font.PLAIN, 13));
        box.setForeground(LABEL);
        box.setFocusPainted(false);
        box.setBorder(BorderFactory.createEmptyBorder());
        return box;
    }

    /** Botón "Mostrar" / "Ocultar" que va a la derecha de un campo de contraseña. */
    private static JLabel toggle(JPasswordField field) {
        char echo = field.getEchoChar();
        JLabel toggle = new JLabel("Mostrar");
        toggle.setFont(Theme.font(Font.BOLD, 12));
        toggle.setForeground(LABEL);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addMouseListener(click(() -> {
            boolean hidden = field.getEchoChar() != 0;
            field.setEchoChar(hidden ? (char) 0 : echo);
            toggle.setText(hidden ? "Ocultar" : "Mostrar");
        }));
        return toggle;
    }

    // ------------------------------------------------------------------ Componentes propios

    /**
     * Campo con el nombre arriba y una línea debajo. La línea se pone azul cuando el campo tiene
     * el foco y roja con su mensaje cuando hay un error.
     */
    private static final class Field extends JPanel {
        private final JLabel label;
        private final JLabel message = new JLabel(" ");
        private String key;
        private boolean focused;
        private boolean error;

        Field(String text, JComponent editor) {
            this(text, editor, null);
        }

        Field(String text, JComponent editor, JComponent trailing) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            key = text.toLowerCase().split(" ")[0];
            label = new JLabel(text);
            label.setFont(Theme.font(Font.PLAIN, 12));
            label.setForeground(LABEL);
            add(left(label));

            JPanel row = new JPanel(new BorderLayout(8, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int thickness = focused || error ? 2 : 1;
                    g.setColor(error ? Theme.DANGER : focused ? Theme.PRIMARY : LINE);
                    g.fillRect(0, getHeight() - thickness, getWidth(), thickness);
                }
            };
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(6, 0, 9, 0));
            row.add(editor, BorderLayout.CENTER);
            if (trailing != null) {
                row.add(trailing, BorderLayout.EAST);
            }
            add(left(row));

            message.setFont(Theme.font(Font.PLAIN, 12));
            message.setForeground(Theme.DANGER);
            message.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            message.setVisible(false);
            add(left(message));

            editor.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    refresh();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    refresh();
                }
            });
            if (editor instanceof JTextComponent) {
                ((JTextComponent) editor).getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        if (error && !((JTextComponent) editor).getText().isEmpty()) {
                            setError(null);
                        }
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        // Borrar no quita el error: así el mensaje sigue a la vista tras un intento fallido.
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        // No aplica a campos de texto plano.
                    }
                });
            }
        }

        /** Cambia la palabra con la que se reconoce este campo en los mensajes de error. */
        Field key(String word) {
            this.key = word;
            return this;
        }

        void setError(String text) {
            error = text != null;
            message.setText(error ? text : " ");
            message.setVisible(error);
            refresh();
            revalidate();
        }

        private void refresh() {
            label.setForeground(error ? Theme.DANGER : focused ? Theme.PRIMARY : LABEL);
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(FORM_WIDTH, getPreferredSize().height);
        }
    }

    /** Botón redondeado tipo píldora: azul principal, verde o con borde. */
    private static final class PillButton extends JButton {
        static final int PRIMARY = 0;
        static final int SUCCESS = 1;
        static final int OUTLINE = 2;
        static final int SHADOW = 6;
        private static final int HEIGHT = 44;

        private final int style;

        PillButton(String text, int style) {
            super(text);
            this.style = style;
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Theme.font(Font.BOLD, 15));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 56, HEIGHT + SHADOW);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            boolean hot = getModel().isRollover() || getModel().isPressed();
            Color fill;
            Color text = Color.WHITE;
            Color glow = null;
            if (!isEnabled()) {
                fill = SOFT_LINE;
                text = ARROW;
            } else if (style == OUTLINE) {
                fill = hot ? new Color(0xF4F4F5) : Color.WHITE;
                text = INK;
            } else if (style == SUCCESS) {
                fill = hot ? new Color(0x15803D) : Theme.SUCCESS;
                glow = Theme.SUCCESS;
            } else {
                fill = hot ? Theme.PRIMARY_DARK : Theme.PRIMARY;
                glow = Theme.PRIMARY;
            }
            if (glow != null) {
                for (int i = SHADOW; i >= 1; i--) {
                    g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 10));
                    g2.fill(new RoundRectangle2D.Double(i, 2 + i * 0.6, w - 2.0 * i, HEIGHT, HEIGHT, HEIGHT));
                }
            }
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, 0, w - 1, HEIGHT - 1, HEIGHT, HEIGHT));
            if (style == OUTLINE && isEnabled()) {
                g2.setColor(LINE);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 2, HEIGHT - 2, HEIGHT, HEIGHT));
            }
            g2.setFont(getFont());
            g2.setColor(text);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2,
                    (HEIGHT - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Opción de la bienvenida: ícono, título, descripción y flecha. Se resalta al pasar el mouse. */
    private static final class OptionRow extends JPanel {
        static final int CAR = 0;
        static final int BADGE = 1;
        private static final int RING = 3;
        private boolean hover;

        OptionRow(int icon, String title, String description, Runnable action) {
            super(new BorderLayout(18, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(18 + RING, 20 + RING, 18 + RING, 20 + RING));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel iconBox = new JPanel(new BorderLayout());
            iconBox.setOpaque(false);
            iconBox.add(new OutlineIcon(icon), BorderLayout.CENTER);
            add(iconBox, BorderLayout.WEST);

            JPanel texts = column();
            JLabel name = new JLabel(title);
            name.setFont(BrandPanel.semibold(17));
            name.setForeground(INK);
            texts.add(left(name));
            texts.add(Box.createVerticalStrut(3));
            texts.add(left(new WrapText(description, 236, Theme.font(Font.PLAIN, 13), LABEL)));
            add(texts, BorderLayout.CENTER);

            JLabel arrow = new JLabel("→");
            arrow.setFont(Theme.font(Font.PLAIN, 20));
            arrow.setForeground(ARROW);
            add(arrow, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    arrow.setForeground(Theme.PRIMARY);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    arrow.setForeground(ARROW);
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(FORM_WIDTH + 2 * RING, getPreferredSize().height);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(FORM_WIDTH + 2 * RING, super.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double w = getWidth() - 2.0 * RING;
            double h = getHeight() - 2.0 * RING;
            if (hover) {
                g2.setColor(new Color(0xDBEAFE));
                g2.setStroke(new BasicStroke(RING * 2));
                g2.draw(new RoundRectangle2D.Double(RING, RING, w - 1, h - 1, 24, 24));
            }
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(RING, RING, w - 1, h - 1, 24, 24));
            g2.setColor(hover ? Theme.PRIMARY : SOFT_LINE);
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Double(RING + 0.5, RING + 0.5, w - 2, h - 2, 24, 24));
            g2.dispose();
        }
    }

    /** Ícono de línea (carro o carné) dentro de un cuadro con borde. */
    private static final class OutlineIcon extends JComponent {
        private static final int SIZE = 44;
        private final int icon;

        OutlineIcon(int icon) {
            this.icon = icon;
            Dimension size = new Dimension(SIZE, SIZE);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - SIZE) / 2;
            g2.translate(0, y);
            g2.setColor(new Color(0x3F3F46));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Double(0.75, 0.75, SIZE - 1.5, SIZE - 1.5, 20, 20));
            if (icon == OptionRow.CAR) {
                Path2D body = new Path2D.Double();
                body.moveTo(12, 27);
                body.lineTo(15, 19);
                body.quadTo(16, 17, 18.5, 17);
                body.lineTo(25.5, 17);
                body.quadTo(28, 17, 29, 19);
                body.lineTo(32, 27);
                body.lineTo(32, 30);
                body.lineTo(12, 30);
                body.closePath();
                g2.draw(body);
                g2.drawLine(12, 26, 32, 26);
                g2.drawOval(15, 28, 4, 4);
                g2.drawOval(25, 28, 4, 4);
            } else {
                g2.draw(new RoundRectangle2D.Double(11, 14, 22, 16, 3, 3));
                g2.drawOval(14, 17, 5, 5);
                g2.drawArc(13, 23, 7, 6, 0, 180);
                g2.drawLine(23, 19, 29, 19);
                g2.drawLine(23, 24, 28, 24);
            }
            g2.dispose();
        }
    }

    /** Logo de RutaPeaje: cuadro azul oscuro con una barra ámbar. */
    private static final class LogoMark extends JComponent {
        LogoMark() {
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(NAVY);
            g2.fill(new RoundRectangle2D.Double(0, 0, 30, 30, 16, 16));
            g2.setColor(Theme.WARNING);
            g2.fill(new RoundRectangle2D.Double(7, 13, 16, 4, 4, 4));
            g2.dispose();
        }
    }

    /** Círculo verde claro con un chulo: confirma que el empleado entró bien. */
    private static final class Badge extends JComponent {
        private static final int SIZE = 44;

        Badge() {
            Dimension size = new Dimension(SIZE, SIZE);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xDCFCE7));
            g2.fillOval(0, 0, SIZE, SIZE);
            g2.setColor(Theme.SUCCESS);
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{14, 20, 31}, new int[]{23, 29, 16}, 3);
            g2.dispose();
        }
    }

    /** Texto de varias líneas que se parte solo al ancho indicado. */
    private static final class WrapText extends JTextArea {
        private final int width;

        WrapText(String text, int width, Font font, Color color) {
            super(text);
            this.width = width;
            setFont(font);
            setForeground(color);
            setLineWrap(true);
            setWrapStyleWord(true);
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setHighlighter(null);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public Dimension getPreferredSize() {
            View view = getUI().getRootView(this);
            view.setSize(width, Short.MAX_VALUE);
            return new Dimension(width, (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    /** Lista desplegable sin recuadro, con una flecha simple: va sobre la línea del campo. */
    private static final class FlatComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton("⌄");
            arrow.setUI(new BasicButtonUI());
            arrow.setFont(Theme.font(Font.BOLD, 16));
            arrow.setForeground(LABEL);
            arrow.setContentAreaFilled(false);
            arrow.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 0));
            arrow.setFocusPainted(false);
            return arrow;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
            // Sin fondo: se ve solo el texto sobre la línea del campo.
        }

        @Override
        public void paintCurrentValue(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
            super.paintCurrentValue(g, bounds, false);
        }
    }

    /** Pestañas con línea gris debajo; la pestaña activa lleva su propia línea azul. */
    private static final class TabBar extends JPanel {
        TabBar() {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(SOFT_LINE);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }

    /** Recuadro blanco con esquinas redondeadas y borde. */
    private static final class RoundedCard extends JPanel {
        private final Color border;

        RoundedCard(Color border) {
            this.border = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
            g2.setColor(border);
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 20, 20));
            g2.dispose();
        }
    }
}
