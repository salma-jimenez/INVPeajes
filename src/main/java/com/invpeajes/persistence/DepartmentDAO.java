package com.invpeajes.persistence;

import com.invpeajes.model.Department;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la tabla {@code PEAJE.DEPARTAMENTOS}.
 * Los departamentos se cargan con el script de datos y casi no cambian,
 * pero el DAO ofrece el CRUD completo como los demás.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class DepartmentDAO implements CrudDAO<Department, String> {

    private static final String COLUMNS = "ID_DEPARTAMENTO, NOMBRE_DEPAR";

    private static final String SQL_INSERT
            = "INSERT INTO PEAJE.DEPARTAMENTOS (" + COLUMNS + ") VALUES (?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM PEAJE.DEPARTAMENTOS";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_DEPARTAMENTO = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY NOMBRE_DEPAR";

    private static final String SQL_UPDATE
            = "UPDATE PEAJE.DEPARTAMENTOS SET NOMBRE_DEPAR = ? WHERE ID_DEPARTAMENTO = ?";

    private static final String SQL_DELETE
            = "DELETE FROM PEAJE.DEPARTAMENTOS WHERE ID_DEPARTAMENTO = ?";

    /**
     * Inserta un departamento.
     *
     * @param department Departamento a guardar.
     * @throws DataAccessException Si falla la inserción.
     */
    @Override
    public void insert(Department department) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, department.getDepartmentId());
            ps.setString(2, department.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el departamento " + department.getDepartmentId(), e);
        }
    }

    /**
     * Busca un departamento por su código DANE de dos dígitos.
     *
     * @param departmentId Código del departamento (ej. "05").
     * @return El departamento, o vacío si no existe.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Department> findById(String departmentId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el departamento " + departmentId, e);
        }
    }

    /**
     * Lista todos los departamentos ordenados por nombre.
     *
     * @return Departamentos registrados.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Department> findAll() {
        List<Department> departments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                departments.add(mapRow(rs));
            }
            return departments;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron listar los departamentos", e);
        }
    }

    /**
     * Cambia el nombre de un departamento.
     *
     * @param department Departamento con el nombre nuevo.
     * @return true si se actualizó.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Department department) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, department.getName());
            ps.setString(2, department.getDepartmentId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el departamento " + department.getDepartmentId(), e);
        }
    }

    /**
     * Elimina un departamento (falla si tiene peajes o municipios asociados).
     *
     * @param departmentId Código del departamento.
     * @return true si se eliminó.
     * @throws DataAccessException Si falla la eliminación.
     */
    @Override
    public boolean delete(String departmentId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, departmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el departamento " + departmentId, e);
        }
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(rs.getString("ID_DEPARTAMENTO"), rs.getString("NOMBRE_DEPAR"));
    }
}
