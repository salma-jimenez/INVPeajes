package com.invpeajes.service;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.ChargeReceipt;
import com.invpeajes.model.Traveler;
import com.invpeajes.persistence.DataAccessException;
import com.invpeajes.persistence.TravelerDAO;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Envía el recibo de un cobro al correo de los viajeros registrados con esa placa.
 *
 * Se llama después de guardar el tiquete y en segundo plano: si el correo falla, el cobro
 * ya quedó registrado y no se deshace. Si la placa no tiene viajero registrado, no se envía
 * nada (no es un error: la mayoría de vehículos que pasan no tienen cuenta).
 *
 * Errores que lanza:
 * <ul>
 *   <li>{@link DataAccessException}: falló la consulta de viajeros.</li>
 *   <li>{@link NotificationException}: hay viajeros pero no se pudo enviar a ninguno,
 *       o el correo no está configurado.</li>
 * </ul>
 *
 * @version 1.0
 */
public class ReceiptMailService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
    private static final String FONT = "font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;";
    private static final Map<String, String> METHOD_LABELS = new HashMap<>();

    static {
        METHOD_LABELS.put(CashierService.CASH, "Efectivo");
        METHOD_LABELS.put(CashierService.CARD, "Tarjeta");
        METHOD_LABELS.put(CashierService.TAG, "TAG Colpass");
    }

    private final TravelerDAO travelerDAO;
    private final Notifier notifier;

    /**
     * Crea el servicio con la base de datos y el correo configurado en las variables de entorno.
     */
    public ReceiptMailService() {
        this(new TravelerDAO(), new SmtpEmailNotifier());
    }

    /**
     * Crea el servicio con dependencias específicas (permite usar datos falsos en pruebas).
     * @param travelerDAO DAO de viajeros.
     * @param notifier Canal de envío.
     */
    public ReceiptMailService(TravelerDAO travelerDAO, Notifier notifier) {
        this.travelerDAO = travelerDAO;
        this.notifier = notifier;
    }

    /**
     * Envía el recibo a los viajeros registrados con la placa del cobro.
     * @param receipt Cobro ya guardado (con número de tiquete).
     * @return Correos a los que se envió; vacío si la placa no tiene viajero registrado.
     * @throws IllegalArgumentException Si el cobro todavía no tiene número de tiquete.
     * @throws NotificationException Si no se pudo enviar a ningún viajero.
     * @throws DataAccessException Si falla la consulta de viajeros.
     */
    public List<String> sendReceipt(ChargeReceipt receipt) {
        if (receipt == null || receipt.getTicketNumber() == null) {
            throw new IllegalArgumentException("Solo se puede enviar el recibo de un cobro ya guardado");
        }
        Map<String, Traveler> owners = ownersByEmail(receipt.getPlate());
        if (owners.isEmpty()) {
            return new ArrayList<>();
        }
        if (!notifier.isConfigured()) {
            throw new NotificationException("El envío de recibos por correo no está configurado");
        }

        String receiptText = ReceiptFormatter.format(receipt, false);
        String subject = "Recibo de peaje " + receipt.getTicketNumber() + " · " + receipt.getSession().getTollName();
        List<String> sent = new ArrayList<>();
        NotificationException lastError = null;
        for (Map.Entry<String, Traveler> owner : owners.entrySet()) {
            try {
                notifier.send(owner.getKey(), subject,
                        buildText(receipt, owner.getValue(), receiptText),
                        buildHtml(receipt, owner.getValue()));
                sent.add(owner.getKey());
            } catch (NotificationException e) {
                lastError = e;
            }
        }
        if (sent.isEmpty() && lastError != null) {
            throw lastError;
        }
        return sent;
    }

    /**
     * Oculta parte de un correo para mostrarlo en la caseta (ej. "la***@gmail.com").
     * @param email Correo completo.
     * @return Correo con la parte del usuario oculta.
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String userPart = email.substring(0, at);
        String visible = userPart.length() <= 2 ? userPart.substring(0, 1) : userPart.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    /** Viajeros con esa placa y con correo, sin repetir correos. */
    private Map<String, Traveler> ownersByEmail(String plate) {
        Map<String, Traveler> owners = new LinkedHashMap<>();
        for (Traveler t : travelerDAO.findByPlate(plate)) {
            String email = t.getEmail() == null ? "" : t.getEmail().trim();
            if (!email.isEmpty()) {
                owners.putIfAbsent(email.toLowerCase(Locale.ROOT), t);
            }
        }
        return owners;
    }

    private String buildText(ChargeReceipt r, Traveler t, String receiptText) {
        return "Hola " + t.getFirstName() + ",\n\n"
                + intro(r) + "\n\n"
                + receiptText + "\n\n"
                + "Este correo se envió automáticamente desde RutaPeaje; por favor no lo responda.";
    }

    /**
     * Arma el correo en HTML. El tiquete se muestra como tabla (etiqueta a la izquierda,
     * valor a la derecha) para que se ajuste al ancho de cualquier pantalla, incluido el celular.
     * Se usan tablas y estilos en línea porque los programas de correo no aceptan CSS moderno.
     */
    private String buildHtml(ChargeReceipt r, Traveler t) {
        CashierSession s = r.getSession();
        StringBuilder rows = new StringBuilder();

        section(rows, "Peaje");
        row(rows, "Peaje", s.getTollName());
        if (hasText(s.getConcessionaire())) {
            row(rows, "Concesión", s.getConcessionaire());
        }
        row(rows, "Caseta", String.valueOf(s.getBooth().getBoothNumber()));
        row(rows, "Operador", s.getOperatorName());

        section(rows, "Vehículo");
        row(rows, "Placa", r.getPlate());
        String category = "Categoría " + r.getCategory().getCode();
        if (hasText(r.getCategory().getDescription())) {
            category += " · " + r.getCategory().getDescription();
        }
        row(rows, "Categoría", category);
        row(rows, "Medio de pago", METHOD_LABELS.getOrDefault(r.getPaymentMethod(), r.getPaymentMethod()));
        if (r.getTagCode() != null) {
            row(rows, "TAG", r.getTagCode());
        }

        section(rows, "Pago");
        row(rows, "Valor peaje", ReceiptFormatter.money(r.getAmount()));
        if (CashierService.CASH.equals(r.getPaymentMethod())) {
            row(rows, "Recibido", ReceiptFormatter.money(r.getReceived()));
            row(rows, "Cambio", ReceiptFormatter.money(r.getChange()));
        }

        section(rows, "Contacto del peaje");
        if (hasText(s.getTollPhone())) {
            row(rows, "Atención", ReceiptFormatter.clean(s.getTollPhone()));
        }
        if (hasText(s.getTowPhone())) {
            row(rows, "Grúa y emergencias", ReceiptFormatter.clean(s.getTowPhone()));
        }
        row(rows, "Línea nacional", ReceiptFormatter.ROAD_EMERGENCY);

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#F1F4F9\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#F1F4F9\">"
                + "<tr><td align=\"center\" style=\"padding:16px 10px\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:480px;"
                + "background:#FFFFFF;border:1px solid #E2E7EF;border-radius:10px;" + FONT + "color:#1F2937\">"
                // Encabezado
                + "<tr><td style=\"background:#0F2A5F;border-radius:10px 10px 0 0;padding:18px 20px\">"
                + "<div style=\"font-size:20px;font-weight:bold;color:#FFFFFF\">RutaPeaje</div>"
                + "<div style=\"font-size:13px;color:#DCE4F2\">Recibo de pago de peaje</div></td></tr>"
                // Saludo
                + "<tr><td style=\"padding:20px 20px 4px;font-size:15px;line-height:1.5\">"
                + "Hola <b>" + escape(t.getFirstName()) + "</b>, su vehículo <b>" + escape(r.getPlate())
                + "</b> pasó por el peaje <b>" + escape(s.getTollName()) + "</b>.</td></tr>"
                // Total y número de tiquete
                + "<tr><td style=\"padding:12px 20px\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#EAF1FE;border-radius:8px\"><tr><td style=\"padding:14px 16px\">"
                + "<div style=\"font-size:13px;color:#6B7280\">Total pagado</div>"
                + "<div style=\"font-size:28px;font-weight:bold;color:#1D4ED8\">"
                + escape(ReceiptFormatter.money(r.getAmount())) + "</div>"
                + "<div style=\"font-size:13px;color:#374151;margin-top:4px\">Tiquete <b>"
                + escape(r.getTicketNumber()) + "</b> · " + escape(r.getDateTime().format(DATE)) + "</div>"
                + "</td></tr></table></td></tr>"
                // Detalle
                + "<tr><td style=\"padding:4px 20px 16px\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px\">"
                + rows + "</table></td></tr>"
                // Pie
                + "<tr><td style=\"padding:12px 20px;border-top:1px solid #E2E7EF;font-size:12px;color:#6B7280\">"
                + "Gracias por su viaje. ¡Buen camino!<br>"
                + "Este correo se envió automáticamente desde RutaPeaje; por favor no lo responda.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    /** Título de un grupo de filas del detalle (ej. "Vehículo"). */
    private static void section(StringBuilder rows, String title) {
        rows.append("<tr><td colspan=\"2\" style=\"padding:16px 0 6px;font-size:12px;font-weight:bold;")
            .append("letter-spacing:0.5px;text-transform:uppercase;color:#6B7280;border-bottom:1px solid #E2E7EF\">")
            .append(escape(title)).append("</td></tr>");
    }

    /** Fila del detalle: etiqueta a la izquierda y valor a la derecha; el valor baja de línea si no cabe. */
    private static void row(StringBuilder rows, String label, String value) {
        rows.append("<tr><td valign=\"top\" style=\"padding:7px 10px 7px 0;color:#6B7280;width:40%\">")
            .append(escape(label)).append("</td>")
            .append("<td valign=\"top\" align=\"right\" style=\"padding:7px 0;color:#1F2937;font-weight:600;")
            .append("word-break:break-word\">").append(escape(value)).append("</td></tr>");
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String intro(ChargeReceipt r) {
        return "Su vehículo de placa " + r.getPlate() + " pasó por el peaje " + r.getSession().getTollName()
                + " el " + r.getDateTime().format(DATE) + ". Este es su recibo de pago (tiquete "
                + r.getTicketNumber() + ").";
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
