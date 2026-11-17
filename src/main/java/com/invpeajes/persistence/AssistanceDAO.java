package com.invpeajes.persistence;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;

import java.sql.CallableStatement;
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
 * <p>Desde la etapa 4 de la base de datos, el historial y el recurso los maneja
 * el trigger {@code trg_historial_asistencia}: al crear la asistencia guarda la
 * fila inicial, y en cada cambio de estado guarda el cambio, ocupa el recurso al
 * ASIGNAR y lo libera al quedar ATENDIDA o CANCELADA. Este DAO solo le pasa la
 * observación del cambio por medio de {@code pkg_control.g_nota_historial}.
 * Los ID de ASISTENCIA e HISTORIAL_ASISTENCIA los genera la base (IDENTITY).</p>
 *
 * @author Salomejimenez
 */
public class AssistanceDAO implements CrudDAO<Assistance, Integer> {

    private static final String INSERT_COLUMNS =
            "TIPO_ASISTENCIA, FECHA_SOLICITUD, LATITUD_INCIDENTE, LONGITUD_INCIDENTE, "
            + "ESTADO_ACTUAL, OBSERVACIONES, TELEFONO_CONTACTO, COSTO, PLACA, ID_PEAJE, ID_RECURSO, ID_TIQUETE, "
            + "ID_VIAJERO";
    private static final String COLUMNS = "ID_ASISTENCIA, " + INSERT_COLUMNS;
    private static final String INSERT_SQL =
            "INSERT INTO PEAJE.ASISTENCIA (" + INSERT_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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

    /** Observación que trg_historial_asistencia copia en la fila del historial. */
    private static final String SET_HISTORY_NOTE_SQL =
            "BEGIN pkg_control.g_nota_historial := ?; END;";
    private static final String HISTORY_FIND_SQL =
            "SELECT * FROM PEAJE.HISTORIAL_ASISTENCIA WHERE ID_ASISTENCIA = ? "
            + "ORDER BY FECHA_CAMBIO, ID_HISTORIAL";

    /** Crea el DAO. */
    public AssistanceDAO() {
    }

    /**
     * Crea el DAO. El DAO de recursos ya no se usa: el trigger
     * {@code trg_historial_asistencia} cambia la disponibilidad del recurso.
     * Se conserva el constructor por compatibilidad.
     *
     * @param resourceDAO no se usa
     */
    public AssistanceDAO(AssistanceResourceDAO resourceDAO) {
        this();
    }

    /**
     * Inserta la asistencia y asigna a la asistencia recibida el id que generó la base.
     * La fila inicial del historial la guarda el trigger, con la nota "Solicitud registrada".
     */
    @Override
    public void insert(Assistance assistance) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                setHistoryNote(con, "Solicitud registrada");
                assistance.setAssistanceId(insertRow(con, assistance));
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
     * Lista las asistencias que pidió un viajero con cuenta.
     *
     * @param travelerId ID del viajero
     * @return asistencias del viajero, más recientes primero
     */
    public List<Assistance> findByTraveler(int travelerId) {
        return query(SELECT_SQL + "WHERE ID_VIAJERO = ? ORDER BY FECHA_SOLICITUD DESC",
                travelerId, "Error al listar asistencias del viajero " + travelerId);
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
     * Guarda el nuevo estado de la asistencia. El trigger {@code trg_historial_asistencia}
     * registra el cambio en el historial (con la nota indicada) y ocupa o libera el recurso.
     *
     * @param assistance           asistencia con el estado nuevo ya asignado
     * @param previousStatus       estado que tenía antes del cambio (lo guarda el trigger)
     * @param notes                motivo del cambio
     * @param resourceAvailability no se usa: la disponibilidad la cambia el trigger
     * @return true si se actualizó la asistencia
     */
    public boolean changeStatus(Assistance assistance, AssistanceStatus previousStatus, String notes,
                                ResourceAvailability resourceAvailability) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(CHANGE_STATUS_SQL)) {
                setHistoryNote(con, notes);
                ps.setString(1, assistance.getCurrentStatus().getDbValue());
                JdbcUtils.setNullableInt(ps, 2, assistance.getResourceId());
                ps.setBigDecimal(3, assistance.getCost());
                ps.setInt(4, assistance.getAssistanceId());
                boolean updated = ps.executeUpdate() > 0;
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

    /**
     * Elimina una asistencia CANCELADA. El trigger {@code trg_borrar_asistencia}
     * borra su historial y rechaza las que no están canceladas.
     */
    @Override
    public boolean delete(Integer id) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
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

    /** Inserta la fila y devuelve el ID_ASISTENCIA que generó la base. */
    private int insertRow(Connection con, Assistance a) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL, new String[] {"ID_ASISTENCIA"})) {
            ps.setString(1, a.getType().getDbValue());
            ps.setTimestamp(2, Timestamp.valueOf(a.getRequestDate()));
            JdbcUtils.setNullableDouble(ps, 3, a.getIncidentLatitude());
            JdbcUtils.setNullableDouble(ps, 4, a.getIncidentLongitude());
            ps.setString(5, a.getCurrentStatus().getDbValue());
            ps.setString(6, a.getNotes());
            ps.setString(7, a.getContactPhone());
            ps.setBigDecimal(8, a.getCost());
            ps.setString(9, a.getPlate());
            ps.setInt(10, a.getTollId());
            JdbcUtils.setNullableInt(ps, 11, a.getResourceId());
            JdbcUtils.setNullableLong(ps, 12, a.getTicketId());
            JdbcUtils.setNullableInt(ps, 13, a.getTravelerId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("La base de datos no devolvió el ID de la asistencia");
                }
                return keys.getInt(1);
            }
        }
    }

    /** Deja en la sesión la nota que el trigger guardará en el historial (máximo 200 caracteres). */
    private void setHistoryNote(Connection con, String notes) throws SQLException {
        String note = notes == null || notes.isBlank() ? null : notes.trim();
        if (note != null && note.length() > 200) {
            note = note.substring(0, 200);
        }
        try (CallableStatement cs = con.prepareCall(SET_HISTORY_NOTE_SQL)) {
            cs.setString(1, note);
            cs.execute();
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
        a.setTravelerId(JdbcUtils.getNullableInt(rs, "ID_VIAJERO"));
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
