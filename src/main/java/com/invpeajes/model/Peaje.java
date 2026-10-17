
package com.invpeajes.model;

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

    /**
     * Constructor base para inicializar los datos generales de cualquier peaje.
     * 
     * @param idPeaje Identificador único de la estación de peaje.
     * @param nombrePeaje Nombre comercial o geográfico de la estación.
     * @param idMunicipios ID del municipio donde está ubicado físicamente.
     * @param idConcesionario ID del concesionario (ej. INVIAS, ANI) que lo administra.
     */
    public Peaje(String idPeaje, String nombrePeaje, String idMunicipios, String idConcesionario) {
        this.idPeaje = idPeaje;
        this.nombrePeaje = nombrePeaje;
        this.idMunicipios = idMunicipios;
        this.idConcesionario = idConcesionario;
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
     * Método abstracto que aplica polimorfismo. 
     * Cada clase hija deberá sobrescribir este método para definir su propia 
     * lógica de cómo opera.
     * 
     * @return Una cadena de texto indicando el estado o tipo de operación.
     */
    public abstract String reportarEstadoOperativo();
}
