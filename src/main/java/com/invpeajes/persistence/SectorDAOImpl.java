package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Sector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Sector sobre Oracle (tabla SECTOR).
 * Cumple el contrato de {@link ISectorDAO}.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class SectorDAOImpl implements ISectorDAO {

    private static final String COLUMNS =
            "ID_SECTOR, NOMBRE_SECTOR, ID_MUNICIPIO_ORIGEN, ID_MUNICIPIO_DESTINO";

    private static final String SQL_INSERT =
            "INSERT INTO SECTOR (" + COLUMNS + ") VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM SECTOR";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_SECTOR = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY NOMBRE_SECTOR";

    private static final String SQL_FIND_BY_NAME =
            SQL_SELECT + " WHERE UPPER(NOMBRE_SECTOR) LIKE UPPER(?) ORDER BY NOMBRE_SECTOR";

    private static final String SQL_UPDATE =
            "UPDATE SECTOR SET NOMBRE_SECTOR = ?, ID_MUNICIPIO_ORIGEN = ?, ID_MUNICIPIO_DESTINO = ? "
          + "WHERE ID_SECTOR = ?";

    private static final String SQL_DELETE = "DELETE FROM SECTOR WHERE ID_SECTOR = ?";

    /**
     * Inserta un nuevo sector.
     * @param sector Sector a guardar (el ID se envía, no lo genera Oracle).
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID repetido
     *                             o municipio inexistente).
     */
    @Override
    public void insert(Sector sector) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, sector.getIdSector());
            setFields(ps, sector, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el sector " + sector.getIdSector(), e);
        }
    }

    /**
     * Busca un sector por su ID.
     * @param idSector ID del sector.
     * @return El sector si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Sector> findById(Integer idSector) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, idSector);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el sector " + idSector, e);
        }
    }

    /**
     * Lista todos los sectores ordenados por nombre.
     * @return Lista de sectores (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Sector> findAll() {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los sectores", e);
        }
    }

    /**
     * Actualiza los datos de un sector existente.
     * @param sector Sector con los datos nuevos.
     * @return true si se actualizó; false si no existe un sector con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Sector sector) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, sector, 1);
            ps.setInt(4, sector.getIdSector());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el sector " + sector.getIdSector(), e);
        }
    }

    /**
     * Elimina un sector por su ID.
     * @param idSector ID del sector.
     * @return true si se eliminó; false si no existe un sector con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene peajes asociados).
     */
    @Override
    public boolean delete(Integer idSector) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idSector);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el sector " + idSector, e);
        }
    }

    @Override
    public List<Sector> findByNameContaining(String text) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar sectores con \"" + text + "\"", e);
        }
    }

    /** Asigna nombre, origen y destino empezando en el índice {@code start}. */
    private void setFields(PreparedStatement ps, Sector s, int start) throws SQLException {
        ps.setString(start, s.getNombreSector());
        ps.setString(start + 1, s.getMunicipioOrigenId());
        ps.setString(start + 2, s.getMunicipioDestinoId());
    }

    private List<Sector> mapList(ResultSet rs) throws SQLException {
        List<Sector> sectores = new ArrayList<>();
        while (rs.next()) {
            sectores.add(mapRow(rs));
        }
        return sectores;
    }

    private Sector mapRow(ResultSet rs) throws SQLException {
        return new Sector(
                rs.getInt("ID_SECTOR"),
                rs.getString("NOMBRE_SECTOR"),
                rs.getString("ID_MUNICIPIO_ORIGEN"),
                rs.getString("ID_MUNICIPIO_DESTINO"));
    }
}