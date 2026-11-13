package com.invpeajes.service;

import com.invpeajes.model.TollUser;
import com.invpeajes.persistence.DataAccessException;
import com.invpeajes.persistence.TollUserDao;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Reglas para los empleados de los peajes (tabla USUARIO_PEAJE): registrar, editar, activar o
 * desactivar, restablecer la contraseña y que cada quien cambie la suya.
 *
 * <ul>
 *   <li>Al registrar a alguien, su contraseña inicial es su cédula.</li>
 *   <li>"Restablecer" vuelve a poner la cédula como contraseña.</li>
 *   <li>Nadie se borra: se desactiva, para conservar sus tiquetes y turnos.</li>
 * </ul>
 *
 * Quién puede editar a quién lo deciden {@link AdminService} (jefes de estación) y el panel del
 * jefe (operadores de su peaje).
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StaffService {

    private static final int MAX_NAME = 30;
    private static final int MAX_EMAIL = 60;

    private final TollUserDao userDAO;

    /** Crea el servicio con la base de datos. */
    public StaffService() {
        this(new TollUserDao());
    }

    /**
     * @param userDAO Acceso a USUARIO_PEAJE.
     */
    public StaffService(TollUserDao userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * @param idNumber Cédula.
     * @return El usuario.
     * @throws NoSuchElementException Si no existe.
     */
    public TollUser find(String idNumber) {
        return userDAO.findById(idNumber)
                .orElseThrow(() -> new NoSuchElementException("No existe un usuario con la cédula " + idNumber));
    }

    /**
     * @param tollId ID del peaje.
     * @return Operadores de caseta del peaje (activos primero).
     */
    public List<TollUser> operators(int tollId) {
        return userDAO.findByToll(tollId, AuthService.ROLE_OPERATOR);
    }

    /**
     * @param tollId ID del peaje.
     * @return Jefes de estación del peaje (activos primero).
     */
    public List<TollUser> managers(int tollId) {
        return userDAO.findByToll(tollId, AuthService.ROLE_MANAGER);
    }

    /**
     * @param level Entidad (PEAJE.NIVEL).
     * @return Jefes y operadores de todos los peajes de la entidad (jefes primero).
     */
    public List<TollUser> staffOfLevel(String level) {
        return userDAO.findByLevel(level);
    }

    /**
     * Registra un empleado nuevo con su cédula como contraseña inicial.
     * @param user Datos del empleado (cédula, nombres, apellidos, teléfono, correo, fecha de ingreso).
     * @param role Cargo (AuthService.ROLE_OPERATOR o ROLE_MANAGER).
     * @param tollId Peaje donde trabaja.
     * @return El empleado registrado.
     * @throws ValidationException Si algún dato no es válido o la cédula o el correo ya existen.
     */
    public TollUser register(TollUser user, String role, int tollId) {
        clean(user);
        if (!AuthService.ROLE_OPERATOR.equals(role) && !AuthService.ROLE_MANAGER.equals(role)) {
            throw new ValidationException("Cargo no válido: " + role);
        }
        validate(user, true);
        if (userDAO.findById(user.getIdNumber()).isPresent()) {
            throw new ValidationException("Ya existe un usuario con la cédula " + user.getIdNumber());
        }
        user.setPosition(role);
        user.setTollId(tollId);
        user.setEntity(null);
        user.setActive(true);
        if (user.getHireDate() == null) {
            user.setHireDate(LocalDate.now());
        }
        try {
            userDAO.insert(user, AuthService.hash(user.getIdNumber(), user.getIdNumber().toCharArray()));
        } catch (DataAccessException e) {
            throw friendly(e);
        }
        user.setDefaultPassword(true);
        return user;
    }

    /**
     * Guarda los nombres, el teléfono, el correo, la fecha de ingreso y el peaje. La cédula, el
     * cargo y la entidad no cambian.
     * @param user Usuario con los datos nuevos.
     * @throws ValidationException Si algún dato no es válido o el correo ya lo usa otra persona.
     */
    public void update(TollUser user) {
        clean(user);
        validate(user, false);
        try {
            if (!userDAO.update(user)) {
                throw new NoSuchElementException("No existe un usuario con la cédula " + user.getIdNumber());
            }
        } catch (DataAccessException e) {
            throw friendly(e);
        }
    }

    /**
     * Activa o desactiva a un usuario.
     * @param idNumber Cédula.
     * @param active true para activar.
     */
    public void setActive(String idNumber, boolean active) {
        if (!userDAO.setActive(idNumber, active)) {
            throw new NoSuchElementException("No existe un usuario con la cédula " + idNumber);
        }
    }

    /**
     * Vuelve a poner la cédula como contraseña.
     * @param idNumber Cédula.
     */
    public void resetPassword(String idNumber) {
        if (!userDAO.updatePassword(idNumber, AuthService.hash(idNumber, idNumber.toCharArray()))) {
            throw new NoSuchElementException("No existe un usuario con la cédula " + idNumber);
        }
    }

    /**
     * Cambia la contraseña propia, pidiendo la actual.
     * @param idNumber Cédula de quien cambia su contraseña.
     * @param current Contraseña actual.
     * @param password Contraseña nueva.
     * @param confirm Confirmación de la nueva.
     * @throws ValidationException Si la actual no coincide, la nueva es muy corta o no coinciden.
     */
    public void changePassword(String idNumber, char[] current, char[] password, char[] confirm) {
        try {
            String saved = userDAO.findPasswordHash(idNumber).orElse("");
            if (current == null || !saved.equalsIgnoreCase(AuthService.hash(idNumber, current))) {
                throw new ValidationException("La contraseña actual no es correcta");
            }
            if (password == null || password.length < AuthService.MIN_PASSWORD_LENGTH) {
                throw new ValidationException("La contraseña nueva debe tener al menos "
                        + AuthService.MIN_PASSWORD_LENGTH + " caracteres");
            }
            if (!Arrays.equals(password, confirm)) {
                throw new ValidationException("Las contraseñas nuevas no coinciden");
            }
            if (new String(password).equals(idNumber)) {
                throw new ValidationException("La contraseña nueva no puede ser tu cédula");
            }
            userDAO.updatePassword(idNumber, AuthService.hash(idNumber, password));
        } finally {
            wipe(current);
            wipe(password);
            wipe(confirm);
        }
    }

    // ------------------------------------------------------------------ Validaciones

    private static void clean(TollUser user) {
        user.setIdNumber(trim(user.getIdNumber()));
        user.setFirstName(trim(user.getFirstName()));
        user.setMiddleName(emptyToNull(user.getMiddleName()));
        user.setFirstSurname(trim(user.getFirstSurname()));
        user.setSecondSurname(emptyToNull(user.getSecondSurname()));
        user.setEmail(trim(user.getEmail()).toLowerCase(Locale.ROOT));
        String phone = trim(user.getPhone()).replaceAll("[\\s-]", "");
        user.setPhone(phone.isEmpty() ? null : phone);
    }

    private static void validate(TollUser user, boolean isNew) {
        if (isNew && !user.getIdNumber().matches("\\d{6,15}")) {
            throw new ValidationException("La cédula debe tener entre 6 y 15 dígitos");
        }
        name(user.getFirstName(), "el primer nombre", true);
        name(user.getMiddleName(), "el segundo nombre", false);
        name(user.getFirstSurname(), "el primer apellido", true);
        name(user.getSecondSurname(), "el segundo apellido", false);
        if (user.getEmail().length() > MAX_EMAIL
                || !user.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[a-z]{2,}")) {
            throw new ValidationException("Escribe un correo válido");
        }
        if (user.getPhone() != null && !user.getPhone().matches("\\d{7,15}")) {
            throw new ValidationException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        if (user.getHireDate() != null && user.getHireDate().isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de ingreso no puede ser futura");
        }
    }

    private static void name(String value, String what, boolean required) {
        if (value == null || value.isEmpty()) {
            if (required) {
                throw new ValidationException("Escribe " + what);
            }
            return;
        }
        if (value.length() > MAX_NAME || !value.matches("[\\p{L} .'-]+")) {
            throw new ValidationException("Revisa " + what + ": solo letras, máximo " + MAX_NAME);
        }
    }

    private static RuntimeException friendly(DataAccessException e) {
        if (e.getOracleErrorCode() == 1) {
            return new ValidationException("Ya existe un usuario con esa cédula o con ese correo");
        }
        return e;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String emptyToNull(String value) {
        String text = trim(value);
        return text.isEmpty() ? null : text;
    }

    private static void wipe(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }
}
