
package com.invpeajes.model;

/**
 * Representa a una entidad externa que procesa pagos electrónicos mediante TAGs.
 * (Ejemplo: Facilpass, Flypass, Copiloto).
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Intermediador {
    private int idIntermediador;
    private String nombre;

    /**
     * Constructor que inicializa un intermediador.
     *
     * @param idIntermediador Identificador numérico único del intermediador.
     * @param nombre Nombre comercial (único en la BD, máximo 40 caracteres).
     */
    public Intermediador(int idIntermediador, String nombre) {
        this.idIntermediador = idIntermediador;
        this.nombre = nombre;
    }

    /**
     * Obtiene el identificador numérico del intermediador.
     * @return El ID del intermediador.
     */
    public int getIdIntermediador() {
        return idIntermediador; 
    }
    
    /**
     * Establece el identificador del intermediador.
     * @param idIntermediador El nuevo ID a asignar.
     */
    public void setIdIntermediador(int idIntermediador) {
        this.idIntermediador = idIntermediador;
    }

    /**
     * Obtiene el nombre comercial de la entidad intermediadora.
     * @return El nombre del intermediador.
     */
    public String getNombre() {
        return nombre; 
    }
    
    /**
     * Establece el nombre comercial de la entidad intermediadora.
     * @param nombre El nuevo nombre a asignar.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre; 
    }
}
