
package com.invpeajes.model;

/**
 *
 * Disponibilidad de un recurso de asistencia. Cada constante guarda el valor
 * @author Salomejimenez
 */
public enum ResourceAvailability {
    AVAILABLE("DISPONIBLE"),
    IN_SERVICE("EN_SERVICIO"),
    OUT_OF_SERVICE("FUERA_DE_SERVICIO");

    private final String dbValue;

    ResourceAvailability(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * Valor con el que se guarda la disponibilidad en Oracle.
     *
     * @return valor de la columna DISPONIBILIDAD
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Convierte el valor guardado en Oracle a la disponibilidad correspondiente.
     *
     * @param dbValue valor de la columna DISPONIBILIDAD
     * @return la disponibilidad
     * @throws IllegalArgumentException si el valor no es reconocido
     */
    public static ResourceAvailability fromDbValue(String dbValue) {
        for (ResourceAvailability availability : values()) {
            if (availability.dbValue.equals(dbValue)) {
                return availability;
            }
        }
        throw new IllegalArgumentException("Disponibilidad desconocida: " + dbValue);
    }
    
}
