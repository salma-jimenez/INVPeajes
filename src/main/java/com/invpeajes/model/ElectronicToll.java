
package com.invpeajes.model;
import java.util.Date;

/**
 * Peaje que acepta pago electrónico con TAG Colpass, además del cobro en caseta.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class ElectronicToll extends Toll {

    /**
     * Crea un peaje electrónico. El indicador de Colpass se fija en "S".
     *
     * @param tollId Identificador interno de la estación.
     * @param code Código oficial de la estación.
     * @param aniId Identificador asignado por la ANI (puede ser null).
     * @param name Nombre comercial o geográfico.
     * @param kilometer Punto kilométrico en la ruta.
     * @param latitude Coordenada de latitud.
     * @param longitude Coordenada de longitud.
     * @param locationDescription Descripción textual de la ubicación.
     * @param direction Sentido del tráfico de la estación.
     * @param operationalStatus Estado actual del peaje.
     * @param level Nivel operativo de la estación.
     * @param date Fecha de registro o actualización.
     * @param phone Teléfono de atención de la estación.
     * @param towPhone Teléfono de servicio de grúa.
     * @param infoSheetUrl URL al documento de ficha técnica.
     * @param routeId ID de la ruta (Llave foránea).
     * @param concessionaireId ID del concesionario (Llave foránea, puede ser null).
     * @param sectorId ID del sector vial (Llave foránea, puede ser null).
     * @param departmentId ID del departamento (Llave foránea).
     */
    public ElectronicToll(int tollId, String code, Integer aniId, String name,
                            String kilometer, double latitude, double longitude, String locationDescription,
                            String direction, String operationalStatus, String level,
                            Date date, String phone, String towPhone, String infoSheetUrl,
                            String routeId, Integer concessionaireId, Integer sectorId,
                            String departmentId) {
        super(tollId, code, aniId, name, kilometer, latitude, longitude,
              locationDescription, direction, operationalStatus, level, "S", date,
              phone, towPhone, infoSheetUrl, routeId, concessionaireId, sectorId, departmentId);
    }

    /**
     * Describe el estado del peaje indicando que acepta Colpass.
     * @return Texto con nombre, estado y tipo de cobro.
     */
    @Override
    public String reportOperationalStatus() {
        return "Peaje " + getName() + " [" + getOperationalStatus()
             + "] - acepta TAG Colpass y cobro en caseta";
    }
}