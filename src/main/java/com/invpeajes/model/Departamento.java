
package com.invpeajes.model;

/**
 * Representa la entidad territorial de mayor nivel en el sistema (Departamento).
 * Almacena los datos básicos de la región a la cual se asocian los municipios.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Departamento {
    private String idDepartamento;
    private String nombreDepartamento;

    /**
     * Constructor que inicializa un nuevo Departamento.
     * 
     * @param idDepartamento Identificador alfanumérico único del departamento (Máx 2 caracteres).
     * @param nombreDepartamento Nombre oficial del departamento.
     */
    public Departamento(String idDepartamento, String nombreDepartamento) {
        this.idDepartamento = idDepartamento;
        this.nombreDepartamento = nombreDepartamento;
    }

    /**
     * Obtiene el identificador del departamento.
     * @return El ID del departamento.
     */
    public String getIdDepartamento() {
        return idDepartamento;
    }

    /**
     * Establece o modifica el identificador del departamento.
     * @param idDepartamento El nuevo ID a asignar.
     */
    public void setIdDepartamento(String idDepartamento) {
        this.idDepartamento = idDepartamento;
    }

    /**
     * Obtiene el nombre del departamento.
     * @return El nombre del departamento.
     */
    public String getNombreDepartamento() {
        return nombreDepartamento;
    }

    /**
     * Establece o modifica el nombre del departamento.
     * @param nombreDepartamento El nuevo nombre a asignar.
     */
    public void setNombreDepartamento(String nombreDepartamento) {
        this.nombreDepartamento = nombreDepartamento;
    }
    
    
    
}
