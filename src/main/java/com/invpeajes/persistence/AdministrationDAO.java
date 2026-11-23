package com.invpeajes.persistence;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Operaciones del administrador hechas con el paquete {@code pkg_administracion} de la base.
 *
 * <p>Cada procedimiento recibe la cédula del administrador y revisa en la base que el peaje
 * sea de su entidad ({@code Pr_validar_admin_peaje}). Si no tiene permiso o un dato no sirve,
 * la base lanza un error -203xx con el mensaje en español, que llega como
 * {@link DataAccessException}.</p>
 *
 * <p>Los parámetros opcionales se envían con notación por nombre ({@code p_estado => ?}),
 * así que solo se mandan los que cambian; lo que llega en NULL no se modifica.</p>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class AdministrationDAO {

    private static final String ADD_TOLL_SQL =
            "{call pkg_administracion.Pr_agregar_peaje(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
    private static final String MODIFY_TOLL_SQL =
            "BEGIN pkg_administracion.Pr_modificar_peaje(p_cedula_admin => ?, p_peaje => ?, p_nombre => ?, "
          + "p_latitud => ?, p_longitud => ?, p_estado => ?, p_ruta => ?, p_concesionario => ?); END;";
    private static final String DELETE_TOLL_SQL =
            "{call pkg_administracion.Pr_eliminar_peaje(?, ?)}";
    private static final String UPDATE_RATE_SQL =
            "{call pkg_administracion.Pr_actualizar_tarifa(?, ?, ?, ?)}";
    private static final String ADD_MANAGER_SQL =
            "{call pkg_administracion.Pr_agregar_jefe(?, ?, ?, ?, ?, ?, ?)}";
    private static final String MODIFY_MANAGER_SQL =
            "BEGIN pkg_administracion.Pr_modificar_jefe(p_cedula_admin => ?, p_cedula => ?, "
          + "p_telefono => ?, p_correo => ?, p_peaje => ?); END;";
    private static final String DELETE_MANAGER_SQL =
            "{call pkg_administracion.Pr_eliminar_jefe(?, ?)}";

    /**
     * Crea un peaje en la entidad del administrador ({@code Pr_agregar_peaje}).
     * La base escoge el ID y lo deja OPERATIVO con la fecha de hoy.
     *
     * @param adminId        cédula del administrador
     * @param name           nombre del peaje
     * @param latitude       latitud
     * @param longitude      longitud
     * @param departmentId   código del departamento
     * @param municipalityId código DANE del municipio, o null
     * @param routeId        ruta, o null
     * @param concessionaire concesionario, o null
     * @return ID del peaje nuevo
     * @throws DataAccessException si la base lo rechaza
     */
    public int addToll(String adminId, String name, double latitude, double longitude, String departmentId,
                       String municipalityId, String routeId, Integer concessionaire) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(ADD_TOLL_SQL)) {
            cs.setString(1, adminId);
            cs.setString(2, name);
            cs.setDouble(3, latitude);
            cs.setDouble(4, longitude);
            cs.setString(5, departmentId);
            cs.setString(6, municipalityId);
            cs.setString(7, routeId);
            JdbcUtils.setNullableInt(cs, 8, concessionaire);
            cs.registerOutParameter(9, Types.NUMERIC);
            cs.execute();
            return cs.getInt(9);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear el peaje", e);
        }
    }

    /**
     * Cambia los datos principales de un peaje ({@code Pr_modificar_peaje}).
     * Lo que se envía en null no cambia.
     *
     * @param adminId        cédula del administrador
     * @param tollId         ID del peaje
     * @param name           nombre nuevo, o null
     * @param latitude       latitud nueva, o null
     * @param longitude      longitud nueva, o null
     * @param status         estado operativo nuevo, o null
     * @param routeId        ruta nueva, o null
     * @param concessionaire concesionario nuevo, o null
     * @throws DataAccessException si la base lo rechaza
     */
    public void modifyToll(String adminId, int tollId, String name, Double latitude, Double longitude,
                           String status, String routeId, Integer concessionaire) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(MODIFY_TOLL_SQL)) {
            cs.setString(1, adminId);
            cs.setInt(2, tollId);
            cs.setString(3, name);
            JdbcUtils.setNullableDouble(cs, 4, latitude);
            JdbcUtils.setNullableDouble(cs, 5, longitude);
            cs.setString(6, status);
            cs.setString(7, routeId);
            JdbcUtils.setNullableInt(cs, 8, concessionaire);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo modificar el peaje " + tollId, e);
        }
    }

    /**
     * Cambia solo el estado operativo de un peaje ({@code Pr_modificar_peaje} con {@code p_estado}).
     * Si deja de estar OPERATIVO, el trigger {@code trg_peaje_estado} apaga sus casetas.
     *
     * @param adminId cédula del administrador
     * @param tollId  ID del peaje
     * @param status  OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA
     * @throws DataAccessException si la base lo rechaza
     */
    public void changeTollStatus(String adminId, int tollId, String status) {
        modifyToll(adminId, tollId, null, null, null, status, null, null);
    }

    /**
     * Borra un peaje sin cobros, asistencias ni empleados, con sus tarifas, casetas y recursos
     * ({@code Pr_eliminar_peaje}).
     *
     * @param adminId cédula del administrador
     * @param tollId  ID del peaje
     * @throws DataAccessException si la base lo rechaza (por ejemplo, tiene cobros)
     */
    public void deleteToll(String adminId, int tollId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(DELETE_TOLL_SQL)) {
            cs.setString(1, adminId);
            cs.setInt(2, tollId);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el peaje " + tollId, e);
        }
    }

    /**
     * Crea o cambia la tarifa de una categoría ({@code Pr_actualizar_tarifa}).
     * La base la deja vigente desde hoy y avisa si sube más del 10 % ({@code trg_alerta_tarifa}).
     *
     * @param adminId    cédula del administrador
     * @param tollId     ID del peaje
     * @param categoryId categoría (1 a 7)
     * @param price      precio
     * @throws DataAccessException si la base lo rechaza
     */
    public void updateRate(String adminId, int tollId, int categoryId, BigDecimal price) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(UPDATE_RATE_SQL)) {
            cs.setString(1, adminId);
            cs.setInt(2, tollId);
            cs.setInt(3, categoryId);
            cs.setBigDecimal(4, price);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la tarifa de la categoría " + categoryId, e);
        }
    }

    /**
     * Crea un jefe de estación con su cédula como contraseña inicial ({@code Pr_agregar_jefe}).
     *
     * @param adminId   cédula del administrador
     * @param idNumber  cédula del jefe
     * @param firstName primer nombre
     * @param lastName  primer apellido
     * @param email     correo
     * @param phone     teléfono
     * @param tollId    peaje del jefe
     * @throws DataAccessException si la base lo rechaza (cédula o correo repetidos, sin permiso)
     */
    public void addManager(String adminId, String idNumber, String firstName, String lastName,
                           String email, String phone, int tollId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(ADD_MANAGER_SQL)) {
            cs.setString(1, adminId);
            cs.setString(2, idNumber);
            cs.setString(3, firstName);
            cs.setString(4, lastName);
            cs.setString(5, email);
            cs.setString(6, phone);
            cs.setInt(7, tollId);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el jefe de estación", e);
        }
    }

    /**
     * Cambia el teléfono, el correo o el peaje de un jefe ({@code Pr_modificar_jefe}).
     *
     * @param adminId  cédula del administrador
     * @param idNumber cédula del jefe
     * @param phone    teléfono nuevo, o null
     * @param email    correo nuevo, o null
     * @param tollId   peaje nuevo, o null
     * @throws DataAccessException si la base lo rechaza
     */
    public void modifyManager(String adminId, String idNumber, String phone, String email, Integer tollId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(MODIFY_MANAGER_SQL)) {
            cs.setString(1, adminId);
            cs.setString(2, idNumber);
            cs.setString(3, phone);
            cs.setString(4, email);
            JdbcUtils.setNullableInt(cs, 5, tollId);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo modificar el jefe de estación " + idNumber, e);
        }
    }

    /**
     * Borra un jefe de estación sin cobros ni turnos ({@code Pr_eliminar_jefe}).
     *
     * @param adminId  cédula del administrador
     * @param idNumber cédula del jefe
     * @throws DataAccessException si la base lo rechaza
     */
    public void deleteManager(String adminId, String idNumber) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(DELETE_MANAGER_SQL)) {
            cs.setString(1, adminId);
            cs.setString(2, idNumber);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el jefe de estación " + idNumber, e);
        }
    }
}
