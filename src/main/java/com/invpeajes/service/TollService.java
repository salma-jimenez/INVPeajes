package com.invpeajes.service;

import com.invpeajes.model.TollBooth;
import com.invpeajes.model.Toll;
import com.invpeajes.persistence.TollBoothDAO;
import com.invpeajes.persistence.TollDAO;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reglas de negocio de las estaciones de peaje y sus casetas.
 * Valida los datos antes de enviarlos a la base de datos y coordina
 * los DAO de Peaje y Caseta.
 *
 * Recibe las interfaces de los DAO por el constructor (Inversión de Dependencias):
 * no sabe si los datos vienen de Oracle o de otra fuente.
 *
 * Errores que lanza:
 * <ul>
 *   <li>{@link IllegalArgumentException}: un dato no cumple las reglas.</li>
 *   <li>{@link IllegalStateException}: la operación no se permite en el estado actual.</li>
 *   <li>{@link NoSuchElementException}: el peaje o la caseta no existe.</li>
 * </ul>
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class TollService {

    public static final String STATUS_OPERATIONAL = "OPERATIVO";
    public static final String BOOTH_ACTIVE = "ACTIVA";
    public static final String BOOTH_INACTIVE = "INACTIVA";

    private static final List<String> TOLL_STATUSES =
            Arrays.asList(STATUS_OPERATIONAL, "NO_OPERATIVO", "SINIESTRADO", "SIN_TARIFA");
    private static final List<String> LEVELS =
            Arrays.asList("INVIAS", "ANI", "CONCESION_TERRITORIAL", "DEPARTAMENTAL",
                          "DISTRITAL", "MUNICIPAL", "DESCONOCIDO");
    private static final List<String> DIRECTIONS =
            Arrays.asList("AMBOS", "SUR_NORTE", "NORTE_SUR",
                          "ORIENTE_OCCIDENTE", "OCCIDENTE_ORIENTE", "OTRO");
    private static final List<String> PAYMENT_TYPES = Arrays.asList("EFECTIVO", "COLPASS", "MIXTO");
    private static final List<String> BOOTH_STATUSES =
            Arrays.asList(BOOTH_ACTIVE, BOOTH_INACTIVE, "MANTENIMIENTO");

    /** Límites geográficos de Colombia, incluido San Andrés (mismos de la BD). */
    private static final double MIN_LATITUDE = -5;
    private static final double MAX_LATITUDE = 14;
    private static final double MIN_LONGITUDE = -82;
    private static final double MAX_LONGITUDE = -66;

    private static final int MAX_NAME_LENGTH = 60;
    private static final int MAX_BOOTH_NUMBER = 999;

    private final TollDAO tollDAO;
    private final TollBoothDAO boothDAO;

    /**
     * Crea el servicio con los DAO de la base de datos.
     */
    public TollService() {
        this(new TollDAO(), new TollBoothDAO());
    }

    /**
     * Crea el servicio con los DAO que usará (permite usar datos falsos en pruebas).
     * @param tollDAO DAO de peajes.
     * @param boothDAO DAO de casetas.
     */
    public TollService(TollDAO tollDAO, TollBoothDAO boothDAO) {
        this.tollDAO = tollDAO;
        this.boothDAO = boothDAO;
    }

    // ------------------------------------------------------------------
    // Peajes
    // ------------------------------------------------------------------

    /**
     * Registra un peaje nuevo después de validar sus datos.
     * @param toll Peaje a registrar.
     * @throws IllegalArgumentException Si algún dato no es válido.
     * @throws IllegalStateException Si ya existe un peaje con ese ID.
     */
    public void registerToll(Toll toll) {
        validateToll(toll);
        if (tollDAO.findById(toll.getTollId()).isPresent()) {
            throw new IllegalStateException("Ya existe un peaje con el ID " + toll.getTollId());
        }
        tollDAO.insert(toll);
    }

    /**
     * Actualiza los datos de un peaje existente.
     * @param toll Peaje con los datos nuevos.
     * @throws IllegalArgumentException Si algún dato no es válido.
     * @throws NoSuchElementException Si el peaje no existe.
     */
    public void updateToll(Toll toll) {
        validateToll(toll);
        if (!tollDAO.update(toll)) {
            throw tollNotFound(toll.getTollId());
        }
    }

    /**
     * Elimina un peaje que no tenga casetas registradas.
     * @param tollId ID del peaje.
     * @throws IllegalStateException Si el peaje tiene casetas.
     * @throws NoSuchElementException Si el peaje no existe.
     */
    public void deleteToll(int tollId) {
        int boothCount = boothDAO.findByToll(tollId).size();
        if (boothCount > 0) {
            throw new IllegalStateException("No se puede eliminar el peaje " + tollId
                    + ": tiene " + boothCount + " caseta(s). Elimínelas primero.");
        }
        if (!tollDAO.delete(tollId)) {
            throw tollNotFound(tollId);
        }
    }

    /**
     * Busca un peaje por su ID.
     * @param tollId ID del peaje.
     * @return El peaje encontrado.
     * @throws NoSuchElementException Si el peaje no existe.
     */
    public Toll findToll(int tollId) {
        return tollDAO.findById(tollId).orElseThrow(() -> tollNotFound(tollId));
    }

    /**
     * Lista todos los peajes.
     * @return Peajes ordenados por nombre.
     */
    public List<Toll> listTolls() {
        return tollDAO.findAll();
    }

    /**
     * Lista los peajes de un departamento.
     * @param departmentId Código del departamento (2 caracteres).
     * @return Peajes del departamento.
     * @throws IllegalArgumentException Si el código no tiene 2 caracteres.
     */
    public List<Toll> listTollsByDepartment(String departmentId) {
        validateDepartment(departmentId);
        return tollDAO.findByDepartment(departmentId);
    }

    /**
     * Lista los peajes con un estado operativo.
     * @param status OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA.
     * @return Peajes con ese estado.
     * @throws IllegalArgumentException Si el estado no es válido.
     */
    public List<Toll> listTollsByStatus(String status) {
        validateValue("estado operativo", status, TOLL_STATUSES);
        return tollDAO.findByOperationalStatus(status);
    }

    /**
     * Cambia el estado operativo de un peaje.
     * Si el peaje deja de estar OPERATIVO, sus casetas activas pasan a INACTIVA.
     * @param tollId ID del peaje.
     * @param newStatus OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA.
     * @return Número de casetas que se desactivaron.
     * @throws IllegalArgumentException Si el estado no es válido.
     * @throws NoSuchElementException Si el peaje no existe.
     */
    public int changeTollStatus(int tollId, String newStatus) {
        validateValue("estado operativo", newStatus, TOLL_STATUSES);
        Toll toll = findToll(tollId);
        toll.setOperationalStatus(newStatus);
        tollDAO.update(toll);

        int deactivated = 0;
        if (!STATUS_OPERATIONAL.equals(newStatus)) {
            for (TollBooth booth : boothDAO.findByToll(tollId)) {
                if (BOOTH_ACTIVE.equals(booth.getStatus())) {
                    boothDAO.updateStatus(booth.getBoothId(), BOOTH_INACTIVE);
                    deactivated++;
                }
            }
        }
        return deactivated;
    }

    // ------------------------------------------------------------------
    // Casetas
    // ------------------------------------------------------------------

    /**
     * Agrega una caseta a un peaje existente. El ID lo genera la base de datos.
     * @param booth Caseta a agregar.
     * @throws IllegalArgumentException Si algún dato no es válido.
     * @throws IllegalStateException Si se intenta activar en un peaje no operativo.
     * @throws NoSuchElementException Si el peaje no existe.
     */
    public void addBooth(TollBooth booth) {
        validateBooth(booth);
        Toll toll = findToll(booth.getTollId());
        validateActivation(toll, booth.getStatus());
        boothDAO.insert(booth);
    }

    /**
     * Lista las casetas de un peaje.
     * @param tollId ID del peaje.
     * @return Casetas ordenadas por número.
     * @throws NoSuchElementException Si el peaje no existe.
     */
    public List<TollBooth> listBooths(int tollId) {
        findToll(tollId);
        return boothDAO.findByToll(tollId);
    }

    /**
     * Cambia el estado de una caseta.
     * @param boothId ID de la caseta.
     * @param newStatus ACTIVA, INACTIVA o MANTENIMIENTO.
     * @throws IllegalArgumentException Si el estado no es válido.
     * @throws IllegalStateException Si se intenta activar en un peaje no operativo.
     * @throws NoSuchElementException Si la caseta no existe.
     */
    public void changeBoothStatus(int boothId, String newStatus) {
        validateValue("estado de la caseta", newStatus, BOOTH_STATUSES);
        TollBooth booth = boothDAO.findById(boothId)
                .orElseThrow(() -> new NoSuchElementException("No existe la caseta " + boothId));
        validateActivation(findToll(booth.getTollId()), newStatus);
        boothDAO.updateStatus(boothId, newStatus);
    }

    /**
     * Cambia la forma de pago, el sentido o el estado de una caseta. Igual que al agregarla, solo
     * puede quedar ACTIVA si su peaje está OPERATIVO.
     *
     * @param booth Caseta con los datos nuevos (con su ID).
     * @throws IllegalArgumentException Si algún dato no es válido.
     * @throws IllegalStateException Si se intenta activar en un peaje que no está operativo.
     * @throws NoSuchElementException Si la caseta no existe.
     */
    public void updateBooth(TollBooth booth) {
        validateBooth(booth);
        validateActivation(findToll(booth.getTollId()), booth.getStatus());
        if (!boothDAO.update(booth)) {
            throw new NoSuchElementException("No existe la caseta " + booth.getBoothId());
        }
    }

    /**
     * Elimina una caseta.
     * @param boothId ID de la caseta.
     * @throws NoSuchElementException Si la caseta no existe.
     */
    public void deleteBooth(int boothId) {
        if (!boothDAO.delete(boothId)) {
            throw new NoSuchElementException("No existe la caseta " + boothId);
        }
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private void validateToll(Toll toll) {
        if (toll == null) {
            throw new IllegalArgumentException("El peaje no puede ser nulo");
        }
        if (toll.getTollId() <= 0) {
            throw new IllegalArgumentException("El ID del peaje debe ser mayor que 0");
        }
        String name = toll.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del peaje es obligatorio");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre del peaje no puede superar "
                    + MAX_NAME_LENGTH + " caracteres");
        }
        validateLocation(toll.getLatitude(), toll.getLongitude());
        validateDepartment(toll.getDepartmentId());
        validateValue("estado operativo", toll.getOperationalStatus(), TOLL_STATUSES);
        validateValue("nivel", toll.getLevel(), LEVELS);
        if (toll.getDirection() != null) {
            validateValue("sentido", toll.getDirection(), DIRECTIONS);
        }
    }

    private void validateLocation(double latitude, double longitude) {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException("La latitud " + latitude
                    + " está fuera de Colombia (debe estar entre " + MIN_LATITUDE
                    + " y " + MAX_LATITUDE + ")");
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException("La longitud " + longitude
                    + " está fuera de Colombia (debe estar entre " + MIN_LONGITUDE
                    + " y " + MAX_LONGITUDE + ")");
        }
    }

    private void validateDepartment(String departmentId) {
        if (departmentId == null || departmentId.trim().length() != 2) {
            throw new IllegalArgumentException("El código de departamento debe tener 2 caracteres");
        }
    }

    private void validateBooth(TollBooth booth) {
        if (booth == null) {
            throw new IllegalArgumentException("La caseta no puede ser nula");
        }
        if (booth.getBoothNumber() < 1 || booth.getBoothNumber() > MAX_BOOTH_NUMBER) {
            throw new IllegalArgumentException("El número de caseta debe estar entre 1 y "
                    + MAX_BOOTH_NUMBER);
        }
        validateValue("tipo de pago", booth.getPaymentType(), PAYMENT_TYPES);
        validateValue("estado de la caseta", booth.getStatus(), BOOTH_STATUSES);
        if (booth.getDirection() != null) {
            validateValue("sentido", booth.getDirection(), DIRECTIONS);
        }
    }

    /** Una caseta solo puede quedar ACTIVA si su peaje está OPERATIVO. */
    private void validateActivation(Toll toll, String boothStatus) {
        if (BOOTH_ACTIVE.equals(boothStatus)
                && !STATUS_OPERATIONAL.equals(toll.getOperationalStatus())) {
            throw new IllegalStateException("No se puede activar una caseta del peaje "
                    + toll.getName() + " porque está " + toll.getOperationalStatus());
        }
    }

    private void validateValue(String field, String value, List<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("Valor no válido para " + field + ": " + value
                    + ". Valores permitidos: " + allowed);
        }
    }

    private NoSuchElementException tollNotFound(int tollId) {
        return new NoSuchElementException("No existe el peaje " + tollId);
    }
}