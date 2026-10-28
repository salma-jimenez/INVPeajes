package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Intermediador;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Intermediador sobre Oracle (tabla INTERMEDIADOR).
 * Cumple el contrato de {@link IIntermediadorDAO}.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class IntermediadorDAOImpl implements IintermediadorDAO {

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
     * @param intermediador Intermediador a guardar (el ID se envía, no lo genera Oracle).
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID o nombre repetido).
     */
    @Override
    public void insert(Intermediador intermediador) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, intermediador.getIdIntermediador());
            ps.setString(2, intermediador.getNombre());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el intermediador "
                    + intermediador.getNombre(), e);
        }
    }

    /**
     * Busca un intermediador por su ID.
     * @param idIntermediador ID del intermediador.
     * @return El intermediador si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Intermediador> findById(Integer idIntermediador) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, idIntermediador);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el intermediador " + idIntermediador, e);
        }
    }

    /**
     * Lista todos los intermediadores ordenados por nombre.
     * @return Lista de intermediadores (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Intermediador> findAll() {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            List<Intermediador> intermediadores = new ArrayList<>();
            while (rs.next()) {
                intermediadores.add(mapRow(rs));
            }
            return intermediadores;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los intermediadores", e);
        }
    }

    /**
     * Actualiza el nombre de un intermediador existente.
     * @param intermediador Intermediador con los datos nuevos.
     * @return true si se actualizó; false si no existe un intermediador con ese ID.
     * @throws DataAccessException Si falla la actualización (por ejemplo, nombre repetido).
     */
    @Override
    public boolean update(Intermediador intermediador) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, intermediador.getNombre());
            ps.setInt(2, intermediador.getIdIntermediador());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el intermediador "
                    + intermediador.getIdIntermediador(), e);
        }
    }

    /**
     * Elimina un intermediador por su ID.
     * @param idIntermediador ID del intermediador.
     * @return true si se eliminó; false si no existe un intermediador con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si hay vehículos con TAG suyo).
     */
    @Override
    public boolean delete(Integer idIntermediador) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idIntermediador);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el intermediador " + idIntermediador, e);
        }
    }

    @Override
    public Optional<Intermediador> findByName(String nombre) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el intermediador " + nombre, e);
        }
    }

    private Intermediador mapRow(ResultSet rs) throws SQLException {
        return new Intermediador(rs.getInt("ID_INTERMEDIADOR"), rs.getString("NOMBRE"));
    }
}