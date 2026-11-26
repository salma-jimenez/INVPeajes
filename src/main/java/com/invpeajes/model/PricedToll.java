package com.invpeajes.model;

import java.math.BigDecimal;

/**
 * Peaje que devuelve una consulta de la base junto con datos calculados allí:
 * su tarifa para una categoría y, en un viaje, los km desde el origen y hasta el destino.
 * Lo llenan las funciones {@code fun_peajes_corredor} y {@code fun_peajes_en_area}.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class PricedToll {

    private final Toll toll;
    private final BigDecimal price;
    private final Double kmFromOrigin;
    private final Double kmToDestination;

    /**
     * @param toll            peaje
     * @param price           tarifa para la categoría, o null si no tiene
     * @param kmFromOrigin    km desde el origen del viaje, o null si la consulta no lo calcula
     * @param kmToDestination km hasta el destino del viaje, o null si la consulta no lo calcula
     */
    public PricedToll(Toll toll, BigDecimal price, Double kmFromOrigin, Double kmToDestination) {
        this.toll = toll;
        this.price = price;
        this.kmFromOrigin = kmFromOrigin;
        this.kmToDestination = kmToDestination;
    }

    /** @return peaje */
    public Toll getToll() {
        return toll;
    }

    /** @return tarifa para la categoría, o null si no tiene */
    public BigDecimal getPrice() {
        return price;
    }

    /** @return km desde el origen del viaje, o null */
    public Double getKmFromOrigin() {
        return kmFromOrigin;
    }

    /** @return km hasta el destino del viaje, o null */
    public Double getKmToDestination() {
        return kmToDestination;
    }
}
