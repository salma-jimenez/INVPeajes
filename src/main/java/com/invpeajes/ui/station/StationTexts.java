package com.invpeajes.ui.station;

import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.ResourceAvailability;
import com.invpeajes.ui.common.Pill;
import java.awt.Color;

/**
 * Textos y colores en español de los tipos y estados de asistencia y de los recursos, para que
 * todas las pantallas del jefe de estación (y el seguimiento del viajero) los muestren igual.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public final class StationTexts {

    private StationTexts() {
    }

    /**
     * @param type Tipo de asistencia.
     * @return Nombre en singular (ej. "Grúa").
     */
    public static String type(AssistanceType type) {
        switch (type) {
            case TOW_TRUCK:
                return "Grúa";
            case AMBULANCE:
                return "Ambulancia";
            case MOBILE_WORKSHOP:
                return "Carro taller";
            case MECHANICAL_AID:
                return "Auxilio mecánico";
            default:
                return "Otro";
        }
    }

    /**
     * @param type Tipo de recurso.
     * @return Nombre en plural para las columnas de recursos (ej. "Grúas").
     */
    public static String typePlural(AssistanceType type) {
        switch (type) {
            case TOW_TRUCK:
                return "Grúas";
            case AMBULANCE:
                return "Ambulancias";
            case MOBILE_WORKSHOP:
                return "Carros taller";
            case MECHANICAL_AID:
                return "Auxilio mecánico";
            default:
                return "Otros";
        }
    }

    /**
     * @param type Tipo de asistencia.
     * @return Color que identifica el tipo en gráficas y leyendas.
     */
    public static Color typeColor(AssistanceType type) {
        switch (type) {
            case TOW_TRUCK:
                return new Color(0xDC2626);
            case MECHANICAL_AID:
                return new Color(0xF59E0B);
            case AMBULANCE:
                return new Color(0x2563EB);
            case MOBILE_WORKSHOP:
                return new Color(0x16A34A);
            default:
                return new Color(0x9CA3AF);
        }
    }

    /**
     * @param status Estado de la asistencia.
     * @return Nombre del estado (ej. "En camino").
     */
    public static String status(AssistanceStatus status) {
        switch (status) {
            case REGISTERED:
                return "Registrada";
            case ASSIGNED:
                return "Asignada";
            case EN_ROUTE:
                return "En camino";
            case IN_PROGRESS:
                return "En proceso";
            case ATTENDED:
                return "Atendida";
            default:
                return "Cancelada";
        }
    }

    /**
     * @param status Estado de la asistencia.
     * @return Pastilla de color con el nombre del estado.
     */
    public static Pill statusPill(AssistanceStatus status) {
        switch (status) {
            case REGISTERED:
                return new Pill(status(status), new Color(0xFEE2E2), new Color(0xB91C1C));
            case ASSIGNED:
                return new Pill(status(status), new Color(0xFEF3C7), new Color(0xB45309));
            case EN_ROUTE:
                return new Pill(status(status), new Color(0xDBEAFE), new Color(0x1D4ED8));
            case IN_PROGRESS:
                return new Pill(status(status), new Color(0xEDE9FE), new Color(0x6D28D9));
            case ATTENDED:
                return new Pill(status(status), new Color(0xDCFCE7), new Color(0x15803D));
            default:
                return new Pill(status(status), new Color(0xE5E7EB), new Color(0x4B5563));
        }
    }

    /**
     * @param availability Disponibilidad del recurso.
     * @return Pastilla de color con la disponibilidad.
     */
    public static Pill availabilityPill(ResourceAvailability availability) {
        switch (availability) {
            case AVAILABLE:
                return new Pill("Disponible", new Color(0xDCFCE7), new Color(0x15803D));
            case IN_SERVICE:
                return new Pill("En servicio", new Color(0xDBEAFE), new Color(0x1D4ED8));
            default:
                return new Pill("Fuera de servicio", new Color(0xE5E7EB), new Color(0x4B5563));
        }
    }

    /**
     * Tiempo de espera en texto corto.
     * @param minutes Minutos.
     * @return "13 min" o "1 h 14".
     */
    public static String duration(long minutes) {
        if (minutes < 60) {
            return Math.max(0, minutes) + " min";
        }
        return (minutes / 60) + " h " + String.format("%02d", minutes % 60);
    }
}
