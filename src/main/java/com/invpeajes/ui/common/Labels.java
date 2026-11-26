package com.invpeajes.ui.common;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Textos y colores en español para los valores que se guardan en la base con mayúsculas y guion
 * bajo (estados de peajes y casetas, sentidos, formas de pago, entidades y cargos).
 *
 * @author Salomejimenez
 * @version 1.0
 */
public final class Labels {

    /** Estados del peaje, en el orden en que se muestran. */
    public static final String[] TOLL_STATUSES = {"OPERATIVO", "SIN_TARIFA", "NO_OPERATIVO", "SINIESTRADO"};
    /** Estados de una caseta. */
    public static final String[] BOOTH_STATUSES = {"ACTIVA", "MANTENIMIENTO", "INACTIVA"};
    /** Sentidos de un peaje o una caseta. */
    public static final String[] DIRECTIONS = {"AMBOS", "ORIENTE_OCCIDENTE", "OCCIDENTE_ORIENTE", "NORTE_SUR",
        "SUR_NORTE", "OTRO"};
    /** Formas de pago de una caseta. */
    public static final String[] PAYMENT_TYPES = {"MIXTO", "EFECTIVO", "COLPASS"};

    private static final Color GREEN_BG = new Color(0xDCFCE7);
    private static final Color GREEN_FG = new Color(0x15803D);
    private static final Color AMBER_BG = new Color(0xFEF3C7);
    private static final Color AMBER_FG = new Color(0xB45309);
    private static final Color GRAY_BG = new Color(0xE5E7EB);
    private static final Color GRAY_FG = new Color(0x4B5563);
    private static final Color RED_BG = new Color(0xFEE2E2);
    private static final Color RED_FG = new Color(0xB91C1C);

    private Labels() {
    }

    /**
     * @param status Estado del peaje (OPERATIVO, SIN_TARIFA...).
     * @return Texto (ej. "Sin tarifa").
     */
    public static String tollStatus(String status) {
        if (status == null) {
            return "—";
        }
        switch (status) {
            case "OPERATIVO":
                return "Operativo";
            case "SIN_TARIFA":
                return "Sin tarifa";
            case "NO_OPERATIVO":
                return "No operativo";
            case "SINIESTRADO":
                return "Siniestrado";
            default:
                return pretty(status);
        }
    }

    /**
     * @param status Estado del peaje.
     * @return Pastilla de color con el estado.
     */
    public static Pill tollStatusPill(String status) {
        String s = status == null ? "" : status;
        switch (s) {
            case "OPERATIVO":
                return new Pill(tollStatus(s), GREEN_BG, GREEN_FG);
            case "SIN_TARIFA":
                return new Pill(tollStatus(s), AMBER_BG, AMBER_FG);
            case "SINIESTRADO":
                return new Pill(tollStatus(s), RED_BG, RED_FG);
            default:
                return new Pill(tollStatus(s), GRAY_BG, GRAY_FG);
        }
    }

    /**
     * @param status Estado de la caseta (ACTIVA, MANTENIMIENTO, INACTIVA).
     * @return Texto (ej. "Mantenimiento").
     */
    public static String boothStatus(String status) {
        return status == null ? "—" : pretty(status);
    }

    /**
     * @param status Estado de la caseta.
     * @return Color del punto y del borde (verde, ámbar o gris).
     */
    public static Color boothColor(String status) {
        if ("ACTIVA".equals(status)) {
            return Theme.SUCCESS;
        }
        return "MANTENIMIENTO".equals(status) ? Theme.WARNING : new Color(0x9CA3AF);
    }

    /**
     * @param status Estado de la caseta.
     * @return Colores de fondo y texto de su pastilla.
     */
    public static Color[] boothPillColors(String status) {
        if ("ACTIVA".equals(status)) {
            return new Color[]{GREEN_BG, GREEN_FG};
        }
        return "MANTENIMIENTO".equals(status) ? new Color[]{AMBER_BG, AMBER_FG} : new Color[]{GRAY_BG, GRAY_FG};
    }

    /**
     * @param status Estado de la caseta.
     * @return Pastilla de color con el estado.
     */
    public static Pill boothStatusPill(String status) {
        Color[] c = boothPillColors(status);
        return new Pill(boothStatus(status), c[0], c[1]);
    }

    /**
     * @param direction Sentido (ej. ORIENTE_OCCIDENTE).
     * @return Texto (ej. "Oriente → Occidente").
     */
    public static String direction(String direction) {
        if (direction == null) {
            return "Sin sentido";
        }
        if ("AMBOS".equals(direction)) {
            return "Ambos sentidos";
        }
        if ("OTRO".equals(direction)) {
            return "Otro";
        }
        StringBuilder text = new StringBuilder();
        for (String part : direction.split("_")) {
            text.append(text.length() == 0 ? "" : " → ").append(pretty(part));
        }
        return text.toString();
    }

    /**
     * @param type Forma de pago de la caseta (EFECTIVO, MIXTO, COLPASS).
     * @return Texto (ej. "Solo COLPASS").
     */
    public static String payment(String type) {
        if ("COLPASS".equals(type)) {
            return "Solo COLPASS";
        }
        if ("MIXTO".equals(type)) {
            return "Mixto (efectivo, tarjeta y TAG)";
        }
        return type == null ? "—" : pretty(type);
    }

    /**
     * @param type Forma de pago de la caseta.
     * @return Texto corto (ej. "Mixto").
     */
    public static String paymentShort(String type) {
        if ("COLPASS".equals(type)) {
            return "Solo COLPASS";
        }
        return type == null ? "—" : pretty(type);
    }

    /**
     * @param method Medio de pago de un tiquete (EFECTIVO, TARJETA, TAG_COLPASS).
     * @return Texto (ej. "COLPASS").
     */
    public static String method(String method) {
        if ("TAG_COLPASS".equals(method)) {
            return "COLPASS";
        }
        return method == null ? "—" : pretty(method);
    }

    /**
     * @param level Entidad (PEAJE.NIVEL).
     * @return Texto corto (ej. "INVÍAS", "Concesión territorial").
     */
    public static String entity(String level) {
        if ("ANI".equals(level)) {
            return "ANI";
        }
        if ("INVIAS".equals(level)) {
            return "INVÍAS";
        }
        return level == null ? "—" : pretty(level);
    }

    /**
     * @param level Entidad (PEAJE.NIVEL).
     * @return Nombre completo (ej. "ANI · Agencia Nacional de Infraestructura").
     */
    public static String entityLong(String level) {
        if ("ANI".equals(level)) {
            return "ANI · Agencia Nacional de Infraestructura";
        }
        if ("INVIAS".equals(level)) {
            return "INVÍAS · Instituto Nacional de Vías";
        }
        return entity(level);
    }

    /**
     * @param role Cargo (OPERADOR DE CASETA, JEFE DE ESTACION, ADMINISTRADOR).
     * @return Texto (ej. "Jefe de estación").
     */
    public static String role(String role) {
        if ("JEFE DE ESTACION".equals(role)) {
            return "Jefe de estación";
        }
        if ("OPERADOR DE CASETA".equals(role)) {
            return "Operador de caseta";
        }
        return role == null ? "—" : pretty(role);
    }

    /**
     * @param role Cargo.
     * @return Pastilla de color con el cargo.
     */
    public static Pill rolePill(String role) {
        if ("JEFE DE ESTACION".equals(role)) {
            return new Pill(role(role), new Color(0xEDE9FE), new Color(0x6D28D9));
        }
        if ("OPERADOR DE CASETA".equals(role)) {
            return new Pill(role(role), new Color(0xDBEAFE), new Color(0x1D4ED8));
        }
        return new Pill(role(role), AMBER_BG, AMBER_FG);
    }

    /**
     * @param active true si el empleado está activo.
     * @return Pastilla "Activo" o "Inactivo".
     */
    public static Pill activePill(boolean active) {
        return active ? new Pill("Activo", GREEN_BG, GREEN_FG) : new Pill("Inactivo", GRAY_BG, GRAY_FG);
    }

    /**
     * @param n Número de categoría (1 a 7).
     * @return Número romano (I a VII).
     */
    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n > 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    /**
     * Variación entre dos valores, como texto con flecha (ej. "▲ 4,2 % frente a ayer a esta hora").
     * @param current Valor actual.
     * @param previous Valor anterior.
     * @param against Contra qué se compara (ej. "ayer a esta hora").
     * @return Texto, o "Sin datos de ..." si el anterior es cero.
     */
    public static String variation(BigDecimal current, BigDecimal previous, String against) {
        if (previous == null || previous.signum() == 0) {
            return "Sin datos de " + against;
        }
        double v = (current.doubleValue() - previous.doubleValue()) * 100 / previous.doubleValue();
        String target = against.startsWith("el ") ? "al " + against.substring(3) : "a " + against;
        return String.format(Locale.forLanguageTag("es-CO"), "%s %.1f %% frente %s", v >= 0 ? "▲" : "▼",
                Math.abs(v), target);
    }

    /**
     * @param value Texto en mayúsculas con guiones bajos (ej. NO_OPERATIVO).
     * @return Texto con mayúscula inicial (ej. "No operativo").
     */
    public static String pretty(String value) {
        String text = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
