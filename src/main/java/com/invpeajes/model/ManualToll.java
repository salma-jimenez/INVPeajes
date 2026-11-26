
package com.invpeajes.model;
import java.util.Date;

/**
 * Peaje que solo cobra en caseta (efectivo o tarjeta) y no acepta TAG Colpass.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class ManualToll extends Toll {

    /**
     * Crea un peaje manual. El indicador de Colpass se fija en "N".
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
    public ManualToll(int tollId, String code, Integer aniId, String name,
                       String kilometer, double latitude, double longitude, String locationDescription,
                       String direction, String operationalStatus, String level,
                       Date date, String phone, String towPhone, String infoSheetUrl,
                       String routeId, Integer concessionaireId, Integer sectorId,
                       String departmentId) {
        super(tollId, code, aniId, name, kilometer, latitude, longitude,
              locationDescription, direction, operationalStatus, level, "N", date,
              phone, towPhone, infoSheetUrl, routeId, concessionaireId, sectorId, departmentId);
    }

    /**
     * Describe el estado del peaje indicando que su cobro es manual.
     * @return Texto con nombre, estado y tipo de cobro.
     */
    @Override
    public String reportOperationalStatus() {
        return "Peaje " + getName() + " [" + getOperationalStatus()
             + "] - cobro manual en caseta (sin Colpass)";
    }
}
