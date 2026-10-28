
package com.invpeajes.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author salma
 */
/**
 * Representa un tiquete de cobro emitido en una caseta de peaje.
 * <p>
 * Corresponde a la tabla {@code PEAJE.TIQUETE}. El valor total a pagar no se
 * asigna directamente: siempre se calcula como valor del peaje más valor de
 * servicios, igual que exige la restricción {@code CK_TIQUETE_TOTAL}.
 * </p>
 */
public class Ticket {
    
    private Long id;
    private String prefix;
    private long sequenceNumber;
    private LocalDateTime paymentDate;
    private String paymentTime;
    private BigDecimal tollAmount;
    private BigDecimal serviceAmount;
    private PaymentMethod paymentMethod;
    private String tagCode;
    private String licensePlate;
    private int categoryId;
    private long boothId;
    private String operatorId;
 
    /**
     * Crea un tiquete vacío con valor de servicios en cero.
     * Lo usa la capa de persistencia al leer registros de la base de datos.
     */
    public Ticket() {
        this.serviceAmount = BigDecimal.ZERO;
    }
 
    /**
     * Crea un tiquete nuevo listo para registrarse.
     * El identificador queda en {@code null} porque Oracle lo genera automáticamente.
     *
     * @param prefix         prefijo del tiquete, normalmente asociado al peaje (ej. "P008")
     * @param sequenceNumber número consecutivo del tiquete dentro del prefijo
     * @param paymentDate    fecha y hora del pago
     * @param tollAmount     valor del peaje según la categoría del vehículo
     * @param serviceAmount  valor de servicios adicionales
     * @param paymentMethod  método de pago utilizado
     * @param tagCode        código del TAG; solo aplica si el pago es con TAG Colpass
     * @param licensePlate   placa del vehículo
     * @param categoryId     identificador de la categoría vehicular cobrada
     * @param boothId        identificador de la caseta donde se emitió el tiquete
     * @param operatorId     cédula del operador que registró el cobro
     */
    public Ticket(String prefix, long sequenceNumber, LocalDateTime paymentDate,
                  BigDecimal tollAmount, BigDecimal serviceAmount, PaymentMethod paymentMethod,
                  String tagCode, String licensePlate, int categoryId, long boothId,
                  String operatorId) {
        this.prefix = prefix;
        this.sequenceNumber = sequenceNumber;
        this.paymentDate = paymentDate;
        validateNotNegative(tollAmount, "El valor del peaje");
        this.tollAmount = tollAmount;
        this.serviceAmount = serviceAmount == null ? BigDecimal.ZERO : serviceAmount;
        validateNotNegative(this.serviceAmount, "El valor de servicios");
        this.paymentMethod = paymentMethod;
        this.tagCode = tagCode;
        this.licensePlate = licensePlate;
        this.categoryId = categoryId;
        this.boothId = boothId;
        this.operatorId = operatorId;
    }
 
    /**
     * Calcula el valor total que paga el usuario.
     *
     * @return valor del peaje más valor de servicios
     */
    public BigDecimal getTotalAmount() {
        BigDecimal toll = tollAmount == null ? BigDecimal.ZERO : tollAmount;
        BigDecimal services = serviceAmount == null ? BigDecimal.ZERO : serviceAmount;
        return toll.add(services);
    }
 
    /**
     * Indica si el tiquete se pagó con TAG Colpass.
     *
     * @return {@code true} si el método de pago es TAG Colpass
     */
    public boolean isPaidWithTag() {
        return paymentMethod == PaymentMethod.TAG_COLPASS;
    }
 
    /**
     * Devuelve el número visible del tiquete, formado por prefijo y consecutivo.
     *
     * @return número del tiquete, por ejemplo "P008-4"
     */
    public String getTicketNumber() {
        return prefix + "-" + sequenceNumber;
    }
 
    /**
     * @return identificador del tiquete, o {@code null} si aún no se ha guardado
     */
    public Long getId() {
        return id;
    }
 
    /**
     * @param id identificador asignado por la base de datos
     */
    public void setId(Long id) {
        this.id = id;
    }
 
    /**
     * @return prefijo del tiquete
     */
    public String getPrefix() {
        return prefix;
    }
 
    /**
     * @param prefix prefijo del tiquete
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
 
    /**
     * @return número consecutivo dentro del prefijo
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }
 
    /**
     * @param sequenceNumber número consecutivo dentro del prefijo
     */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
 
    /**
     * @return fecha y hora del pago
     */
    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
 
    /**
     * @param paymentDate fecha y hora del pago
     */
    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
 
    /**
     * @return hora del pago en texto (formato HH:mm)
     */
    public String getPaymentTime() {
        return paymentTime;
    }
 
    /**
     * @param paymentTime hora del pago en texto (formato HH:mm)
     */
    public void setPaymentTime(String paymentTime) {
        this.paymentTime = paymentTime;
    }
 
    /**
     * @return valor del peaje
     */
    public BigDecimal getTollAmount() {
        return tollAmount;
    }
 
    /**
     * @param tollAmount valor del peaje; no puede ser negativo
     * @throws IllegalArgumentException si el valor es negativo
     */
    public void setTollAmount(BigDecimal tollAmount) {
        validateNotNegative(tollAmount, "El valor del peaje");
        this.tollAmount = tollAmount;
    }
 
    /**
     * @return valor de servicios adicionales
     */
    public BigDecimal getServiceAmount() {
        return serviceAmount;
    }
 
    /**
     * @param serviceAmount valor de servicios adicionales; si es {@code null} se toma como cero
     * @throws IllegalArgumentException si el valor es negativo
     */
    public void setServiceAmount(BigDecimal serviceAmount) {
        BigDecimal value = serviceAmount == null ? BigDecimal.ZERO : serviceAmount;
        validateNotNegative(value, "El valor de servicios");
        this.serviceAmount = value;
    }
 
    /**
     * @return método de pago
     */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
 
    /**
     * @param paymentMethod método de pago
     */
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
 
    /**
     * @return código del TAG, o {@code null} si no se pagó con TAG
     */
    public String getTagCode() {
        return tagCode;
    }
 
    /**
     * @param tagCode código del TAG Colpass
     */
    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }
 
    /**
     * @return placa del vehículo
     */
    public String getLicensePlate() {
        return licensePlate;
    }
 
    /**
     * @param licensePlate placa del vehículo
     */
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
 
    /**
     * @return identificador de la categoría vehicular
     */
    public int getCategoryId() {
        return categoryId;
    }
 
    /**
     * @param categoryId identificador de la categoría vehicular
     */
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
 
    /**
     * @return identificador de la caseta
     */
    public long getBoothId() {
        return boothId;
    }
 
    /**
     * @param boothId identificador de la caseta
     */
    public void setBoothId(long boothId) {
        this.boothId = boothId;
    }
 
    /**
     * @return cédula del operador que registró el cobro
     */
    public String getOperatorId() {
        return operatorId;
    }
 
    /**
     * @param operatorId cédula del operador que registró el cobro
     */
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
 
    private static void validateNotNegative(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " no puede ser negativo.");
        }
    }
 
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Ticket)) {
            return false;
        }
        Ticket ticket = (Ticket) other;
        return sequenceNumber == ticket.sequenceNumber && Objects.equals(prefix, ticket.prefix);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(prefix, sequenceNumber);
    }
 
    @Override
    public String toString() {
        return "Tiquete " + getTicketNumber()
                + " | Placa: " + licensePlate
                + " | Fecha: " + paymentDate
                + " | Peaje: $" + tollAmount
                + " | Servicios: $" + serviceAmount
                + " | Total: $" + getTotalAmount()
                + " | Pago: " + (paymentMethod == null ? "-" : paymentMethod.getDatabaseValue());
    }

}
