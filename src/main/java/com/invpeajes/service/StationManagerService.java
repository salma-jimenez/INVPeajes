package com.invpeajes.service;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;
import com.invpeajes.model.StatPeriod;
import com.invpeajes.model.Toll;
import com.invpeajes.model.TollStatistics;
import com.invpeajes.model.TollStatistics.Point;
import com.invpeajes.model.TollStatistics.Total;
import com.invpeajes.persistence.StatisticsDAO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reglas de la sesión del jefe de estación: tablero de asistencias de su peaje, recursos de
 * asistencia y estadísticas del peaje por día, semana, mes, semestre y año.
 *
 * Los cambios de estado de las asistencias y de los recursos los hacen {@link AssistanceService} y
 * {@link AssistanceResourceService}; este servicio solo los reúne para las pantallas del jefe.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StationManagerService {

    private static final String[] WEEK_DAYS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StatisticsDAO statisticsDAO;
    private final AssistanceService assistanceService;
    private final AssistanceResourceService resourceService;
    private final TollService tollService;

    /** Resumen que ve el jefe al iniciar sesión. */
    public static final class Summary {
        private final int unattended;
        private final int availableResources;
        private final int totalResources;
        private final BigDecimal revenueToday;

        /**
         * @param unattended Solicitudes sin atender (registradas).
         * @param availableResources Recursos disponibles.
         * @param totalResources Recursos del peaje.
         * @param revenueToday Recaudo de hoy.
         */
        public Summary(int unattended, int availableResources, int totalResources, BigDecimal revenueToday) {
            this.unattended = unattended;
            this.availableResources = availableResources;
            this.totalResources = totalResources;
            this.revenueToday = revenueToday;
        }

        /** @return Solicitudes sin atender. */
        public int getUnattended() {
            return unattended;
        }

        /** @return Recursos disponibles. */
        public int getAvailableResources() {
            return availableResources;
        }

        /** @return Recursos del peaje. */
        public int getTotalResources() {
            return totalResources;
        }

        /** @return Recaudo de hoy. */
        public BigDecimal getRevenueToday() {
            return revenueToday;
        }
    }

    /** Crea el servicio con las implementaciones de la base de datos. */
    public StationManagerService() {
        this(new StatisticsDAO(), new AssistanceService(), new AssistanceResourceService(), new TollService());
    }

    /**
     * Crea el servicio con dependencias específicas (permite usar datos falsos en pruebas).
     * @param statisticsDAO DAO de estadísticas.
     * @param assistanceService Servicio de asistencias.
     * @param resourceService Servicio de recursos de asistencia.
     * @param tollService Servicio de peajes.
     */
    public StationManagerService(StatisticsDAO statisticsDAO, AssistanceService assistanceService,
                                 AssistanceResourceService resourceService, TollService tollService) {
        this.statisticsDAO = statisticsDAO;
        this.assistanceService = assistanceService;
        this.resourceService = resourceService;
        this.tollService = tollService;
    }

    /** @return Servicio de asistencias (asignar, cambiar estado, cancelar). */
    public AssistanceService assistances() {
        return assistanceService;
    }

    /** @return Servicio de recursos (registrar, sacar de servicio, reactivar). */
    public AssistanceResourceService resources() {
        return resourceService;
    }

    /**
     * Datos del peaje del jefe (para la ubicación y la distancia de las solicitudes).
     * @param tollId ID del peaje.
     * @return El peaje, o null si no se encuentra.
     */
    public Toll toll(int tollId) {
        try {
            return tollService.findToll(tollId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Resumen para el saludo del jefe al iniciar sesión.
     * @param tollId ID del peaje.
     * @return Solicitudes sin atender, recursos disponibles y recaudo de hoy.
     */
    public Summary summary(int tollId) {
        int unattended = (int) assistanceService.listByToll(tollId).stream()
                .filter(a -> a.getCurrentStatus() == AssistanceStatus.REGISTERED).count();
        List<AssistanceResource> resources = resourceService.listByToll(tollId);
        int available = (int) resources.stream()
                .filter(r -> r.getAvailability() == ResourceAvailability.AVAILABLE).count();
        LocalDate today = LocalDate.now();
        BigDecimal revenue = statisticsDAO.total(tollId, today, today.plusDays(1)).getRevenue();
        return new Summary(unattended, available, resources.size(), revenue);
    }

    /**
     * Hora de cierre de las asistencias atendidas o canceladas hoy (para la columna "Atendidas hoy").
     * @param tollId ID del peaje.
     * @return Fecha y hora de cierre por ID de asistencia.
     */
    public Map<Integer, LocalDateTime> closedToday(int tollId) {
        return statisticsDAO.closedSince(tollId, LocalDate.now().atStartOfDay());
    }

    /**
     * Cuántas asistencias atendió hoy cada recurso.
     * @param tollId ID del peaje.
     * @return Cantidad por ID de recurso.
     */
    public Map<Integer, Integer> attendedTodayByResource(int tollId) {
        return statisticsDAO.attendedByResource(tollId, LocalDate.now().atStartOfDay());
    }

    /**
     * Distancia en línea recta entre dos puntos (fórmula de Haversine).
     * @param lat1 Latitud del primer punto.
     * @param lon1 Longitud del primer punto.
     * @param lat2 Latitud del segundo punto.
     * @param lon2 Longitud del segundo punto.
     * @return Distancia en kilómetros.
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Enlace de Google Maps a unas coordenadas.
     * @param latitude Latitud.
     * @param longitude Longitud.
     * @return URL que abre el punto en el navegador.
     */
    public static String mapsUrl(double latitude, double longitude) {
        return String.format(Locale.ROOT, "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
                latitude, longitude);
    }

    /**
     * Arma las estadísticas de un peaje en un periodo, comparadas con el periodo anterior.
     * @param tollId ID del peaje.
     * @param period Tipo de periodo.
     * @param start Inicio del periodo (de {@link StatPeriod#recentStarts}).
     * @return Estadísticas listas para mostrar.
     */
    public TollStatistics statistics(int tollId, StatPeriod period, LocalDate start) {
        LocalDate today = LocalDate.now();
        LocalDate end = period.end(start);
        LocalDate prevStart = period.previousStart(start);

        TollStatistics stats = new TollStatistics();
        stats.setPeriod(period);
        stats.setStart(start);
        LocalDate lastDay = end.isAfter(today.plusDays(1)) ? today.plusDays(1) : end;
        LocalDate prevEnd = comparableEnd(prevStart, start, start, lastDay);
        stats.setTotal(statisticsDAO.total(tollId, start, end));
        stats.setPreviousTotal(statisticsDAO.total(tollId, prevStart, prevEnd));
        stats.setDays((int) Math.max(1, ChronoUnit.DAYS.between(start, lastDay)));
        stats.setPartial(lastDay.isBefore(end));

        if (period.grouping() == StatPeriod.Grouping.HOUR) {
            Map<Integer, Total> yesterday = statisticsDAO.hourlyTotals(tollId, prevStart, start);
            if (stats.isPartial()) {
                int hour = java.time.LocalTime.now().getHour();
                stats.setPreviousTotal(yesterday.entrySet().stream().filter(e -> e.getKey() <= hour)
                        .map(Map.Entry::getValue).reduce(new Total(0, BigDecimal.ZERO), Total::plus));
            }
            buildHourly(stats, statisticsDAO.hourlyTotals(tollId, start, end), yesterday);
        } else {
            Map<LocalDate, Total> current = statisticsDAO.dailyTotals(tollId, start, end);
            Map<LocalDate, Total> previous = statisticsDAO.dailyTotals(tollId, prevStart, start);
            if (period.grouping() == StatPeriod.Grouping.DAY) {
                buildDaily(stats, period, start, end, prevStart, current, previous);
            } else {
                buildMonthly(stats, start, end, prevStart, current, previous);
            }
            current.entrySet().stream()
                    .max((a, b) -> a.getValue().getRevenue().compareTo(b.getValue().getRevenue()))
                    .ifPresent(best -> {
                        LocalDate d = best.getKey();
                        stats.setBestLabel(WEEK_DAYS[d.getDayOfWeek().getValue() - 1] + " "
                                + d.getDayOfMonth() + "/" + d.getMonthValue());
                        stats.setBestTotal(best.getValue());
                    });
        }

        stats.setVehiclesByCategory(statisticsDAO.vehiclesByCategory(tollId, start, end));
        stats.setRevenueByMethod(statisticsDAO.revenueByMethod(tollId, start, end));
        stats.setPeriods(periodRows(tollId, period, start, today));

        Map<AssistanceType, Integer> byType = statisticsDAO.attendedByType(tollId, start, end);
        stats.setAttendedByType(byType);
        stats.setAttended(byType.values().stream().mapToInt(Integer::intValue).sum());
        stats.setCancelled(statisticsDAO.cancelledAssistances(tollId, start, end));
        stats.setAverageArrivalMinutes(statisticsDAO.averageArrivalMinutes(tollId, start, end));
        stats.setPreviousAverageArrivalMinutes(statisticsDAO.averageArrivalMinutes(tollId, prevStart, prevEnd));
        return stats;
    }

    /**
     * Fin del tramo del periodo anterior que se compara. Si el periodo actual todavía no termina
     * (ej. vamos en el día 10 del mes), se compara con los mismos días del periodo anterior
     * (días 1 a 10), para no comparar medio mes contra un mes completo.
     */
    private static LocalDate comparableEnd(LocalDate prevStart, LocalDate prevLimit, LocalDate start, LocalDate lastDay) {
        LocalDate end = prevStart.plusDays(ChronoUnit.DAYS.between(start, lastDay));
        return end.isAfter(prevLimit) ? prevLimit : end;
    }

    private static void buildHourly(TollStatistics stats, Map<Integer, Total> current, Map<Integer, Total> previous) {
        List<Point> points = new ArrayList<>();
        Total zero = new Total(0, BigDecimal.ZERO);
        for (int h = 0; h < 24; h++) {
            points.add(new Point(h + "h", current.getOrDefault(h, zero).getRevenue(),
                    previous.getOrDefault(h, zero).getRevenue()));
        }
        stats.setSeries(points);
        current.entrySet().stream()
                .max((a, b) -> Long.compare(a.getValue().getVehicles(), b.getValue().getVehicles()))
                .ifPresent(best -> {
                    stats.setBestLabel(best.getKey() + ":00 – " + (best.getKey() + 1) + ":00");
                    stats.setBestTotal(best.getValue());
                });
    }

    private static void buildDaily(TollStatistics stats, StatPeriod period, LocalDate start, LocalDate end,
                                   LocalDate prevStart, Map<LocalDate, Total> current,
                                   Map<LocalDate, Total> previous) {
        List<Point> points = new ArrayList<>();
        Total zero = new Total(0, BigDecimal.ZERO);
        int days = (int) ChronoUnit.DAYS.between(start, end);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            String label = period == StatPeriod.WEEK
                    ? WEEK_DAYS[day.getDayOfWeek().getValue() - 1] : String.valueOf(day.getDayOfMonth());
            BigDecimal value = day.isAfter(today) ? null : current.getOrDefault(day, zero).getRevenue();
            points.add(new Point(label, value, previous.getOrDefault(prevStart.plusDays(i), zero).getRevenue()));
        }
        stats.setSeries(points);
    }

    private static void buildMonthly(TollStatistics stats, LocalDate start, LocalDate end, LocalDate prevStart,
                                     Map<LocalDate, Total> current, Map<LocalDate, Total> previous) {
        List<Point> points = new ArrayList<>();
        int months = (int) ChronoUnit.MONTHS.between(start, end);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < months; i++) {
            LocalDate month = start.plusMonths(i);
            LocalDate prevMonth = prevStart.plusMonths(i);
            BigDecimal value = month.isAfter(today) ? null : sumMonth(current, month);
            points.add(new Point(StatPeriod.monthName(month.getMonthValue()).substring(0, 3),
                    value, sumMonth(previous, prevMonth)));
        }
        stats.setSeries(points);
    }

    private static BigDecimal sumMonth(Map<LocalDate, Total> daily, LocalDate month) {
        return daily.entrySet().stream()
                .filter(e -> e.getKey().getYear() == month.getYear() && e.getKey().getMonth() == month.getMonth())
                .map(e -> e.getValue().getRevenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<TollStatistics.PeriodRow> periodRows(int tollId, StatPeriod selected, LocalDate selectedStart,
                                                      LocalDate today) {
        List<TollStatistics.PeriodRow> rows = new ArrayList<>();
        Map<StatPeriod, String> names = new EnumMap<>(StatPeriod.class);
        names.put(StatPeriod.DAY, "Hoy");
        names.put(StatPeriod.WEEK, "Esta semana");
        for (StatPeriod p : StatPeriod.values()) {
            LocalDate s = p.start(today);
            LocalDate last = today.plusDays(1).isBefore(p.end(s)) ? today.plusDays(1) : p.end(s);
            Total current = statisticsDAO.total(tollId, s, p.end(s));
            Total previous = statisticsDAO.total(tollId, p.previousStart(s),
                    comparableEnd(p.previousStart(s), s, s, last));
            String label = names.getOrDefault(p, p.describe(s));
            rows.add(new TollStatistics.PeriodRow(label, current,
                    TollStatistics.variation(current.getRevenue(), previous.getRevenue()),
                    p == selected && s.equals(selectedStart)));
        }
        return rows;
    }

    /**
     * Asistencias activas del peaje (registradas, asignadas, en camino o en proceso).
     * @param all Asistencias del peaje.
     * @return Solo las que no están cerradas.
     */
    public static List<Assistance> active(List<Assistance> all) {
        List<Assistance> list = new ArrayList<>();
        for (Assistance a : all) {
            if (a.getCurrentStatus() != AssistanceStatus.ATTENDED && a.getCurrentStatus() != AssistanceStatus.CANCELLED) {
                list.add(a);
            }
        }
        return list;
    }
}
