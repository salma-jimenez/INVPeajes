package com.invpeajes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de un cobro en caseta, tal como se imprimen en el tiquete.
 * Primero se arma un borrador sin número de tiquete; al guardarlo en la base de datos
 * se completa con {@link #withTicketData}.
 *
 * El recibido y el cambio no se guardan en Oracle (la tabla TIQUETE no tiene esas
 * columnas); solo se muestran e imprimen.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public final class ChargeReceipt {
    private final Long ticketId;
    private final String prefix;
    private final Long consecutive;
    private final LocalDateTime dateTime;
    private final CashierSession session;
    private final CategoryRate category;
    private final String plate;
    private final String paymentMethod;
    private final String tagCode;
    private final BigDecimal received;
    private final BigDecimal change;

    /**
     * Crea el borrador de un cobro (todavía sin número de tiquete).
     *
     * @param session Turno del operador que cobra.
     * @param category Categoría y precio cobrado.
     * @param plate Placa del vehículo.
     * @param paymentMethod EFECTIVO, TARJETA o TAG_COLPASS.
     * @param tagCode Código del TAG (solo con TAG_COLPASS), o null.
     * @param received Valor recibido.
     * @param change Cambio devuelto.
     */
    public ChargeReceipt(CashierSession session, CategoryRate category, String plate,
                         String paymentMethod, String tagCode, BigDecimal received, BigDecimal change) {
        this(null, null, null, null, session, category, plate, paymentMethod, tagCode, received, change);
    }

    private ChargeReceipt(Long ticketId, String prefix, Long consecutive, LocalDateTime dateTime,
                          CashierSession session, CategoryRate category, String plate,
                          String paymentMethod, String tagCode, BigDecimal received, BigDecimal change) {
        this.ticketId = ticketId;
        this.prefix = prefix;
        this.consecutive = consecutive;
        this.dateTime = dateTime;
        this.session = session;
        this.category = category;
        this.plate = plate;
        this.paymentMethod = paymentMethod;
        this.tagCode = tagCode;
        this.received = received;
        this.change = change;
    }

    /**
     * Devuelve una copia del cobro con los datos que asignó la base de datos.
     *
     * @param ticketId ID generado por Oracle.
     * @param prefix Prefijo del tiquete (ej. "P007").
     * @param consecutive Consecutivo dentro del prefijo.
     * @param dateTime Fecha y hora del pago.
     * @return Cobro completo.
     */
    public ChargeReceipt withTicketData(long ticketId, String prefix, long consecutive, LocalDateTime dateTime) {
        return new ChargeReceipt(ticketId, prefix, consecutive, dateTime, session, category,
                plate, paymentMethod, tagCode, received, change);
    }

    /** @return Número legible del tiquete (ej. "P007-000123"), o null si es un borrador. */
    public String getTicketNumber() {
        return prefix == null ? null : String.format("%s-%06d", prefix, consecutive);
    }

    /** @return ID del tiquete en Oracle, o null si es un borrador. */
    public Long getTicketId() { return ticketId; }

    /** @return Prefijo del tiquete. */
    public String getPrefix() { return prefix; }

    /** @return Consecutivo del tiquete. */
    public Long getConsecutive() { return consecutive; }

    /** @return Fecha y hora del pago. */
    public LocalDateTime getDateTime() { return dateTime; }

    /** @return Turno en el que se hizo el cobro. */
    public CashierSession getSession() { return session; }

    /** @return Categoría y precio cobrado. */
    public CategoryRate getCategory() { return category; }

    /** @return Valor del peaje cobrado. */
    public BigDecimal getAmount() { return category.getPrice(); }

    /** @return Placa del vehículo. */
    public String getPlate() { return plate; }

    /** @return Método de pago. */
    public String getPaymentMethod() { return paymentMethod; }

    /** @return Código del TAG, o null. */
    public String getTagCode() { return tagCode; }

    /** @return Valor recibido. */
    public BigDecimal getReceived() { return received; }

    /** @return Cambio devuelto. */
    public BigDecimal getChange() { return change; }
}
