package com.invpeajes.model;

import com.invpeajes.model.TollStatistics.Total;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resumen de los peajes de una entidad para el inicio del administrador: recaudo de hoy y del
 * mes (comparados con el mismo tramo del periodo anterior), recaudo por mes del año, peajes con
 * más recaudo, asistencias activas y los pendientes que requieren atención.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AdminSummary {

    /** Un aviso de "Pendientes de tus peajes". */
    public static final class Pending {
        /** Clase de aviso: decide el color y el ícono. */
        public enum Kind { NO_RATES, DAMAGED, NO_MANAGER, CLOSE_TOLLS, OLD_RATES }

        private final Kind kind;
        private final String title;
        private final String detail;

        /**
         * @param kind Clase de aviso.
         * @param title Título corto (ej. "2 peajes sin tarifa").
         * @param detail Explicación (ej. los nombres de los peajes).
         */
        public Pending(Kind kind, String title, String detail) {
            this.kind = kind;
            this.title = title;
            this.detail = detail;
        }

        /** @return Clase de aviso. */
        public Kind getKind() {
            return kind;
        }

        /** @return Título corto. */
        public String getTitle() {
            return title;
        }

        /** @return Explicación. */
        public String getDetail() {
            return detail;
        }
    }

    private Total today = new Total(0, BigDecimal.ZERO);
    private Total yesterdaySameTime = new Total(0, BigDecimal.ZERO);
    private Total month = new Total(0, BigDecimal.ZERO);
    private Total previousMonthSameDays = new Total(0, BigDecimal.ZERO);
    private Map<Integer, BigDecimal> revenueByMonth = new LinkedHashMap<>();
    private Map<String, BigDecimal> topTolls = new LinkedHashMap<>();
    private int activeAssistances;
    private int unattendedAssistances;
    private int operatingTolls;
    private List<Pending> pendings = new ArrayList<>();

    /** @return Vehículos y recaudo de hoy. */
    public Total getToday() {
        return today;
    }

    /** @param today Vehículos y recaudo de hoy. */
    public void setToday(Total today) {
        this.today = today;
    }

    /** @return Ayer, desde las 00:00 hasta la misma hora de ahora. */
    public Total getYesterdaySameTime() {
        return yesterdaySameTime;
    }

    /** @param yesterdaySameTime Ayer hasta la misma hora de ahora. */
    public void setYesterdaySameTime(Total yesterdaySameTime) {
        this.yesterdaySameTime = yesterdaySameTime;
    }

    /** @return Lo que va del mes. */
    public Total getMonth() {
        return month;
    }

    /** @param month Lo que va del mes. */
    public void setMonth(Total month) {
        this.month = month;
    }

    /** @return Los mismos días del mes anterior. */
    public Total getPreviousMonthSameDays() {
        return previousMonthSameDays;
    }

    /** @param previousMonthSameDays Los mismos días del mes anterior. */
    public void setPreviousMonthSameDays(Total previousMonthSameDays) {
        this.previousMonthSameDays = previousMonthSameDays;
    }

    /** @return Recaudo de cada mes del año (1 = enero), hasta el mes actual. */
    public Map<Integer, BigDecimal> getRevenueByMonth() {
        return revenueByMonth;
    }

    /** @param revenueByMonth Recaudo de cada mes del año. */
    public void setRevenueByMonth(Map<Integer, BigDecimal> revenueByMonth) {
        this.revenueByMonth = revenueByMonth;
    }

    /** @return Peajes con más recaudo en el mes (nombre → recaudo), de mayor a menor. */
    public Map<String, BigDecimal> getTopTolls() {
        return topTolls;
    }

    /** @param topTolls Peajes con más recaudo en el mes. */
    public void setTopTolls(Map<String, BigDecimal> topTolls) {
        this.topTolls = topTolls;
    }

    /** @return Solicitudes de asistencia sin cerrar. */
    public int getActiveAssistances() {
        return activeAssistances;
    }

    /** @param activeAssistances Solicitudes sin cerrar. */
    public void setActiveAssistances(int activeAssistances) {
        this.activeAssistances = activeAssistances;
    }

    /** @return Solicitudes registradas que nadie ha atendido. */
    public int getUnattendedAssistances() {
        return unattendedAssistances;
    }

    /** @param unattendedAssistances Solicitudes sin atender. */
    public void setUnattendedAssistances(int unattendedAssistances) {
        this.unattendedAssistances = unattendedAssistances;
    }

    /** @return Peajes de la entidad en estado OPERATIVO. */
    public int getOperatingTolls() {
        return operatingTolls;
    }

    /** @param operatingTolls Peajes operativos. */
    public void setOperatingTolls(int operatingTolls) {
        this.operatingTolls = operatingTolls;
    }

    /** @return Avisos de "Pendientes de tus peajes". */
    public List<Pending> getPendings() {
        return pendings;
    }

    /** @param pendings Avisos. */
    public void setPendings(List<Pending> pendings) {
        this.pendings = pendings;
    }
}
