package com.invpeajes.model;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
 
/**
 * Representa la tarifa que cobra un peaje a una categoría vehicular.
 * <p>
 * Corresponde a la tabla {@code PEAJE.PEAJE_CATEGORIA}. Cada tarifa se
 * identifica por la combinación de peaje y categoría, es decir, un peaje
 * tiene como máximo una tarifa por categoría.
 * </p>
 */
public class TollRate {
 
    private int tollId;
    private int categoryId;
    private BigDecimal price;
    private LocalDate effectiveDate;
    private String source;
 
    /**
     * Crea una tarifa vacía.
     * Lo usa la capa de persistencia al leer registros de la base de datos.
     */
    public TollRate() {
    }
 
    /**
     * Crea una tarifa con todos sus datos.
     *
     * @param tollId        identificador del peaje
     * @param categoryId    identificador de la categoría vehicular
     * @param price         valor que se cobra; no puede ser negativo
     * @param effectiveDate fecha desde la que rige la tarifa (opcional)
     * @param source        entidad que publica la tarifa: ANI o INVIAS (opcional)
     * @throws IllegalArgumentException si el precio es negativo
     */
    public TollRate(int tollId, int categoryId, BigDecimal price, LocalDate effectiveDate, String source) {
        validateNotNegative(price);
        this.tollId = tollId;
        this.categoryId = categoryId;
        this.price = price;
        this.effectiveDate = effectiveDate;
        this.source = source;
    }
 
    /**
     * @return identificador del peaje
     */
    public int getTollId() {
        return tollId;
    }
 
    /**
     * @param tollId identificador del peaje
     */
    public void setTollId(int tollId) {
        this.tollId = tollId;
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
     * @return valor que se cobra
     */
    public BigDecimal getPrice() {
        return price;
    }
 
    /**
     * @param price valor que se cobra; no puede ser negativo
     * @throws IllegalArgumentException si el precio es negativo
     */
    public void setPrice(BigDecimal price) {
        validateNotNegative(price);
        this.price = price;
    }
 
    /**
     * @return fecha desde la que rige la tarifa, o {@code null} si no se conoce
     */
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
 
    /**
     * @param effectiveDate fecha desde la que rige la tarifa
     */
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
 
    /**
     * @return entidad que publica la tarifa (ANI o INVIAS), o {@code null} si no se conoce
     */
    public String getSource() {
        return source;
    }
 
    /**
     * @param source entidad que publica la tarifa: ANI o INVIAS
     */
    public void setSource(String source) {
        this.source = source;
    }
 
    private static void validateNotNegative(BigDecimal value) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException("El precio de la tarifa no puede ser negativo.");
        }
    }
 
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TollRate)) {
            return false;
        }
        TollRate rate = (TollRate) other;
        return tollId == rate.tollId && categoryId == rate.categoryId;
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(tollId, categoryId);
    }
 
    @Override
    public String toString() {
        return "Tarifa peaje " + tollId
                + " | Categoría: " + categoryId
                + " | Precio: $" + price
                + " | Vigente desde: " + (effectiveDate == null ? "-" : effectiveDate)
                + " | Fuente: " + (source == null ? "-" : source);
    }
}