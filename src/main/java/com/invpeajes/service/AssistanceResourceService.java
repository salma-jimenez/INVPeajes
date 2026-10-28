
package com.invpeajes.service;

import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;
import com.invpeajes.persistence.AssistanceResourceDAOImpl;
import com.invpeajes.persistence.IAssistanceResourceDAO;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio para los recursos de asistencia (grúas, ambulancias,
 * carros taller...): registro, consulta y control de disponibilidad.
 *
 * <p>El paso a EN_SERVICIO y el regreso a DISPONIBLE los hace
 * {@link AssistanceService} al asignar y cerrar asistencias; aquí solo se
 * permite sacar un recurso de servicio o reactivarlo manualmente.</p>
 */
public class AssistanceResourceService {

    private static final int MAX_IDENTIFICATION_LENGTH = 20;

    private final IAssistanceResourceDAO resourceDAO;

    /** Crea el servicio con su DAO por defecto. */
    public AssistanceResourceService() {
        this(new AssistanceResourceDAOImpl());
    }

    /**
     * Crea el servicio con un DAO inyectado.
     *
     * @param resourceDAO DAO de recursos de asistencia
     */
    public AssistanceResourceService(IAssistanceResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    /**
     * Registra un recurso nuevo en estado DISPONIBLE.
     *
     * @param resource recurso a registrar
     * @return id asignado al recurso
     */
    public int register(AssistanceResource resource) {
        validate(resource);
        ensureIdentificationAvailable(resource.getIdentification(), null);
        resource.setAvailability(ResourceAvailability.AVAILABLE);
        resourceDAO.insert(resource);
        return resource.getResourceId();
    }

    /**
     * Obtiene un recurso o lanza error si no existe.
     *
     * @param resourceId id del recurso
     * @return el recurso encontrado
     */
    public AssistanceResource get(int resourceId) {
        return resourceDAO.findById(resourceId)
                .orElseThrow(() -> new ValidationException("No existe el recurso " + resourceId));
    }

    /**
     * Lista todos los recursos.
     *
     * @return recursos ordenados por peaje y tipo
     */
    public List<AssistanceResource> listAll() {
        return resourceDAO.findAll();
    }

    /**
     * Lista los recursos de un peaje.
     *
     * @param tollId peaje a filtrar
     * @return recursos del peaje
     */
    public List<AssistanceResource> listByToll(int tollId) {
        return resourceDAO.findByToll(tollId);
    }

    /**
     * Lista los recursos disponibles de un tipo en un peaje.
     *
     * @param type   tipo de recurso requerido
     * @param tollId peaje que atiende la solicitud
     * @return recursos DISPONIBLES que cumplen el filtro
     */
    public List<AssistanceResource> listAvailable(AssistanceType type, int tollId) {
        if (type == null) {
            throw new ValidationException("Debe indicar el tipo de recurso");
        }
        return resourceDAO.findAvailable(type, tollId);
    }

    /**
     * Actualiza tipo, identificación y peaje de un recurso que no esté en
     * servicio. La disponibilidad no se cambia por aquí.
     *
     * @param resource recurso con los datos nuevos
     */
    public void update(AssistanceResource resource) {
        validate(resource);
        AssistanceResource current = get(resource.getResourceId());
        if (current.getAvailability() == ResourceAvailability.IN_SERVICE) {
            throw new ValidationException("No se puede modificar un recurso que está atendiendo una asistencia");
        }
        ensureIdentificationAvailable(resource.getIdentification(), resource.getResourceId());
        resource.setAvailability(current.getAvailability());
        resourceDAO.update(resource);
    }

    /**
     * Saca un recurso de servicio (mantenimiento, daño...).
     *
     * @param resourceId recurso a desactivar
     */
    public void markOutOfService(int resourceId) {
        AssistanceResource resource = get(resourceId);
        if (resource.getAvailability() == ResourceAvailability.IN_SERVICE) {
            throw new ValidationException("El recurso está atendiendo una asistencia; ciérrela primero");
        }
        resourceDAO.updateAvailability(resourceId, ResourceAvailability.OUT_OF_SERVICE);
    }

    /**
     * Vuelve a poner disponible un recurso que estaba fuera de servicio.
     *
     * @param resourceId recurso a reactivar
     */
    public void reactivate(int resourceId) {
        AssistanceResource resource = get(resourceId);
        if (resource.getAvailability() != ResourceAvailability.OUT_OF_SERVICE) {
            throw new ValidationException("Solo se pueden reactivar recursos fuera de servicio");
        }
        resourceDAO.updateAvailability(resourceId, ResourceAvailability.AVAILABLE);
    }

    /**
     * Elimina un recurso que no esté en servicio ni referenciado por asistencias.
     *
     * @param resourceId recurso a eliminar
     */
    public void delete(int resourceId) {
        if (get(resourceId).getAvailability() == ResourceAvailability.IN_SERVICE) {
            throw new ValidationException("No se puede eliminar un recurso que está en servicio");
        }
        resourceDAO.delete(resourceId);
    }

    private void validate(AssistanceResource resource) {
        if (resource.getType() == null) {
            throw new ValidationException("El tipo de recurso es obligatorio");
        }
        String identification = resource.getIdentification() == null ? "" : resource.getIdentification().trim();
        if (identification.isEmpty()) {
            throw new ValidationException("La identificación del recurso es obligatoria");
        }
        if (identification.length() > MAX_IDENTIFICATION_LENGTH) {
            throw new ValidationException("La identificación admite máximo " + MAX_IDENTIFICATION_LENGTH
                    + " caracteres");
        }
        if (resource.getTollId() <= 0) {
            throw new ValidationException("Debe indicar el peaje al que pertenece el recurso");
        }
        resource.setIdentification(identification.toUpperCase());
    }

    private void ensureIdentificationAvailable(String identification, Integer ownResourceId) {
        Optional<AssistanceResource> existing = resourceDAO.findByIdentification(identification);
        if (existing.isPresent() && !Integer.valueOf(existing.get().getResourceId()).equals(ownResourceId)) {
            throw new ValidationException("Ya existe un recurso con la identificación " + identification);
        }
    }
    
}
