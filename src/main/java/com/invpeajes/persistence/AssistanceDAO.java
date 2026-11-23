package com.invpeajes.persistence;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
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
 * ASIGNAR y lo libera al quedar ATENDIDA o CANCELADA.</p>
 *
 * <p>Las solicitudes se crean con {@code pkg_asistencias.Pr_solicitar_asistencia} y los cambios
 * de estado se hacen con {@code pkg_asistencias.Pr_cambiar_estado_asistencia}, que recibe la
 * cédula del jefe de estación. Los ID de ASISTENCIA e HISTORIAL_ASISTENCIA los genera la base
 * (IDENTITY).</p>
 *
 * @author Salomejimenez
 */
public class AssistanceDAO implements CrudDAO<Assistance, Integer> {

    private static final String INSERT_COLUMNS =
            "TIPO_ASISTENCIA, FECHA_SOLICITUD, LATITUD_INCIDENTE, LONGITUD_INCIDENTE, "
            + "ESTADO_ACTUAL, OBSERVACIONES, TELEFONO_CONTACTO, COSTO, PLACA, ID_PEAJE, ID_RECURSO, ID_TIQUETE, "
            + "ID_VIAJERO";
    private static final String COLUMNS = "ID_ASISTENCIA, " + INSERT_COLUMNS;
    private static final String SELECT_SQL =
            "SELECT " + COLUMNS + " FROM PEAJE.ASISTENCIA ";
    private static final String UPDATE_SQL =
            "UPDATE PEAJE.ASISTENCIA SET TIPO_ASISTENCIA = ?, LATITUD_INCIDENTE = ?, LONGITUD_INCIDENTE = ?, "
            + "OBSERVACIONES = ?, TELEFONO_CONTACTO = ?, COSTO = ?, PLACA = ?, ID_PEAJE = ?, ID_RECURSO = ?, "
            + "ID_TIQUETE = ? WHERE ID_ASISTENCIA = ?";
    /**
     * Solicitud nueva con {@code pkg_asistencias.Pr_solicitar_asistencia}. Antes se deja la nota
     * que el trigger guarda en la primera fila del historial (misma conexión y misma sesión).
     */
    private static final String REQUEST_SQL =
            "BEGIN "
          + "  pkg_control.g_nota_historial := ?; "
          + "  pkg_asistencias.Pr_solicitar_asistencia(p_tipo => ?, p_peaje => ?, p_telefono => ?, "
          + "      p_latitud => ?, p_longitud => ?, p_id_asistencia => ?, p_placa => ?, "
          + "      p_observaciones => ?, p_id_viajero => ?); "
          + "END;";
    /** Cambio de estado hecho por el jefe de estación ({@code Pr_cambiar_estado_asistencia}). */
    private static final String CHANGE_STATUS_SQL =
            "BEGIN pkg_asistencias.Pr_cambiar_estado_asistencia(p_cedula_jefe => ?, p_asistencia => ?, "
          + "p_estado => ?, p_observacion => ?, p_recurso => ?, p_costo => ?); END;";
    /** Datos que el procedimiento no recibe (ID_TIQUETE y COSTO inicial). */
    private static final String EXTRA_DATA_SQL =
            "UPDATE PEAJE.ASISTENCIA SET ID_TIQUETE = ?, COSTO = ? WHERE ID_ASISTENCIA = ?";
    private static final String DELETE_SQL =
            "DELETE FROM PEAJE.ASISTENCIA WHERE ID_ASISTENCIA = ?";

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
     * Registra la solicitud con {@code pkg_asistencias.Pr_solicitar_asistencia} y asigna a la
     * asistencia recibida el ID que generó la base. La fila inicial del historial la guarda el
     * trigger {@code trg_historial_asistencia}, con la nota "Solicitud registrada".
     */
    @Override
    public void insert(Assistance assistance) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (CallableStatement cs = con.prepareCall(REQUEST_SQL)) {
                    cs.setString(1, "Solicitud registrada");
                    cs.setString(2, assistance.getType().getDbValue());
                    cs.setInt(3, assistance.getTollId());
                    cs.setString(4, assistance.getContactPhone());
                    JdbcUtils.setNullableDouble(cs, 5, assistance.getIncidentLatitude());
                    JdbcUtils.setNullableDouble(cs, 6, assistance.getIncidentLongitude());
                    cs.registerOutParameter(7, Types.NUMERIC);
                    cs.setString(8, assistance.getPlate());
                    cs.setString(9, assistance.getNotes());
                    JdbcUtils.setNullableInt(cs, 10, assistance.getTravelerId());
                    cs.execute();
                    assistance.setAssistanceId(cs.getInt(7));
                }
                if (assistance.getTicketId() != null || assistance.getCost() != null) {
                    try (PreparedStatement ps = con.prepareStatement(EXTRA_DATA_SQL)) {
                        JdbcUtils.setNullableLong(ps, 1, assistance.getTicketId());
                        ps.setBigDecimal(2, assistance.getCost());
                        ps.setInt(3, assistance.getAssistanceId());
                        ps.executeUpdate();
                    }
                }
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
     * Cambia el estado de una asistencia con {@code pkg_asistencias.Pr_cambiar_estado_asistencia}.
     * La base revisa que el jefe sea del peaje de la asistencia, que el cambio esté permitido y, al
     * ASIGNAR, que el recurso esté DISPONIBLE. El trigger {@code trg_historial_asistencia} guarda
     * la fila del historial (con la nota) y ocupa o libera el recurso.
     *
     * @param managerId    cédula del jefe de estación
     * @param assistanceId asistencia
     * @param status       estado nuevo (valor de la base, ej. ASIGNADA)
     * @param notes        motivo del cambio (va al historial)
     * @param resourceId   recurso, solo al asignar; si no, null
     * @param cost         costo, solo al atender; si no, null
     * @throws DataAccessException si la base rechaza el cambio
     */
    public void changeStatus(String managerId, int assistanceId, String status, String notes,
                             Integer resourceId, java.math.BigDecimal cost) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(CHANGE_STATUS_SQL)) {
            cs.setString(1, managerId);
            cs.setInt(2, assistanceId);
            cs.setString(3, status);
            cs.setString(4, limit(notes));
            JdbcUtils.setNullableInt(cs, 5, resourceId);
            cs.setBigDecimal(6, cost);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar el estado de la asistencia " + assistanceId, e);
        }
    }

    /** La observación del historial admite máximo 200 caracteres. */
    private static String limit(String notes) {
        String note = notes == null || notes.isBlank() ? null : notes.trim();
        return note != null && note.length() > 200 ? note.substring(0, 200) : note;
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
