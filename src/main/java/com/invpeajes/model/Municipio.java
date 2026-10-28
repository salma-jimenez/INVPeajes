
package com.invpeajes.model;

/**
 * Representa la entidad geográfica de un Municipio en el sistema de peajes.
 * Esta clase almacena la información básica del municipio y actúa como puente
 * de ubicación para las estaciones de peaje y las rutas.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Municipio {
    private String idMunicipios;
    private String nombreMunicipio;
    private String idDepartamento; // Llave foránea

    /**
     * Constructor que inicializa un nuevo Municipio con todos sus datos.
     * 
     * @param idMunicipios Identificador único del municipio (Llave primaria).
     * @param nombreMunicipio Nombre oficial del municipio.
     * @param idDepartamento Identificador del departamento al que pertenece (Llave foránea).
     */
    public Municipio(String idMunicipios, String nombreMunicipio, String idDepartamento) {
        this.idMunicipios = idMunicipios;
        this.nombreMunicipio = nombreMunicipio;
        this.idDepartamento = idDepartamento;
    }
    
    /**
     * Obtiene el identificador del municipio.
     * @return El ID del municipio.
     */
    public String getIdMunicipios() {
        return idMunicipios;
    }

    /**
     * Establece o modifica el identificador del municipio.
     * @param idMunicipios El nuevo ID a asignar.
     */
    public void setIdMunicipios(String idMunicipios) {
        this.idMunicipios = idMunicipios;
    }

    /**
     * Obtiene el nombre del municipio.
     * @return El nombre completo del municipio.
     */
    public String getNombreMunicipio() {
        return nombreMunicipio;
    }

    /**
     * Establece o modifica el nombre del municipio.
     * @param nombreMunicipio El nuevo nombre a asignar.
     */
    public void setNombreMunicipio(String nombreMunicipio) {
        this.nombreMunicipio = nombreMunicipio;
    }

    /**
     * Obtiene el identificador del departamento asociado.
     * @return El ID del departamento.
     */
    public String getIdDepartamento() {
        return idDepartamento;
    }

    /**
     * Establece o modifica el identificador del departamento.
     * @param idDepartamento El nuevo ID de departamento a vincular.
     */
    public void setIdDepartamento(String idDepartamento) {
        this.idDepartamento = idDepartamento;
    }
    
    
}
