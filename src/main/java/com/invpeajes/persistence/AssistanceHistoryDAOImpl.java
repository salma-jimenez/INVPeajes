
package com.invpeajes.persistence;

import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code PEAJE.HISTORIAL_ASISTENCIA}.
 *
 * <p>El historial es de solo inserción: no se actualiza ni se borra de forma
 * individual, por eso no implementa {@link CrudDAO}. Las inserciones se hacen
 * dentro de la transacción de {@link AssistanceDAOImpl}.</p>
 * 
 * @author Salomejimenez
 */
public class AssistanceHistoryDAOImpl implements IAssistanceHistoryDAO {

    private static final String NEXT_ID_SQL =
            "SELECT NVL(MAX(ID_HISTORIAL), 0) + 1 FROM PEAJE.HISTORIAL_ASISTENCIA";
    private static final String INSERT_SQL =
            "INSERT INTO PEAJE.HISTORIAL_ASISTENCIA "
            + "(ID_HISTORIAL, ESTADO_ANTERIOR, ESTADO_NUEVO, FECHA_CAMBIO, OBSERVACIONES, ID_ASISTENCIA) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_ASSISTANCE_SQL =
            "SELECT * FROM PEAJE.HISTORIAL_ASISTENCIA WHERE ID_ASISTENCIA = ? "
            + "ORDER BY FECHA_CAMBIO, ID_HISTORIAL";
    private static final String DELETE_BY_ASSISTANCE_SQL =
            "DELETE FROM PEAJE.HISTORIAL_ASISTENCIA WHERE ID_ASISTENCIA = ?";

    /**
     * Inserta un registro de historial usando una conexión ya abierta,
     * para que participe en la transacción de quien llama.
     *
     * @param con     conexión con la transacción en curso
     * @param history registro a insertar; se le asigna el id generado
     * @throws SQLException si falla la inserción
     */
    void insert(Connection con, AssistanceHistory history) throws SQLException {
        history.setHistoryId(nextId(con));
        AssistanceStatus previous = history.getPreviousStatus();
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            ps.setLong(1, history.getHistoryId());
            ps.setString(2, previous == null ? null : previous.getDbValue());
            ps.setString(3, history.getNewStatus().getDbValue());
            ps.setTimestamp(4, Timestamp.valueOf(history.getChangeDate()));
            ps.setString(5, history.getNotes());
            ps.setInt(6, history.getAssistanceId());
            ps.executeUpdate();
        }
    }

    /**
     * Borra todo el historial de una asistencia (usado al eliminarla).
     *
     * @param con          conexión con la transacción en curso
     * @param assistanceId asistencia cuyo historial se borra
     * @throws SQLException si falla el borrado
     */
    void deleteByAssistance(Connection con, int assistanceId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(DELETE_BY_ASSISTANCE_SQL)) {
            ps.setInt(1, assistanceId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<AssistanceHistory> findByAssistance(int assistanceId) {
        List<AssistanceHistory> history = new ArrayList<>();
        try (Connection con = VehicleConnectionFactory.open();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ASSISTANCE_SQL)) {
            ps.setInt(1, assistanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapRow(rs));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el historial de la asistencia " + assistanceId, e);
        }
    }

    private long nextId(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(NEXT_ID_SQL);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private AssistanceHistory mapRow(ResultSet rs) throws SQLException {
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
