
package com.invpeajes.service;

/**
 *
 * Error de regla de negocio o de datos de entrada inválidos.
 * La UI puede mostrar su mensaje directamente al usuario.
 *  @author Salomejimenez
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje para el usuario.
     *
     * @param message explicación de la regla incumplida
     */
    public ValidationException(String message) {
        super(message);
    }
    
}
