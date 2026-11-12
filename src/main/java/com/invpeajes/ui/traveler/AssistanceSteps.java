package com.invpeajes.ui.traveler;

import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.ui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 * Los pasos de una asistencia (Registrada → Asignada → En camino → En proceso → Atendida)
 * dibujados como una barra de progreso con la hora de cada paso.
 *
 * <ul>
 *   <li>Modo grande (Mis solicitudes): círculos con ✓, el paso actual resaltado y su hora.</li>
 *   <li>Modo compacto (franja del Inicio, fondo oscuro): cinco barras con el nombre debajo.</li>
 * </ul>
 * Si la solicitud se canceló, los pasos que alcanzó quedan en gris.
 *
 * <p>También tiene los textos que explican al viajero qué está pasando en cada paso.</p>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class AssistanceSteps extends JComponent {

    /** Los pasos en orden (CANCELADA no es un paso: se muestra aparte). */
    public static final AssistanceStatus[] STEPS = {
        AssistanceStatus.REGISTERED, AssistanceStatus.ASSIGNED, AssistanceStatus.EN_ROUTE,
        AssistanceStatus.IN_PROGRESS, AssistanceStatus.ATTENDED};
    private static final String[] NAMES = {"Registrada", "Asignada", "En camino", "En proceso", "Atendida"};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final Color GRAY_LINE = Theme.BORDER;
    private static final Color GRAY_RING = Theme.INPUT_BORDER;
    private static final Color CANCELLED = new Color(0x9CA3AF);

    private final boolean compact;
    private AssistanceStatus status = AssistanceStatus.REGISTERED;
    private int reached;
    private final Map<AssistanceStatus, LocalDateTime> times = new EnumMap<>(AssistanceStatus.class);

    /**
     * @param compact true para la versión pequeña sobre fondo oscuro (Inicio)
     */
    public AssistanceSteps(boolean compact) {
        this.compact = compact;
        setOpaque(false);
        setPreferredSize(new Dimension(400, compact ? 38 : 86));
        setMinimumSize(new Dimension(200, compact ? 38 : 86));
    }

    /**
     * @param current estado actual de la solicitud
     * @param history cambios de estado (puede ser null o vacía mientras carga)
     */
    public void setData(AssistanceStatus current, List<AssistanceHistory> history) {
        status = current == null ? AssistanceStatus.REGISTERED : current;
        times.clear();
        for (AssistanceHistory h : history == null ? Collections.<AssistanceHistory>emptyList() : history) {
            if (h.getNewStatus() != null && h.getChangeDate() != null) {
                LocalDateTime old = times.get(h.getNewStatus());
                if (old == null || h.getChangeDate().isAfter(old)) {
                    times.put(h.getNewStatus(), h.getChangeDate());
                }
            }
        }
        reached = reachedIndex(status, history);
        repaint();
    }

    /**
     * Índice del último paso alcanzado (0 a 4). Para una cancelada, el último paso al que llegó.
     */
    private static int reachedIndex(AssistanceStatus status, List<AssistanceHistory> history) {
        if (status != AssistanceStatus.CANCELLED) {
            return indexOf(status);
        }
        int best = 0;
        if (history != null) {
            for (AssistanceHistory h : history) {
                best = Math.max(best, indexOf(h.getNewStatus()));
                best = Math.max(best, indexOf(h.getPreviousStatus()));
            }
        }
        return best;
    }

    private static int indexOf(AssistanceStatus s) {
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i] == s) {
                return i;
            }
        }
        return 0;
    }

    /**
     * @param status estado
     * @return cuántos de los cinco pasos lleva (para la mini barra de las tarjetas)
     */
    public static int progress(AssistanceStatus status) {
        return status == AssistanceStatus.CANCELLED ? 0 : indexOf(status) + 1;
    }

    // ------------------------------------------------------------------ Textos para el viajero

    /**
     * @param type tipo de asistencia
     * @return el recurso con su artículo ("La grúa", "El carro taller"...)
     */
    public static String withArticle(AssistanceType type) {
        if (type == null) {
            return "El recurso";
        }
        switch (type) {
            case TOW_TRUCK:
                return "La grúa";
            case AMBULANCE:
                return "La ambulancia";
            case MOBILE_WORKSHOP:
                return "El carro taller";
            case MECHANICAL_AID:
                return "El auxilio mecánico";
            default:
                return "El recurso";
        }
    }

    /**
     * @param status   estado actual
     * @param type     tipo de asistencia
     * @param tollName nombre del peaje que atiende
     * @return título corto de lo que está pasando ("La grúa va en camino")
     */
    public static String headline(AssistanceStatus status, AssistanceType type, String tollName) {
        String who = withArticle(type);
        switch (status) {
            case REGISTERED:
                return "Recibimos su solicitud";
            case ASSIGNED:
                return "Ya le asignaron " + Character.toLowerCase(who.charAt(0)) + who.substring(1).toLowerCase();
            case EN_ROUTE:
                return who + " va en camino hacia su vehículo";
            case IN_PROGRESS:
                return "Lo están atendiendo";
            case ATTENDED:
                return "Asistencia terminada";
            default:
                return "La solicitud fue cancelada";
        }
    }

    /**
     * @param status   estado actual
     * @param tollName nombre del peaje que atiende
     * @param phone    teléfono de contacto de la solicitud
     * @return explicación de una o dos frases para el viajero
     */
    public static String explanation(AssistanceStatus status, String tollName, String phone) {
        String tel = phone == null || phone.isBlank() ? "a su teléfono" : "al " + phone;
        switch (status) {
            case REGISTERED:
                return "El peaje " + tollName + " ya la tiene. En breve el jefe de estación le asigna un recurso.";
            case ASSIGNED:
                return "Saldrá pronto desde el peaje " + tollName + ". Quédese cerca de su vehículo.";
            case EN_ROUTE:
                return "Quédese cerca del vehículo y tenga el teléfono a la mano: pueden llamarle " + tel + ".";
            case IN_PROGRESS:
                return "El equipo está trabajando en su vehículo. Siga sus indicaciones.";
            case ATTENDED:
                return "Gracias por usar RutaPeaje. ¡Buen viaje!";
            default:
                return "Si aún necesita ayuda, pida una nueva asistencia o llame al 123.";
        }
    }

    /**
     * @param date fecha
     * @return "hoy 14:20" o "03/11 14:20"
     */
    public static String when(LocalDateTime date) {
        if (date == null) {
            return "—";
        }
        return date.toLocalDate().equals(LocalDate.now()) ? "hoy " + date.format(TIME) : date.format(DATE_TIME);
    }

    // ------------------------------------------------------------------ Dibujo

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (compact) {
            paintCompact(g2);
        } else {
            paintLarge(g2);
        }
        g2.dispose();
    }

    private void paintLarge(Graphics2D g) {
        boolean cancelled = status == AssistanceStatus.CANCELLED;
        Color done = cancelled ? CANCELLED : Theme.PRIMARY;
        int n = STEPS.length;
        double cell = getWidth() / (double) n;
        int r = 18;
        int cy = r + 2;
        for (int i = 0; i < n - 1; i++) {
            int x1 = (int) (cell * i + cell / 2);
            int x2 = (int) (cell * (i + 1) + cell / 2);
            g.setColor(i < reached ? done : GRAY_LINE);
            g.fillRect(x1, cy - 2, x2 - x1, 4);
        }
        for (int i = 0; i < n; i++) {
            int cx = (int) (cell * i + cell / 2);
            boolean isDone = i < reached || (i == reached && (status == AssistanceStatus.ATTENDED || cancelled));
            boolean isCurrent = i == reached && !isDone;
            if (isDone) {
                g.setColor(done);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{cx - 7, cx - 2, cx + 8}, new int[]{cy, cy + 5, cy - 5}, 3);
            } else if (isCurrent) {
                g.setColor(Theme.PRIMARY_SOFT);
                g.fillOval(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2);
                g.setColor(Color.WHITE);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(Theme.PRIMARY);
                g.setStroke(new BasicStroke(4f));
                g.drawOval(cx - r + 2, cy - r + 2, r * 2 - 4, r * 2 - 4);
                g.fillOval(cx - 6, cy - 6, 12, 12);
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(GRAY_RING);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(cx - r + 2, cy - r + 2, r * 2 - 4, r * 2 - 4);
            }
            boolean strong = isCurrent || isDone;
            g.setFont(Theme.font(isCurrent ? Font.BOLD : (strong ? Font.BOLD : Font.PLAIN), 14));
            g.setColor(isCurrent ? Theme.PRIMARY_DARK : (strong ? Theme.TEXT : Theme.MUTED));
            center(g, NAMES[i], cx, cy + r + 22);
            LocalDateTime t = times.get(STEPS[i]);
            String sub = t == null ? (isDone && i == 0 ? "" : "—") : t.format(TIME) + (isCurrent ? " · ahora" : "");
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.MUTED);
            center(g, sub, cx, cy + r + 40);
        }
    }

    private void paintCompact(Graphics2D g) {
        int n = STEPS.length;
        int gap = 6;
        double w = (getWidth() - gap * (n - 1)) / (double) n;
        for (int i = 0; i < n; i++) {
            int x = (int) (i * (w + gap));
            boolean isDone = i < reached;
            boolean isCurrent = i == reached;
            g.setColor(isCurrent ? Color.WHITE : (isDone ? new Color(0x60A5FA) : new Color(0x1B3A78)));
            g.fillRoundRect(x, 0, (int) w, 6, 6, 6);
            LocalDateTime t = times.get(STEPS[i]);
            String text = NAMES[i] + (t != null && (isDone || isCurrent) ? " · " + t.format(TIME) : "");
            g.setFont(Theme.font(isCurrent ? Font.BOLD : Font.PLAIN, 12));
            g.setColor(isCurrent ? Color.WHITE : (isDone ? Theme.SIDEBAR_TEXT : Theme.SIDEBAR_MUTED));
            g.drawString(text, x, 26);
        }
    }

    private static void center(Graphics2D g, String text, int cx, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
    }
}
