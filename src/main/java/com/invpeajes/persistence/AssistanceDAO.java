package com.invpeajes.persistence;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de las tablas {@code PEAJE.ASISTENCIA} y
 * {@code PEAJE.HISTORIAL_ASISTENCIA}.
 *
 * <p>El historial se maneja aquí porque solo existe ligado a una asistencia:
 * toda operación que cambia el estado de una asistencia deja su registro de
 * historial y, si corresponde, actualiza la disponibilidad del recurso
 * asignado, todo dentro de la misma transacción.</p>
 *
 * @author Salomejimenez
 */
public class AssistanceDAO implements CrudDAO<Assistance, Integer> {

    private static final String COLUMNS =
            "ID_ASISTENCIA, TIPO_ASISTENCIA, FECHA_SOLICITUD, LATITUD_INCIDENTE, LONGITUD_INCIDENTE, "
            + "ESTADO_ACTUAL, OBSERVACIONES, TELEFONO_CONTACTO, COSTO, PLACA, ID_PEAJE, ID_RECURSO, ID_TIQUETE";
    private static final String NEXT_ID_SQL =
            "SELECT NVL(MAX(ID_ASISTENCIA), 0) + 1 FROM PEAJE.ASISTENCIA";
    private static final String INSERT_SQL =
            "INSERT INTO PEAJE.ASISTENCIA (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SQL =
            "SELECT " + COLUMNS + " FROM PEAJE.ASISTENCIA ";
    private static final String UPDATE_SQL =
            "UPDATE PEAJE.ASISTENCIA SET TIPO_ASISTENCIA = ?, LATITUD_INCIDENTE = ?, LONGITUD_INCIDENTE = ?, "
            + "OBSERVACIONES = ?, TELEFONO_CONTACTO = ?, COSTO = ?, PLACA = ?, ID_PEAJE = ?, ID_RECURSO = ?, "
            + "ID_TIQUETE = ? WHERE ID_ASISTENCIA = ?";
    private static final String CHANGE_STATUS_SQL =
            "UPDATE PEAJE.ASISTENCIA SET ESTADO_ACTUAL = ?, ID_RECURSO = ?, COSTO = ? WHERE ID_ASISTENCIA = ?";
    private static final String DELETE_SQL =
            "DELETE FROM PEAJE.ASISTENCIA WHERE ID_ASISTENCIA = ?";

    private static final String HISTORY_NEXT_ID_SQL =
            "SELECT NVL(MAX(ID_HISTORIAL), 0) + 1 FROM PEAJE.HISTORIAL_ASISTENCIA";
    private static final String HISTORY_INSERT_SQL =
            "INSERT INTO PEAJE.HISTORIAL_ASISTENCIA "
            + "(ID_HISTORIAL, ESTADO_ANTERIOR, ESTADO_NUEVO, FECHA_CAMBIO, OBSERVACIONES, ID_ASISTENCIA) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String HISTORY_FIND_SQL =
            "SELECT * FROM PEAJE.HISTORIAL_ASISTENCIA WHERE ID_ASISTENCIA = ? "
            + "ORDER BY FECHA_CAMBIO, ID_HISTORIAL";
    private static final String HISTORY_DELETE_SQL =
            "DELETE FROM PEAJE.HISTORIAL_ASISTENCIA WHERE ID_ASISTENCIA = ?";

    private final AssistanceResourceDAO resourceDAO;

    /** Crea el DAO con su DAO de recursos por defecto. */
    public AssistanceDAO() {
        this(new AssistanceResourceDAO());
    }

    /**
     * Crea el DAO con un DAO de recursos inyectado.
     *
     * @param resourceDAO DAO para actualizar la disponibilidad del recurso
     */
    public AssistanceDAO(AssistanceResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    /**
     * Inserta la asistencia y su registro inicial de historial en una sola
     * transacción. Asigna el id generado a la asistencia recibida.
     */
    @Override
    public void insert(Assistance assistance) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                assistance.setAssistanceId(nextId(con));
                insertRow(con, assistance);
                insertHistory(con, new AssistanceHistory(assistance.getAssistanceId(), null,
                        assistance.getCurrentStatus(), "Solicitud registrada"));
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar la asistencia", e);
        }
    }

    @Override
    public Optional<Assistance> findById(Integer id) {
        List<Assistance> result = query(SELECT_SQL + "WHERE ID_ASISTENCIA = ?", id,
                "Error al buscar la asistencia " + id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Assistance> findAll() {
        return query(SELECT_SQL + "ORDER BY FECHA_SOLICITUD DESC", null,
                "Error al listar las asistencias");
    }

    /**
     * Lista las asistencias que están en un estado dado.
     *
     * @param status estado a filtrar
     * @return asistencias en ese estado, más recientes primero
     */
    public List<Assistance> findByStatus(AssistanceStatus status) {
        return query(SELECT_SQL + "WHERE ESTADO_ACTUAL = ? ORDER BY FECHA_SOLICITUD DESC",
                status.getDbValue(), "Error al listar asistencias en estado " + status.getDbValue());
    }

    /**
     * Lista las asistencias prestadas a un vehículo.
     *
     * @param plate placa del vehículo
     * @return asistencias del vehículo, más recientes primero
     */
    public List<Assistance> findByPlate(String plate) {
        return query(SELECT_SQL + "WHERE PLACA = ? ORDER BY FECHA_SOLICITUD DESC",
                plate, "Error al listar asistencias de la placa " + plate);
    }

    /**
     * Lista las asistencias atendidas desde un peaje.
     *
     * @param tollId peaje a filtrar
     * @return asistencias del peaje, más recientes primero
     */
    public List<Assistance> findByToll(int tollId) {
        return query(SELECT_SQL + "WHERE ID_PEAJE = ? ORDER BY FECHA_SOLICITUD DESC",
                tollId, "Error al listar asistencias del peaje " + tollId);
    }

    /**
     * Actualiza los datos descriptivos de la asistencia. No cambia el estado:
     * para eso se usa {@link #changeStatus}.
     */
    @Override
    public boolean update(Assistance assistance) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, assistance.getType().getDbValue());
            JdbcUtils.setNullableDouble(ps, 2, assistance.getIncidentLatitude());
            JdbcUtils.setNullableDouble(ps, 3, assistance.getIncidentLongitude());
            ps.setString(4, assistance.getNotes());
            ps.setString(5, assistance.getContactPhone());
            ps.setBigDecimal(6, assistance.getCost());
            ps.setString(7, assistance.getPlate());
            ps.setInt(8, assistance.getTollId());
            JdbcUtils.setNullableInt(ps, 9, assistance.getResourceId());
            JdbcUtils.setNullableLong(ps, 10, assistance.getTicketId());
            ps.setInt(11, assistance.getAssistanceId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar la asistencia " + assistance.getAssistanceId(), e);
        }
    }

    /**
     * Guarda el nuevo estado de la asistencia, registra el cambio en el
     * historial y actualiza la disponibilidad del recurso asignado, todo en
     * una sola transacción.
     *
     * @param assistance           asistencia con el estado nuevo ya asignado
     * @param previousStatus       estado que tenía antes del cambio
     * @param notes                motivo del cambio
     * @param resourceAvailability nueva disponibilidad del recurso asignado,
     *                             o null si el recurso no cambia
     * @return true si se actualizó la asistencia
     */
    public boolean changeStatus(Assistance assistance, AssistanceStatus previousStatus, String notes,
                                ResourceAvailability resourceAvailability) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(CHANGE_STATUS_SQL)) {
                ps.setString(1, assistance.getCurrentStatus().getDbValue());
                JdbcUtils.setNullableInt(ps, 2, assistance.getResourceId());
                ps.setBigDecimal(3, assistance.getCost());
                ps.setInt(4, assistance.getAssistanceId());
                boolean updated = ps.executeUpdate() > 0;
                if (updated) {
                    insertHistory(con, new AssistanceHistory(assistance.getAssistanceId(),
                            previousStatus, assistance.getCurrentStatus(), notes));
                    if (resourceAvailability != null && assistance.getResourceId() != null) {
                        resourceDAO.updateAvailability(con, assistance.getResourceId(), resourceAvailability);
                    }
                }
                con.commit();
                return updated;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al cambiar el estado de la asistencia " + assistance.getAssistanceId(), e);
        }
    }

    /** Elimina la asistencia junto con su historial en una sola transacción. */
    @Override
    public boolean delete(Integer id) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement history = con.prepareStatement(HISTORY_DELETE_SQL);
                 PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
                history.setInt(1, id);
                history.executeUpdate();
                ps.setInt(1, id);
                boolean deleted = ps.executeUpdate() > 0;
                con.commit();
                return deleted;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar la asistencia " + id, e);
        }
    }

    /**
     * Lista los cambios de estado de una asistencia en orden cronológico.
     *
     * @param assistanceId asistencia a consultar
     * @return historial de la asistencia
     */
    public List<AssistanceHistory> findHistory(int assistanceId) {
        List<AssistanceHistory> history = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(HISTORY_FIND_SQL)) {
            ps.setInt(1, assistanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapHistoryRow(rs));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el historial de la asistencia " + assistanceId, e);
        }
    }

    private void insertRow(Connection con, Assistance a) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, a.getAssistanceId());
            ps.setString(2, a.getType().getDbValue());
            ps.setTimestamp(3, Timestamp.valueOf(a.getRequestDate()));
            JdbcUtils.setNullableDouble(ps, 4, a.getIncidentLatitude());
            JdbcUtils.setNullableDouble(ps, 5, a.getIncidentLongitude());
            ps.setString(6, a.getCurrentStatus().getDbValue());
            ps.setString(7, a.getNotes());
            ps.setString(8, a.getContactPhone());
            ps.setBigDecimal(9, a.getCost());
            ps.setString(10, a.getPlate());
            ps.setInt(11, a.getTollId());
            JdbcUtils.setNullableInt(ps, 12, a.getResourceId());
            JdbcUtils.setNullableLong(ps, 13, a.getTicketId());
            ps.executeUpdate();
        }
    }

    private void insertHistory(Connection con, AssistanceHistory history) throws SQLException {
        try (PreparedStatement idPs = con.prepareStatement(HISTORY_NEXT_ID_SQL);
             ResultSet rs = idPs.executeQuery()) {
            rs.next();
            history.setHistoryId(rs.getLong(1));
        }
        AssistanceStatus previous = history.getPreviousStatus();
        try (PreparedStatement ps = con.prepareStatement(HISTORY_INSERT_SQL)) {
            ps.setLong(1, history.getHistoryId());
            ps.setString(2, previous == null ? null : previous.getDbValue());
            ps.setString(3, history.getNewStatus().getDbValue());
            ps.setTimestamp(4, Timestamp.valueOf(history.getChangeDate()));
            ps.setString(5, history.getNotes());
            ps.setInt(6, history.getAssistanceId());
            ps.executeUpdate();
        }
    }

    private int nextId(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(NEXT_ID_SQL);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<Assistance> query(String sql, Object parameter, String errorMessage) {
        List<Assistance> assistances = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setObject(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assistances.add(mapRow(rs));
                }
            }
            return assistances;
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    private Assistance mapRow(ResultSet rs) throws SQLException {
        Assistance a = new Assistance();
        a.setAssistanceId(rs.getInt("ID_ASISTENCIA"));
        a.setType(AssistanceType.fromDbValue(rs.getString("TIPO_ASISTENCIA")));
        a.setRequestDate(rs.getTimestamp("FECHA_SOLICITUD").toLocalDateTime());
        a.setIncidentLatitude(JdbcUtils.getNullableDouble(rs, "LATITUD_INCIDENTE"));
        a.setIncidentLongitude(JdbcUtils.getNullableDouble(rs, "LONGITUD_INCIDENTE"));
        a.setCurrentStatus(AssistanceStatus.fromDbValue(rs.getString("ESTADO_ACTUAL")));
        a.setNotes(rs.getString("OBSERVACIONES"));
        a.setContactPhone(rs.getString("TELEFONO_CONTACTO"));
        a.setCost(rs.getBigDecimal("COSTO"));
        a.setPlate(rs.getString("PLACA"));
        a.setTollId(rs.getInt("ID_PEAJE"));
        a.setResourceId(JdbcUtils.getNullableInt(rs, "ID_RECURSO"));
        a.setTicketId(JdbcUtils.getNullableLong(rs, "ID_TIQUETE"));
        return a;
    }

    private AssistanceHistory mapHistoryRow(ResultSet rs) throws SQLException {
        AssistanceHistory history = new AssistanceHistory();
        history.setHistoryId(rs.getLong("ID_HISTORIAL"));
        history.setPreviousStatus(AssistanceStatus.fromDbValue(rs.getString("ESTADO_ANTERIOR")));
        history.setNewStatus(AssistanceStatus.fromDbValue(rs.getString("ESTADO_NUEVO")));
        history.setChangeDate(rs.getTimestamp("FECHA_CAMBIO").toLocalDateTime());
        history.setNotes(rs.getString("OBSERVACIONES"));
        history.setAssistanceId(rs.getInt("ID_ASISTENCIA"));
        return history;
    }
}
