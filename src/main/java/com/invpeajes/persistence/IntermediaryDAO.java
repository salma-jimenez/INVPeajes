package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Intermediary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Intermediador sobre Oracle (tabla INTERMEDIADOR).
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega las consultas propias de Intermediador.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class IntermediaryDAO implements CrudDAO<Intermediary, Integer> {

    private static final String COLUMNS = "ID_INTERMEDIADOR, NOMBRE";

    private static final String SQL_INSERT =
            "INSERT INTO INTERMEDIADOR (" + COLUMNS + ") VALUES (?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM INTERMEDIADOR";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_INTERMEDIADOR = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY NOMBRE";

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + " WHERE UPPER(NOMBRE) = UPPER(?)";

    private static final String SQL_UPDATE =
            "UPDATE INTERMEDIADOR SET NOMBRE = ? WHERE ID_INTERMEDIADOR = ?";

    private static final String SQL_DELETE = "DELETE FROM INTERMEDIADOR WHERE ID_INTERMEDIADOR = ?";

    /**
     * Inserta un nuevo intermediador.
     * @param intermediary Intermediador a guardar (el ID se envía, no lo genera Oracle).
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID o nombre repetido).
     */
    @Override
    public void insert(Intermediary intermediary) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, intermediary.getIntermediaryId());
            ps.setString(2, intermediary.getName());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el intermediador "
                    + intermediary.getName(), e);
        }
    }

    /**
     * Busca un intermediador por su ID.
     * @param intermediaryId ID del intermediador.
     * @return El intermediador si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Intermediary> findById(Integer intermediaryId) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, intermediaryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el intermediador " + intermediaryId, e);
        }
    }

    /**
     * Lista todos los intermediadores ordenados por nombre.
     * @return Lista de intermediadores (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Intermediary> findAll() {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            List<Intermediary> intermediaries = new ArrayList<>();
            while (rs.next()) {
                intermediaries.add(mapRow(rs));
            }
            return intermediaries;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los intermediadores", e);
        }
    }

    /**
     * Actualiza el nombre de un intermediador existente.
     * @param intermediary Intermediador con los datos nuevos.
     * @return true si se actualizó; false si no existe un intermediador con ese ID.
     * @throws DataAccessException Si falla la actualización (por ejemplo, nombre repetido).
     */
    @Override
    public boolean update(Intermediary intermediary) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, intermediary.getName());
            ps.setInt(2, intermediary.getIntermediaryId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el intermediador "
                    + intermediary.getIntermediaryId(), e);
        }
    }

    /**
     * Elimina un intermediador por su ID.
     * @param intermediaryId ID del intermediador.
     * @return true si se eliminó; false si no existe un intermediador con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si hay vehículos con TAG suyo).
     */
    @Override
    public boolean delete(Integer intermediaryId) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, intermediaryId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el intermediador " + intermediaryId, e);
        }
    }

    /**
     * Busca un intermediador por su nombre, sin distinguir mayúsculas.
     * @param name Nombre del intermediador (ej. "flypass").
     * @return El intermediador si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public Optional<Intermediary> findByName(String name) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el intermediador " + name, e);
        }
    }

    private Intermediary mapRow(ResultSet rs) throws SQLException {
        return new Intermediary(rs.getInt("ID_INTERMEDIADOR"), rs.getString("NOMBRE"));
    }
}