package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Caseta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Caseta sobre Oracle (tabla CASETAS).
 * Cumple el contrato de {@link ICasetaDAO}.
 *
 * El ID de la caseta lo genera Oracle (columna IDENTITY): al insertar no se envía,
 * y el número generado se asigna de vuelta al objeto.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class CasetaDAOImpl implements ICasetaDAO {

    private static final String COLUMNS =
            "ID_CASETA, NUMERO_CASETA, TIPO_PAGO, SENTIDO, ESTADO, ID_PEAJE";

    private static final String SQL_INSERT =
            "INSERT INTO CASETAS (NUMERO_CASETA, TIPO_PAGO, SENTIDO, ESTADO, ID_PEAJE) "
          + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM CASETAS";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_CASETA = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY ID_PEAJE, NUMERO_CASETA";

    private static final String SQL_FIND_BY_TOLL =
            SQL_SELECT + " WHERE ID_PEAJE = ? ORDER BY NUMERO_CASETA";

    private static final String SQL_UPDATE =
            "UPDATE CASETAS SET NUMERO_CASETA = ?, TIPO_PAGO = ?, SENTIDO = ?, "
          + "ESTADO = ?, ID_PEAJE = ? WHERE ID_CASETA = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE CASETAS SET ESTADO = ? WHERE ID_CASETA = ?";

    private static final String SQL_DELETE = "DELETE FROM CASETAS WHERE ID_CASETA = ?";

    /**
     * Inserta una nueva caseta. Oracle genera el ID y se asigna al objeto.
     * @param caseta Caseta a guardar (su ID puede ser null).
     * @throws DataAccessException Si falla la inserción (por ejemplo, número repetido en el mismo peaje).
     */
    @Override
    public void insert(Caseta caseta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, new String[] {"ID_CASETA"})) {

            setFields(ps, caseta);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    caseta.setIdCaseta(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la caseta "
                    + caseta.getNumeroCaseta() + " del peaje " + caseta.getIdPeaje(), e);
        }
    }

    /**
     * Busca una caseta por su ID.
     * @param idCaseta ID de la caseta.
     * @return La caseta si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Caseta> findById(Integer idCaseta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, idCaseta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar la caseta " + idCaseta, e);
        }
    }

    /**
     * Lista todas las casetas, agrupadas por peaje y ordenadas por número.
     * @return Lista de casetas (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Caseta> findAll() {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las casetas", e);
        }
    }

    /**
     * Actualiza todos los datos de una caseta existente.
     * @param caseta Caseta con los datos nuevos.
     * @return true si se actualizó; false si no existe una caseta con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Caseta caseta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, caseta);
            ps.setInt(6, caseta.getIdCaseta());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la caseta " + caseta.getIdCaseta(), e);
        }
    }

    /**
     * Elimina una caseta por su ID.
     * @param idCaseta ID de la caseta.
     * @return true si se eliminó; false si no existe una caseta con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene tiquetes asociados).
     */
    @Override
    public boolean delete(Integer idCaseta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idCaseta);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar la caseta " + idCaseta, e);
        }
    }

    @Override
    public List<Caseta> findByToll(int idPeaje) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TOLL)) {

            ps.setInt(1, idPeaje);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las casetas del peaje " + idPeaje, e);
        }
    }

    @Override
    public boolean updateStatus(int idCaseta, String estado) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, estado);
            ps.setInt(2, idCaseta);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado de la caseta " + idCaseta, e);
        }
    }

    /** Asigna los 5 campos que no son la llave (índices 1 a 5), comunes a INSERT y UPDATE. */
    private void setFields(PreparedStatement ps, Caseta c) throws SQLException {
        ps.setInt(1, c.getNumeroCaseta());
        ps.setString(2, c.getTipoPago());
        ps.setString(3, c.getSentido());
        ps.setString(4, c.getEstado());
        ps.setInt(5, c.getIdPeaje());
    }

    private List<Caseta> mapList(ResultSet rs) throws SQLException {
        List<Caseta> casetas = new ArrayList<>();
        while (rs.next()) {
            casetas.add(mapRow(rs));
        }
        return casetas;
    }

    private Caseta mapRow(ResultSet rs) throws SQLException {
        return new Caseta(
                rs.getInt("ID_CASETA"),
                rs.getInt("NUMERO_CASETA"),
                rs.getString("TIPO_PAGO"),
                rs.getString("SENTIDO"),
                rs.getString("ESTADO"),
                rs.getInt("ID_PEAJE"));
    }
}