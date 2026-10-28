
package com.invpeajes.model;

/**
 * Vehículo registrado en el sistema de peajes.
 * @author Salomejimenez
 */
public class Vehicle {
   

    private String plate;
    private String type;
    private String brand;
    private String tagCode;
    private Integer intermediaryId;
    private int categoryId;

    /** Constructor vacío requerido para mapear filas desde la base de datos. */
    public Vehicle() {
    }

    /**
     * Crea un vehículo con todos sus datos.
     *
     * @param plate          placa del vehículo
     * @param type           tipo de vehículo (automóvil, camión, moto...)
     * @param brand          marca del vehículo
     * @param tagCode        código del tag COLPASS, o null si paga en efectivo/tarjeta
     * @param intermediaryId intermediador que emitió el tag, o null si no tiene tag
     * @param categoryId     categoría vehicular a la que pertenece
     */
    public Vehicle(String plate, String type, String brand, String tagCode,
                   Integer intermediaryId, int categoryId) {
        this.plate = plate;
        this.type = type;
        this.brand = brand;
        this.tagCode = tagCode;
        this.intermediaryId = intermediaryId;
        this.categoryId = categoryId;
    }

    /**
     * Indica si el vehículo tiene un tag COLPASS registrado.
     *
     * @return true si tiene código de tag
     */
    public boolean hasTag() {
        return tagCode != null && !tagCode.isEmpty();
    }

    /**
     * Describe cómo puede pagar el vehículo en el peaje, para mostrar en la UI.
     *
     * @return "COLPASS" si tiene tag, o "EFECTIVO / TARJETA" si no
     */
    public String getPaymentOption() {
        return hasTag() ? "COLPASS" : "EFECTIVO / TARJETA";
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public Integer getIntermediaryId() {
        return intermediaryId;
    }

    public void setIntermediaryId(Integer intermediaryId) {
        this.intermediaryId = intermediaryId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return String.format("%s | %s %s | Cat. %d | Pago: %s%s", plate,
                type == null ? "-" : type, brand == null ? "-" : brand,
                categoryId, getPaymentOption(), hasTag() ? " (tag " + tagCode + ")" : "");
    }
}

