package com.invpeajes.model;
 
import java.time.LocalDate;
import java.util.Objects;
 
/**
 * Representa a un usuario que trabaja en una estación de peaje,
 * como un jefe de estación o un operador de caseta.
 * <p>
 * Corresponde a la tabla {@code PEAJE.USUARIO_PEAJE}. La cédula es la llave
 * primaria y el correo debe ser único.
 * </p>
 */
public class TollUser {
 
    private String idNumber;
    private String firstName;
    private String middleName;
    private String firstSurname;
    private String secondSurname;
    private String phone;
    private String email;
    private String position;
    private LocalDate hireDate;
    private Integer tollId;
 
    /**
     * Crea un usuario vacío.
     * Lo usa la capa de persistencia al leer registros de la base de datos.
     */
    public TollUser() {
    }
 
    /**
     * Crea un usuario con todos sus datos.
     *
     * @param idNumber      cédula del usuario
     * @param firstName     primer nombre
     * @param middleName    segundo nombre (opcional)
     * @param firstSurname  primer apellido
     * @param secondSurname segundo apellido (opcional)
     * @param phone         teléfono de contacto (opcional)
     * @param email         correo electrónico
     * @param position      cargo dentro de la estación (ej. "OPERADOR")
     * @param hireDate      fecha de ingreso (opcional)
     * @param tollId        identificador del peaje donde trabaja (opcional)
     */
    public TollUser(String idNumber, String firstName, String middleName, String firstSurname,
                    String secondSurname, String phone, String email, String position,
                    LocalDate hireDate, Integer tollId) {
        this.idNumber = idNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.firstSurname = firstSurname;
        this.secondSurname = secondSurname;
        this.phone = phone;
        this.email = email;
        this.position = position;
        this.hireDate = hireDate;
        this.tollId = tollId;
    }
 
    /**
     * Arma el nombre completo omitiendo los campos opcionales vacíos.
     *
     * @return nombres y apellidos separados por un espacio
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        appendIfPresent(fullName, firstName);
        appendIfPresent(fullName, middleName);
        appendIfPresent(fullName, firstSurname);
        appendIfPresent(fullName, secondSurname);
        return fullName.toString();
    }
 
    private static void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
    }
 
    /**
     * @return cédula del usuario
     */
    public String getIdNumber() {
        return idNumber;
    }
 
    /**
     * @param idNumber cédula del usuario
     */
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
 
    /**
     * @return primer nombre
     */
    public String getFirstName() {
        return firstName;
    }
 
    /**
     * @param firstName primer nombre
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
 
    /**
     * @return segundo nombre, o {@code null} si no tiene
     */
    public String getMiddleName() {
        return middleName;
    }
 
    /**
     * @param middleName segundo nombre
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
 
    /**
     * @return primer apellido
     */
    public String getFirstSurname() {
        return firstSurname;
    }
 
    /**
     * @param firstSurname primer apellido
     */
    public void setFirstSurname(String firstSurname) {
        this.firstSurname = firstSurname;
    }
 
    /**
     * @return segundo apellido, o {@code null} si no tiene
     */
    public String getSecondSurname() {
        return secondSurname;
    }
 
    /**
     * @param secondSurname segundo apellido
     */
    public void setSecondSurname(String secondSurname) {
        this.secondSurname = secondSurname;
    }
 
    /**
     * @return teléfono de contacto
     */
    public String getPhone() {
        return phone;
    }
 
    /**
     * @param phone teléfono de contacto
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }
 
    /**
     * @return correo electrónico
     */
    public String getEmail() {
        return email;
    }
 
    /**
     * @param email correo electrónico
     */
    public void setEmail(String email) {
        this.email = email;
    }
 
    /**
     * @return cargo dentro de la estación
     */
    public String getPosition() {
        return position;
    }
 
    /**
     * @param position cargo dentro de la estación
     */
    public void setPosition(String position) {
        this.position = position;
    }
 
    /**
     * @return fecha de ingreso
     */
    public LocalDate getHireDate() {
        return hireDate;
    }
 
    /**
     * @param hireDate fecha de ingreso
     */
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
 
    /**
     * @return identificador del peaje donde trabaja, o {@code null} si no está asignado
     */
    public Integer getTollId() {
        return tollId;
    }
 
    /**
     * @param tollId identificador del peaje donde trabaja
     */
    public void setTollId(Integer tollId) {
        this.tollId = tollId;
    }
 
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TollUser)) {
            return false;
        }
        TollUser user = (TollUser) other;
        return Objects.equals(idNumber, user.idNumber);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(idNumber);
    }
 
    @Override
    public String toString() {
        return "Usuario " + idNumber
                + " | " + getFullName()
                + " | Cargo: " + (position == null ? "-" : position)
                + " | Correo: " + email
                + " | Peaje: " + (tollId == null ? "-" : tollId);
    }
}
 