package com.invpeajes.model;

import java.time.LocalDate;

/**
 * Viajero registrado en RutaPeaje (tabla VIAJERO).
 * El viajero que entra como invitado no tiene registro en esta tabla.
 *
 * La contraseña nunca se guarda en texto: solo su hash SHA-256, que calcula
 * {@code AuthService}.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class Traveler {

    private Integer travelerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String plate;
    private String passwordHash;
    private LocalDate registeredAt;

    /** Constructor vacío. */
    public Traveler() {
    }

    /**
     * Crea un viajero.
     * @param travelerId ID del viajero (null si todavía no está guardado).
     * @param firstName Nombre.
     * @param lastName Apellido.
     * @param email Correo, con el que inicia sesión.
     * @param phone Teléfono de contacto.
     * @param plate Placa de su vehículo (opcional).
     * @param passwordHash Hash de la contraseña.
     * @param registeredAt Fecha de registro.
     */
    public Traveler(Integer travelerId, String firstName, String lastName, String email, String phone,
                    String plate, String passwordHash, LocalDate registeredAt) {
        this.travelerId = travelerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.plate = plate;
        this.passwordHash = passwordHash;
        this.registeredAt = registeredAt;
    }

    /** @return Nombre y apellido. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** @return ID del viajero. */
    public Integer getTravelerId() {
        return travelerId;
    }

    /** @param travelerId ID del viajero. */
    public void setTravelerId(Integer travelerId) {
        this.travelerId = travelerId;
    }

    /** @return Nombre. */
    public String getFirstName() {
        return firstName;
    }

    /** @param firstName Nombre. */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** @return Apellido. */
    public String getLastName() {
        return lastName;
    }

    /** @param lastName Apellido. */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /** @return Correo. */
    public String getEmail() {
        return email;
    }

    /** @param email Correo. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return Teléfono. */
    public String getPhone() {
        return phone;
    }

    /** @param phone Teléfono. */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** @return Placa del vehículo, o null. */
    public String getPlate() {
        return plate;
    }

    /** @param plate Placa del vehículo. */
    public void setPlate(String plate) {
        this.plate = plate;
    }

    /** @return Hash de la contraseña. */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @param passwordHash Hash de la contraseña. */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** @return Fecha de registro. */
    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    /** @param registeredAt Fecha de registro. */
    public void setRegisteredAt(LocalDate registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "Traveler{" + "travelerId=" + travelerId + ", name=" + getFullName() + ", email=" + email + '}';
    }
}
