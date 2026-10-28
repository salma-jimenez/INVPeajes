
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
public abstract class Peaje {
    // Atributos de Identificación
    private int idPeaje;
    private String codigoPeaje;
    private Integer idAni;
    private String nombrePeaje;
    
    // Atributos de Ubicación Geográfica
    private String kilometro;
    private double latitud;
    private double longitud;
    private String ubicacionDescripcion;
    private String sentido;
    
    // Atributos Operativos
    private String estadoOperativo;
    private String nivel;
    private String aceptaColpass;
    private Date fecha;
    
    // Atributos de Contacto y Referencia
    private String telefono;
    private String telefonoGrua;
    private String urlFicha;

    // Llaves Foráneas (Relaciones)
    private String idRuta;
    private Integer idConcesionario;
    private Integer idSector;
    private String idDepartamento;

    /**
     * Constructor base masivo para inicializar todos los datos de la estación de peaje.
     * 
     * @param idPeaje Identificador interno de la estación.
     * @param codigoPeaje Código oficial de la estación.
     * @param idAni Identificador asignado por la ANI.
     * @param nombrePeaje Nombre comercial o geográfico.
     * @param kilometro Punto kilométrico exacto en la ruta.
     * @param latitud Coordenada de latitud.
     * @param longitud Coordenada de longitud.
     * @param ubicacionDescripcion Descripción textual de la ubicación.
     * @param sentido Sentido del tráfico de la estación.
     * @param estadoOperativo Estado actual del peaje (Activo/Inactivo).
     * @param nivel Nivel operativo de la estación.
     * @param aceptaColpass Indicador de si acepta pago por Colpass (S/N).
     * @param fecha Fecha de registro o actualización.
     * @param telefono Teléfono de atención de la estación.
     * @param telefonoGrua Teléfono de servicio de grúa.
     * @param urlFicha URL al documento de ficha técnica.
     * @param idRuta ID de la ruta (Llave foránea).
     * @param idConcesionario ID del concesionario (Llave foránea).
     * @param idSector ID del sector vial (Llave foránea).
     * @param idDepartamento ID del departamento (Llave foránea).
     */
    public Peaje(int idPeaje, String codigoPeaje, Integer idAni, String nombrePeaje, 
                 String kilometro, double latitud, double longitud, String ubicacionDescripcion, 
                 String sentido, String estadoOperativo, String nivel, String aceptaColpass, 
                 Date fecha, String telefono, String telefonoGrua, String urlFicha, 
                 String idRuta, Integer idConcesionario, Integer idSector, 
                 String idDepartamento) {
        this.idPeaje = idPeaje;
        this.codigoPeaje = codigoPeaje;
        this.idAni = idAni;
        this.nombrePeaje = nombrePeaje;
        this.kilometro = kilometro;
        this.latitud = latitud;
        this.longitud = longitud;
        this.ubicacionDescripcion = ubicacionDescripcion;
        this.sentido = sentido;
        this.estadoOperativo = estadoOperativo;
        this.nivel = nivel;
        this.aceptaColpass = aceptaColpass;
        this.fecha = fecha;
        this.telefono = telefono;
        this.telefonoGrua = telefonoGrua;
        this.urlFicha = urlFicha;
        this.idRuta = idRuta;
        this.idConcesionario = idConcesionario;
        this.idSector = idSector;
        this.idDepartamento = idDepartamento;
    }

    /**
     * Obtiene el identificador del peaje.
     * @return El ID del peaje.
     */
    public int getIdPeaje() {
        return idPeaje;
    }

    /**
     * Establece el identificador del peaje.
     * @param idPeaje El nuevo ID a asignar.
     */
    public void setIdPeaje(int idPeaje) {
        this.idPeaje = idPeaje;
    }

    /**
     * Obtiene el nombre del peaje.
     * @return El nombre oficial de la estación.
     */
    public String getNombrePeaje() {
        return nombrePeaje;
    }

    /**
     * Establece el nombre del peaje.
     * @param nombrePeaje El nuevo nombre a asignar.
     */
    public void setNombrePeaje(String nombrePeaje) {
        this.nombrePeaje = nombrePeaje;
    }

    /**
     * Obtiene el ID del concesionario que administra el peaje.
     * @return El ID del concesionario.
     */
    public Integer getIdConcesionario() {
        return idConcesionario;
    }

    /**
     * Establece el ID del concesionario administrador.
     * @param idConcesionario El nuevo ID de concesionario.
     */
    public void setIdConcesionario(Integer idConcesionario) {
        this.idConcesionario = idConcesionario;
    }
    
    /**
     * Obtiene la ubicación kilométrica del peaje en la vía.
     * @return El punto kilométrico.
     */
    public String getKilometro() {
        return kilometro;
    }
    
    /**
     * Establece la ubicación kilométrica del peaje.
     * @param kilometro El nuevo punto kilométrico a asignar.
     */
    public void setKilometro(String kilometro) {
        this.kilometro = kilometro;
    }

    /**
     * Obtiene la latitud geográfica del peaje.
     * @return La coordenada de latitud.
     */
    public double getLatitud() {
        return latitud;
    }
    
    /**
     * Establece la latitud geográfica del peaje.
     * @param latitud La nueva coordenada de latitud a asignar.
     */
    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    /**
     * Obtiene la longitud geográfica del peaje.
     * @return La coordenada de longitud.
     */
    public double getLongitud() {
        return longitud;
    }
    
    /**
     * Establece la longitud geográfica del peaje.
     * @param longitud La nueva coordenada de longitud a asignar.
     */
    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    /**
     * Obtiene el ID de la ruta que administra el peaje.
     * @return El ID de la ruta.
     */
    public String getIdRuta() {
        return idRuta;
    }

    /**
     * Establece el ID de la ruta del peaje.
     * @param idRuta El nuevo ID de la ruta a asignar.
     */
    public void setIdRuta(String idRuta) {
        this.idRuta = idRuta;
    }

    /**
     * Obtiene el estado operativo actual del peaje.
     * @return El estado operativo (OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA).
     */
    public String getEstadoOperativo() {
        return estadoOperativo;
    }

    /**
     * Establece el estado operativo del peaje.
     * @param estadoOperativo El nuevo estado operativo a asignar.
     */
    public void setEstadoOperativo(String estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
    }

    /**
     * Obtiene la fecha de registro o actualización del peaje.
     * @return La fecha correspondiente.
     */
    public Date getFecha() {
        return fecha;
    }

    /**
     * Establece la fecha de registro o actualización.
     * @param fecha La nueva fecha a asignar.
     */
    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    /**
     * Obtiene el código oficial de la estación de peaje.
     * @return El código del peaje.
     */
    public String getCodigoPeaje() {
        return codigoPeaje;
    }

    /**
     * Establece el código oficial de la estación de peaje.
     * @param codigoPeaje El nuevo código a asignar.
     */
    public void setCodigoPeaje(String codigoPeaje) {
        this.codigoPeaje = codigoPeaje;
    }

    /**
     * Obtiene el identificador asignado por la ANI.
     * @return El ID de la ANI.
     */
    public Integer getIdAni() {
        return idAni;
    }

    /**
     * Establece el identificador asignado por la ANI.
     * @param idAni El nuevo ID de la ANI.
     */
    public void setIdAni(Integer idAni) {
        this.idAni = idAni;
    }

    /**
     * Obtiene la descripción textual detallada de la ubicación.
     * @return La descripción de la ubicación.
     */
    public String getUbicacionDescripcion() {
        return ubicacionDescripcion;
    }

    /**
     * Establece la descripción textual de la ubicación del peaje.
     * @param ubicacionDescripcion La nueva descripción a asignar.
     */
    public void setUbicacionDescripcion(String ubicacionDescripcion) {
        this.ubicacionDescripcion = ubicacionDescripcion;
    }

    /**
     * Obtiene el sentido del tráfico que atiende la estación.
     * @return El sentido del tráfico.
     */
    public String getSentido() {
        return sentido;
    }

    /**
     * Establece el sentido del tráfico que atiende la estación.
     * @param sentido El nuevo sentido a asignar.
     */
    public void setSentido(String sentido) {
        this.sentido = sentido;
    }

    /**
     * Obtiene el nivel operativo y jerárquico de la estación.
     * @return El nivel del peaje.
     */
    public String getNivel() {
        return nivel;
    }

    /**
     * Establece el nivel operativo de la estación.
     * @param nivel El nuevo nivel a asignar.
     */
    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    /**
     * Obtiene el indicador de si la estación acepta pago mediante TAG Colpass.
     * @return Un carácter (ej. "S" o "N") indicando si acepta Colpass.
     */
    public String getAceptaColpass() {
        return aceptaColpass;
    }

    /**
     * Establece el indicador de aceptación de pago por Colpass.
     * @param aceptaColpass El nuevo indicador (S/N).
     */
    public void setAceptaColpass(String aceptaColpass) {
        this.aceptaColpass = aceptaColpass;
    }

    /**
     * Obtiene el teléfono de atención de la estación.
     * @return El número de teléfono.
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * Establece el teléfono de atención de la estación.
     * @param telefono El nuevo teléfono a asignar.
     */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /**
     * Obtiene el teléfono del servicio de grúa adscrito al peaje.
     * @return El número de teléfono de la grúa.
     */
    public String getTelefonoGrua() {
        return telefonoGrua;
    }

    /**
     * Establece el teléfono del servicio de grúa adscrito.
     * @param telefonoGrua El nuevo teléfono de grúa a asignar.
     */
    public void setTelefonoGrua(String telefonoGrua) {
        this.telefonoGrua = telefonoGrua;
    }

    /**
     * Obtiene el enlace URL al documento de la ficha técnica oficial.
     * @return La URL de la ficha técnica.
     */
    public String getUrlFicha() {
        return urlFicha;
    }

    /**
     * Establece el enlace URL a la ficha técnica oficial del peaje.
     * @param urlFicha La nueva URL a asignar.
     */
    public void setUrlFicha(String urlFicha) {
        this.urlFicha = urlFicha;
    }

    /**
     * Obtiene el ID del sector vial al que pertenece el peaje.
     * @return El ID del sector (Llave foránea).
     */
    public Integer getIdSector() {
        return idSector;
    }

    /**
     * Establece el ID del sector vial.
     * @param idSector El nuevo ID de sector a asignar.
     */
    public void setIdSector(Integer idSector) {
        this.idSector = idSector;
    }

    /**
     * Obtiene el ID del departamento donde se ubica el peaje.
     * @return El ID del departamento (Llave foránea).
     */
    public String getIdDepartamento() {
        return idDepartamento;
    }

    /**
     * Establece el ID del departamento.
     * @param idDepartamento El nuevo ID de departamento a asignar.
     */
    public void setIdDepartamento(String idDepartamento) {
        this.idDepartamento = idDepartamento;
    }
    
    /**
     * Método abstracto que aplica polimorfismo. 
     * Cada clase hija deberá sobrescribir este método para definir su propia 
     * lógica de cómo opera.
     * 
     * @return Una cadena de texto indicando el estado o tipo de operación.
     */
    public abstract String reportarEstadoOperativo();
}
