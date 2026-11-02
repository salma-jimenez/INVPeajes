package com.invpeajes.model;

import java.math.BigDecimal;

/**
 * Precio que cobra un peaje a una categoría vehicular, junto con los datos de la categoría.
 * Se usa para mostrar los botones de categoría en la caja.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public final class CategoryRate {
    private final int categoryId;
    private final String code;
    private final String description;
    private final BigDecimal price;

    /**
     * @param categoryId ID de la categoría (1 a 7).
     * @param code Código romano de la categoría (I a VII).
     * @param description Descripción (ej. "Automóviles, camperos y camionetas").
     * @param price Precio del peaje para esa categoría.
     */
    public CategoryRate(int categoryId, String code, String description, BigDecimal price) {
        this.categoryId = categoryId;
        this.code = code;
        this.description = description;
        this.price = price;
    }

    /** @return ID de la categoría. */
    public int getCategoryId() { return categoryId; }

    /** @return Código romano (I a VII). */
    public String getCode() { return code; }

    /** @return Descripción de la categoría, o null. */
    public String getDescription() { return description; }

    /** @return Precio del peaje para la categoría. */
    public BigDecimal getPrice() { return price; }
}
