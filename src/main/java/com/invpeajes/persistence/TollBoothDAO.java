package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.TollBooth;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Caseta sobre Oracle (tabla CASETAS).
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega las consultas propias de Caseta.
 *
 * El ID de la caseta lo genera Oracle (columna IDENTITY): al insertar no se envía,
 * y el número generado se asigna de vuelta al objeto.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class TollBoothDAO implements CrudDAO<TollBooth, Integer> {

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
     * @param booth Caseta a guardar (su ID puede ser null).
     * @throws DataAccessException Si falla la inserción (por ejemplo, número repetido en el mismo peaje).
     */
    @Override
    public void insert(TollBooth booth) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, new String[] {"ID_CASETA"})) {

            setFields(ps, booth);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    booth.setBoothId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la caseta "
                    + booth.getBoothNumber() + " del peaje " + booth.getTollId(), e);
        }
    }

    /**
     * Busca una caseta por su ID.
     * @param boothId ID de la caseta.
     * @return La caseta si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<TollBooth> findById(Integer boothId) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, boothId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar la caseta " + boothId, e);
        }
    }

    /**
     * Lista todas las casetas, agrupadas por peaje y ordenadas por número.
     * @return Lista de casetas (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<TollBooth> findAll() {
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
     * @param booth Caseta con los datos nuevos.
     * @return true si se actualizó; false si no existe una caseta con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(TollBooth booth) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, booth);
            ps.setInt(6, booth.getBoothId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la caseta " + booth.getBoothId(), e);
        }
    }

    /**
     * Elimina una caseta por su ID.
     * @param boothId ID de la caseta.
     * @return true si se eliminó; false si no existe una caseta con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene tiquetes asociados).
     */
    @Override
    public boolean delete(Integer boothId) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, boothId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar la caseta " + boothId, e);
        }
    }

    /**
     * Lista las casetas de una estación de peaje.
     * @param tollId ID del peaje.
     * @return Casetas del peaje, ordenadas por número.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public List<TollBooth> findByToll(int tollId) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TOLL)) {

            ps.setInt(1, tollId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las casetas del peaje " + tollId, e);
        }
    }

    /**
     * Cambia solo el estado de una caseta.
     * @param boothId ID de la caseta.
     * @param status ACTIVA, INACTIVA o MANTENIMIENTO.
     * @return true si se actualizó; false si no existe una caseta con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    public boolean updateStatus(int boothId, String status) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, status);
            ps.setInt(2, boothId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado de la caseta " + boothId, e);
        }
    }

    /** Asigna los 5 campos que no son la llave (índices 1 a 5), comunes a INSERT y UPDATE. */
    private void setFields(PreparedStatement ps, TollBooth c) throws SQLException {
        ps.setInt(1, c.getBoothNumber());
        ps.setString(2, c.getPaymentType());
        ps.setString(3, c.getDirection());
        ps.setString(4, c.getStatus());
        ps.setInt(5, c.getTollId());
    }

    private List<TollBooth> mapList(ResultSet rs) throws SQLException {
        List<TollBooth> booths = new ArrayList<>();
        while (rs.next()) {
            booths.add(mapRow(rs));
        }
        return booths;
    }

    private TollBooth mapRow(ResultSet rs) throws SQLException {
        return new TollBooth(
                rs.getInt("ID_CASETA"),
                rs.getInt("NUMERO_CASETA"),
                rs.getString("TIPO_PAGO"),
                rs.getString("SENTIDO"),
                rs.getString("ESTADO"),
                rs.getInt("ID_PEAJE"));
    }
}