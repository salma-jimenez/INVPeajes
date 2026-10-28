package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Ruta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Ruta sobre Oracle (tabla RUTA).
 * Cumple el contrato de {@link IRutaDAO}.
 *
 * Las columnas de municipio se nombran explícitamente (ID_MUN_ORIGEN, ID_MUN_DESTINO)
 * porque en la tabla están en orden inverso al del constructor de {@link Ruta}.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class RutaDAOImpl implements IRutaDAO {

    private static final String COLUMNS = "ID_RUTA, NOMBRE_RUTA, ID_MUN_ORIGEN, ID_MUN_DESTINO";

    private static final String SQL_INSERT =
            "INSERT INTO RUTA (" + COLUMNS + ") VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM RUTA";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_RUTA = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY NOMBRE_RUTA";

    private static final String SQL_FIND_BY_MUNICIPALITY =
            SQL_SELECT + " WHERE ID_MUN_ORIGEN = ? OR ID_MUN_DESTINO = ? ORDER BY NOMBRE_RUTA";

    private static final String SQL_UPDATE =
            "UPDATE RUTA SET NOMBRE_RUTA = ?, ID_MUN_ORIGEN = ?, ID_MUN_DESTINO = ? "
          + "WHERE ID_RUTA = ?";

    private static final String SQL_DELETE = "DELETE FROM RUTA WHERE ID_RUTA = ?";

    /**
     * Inserta una nueva ruta.
     * @param ruta Ruta a guardar.
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID repetido
     *                             o municipio inexistente).
     */
    @Override
    public void insert(Ruta ruta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, ruta.getIdRuta());
            setFields(ps, ruta, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la ruta " + ruta.getIdRuta(), e);
        }
    }

    /**
     * Busca una ruta por su ID.
     * @param idRuta ID de la ruta.
     * @return La ruta si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Ruta> findById(String idRuta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, idRuta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar la ruta " + idRuta, e);
        }
    }

    /**
     * Lista todas las rutas ordenadas por nombre.
     * @return Lista de rutas (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Ruta> findAll() {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las rutas", e);
        }
    }

    /**
     * Actualiza los datos de una ruta existente.
     * @param ruta Ruta con los datos nuevos.
     * @return true si se actualizó; false si no existe una ruta con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Ruta ruta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, ruta, 1);
            ps.setString(4, ruta.getIdRuta());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la ruta " + ruta.getIdRuta(), e);
        }
    }

    /**
     * Elimina una ruta por su ID.
     * @param idRuta ID de la ruta.
     * @return true si se eliminó; false si no existe una ruta con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene peajes asociados).
     */
    @Override
    public boolean delete(String idRuta) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setString(1, idRuta);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar la ruta " + idRuta, e);
        }
    }

    @Override
    public List<Ruta> findByMunicipality(String idMunicipio) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_MUNICIPALITY)) {

            ps.setString(1, idMunicipio);
            ps.setString(2, idMunicipio);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las rutas del municipio " + idMunicipio, e);
        }
    }

    /** Asigna nombre, origen y destino empezando en el índice {@code start}. */
    private void setFields(PreparedStatement ps, Ruta r, int start) throws SQLException {
        ps.setString(start, r.getNombreRuta());
        ps.setString(start + 1, r.getMunicipioOrigenId());
        ps.setString(start + 2, r.getMunicipioDestinoId());
    }

    private List<Ruta> mapList(ResultSet rs) throws SQLException {
        List<Ruta> rutas = new ArrayList<>();
        while (rs.next()) {
            rutas.add(mapRow(rs));
        }
        return rutas;
    }

    private Ruta mapRow(ResultSet rs) throws SQLException {
        return new Ruta(
                rs.getString("ID_RUTA"),
                rs.getString("NOMBRE_RUTA"),
                rs.getString("ID_MUN_ORIGEN"),
                rs.getString("ID_MUN_DESTINO"));
    }
}