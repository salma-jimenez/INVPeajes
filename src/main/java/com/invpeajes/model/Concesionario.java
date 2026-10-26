
package com.invpeajes.model;

import java.util.Date;

/**
 * Representa a la entidad o empresa administradora de las estaciones de peaje (Concesionario).
 * Ejemplos de esta entidad pueden ser INVIAS, ANI o concesiones privadas.
 * Es una clase base de la cual dependen las estaciones de peaje.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Concesionario {
    private String idConcesionario;
    private String nombreConcesionario;
    private Date fecha;
    private String nit;
    private String tipoConcesionario;
    private String correo;
    private String telefono;
    private String direccion;

    /**
     * Constructor que inicializa un nuevo Concesionario con sus datos obligatorios.
     * 
     * @param idConcesionario Identificador alfanumérico único (NIT o código interno).
     * @param nombreConcesionario Nombre oficial de la empresa o entidad administradora.
     * @param fecha Fecha de registro o constitución.
     * @param nit Número de Identificación Tributaria del concesionario.
     * @param tipoConcesionario Clasificación de la entidad (ej. Pública, Privada).
     * @param correo Correo electrónico de contacto oficial.
     * @param telefono Número de teléfono de contacto.
     * @param direccion direccion del concesionario.
     */
    public Concesionario(String idConcesionario, String nombreConcesionario, 
            Date fecha, String nit, String tipoConcesionario, String correo, 
            String telefono, String direccion) {
        this.idConcesionario = idConcesionario;
        this.nombreConcesionario = nombreConcesionario;
        this.fecha = fecha;
        this.nit = nit;
        this.tipoConcesionario = tipoConcesionario;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    /**
     * Obtiene el identificador del concesionario.
     * @return El ID o NIT del concesionario.
     */
    public String getIdConcesionario() {
        return idConcesionario;
    }

    /**
     * Establece o modifica el identificador del concesionario.
     * @param idConcesionario El nuevo ID a asignar.
     */
    public void setIdConcesionario(String idConcesionario) {
        this.idConcesionario = idConcesionario;
    }

    /**
     * Obtiene el nombre del concesionario.
     * @return El nombre oficial del concesionario.
     */
    public String getNombreConcesionario() {
        return nombreConcesionario;
    }

    /**
     * Establece o modifica el nombre del concesionario.
     * @param nombreConcesionario El nuevo nombre a asignar.
     */
    public void setNombreConcesionario(String nombreConcesionario) {
        this.nombreConcesionario = nombreConcesionario;
    }

    /**
     * Obtiene la fecha de registro o constitución del concesionario.
     * @return La fecha asociada a la entidad.
     */
    public Date getFecha() {
        return fecha;
    }

    /**
     * Establece o modifica la fecha de registro o constitución del concesionario.
     * @param fecha La nueva fecha a asignar.
     */
    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    /**
     * Obtiene el Número de Identificación Tributaria (NIT) del concesionario.
     * @return El NIT de la entidad administradora.
     */
    public String getNit() {
        return nit;
    }

    /**
     * Establece o modifica el Número de Identificación Tributaria (NIT) del concesionario.
     * @param nit El nuevo NIT a asignar.
     */
    public void setNit(String nit) {
        this.nit = nit;
    }

    /**
     * Obtiene la clasificación o tipo de la entidad administradora.
     * @return El tipo de concesionario (ej. Pública, Privada).
     */
    public String getTipoConcesionario() {
        return tipoConcesionario;
    }

    /**
     * Establece o modifica la clasificación o tipo de la entidad administradora.
     * @param tipoConcesionario El nuevo tipo de concesionario a asignar.
     */
    public void setTipoConcesionario(String tipoConcesionario) {
        this.tipoConcesionario = tipoConcesionario;
    }

    /**
     * Obtiene el correo electrónico oficial de contacto del concesionario.
     * @return El correo electrónico de la entidad.
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * Establece o modifica el correo electrónico oficial de contacto del concesionario.
     * @param correo El nuevo correo electrónico a asignar.
     */
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    /**
     * Obtiene el número de teléfono de contacto o atención del concesionario.
     * @return El número de teléfono de la entidad.
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * Establece o modifica el número de teléfono de contacto o atención del concesionario.
     * @param telefono El nuevo número de teléfono a asignar.
     */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /**
     * Obtiene el número de teléfono de contacto o atención del concesionario.
     * @return La direccion de la entidad.
     */
    public String getDireccion() {
        return direccion;
    }

    /**
     * Establece o modifica la direccion actual del concesionario.
     * @param direccion La nueva direccion a asignar.
     */
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}