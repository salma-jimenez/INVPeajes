package com.invpeajes.model;

/**
 * Representa la entidad geográfica de un Municipio en el sistema de peajes.
 * Esta clase almacena la información básica del municipio y sus coordenadas
 * geográficas para el cálculo de rutas y ubicación de estaciones de peaje.
 *
 * @author Luis Guerrero
 * @version 1.1
 */
public class Municipality {
    private String municipalityId;
    private String name;
    private String departmentId;
    private double latitude;
    private double longitude;

    /**
     * Constructor básico sin coordenadas explícitas.
     *
     * @param municipalityId Identificador único del municipio (Llave primaria).
     * @param name           Nombre oficial del municipio.
     * @param departmentId   Identificador del departamento al que pertenece (Llave foránea).
     */
    public Municipality(String municipalityId, String name, String departmentId) {
        this(municipalityId, name, departmentId, 0.0, 0.0);
    }

    /**
     * Constructor que inicializa un nuevo Municipio con todos sus datos y coordenadas.
     *
     * @param municipalityId Identificador único del municipio (Llave primaria).
     * @param name           Nombre oficial del municipio.
     * @param departmentId   Identificador del departamento al que pertenece (Llave foránea).
     * @param latitude       Latitud geográfica en grados decimales.
     * @param longitude      Longitud geográfica en grados decimales.
     */
    public Municipality(String municipalityId, String name, String departmentId,
                        double latitude, double longitude) {
        this.municipalityId = municipalityId;
        this.name = name;
        this.departmentId = departmentId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getMunicipalityId() {
        return municipalityId;
    }

    public void setMunicipalityId(String municipalityId) {
        this.municipalityId = municipalityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}