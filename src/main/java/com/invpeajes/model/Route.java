
package com.invpeajes.model;

/**
 * Representa un trayecto o vía geográfica dentro del sistema de peajes.
 * Conecta un municipio de origen con un municipio de destino, permitiendo
 * trazar el recorrido de los vehículos.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Route {
    private String routeId;
    private String name;
    private String originMunicipalityId;  // Llave foránea a Municipio (Origen)
    private String destinationMunicipalityId; // Llave foránea a Municipio (Destino)

    /**
     * Constructor que inicializa los datos de un trayecto o ruta.
     * 
     * @param routeId Identificador alfanumérico único de la ruta.
     * @param name Nombre de la vía (ej. "Ruta del Sol Sector 2").
     * @param originMunicipalityId ID del municipio donde inicia el trayecto.
     * @param destinationMunicipalityId ID del municipio donde finaliza el trayecto.
     */
    public Route(String routeId, String name, String originMunicipalityId, String destinationMunicipalityId) {
        this.routeId = routeId;
        this.name = name;
        this.originMunicipalityId = originMunicipalityId;
        this.destinationMunicipalityId = destinationMunicipalityId;
    }

    /**
     * Obtiene el identificador de la ruta.
     * @return El ID de la ruta.
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Establece o modifica el identificador de la ruta.
     * @param routeId El nuevo ID a asignar.
     */
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    /**
     * Obtiene el nombre de la vía o ruta.
     * @return El nombre oficial de la ruta.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece o modifica el nombre de la ruta.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene el ID del municipio de origen.
     * @return El ID del municipio donde inicia.
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
     * @return El ID del municipio donde finaliza.
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