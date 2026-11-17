package com.invpeajes.persistence;

import com.invpeajes.model.HealthCenter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de HealthCenter sobre Oracle (tabla CENTRO_SALUD).
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega las consultas que usa el mapa.
 *
 * ID_CENTRO es una columna IDENTITY: Oracle la genera al insertar, por eso
 * no se envía en el INSERT.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class HealthCenterDAO implements CrudDAO<HealthCenter, Integer> {

    private static final String DATA_COLUMNS =
            "COD_HABILITACION, NOMBRE_SEDE, PRESTADOR, TIPO, ES_ESE, DEPARTAMENTO, MUNICIPIO, "
          + "COD_DIVIPOLA, DIRECCION, TELEFONO, LATITUD, LONGITUD, PRECISION_COORD";

    private static final String SQL_INSERT =
            "INSERT INTO CENTRO_SALUD (" + DATA_COLUMNS + ") "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT ID_CENTRO, " + DATA_COLUMNS + " FROM CENTRO_SALUD";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_CENTRO = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY DEPARTAMENTO, MUNICIPIO, NOMBRE_SEDE";

    private static final String SQL_FIND_BY_DEPARTMENT =
            SQL_SELECT + " WHERE UPPER(DEPARTAMENTO) = UPPER(?) ORDER BY MUNICIPIO, NOMBRE_SEDE";

    private static final String SQL_FIND_BY_MUNICIPALITY =
            SQL_SELECT + " WHERE COD_DIVIPOLA = ? ORDER BY NOMBRE_SEDE";

    private static final String SQL_FIND_BY_TYPE =
            SQL_SELECT + " WHERE TIPO = ? ORDER BY DEPARTAMENTO, NOMBRE_SEDE";

    private static final String SQL_FIND_IN_AREA =
            SQL_SELECT + " WHERE LATITUD BETWEEN ? AND ? AND LONGITUD BETWEEN ? AND ?";

    private static final String SQL_UPDATE =
            "UPDATE CENTRO_SALUD SET COD_HABILITACION = ?, NOMBRE_SEDE = ?, PRESTADOR = ?, TIPO = ?, "
          + "ES_ESE = ?, DEPARTAMENTO = ?, MUNICIPIO = ?, COD_DIVIPOLA = ?, DIRECCION = ?, "
          + "TELEFONO = ?, LATITUD = ?, LONGITUD = ?, PRECISION_COORD = ? WHERE ID_CENTRO = ?";

    private static final String SQL_DELETE = "DELETE FROM CENTRO_SALUD WHERE ID_CENTRO = ?";

    private static final int FIELD_COUNT = 13;

    /**
     * Inserta un centro de salud nuevo.
     *
     * @param center centro a guardar
     * @throws DataAccessException si falla la inserción (por ejemplo, código de habilitación repetido)
     */
    @Override
    public void insert(HealthCenter center) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            setFields(ps, center);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el centro de salud " + center.getName(), e);
        }
    }

    /**
     * Busca un centro de salud por su ID.
     *
     * @param healthCenterId ID del centro
     * @return el centro si existe, o {@code Optional.empty()}
     * @throws DataAccessException si falla la consulta
     */
    @Override
    public Optional<HealthCenter> findById(Integer healthCenterId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, healthCenterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el centro de salud " + healthCenterId, e);
        }
    }

    /**
     * Lista todos los centros de salud.
     *
     * @return lista de centros (vacía si no hay registros)
     * @throws DataAccessException si falla la consulta
     */
    @Override
    public List<HealthCenter> findAll() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los centros de salud", e);
        }
    }

    /**
     * Actualiza los datos de un centro de salud.
     *
     * @param center centro con los datos nuevos
     * @return true si se actualizó; false si no existe un centro con ese ID
     * @throws DataAccessException si falla la actualización
     */
    @Override
    public boolean update(HealthCenter center) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, center);
            ps.setInt(FIELD_COUNT + 1, center.getHealthCenterId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el centro de salud " + center.getHealthCenterId(), e);
        }
    }

    /**
     * Elimina un centro de salud por su ID.
     *
     * @param healthCenterId ID del centro
     * @return true si se eliminó; false si no existe
     * @throws DataAccessException si falla el borrado
     */
    @Override
    public boolean delete(Integer healthCenterId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, healthCenterId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el centro de salud " + healthCenterId, e);
        }
    }

    /**
     * Lista las sedes de un departamento.
     *
     * @param department nombre del departamento (sin importar mayúsculas)
     * @return sedes del departamento, ordenadas por municipio y nombre
     * @throws DataAccessException si falla la consulta
     */
    public List<HealthCenter> findByDepartment(String department) {
        return queryByText(SQL_FIND_BY_DEPARTMENT, department,
                "No se pudo listar los centros de salud del departamento " + department);
    }

    /**
     * Lista las sedes de un municipio.
     *
     * @param divipolaCode código DANE del municipio (5 caracteres, ej. "20001")
     * @return sedes del municipio, ordenadas por nombre
     * @throws DataAccessException si falla la consulta
     */
    public List<HealthCenter> findByMunicipality(String divipolaCode) {
        return queryByText(SQL_FIND_BY_MUNICIPALITY, divipolaCode,
                "No se pudo listar los centros de salud del municipio " + divipolaCode);
    }

    /**
     * Lista las sedes de un tipo.
     *
     * @param type Hospital, Centro de salud, Puesto de salud u Otro / Unidad de salud
     * @return sedes de ese tipo
     * @throws DataAccessException si falla la consulta
     */
    public List<HealthCenter> findByType(String type) {
        return queryByText(SQL_FIND_BY_TYPE, type,
                "No se pudo listar los centros de salud de tipo " + type);
    }

    /**
     * Lista las sedes dentro de un rectángulo geográfico.
     *
     * @param latMin latitud mínima
     * @param latMax latitud máxima
     * @param lonMin longitud mínima
     * @param lonMax longitud máxima
     * @return sedes dentro del rectángulo
     * @throws DataAccessException si falla la consulta
     */
    public List<HealthCenter> findInArea(double latMin, double latMax, double lonMin, double lonMax) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_IN_AREA)) {

            ps.setDouble(1, latMin);
            ps.setDouble(2, latMax);
            ps.setDouble(3, lonMin);
            ps.setDouble(4, lonMax);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los centros de salud de la zona", e);
        }
    }

    /** Ejecuta una consulta que recibe un solo parámetro de texto. */
    private List<HealthCenter> queryByText(String sql, String value, String errorMessage) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    /** Asigna los 13 campos que comparten INSERT y UPDATE (posiciones 1 a 13). */
    private void setFields(PreparedStatement ps, HealthCenter c) throws SQLException {
        ps.setString(1, c.getHabilitationCode());
        ps.setString(2, c.getName());
        ps.setString(3, c.getProvider());
        ps.setString(4, c.getType());
        ps.setString(5, c.getEse());
        ps.setString(6, c.getDepartment());
        ps.setString(7, c.getMunicipality());
        ps.setString(8, c.getDivipolaCode());
        ps.setString(9, c.getAddress());
        ps.setString(10, c.getPhone());
        ps.setDouble(11, c.getLatitude());
        ps.setDouble(12, c.getLongitude());
        ps.setString(13, c.getPrecision());
    }

    private List<HealthCenter> mapList(ResultSet rs) throws SQLException {
        List<HealthCenter> centers = new ArrayList<>();
        while (rs.next()) {
            centers.add(mapRow(rs));
        }
        return centers;
    }

    private HealthCenter mapRow(ResultSet rs) throws SQLException {
        return new HealthCenter(
                rs.getInt("ID_CENTRO"),
                rs.getString("COD_HABILITACION"),
                rs.getString("NOMBRE_SEDE"),
                rs.getString("PRESTADOR"),
                rs.getString("TIPO"),
                rs.getString("ES_ESE"),
                rs.getString("DEPARTAMENTO"),
                rs.getString("MUNICIPIO"),
                rs.getString("COD_DIVIPOLA"),
                rs.getString("DIRECCION"),
                rs.getString("TELEFONO"),
                rs.getDouble("LATITUD"),
                rs.getDouble("LONGITUD"),
                rs.getString("PRECISION_COORD"));
    }
}
