package com.invpeajes.persistence;

import com.invpeajes.model.Municipality;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Municipio sobre Oracle (tabla {@code PEAJE.MUNICIPIOS}).
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega consultas por departamento y nombre.
 *
 * @author Luis Guerrero
 * @version 1.1
 */
public class MunicipalityDAO implements CrudDAO<Municipality, String> {

    private static final String COLUMNS =
            "ID_MUNICIPIOS, NOMBRE_MUNICIPIO, ID_DEPARTAMENTO, LATITUD, LONGITUD";

    private static final String SQL_INSERT =
            "INSERT INTO PEAJE.MUNICIPIOS (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT =
            "SELECT " + COLUMNS + " FROM PEAJE.MUNICIPIOS";

    private static final String SQL_FIND_BY_ID =
            SQL_SELECT + " WHERE ID_MUNICIPIOS = ?";

    private static final String SQL_FIND_ALL =
            SQL_SELECT + " ORDER BY NOMBRE_MUNICIPIO";

    private static final String SQL_FIND_BY_DEPARTMENT =
            SQL_SELECT + " WHERE ID_DEPARTAMENTO = ? ORDER BY NOMBRE_MUNICIPIO";

    private static final String SQL_FIND_BY_NAME =
            SQL_SELECT + " WHERE UPPER(NOMBRE_MUNICIPIO) = UPPER(?) ORDER BY ID_DEPARTAMENTO";

    private static final String SQL_UPDATE =
            "UPDATE PEAJE.MUNICIPIOS SET NOMBRE_MUNICIPIO = ?, ID_DEPARTAMENTO = ?, "
                    + "LATITUD = ?, LONGITUD = ? WHERE ID_MUNICIPIOS = ?";

    private static final String SQL_DELETE =
            "DELETE FROM PEAJE.MUNICIPIOS WHERE ID_MUNICIPIOS = ?";

    @Override
    public void insert(Municipality municipality) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, municipality.getMunicipalityId());
            ps.setString(2, municipality.getName());
            ps.setString(3, municipality.getDepartmentId());
            ps.setDouble(4, municipality.getLatitude());
            ps.setDouble(5, municipality.getLongitude());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el municipio " + municipality.getName(), e);
        }
    }

    @Override
    public Optional<Municipality> findById(String municipalityId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, municipalityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el municipio " + municipalityId, e);
        }
    }

    @Override
    public List<Municipality> findAll() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron listar los municipios", e);
        }
    }

    public List<Municipality> findByDepartment(String departmentId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DEPARTMENT)) {
            ps.setString(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron listar los municipios del departamento "
                    + departmentId, e);
        }
    }

    public List<Municipality> findByName(String name) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el municipio " + name, e);
        }
    }

    @Override
    public boolean update(Municipality municipality) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, municipality.getName());
            ps.setString(2, municipality.getDepartmentId());
            ps.setDouble(3, municipality.getLatitude());
            ps.setDouble(4, municipality.getLongitude());
            ps.setString(5, municipality.getMunicipalityId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el municipio "
                    + municipality.getMunicipalityId(), e);
        }
    }

    @Override
    public boolean delete(String municipalityId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, municipalityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el municipio " + municipalityId, e);
        }
    }

    private List<Municipality> mapList(ResultSet rs) throws SQLException {
        List<Municipality> municipalities = new ArrayList<>();
        while (rs.next()) {
            municipalities.add(mapRow(rs));
        }
        return municipalities;
    }

    private Municipality mapRow(ResultSet rs) throws SQLException {
        return new Municipality(
                rs.getString("ID_MUNICIPIOS"),
                rs.getString("NOMBRE_MUNICIPIO"),
                rs.getString("ID_DEPARTAMENTO"),
                rs.getDouble("LATITUD"),
                rs.getDouble("LONGITUD"));
    }
}