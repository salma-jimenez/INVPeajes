
package com.invpeajes.model;

/**
 * Representa a una entidad externa que procesa pagos electrónicos mediante TAGs.
 * (Ejemplo: Facilpass, Flypass, Copiloto).
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Intermediary {
    private int intermediaryId;
    private String name;

    /**
     * Constructor que inicializa un intermediador.
     *
     * @param intermediaryId Identificador numérico único del intermediador.
     * @param name Nombre comercial (único en la BD, máximo 40 caracteres).
     */
    public Intermediary(int intermediaryId, String name) {
        this.intermediaryId = intermediaryId;
        this.name = name;
    }

    /**
     * Obtiene el identificador numérico del intermediador.
     * @return El ID del intermediador.
     */
    public int getIntermediaryId() {
        return intermediaryId; 
    }
    
    /**
     * Establece el identificador del intermediador.
     * @param intermediaryId El nuevo ID a asignar.
     */
    public void setIntermediaryId(int intermediaryId) {
        this.intermediaryId = intermediaryId;
    }

    /**
     * Obtiene el nombre comercial de la entidad intermediadora.
     * @return El nombre del intermediador.
     */
    public String getName() {
        return name; 
    }
    
    /**
     * Establece el nombre comercial de la entidad intermediadora.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name; 
    }
}
