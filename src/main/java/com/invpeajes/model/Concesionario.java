
package com.invpeajes.model;

import java.util.Date;

/**
 * Representa a la entidad o empresa administradora de las estaciones de peaje (Concesionario).
 * Ejemplos de esta entidad pueden ser INVIAS, ANI o concesiones privadas.
 * Es una clase base de la cual dependen las estaciones de peaje.
 * 
 * @author Luis Guerrero
 * @version 1.1
 */
public class Concesionario {
    private int idConcesionario;
    private String nombreConcesionario;
    private Date fecha;
    private String nit;
    private String tipoConcesionario;
    private String correo;
    private String telefono;
    private String direccion;
    /**
     * Constructor que inicializa un nuevo Concesionario con sus datos.
     *
     * @param idConcesionario Identificador numérico único del concesionario.
     * @param nombreConcesionario Nombre oficial de la empresa o entidad (único en la BD).
     * @param fecha Fecha de registro o constitución (puede ser null).
     * @param nit Número de Identificación Tributaria (puede ser null).
     * @param tipoConcesionario Clasificación de la entidad: INVIAS, CONCESION u OTRO.
     * @param correo Correo electrónico de contacto oficial (puede ser null).
     * @param telefono Número de teléfono de contacto (puede ser null).
     * @param direccion Dirección del concesionario (puede ser null).
     */
    public Concesionario(int idConcesionario, String nombreConcesionario, 
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
     * @return El ID del concesionario.
     */
    public int getIdConcesionario() {
        return idConcesionario;
    }

    /**
     * Establece o modifica el identificador del concesionario.
     * @param idConcesionario El nuevo ID a asignar.
     */
    public void setIdConcesionario(int idConcesionario) {
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
     * Obtiene la clasificación de la entidad administradora.
     * @return INVIAS, CONCESION u OTRO.
     */
    public String getTipoConcesionario() {
        return tipoConcesionario;
    }

    /**
     * Establece o modifica la clasificación de la entidad administradora.
     * @param tipoConcesionario INVIAS, CONCESION u OTRO.
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
     * Obtiene la dirección del concesionario.
     * @return La dirección de la entidad.
     */
    public String getDireccion() {
        return direccion;
    }

    /**
     * Establece o modifica la dirección del concesionario.
     * @param direccion La nueva dirección a asignar.
     */
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}