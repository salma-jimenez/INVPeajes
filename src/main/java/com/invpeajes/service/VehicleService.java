package com.invpeajes.service;

import com.invpeajes.model.Vehicle;
import com.invpeajes.persistence.IVehicleDAO;
import com.invpeajes.persistence.VehicleDAOImpl;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de negocio para vehículos: validación de placas colombianas y registro
 * de tags de pago electrónico.
 *
 * @author Salomejimenez
 */
public class VehicleService {

    private static final Pattern PRIVATE_PLATE = Pattern.compile("^[A-Z]{3}\\d{3}$");
    private static final Pattern MOTORCYCLE_PLATE = Pattern.compile("^[A-Z]{3}\\d{2}[A-Z]$");
    private static final Pattern TRAILER_PLATE = Pattern.compile("^R\\d{5}$");
    private static final Pattern TAG_FORMAT = Pattern.compile("^[A-Z0-9-]{6,30}$");

    private final IVehicleDAO vehicleDAO;
    private final VehicleCategoryService categoryService;

    /**
     * Crea el servicio con sus dependencias por defecto.
     */
    public VehicleService() {
        this(new VehicleDAOImpl(), new VehicleCategoryService());
    }

    /**
     * Crea el servicio con dependencias inyectadas.
     *
     * @param vehicleDAO DAO de vehículos
     * @param categoryService servicio para verificar categorías
     */
    public VehicleService(IVehicleDAO vehicleDAO, VehicleCategoryService categoryService) {
        this.vehicleDAO = vehicleDAO;
        this.categoryService = categoryService;
    }

    /**
     * Quita espacios y guiones de una placa y la pasa a mayúsculas.
     *
     * @param plate placa como la escribió el usuario (ej. "abc-123")
     * @return placa normalizada (ej. "ABC123"), o cadena vacía si es null
     */
    public static String normalizePlate(String plate) {
        return plate == null ? "" : plate.replaceAll("[\\s-]", "").toUpperCase();
    }

    /**
     * Verifica si una placa tiene formato colombiano válido: particular/público
     * (ABC123), moto (ABC12D) o remolque (R12345).
     *
     * @param plate placa a verificar (se normaliza antes)
     * @return true si el formato es válido
     */
    public static boolean isValidPlate(String plate) {
        String normalized = normalizePlate(plate);
        return PRIVATE_PLATE.matcher(normalized).matches()
                || MOTORCYCLE_PLATE.matcher(normalized).matches()
                || TRAILER_PLATE.matcher(normalized).matches();
    }

    /**
     * Registra un vehículo nuevo.
     *
     * @param vehicle vehículo a registrar
     */
    public void register(Vehicle vehicle) {
        validate(vehicle);
        if (vehicleDAO.findById(vehicle.getPlate()).isPresent()) {
            throw new ValidationException("La placa " + vehicle.getPlate() + " ya está registrada");
        }
        if (vehicle.hasTag()) {
            ensureTagAvailable(vehicle.getTagCode(), vehicle.getPlate());
        }
        vehicleDAO.insert(vehicle);
    }

    /**
     * Busca un vehículo por placa.
     *
     * @param plate placa a buscar (se normaliza)
     * @return el vehículo si existe
     */
    public Optional<Vehicle> find(String plate) {
        return vehicleDAO.findById(normalizePlate(plate));
    }

    /**
     * Obtiene un vehículo o lanza error si no existe.
     *
     * @param plate placa a buscar (se normaliza)
     * @return el vehículo encontrado
     */
    public Vehicle get(String plate) {
        String normalized = normalizePlate(plate);
        return vehicleDAO.findById(normalized)
                .orElseThrow(() -> new ValidationException("No existe un vehículo con placa " + normalized));
    }

    /**
     * Busca el vehículo asociado a un tag electrónico, útil al pasar por una
     * caseta COLPASS.
     *
     * @param tagCode código del tag
     * @return el vehículo si existe
     */
    public Optional<Vehicle> findByTag(String tagCode) {
        return vehicleDAO.findByTag(normalizeTag(tagCode));
    }

    /**
     * Lista todos los vehículos.
     *
     * @return vehículos ordenados por placa
     */
    public List<Vehicle> listAll() {
        return vehicleDAO.findAll();
    }

    /**
     * Lista los vehículos de una categoría.
     *
     * @param categoryId categoría a filtrar
     * @return vehículos de esa categoría
     */
    public List<Vehicle> listByCategory(int categoryId) {
        return vehicleDAO.findByCategory(categoryId);
    }

    /**
     * Actualiza los datos de un vehículo existente.
     *
     * @param vehicle vehículo con los datos nuevos
     */
    public void update(Vehicle vehicle) {
        validate(vehicle);
        if (vehicle.hasTag()) {
            ensureTagAvailable(vehicle.getTagCode(), vehicle.getPlate());
        }
        if (!vehicleDAO.update(vehicle)) {
            throw new ValidationException("No existe un vehículo con placa " + vehicle.getPlate());
        }
    }

    /**
     * Asigna un tag de pago electrónico (COLPASS) a un vehículo.
     *
     * @param plate placa del vehículo
     * @param tagCode código del tag
     * @param intermediaryId intermediador que emite el tag (puede ser null)
     */
    public void registerTag(String plate, String tagCode, Integer intermediaryId) {
        Vehicle vehicle = get(plate);
        String tag = normalizeTag(tagCode);
        if (!TAG_FORMAT.matcher(tag).matches()) {
            throw new ValidationException("El tag debe tener entre 6 y 30 letras, números o guiones");
        }
        ensureTagAvailable(tag, vehicle.getPlate());
        vehicleDAO.updateTag(vehicle.getPlate(), tag, intermediaryId);
    }

    /**
     * Retira el tag electrónico de un vehículo. También se quita el
     * intermediador, porque la base de datos no permite uno sin tag.
     *
     * @param plate placa del vehículo
     */
    public void removeTag(String plate) {
        Vehicle vehicle = get(plate);
        if (!vehicle.hasTag()) {
            throw new ValidationException("El vehículo " + vehicle.getPlate() + " no tiene tag registrado");
        }
        vehicleDAO.updateTag(vehicle.getPlate(), null, null);
    }

    /**
     * Elimina un vehículo sin tiquetes ni asistencias asociadas.
     *
     * @param plate placa del vehículo
     */
    public void delete(String plate) {
        String normalized = normalizePlate(plate);
        if (!vehicleDAO.delete(normalized)) {
            throw new ValidationException("No existe un vehículo con placa " + normalized);
        }
    }

    private void validate(Vehicle vehicle) {
        if (!isValidPlate(vehicle.getPlate())) {
            throw new ValidationException("Placa inválida. Formatos: ABC123, ABC12D (moto) o R12345 (remolque)");
        }
        vehicle.setPlate(normalizePlate(vehicle.getPlate()));
        if (!categoryService.exists(vehicle.getCategoryId())) {
            throw new ValidationException("No existe la categoría " + vehicle.getCategoryId());
        }
        if (vehicle.getIntermediaryId() != null && !vehicle.hasTag()) {
            throw new ValidationException("No se puede asignar un intermediador sin código de tag");
        }
        if (vehicle.hasTag()) {
            vehicle.setTagCode(normalizeTag(vehicle.getTagCode()));
        }
    }

    private void ensureTagAvailable(String tagCode, String ownPlate) {
        Optional<Vehicle> owner = vehicleDAO.findByTag(tagCode);
        if (owner.isPresent() && !owner.get().getPlate().equals(ownPlate)) {
            throw new ValidationException("El tag " + tagCode + " ya pertenece al vehículo "
                    + owner.get().getPlate());
        }
    }

    private String normalizeTag(String tagCode) {
        return tagCode == null ? "" : tagCode.trim().toUpperCase();
    }

}
