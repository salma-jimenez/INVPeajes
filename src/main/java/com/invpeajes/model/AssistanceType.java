
package com.invpeajes.model;

/**
 * Tipos de asistencia vial. Cada constante guarda el valor exacto que exige
 * @author Salomejimenez
 */
public enum AssistanceType {
    AMBULANCE("AMBULANCIA"),
    TOW_TRUCK("GRUA"),
    MOBILE_WORKSHOP("CARRO_TALLER"),
    MECHANICAL_AID("AUXILIO_MECANICO"),
    OTHER("OTRO");

    private final String dbValue;

    AssistanceType(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * Valor con el que se guarda el tipo en Oracle.
     *
     * @return valor de la columna TIPO_ASISTENCIA
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Convierte el valor guardado en Oracle al tipo correspondiente.
     *
     * @param dbValue valor de la columna TIPO_ASISTENCIA
     * @return el tipo de asistencia
     * @throws IllegalArgumentException si el valor no es reconocido
     */
    public static AssistanceType fromDbValue(String dbValue) {
        for (AssistanceType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de asistencia desconocido: " + dbValue);
    }
}