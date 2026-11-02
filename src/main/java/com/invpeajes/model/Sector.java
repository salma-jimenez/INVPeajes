package com.invpeajes.model;

/**
 * Representa un tramo vial entre dos municipios donde se ubican estaciones de peaje
 * (ej. "Armenia - La Línea"). Cada peaje puede pertenecer a un sector.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class Sector {
    private int sectorId;
    private String name;
    private String originMunicipalityId;  // Llave foránea a Municipio (Origen)
    private String destinationMunicipalityId; // Llave foránea a Municipio (Destino)

    /**
     * Constructor que inicializa los datos de un sector vial.
     *
     * @param sectorId Identificador numérico único del sector.
     * @param name Nombre del tramo (ej. "Armenia - La Línea").
     * @param originMunicipalityId ID del municipio donde inicia el tramo (puede ser null).
     * @param destinationMunicipalityId ID del municipio donde termina el tramo (puede ser null).
     */
    public Sector(int sectorId, String name,
                  String originMunicipalityId, String destinationMunicipalityId) {
        this.sectorId = sectorId;
        this.name = name;
        this.originMunicipalityId = originMunicipalityId;
        this.destinationMunicipalityId = destinationMunicipalityId;
    }

    /**
     * Obtiene el identificador del sector.
     * @return El ID del sector.
     */
    public int getSectorId() {
        return sectorId;
    }

    /**
     * Establece o modifica el identificador del sector.
     * @param sectorId El nuevo ID a asignar.
     */
    public void setSectorId(int sectorId) {
        this.sectorId = sectorId;
    }

    /**
     * Obtiene el nombre del sector.
     * @return El nombre del tramo vial.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece o modifica el nombre del sector.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene el ID del municipio de origen.
     * @return El ID del municipio donde inicia el tramo, o null.
     */
    public String getOriginMunicipalityId() {
        return originMunicipalityId;
    }

    /**
     * Establece el municipio de origen.
     * @param originMunicipalityId El nuevo ID del municipio de inicio.
     */
    public void setOriginMunicipalityId(String originMunicipalityId) {
        this.originMunicipalityId = originMunicipalityId;
    }

    /**
     * Obtiene el ID del municipio de destino.
     * @return El ID del municipio donde termina el tramo, o null.
     */
    public String getDestinationMunicipalityId() {
        return destinationMunicipalityId;
    }

    /**
     * Establece el municipio de destino.
     * @param destinationMunicipalityId El nuevo ID del municipio final.
     */
    public void setDestinationMunicipalityId(String destinationMunicipalityId) {
        this.destinationMunicipalityId = destinationMunicipalityId;
    }
}