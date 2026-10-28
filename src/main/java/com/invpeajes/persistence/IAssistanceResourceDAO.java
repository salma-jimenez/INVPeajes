package com.invpeajes.persistence;

import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para los recursos de asistencia.
 * Hereda las operaciones CRUD de {@link CrudDAO}; la llave es ID_RECURSO.
 */
public interface IAssistanceResourceDAO extends CrudDAO<AssistanceResource, Integer> {

    /**
     * Busca un recurso por su identificación única (ej. placa de la grúa).
     *
     * @param identification identificación del recurso
     * @return el recurso si existe
     */
    Optional<AssistanceResource> findByIdentification(String identification);

    /**
     * Lista los recursos que pertenecen a un peaje.
     *
     * @param tollId peaje a filtrar
     * @return recursos del peaje
     */
    List<AssistanceResource> findByToll(int tollId);

    /**
     * Lista los recursos disponibles de un tipo en un peaje, para asignar
     * uno cuando llega una solicitud de asistencia.
     *
     * @param type   tipo de recurso requerido
     * @param tollId peaje que atiende la solicitud
     * @return recursos DISPONIBLES que cumplen el filtro
     */
    List<AssistanceResource> findAvailable(AssistanceType type, int tollId);

    /**
     * Cambia solo la disponibilidad de un recurso.
     *
     * @param resourceId   recurso a modificar
     * @param availability nueva disponibilidad
     * @return true si se actualizó el recurso
     */
    boolean updateAvailability(int resourceId, ResourceAvailability availability);
}
