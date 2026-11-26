package com.invpeajes.service;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

/**
 * Envía correos por SMTP (por defecto Gmail) con Jakarta Mail.
 *
 * Igual que la contraseña de la base de datos, los datos de la cuenta se leen de
 * variables de entorno para no guardarlos en el código ni subirlos a GitHub:
 * <ul>
 *   <li>{@code INVPEAJES_MAIL_USER}: correo que envía (ej. rutapeaje.recibos@gmail.com).</li>
 *   <li>{@code INVPEAJES_MAIL_PASSWORD}: contraseña de aplicación de Gmail (no la contraseña normal).</li>
 *   <li>{@code INVPEAJES_MAIL_HOST} y {@code INVPEAJES_MAIL_PORT}: opcionales; por defecto
 *       smtp.gmail.com y 587.</li>
 * </ul>
 *
 * @version 1.0
 */
public class SmtpEmailNotifier implements Notifier {

    public static final String ENV_USER = "INVPEAJES_MAIL_USER";
    public static final String ENV_PASSWORD = "INVPEAJES_MAIL_PASSWORD";
    public static final String ENV_HOST = "INVPEAJES_MAIL_HOST";
    public static final String ENV_PORT = "INVPEAJES_MAIL_PORT";

    private static final String DEFAULT_HOST = "smtp.gmail.com";
    private static final String DEFAULT_PORT = "587";
    private static final String SENDER_NAME = "RutaPeaje Colombia";
    /** Milisegundos máximos para conectar y enviar, para que el envío no quede colgado. */
    private static final String TIMEOUT_MS = "15000";

    private final String user;
    private final String password;
    private final String host;
    private final String port;
    private Session session;

    /**
     * Crea el notificador con los datos de las variables de entorno.
     */
    public SmtpEmailNotifier() {
        this(System.getenv(ENV_USER), System.getenv(ENV_PASSWORD),
                envOrDefault(ENV_HOST, DEFAULT_HOST), envOrDefault(ENV_PORT, DEFAULT_PORT));
    }

    /**
     * Crea el notificador con datos específicos (útil en pruebas o con otro servidor).
     * @param user Correo que envía.
     * @param password Contraseña de aplicación.
     * @param host Servidor SMTP.
     * @param port Puerto SMTP (587 con STARTTLS).
     */
    public SmtpEmailNotifier(String user, String password, String host, String port) {
        this.user = user == null ? null : user.trim();
        // Gmail muestra la contraseña de aplicación en grupos de 4 con espacios: se quitan.
        this.password = password == null ? null : password.replaceAll("\\s", "");
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean isConfigured() {
        return hasText(user) && hasText(password);
    }

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) {
        if (!isConfigured()) {
            throw new NotificationException("El envío de correos no está configurado: faltan las variables de entorno "
                    + ENV_USER + " y " + ENV_PASSWORD);
        }
        try {
            MimeMessage message = new MimeMessage(session());
            message.setFrom(new InternetAddress(user, SENDER_NAME, "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to, true));
            message.setSubject(subject, "UTF-8");
            message.setSentDate(new Date());

            MimeBodyPart text = new MimeBodyPart();
            text.setText(textBody, "UTF-8");
            MimeBodyPart html = new MimeBodyPart();
            html.setContent(htmlBody, "text/html; charset=UTF-8");
            // "alternative": el programa de correo muestra el HTML, o el texto si no puede.
            MimeMultipart body = new MimeMultipart("alternative");
            body.addBodyPart(text);
            body.addBodyPart(html);
            message.setContent(body);

            Transport.send(message);
        } catch (AddressException e) {
            throw new NotificationException("El correo " + to + " no es válido", e);
        } catch (AuthenticationFailedException e) {
            throw new NotificationException("El servidor de correo rechazó el usuario o la contraseña de aplicación", e);
        } catch (SendFailedException e) {
            throw new NotificationException("El servidor de correo no aceptó el destinatario " + to, e);
        } catch (MessagingException e) {
            throw new NotificationException("No se pudo conectar con el servidor de correo "
                    + host + ":" + port + " (revise internet o que la red permita ese puerto)", e);
        } catch (UnsupportedEncodingException e) {
            throw new NotificationException("No se pudo preparar el correo", e);
        }
    }

    /** Crea la sesión SMTP la primera vez y la reutiliza después. */
    private synchronized Session session() {
        if (session == null) {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
            props.put("mail.smtp.timeout", TIMEOUT_MS);
            props.put("mail.smtp.writetimeout", TIMEOUT_MS);
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            });
        }
        return session;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return hasText(value) ? value.trim() : defaultValue;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
