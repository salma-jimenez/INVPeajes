package com.invpeajes.model;

/**
 * Sede pública de salud de Colombia (hospital, centro de salud, puesto de salud u otra
 * unidad) tomada del REPS del Ministerio de Salud (tabla CENTRO_SALUD).
 * Se muestra en el mapa para ubicar la atención médica cercana a los peajes.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class HealthCenter {

    /** La coordenada es la ubicación real de la sede. */
    public static final String PRECISION_EXACT = "EXACTA";
    /** La coordenada es el centro del municipio (la sede no se pudo ubicar con exactitud). */
    public static final String PRECISION_MUNICIPALITY = "APROX_MUNICIPIO";

    private Integer healthCenterId;
    private String habilitationCode;
    private String name;
    private String provider;
    private String type;
    private String ese;
    private String department;
    private String municipality;
    private String divipolaCode;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private String precision;

    /**
     * Crea un centro de salud con todos sus datos.
     *
     * @param healthCenterId   identificador interno (null si aún no se ha guardado)
     * @param habilitationCode código de habilitación de la sede en el REPS
     * @param name             nombre de la sede
     * @param provider         entidad prestadora (ESE, alcaldía, etc.)
     * @param type             Hospital, Centro de salud, Puesto de salud u Otro / Unidad de salud
     * @param ese              "SI" si el prestador es una Empresa Social del Estado, "NO" si no
     * @param department       nombre del departamento
     * @param municipality     nombre del municipio
     * @param divipolaCode     código DANE del municipio (5 caracteres)
     * @param address          dirección de la sede
     * @param phone            teléfono de la sede
     * @param latitude         latitud en grados decimales
     * @param longitude        longitud en grados decimales
     * @param precision        {@link #PRECISION_EXACT} o {@link #PRECISION_MUNICIPALITY}
     */
    public HealthCenter(Integer healthCenterId, String habilitationCode, String name, String provider,
                        String type, String ese, String department, String municipality,
                        String divipolaCode, String address, String phone,
                        double latitude, double longitude, String precision) {
        this.healthCenterId = healthCenterId;
        this.habilitationCode = habilitationCode;
        this.name = name;
        this.provider = provider;
        this.type = type;
        this.ese = ese;
        this.department = department;
        this.municipality = municipality;
        this.divipolaCode = divipolaCode;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.precision = precision;
    }

    /**
     * @return true si la coordenada es la ubicación real; false si es el centro del municipio
     */
    public boolean hasExactLocation() {
        return PRECISION_EXACT.equals(precision);
    }

    /** @return identificador interno */
    public Integer getHealthCenterId() {
        return healthCenterId;
    }

    /** @param healthCenterId identificador interno */
    public void setHealthCenterId(Integer healthCenterId) {
        this.healthCenterId = healthCenterId;
    }

    /** @return código de habilitación de la sede */
    public String getHabilitationCode() {
        return habilitationCode;
    }

    /** @param habilitationCode código de habilitación de la sede */
    public void setHabilitationCode(String habilitationCode) {
        this.habilitationCode = habilitationCode;
    }

    /** @return nombre de la sede */
    public String getName() {
        return name;
    }

    /** @param name nombre de la sede */
    public void setName(String name) {
        this.name = name;
    }

    /** @return entidad prestadora */
    public String getProvider() {
        return provider;
    }

    /** @param provider entidad prestadora */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /** @return tipo de sede */
    public String getType() {
        return type;
    }

    /** @param type tipo de sede */
    public void setType(String type) {
        this.type = type;
    }

    /** @return "SI" o "NO" */
    public String getEse() {
        return ese;
    }

    /** @param ese "SI" o "NO" */
    public void setEse(String ese) {
        this.ese = ese;
    }

    /** @return nombre del departamento */
    public String getDepartment() {
        return department;
    }

    /** @param department nombre del departamento */
    public void setDepartment(String department) {
        this.department = department;
    }

    /** @return nombre del municipio */
    public String getMunicipality() {
        return municipality;
    }

    /** @param municipality nombre del municipio */
    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    /** @return código DANE del municipio */
    public String getDivipolaCode() {
        return divipolaCode;
    }

    /** @param divipolaCode código DANE del municipio */
    public void setDivipolaCode(String divipolaCode) {
        this.divipolaCode = divipolaCode;
    }

    /** @return dirección de la sede */
    public String getAddress() {
        return address;
    }

    /** @param address dirección de la sede */
    public void setAddress(String address) {
        this.address = address;
    }

    /** @return teléfono de la sede */
    public String getPhone() {
        return phone;
    }

    /** @param phone teléfono de la sede */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** @return latitud en grados decimales */
    public double getLatitude() {
        return latitude;
    }

    /** @param latitude latitud en grados decimales */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /** @return longitud en grados decimales */
    public double getLongitude() {
        return longitude;
    }

    /** @param longitude longitud en grados decimales */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /** @return precisión de la coordenada */
    public String getPrecision() {
        return precision;
    }

    /** @param precision precisión de la coordenada */
    public void setPrecision(String precision) {
        this.precision = precision;
    }

    @Override
    public String toString() {
        return name + " (" + municipality + ", " + department + ")";
    }
}
