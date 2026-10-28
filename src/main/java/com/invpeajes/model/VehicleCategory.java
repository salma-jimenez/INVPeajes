package com.invpeajes.model;

/**
 *
 * @author Salomejimenez
 */
public class VehicleCategory {

    private int categoryId;
    private String categoryCode;
    private String description;
    private Integer axleCount;

    /**
     * Constructor vacío requerido para mapear filas desde la base de datos.
     */
    public VehicleCategory() {
    }

    /**
     * Crea una categoría con todos sus datos.
     *
     * @param categoryId identificador de la categoría 
     * @param categoryCode código corto de la categoría, ej. "I", "II"
     * (CATEGORIA)
     * @param description descripción legible de la categoría
     * @param axleCount número de ejes que define la categoría
     */
    public VehicleCategory(int categoryId, String categoryCode, String description, Integer axleCount) {
        this.categoryId = categoryId;
        this.categoryCode = categoryCode;
        this.description = description;
        this.axleCount = axleCount;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAxleCount() {
        return axleCount;
    }

    public void setAxleCount(Integer axleCount) {
        this.axleCount = axleCount;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - %s (%s ejes)", categoryId, categoryCode,
                description == null ? "" : description,
                axleCount == null ? "?" : axleCount.toString());
    }
}
