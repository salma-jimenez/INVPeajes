
package com.invpeajes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * @author Salomejimenez
 */
public class Assistance {

    private int assistanceId;
    private AssistanceType type;
    private LocalDateTime requestDate;
    private Double incidentLatitude;
    private Double incidentLongitude;
    private AssistanceStatus currentStatus;
    private String notes;
    private String contactPhone;
    private BigDecimal cost;
    private String plate;
    private int tollId;
    private Integer resourceId;
    private Long ticketId;

    /** Constructor vacío requerido para mapear filas desde la base de datos. */
    public Assistance() {
        this.currentStatus = AssistanceStatus.REGISTERED;
        this.cost = BigDecimal.ZERO;
    }

    /**
     * Crea una nueva solicitud de asistencia en estado REGISTERED.
     *
     * @param type         tipo de auxilio solicitado
     * @param tollId       peaje desde el que se atiende
     * @param plate        placa del vehículo afectado (puede ser null)
     * @param contactPhone teléfono de quien solicita
     * @param notes        descripción del incidente
     */
    public Assistance(AssistanceType type, int tollId, String plate, String contactPhone, String notes) {
        this();
        this.type = type;
        this.tollId = tollId;
        this.plate = plate;
        this.contactPhone = contactPhone;
        this.notes = notes;
        this.requestDate = LocalDateTime.now();
    }

    public int getAssistanceId() {
        return assistanceId;
    }

    public void setAssistanceId(int assistanceId) {
        this.assistanceId = assistanceId;
    }

    public AssistanceType getType() {
        return type;
    }

    public void setType(AssistanceType type) {
        this.type = type;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public Double getIncidentLatitude() {
        return incidentLatitude;
    }

    public void setIncidentLatitude(Double incidentLatitude) {
        this.incidentLatitude = incidentLatitude;
    }

    public Double getIncidentLongitude() {
        return incidentLongitude;
    }

    public void setIncidentLongitude(Double incidentLongitude) {
        this.incidentLongitude = incidentLongitude;
    }

    public AssistanceStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(AssistanceStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public int getTollId() {
        return tollId;
    }

    public void setTollId(int tollId) {
        this.tollId = tollId;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | Peaje %d | Placa %s | Costo $%s",
                assistanceId, type == null ? "-" : type.getDbValue(),
                currentStatus.getDbValue(), tollId, plate == null ? "-" : plate, cost);
    }
   }
