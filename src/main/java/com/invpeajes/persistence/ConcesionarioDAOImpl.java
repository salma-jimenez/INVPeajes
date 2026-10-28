package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Concesionario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Concesionario sobre Oracle (tabla CONCESIONARIO).
 * Cumple el contrato de {@link IConcesionarioDAO}.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class ConcesionarioDAOImpl implements IConcesionarioDAO {

    private static final String COLUMNS =
            "ID_CONCESIONARIO, NOMBRE, FECHA, NIT, TIPO_CONCESIONARIO, CORREO, TELEFONO, DIRECCION";

    private static final String SQL_INSERT =
            "INSERT INTO CONCESIONARIO (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM CONCESIONARIO";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_CONCESIONARIO = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY NOMBRE";

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + " WHERE NOMBRE = ?";

    private static final String SQL_FIND_BY_TYPE =
            SQL_SELECT + " WHERE TIPO_CONCESIONARIO = ? ORDER BY NOMBRE";

    private static final String SQL_UPDATE =
            "UPDATE CONCESIONARIO SET NOMBRE = ?, FECHA = ?, NIT = ?, TIPO_CONCESIONARIO = ?, "
          + "CORREO = ?, TELEFONO = ?, DIRECCION = ? WHERE ID_CONCESIONARIO = ?";

    private static final String SQL_DELETE = "DELETE FROM CONCESIONARIO WHERE ID_CONCESIONARIO = ?";

    /**
     * Inserta un nuevo concesionario.
     * @param concesionario Concesionario a guardar (el ID se envía, no lo genera Oracle).
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID o nombre repetido).
     */
    @Override
    public void insert(Concesionario concesionario) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, concesionario.getIdConcesionario());
            setFields(ps, concesionario, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el concesionario "
                    + concesionario.getNombreConcesionario(), e);
        }
    }

    /**
     * Busca un concesionario por su ID.
     * @param idConcesionario ID del concesionario.
     * @return El concesionario si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Concesionario> findById(Integer idConcesionario) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, idConcesionario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el concesionario " + idConcesionario, e);
        }
    }

    /**
     * Lista todos los concesionarios ordenados por nombre.
     * @return Lista de concesionarios (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Concesionario> findAll() {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los concesionarios", e);
        }
    }

    /**
     * Actualiza los datos de un concesionario existente.
     * @param concesionario Concesionario con los datos nuevos.
     * @return true si se actualizó; false si no existe un concesionario con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Concesionario concesionario) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, concesionario, 1);
            ps.setInt(8, concesionario.getIdConcesionario());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el concesionario "
                    + concesionario.getIdConcesionario(), e);
        }
    }

    /**
     * Elimina un concesionario por su ID.
     * @param idConcesionario ID del concesionario.
     * @return true si se eliminó; false si no existe un concesionario con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si administra peajes).
     */
    @Override
    public boolean delete(Integer idConcesionario) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idConcesionario);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el concesionario " + idConcesionario, e);
        }
    }

    @Override
    public Optional<Concesionario> findByName(String nombre) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el concesionario " + nombre, e);
        }
    }

    @Override
    public List<Concesionario> findByType(String tipoConcesionario) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TYPE)) {

            ps.setString(1, tipoConcesionario);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los concesionarios de tipo "
                    + tipoConcesionario, e);
        }
    }

    /** Asigna los 7 campos que no son la llave, empezando en el índice {@code start}. */
    private void setFields(PreparedStatement ps, Concesionario c, int start) throws SQLException {
        int i = start;
        ps.setString(i++, c.getNombreConcesionario());
        if (c.getFecha() != null) {
            ps.setDate(i++, new java.sql.Date(c.getFecha().getTime()));
        } else {
            ps.setNull(i++, Types.DATE);
        }
        ps.setString(i++, c.getNit());
        ps.setString(i++, c.getTipoConcesionario());
        ps.setString(i++, c.getCorreo());
        ps.setString(i++, c.getTelefono());
        ps.setString(i, c.getDireccion());
    }

    private List<Concesionario> mapList(ResultSet rs) throws SQLException {
        List<Concesionario> concesionarios = new ArrayList<>();
        while (rs.next()) {
            concesionarios.add(mapRow(rs));
        }
        return concesionarios;
    }

    private Concesionario mapRow(ResultSet rs) throws SQLException {
        return new Concesionario(
                rs.getInt("ID_CONCESIONARIO"),
                rs.getString("NOMBRE"),
                rs.getDate("FECHA"),
                rs.getString("NIT"),
                rs.getString("TIPO_CONCESIONARIO"),
                rs.getString("CORREO"),
                rs.getString("TELEFONO"),
                rs.getString("DIRECCION"));
    }
}