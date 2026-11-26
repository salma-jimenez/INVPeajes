package com.invpeajes.model;

/**
 * Recurso físico que atiende asistencias viales (grúa, ambulancia, carro
 * taller...) y que pertenece a una estación de peaje.
 * Mapea la tabla {@code PEAJE.RECURSO_ASISTENCIA}.
 *
 * <p>El tipo reutiliza {@link AssistanceType} porque la base de datos admite
 * exactamente los mismos valores para recursos y para asistencias.</p>
 */
public class AssistanceResource {

    private int resourceId;
    private AssistanceType type;
    private String identification;
    private int tollId;
    private ResourceAvailability availability;

    /** Constructor vacío requerido para mapear filas desde la base de datos. */
    public AssistanceResource() {
        this.availability = ResourceAvailability.AVAILABLE;
    }

    /**
     * Crea un recurso con todos sus datos.
     *
     * @param resourceId     identificador del recurso (ID_RECURSO)
     * @param type           tipo de recurso
     * @param identification identificación única, ej. la placa de la grúa
     * @param tollId         peaje al que pertenece el recurso
     * @param availability   disponibilidad actual
     */
    public AssistanceResource(int resourceId, AssistanceType type, String identification,
                              int tollId, ResourceAvailability availability) {
        this.resourceId = resourceId;
        this.type = type;
        this.identification = identification;
        this.tollId = tollId;
        this.availability = availability;
    }

    /**
     * Indica si el recurso puede asignarse a una asistencia ahora.
     *
     * @return true si está DISPONIBLE
     */
    public boolean isAvailable() {
        return availability == ResourceAvailability.AVAILABLE;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public AssistanceType getType() {
        return type;
    }

    public void setType(AssistanceType type) {
        this.type = type;
    }

    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public int getTollId() {
        return tollId;
    }

    public void setTollId(int tollId) {
        this.tollId = tollId;
    }

    public ResourceAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(ResourceAvailability availability) {
        this.availability = availability;
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | Peaje %d | %s", resourceId,
                type == null ? "-" : type.getDbValue(), identification, tollId,
                availability.getDbValue());
    }
}
