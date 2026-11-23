package com.invpeajes.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Operaciones del jefe de estación hechas con el paquete {@code pkg_estacion} de la base.
 *
 * <p>Cada procedimiento recibe la cédula del jefe; la base busca su peaje y revisa que la
 * caseta, el recurso o el operador sean de ese peaje. Si no, lanza un error -204xx con el
 * mensaje en español, que llega como {@link DataAccessException}.</p>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class StationDAO {

    private static final String ADD_OPERATOR_SQL =
            "{call pkg_estacion.Pr_agregar_operador(?, ?, ?, ?, ?, ?)}";
    private static final String MODIFY_OPERATOR_SQL =
            "BEGIN pkg_estacion.Pr_modificar_operador(p_cedula_jefe => ?, p_cedula => ?, "
          + "p_telefono => ?, p_correo => ?); END;";
    private static final String DELETE_OPERATOR_SQL =
            "{call pkg_estacion.Pr_eliminar_operador(?, ?)}";
    private static final String BOOTH_STATUS_SQL =
            "{call pkg_estacion.Pr_cambiar_estado_caseta(?, ?, ?)}";
    private static final String RESOURCE_AVAILABILITY_SQL =
            "{call pkg_estacion.Pr_cambiar_disponibilidad_recurso(?, ?, ?)}";

    /**
     * Crea un operador en el peaje del jefe, con su cédula como contraseña inicial
     * ({@code Pr_agregar_operador}).
     *
     * @param managerId cédula del jefe de estación
     * @param idNumber  cédula del operador
     * @param firstName primer nombre
     * @param lastName  primer apellido
     * @param email     correo
     * @param phone     teléfono
     * @throws DataAccessException si la base lo rechaza (cédula o correo repetidos, sin permiso)
     */
    public void addOperator(String managerId, String idNumber, String firstName, String lastName,
                            String email, String phone) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(ADD_OPERATOR_SQL)) {
            cs.setString(1, managerId);
            cs.setString(2, idNumber);
            cs.setString(3, firstName);
            cs.setString(4, lastName);
            cs.setString(5, email);
            cs.setString(6, phone);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el operador", e);
        }
    }

    /**
     * Cambia el teléfono o el correo de un operador del peaje ({@code Pr_modificar_operador}).
     *
     * @param managerId cédula del jefe de estación
     * @param idNumber  cédula del operador
     * @param phone     teléfono nuevo, o null
     * @param email     correo nuevo, o null
     * @throws DataAccessException si la base lo rechaza
     */
    public void modifyOperator(String managerId, String idNumber, String phone, String email) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(MODIFY_OPERATOR_SQL)) {
            cs.setString(1, managerId);
            cs.setString(2, idNumber);
            cs.setString(3, phone);
            cs.setString(4, email);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo modificar el operador " + idNumber, e);
        }
    }

    /**
     * Borra un operador sin cobros ni turnos ({@code Pr_eliminar_operador}).
     *
     * @param managerId cédula del jefe de estación
     * @param idNumber  cédula del operador
     * @throws DataAccessException si la base lo rechaza
     */
    public void deleteOperator(String managerId, String idNumber) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(DELETE_OPERATOR_SQL)) {
            cs.setString(1, managerId);
            cs.setString(2, idNumber);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el operador " + idNumber, e);
        }
    }

    /**
     * Cambia el estado de una caseta del peaje ({@code Pr_cambiar_estado_caseta}).
     * La base no deja apagar una caseta con un turno abierto, y el trigger
     * {@code trg_caseta_activa} no deja activarla si el peaje no está OPERATIVO.
     *
     * @param managerId cédula del jefe de estación
     * @param boothId   ID de la caseta
     * @param status    ACTIVA, INACTIVA o MANTENIMIENTO
     * @throws DataAccessException si la base lo rechaza
     */
    public void changeBoothStatus(String managerId, long boothId, String status) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(BOOTH_STATUS_SQL)) {
            cs.setString(1, managerId);
            cs.setLong(2, boothId);
            cs.setString(3, status);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado de la caseta", e);
        }
    }

    /**
     * Cambia la disponibilidad de un recurso del peaje ({@code Pr_cambiar_disponibilidad_recurso}).
     * EN_SERVICIO no se puede poner a mano: lo pone y lo quita la asistencia.
     *
     * @param managerId    cédula del jefe de estación
     * @param resourceId   ID del recurso
     * @param availability DISPONIBLE o FUERA_DE_SERVICIO
     * @throws DataAccessException si la base lo rechaza
     */
    public void changeResourceAvailability(String managerId, int resourceId, String availability) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(RESOURCE_AVAILABILITY_SQL)) {
            cs.setString(1, managerId);
            cs.setInt(2, resourceId);
            cs.setString(3, availability);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar la disponibilidad del recurso", e);
        }
    }
}
