package com.invpeajes.model;

/**
 * Representa un tramo vial entre dos municipios donde se ubican estaciones de peaje
 * (ej. "Armenia - La Línea"). Cada peaje puede pertenecer a un sector.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class Sector {
    private int idSector;
    private String nombreSector;
    private String municipioOrigenId;  // Llave foránea a Municipio (Origen)
    private String municipioDestinoId; // Llave foránea a Municipio (Destino)

    /**
     * Constructor que inicializa los datos de un sector vial.
     *
     * @param idSector Identificador numérico único del sector.
     * @param nombreSector Nombre del tramo (ej. "Armenia - La Línea").
     * @param municipioOrigenId ID del municipio donde inicia el tramo (puede ser null).
     * @param municipioDestinoId ID del municipio donde termina el tramo (puede ser null).
     */
    public Sector(int idSector, String nombreSector,
                  String municipioOrigenId, String municipioDestinoId) {
        this.idSector = idSector;
        this.nombreSector = nombreSector;
        this.municipioOrigenId = municipioOrigenId;
        this.municipioDestinoId = municipioDestinoId;
    }

    /**
     * Obtiene el identificador del sector.
     * @return El ID del sector.
     */
    public int getIdSector() {
        return idSector;
    }

    /**
     * Establece o modifica el identificador del sector.
     * @param idSector El nuevo ID a asignar.
     */
    public void setIdSector(int idSector) {
        this.idSector = idSector;
    }

    /**
     * Obtiene el nombre del sector.
     * @return El nombre del tramo vial.
     */
    public String getNombreSector() {
        return nombreSector;
    }

    /**
     * Establece o modifica el nombre del sector.
     * @param nombreSector El nuevo nombre a asignar.
     */
    public void setNombreSector(String nombreSector) {
        this.nombreSector = nombreSector;
    }

    /**
     * Obtiene el ID del municipio de origen.
     * @return El ID del municipio donde inicia el tramo, o null.
     */
    public String getMunicipioOrigenId() {
        return municipioOrigenId;
    }

    /**
     * Establece el municipio de origen.
     * @param municipioOrigenId El nuevo ID del municipio de inicio.
     */
    public void setMunicipioOrigenId(String municipioOrigenId) {
        this.municipioOrigenId = municipioOrigenId;
    }

    /**
     * Obtiene el ID del municipio de destino.
     * @return El ID del municipio donde termina el tramo, o null.
     */
    public String getMunicipioDestinoId() {
        return municipioDestinoId;
    }

    /**
     * Establece el municipio de destino.
     * @param municipioDestinoId El nuevo ID del municipio final.
     */
    public void setMunicipioDestinoId(String municipioDestinoId) {
        this.municipioDestinoId = municipioDestinoId;
    }
}