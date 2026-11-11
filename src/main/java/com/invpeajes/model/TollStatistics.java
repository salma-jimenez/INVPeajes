package com.invpeajes.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estadísticas de un peaje en un periodo (día, semana, mes, semestre o año), con la comparación
 * contra el periodo anterior. Las arma {@code StationManagerService} y las muestra el apartado
 * "Estadísticas del peaje" del jefe de estación.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class TollStatistics {

    /** Vehículos y dinero recaudado de un grupo de cobros. */
    public static final class Total {
        private final long vehicles;
        private final BigDecimal revenue;

        /**
         * @param vehicles Cantidad de cobros.
         * @param revenue Valor recaudado.
         */
        public Total(long vehicles, BigDecimal revenue) {
            this.vehicles = vehicles;
            this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }

        /** @return Cantidad de cobros. */
        public long getVehicles() {
            return vehicles;
        }

        /** @return Valor recaudado. */
        public BigDecimal getRevenue() {
            return revenue;
        }

        /**
         * Suma dos totales.
         * @param other Otro total.
         * @return Total con ambos.
         */
        public Total plus(Total other) {
            return new Total(vehicles + other.vehicles, revenue.add(other.revenue));
        }
    }

    /** Un punto de la gráfica: etiqueta, valor del periodo y valor del periodo anterior. */
    public static final class Point {
        private final String label;
        private final BigDecimal current;
        private final BigDecimal previous;

        /**
         * @param label Etiqueta del eje (hora, día o mes).
         * @param current Recaudo del periodo.
         * @param previous Recaudo en la misma posición del periodo anterior.
         */
        public Point(String label, BigDecimal current, BigDecimal previous) {
            this.label = label;
            this.current = current;
            this.previous = previous;
        }

        /** @return Etiqueta del eje. */
        public String getLabel() {
            return label;
        }

        /** @return Recaudo del periodo. */
        public BigDecimal getCurrent() {
            return current;
        }

        /** @return Recaudo del periodo anterior (puede ser null si no aplica). */
        public BigDecimal getPrevious() {
            return previous;
        }
    }

    /** Una fila de la tabla "Todos los periodos". */
    public static final class PeriodRow {
        private final String label;
        private final Total total;
        private final Double variation;
        private final boolean selected;

        /**
         * @param label Nombre del periodo (ej. "Octubre 2026").
         * @param total Vehículos y recaudo.
         * @param variation Variación porcentual contra el periodo anterior (null si no hay base).
         * @param selected true si es el periodo que se está viendo.
         */
        public PeriodRow(String label, Total total, Double variation, boolean selected) {
            this.label = label;
            this.total = total;
            this.variation = variation;
            this.selected = selected;
        }

        /** @return Nombre del periodo. */
        public String getLabel() {
            return label;
        }

        /** @return Vehículos y recaudo. */
        public Total getTotal() {
            return total;
        }

        /** @return Variación porcentual, o null. */
        public Double getVariation() {
            return variation;
        }

        /** @return true si es el periodo seleccionado. */
        public boolean isSelected() {
            return selected;
        }
    }

    private StatPeriod period;
    private LocalDate start;
    private Total total = new Total(0, BigDecimal.ZERO);
    private Total previousTotal = new Total(0, BigDecimal.ZERO);
    private int days = 1;
    private boolean partial;
    private String bestLabel;
    private Total bestTotal;
    private List<Point> series = new ArrayList<>();
    private Map<Integer, Long> vehiclesByCategory = new LinkedHashMap<>();
    private Map<String, BigDecimal> revenueByMethod = new LinkedHashMap<>();
    private List<PeriodRow> periods = new ArrayList<>();
    private int attended;
    private int cancelled;
    private Double averageArrivalMinutes;
    private Double previousAverageArrivalMinutes;
    private Map<AssistanceType, Integer> attendedByType = new EnumMap<>(AssistanceType.class);

    /**
     * Variación porcentual entre dos valores.
     * @param current Valor actual.
     * @param previous Valor anterior.
     * @return Porcentaje (ej. 6.2), o null si el anterior es cero.
     */
    public static Double variation(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).doubleValue() * 100.0 / previous.doubleValue();
    }

    /** @return Variación del recaudo contra el periodo anterior, o null. */
    public Double getRevenueVariation() {
        return variation(total.getRevenue(), previousTotal.getRevenue());
    }

    /** @return Variación de los vehículos contra el periodo anterior, o null. */
    public Double getVehiclesVariation() {
        return variation(BigDecimal.valueOf(total.getVehicles()), BigDecimal.valueOf(previousTotal.getVehicles()));
    }

    /** @return Recaudo promedio por día del periodo. */
    public BigDecimal getAverageDailyRevenue() {
        return total.getRevenue().divide(BigDecimal.valueOf(Math.max(1, days)), 0, java.math.RoundingMode.HALF_UP);
    }

    /** @return Vehículos promedio por día. */
    public long getAverageDailyVehicles() {
        return Math.round(total.getVehicles() / (double) Math.max(1, days));
    }

    /** @return Periodo consultado. */
    public StatPeriod getPeriod() {
        return period;
    }

    /** @param period Periodo consultado. */
    public void setPeriod(StatPeriod period) {
        this.period = period;
    }

    /** @return Inicio del periodo. */
    public LocalDate getStart() {
        return start;
    }

    /** @param start Inicio del periodo. */
    public void setStart(LocalDate start) {
        this.start = start;
    }

    /** @return Total del periodo. */
    public Total getTotal() {
        return total;
    }

    /** @param total Total del periodo. */
    public void setTotal(Total total) {
        this.total = total;
    }

    /** @return Total del periodo anterior. */
    public Total getPreviousTotal() {
        return previousTotal;
    }

    /** @param previousTotal Total del periodo anterior. */
    public void setPreviousTotal(Total previousTotal) {
        this.previousTotal = previousTotal;
    }

    /** @return Días transcurridos del periodo (para los promedios). */
    public int getDays() {
        return days;
    }

    /** @param days Días transcurridos del periodo. */
    public void setDays(int days) {
        this.days = days;
    }

    /** @return true si el periodo todavía no termina (se compara con los mismos días del anterior). */
    public boolean isPartial() {
        return partial;
    }

    /** @param partial true si el periodo todavía no termina. */
    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    /** @return Mejor día (o mejor hora si el periodo es un día), o null si no hubo cobros. */
    public String getBestLabel() {
        return bestLabel;
    }

    /** @param bestLabel Mejor día u hora. */
    public void setBestLabel(String bestLabel) {
        this.bestLabel = bestLabel;
    }

    /** @return Total del mejor día u hora. */
    public Total getBestTotal() {
        return bestTotal;
    }

    /** @param bestTotal Total del mejor día u hora. */
    public void setBestTotal(Total bestTotal) {
        this.bestTotal = bestTotal;
    }

    /** @return Puntos de la gráfica. */
    public List<Point> getSeries() {
        return series;
    }

    /** @param series Puntos de la gráfica. */
    public void setSeries(List<Point> series) {
        this.series = series;
    }

    /** @return Vehículos por ID de categoría. */
    public Map<Integer, Long> getVehiclesByCategory() {
        return vehiclesByCategory;
    }

    /** @param vehiclesByCategory Vehículos por ID de categoría. */
    public void setVehiclesByCategory(Map<Integer, Long> vehiclesByCategory) {
        this.vehiclesByCategory = vehiclesByCategory;
    }

    /** @return Recaudo por medio de pago (EFECTIVO, TARJETA, TAG_COLPASS). */
    public Map<String, BigDecimal> getRevenueByMethod() {
        return revenueByMethod;
    }

    /** @param revenueByMethod Recaudo por medio de pago. */
    public void setRevenueByMethod(Map<String, BigDecimal> revenueByMethod) {
        this.revenueByMethod = revenueByMethod;
    }

    /** @return Filas de la tabla "Todos los periodos". */
    public List<PeriodRow> getPeriods() {
        return periods;
    }

    /** @param periods Filas de la tabla de periodos. */
    public void setPeriods(List<PeriodRow> periods) {
        this.periods = periods;
    }

    /** @return Asistencias atendidas en el periodo. */
    public int getAttended() {
        return attended;
    }

    /** @param attended Asistencias atendidas. */
    public void setAttended(int attended) {
        this.attended = attended;
    }

    /** @return Asistencias canceladas en el periodo. */
    public int getCancelled() {
        return cancelled;
    }

    /** @param cancelled Asistencias canceladas. */
    public void setCancelled(int cancelled) {
        this.cancelled = cancelled;
    }

    /** @return Minutos promedio desde la solicitud hasta que el recurso llega, o null. */
    public Double getAverageArrivalMinutes() {
        return averageArrivalMinutes;
    }

    /** @param averageArrivalMinutes Minutos promedio de llegada. */
    public void setAverageArrivalMinutes(Double averageArrivalMinutes) {
        this.averageArrivalMinutes = averageArrivalMinutes;
    }

    /** @return Minutos promedio de llegada del periodo anterior, o null. */
    public Double getPreviousAverageArrivalMinutes() {
        return previousAverageArrivalMinutes;
    }

    /** @param previousAverageArrivalMinutes Minutos promedio de llegada del periodo anterior. */
    public void setPreviousAverageArrivalMinutes(Double previousAverageArrivalMinutes) {
        this.previousAverageArrivalMinutes = previousAverageArrivalMinutes;
    }

    /** @return Asistencias atendidas por tipo. */
    public Map<AssistanceType, Integer> getAttendedByType() {
        return attendedByType;
    }

    /** @param attendedByType Asistencias atendidas por tipo. */
    public void setAttendedByType(Map<AssistanceType, Integer> attendedByType) {
        this.attendedByType = attendedByType;
    }
}
