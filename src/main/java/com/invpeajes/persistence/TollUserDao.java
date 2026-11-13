package com.invpeajes.persistence;

import com.invpeajes.model.TollUser;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla PEAJE.USUARIO_PEAJE: empleados de los peajes (operadores de caseta, jefes de
 * estación) y administradores de cada entidad.
 *
 * <p>La contraseña nunca se lee ni se guarda en el objeto {@link TollUser}: se cambia con
 * {@link #updatePassword}. Al leer se avisa si la contraseña sigue siendo la cédula
 * ({@link TollUser#isDefaultPassword()}).</p>
 *
 * @author salma
 * @author Salomejimenez
 * @version 2.0
 */
public class TollUserDao {

    private static final String COLUMNS = "u.CEDULA, u.PRIMER_NOMBRE, u.SEGUNDO_NOMBRE, u.PRIMER_APELLIDO, "
            + "u.SEGUNDO_APELLIDO, u.TELEFONO, u.CORREO, u.CARGO, u.FECHA_INGRESO, u.ID_PEAJE, u.ACTIVO, "
            + "u.ENTIDAD, CASE WHEN u.CONTRASENA_HASH = RAWTOHEX(STANDARD_HASH(u.CEDULA || ':' || u.CEDULA, "
            + "'SHA256')) THEN 'S' ELSE 'N' END CLAVE_INICIAL";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLUMNS + " FROM PEAJE.USUARIO_PEAJE u WHERE u.CEDULA = ?";

    private static final String SQL_FIND_BY_TOLL =
            "SELECT " + COLUMNS + " FROM PEAJE.USUARIO_PEAJE u WHERE u.ID_PEAJE = ? AND u.CARGO = ? "
            + "ORDER BY u.ACTIVO DESC, u.PRIMER_NOMBRE, u.PRIMER_APELLIDO";

    private static final String SQL_FIND_BY_LEVEL =
            "SELECT " + COLUMNS + " FROM PEAJE.USUARIO_PEAJE u JOIN PEAJE.PEAJE p ON p.ID_PEAJE = u.ID_PEAJE "
            + "WHERE p.NIVEL = ? ORDER BY CASE u.CARGO WHEN 'JEFE DE ESTACION' THEN 0 ELSE 1 END, p.NOMBRE, "
            + "u.PRIMER_NOMBRE, u.PRIMER_APELLIDO";

    private static final String SQL_INSERT =
            "INSERT INTO PEAJE.USUARIO_PEAJE (CEDULA, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, "
            + "SEGUNDO_APELLIDO, TELEFONO, CORREO, CARGO, FECHA_INGRESO, ID_PEAJE, ACTIVO, ENTIDAD, "
            + "CONTRASENA_HASH) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'S', ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE PEAJE.USUARIO_PEAJE SET PRIMER_NOMBRE = ?, SEGUNDO_NOMBRE = ?, PRIMER_APELLIDO = ?, "
            + "SEGUNDO_APELLIDO = ?, TELEFONO = ?, CORREO = ?, FECHA_INGRESO = ?, ID_PEAJE = ? WHERE CEDULA = ?";

    private static final String SQL_SET_ACTIVE =
            "UPDATE PEAJE.USUARIO_PEAJE SET ACTIVO = ? WHERE CEDULA = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE PEAJE.USUARIO_PEAJE SET CONTRASENA_HASH = ? WHERE CEDULA = ?";

    private static final String SQL_FIND_PASSWORD =
            "SELECT CONTRASENA_HASH FROM PEAJE.USUARIO_PEAJE WHERE CEDULA = ?";

    /**
     * Busca un empleado o administrador por su cédula (activo o no).
     * @param idNumber Cédula.
     * @return El usuario, o vacío si no existe.
     */
    public Optional<TollUser> findById(String idNumber) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, idNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el usuario " + idNumber, e);
        }
    }

    /**
     * Lista los usuarios de un peaje con un cargo (activos primero).
     * @param tollId ID del peaje.
     * @param position Cargo, ej. "OPERADOR DE CASETA".
     * @return Usuarios del peaje con ese cargo.
     */
    public List<TollUser> findByToll(int tollId, String position) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TOLL)) {
            ps.setInt(1, tollId);
            ps.setString(2, position);
            return list(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los empleados del peaje " + tollId, e);
        }
    }

    /**
     * Lista los jefes y operadores de todos los peajes de una entidad (jefes primero).
     * @param level Entidad, igual que PEAJE.NIVEL (ej. "ANI").
     * @return Empleados de los peajes de esa entidad.
     */
    public List<TollUser> findByLevel(String level) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_LEVEL)) {
            ps.setString(1, level);
            return list(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los empleados de " + level, e);
        }
    }

    /**
     * Registra un usuario nuevo, ya activo.
     * @param user Datos del usuario (con cargo, y entidad si es administrador).
     * @param passwordHash Hash de su contraseña inicial.
     */
    public void insert(TollUser user, String passwordHash) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, user.getIdNumber());
            ps.setString(2, user.getFirstName());
            ps.setString(3, user.getMiddleName());
            ps.setString(4, user.getFirstSurname());
            ps.setString(5, user.getSecondSurname());
            ps.setString(6, user.getPhone());
            ps.setString(7, user.getEmail());
            ps.setString(8, user.getPosition());
            setDate(ps, 9, user);
            setToll(ps, 10, user.getTollId());
            ps.setString(11, user.getEntity());
            ps.setString(12, passwordHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar el usuario " + user.getIdNumber(), e);
        }
    }

    /**
     * Actualiza nombres, teléfono, correo, fecha de ingreso y peaje. La cédula, el cargo y la
     * entidad no cambian.
     * @param user Usuario con los datos nuevos.
     * @return true si se actualizó.
     */
    public boolean update(TollUser user) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getMiddleName());
            ps.setString(3, user.getFirstSurname());
            ps.setString(4, user.getSecondSurname());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getEmail());
            setDate(ps, 7, user);
            setToll(ps, 8, user.getTollId());
            ps.setString(9, user.getIdNumber());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el usuario " + user.getIdNumber(), e);
        }
    }

    /**
     * Activa o desactiva un usuario. Desactivado no puede iniciar sesión, pero su historial se
     * conserva.
     * @param idNumber Cédula.
     * @param active true para activar.
     * @return true si se actualizó.
     */
    public boolean setActive(String idNumber, boolean active) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SET_ACTIVE)) {
            ps.setString(1, active ? "S" : "N");
            ps.setString(2, idNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar el estado del usuario " + idNumber, e);
        }
    }

    /**
     * Cambia el hash de la contraseña.
     * @param idNumber Cédula.
     * @param passwordHash Hash nuevo (ver AuthService.hash).
     * @return true si se actualizó.
     */
    public boolean updatePassword(String idNumber, String passwordHash) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setString(2, idNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar la contraseña de " + idNumber, e);
        }
    }

    /**
     * Hash de la contraseña actual (activo o no).
     * @param idNumber Cédula.
     * @return El hash, o vacío si no existe o no tiene contraseña.
     */
    public Optional<String> findPasswordHash(String idNumber) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PASSWORD)) {
            ps.setString(1, idNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar la contraseña de " + idNumber, e);
        }
    }

    // ------------------------------------------------------------------ Apoyo

    private static List<TollUser> list(PreparedStatement ps) throws SQLException {
        List<TollUser> users = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
        }
        return users;
    }

    private static TollUser map(ResultSet rs) throws SQLException {
        Date hired = rs.getDate("FECHA_INGRESO");
        int tollId = rs.getInt("ID_PEAJE");
        Integer toll = rs.wasNull() ? null : tollId;
        TollUser user = new TollUser(rs.getString("CEDULA"), rs.getString("PRIMER_NOMBRE"),
                rs.getString("SEGUNDO_NOMBRE"), rs.getString("PRIMER_APELLIDO"), rs.getString("SEGUNDO_APELLIDO"),
                rs.getString("TELEFONO"), rs.getString("CORREO"), rs.getString("CARGO"),
                hired == null ? null : hired.toLocalDate(), toll);
        user.setActive(!"N".equals(rs.getString("ACTIVO")));
        user.setEntity(rs.getString("ENTIDAD"));
        user.setDefaultPassword("S".equals(rs.getString("CLAVE_INICIAL")));
        return user;
    }

    private static void setDate(PreparedStatement ps, int index, TollUser user) throws SQLException {
        if (user.getHireDate() == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(user.getHireDate()));
        }
    }

    private static void setToll(PreparedStatement ps, int index, Integer tollId) throws SQLException {
        if (tollId == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setInt(index, tollId);
        }
    }
}
