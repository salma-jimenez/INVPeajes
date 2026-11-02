package com.invpeajes.ui.cashier;

import com.invpeajes.model.ChargeReceipt;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro de los cobros hechos en el turno actual. La pantalla de cobro agrega cada tiquete
 * y la pantalla "Mis cobros del turno" se actualiza sola cuando cambia.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class ShiftLog {

    private final List<ChargeReceipt> receipts = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    /**
     * Agrega un cobro al inicio de la lista (el más reciente primero) y avisa a las pantallas.
     * @param receipt Cobro confirmado.
     */
    public void add(ChargeReceipt receipt) {
        receipts.add(0, receipt);
        listeners.forEach(Runnable::run);
    }

    /**
     * @param listener Acción a ejecutar cada vez que se agrega un cobro.
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /** @return Cobros del turno, del más reciente al más antiguo (solo lectura). */
    public List<ChargeReceipt> getReceipts() {
        return Collections.unmodifiableList(receipts);
    }

    /** @return Número de cobros del turno. */
    public int count() {
        return receipts.size();
    }

    /** @return Total recaudado en el turno. */
    public BigDecimal total() {
        return receipts.stream().map(ChargeReceipt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * @param paymentMethod EFECTIVO, TARJETA o TAG_COLPASS.
     * @return Total recaudado con ese medio de pago.
     */
    public BigDecimal totalBy(String paymentMethod) {
        return receipts.stream()
                .filter(r -> r.getPaymentMethod().equals(paymentMethod))
                .map(ChargeReceipt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
