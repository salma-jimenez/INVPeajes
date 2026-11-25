package com.invpeajes.service;

/**
 * Canal para enviar un mensaje a una persona (por ahora, correo electrónico).
 *
 * {@link ReceiptMailService} depende de esta interfaz y no de una implementación concreta:
 * se puede agregar otro canal (por ejemplo SMS) o usar uno falso en pruebas sin cambiar
 * el servicio.
 *
 * @version 1.0
 */
public interface Notifier {

    /**
     * Indica si el canal tiene todo lo necesario para enviar (por ejemplo, usuario y
     * contraseña del correo).
     * @return true si se puede enviar.
     */
    boolean isConfigured();

    /**
     * Envía un mensaje.
     * @param to Destinatario (correo).
     * @param subject Asunto.
     * @param textBody Contenido en texto plano, para clientes que no muestran HTML.
     * @param htmlBody Contenido en HTML.
     * @throws NotificationException Si no se pudo enviar.
     */
    void send(String to, String subject, String textBody, String htmlBody);
}
