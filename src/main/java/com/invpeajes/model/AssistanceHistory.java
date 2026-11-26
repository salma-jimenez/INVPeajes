
package com.invpeajes.model;

import java.time.LocalDateTime;

/**
 * Registro de un cambio de estado de una asistencia vial, para trazabilidad.
 * @author Salomejimenez
 */
public class AssistanceHistory {
     private long historyId;
    private AssistanceStatus previousStatus;
    private AssistanceStatus newStatus;
    private LocalDateTime changeDate;
    private String notes;
    private int assistanceId;

    /** Constructor vacío requerido para mapear filas desde la base de datos. */
    public AssistanceHistory() {
    }

    /**
     * Crea un registro de cambio de estado con la fecha actual.
     *
     * @param assistanceId   asistencia que cambió de estado
     * @param previousStatus estado previo (null si es el registro inicial)
     * @param newStatus      estado al que pasó
     * @param notes          motivo o detalle del cambio
     */
    public AssistanceHistory(int assistanceId, AssistanceStatus previousStatus,
                             AssistanceStatus newStatus, String notes) {
        this.assistanceId = assistanceId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.notes = notes;
        this.changeDate = LocalDateTime.now();
    }

    public long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(long historyId) {
        this.historyId = historyId;
    }

    public AssistanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(AssistanceStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public AssistanceStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AssistanceStatus newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getAssistanceId() {
        return assistanceId;
    }

    public void setAssistanceId(int assistanceId) {
        this.assistanceId = assistanceId;
    }

    @Override
    public String toString() {
        return String.format("%s | %s → %s | %s", changeDate,
                previousStatus == null ? "(inicio)" : previousStatus.getDbValue(),
                newStatus.getDbValue(), notes == null ? "" : notes);
    }
    
}
