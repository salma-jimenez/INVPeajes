package com.invpeajes.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Periodos que consulta el jefe de estación en las estadísticas del peaje.
 * Cada periodo calcula su rango de fechas, el periodo anterior (para comparar)
 * y cómo se agrupa la gráfica (por hora, por día o por mes).
 *
 * @author Salomejimenez
 * @version 1.0
 */
public enum StatPeriod {
    DAY("Día", "ayer"),
    WEEK("Semana", "la semana pasada"),
    MONTH("Mes", "el mes anterior"),
    SEMESTER("Semestre", "el semestre anterior"),
    YEAR("Año", "el año anterior");

    /** Cómo se agrupan los valores de la gráfica. */
    public enum Grouping { HOUR, DAY, MONTH }

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("dd/MM");

    private final String label;
    private final String previousLabel;

    StatPeriod(String label, String previousLabel) {
        this.label = label;
        this.previousLabel = previousLabel;
    }

    /** @return Nombre del periodo para el selector (ej. "Mes"). */
    public String getLabel() {
        return label;
    }

    /** @return Cómo se nombra el periodo anterior (ej. "el mes anterior"). */
    public String getPreviousLabel() {
        return previousLabel;
    }

    /**
     * Primer día del periodo que contiene la fecha.
     * @param date Cualquier fecha del periodo.
     * @return Fecha de inicio (incluida).
     */
    public LocalDate start(LocalDate date) {
        switch (this) {
            case DAY:
                return date;
            case WEEK:
                return date.with(DayOfWeek.MONDAY);
            case MONTH:
                return date.withDayOfMonth(1);
            case SEMESTER:
                return LocalDate.of(date.getYear(), date.getMonthValue() <= 6 ? 1 : 7, 1);
            default:
                return LocalDate.of(date.getYear(), 1, 1);
        }
    }

    /**
     * Día siguiente al último día del periodo.
     * @param start Inicio del periodo.
     * @return Fecha de fin (no incluida).
     */
    public LocalDate end(LocalDate start) {
        switch (this) {
            case DAY:
                return start.plusDays(1);
            case WEEK:
                return start.plusWeeks(1);
            case MONTH:
                return start.plusMonths(1);
            case SEMESTER:
                return start.plusMonths(6);
            default:
                return start.plusYears(1);
        }
    }

    /**
     * Inicio del periodo anterior, del mismo tamaño.
     * @param start Inicio del periodo actual.
     * @return Inicio del periodo anterior.
     */
    public LocalDate previousStart(LocalDate start) {
        switch (this) {
            case DAY:
                return start.minusDays(1);
            case WEEK:
                return start.minusWeeks(1);
            case MONTH:
                return start.minusMonths(1);
            case SEMESTER:
                return start.minusMonths(6);
            default:
                return start.minusYears(1);
        }
    }

    /** @return Agrupación de la gráfica: por hora (día), por día (semana y mes) o por mes. */
    public Grouping grouping() {
        switch (this) {
            case DAY:
                return Grouping.HOUR;
            case WEEK:
            case MONTH:
                return Grouping.DAY;
            default:
                return Grouping.MONTH;
        }
    }

    /**
     * Nombre del periodo que empieza en la fecha.
     * @param start Inicio del periodo.
     * @return Texto como "02/11/2026", "Semana del 26/10", "Octubre 2026", "2.º semestre 2026" o "2026".
     */
    public String describe(LocalDate start) {
        switch (this) {
            case DAY:
                return start.format(DAY_FORMAT);
            case WEEK:
                return "Semana del " + start.format(SHORT_DAY);
            case MONTH:
                return monthName(start.getMonthValue()) + " " + start.getYear();
            case SEMESTER:
                return (start.getMonthValue() == 1 ? "1.er" : "2.º") + " semestre " + start.getYear();
            default:
                return String.valueOf(start.getYear());
        }
    }

    /**
     * Periodos que se pueden escoger en el selector, del más reciente al más antiguo.
     * @param today Fecha actual.
     * @return Inicios de los periodos.
     */
    public List<LocalDate> recentStarts(LocalDate today) {
        int count;
        switch (this) {
            case DAY:
                count = 14;
                break;
            case WEEK:
                count = 8;
                break;
            case MONTH:
                count = 12;
                break;
            case SEMESTER:
                count = 4;
                break;
            default:
                count = 3;
                break;
        }
        List<LocalDate> starts = new ArrayList<>();
        LocalDate current = start(today);
        for (int i = 0; i < count; i++) {
            starts.add(current);
            current = previousStart(current);
        }
        return starts;
    }

    /**
     * Nombre del mes en español con mayúscula inicial.
     * @param month Número del mes (1 a 12).
     * @return Ej. "Octubre".
     */
    public static String monthName(int month) {
        String name = java.time.Month.of(month).getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
