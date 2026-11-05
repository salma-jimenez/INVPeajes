
package com.invpeajes.model;

/**
 * Representa la entidad geográfica de un Municipio en el sistema de peajes.
 * Esta clase almacena la información básica del municipio y actúa como puente
 * de ubicación para las estaciones de peaje y las rutas.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Municipality {
    private String municipalityId;
    private String name;
    private String departmentId; // Llave foránea

    /**
     * Constructor que inicializa un nuevo Municipio con todos sus datos.
     * 
     * @param municipalityId Identificador único del municipio (Llave primaria).
     * @param name Nombre oficial del municipio.
     * @param departmentId Identificador del departamento al que pertenece (Llave foránea).
     */
    public Municipality(String municipalityId, String name, String departmentId) {
        this.municipalityId = municipalityId;
        this.name = name;
        this.departmentId = departmentId;
    }
    
    /**
     * Obtiene el identificador del municipio.
     * @return El ID del municipio.
     */
    public String getMunicipalityId() {
        return municipalityId;
    }

    /**
     * Establece o modifica el identificador del municipio.
     * @param municipalityId El nuevo ID a asignar.
     */
    public void setMunicipalityId(String municipalityId) {
        this.municipalityId = municipalityId;
    }

    /**
     * Obtiene el nombre del municipio.
     * @return El nombre completo del municipio.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece o modifica el nombre del municipio.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene el identificador del departamento asociado.
     * @return El ID del departamento.
     */
    public String getDepartmentId() {
        return departmentId;
    }

    /**
     * Establece o modifica el identificador del departamento.
     * @param departmentId El nuevo ID de departamento a vincular.
     */
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    
}
