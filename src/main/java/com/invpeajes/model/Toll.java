
package com.invpeajes.model;

import java.util.Date;

/**
 * Clase abstracta que representa una estación de peaje genérica en la vía.
 * Sirve como plantilla base para implementar tipos de peajes específicos
 * como PeajeManual o PeajeElectronico aplicando herencia y polimorfismo.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public abstract class Toll {
    // Atributos de Identificación
    private int tollId;
    private String code;
    private Integer aniId;
    private String name;
    
    // Atributos de Ubicación Geográfica
    private String kilometer;
    private double latitude;
    private double longitude;
    private String locationDescription;
    private String direction;
    
    // Atributos Operativos
    private String operationalStatus;
    private String level;
    private String acceptsColpass;
    private Date date;
    
    // Atributos de Contacto y Referencia
    private String phone;
    private String towPhone;
    private String infoSheetUrl;

    // Llaves Foráneas (Relaciones)
    private String routeId;
    private Integer concessionaireId;
    private Integer sectorId;
    private String departmentId;

    /**
     * Constructor base masivo para inicializar todos los datos de la estación de peaje.
     * 
     * @param tollId Identificador interno de la estación.
     * @param code Código oficial de la estación.
     * @param aniId Identificador asignado por la ANI.
     * @param name Nombre comercial o geográfico.
     * @param kilometer Punto kilométrico exacto en la ruta.
     * @param latitude Coordenada de latitud.
     * @param longitude Coordenada de longitud.
     * @param locationDescription Descripción textual de la ubicación.
     * @param direction Sentido del tráfico de la estación.
     * @param operationalStatus Estado actual del peaje (Activo/Inactivo).
     * @param level Nivel operativo de la estación.
     * @param acceptsColpass Indicador de si acepta pago por Colpass (S/N).
     * @param date Fecha de registro o actualización.
     * @param phone Teléfono de atención de la estación.
     * @param towPhone Teléfono de servicio de grúa.
     * @param infoSheetUrl URL al documento de ficha técnica.
     * @param routeId ID de la ruta (Llave foránea).
     * @param concessionaireId ID del concesionario (Llave foránea).
     * @param sectorId ID del sector vial (Llave foránea).
     * @param departmentId ID del departamento (Llave foránea).
     */
    public Toll(int tollId, String code, Integer aniId, String name, 
                 String kilometer, double latitude, double longitude, String locationDescription, 
                 String direction, String operationalStatus, String level, String acceptsColpass, 
                 Date date, String phone, String towPhone, String infoSheetUrl, 
                 String routeId, Integer concessionaireId, Integer sectorId, 
                 String departmentId) {
        this.tollId = tollId;
        this.code = code;
        this.aniId = aniId;
        this.name = name;
        this.kilometer = kilometer;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationDescription = locationDescription;
        this.direction = direction;
        this.operationalStatus = operationalStatus;
        this.level = level;
        this.acceptsColpass = acceptsColpass;
        this.date = date;
        this.phone = phone;
        this.towPhone = towPhone;
        this.infoSheetUrl = infoSheetUrl;
        this.routeId = routeId;
        this.concessionaireId = concessionaireId;
        this.sectorId = sectorId;
        this.departmentId = departmentId;
    }

    /**
     * Obtiene el identificador del peaje.
     * @return El ID del peaje.
     */
    public int getTollId() {
        return tollId;
    }

    /**
     * Establece el identificador del peaje.
     * @param tollId El nuevo ID a asignar.
     */
    public void setTollId(int tollId) {
        this.tollId = tollId;
    }

    /**
     * Obtiene el nombre del peaje.
     * @return El nombre oficial de la estación.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre del peaje.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene el ID del concesionario que administra el peaje.
     * @return El ID del concesionario.
     */
    public Integer getConcessionaireId() {
        return concessionaireId;
    }

    /**
     * Establece el ID del concesionario administrador.
     * @param concessionaireId El nuevo ID de concesionario.
     */
    public void setConcessionaireId(Integer concessionaireId) {
        this.concessionaireId = concessionaireId;
    }
    
    /**
     * Obtiene la ubicación kilométrica del peaje en la vía.
     * @return El punto kilométrico.
     */
    public String getKilometer() {
        return kilometer;
    }
    
    /**
     * Establece la ubicación kilométrica del peaje.
     * @param kilometer El nuevo punto kilométrico a asignar.
     */
    public void setKilometer(String kilometer) {
        this.kilometer = kilometer;
    }

    /**
     * Obtiene la latitud geográfica del peaje.
     * @return La coordenada de latitud.
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * Establece la latitud geográfica del peaje.
     * @param latitude La nueva coordenada de latitud a asignar.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Obtiene la longitud geográfica del peaje.
     * @return La coordenada de longitud.
     */
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * Establece la longitud geográfica del peaje.
     * @param longitude La nueva coordenada de longitud a asignar.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Obtiene el ID de la ruta que administra el peaje.
     * @return El ID de la ruta.
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Establece el ID de la ruta del peaje.
     * @param routeId El nuevo ID de la ruta a asignar.
     */
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    /**
     * Obtiene el estado operativo actual del peaje.
     * @return El estado operativo (OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA).
     */
    public String getOperationalStatus() {
        return operationalStatus;
    }

    /**
     * Establece el estado operativo del peaje.
     * @param operationalStatus El nuevo estado operativo a asignar.
     */
    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    /**
     * Obtiene la fecha de registro o actualización del peaje.
     * @return La fecha correspondiente.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Establece la fecha de registro o actualización.
     * @param date La nueva fecha a asignar.
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Obtiene el código oficial de la estación de peaje.
     * @return El código del peaje.
     */
    public String getCode() {
        return code;
    }

    /**
     * Establece el código oficial de la estación de peaje.
     * @param code El nuevo código a asignar.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Obtiene el identificador asignado por la ANI.
     * @return El ID de la ANI.
     */
    public Integer getAniId() {
        return aniId;
    }

    /**
     * Establece el identificador asignado por la ANI.
     * @param aniId El nuevo ID de la ANI.
     */
    public void setAniId(Integer aniId) {
        this.aniId = aniId;
    }

    /**
     * Obtiene la descripción textual detallada de la ubicación.
     * @return La descripción de la ubicación.
     */
    public String getLocationDescription() {
        return locationDescription;
    }

    /**
     * Establece la descripción textual de la ubicación del peaje.
     * @param locationDescription La nueva descripción a asignar.
     */
    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    /**
     * Obtiene el sentido del tráfico que atiende la estación.
     * @return El sentido del tráfico.
     */
    public String getDirection() {
        return direction;
    }

    /**
     * Establece el sentido del tráfico que atiende la estación.
     * @param direction El nuevo sentido a asignar.
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * Obtiene el nivel operativo y jerárquico de la estación.
     * @return El nivel del peaje.
     */
    public String getLevel() {
        return level;
    }

    /**
     * Establece el nivel operativo de la estación.
     * @param level El nuevo nivel a asignar.
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Obtiene el indicador de si la estación acepta pago mediante TAG Colpass.
     * @return Un carácter (ej. "S" o "N") indicando si acepta Colpass.
     */
    public String getAcceptsColpass() {
        return acceptsColpass;
    }

    /**
     * Establece el indicador de aceptación de pago por Colpass.
     * @param acceptsColpass El nuevo indicador (S/N).
     */
    public void setAcceptsColpass(String acceptsColpass) {
        this.acceptsColpass = acceptsColpass;
    }

    /**
     * Obtiene el teléfono de atención de la estación.
     * @return El número de teléfono.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Establece el teléfono de atención de la estación.
     * @param phone El nuevo teléfono a asignar.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Obtiene el teléfono del servicio de grúa adscrito al peaje.
     * @return El número de teléfono de la grúa.
     */
    public String getTowPhone() {
        return towPhone;
    }

    /**
     * Establece el teléfono del servicio de grúa adscrito.
     * @param towPhone El nuevo teléfono de grúa a asignar.
     */
    public void setTowPhone(String towPhone) {
        this.towPhone = towPhone;
    }

    /**
     * Obtiene el enlace URL al documento de la ficha técnica oficial.
     * @return La URL de la ficha técnica.
     */
    public String getInfoSheetUrl() {
        return infoSheetUrl;
    }

    /**
     * Establece el enlace URL a la ficha técnica oficial del peaje.
     * @param infoSheetUrl La nueva URL a asignar.
     */
    public void setInfoSheetUrl(String infoSheetUrl) {
        this.infoSheetUrl = infoSheetUrl;
    }

    /**
     * Obtiene el ID del sector vial al que pertenece el peaje.
     * @return El ID del sector (Llave foránea).
     */
    public Integer getSectorId() {
        return sectorId;
    }

    /**
     * Establece el ID del sector vial.
     * @param sectorId El nuevo ID de sector a asignar.
     */
    public void setSectorId(Integer sectorId) {
        this.sectorId = sectorId;
    }

    /**
     * Obtiene el ID del departamento donde se ubica el peaje.
     * @return El ID del departamento (Llave foránea).
     */
    public String getDepartmentId() {
        return departmentId;
    }

    /**
     * Establece el ID del departamento.
     * @param departmentId El nuevo ID de departamento a asignar.
     */
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    /**
     * Método abstracto que aplica polimorfismo. 
     * Cada clase hija deberá sobrescribir este método para definir su propia 
     * lógica de cómo opera.
     * 
     * @return Una cadena de texto indicando el estado o tipo de operación.
     */
    public abstract String reportOperationalStatus();
}
