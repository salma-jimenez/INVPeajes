package com.invpeajes.persistence;

import com.invpeajes.model.CashierSession;
import java.util.Optional;

/**
 * Consultas propias de la caja del peaje.
 * Los tiquetes, tarifas y vehículos se manejan con los servicios del equipo
 * ({@code TicketService}, {@code TollRateService} y {@code VehicleService}); aquí solo
 * está la consulta que reúne los datos del operador y de su peaje para abrir el turno.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public interface ICashierDAO {

    /**
     * Busca un empleado con los datos de su peaje (nombre, estado, teléfonos y concesionario).
     * @param operatorId Cédula del empleado.
     * @return Sesión sin caseta, o vacío si no existe.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    Optional<CashierSession> findOperator(String operatorId);

    /**
     * Busca la contraseña cifrada (hash) de un empleado activo, para validar el inicio de sesión.
     * @param operatorId Cédula del empleado.
     * @return Hash de la contraseña, o vacío si el empleado no existe, está inactivo o no tiene contraseña.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    Optional<String> findPasswordHash(String operatorId);
}
