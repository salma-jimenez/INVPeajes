package com.invpeajes.service;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.ChargeReceipt;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el texto del tiquete de un cobro con ancho fijo (impresora térmica de 80 mm).
 *
 * Lo usan la ventana del tiquete ({@code ReceiptDialog}) para mostrarlo e imprimirlo y
 * {@link ReceiptMailService} para enviarlo por correo, así el recibo impreso y el del
 * correo siempre son iguales. Está en la capa de servicio para que el envío del correo
 * no dependa de clases de la interfaz gráfica.
 *
 * @version 1.0
 */
public final class ReceiptFormatter {

    /** Ancho del tiquete en caracteres (impresora térmica de 80 mm). */
    public static final int WIDTH = 40;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    /** Línea de la Policía de Carreteras, para peajes sin teléfono registrado. */
    static final String ROAD_EMERGENCY = "#767 Policía de Carreteras";
    private static final DecimalFormat MONEY = createMoneyFormat();

    private ReceiptFormatter() {
    }

    /**
     * Arma el texto del tiquete.
     * @param r Cobro guardado.
     * @param copy true si es una reimpresión (el tiquete dice "COPIA").
     * @return Texto del tiquete, una línea por renglón.
     */
    public static String format(ChargeReceipt r, boolean copy) {
        CashierSession s = r.getSession();
        String line = "-".repeat(WIDTH);
        List<String> out = new ArrayList<>();
        out.add(center("RUTAPEAJE COLOMBIA"));
        out.add(center("Peaje " + s.getTollName()));
        if (s.getConcessionaire() != null) {
            wrap(s.getConcessionaire()).forEach(l -> out.add(center(l)));
        }
        out.add(center("Caseta " + s.getBooth().getBoothNumber()));
        if (copy) {
            out.add(center("*** COPIA ***"));
        }
        out.add(line);
        out.add(pair("Tiquete:", r.getTicketNumber()));
        out.add(pair("Fecha:", r.getDateTime().format(DATE)));
        out.add(pair("Operador:", s.getOperatorName()));
        out.add(line);
        out.add(pair("Placa:", r.getPlate()));
        out.add(pair("Categoría:", r.getCategory().getCode()));
        if (r.getCategory().getDescription() != null) {
            out.addAll(wrap(r.getCategory().getDescription()));
        }
        out.add(pair("Medio de pago:", r.getPaymentMethod()));
        if (r.getTagCode() != null) {
            out.add(pair("TAG:", r.getTagCode()));
        }
        out.add(line);
        out.add(pair("VALOR PEAJE", money(r.getAmount())));
        if (CashierService.CASH.equals(r.getPaymentMethod())) {
            out.add(pair("RECIBIDO", money(r.getReceived())));
            out.add(pair("CAMBIO", money(r.getChange())));
        }
        out.add(line);
        out.add(center("Gracias por su viaje. ¡Buen camino!"));
        out.add(line);
        out.add("CONTACTO DEL PEAJE");
        boolean anyPhone = false;
        if (hasText(s.getTollPhone())) {
            out.add("Atención:");
            wrap(clean(s.getTollPhone())).forEach(l -> out.add("  " + l));
            anyPhone = true;
        }
        if (hasText(s.getTowPhone())) {
            out.add("Grúa y emergencias en la vía:");
            wrap(clean(s.getTowPhone())).forEach(l -> out.add("  " + l));
            anyPhone = true;
        }
        out.add(anyPhone ? "Línea nacional:" : "Emergencias en la vía (línea nacional):");
        out.add("  " + ROAD_EMERGENCY);
        return String.join("\n", out);
    }

    /**
     * Formatea un valor en pesos colombianos sin decimales (ej. "$27.300").
     * @param value Valor a formatear.
     * @return Texto con el valor.
     */
    public static String money(BigDecimal value) {
        synchronized (MONEY) {
            return value == null ? "$0" : MONEY.format(value);
        }
    }

    private static DecimalFormat createMoneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("$#,##0", symbols);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Une espacios repetidos y quita guiones sueltos de los teléfonos de los datos abiertos. */
    static String clean(String phones) {
        return phones.replaceAll("-{2,}", " ").replaceAll("\\s+", " ").trim();
    }

    /** Parte un texto largo en líneas del ancho del tiquete, sin cortar palabras. */
    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            int limit = WIDTH - 2;
            if (current.length() > 0 && current.length() + 1 + word.length() > limit) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word.length() > limit ? word.substring(0, limit) : word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String center(String text) {
        String t = text.length() <= WIDTH ? text : text.substring(0, WIDTH);
        int pad = (WIDTH - t.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + t;
    }

    private static String pair(String label, String value) {
        int spaces = WIDTH - label.length() - value.length();
        return spaces >= 1 ? label + " ".repeat(spaces) + value : label + " " + value;
    }
}
