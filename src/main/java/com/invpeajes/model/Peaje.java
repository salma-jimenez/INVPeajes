
package com.invpeajes.model;

import java.util.Date;

/**
 * Clase abstracta que representa una estación de peaje genérica en la vía.
 * Sirve como plantilla base para implementar tipos de peajes específicos
 * como PeajeManual o PeajeElectronico aplicando herencia y polimorfismo.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public abstract class Peaje {
    private String idPeaje;
    private String nombrePeaje;
    private String idMunicipios; // Llave foránea a Municipio
    private String idConcesionario; // Llave foránea a Concesionario
    private String idRuta; // Llave foránea a Ruta
    private String kilometro; // Ubicación en la vía
    private String latitud;   // Coordenada geográfica
    private String longitud;  // Coordenada geográfica
    private String estadoOperativo;
    private Date fecha;

    /**
     * Constructor base para inicializar los datos generales de cualquier peaje.
     * 
     * @param idPeaje Identificador único de la estación de peaje.
     * @param nombrePeaje Nombre comercial o geográfico de la estación.
     * @param idMunicipios ID del municipio donde está ubicado físicamente.
     * @param idConcesionario ID del concesionario (ej. INVIAS, ANI) que lo administra.
     * @param idRuta ID de la ruta a la que pertenece.
     * @param kilometro Punto kilométrico exacto en la ruta (ej. "Km 45").
     * @param latitud Coordenada de latitud para geolocalización.
     * @param longitud Coordenada de longitud para geolocalización.
     * @param estadoOperativo Estado actual del peaje (Activo o Inactivo).
     * @param fecha Fecha de registro o actualización.
     */
    public Peaje(String idPeaje, String nombrePeaje, String kilometro, String latitud, 
                 String longitud, String estadoOperativo, Date fecha, 
                 String idRuta, String idConcesionario, String idMunicipios) {
        this.idPeaje = idPeaje;
        this.nombrePeaje = nombrePeaje;
        this.idMunicipios = idMunicipios;
        this.idConcesionario = idConcesionario;
        this.idRuta = idRuta;
        this.kilometro = kilometro;
        this.latitud = latitud;
        this.longitud = longitud;
        this.estadoOperativo = estadoOperativo;
        this.fecha = fecha;
    }

    /**
     * Obtiene el identificador del peaje.
     * @return El ID del peaje.
     */
    public String getIdPeaje() {
        return idPeaje;
    }

    /**
     * Establece el identificador del peaje.
     * @param idPeaje El nuevo ID a asignar.
     */
    public void setIdPeaje(String idPeaje) {
        this.idPeaje = idPeaje;
    }

    /**
     * Obtiene el nombre del peaje.
     * @return El nombre oficial de la estación.
     */
    public String getNombrePeaje() {
        return nombrePeaje;
    }

    /**
     * Establece el nombre del peaje.
     * @param nombrePeaje El nuevo nombre a asignar.
     */
    public void setNombrePeaje(String nombrePeaje) {
        this.nombrePeaje = nombrePeaje;
    }

    /**
     * Obtiene el ID del municipio donde se ubica el peaje.
     * @return El ID del municipio asociado.
     */
    public String getIdMunicipios() {
        return idMunicipios;
    }

    /**
     * Establece el ID del municipio.
     * @param idMunicipios El nuevo ID de municipio.
     */
    public void setIdMunicipios(String idMunicipios) {
        this.idMunicipios = idMunicipios;
    }

    /**
     * Obtiene el ID del concesionario que administra el peaje.
     * @return El ID del concesionario.
     */
    public String getIdConcesionario() {
        return idConcesionario;
    }

    /**
     * Establece el ID del concesionario administrador.
     * @param idConcesionario El nuevo ID de concesionario.
     */
    public void setIdConcesionario(String idConcesionario) {
        this.idConcesionario = idConcesionario;
    }
    
    /**
     * Obtiene la ubicación kilométrica del peaje en la vía.
     * @return El punto kilométrico.
     */
    public String getKilometro() {
        return kilometro;
    }
    
    /**
     * Establece la ubicación kilométrica del peaje.
     * @param kilometro El nuevo punto kilométrico a asignar.
     */
    public void setKilometro(String kilometro) {
        this.kilometro = kilometro;
    }

    /**
     * Obtiene la latitud geográfica del peaje.
     * @return La coordenada de latitud.
     */
    public String getLatitud() {
        return latitud;
    }
    
    /**
     * Establece la latitud geográfica del peaje.
     * @param latitud La nueva coordenada de latitud a asignar.
     */
    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    /**
     * Obtiene la longitud geográfica del peaje.
     * @return La coordenada de longitud.
     */
    public String getLongitud() {
        return longitud;
    }
    
    /**
     * Establece la longitud geográfica del peaje.
     * @param longitud La nueva coordenada de longitud a asignar.
     */
    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }

    /**
     * Obtiene el ID de la ruta que administra el peaje.
     * @return El ID de la ruta.
     */
    public String getIdRuta() {
        return idRuta;
    }

    /**
     * Establece el ID de la ruta del peaje.
     * @param idRuta El nuevo ID de la ruta a asignar.
     */
    public void setIdRuta(String idRuta) {
        this.idRuta = idRuta;
    }

    /**
     * Obtiene el estado operativo actual del peaje.
     * @return El estado operativo (ej. Activo, Inactivo).
     */
    public String getEstadoOperativo() {
        return estadoOperativo;
    }

    /**
     * Establece el estado operativo del peaje.
     * @param estadoOperativo El nuevo estado operativo a asignar.
     */
    public void setEstadoOperativo(String estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
    }

    /**
     * Obtiene la fecha de registro o actualización del peaje.
     * @return La fecha correspondiente.
     */
    public Date getFecha() {
        return fecha;
    }

    /**
     * Establece la fecha de registro o actualización.
     * @param fecha La nueva fecha a asignar.
     */
    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
    
    

    /**
     * Método abstracto que aplica polimorfismo. 
     * Cada clase hija deberá sobrescribir este método para definir su propia 
     * lógica de cómo opera.
     * 
     * @return Una cadena de texto indicando el estado o tipo de operación.
     */
    public abstract String reportarEstadoOperativo();
}
