
package com.invpeajes.service;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.persistence.IVehicleCategoryDAO;
import com.invpeajes.persistence.VehicleCategoryDAOImpl;

import java.util.List;

/**
 * Reglas de negocio para las categorías vehiculares.
 * @author Salomejimenez
 */
public class VehicleCategoryService {

    private static final int MAX_CODE_LENGTH = 12;
    private static final int MIN_AXLES = 2;
    private static final int MAX_AXLES = 10;

    private final IVehicleCategoryDAO categoryDAO;

    /** Crea el servicio con su DAO por defecto. */
    public VehicleCategoryService() {
        this(new VehicleCategoryDAOImpl());
    }

    /**
     * Crea el servicio con un DAO inyectado.
     *
     * @param categoryDAO DAO de categorías
     */
    public VehicleCategoryService(IVehicleCategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    /**
     * Registra una categoría nueva tras validar sus datos.
     *
     * @param category categoría a registrar
     */
    public void register(VehicleCategory category) {
        validate(category);
        if (exists(category.getCategoryId())) {
            throw new ValidationException("Ya existe la categoría con id " + category.getCategoryId());
        }
        categoryDAO.insert(category);
    }

    /**
     * Obtiene una categoría o lanza error si no existe.
     *
     * @param categoryId id de la categoría
     * @return la categoría encontrada
     */
    public VehicleCategory get(int categoryId) {
        return categoryDAO.findById(categoryId)
                .orElseThrow(() -> new ValidationException("No existe la categoría " + categoryId));
    }

    /**
     * Indica si existe una categoría con ese id.
     *
     * @param categoryId id a verificar
     * @return true si existe
     */
    public boolean exists(int categoryId) {
        return categoryDAO.findById(categoryId).isPresent();
    }

    /**
     * Lista todas las categorías.
     *
     * @return categorías ordenadas por id
     */
    public List<VehicleCategory> listAll() {
        return categoryDAO.findAll();
    }

    /**
     * Actualiza una categoría existente.
     *
     * @param category categoría con los datos nuevos
     */
    public void update(VehicleCategory category) {
        validate(category);
        if (!categoryDAO.update(category)) {
            throw new ValidationException("No existe la categoría " + category.getCategoryId());
        }
    }

    /**
     * Elimina una categoría que no tenga vehículos ni tarifas asociadas.
     *
     * @param categoryId id de la categoría
     */
    public void delete(int categoryId) {
        if (!categoryDAO.delete(categoryId)) {
            throw new ValidationException("No existe la categoría " + categoryId);
        }
    }

    private void validate(VehicleCategory category) {
        if (category.getCategoryId() <= 0) {
            throw new ValidationException("El id de la categoría debe ser positivo");
        }
        String code = category.getCategoryCode() == null ? "" : category.getCategoryCode().trim();
        if (code.isEmpty()) {
            throw new ValidationException("El código de la categoría es obligatorio");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new ValidationException("La categoría admite máximo " + MAX_CODE_LENGTH + " caracteres");
        }
        Integer axles = category.getAxleCount();
        if (axles != null && (axles < MIN_AXLES || axles > MAX_AXLES)) {
            throw new ValidationException("El número de ejes debe estar entre " + MIN_AXLES + " y " + MAX_AXLES);
        }
        category.setCategoryCode(code.toUpperCase());
    }
    
}
