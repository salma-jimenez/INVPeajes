
package com.invpeajes.model;

/**
 * Representa a una entidad externa que procesa pagos electrónicos mediante TAGs.
 * (Ejemplo: Facilpass, Flypass, Copiloto).
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Intermediador {
    private String idIntermediador;
    private String nombre;

    public Intermediador(String idIntermediador, String nombre) {
        this.idIntermediador = idIntermediador;
        this.nombre = nombre;
    }

    /**
     * Obtiene el identificador numérico del intermediador.
     * @return El ID del intermediador.
     */
    public String getIdIntermediador() { return idIntermediador; }
    public void setIdIntermediador(String idIntermediador) { this.idIntermediador = idIntermediador; }

    /**
     * Obtiene el nombre comercial de la entidad intermediadora.
     * @return El nombre del intermediador.
     */
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
