package com.invpeajes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que pasa hoy en una caseta, para el apartado "Casetas del peaje" del jefe de estación:
 * cuántos cobros lleva, cuánto ha recaudado y quién hizo el último cobro.
 *
 * <p>La base de datos no guarda los turnos; por eso el operador "en turno" es quien hizo el
 * último cobro de la caseta, si fue hace poco (ver {@link #isAttended}).</p>
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class BoothActivity {

    /** Minutos sin cobros después de los cuales la caseta se muestra sin operador. */
    public static final int ATTENDED_MINUTES = 120;

    private final TollBooth booth;
    private final long tickets;
    private final BigDecimal revenue;
    private final String lastOperatorId;
    private final String lastOperator;
    private final LocalDateTime lastCharge;

    /**
     * @param booth Caseta.
     * @param tickets Cobros de hoy.
     * @param revenue Recaudo de hoy.
     * @param lastOperatorId Cédula de quien hizo el último cobro de hoy, o null.
     * @param lastOperator Nombre de quien hizo el último cobro de hoy, o null.
     * @param lastCharge Fecha y hora del último cobro de hoy, o null.
     */
    public BoothActivity(TollBooth booth, long tickets, BigDecimal revenue, String lastOperatorId,
                         String lastOperator, LocalDateTime lastCharge) {
        this.booth = booth;
        this.tickets = tickets;
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
        this.lastOperatorId = lastOperatorId;
        this.lastOperator = lastOperator;
        this.lastCharge = lastCharge;
    }

    /** @return Caseta. */
    public TollBooth getBooth() {
        return booth;
    }

    /** @return Cobros de hoy. */
    public long getTickets() {
        return tickets;
    }

    /** @return Recaudo de hoy. */
    public BigDecimal getRevenue() {
        return revenue;
    }

    /** @return Cédula de quien hizo el último cobro de hoy, o null. */
    public String getLastOperatorId() {
        return lastOperatorId;
    }

    /** @return Quien hizo el último cobro de hoy, o null. */
    public String getLastOperator() {
        return lastOperator;
    }

    /** @return Hora del último cobro de hoy, o null. */
    public LocalDateTime getLastCharge() {
        return lastCharge;
    }

    /**
     * @param now Fecha y hora actual.
     * @return true si alguien cobró en esta caseta en los últimos {@link #ATTENDED_MINUTES} minutos.
     */
    public boolean isAttended(LocalDateTime now) {
        return lastOperator != null && lastCharge != null
                && lastCharge.isAfter(now.minusMinutes(ATTENDED_MINUTES));
    }

    /** Un cobro reciente del peaje, para la lista de "Últimos cobros". */
    public static final class Charge {
        private final LocalDateTime paidAt;
        private final int boothNumber;
        private final String ticket;
        private final String plate;
        private final String operator;
        private final String method;
        private final BigDecimal amount;

        /**
         * @param paidAt Fecha y hora del cobro.
         * @param boothNumber Número de la caseta.
         * @param ticket Número del tiquete (ej. P001-004113).
         * @param plate Placa.
         * @param operator Nombre del operador, o null.
         * @param method EFECTIVO, TARJETA o TAG_COLPASS.
         * @param amount Valor pagado.
         */
        public Charge(LocalDateTime paidAt, int boothNumber, String ticket, String plate, String operator,
                      String method, BigDecimal amount) {
            this.paidAt = paidAt;
            this.boothNumber = boothNumber;
            this.ticket = ticket;
            this.plate = plate;
            this.operator = operator;
            this.method = method;
            this.amount = amount;
        }

        /** @return Fecha y hora del cobro. */
        public LocalDateTime getPaidAt() {
            return paidAt;
        }

        /** @return Número de la caseta. */
        public int getBoothNumber() {
            return boothNumber;
        }

        /** @return Número del tiquete. */
        public String getTicket() {
            return ticket;
        }

        /** @return Placa. */
        public String getPlate() {
            return plate;
        }

        /** @return Nombre del operador, o null. */
        public String getOperator() {
            return operator;
        }

        /** @return Medio de pago. */
        public String getMethod() {
            return method;
        }

        /** @return Valor pagado. */
        public BigDecimal getAmount() {
            return amount;
        }
    }
}
