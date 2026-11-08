package com.invpeajes.persistence;

import com.invpeajes.model.Concessionaire;
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
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega las consultas propias de Concesionario.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class ConcessionaireDAO implements CrudDAO<Concessionaire, Integer> {

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
     * @param concessionaire Concesionario a guardar (el ID se envía, no lo genera Oracle).
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID o nombre repetido).
     */
    @Override
    public void insert(Concessionaire concessionaire) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, concessionaire.getConcessionaireId());
            setFields(ps, concessionaire, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el concesionario "
                    + concessionaire.getName(), e);
        }
    }

    /**
     * Busca un concesionario por su ID.
     * @param concessionaireId ID del concesionario.
     * @return El concesionario si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Concessionaire> findById(Integer concessionaireId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, concessionaireId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el concesionario " + concessionaireId, e);
        }
    }

    /**
     * Lista todos los concesionarios ordenados por nombre.
     * @return Lista de concesionarios (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Concessionaire> findAll() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los concesionarios", e);
        }
    }

    /**
     * Actualiza los datos de un concesionario existente.
     * @param concessionaire Concesionario con los datos nuevos.
     * @return true si se actualizó; false si no existe un concesionario con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Concessionaire concessionaire) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, concessionaire, 1);
            ps.setInt(8, concessionaire.getConcessionaireId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el concesionario "
                    + concessionaire.getConcessionaireId(), e);
        }
    }

    /**
     * Elimina un concesionario por su ID.
     * @param concessionaireId ID del concesionario.
     * @return true si se eliminó; false si no existe un concesionario con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si administra peajes).
     */
    @Override
    public boolean delete(Integer concessionaireId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, concessionaireId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el concesionario " + concessionaireId, e);
        }
    }

    /**
     * Busca un concesionario por su nombre exacto (el nombre es único en la BD).
     * @param name Nombre del concesionario.
     * @return El concesionario si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public Optional<Concessionaire> findByName(String name) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el concesionario " + name, e);
        }
    }

    /**
     * Lista los concesionarios de un tipo.
     * @param type INVIAS, CONCESION u OTRO.
     * @return Concesionarios de ese tipo, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public List<Concessionaire> findByType(String type) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TYPE)) {

            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los concesionarios de tipo "
                    + type, e);
        }
    }

    /** Asigna los 7 campos que no son la llave, empezando en el índice {@code start}. */
    private void setFields(PreparedStatement ps, Concessionaire c, int start) throws SQLException {
        int i = start;
        ps.setString(i++, c.getName());
        if (c.getDate() != null) {
            ps.setDate(i++, new java.sql.Date(c.getDate().getTime()));
        } else {
            ps.setNull(i++, Types.DATE);
        }
        ps.setString(i++, c.getNit());
        ps.setString(i++, c.getType());
        ps.setString(i++, c.getEmail());
        ps.setString(i++, c.getPhone());
        ps.setString(i, c.getAddress());
    }

    private List<Concessionaire> mapList(ResultSet rs) throws SQLException {
        List<Concessionaire> concessionaires = new ArrayList<>();
        while (rs.next()) {
            concessionaires.add(mapRow(rs));
        }
        return concessionaires;
    }

    private Concessionaire mapRow(ResultSet rs) throws SQLException {
        return new Concessionaire(
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