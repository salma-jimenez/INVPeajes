package com.invpeajes.persistence;

import com.invpeajes.model.VehicleCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la tabla {@code PEAJE.CATEGORIA_VEHICULAR}.
 *
 * @author Salomejimenez
 */
public class VehicleCategoryDAOImpl implements IVehicleCategoryDAO {

    private static final String INSERT_SQL
            = "INSERT INTO PEAJE.CATEGORIA_VEHICULAR (ID_CATEGORIA, CATEGORIA, DESCRIPCION, NUMERO_EJES) "
            + "VALUES (?, ?, ?, ?)";
    private static final String FIND_BY_ID_SQL
            = "SELECT * FROM PEAJE.CATEGORIA_VEHICULAR WHERE ID_CATEGORIA = ?";
    private static final String FIND_ALL_SQL
            = "SELECT * FROM PEAJE.CATEGORIA_VEHICULAR ORDER BY ID_CATEGORIA";
    private static final String UPDATE_SQL
            = "UPDATE PEAJE.CATEGORIA_VEHICULAR SET CATEGORIA = ?, DESCRIPCION = ?, NUMERO_EJES = ? "
            + "WHERE ID_CATEGORIA = ?";
    private static final String DELETE_SQL
            = "DELETE FROM PEAJE.CATEGORIA_VEHICULAR WHERE ID_CATEGORIA = ?";

    @Override
    public void insert(VehicleCategory category) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, category.getCategoryId());
            ps.setString(2, category.getCategoryCode());
            ps.setString(3, category.getDescription());
            JdbcUtils.setNullableInt(ps, 4, category.getAxleCount());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar la categoría", e);
        }
    }

    @Override
    public Optional<VehicleCategory> findById(Integer id) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(FIND_BY_ID_SQL)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar la categoría " + id, e);
        }
    }

    @Override
    public List<VehicleCategory> findAll() {
        List<VehicleCategory> categories = new ArrayList<>();
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(FIND_ALL_SQL); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar las categorías", e);
        }
    }

    @Override
    public boolean update(VehicleCategory category) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, category.getCategoryCode());
            ps.setString(2, category.getDescription());
            JdbcUtils.setNullableInt(ps, 3, category.getAxleCount());
            ps.setInt(4, category.getCategoryId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar la categoría", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        try (Connection con = VehicleConnectionFactory.open(); PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar la categoría " + id, e);
        }
    }

    private VehicleCategory mapRow(ResultSet rs) throws SQLException {
        return new VehicleCategory(rs.getInt("ID_CATEGORIA"), rs.getString("CATEGORIA"),
                rs.getString("DESCRIPCION"), JdbcUtils.getNullableInt(rs, "NUMERO_EJES"));
    }
}