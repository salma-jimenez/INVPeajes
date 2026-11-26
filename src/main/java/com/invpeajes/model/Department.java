
package com.invpeajes.model;

/**
 * Representa la entidad territorial de mayor nivel en el sistema (Departamento).
 * Almacena los datos básicos de la región a la cual se asocian los municipios.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Department {
    private String departmentId;
    private String name;

    /**
     * Constructor que inicializa un nuevo Departamento.
     * 
     * @param departmentId Identificador alfanumérico único del departamento (Máx 2 caracteres).
     * @param name Nombre oficial del departamento.
     */
    public Department(String departmentId, String name) {
        this.departmentId = departmentId;
        this.name = name;
    }

    /**
     * Obtiene el identificador del departamento.
     * @return El ID del departamento.
     */
    public String getDepartmentId() {
        return departmentId;
    }

    /**
     * Establece o modifica el identificador del departamento.
     * @param departmentId El nuevo ID a asignar.
     */
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    /**
     * Obtiene el nombre del departamento.
     * @return El nombre del departamento.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece o modifica el nombre del departamento.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    
    
}
