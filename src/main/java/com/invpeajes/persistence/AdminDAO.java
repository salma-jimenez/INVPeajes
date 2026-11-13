package com.invpeajes.persistence;

import com.invpeajes.model.AdminSummary;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consultas del panel de Administración sobre los peajes de una entidad (PEAJE.NIVEL): recaudo,
 * asistencias activas, pendientes y lo que impide borrar un peaje o una caseta.
 *
 * <p>Las consultas de un mismo resumen usan una sola conexión, para que la pantalla cargue
 * rápido aunque la base esté en la nube.</p>
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AdminDAO {

    private static final String TICKETS_OF_LEVEL =
            " FROM PEAJE.TIQUETE t JOIN PEAJE.CASETAS c ON c.ID_CASETA = t.ID_CASETA "
          + "JOIN PEAJE.PEAJE p ON p.ID_PEAJE = c.ID_PEAJE WHERE p.NIVEL = ? ";

    private static final String SQL_PERIODS =
            "SELECT SUM(CASE WHEN t.FECHA_PAGO >= ? THEN 1 ELSE 0 END), "
          + "NVL(SUM(CASE WHEN t.FECHA_PAGO >= ? THEN t.VALOR_PAGO END), 0), "
          + "SUM(CASE WHEN t.FECHA_PAGO >= ? AND t.FECHA_PAGO < ? THEN 1 ELSE 0 END), "
          + "NVL(SUM(CASE WHEN t.FECHA_PAGO >= ? AND t.FECHA_PAGO < ? THEN t.VALOR_PAGO END), 0), "
          + "SUM(CASE WHEN t.FECHA_PAGO >= ? THEN 1 ELSE 0 END), "
          + "NVL(SUM(CASE WHEN t.FECHA_PAGO >= ? THEN t.VALOR_PAGO END), 0), "
          + "SUM(CASE WHEN t.FECHA_PAGO >= ? AND t.FECHA_PAGO < ? THEN 1 ELSE 0 END), "
          + "NVL(SUM(CASE WHEN t.FECHA_PAGO >= ? AND t.FECHA_PAGO < ? THEN t.VALOR_PAGO END), 0)"
          + TICKETS_OF_LEVEL + "AND t.FECHA_PAGO >= ?";

    private static final String SQL_BY_MONTH =
            "SELECT EXTRACT(MONTH FROM t.FECHA_PAGO), NVL(SUM(t.VALOR_PAGO), 0)" + TICKETS_OF_LEVEL
          + "AND t.FECHA_PAGO >= ? AND t.FECHA_PAGO < ? GROUP BY EXTRACT(MONTH FROM t.FECHA_PAGO)";

    private static final String SQL_TOP_TOLLS =
            "SELECT p.NOMBRE, NVL(SUM(t.VALOR_PAGO), 0)" + TICKETS_OF_LEVEL
          + "AND t.FECHA_PAGO >= ? GROUP BY p.ID_PEAJE, p.NOMBRE ORDER BY 2 DESC FETCH FIRST 5 ROWS ONLY";

    private static final String SQL_ASSISTANCES =
            "SELECT COUNT(*), NVL(SUM(CASE WHEN a.ESTADO_ACTUAL = 'REGISTRADA' THEN 1 ELSE 0 END), 0) "
          + "FROM PEAJE.ASISTENCIA a JOIN PEAJE.PEAJE p ON p.ID_PEAJE = a.ID_PEAJE "
          + "WHERE p.NIVEL = ? AND a.ESTADO_ACTUAL NOT IN ('ATENDIDA', 'CANCELADA')";

    private static final String SQL_NO_MANAGER =
            "SELECT p.NOMBRE FROM PEAJE.PEAJE p WHERE p.NIVEL = ? AND p.ESTADO_OPERATIVO <> 'NO_OPERATIVO' "
          + "AND NOT EXISTS (SELECT 1 FROM PEAJE.USUARIO_PEAJE u WHERE u.ID_PEAJE = p.ID_PEAJE "
          + "AND u.CARGO = 'JEFE DE ESTACION' AND u.ACTIVO = 'S') ORDER BY p.NOMBRE";

    private static final String SQL_NO_RATES =
            "SELECT p.NOMBRE FROM PEAJE.PEAJE p WHERE p.NIVEL = ? AND p.ESTADO_OPERATIVO <> 'NO_OPERATIVO' "
          + "AND (p.ESTADO_OPERATIVO = 'SIN_TARIFA' OR NOT EXISTS "
          + "(SELECT 1 FROM PEAJE.PEAJE_CATEGORIA pc WHERE pc.ID_PEAJE = p.ID_PEAJE)) ORDER BY p.NOMBRE";

    private static final String SQL_OLD_RATES =
            "SELECT COUNT(*) FROM (SELECT pc.ID_PEAJE FROM PEAJE.PEAJE_CATEGORIA pc "
          + "JOIN PEAJE.PEAJE p ON p.ID_PEAJE = pc.ID_PEAJE WHERE p.NIVEL = ? "
          + "GROUP BY pc.ID_PEAJE HAVING MAX(pc.VIGENTE_DESDE) < ?)";

    private static final String SQL_TICKETS_OF_TOLL =
            "SELECT COUNT(*) FROM PEAJE.TIQUETE t JOIN PEAJE.CASETAS c ON c.ID_CASETA = t.ID_CASETA "
          + "WHERE c.ID_PEAJE = ?";

    private static final String SQL_TICKETS_OF_BOOTH =
            "SELECT COUNT(*) FROM PEAJE.TIQUETE WHERE ID_CASETA = ?";

    private static final String SQL_USERS_OF_TOLL =
            "SELECT COUNT(*) FROM PEAJE.USUARIO_PEAJE WHERE ID_PEAJE = ?";

    private static final String SQL_NEXT_TOLL_ID =
            "SELECT NVL(MAX(ID_PEAJE), 0) + 1 FROM PEAJE.PEAJE";

    private static final String[] SQL_DELETE_TOLL = {
        "DELETE FROM PEAJE.PEAJE_CATEGORIA WHERE ID_PEAJE = ?",
        "DELETE FROM PEAJE.CASETAS WHERE ID_PEAJE = ?",
        "DELETE FROM PEAJE.PEAJE WHERE ID_PEAJE = ?",
    };

    /** Peajes que necesitan atención: sin jefe, sin tarifas y con tarifas de años anteriores. */
    public static final class PendingData {
        private final List<String> withoutManager;
        private final List<String> withoutRates;
        private final int withOldRates;

        /**
         * @param withoutManager Nombres de los peajes sin jefe de estación activo.
         * @param withoutRates Nombres de los peajes sin tarifas.
         * @param withOldRates Cuántos peajes tienen tarifas de un año anterior.
         */
        public PendingData(List<String> withoutManager, List<String> withoutRates, int withOldRates) {
            this.withoutManager = withoutManager;
            this.withoutRates = withoutRates;
            this.withOldRates = withOldRates;
        }

        /** @return Peajes sin jefe de estación activo. */
        public List<String> getWithoutManager() {
            return withoutManager;
        }

        /** @return Peajes sin tarifas. */
        public List<String> getWithoutRates() {
            return withoutRates;
        }

        /** @return Peajes con tarifas de un año anterior. */
        public int getWithOldRates() {
            return withOldRates;
        }
    }

    /**
     * Recaudo, recaudo por mes, peajes con más recaudo y asistencias activas de una entidad.
     * Los pendientes y los peajes operativos los completa el servicio.
     * @param level Entidad (PEAJE.NIVEL).
     * @param now Fecha y hora actual.
     * @return Resumen con los datos de la base.
     * @throws DataAccessException Si falla una consulta.
     */
    public AdminSummary summary(String level, LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime month = today.withDayOfMonth(1);
        LocalDateTime previousMonth = month.minusMonths(1);
        LocalDateTime previousMonthEnd = previousMonth.plus(java.time.Duration.between(month, now));
        if (previousMonthEnd.isAfter(month)) {
            previousMonthEnd = month;
        }
        LocalDateTime year = today.withDayOfYear(1);

        AdminSummary summary = new AdminSummary();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQL_PERIODS)) {
                Object[] params = {today, today, yesterday, now.minusDays(1), yesterday, now.minusDays(1),
                    month, month, previousMonth, previousMonthEnd, previousMonth, previousMonthEnd};
                int i = 1;
                for (Object p : params) {
                    ps.setTimestamp(i++, Timestamp.valueOf((LocalDateTime) p));
                }
                ps.setString(i++, level);
                ps.setTimestamp(i, Timestamp.valueOf(previousMonth.isBefore(yesterday) ? previousMonth : yesterday));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setToday(new Total(rs.getLong(1), rs.getBigDecimal(2)));
                        summary.setYesterdaySameTime(new Total(rs.getLong(3), rs.getBigDecimal(4)));
                        summary.setMonth(new Total(rs.getLong(5), rs.getBigDecimal(6)));
                        summary.setPreviousMonthSameDays(new Total(rs.getLong(7), rs.getBigDecimal(8)));
                    }
                }
            }
            Map<Integer, BigDecimal> byMonth = new TreeMap<>();
            for (int m = 1; m <= now.getMonthValue(); m++) {
                byMonth.put(m, BigDecimal.ZERO);
            }
            try (PreparedStatement ps = conn.prepareStatement(SQL_BY_MONTH)) {
                ps.setString(1, level);
                ps.setTimestamp(2, Timestamp.valueOf(year));
                ps.setTimestamp(3, Timestamp.valueOf(year.plusYears(1)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        byMonth.put(rs.getInt(1), rs.getBigDecimal(2));
                    }
                }
            }
            summary.setRevenueByMonth(byMonth);
            Map<String, BigDecimal> top = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(SQL_TOP_TOLLS)) {
                ps.setString(1, level);
                ps.setTimestamp(2, Timestamp.valueOf(month));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        top.put(rs.getString(1), rs.getBigDecimal(2));
                    }
                }
            }
            summary.setTopTolls(top);
            try (PreparedStatement ps = conn.prepareStatement(SQL_ASSISTANCES)) {
                ps.setString(1, level);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setActiveAssistances(rs.getInt(1));
                        summary.setUnattendedAssistances(rs.getInt(2));
                    }
                }
            }
            return summary;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cargar el resumen de " + level, e);
        }
    }

    /**
     * Peajes de la entidad sin jefe de estación, sin tarifas o con tarifas de años anteriores.
     * @param level Entidad (PEAJE.NIVEL).
     * @param yearStart 1 de enero del año actual.
     * @return Datos de los pendientes.
     * @throws DataAccessException Si falla una consulta.
     */
    public PendingData pending(String level, LocalDate yearStart) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<String> noManager = names(conn, SQL_NO_MANAGER, level);
            List<String> noRates = names(conn, SQL_NO_RATES, level);
            int oldRates;
            try (PreparedStatement ps = conn.prepareStatement(SQL_OLD_RATES)) {
                ps.setString(1, level);
                ps.setDate(2, Date.valueOf(yearStart));
                try (ResultSet rs = ps.executeQuery()) {
                    oldRates = rs.next() ? rs.getInt(1) : 0;
                }
            }
            return new PendingData(noManager, noRates, oldRates);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron revisar los pendientes de " + level, e);
        }
    }

    /**
     * @param tollId ID del peaje.
     * @return Cuántos tiquetes se han cobrado en el peaje (en todas sus casetas).
     */
    public long ticketsOfToll(int tollId) {
        return count(SQL_TICKETS_OF_TOLL, tollId, "No se pudieron contar los tiquetes del peaje " + tollId);
    }

    /**
     * @param boothId ID de la caseta.
     * @return Cuántos tiquetes se han cobrado en la caseta.
     */
    public long ticketsOfBooth(int boothId) {
        return count(SQL_TICKETS_OF_BOOTH, boothId, "No se pudieron contar los tiquetes de la caseta " + boothId);
    }

    /**
     * @param tollId ID del peaje.
     * @return Cuántos empleados (activos o no) tiene asignados el peaje.
     */
    public long usersOfToll(int tollId) {
        return count(SQL_USERS_OF_TOLL, tollId, "No se pudieron contar los empleados del peaje " + tollId);
    }

    /**
     * @return El siguiente ID libre para un peaje nuevo (la tabla PEAJE no lo genera sola).
     */
    public int nextTollId() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_NEXT_TOLL_ID);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el ID del peaje nuevo", e);
        }
    }

    /**
     * Borra un peaje con sus tarifas y casetas, todo o nada. Solo sirve para peajes sin cobros ni
     * empleados (el servicio lo revisa antes).
     * @param tollId ID del peaje.
     * @return true si se borró.
     * @throws DataAccessException Si Oracle no deja borrarlo (ej. tiene asistencias).
     */
    public boolean deleteToll(int tollId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int deleted = 0;
                for (String sql : SQL_DELETE_TOLL) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, tollId);
                        deleted = ps.executeUpdate();
                    }
                }
                conn.commit();
                return deleted > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el peaje " + tollId, e);
        }
    }

    // ------------------------------------------------------------------ Apoyo

    private static List<String> names(Connection conn, String sql, String level) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        }
        return names;
    }

    private static long count(String sql, int id, String error) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException(error, e);
        }
    }
}
