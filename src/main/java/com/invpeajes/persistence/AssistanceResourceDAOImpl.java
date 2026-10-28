package com.invpeajes.persistence;

import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del acceso a la tabla {@code PEAJE.RECURSO_ASISTENCIA}.
 */
public class AssistanceResourceDAOImpl implements IAssistanceResourceDAO {

    private static final String NEXT_ID_SQL =
            "SELECT NVL(MAX(ID_RECURSO), 0) + 1 FROM PEAJE.RECURSO_ASISTENCIA";
    private static final String INSERT_SQL =
            "INSERT INTO PEAJE.RECURSO_ASISTENCIA (ID_RECURSO, TIPO, IDENTIFICACION, ID_PEAJE, DISPONIBILIDAD) "
            + "VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_SQL =
            "SELECT * FROM PEAJE.RECURSO_ASISTENCIA ";
    private static final String UPDATE_SQL =
            "UPDATE PEAJE.RECURSO_ASISTENCIA SET TIPO = ?, IDENTIFICACION = ?, ID_PEAJE = ?, DISPONIBILIDAD = ? "
            + "WHERE ID_RECURSO = ?";
    private static final String UPDATE_AVAILABILITY_SQL =
            "UPDATE PEAJE.RECURSO_ASISTENCIA SET DISPONIBILIDAD = ? WHERE ID_RECURSO = ?";
    private static final String DELETE_SQL =
            "DELETE FROM PEAJE.RECURSO_ASISTENCIA WHERE ID_RECURSO = ?";

    /**
     * Inserta el recurso y le asigna el id generado.
     */
    @Override
    public void insert(AssistanceResource resource) {
        try (Connection con = VehicleConnectionFactory.open()) {
            resource.setResourceId(nextId(con));
            try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
                ps.setInt(1, resource.getResourceId());
                ps.setString(2, resource.getType().getDbValue());
                ps.setString(3, resource.getIdentification());
                ps.setInt(4, resource.getTollId());
                ps.setString(5, resource.getAvailability().getDbValue());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar el recurso " + resource.getIdentification(), e);
        }
    }

    @Override
    public Optional<AssistanceResource> findById(Integer id) {
        List<AssistanceResource> result = query(SELECT_SQL + "WHERE ID_RECURSO = ?",
                "Error al buscar el recurso " + id, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public Optional<AssistanceResource> findByIdentification(String identification) {
        List<AssistanceResource> result = query(SELECT_SQL + "WHERE IDENTIFICACION = ?",
                "Error al buscar el recurso " + identification, identification);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<AssistanceResource> findAll() {
        return query(SELECT_SQL + "ORDER BY ID_PEAJE, TIPO, ID_RECURSO", "Error al listar los recursos");
    }

    @Override
    public List<AssistanceResource> findByToll(int tollId) {
        return query(SELECT_SQL + "WHERE ID_PEAJE = ? ORDER BY TIPO, ID_RECURSO",
                "Error al listar los recursos del peaje " + tollId, tollId);
    }

    @Override
    public List<AssistanceResource> findAvailable(AssistanceType type, int tollId) {
        return query(SELECT_SQL + "WHERE TIPO = ? AND ID_PEAJE = ? AND DISPONIBILIDAD = ? ORDER BY ID_RECURSO",
                "Error al buscar recursos disponibles", type.getDbValue(), tollId,
                ResourceAvailability.AVAILABLE.getDbValue());
    }

    @Override
    public boolean update(AssistanceResource resource) {
        try (Connection con = VehicleConnectionFactory.open();
             PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, resource.getType().getDbValue());
            ps.setString(2, resource.getIdentification());
            ps.setInt(3, resource.getTollId());
            ps.setString(4, resource.getAvailability().getDbValue());
            ps.setInt(5, resource.getResourceId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el recurso " + resource.getResourceId(), e);
        }
    }

    @Override
    public boolean updateAvailability(int resourceId, ResourceAvailability availability) {
        try (Connection con = VehicleConnectionFactory.open()) {
            return updateAvailability(con, resourceId, availability);
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar la disponibilidad del recurso " + resourceId, e);
        }
    }

    /**
     * Cambia la disponibilidad usando una conexión ya abierta, para que
     * participe en la transacción de {@link AssistanceDAOImpl}.
     *
     * @param con          conexión con la transacción en curso
     * @param resourceId   recurso a modificar
     * @param availability nueva disponibilidad
     * @return true si se actualizó el recurso
     * @throws SQLException si falla la actualización
     */
    boolean updateAvailability(Connection con, int resourceId, ResourceAvailability availability)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(UPDATE_AVAILABILITY_SQL)) {
            ps.setString(1, availability.getDbValue());
            ps.setInt(2, resourceId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) {
        try (Connection con = VehicleConnectionFactory.open();
             PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar el recurso " + id, e);
        }
    }

    private int nextId(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(NEXT_ID_SQL);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<AssistanceResource> query(String sql, String errorMessage, Object... parameters) {
        List<AssistanceResource> resources = new ArrayList<>();
        try (Connection con = VehicleConnectionFactory.open();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                ps.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapRow(rs));
                }
            }
            return resources;
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    private AssistanceResource mapRow(ResultSet rs) throws SQLException {
        return new AssistanceResource(rs.getInt("ID_RECURSO"),
                AssistanceType.fromDbValue(rs.getString("TIPO")),
                rs.getString("IDENTIFICACION"), rs.getInt("ID_PEAJE"),
                ResourceAvailability.fromDbValue(rs.getString("DISPONIBILIDAD")));
    }
    
}
