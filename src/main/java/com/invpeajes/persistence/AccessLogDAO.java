package com.invpeajes.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Bitácora de inicios de sesión (tabla {@code PEAJE.BITACORA_ACCESO}).
 *
 * <p>Usa el procedimiento {@code pkg_seguridad.Pr_registrar_acceso}: guarda cada intento
 * y, si un empleado falla 3 veces seguidas en 15 minutos, la base de datos lo bloquea
 * ({@code USUARIO_PEAJE.ACTIVO = 'N'}). Solo un superior lo puede reactivar con
 * {@code pkg_seguridad.Pr_reactivar_usuario}.</p>
 *
 * @author Salomejimenez
 */
public class AccessLogDAO {

    public static final String EMPLOYEE = "EMPLEADO";
    public static final String TRAVELER = "VIAJERO";

    private static final String REGISTER_SQL = "{call pkg_seguridad.Pr_registrar_acceso(?, ?, ?)}";
    private static final String BLOCKED_SQL =
            "SELECT ACTIVO FROM PEAJE.USUARIO_PEAJE WHERE CEDULA = ?";

    /**
     * Guarda un intento de inicio de sesión.
     *
     * @param identifier cédula del empleado o correo del viajero
     * @param userType   {@link #EMPLOYEE} o {@link #TRAVELER}
     * @param successful true si la contraseña fue correcta
     * @throws DataAccessException si falla la base de datos
     */
    public void register(String identifier, String userType, boolean successful) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(REGISTER_SQL)) {
            cs.setString(1, identifier);
            cs.setString(2, userType);
            cs.setString(3, successful ? "S" : "N");
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el inicio de sesión", e);
        }
    }

    /**
     * Indica si la cuenta de un empleado está bloqueada.
     *
     * @param employeeId cédula del empleado
     * @return true si existe y tiene {@code ACTIVO = 'N'}
     * @throws DataAccessException si falla la base de datos
     */
    public boolean isEmployeeBlocked(String employeeId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BLOCKED_SQL)) {
            ps.setString(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "N".equals(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el estado de la cuenta " + employeeId, e);
        }
    }
}
