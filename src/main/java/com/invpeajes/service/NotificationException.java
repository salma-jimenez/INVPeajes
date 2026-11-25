package com.invpeajes.service;

/**
 * Error al enviar una notificación (por ejemplo, un correo), con un mensaje en español
 * que se puede mostrar en la interfaz.
 *
 * @version 1.0
 */
public class NotificationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message Mensaje para el usuario.
     */
    public NotificationException(String message) {
        super(message);
    }

    /**
     * @param message Mensaje para el usuario.
     * @param cause Error original.
     */
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
