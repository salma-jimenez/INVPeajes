package com.invpeajes.persistence;

import com.invpeajes.model.CashierSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Implementación JDBC de {@link ICashierDAO} sobre las tablas USUARIO_PEAJE, PEAJE y CONCESIONARIO.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class CashierDAOImpl implements ICashierDAO {

    private static final String SQL_FIND_OPERATOR =
            "SELECT u.CEDULA, u.PRIMER_NOMBRE, u.PRIMER_APELLIDO, u.CARGO, "
          + "p.ID_PEAJE, p.NOMBRE, p.ESTADO_OPERATIVO, p.TELEFONO, p.TELEFONO_GRUA, "
          + "c.NOMBRE AS CONCESIONARIO "
          + "FROM PEAJE.USUARIO_PEAJE u LEFT JOIN PEAJE.PEAJE p ON p.ID_PEAJE = u.ID_PEAJE "
          + "LEFT JOIN PEAJE.CONCESIONARIO c ON c.ID_CONCESIONARIO = p.ID_CONCESIONARIO "
          + "WHERE u.CEDULA = ?";

    private static final String SQL_FIND_PASSWORD =
            "SELECT CONTRASENA_HASH FROM PEAJE.USUARIO_PEAJE WHERE CEDULA = ? AND ACTIVO = 'S'";

    @Override
    public Optional<CashierSession> findOperator(String operatorId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_OPERATOR)) {

            ps.setString(1, operatorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String name = rs.getString("PRIMER_NOMBRE") + " " + rs.getString("PRIMER_APELLIDO");
                return Optional.of(new CashierSession(rs.getString("CEDULA"), name,
                        rs.getString("CARGO"), rs.getInt("ID_PEAJE"), rs.getString("NOMBRE"),
                        rs.getString("ESTADO_OPERATIVO"), rs.getString("TELEFONO"),
                        rs.getString("TELEFONO_GRUA"), rs.getString("CONCESIONARIO"), null));
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el empleado " + operatorId, e);
        }
    }

    @Override
    public Optional<String> findPasswordHash(String operatorId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PASSWORD)) {

            ps.setString(1, operatorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar el empleado " + operatorId, e);
        }
    }
}
