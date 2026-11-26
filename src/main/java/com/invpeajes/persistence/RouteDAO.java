package com.invpeajes.persistence;

import com.invpeajes.model.Route;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import oracle.jdbc.OracleTypes;

/**
 * Implementación JDBC del DAO de Ruta sobre Oracle (tabla RUTA).
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega las consultas propias de Ruta.
 *
 * Las columnas de municipio se nombran explícitamente (ID_MUN_ORIGEN, ID_MUN_DESTINO)
 * porque en la tabla están en orden inverso al del constructor de {@link Route}.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class RouteDAO implements CrudDAO<Route, String> {

    private static final String COLUMNS = "ID_RUTA, NOMBRE_RUTA, ID_MUN_ORIGEN, ID_MUN_DESTINO";

    private static final String SQL_INSERT =
            "INSERT INTO RUTA (" + COLUMNS + ") VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM RUTA";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_RUTA = ?";

    /** Todas las rutas, por nombre (función con cursor). */
    private static final String SQL_LISTAR_RUTAS = "{ ? = call fun_listar_rutas }";
    /** Rutas que tienen peajes: opciones del filtro del mapa (función con cursor). */
    private static final String SQL_RUTAS_CON_PEAJES = "{ ? = call fun_rutas_con_peajes }";
    /** Rutas que pasan entre un origen y un destino (función con cursor). */
    private static final String SQL_RUTAS_ENTRE = "{ ? = call fun_rutas_entre(?, ?, ?, ?, ?) }";

    private static final String SQL_FIND_BY_MUNICIPALITY =
            SQL_SELECT + " WHERE ID_MUN_ORIGEN = ? OR ID_MUN_DESTINO = ? ORDER BY NOMBRE_RUTA";

    private static final String SQL_UPDATE =
            "UPDATE RUTA SET NOMBRE_RUTA = ?, ID_MUN_ORIGEN = ?, ID_MUN_DESTINO = ? "
          + "WHERE ID_RUTA = ?";

    private static final String SQL_DELETE = "DELETE FROM RUTA WHERE ID_RUTA = ?";

    /**
     * Inserta una nueva ruta.
     * @param route Ruta a guardar.
     * @throws DataAccessException Si falla la inserción (por ejemplo, ID repetido
     *                             o municipio inexistente).
     */
    @Override
    public void insert(Route route) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, route.getRouteId());
            setFields(ps, route, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la ruta " + route.getRouteId(), e);
        }
    }

    /**
     * Busca una ruta por su ID.
     * @param routeId ID de la ruta.
     * @return La ruta si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Route> findById(String routeId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar la ruta " + routeId, e);
        }
    }

    /**
     * Lista todas las rutas ordenadas por nombre.
     * @return Lista de rutas (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Route> findAll() {
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(SQL_LISTAR_RUTAS)) {

            cs.registerOutParameter(1, OracleTypes.CURSOR); // lo que devuelve fun_listar_rutas
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las rutas", e);
        }
    }

    /**
     * Rutas que tienen al menos un peaje ({@code fun_rutas_con_peajes}).
     * Son las opciones del filtro "Ruta" del mapa.
     *
     * @return rutas con peajes, por nombre (sin municipios de origen y destino)
     * @throws DataAccessException si falla la consulta
     */
    public List<Route> findWithTolls() {
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(SQL_RUTAS_CON_PEAJES)) {

            cs.registerOutParameter(1, OracleTypes.CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return mapIdAndName(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las rutas con peajes", e);
        }
    }

    /**
     * Rutas que pasan entre un origen y un destino ({@code fun_rutas_entre}): las que tienen
     * peajes en el corredor de {@code corridorKm} km, primero las que tienen más peajes.
     * Son las opciones del combo "Ruta" de Planificar viaje.
     *
     * @param originLat  latitud del origen
     * @param originLon  longitud del origen
     * @param destLat    latitud del destino
     * @param destLon    longitud del destino
     * @param corridorKm ancho del corredor en km
     * @return rutas del trayecto (sin municipios de origen y destino)
     * @throws DataAccessException si falla la consulta
     */
    public List<Route> findBetween(double originLat, double originLon, double destLat, double destLon,
                                   double corridorKm) {
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(SQL_RUTAS_ENTRE)) {

            cs.registerOutParameter(1, OracleTypes.CURSOR);
            cs.setDouble(2, originLat);  // x_lat_origen
            cs.setDouble(3, originLon);  // x_lon_origen
            cs.setDouble(4, destLat);    // x_lat_destino
            cs.setDouble(5, destLon);    // x_lon_destino
            cs.setDouble(6, corridorKm); // x_km
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return mapIdAndName(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las rutas del trayecto", e);
        }
    }

    /** Cursores que solo traen ID_RUTA y NOMBRE_RUTA (y el número de peajes). */
    private List<Route> mapIdAndName(ResultSet rs) throws SQLException {
        List<Route> routes = new ArrayList<>();
        while (rs.next()) {
            routes.add(new Route(rs.getString("ID_RUTA"), rs.getString("NOMBRE_RUTA"), null, null));
        }
        return routes;
    }

    /**
     * Actualiza los datos de una ruta existente.
     * @param route Ruta con los datos nuevos.
     * @return true si se actualizó; false si no existe una ruta con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Route route) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, route, 1);
            ps.setString(4, route.getRouteId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la ruta " + route.getRouteId(), e);
        }
    }

    /**
     * Elimina una ruta por su ID.
     * @param routeId ID de la ruta.
     * @return true si se eliminó; false si no existe una ruta con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene peajes asociados).
     */
    @Override
    public boolean delete(String routeId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setString(1, routeId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar la ruta " + routeId, e);
        }
    }

    /**
     * Lista las rutas que inician o terminan en un municipio.
     * @param municipalityId Código del municipio (5 caracteres, ej. "20001").
     * @return Rutas que pasan por ese municipio como origen o destino, ordenadas por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public List<Route> findByMunicipality(String municipalityId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_MUNICIPALITY)) {

            ps.setString(1, municipalityId);
            ps.setString(2, municipalityId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar las rutas del municipio " + municipalityId, e);
        }
    }

    /** Asigna nombre, origen y destino empezando en el índice {@code start}. */
    private void setFields(PreparedStatement ps, Route r, int start) throws SQLException {
        ps.setString(start, r.getName());
        ps.setString(start + 1, r.getOriginMunicipalityId());
        ps.setString(start + 2, r.getDestinationMunicipalityId());
    }

    private List<Route> mapList(ResultSet rs) throws SQLException {
        List<Route> routes = new ArrayList<>();
        while (rs.next()) {
            routes.add(mapRow(rs));
        }
        return routes;
    }

    private Route mapRow(ResultSet rs) throws SQLException {
        return new Route(
                rs.getString("ID_RUTA"),
                rs.getString("NOMBRE_RUTA"),
                rs.getString("ID_MUN_ORIGEN"),
                rs.getString("ID_MUN_DESTINO"));
    }
}