package com.invpeajes.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Estados del ciclo de vida de una asistencia vial y las transiciones válidas
 * entre ellos.
 *
 * @author Salomejimenez
 */
public enum AssistanceStatus {
    REGISTERED("REGISTRADA"),
    ASSIGNED("ASIGNADA"),
    EN_ROUTE("EN_CAMINO"),
    IN_PROGRESS("EN_PROCESO"),
    ATTENDED("ATENDIDA"),
    CANCELLED("CANCELADA");

    private final String dbValue;

    AssistanceStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * Valor con el que se guarda el estado en Oracle.
     *
     * @return valor de las columnas ESTADO_*
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Convierte el valor guardado en Oracle al estado correspondiente.
     *
     * @param dbValue valor de una columna ESTADO_* (puede ser null)
     * @return el estado, o null si el valor es null
     * @throws IllegalArgumentException si el valor no es reconocido
     */
    public static AssistanceStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (AssistanceStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de asistencia desconocido: " + dbValue);
    }

    /**
     * Devuelve los estados a los que se puede pasar desde este estado.
     *
     * @return conjunto inmutable de estados siguientes válidos
     */
    public Set<AssistanceStatus> allowedNext() {
        switch (this) {
            case REGISTERED:
                return setOf(ASSIGNED, CANCELLED);
            case ASSIGNED:
                return setOf(EN_ROUTE, CANCELLED);
            case EN_ROUTE:
                return setOf(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS:
                return setOf(ATTENDED);
            default:
                return Collections.emptySet();
        }
    }

    /**
     * Indica si se puede pasar de este estado al estado indicado.
     *
     * @param next estado destino
     * @return true si la transición es válida
     */
    public boolean canChangeTo(AssistanceStatus next) {
        return allowedNext().contains(next);
    }

    /**
     * Indica si el estado es final (no admite más cambios).
     *
     * @return true para ATTENDED y CANCELLED
     */
    public boolean isFinal() {
        return allowedNext().isEmpty();
    }

    private static Set<AssistanceStatus> setOf(AssistanceStatus... statuses) {
        return Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(statuses)));
    }

}
