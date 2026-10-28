package com.invpeajes.persistence;

import com.invpeajes.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * Acceso a datos de la tabla {@code PEAJE.VEHICULOS}.
 *
 * @author Salomejimenez
 */
public class VehicleDAOImpl implements IVehicleDAO {

    private static final String INSERT_SQL
            = "INSERT INTO PEAJE.VEHICULOS (PLACA, TIPO, MARCA, CODIGO_TAG, ID_INTERMEDIADOR, ID_CATEGORIA) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_PLATE_SQL
            = "SELECT * FROM PEAJE.VEHICULOS WHERE PLACA = ?";
    private static final String FIND_BY_TAG_SQL
            = "SELECT * FROM PEAJE.VEHICULOS WHERE CODIGO_TAG = ?";
    private static final String FIND_ALL_SQL
            = "SELECT * FROM PEAJE.VEHICULOS ORDER BY PLACA";
    private static final String FIND_BY_CATEGORY_SQL
            = "SELECT * FROM PEAJE.VEHICULOS WHERE ID_CATEGORIA = ? ORDER BY PLACA";
    private static final String UPDATE_SQL
            = "UPDATE PEAJE.VEHICULOS SET TIPO = ?, MARCA = ?, CODIGO_TAG = ?, ID_INTERMEDIADOR = ?, "
            + "ID_CATEGORIA = ? WHERE PLACA = ?";
    private static final String UPDATE_TAG_SQL
            = "UPDATE PEAJE.VEHICULOS SET CODIGO_TAG = ?, ID_INTERMEDIADOR = ? WHERE PLACA = ?";
    private static final String DELETE_SQL
            = "DELETE FROM PEAJE.VEHICULOS WHERE PLACA = ?";

    @Override
    public void insert(Vehicle vehicle) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            ps.setString(1, vehicle.getPlate());
            ps.setString(2, vehicle.getType());
            ps.setString(3, vehicle.getBrand());
            ps.setString(4, vehicle.getTagCode());
            JdbcUtils.setNullableInt(ps, 5, vehicle.getIntermediaryId());
            ps.setInt(6, vehicle.getCategoryId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar el vehículo " + vehicle.getPlate(), e);
        }
    }

    @Override
    public Optional<Vehicle> findById(String plate) {
        return findOne(FIND_BY_PLATE_SQL, plate, "Error al buscar la placa " + plate);
    }

    @Override
    public Optional<Vehicle> findByTag(String tagCode) {
        return findOne(FIND_BY_TAG_SQL, tagCode, "Error al buscar el tag " + tagCode);
    }

    @Override
    public List<Vehicle> findAll() {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(FIND_ALL_SQL); ResultSet rs = ps.executeQuery()) {
            return mapRows(rs);
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los vehículos", e);
        }
    }

    @Override
    public List<Vehicle> findByCategory(int categoryId) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(FIND_BY_CATEGORY_SQL)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar vehículos de la categoría " + categoryId, e);
        }
    }

    @Override
    public boolean update(Vehicle vehicle) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, vehicle.getType());
            ps.setString(2, vehicle.getBrand());
            ps.setString(3, vehicle.getTagCode());
            JdbcUtils.setNullableInt(ps, 4, vehicle.getIntermediaryId());
            ps.setInt(5, vehicle.getCategoryId());
            ps.setString(6, vehicle.getPlate());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el vehículo " + vehicle.getPlate(), e);
        }
    }

    @Override
    public boolean updateTag(String plate, String tagCode, Integer intermediaryId) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(UPDATE_TAG_SQL)) {
            ps.setString(1, tagCode);
            JdbcUtils.setNullableInt(ps, 2, intermediaryId);
            ps.setString(3, plate);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el tag de " + plate, e);
        }
    }

    @Override
    public boolean delete(String plate) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
            ps.setString(1, plate);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar el vehículo " + plate, e);
        }
    }

    private Optional<Vehicle> findOne(String sql, String value, String errorMessage) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    private List<Vehicle> mapRows(ResultSet rs) throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        while (rs.next()) {
            vehicles.add(mapRow(rs));
        }
        return vehicles;
    }

    private Vehicle mapRow(ResultSet rs) throws SQLException {
        return new Vehicle(rs.getString("PLACA"), rs.getString("TIPO"), rs.getString("MARCA"),
                rs.getString("CODIGO_TAG"), JdbcUtils.getNullableInt(rs, "ID_INTERMEDIADOR"),
                rs.getInt("ID_CATEGORIA"));
    }

}
