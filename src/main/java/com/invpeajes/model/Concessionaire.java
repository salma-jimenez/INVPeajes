
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
public class Concessionaire {
    private int concessionaireId;
    private String name;
    private Date date;
    private String nit;
    private String type;
    private String email;
    private String phone;
    private String address;
    /**
     * Constructor que inicializa un nuevo Concesionario con sus datos.
     *
     * @param concessionaireId Identificador numérico único del concesionario.
     * @param name Nombre oficial de la empresa o entidad (único en la BD).
     * @param date Fecha de registro o constitución (puede ser null).
     * @param nit Número de Identificación Tributaria (puede ser null).
     * @param type Clasificación de la entidad: INVIAS, CONCESION u OTRO.
     * @param email Correo electrónico de contacto oficial (puede ser null).
     * @param phone Número de teléfono de contacto (puede ser null).
     * @param address Dirección del concesionario (puede ser null).
     */
    public Concessionaire(int concessionaireId, String name, 
            Date date, String nit, String type, String email, 
            String phone, String address) {
        this.concessionaireId = concessionaireId;
        this.name = name;
        this.date = date;
        this.nit = nit;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Obtiene el identificador del concesionario.
     * @return El ID del concesionario.
     */
    public int getConcessionaireId() {
        return concessionaireId;
    }

    /**
     * Establece o modifica el identificador del concesionario.
     * @param concessionaireId El nuevo ID a asignar.
     */
    public void setConcessionaireId(int concessionaireId) {
        this.concessionaireId = concessionaireId;
    }

    /**
     * Obtiene el nombre del concesionario.
     * @return El nombre oficial del concesionario.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece o modifica el nombre del concesionario.
     * @param name El nuevo nombre a asignar.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene la fecha de registro o constitución del concesionario.
     * @return La fecha asociada a la entidad.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Establece o modifica la fecha de registro o constitución del concesionario.
     * @param date La nueva fecha a asignar.
     */
    public void setDate(Date date) {
        this.date = date;
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
    public String getType() {
        return type;
    }

    /**
     * Establece o modifica la clasificación de la entidad administradora.
     * @param type INVIAS, CONCESION u OTRO.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Obtiene el correo electrónico oficial de contacto del concesionario.
     * @return El correo electrónico de la entidad.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece o modifica el correo electrónico oficial de contacto del concesionario.
     * @param email El nuevo correo electrónico a asignar.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene el número de teléfono de contacto o atención del concesionario.
     * @return El número de teléfono de la entidad.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Establece o modifica el número de teléfono de contacto o atención del concesionario.
     * @param phone El nuevo número de teléfono a asignar.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Obtiene la dirección del concesionario.
     * @return La dirección de la entidad.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Establece o modifica la dirección del concesionario.
     * @param address La nueva dirección a asignar.
     */
    public void setAddress(String address) {
        this.address = address;
    }
}