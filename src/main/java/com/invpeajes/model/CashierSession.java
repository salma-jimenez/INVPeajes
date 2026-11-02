package com.invpeajes.model;

/**
 * Turno abierto de un operador de caseta: quién cobra, en qué peaje y en qué caseta,
 * junto con los datos de contacto del peaje que se imprimen en el tiquete.
 * Es inmutable; al elegir la caseta se crea una copia con {@link #withBooth(Caseta)}.
 *
 * @author Luis Guerrero
 * @version 1.1
 */
public final class CashierSession {
    private final String operatorId;
    private final String operatorName;
    private final String role;
    private final int tollId;
    private final String tollName;
    private final String tollStatus;
    private final String tollPhone;
    private final String towPhone;
    private final String concessionaire;
    private final Caseta booth;

    /**
     * Crea la sesión de un operador.
     *
     * @param operatorId Cédula del operador (USUARIO_PEAJE.CEDULA).
     * @param operatorName Nombre y apellido del operador.
     * @param role Cargo del empleado (ej. "OPERADOR DE CASETA").
     * @param tollId ID del peaje donde trabaja.
     * @param tollName Nombre del peaje.
     * @param tollStatus Estado operativo del peaje.
     * @param tollPhone Teléfono de atención del peaje, o null.
     * @param towPhone Teléfono de grúa del peaje, o null.
     * @param concessionaire Nombre del concesionario que administra el peaje, o null.
     * @param booth Caseta del turno, o null si aún no se ha elegido.
     */
    public CashierSession(String operatorId, String operatorName, String role, int tollId,
                          String tollName, String tollStatus, String tollPhone, String towPhone,
                          String concessionaire, Caseta booth) {
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.role = role;
        this.tollId = tollId;
        this.tollName = tollName;
        this.tollStatus = tollStatus;
        this.tollPhone = tollPhone;
        this.towPhone = towPhone;
        this.concessionaire = concessionaire;
        this.booth = booth;
    }

    /**
     * Devuelve una copia de la sesión con la caseta del turno asignada.
     * @param booth Caseta elegida al abrir el turno.
     * @return Nueva sesión con la caseta.
     */
    public CashierSession withBooth(Caseta booth) {
        return new CashierSession(operatorId, operatorName, role, tollId, tollName, tollStatus,
                tollPhone, towPhone, concessionaire, booth);
    }

    /** @return Cédula del operador. */
    public String getOperatorId() { return operatorId; }

    /** @return Nombre y apellido del operador. */
    public String getOperatorName() { return operatorName; }

    /** @return Cargo del empleado. */
    public String getRole() { return role; }

    /** @return ID del peaje. */
    public int getTollId() { return tollId; }

    /** @return Nombre del peaje. */
    public String getTollName() { return tollName; }

    /** @return Estado operativo del peaje. */
    public String getTollStatus() { return tollStatus; }

    /** @return Teléfono de atención del peaje, o null. */
    public String getTollPhone() { return tollPhone; }

    /** @return Teléfono de grúa del peaje, o null. */
    public String getTowPhone() { return towPhone; }

    /** @return Concesionario que administra el peaje, o null. */
    public String getConcessionaire() { return concessionaire; }

    /** @return Caseta del turno, o null si no se ha elegido. */
    public Caseta getBooth() { return booth; }
}
