

package com.invpeajes.persistence;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceStatus;

import java.util.List;

/**
 * Contrato de acceso a datos para las asistencias viales.
 * @author Salomejimenez
 */
public interface IAssistanceDAO extends CrudDAO<Assistance, Integer> {

    /**
     * Lista las asistencias que están en un estado dado.
     *
     * @param status estado a filtrar
     * @return asistencias en ese estado, más recientes primero
     */
    List<Assistance> findByStatus(AssistanceStatus status);

    /**
     * Lista las asistencias prestadas a un vehículo.
     *
     * @param plate placa del vehículo
     * @return asistencias del vehículo, más recientes primero
     */
    List<Assistance> findByPlate(String plate);

    /**
     * Lista las asistencias atendidas desde un peaje.
     *
     * @param tollId peaje a filtrar
     * @return asistencias del peaje, más recientes primero
     */
    List<Assistance> findByToll(int tollId);

    /**
     * Guarda el nuevo estado de la asistencia y registra el cambio en el
     * historial dentro de una sola transacción.
     *
     * @param assistance     asistencia con el estado nuevo ya asignado
     * @param previousStatus estado que tenía antes del cambio
     * @param notes          motivo del cambio
     * @return true si se actualizó la asistencia
     */
    boolean changeStatus(Assistance assistance, AssistanceStatus previousStatus, String notes);
    
}
