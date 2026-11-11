package com.invpeajes.persistence;

import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.TollStatistics.Total;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consultas de solo lectura para las estadísticas del jefe de estación: recaudo y vehículos
 * (tabla TIQUETE, unida con CASETAS para saber el peaje) y asistencias (ASISTENCIA e
 * HISTORIAL_ASISTENCIA).
 *
 * No implementa {@link CrudDAO} porque no guarda ni modifica datos: solo agrupa y cuenta.
 * Todos los rangos son {@code [desde, hasta)}: incluyen el primer día y excluyen el último.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StatisticsDAO {

    private static final String TICKETS_OF_TOLL =
            " FROM PEAJE.TIQUETE t JOIN PEAJE.CASETAS c ON c.ID_CASETA = t.ID_CASETA "
          + "WHERE c.ID_PEAJE = ? AND t.FECHA_PAGO >= ? AND t.FECHA_PAGO < ? ";

    private static final String SQL_TOTAL =
            "SELECT COUNT(*), NVL(SUM(t.VALOR_PAGO), 0)" + TICKETS_OF_TOLL;
    private static final String SQL_DAILY =
            "SELECT TRUNC(t.FECHA_PAGO) DIA, COUNT(*), NVL(SUM(t.VALOR_PAGO), 0)" + TICKETS_OF_TOLL
          + "GROUP BY TRUNC(t.FECHA_PAGO)";
    private static final String SQL_HOURLY =
            "SELECT NVL(TO_NUMBER(SUBSTR(t.HORA_PAGO, 1, 2)), 0) HORA, COUNT(*), NVL(SUM(t.VALOR_PAGO), 0)"
          + TICKETS_OF_TOLL + "GROUP BY NVL(TO_NUMBER(SUBSTR(t.HORA_PAGO, 1, 2)), 0)";
    private static final String SQL_BY_METHOD =
            "SELECT t.METODO_PAGO, NVL(SUM(t.VALOR_PAGO), 0)" + TICKETS_OF_TOLL + "GROUP BY t.METODO_PAGO";
    private static final String SQL_BY_CATEGORY =
            "SELECT t.ID_CATEGORIA, COUNT(*)" + TICKETS_OF_TOLL + "GROUP BY t.ID_CATEGORIA ORDER BY t.ID_CATEGORIA";

    private static final String ASSISTANCE_OF_TOLL =
            " FROM PEAJE.ASISTENCIA a WHERE a.ID_PEAJE = ? AND a.FECHA_SOLICITUD >= ? AND a.FECHA_SOLICITUD < ? ";
    private static final String SQL_ATTENDED_BY_TYPE =
            "SELECT a.TIPO_ASISTENCIA, COUNT(*)" + ASSISTANCE_OF_TOLL
          + "AND a.ESTADO_ACTUAL = 'ATENDIDA' GROUP BY a.TIPO_ASISTENCIA";
    private static final String SQL_CANCELLED =
            "SELECT COUNT(*)" + ASSISTANCE_OF_TOLL + "AND a.ESTADO_ACTUAL = 'CANCELADA'";
    private static final String SQL_ARRIVAL =
            "SELECT AVG((CAST(h.FECHA_CAMBIO AS DATE) - CAST(a.FECHA_SOLICITUD AS DATE)) * 1440) "
          + "FROM PEAJE.ASISTENCIA a JOIN PEAJE.HISTORIAL_ASISTENCIA h "
          + "ON h.ID_ASISTENCIA = a.ID_ASISTENCIA AND h.ESTADO_NUEVO = 'EN_PROCESO' "
          + "WHERE a.ID_PEAJE = ? AND a.FECHA_SOLICITUD >= ? AND a.FECHA_SOLICITUD < ?";
    private static final String SQL_CLOSED_SINCE =
            "SELECT h.ID_ASISTENCIA, MAX(h.FECHA_CAMBIO) FROM PEAJE.HISTORIAL_ASISTENCIA h "
          + "JOIN PEAJE.ASISTENCIA a ON a.ID_ASISTENCIA = h.ID_ASISTENCIA "
          + "WHERE a.ID_PEAJE = ? AND h.ESTADO_NUEVO IN ('ATENDIDA', 'CANCELADA') AND h.FECHA_CAMBIO >= ? "
          + "GROUP BY h.ID_ASISTENCIA";
    private static final String SQL_ATTENDED_BY_RESOURCE =
            "SELECT a.ID_RECURSO, COUNT(*) FROM PEAJE.ASISTENCIA a JOIN PEAJE.HISTORIAL_ASISTENCIA h "
          + "ON h.ID_ASISTENCIA = a.ID_ASISTENCIA AND h.ESTADO_NUEVO = 'ATENDIDA' "
          + "WHERE a.ID_PEAJE = ? AND h.FECHA_CAMBIO >= ? AND a.ID_RECURSO IS NOT NULL GROUP BY a.ID_RECURSO";

    /**
     * Total de vehículos y recaudo de un peaje en un rango de fechas.
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Total del rango.
     * @throws DataAccessException Si falla la consulta.
     */
    public Total total(int tollId, LocalDate from, LocalDate to) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareDates(conn, SQL_TOTAL, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? new Total(rs.getLong(1), rs.getBigDecimal(2)) : new Total(0, BigDecimal.ZERO);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el recaudo del peaje " + tollId, e);
        }
    }

    /**
     * Vehículos y recaudo de cada día del rango (solo los días con cobros).
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Totales por día, ordenados por fecha.
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<LocalDate, Total> dailyTotals(int tollId, LocalDate from, LocalDate to) {
        Map<LocalDate, Total> result = new TreeMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareDates(conn, SQL_DAILY, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getDate(1).toLocalDate(), new Total(rs.getLong(2), rs.getBigDecimal(3)));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el recaudo diario del peaje " + tollId, e);
        }
    }

    /**
     * Vehículos y recaudo por hora (0 a 23) de un rango, según HORA_PAGO.
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Totales por hora.
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<Integer, Total> hourlyTotals(int tollId, LocalDate from, LocalDate to) {
        Map<Integer, Total> result = new TreeMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareDates(conn, SQL_HOURLY, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getInt(1), new Total(rs.getLong(2), rs.getBigDecimal(3)));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el recaudo por hora del peaje " + tollId, e);
        }
    }

    /**
     * Recaudo por medio de pago.
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Recaudo por EFECTIVO, TARJETA y TAG_COLPASS (solo los que tienen cobros).
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<String, BigDecimal> revenueByMethod(int tollId, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareDates(conn, SQL_BY_METHOD, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getBigDecimal(2));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el recaudo por medio de pago", e);
        }
    }

    /**
     * Vehículos por categoría.
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Cantidad de cobros por ID de categoría.
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<Integer, Long> vehiclesByCategory(int tollId, LocalDate from, LocalDate to) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareDates(conn, SQL_BY_CATEGORY, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getInt(1), rs.getLong(2));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar los vehículos por categoría", e);
        }
    }

    /**
     * Asistencias atendidas por tipo, de las solicitudes hechas en el rango.
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Cantidad por tipo de asistencia.
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<AssistanceType, Integer> attendedByType(int tollId, LocalDate from, LocalDate to) {
        Map<AssistanceType, Integer> result = new EnumMap<>(AssistanceType.class);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareTimestamps(conn, SQL_ATTENDED_BY_TYPE, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(AssistanceType.fromDbValue(rs.getString(1)), rs.getInt(2));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar las asistencias atendidas", e);
        }
    }

    /**
     * Asistencias canceladas de las solicitudes hechas en el rango.
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Cantidad de canceladas.
     * @throws DataAccessException Si falla la consulta.
     */
    public int cancelledAssistances(int tollId, LocalDate from, LocalDate to) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareTimestamps(conn, SQL_CANCELLED, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar las asistencias canceladas", e);
        }
    }

    /**
     * Minutos promedio desde que el viajero pide la asistencia hasta que el recurso llega
     * (cambio a EN_PROCESO).
     * @param tollId ID del peaje.
     * @param from Primer día (incluido).
     * @param to Día final (excluido).
     * @return Promedio en minutos, o null si no hubo asistencias que llegaran.
     * @throws DataAccessException Si falla la consulta.
     */
    public Double averageArrivalMinutes(int tollId, LocalDate from, LocalDate to) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepareTimestamps(conn, SQL_ARRIVAL, tollId, from, to);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            double value = rs.getDouble(1);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el tiempo de llegada", e);
        }
    }

    /**
     * Hora en que se cerró (atendida o cancelada) cada asistencia del peaje desde un momento dado.
     * @param tollId ID del peaje.
     * @param since Momento desde el que se buscan los cierres.
     * @return Fecha y hora de cierre por ID de asistencia.
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<Integer, LocalDateTime> closedSince(int tollId, LocalDateTime since) {
        Map<Integer, LocalDateTime> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CLOSED_SINCE)) {
            ps.setInt(1, tollId);
            ps.setTimestamp(2, Timestamp.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt(1), rs.getTimestamp(2).toLocalDateTime());
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar las asistencias cerradas", e);
        }
    }

    /**
     * Cuántas asistencias atendió cada recurso desde un momento dado.
     * @param tollId ID del peaje.
     * @param since Momento desde el que se cuentan.
     * @return Cantidad atendida por ID de recurso.
     * @throws DataAccessException Si falla la consulta.
     */
    public Map<Integer, Integer> attendedByResource(int tollId, LocalDateTime since) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ATTENDED_BY_RESOURCE)) {
            ps.setInt(1, tollId);
            ps.setTimestamp(2, Timestamp.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt(1), rs.getInt(2));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar las asistencias por recurso", e);
        }
    }

    /** Prepara una consulta de tiquetes (FECHA_PAGO es DATE). */
    private static PreparedStatement prepareDates(Connection conn, String sql, int tollId,
                                                  LocalDate from, LocalDate to) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, tollId);
        ps.setDate(2, Date.valueOf(from));
        ps.setDate(3, Date.valueOf(to));
        return ps;
    }

    /** Prepara una consulta de asistencias (FECHA_SOLICITUD guarda fecha y hora). */
    private static PreparedStatement prepareTimestamps(Connection conn, String sql, int tollId,
                                                       LocalDate from, LocalDate to) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, tollId);
        ps.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
        ps.setTimestamp(3, Timestamp.valueOf(to.atStartOfDay()));
        return ps;
    }
}
