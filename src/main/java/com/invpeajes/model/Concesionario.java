
package com.invpeajes.model;

/**
 * Representa a la entidad o empresa administradora de las estaciones de peaje (Concesionario).
 * Ejemplos de esta entidad pueden ser INVIAS, ANI o concesiones privadas.
 * Es una clase base de la cual dependen las estaciones de peaje.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Concesionario {
    private String idConcesionario;
    private String nombreConcesionario;

    /**
     * Constructor que inicializa un nuevo Concesionario con sus datos obligatorios.
     * 
     * @param idConcesionario Identificador alfanumérico único (NIT o código interno).
     * @param nombreConcesionario Nombre oficial de la empresa o entidad administradora.
     */
    public Concesionario(String idConcesionario, String nombreConcesionario) {
        this.idConcesionario = idConcesionario;
        this.nombreConcesionario = nombreConcesionario;
    }

    /**
     * Obtiene el identificador del concesionario.
     * @return El ID o NIT del concesionario.
     */
    public String getIdConcesionario() {
        return idConcesionario;
    }

    /**
     * Establece o modifica el identificador del concesionario.
     * @param idConcesionario El nuevo ID a asignar.
     */
    public void setIdConcesionario(String idConcesionario) {
        this.idConcesionario = idConcesionario;
    }

    /**
     * Obtiene el nombre del concesionario.
     * @return El nombre oficial del concesionario.
     */
    public String getNombreConcesionario() {
        return nombreConcesionario;
    }

    /**
     * Establece o modifica el nombre del concesionario.
     * @param nombreConcesionario El nuevo nombre a asignar.
     */
    public void setNombreConcesionario(String nombreConcesionario) {
        this.nombreConcesionario = nombreConcesionario;
    }
}