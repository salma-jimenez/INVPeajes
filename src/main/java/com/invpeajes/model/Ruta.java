
package com.invpeajes.model;

/**
 * Representa un trayecto o vía geográfica dentro del sistema de peajes.
 * Conecta un municipio de origen con un municipio de destino, permitiendo
 * trazar el recorrido de los vehículos.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Ruta {
    private String idRuta;
    private String nombreRuta;
    private String municipioOrigenId;  // Llave foránea a Municipio (Origen)
    private String municipioDestinoId; // Llave foránea a Municipio (Destino)

    /**
     * Constructor que inicializa los datos de un trayecto o ruta.
     * 
     * @param idRuta Identificador alfanumérico único de la ruta.
     * @param nombreRuta Nombre de la vía (ej. "Ruta del Sol Sector 2").
     * @param municipioOrigenId ID del municipio donde inicia el trayecto.
     * @param municipioDestinoId ID del municipio donde finaliza el trayecto.
     */
    public Ruta(String idRuta, String nombreRuta, String municipioOrigenId, String municipioDestinoId) {
        this.idRuta = idRuta;
        this.nombreRuta = nombreRuta;
        this.municipioOrigenId = municipioOrigenId;
        this.municipioDestinoId = municipioDestinoId;
    }

    /**
     * Obtiene el identificador de la ruta.
     * @return El ID de la ruta.
     */
    public String getIdRuta() {
        return idRuta;
    }

    /**
     * Establece o modifica el identificador de la ruta.
     * @param idRuta El nuevo ID a asignar.
     */
    public void setIdRuta(String idRuta) {
        this.idRuta = idRuta;
    }

    /**
     * Obtiene el nombre de la vía o ruta.
     * @return El nombre oficial de la ruta.
     */
    public String getNombreRuta() {
        return nombreRuta;
    }

    /**
     * Establece o modifica el nombre de la ruta.
     * @param nombreRuta El nuevo nombre a asignar.
     */
    public void setNombreRuta(String nombreRuta) {
        this.nombreRuta = nombreRuta;
    }

    /**
     * Obtiene el ID del municipio de origen.
     * @return El ID del municipio donde inicia.
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
     * @return El ID del municipio donde finaliza.
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