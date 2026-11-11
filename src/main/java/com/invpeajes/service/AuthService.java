package com.invpeajes.service;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.Traveler;
import com.invpeajes.persistence.CashierDAOImpl;
import com.invpeajes.persistence.DataAccessException;
import com.invpeajes.persistence.ICashierDAO;
import com.invpeajes.persistence.TravelerDAO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Inicio de sesión unificado de RutaPeaje.
 *
 * <ul>
 *   <li><b>Empleados</b> (operador de caseta, jefe de estación y administrador): entran con su
 *       cédula y contraseña. El cargo guardado en USUARIO_PEAJE decide a qué pantalla van.</li>
 *   <li><b>Viajeros</b>: se registran con su correo, o entran como invitados sin cuenta.</li>
 * </ul>
 *
 * Las contraseñas se guardan como hash SHA-256 en hexadecimal (mayúsculas) de
 * {@code identificador + ":" + contraseña}: el identificador es la cédula para los empleados y el
 * correo en minúsculas para los viajeros. Es el mismo cálculo que hace Oracle con
 * {@code RAWTOHEX(STANDARD_HASH(CEDULA || ':' || CONTRASENA, 'SHA256'))}, para que el script de la
 * base de datos pueda crear las contraseñas iniciales.
 *
 * Después de {@link #MAX_ATTEMPTS} intentos fallidos con la misma cédula o correo, se bloquea por
 * {@link #LOCK_TIME}.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AuthService {

    public static final String ROLE_OPERATOR = "OPERADOR DE CASETA";
    public static final String ROLE_MANAGER = "JEFE DE ESTACION";
    public static final String ROLE_ADMIN = "ADMINISTRADOR";

    public static final int MAX_ATTEMPTS = 3;
    public static final Duration LOCK_TIME = Duration.ofMinutes(1);
    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final String WRONG_EMPLOYEE = "Cédula o contraseña incorrecta";
    private static final String WRONG_TRAVELER = "Correo o contraseña incorrecta";
    private static final Pattern EMPLOYEE_ID = Pattern.compile("\\d{6,15}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}");
    private static final Pattern NAME = Pattern.compile("[\\p{L} '.-]{2,30}");
    private static final Pattern PHONE = Pattern.compile("\\d{7,15}");
    private static final Pattern PLATE = Pattern.compile("[A-Z]{3}\\d{3}|[A-Z]{3}\\d{2}[A-Z]|[RS]\\d{5}");

    private final ICashierDAO employeeDAO;
    private final TravelerDAO travelerDAO;
    private final Map<String, Attempts> attempts = new HashMap<>();

    /** Crea el servicio con los DAO de la base de datos. */
    public AuthService() {
        this(new CashierDAOImpl(), new TravelerDAO());
    }

    /**
     * Crea el servicio con DAO específicos (permite usar datos falsos en pruebas).
     * @param employeeDAO DAO de empleados.
     * @param travelerDAO DAO de viajeros.
     */
    public AuthService(ICashierDAO employeeDAO, TravelerDAO travelerDAO) {
        this.employeeDAO = employeeDAO;
        this.travelerDAO = travelerDAO;
    }

    /**
     * Valida la cédula y la contraseña de un empleado.
     * @param employeeId Cédula.
     * @param password Contraseña escrita (se borra de memoria al terminar).
     * @return Sesión del empleado con su cargo y su peaje (sin caseta).
     * @throws IllegalArgumentException Si la cédula no tiene formato válido o los datos no coinciden.
     * @throws IllegalStateException Si la cuenta está bloqueada por intentos fallidos o el cargo no es válido.
     * @throws DataAccessException Si falla la base de datos.
     */
    public CashierSession employeeLogin(String employeeId, char[] password) {
        try {
            String id = employeeId == null ? "" : employeeId.trim();
            if (!EMPLOYEE_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("La cédula debe tener entre 6 y 15 dígitos");
            }
            checkLocked("E:" + id);
            String expected = employeeDAO.findPasswordHash(id).orElse(null);
            if (expected == null || !expected.equalsIgnoreCase(hash(id, password))) {
                throw new IllegalArgumentException(failed("E:" + id, WRONG_EMPLOYEE));
            }
            CashierSession session = employeeDAO.findOperator(id)
                    .orElseThrow(() -> new IllegalArgumentException(WRONG_EMPLOYEE));
            String role = session.getRole();
            if (!ROLE_OPERATOR.equals(role) && !ROLE_MANAGER.equals(role) && !ROLE_ADMIN.equals(role)) {
                throw new IllegalStateException("El cargo " + role + " no tiene acceso al sistema");
            }
            if (!ROLE_ADMIN.equals(role) && session.getTollId() == 0) {
                throw new IllegalStateException(session.getOperatorName() + " no tiene un peaje asignado");
            }
            attempts.remove("E:" + id);
            return session;
        } finally {
            clear(password);
        }
    }

    /**
     * Valida el correo y la contraseña de un viajero.
     * @param email Correo.
     * @param password Contraseña escrita (se borra de memoria al terminar).
     * @return El viajero.
     * @throws IllegalArgumentException Si los datos no coinciden.
     * @throws IllegalStateException Si la cuenta está bloqueada por intentos fallidos.
     * @throws DataAccessException Si falla la base de datos.
     */
    public Traveler travelerLogin(String email, char[] password) {
        try {
            String mail = normalizeEmail(email);
            if (!EMAIL.matcher(mail).matches()) {
                throw new IllegalArgumentException("Escriba un correo válido");
            }
            checkLocked("V:" + mail);
            Traveler traveler = travelerDAO.findByEmail(mail).orElse(null);
            if (traveler == null || !traveler.getPasswordHash().equalsIgnoreCase(hash(mail, password))) {
                throw new IllegalArgumentException(failed("V:" + mail, WRONG_TRAVELER));
            }
            attempts.remove("V:" + mail);
            return traveler;
        } finally {
            clear(password);
        }
    }

    /**
     * Registra un viajero nuevo.
     * @param firstName Nombre.
     * @param lastName Apellido.
     * @param email Correo (será su usuario).
     * @param phone Teléfono (7 a 15 dígitos).
     * @param plate Placa (opcional; puede ser vacía).
     * @param password Contraseña (mínimo {@link #MIN_PASSWORD_LENGTH} caracteres).
     * @param confirmation Confirmación de la contraseña.
     * @return El viajero ya guardado, con su ID.
     * @throws IllegalArgumentException Si algún dato no es válido o el correo ya está registrado.
     * @throws DataAccessException Si falla la base de datos.
     */
    public Traveler registerTraveler(String firstName, String lastName, String email, String phone,
                                     String plate, char[] password, char[] confirmation) {
        try {
            String first = trim(firstName);
            String last = trim(lastName);
            String mail = normalizeEmail(email);
            String cleanPhone = phone == null ? "" : phone.replaceAll("[\\s-]", "");
            String cleanPlate = plate == null ? "" : plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);

            if (!NAME.matcher(first).matches()) {
                throw new IllegalArgumentException("Escriba un nombre válido");
            }
            if (!NAME.matcher(last).matches()) {
                throw new IllegalArgumentException("Escriba un apellido válido");
            }
            if (!EMAIL.matcher(mail).matches()) {
                throw new IllegalArgumentException("Escriba un correo válido");
            }
            if (!PHONE.matcher(cleanPhone).matches()) {
                throw new IllegalArgumentException("El teléfono debe tener entre 7 y 15 dígitos");
            }
            if (!cleanPlate.isEmpty() && !PLATE.matcher(cleanPlate).matches()) {
                throw new IllegalArgumentException("Placa no válida: use el formato ABC123 (carro) o ABC12D (moto)");
            }
            if (password == null || password.length < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("La contraseña debe tener al menos "
                        + MIN_PASSWORD_LENGTH + " caracteres");
            }
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Las contraseñas no coinciden");
            }
            if (travelerDAO.findByEmail(mail).isPresent()) {
                throw new IllegalArgumentException("Ya existe una cuenta con este correo");
            }
            Traveler traveler = new Traveler(null, first, last, mail, cleanPhone,
                    cleanPlate.isEmpty() ? null : cleanPlate, hash(mail, password), null);
            travelerDAO.insert(traveler);
            return traveler;
        } finally {
            clear(password);
            clear(confirmation);
        }
    }

    /**
     * Calcula el hash SHA-256 de {@code salt + ":" + password} en hexadecimal (mayúsculas).
     * @param salt Cédula del empleado o correo del viajero en minúsculas.
     * @param password Contraseña.
     * @return Hash de 64 caracteres.
     */
    public static String hash(String salt, char[] password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + new String(password == null ? new char[0] : password))
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private void checkLocked(String key) {
        Attempts a = attempts.get(key);
        if (a != null && a.lockedUntil != null && LocalDateTime.now().isBefore(a.lockedUntil)) {
            long seconds = Duration.between(LocalDateTime.now(), a.lockedUntil).getSeconds() + 1;
            throw new IllegalStateException("Demasiados intentos fallidos. Intente de nuevo en "
                    + seconds + " segundos");
        }
    }

    private String failed(String key, String message) {
        Attempts a = attempts.computeIfAbsent(key, k -> new Attempts());
        if (a.lockedUntil != null && LocalDateTime.now().isAfter(a.lockedUntil)) {
            a.count = 0;
            a.lockedUntil = null;
        }
        a.count++;
        int left = MAX_ATTEMPTS - a.count;
        if (left <= 0) {
            a.lockedUntil = LocalDateTime.now().plus(LOCK_TIME);
            return message + ". Por seguridad, espere " + LOCK_TIME.getSeconds() + " segundos";
        }
        return message + ". Te " + (left == 1 ? "queda 1 intento" : "quedan " + left + " intentos");
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static void clear(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    /** Intentos fallidos de una cédula o correo. */
    private static final class Attempts {
        private int count;
        private LocalDateTime lockedUntil;
    }
}
