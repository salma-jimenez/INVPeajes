package com.invpeajes.service;

import com.invpeajes.model.TollRate;
import com.invpeajes.persistence.TollRateDao;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Reglas de negocio para la gestión de las tarifas de los peajes.
 * <p>
 * Valida los datos de cada tarifa antes de guardarla y entrega el precio
 * vigente que otros servicios, como el de tiquetes, necesitan para cobrar.
 * </p>
 * <p>
 * Los datos inválidos se reportan con {@link IllegalArgumentException} y los
 * errores de base de datos con {@link SQLException}.
 * </p>
 */
public class TollRateService {

    /** Entidades que pueden publicar una tarifa. */
    public static final List<String> SOURCES = List.of("ANI", "INVIAS");

    private static final int ORACLE_DUPLICATE_KEY_ERROR = 1;
    private static final int ORACLE_PARENT_NOT_FOUND_ERROR = 2291;

    private final TollRateDao tollRateDAO;

    /**
     * Crea el servicio con su acceso a datos por defecto.
     */
    public TollRateService() {
        this(new TollRateDao());
    }

    /**
     * Crea el servicio con un acceso a datos específico.
     * Permite reemplazar el DAO en pruebas.
     *
     * @param tollRateDAO acceso a datos de tarifas
     */
    public TollRateService(TollRateDao tollRateDAO) {
        this.tollRateDAO = tollRateDAO;
    }

    /**
     * Registra una tarifa nueva para un peaje y una categoría.
     *
     * @param rate tarifa a registrar
     * @throws IllegalArgumentException si los datos no son válidos, si ya existe
     *                                  una tarifa para ese peaje y categoría, o
     *                                  si el peaje o la categoría no existen
     * @throws SQLException             si ocurre otro error al guardar
     */
    public void register(TollRate rate) throws SQLException {
        validate(rate);
        try {
            tollRateDAO.create(rate);
        } catch (SQLException e) {
            if (e.getErrorCode() == ORACLE_DUPLICATE_KEY_ERROR) {
                throw new IllegalArgumentException("El peaje " + rate.getTollId()
                        + " ya tiene una tarifa para la categoría " + rate.getCategoryId()
                        + ". Use la opción de actualizar.");
            }
            if (e.getErrorCode() == ORACLE_PARENT_NOT_FOUND_ERROR) {
                throw new IllegalArgumentException("El peaje o la categoría indicados no existen.");
            }
            throw e;
        }
    }

    /**
     * Actualiza el precio, la fecha de vigencia y la fuente de una tarifa existente.
     *
     * @param rate tarifa con los datos nuevos
     * @throws IllegalArgumentException si los datos no son válidos o la tarifa no existe
     * @throws SQLException             si ocurre un error al actualizar
     */
    public void update(TollRate rate) throws SQLException {
        validate(rate);
        if (!tollRateDAO.update(rate)) {
            throw notFound(rate.getTollId(), rate.getCategoryId());
        }
    }

    /**
     * Obtiene el precio que cobra un peaje a una categoría vehicular.
     *
     * @param tollId     identificador del peaje
     * @param categoryId identificador de la categoría vehicular
     * @return precio de la tarifa
     * @throws IllegalArgumentException si los identificadores no son válidos o
     *                                  el peaje no tiene tarifa para esa categoría
     * @throws SQLException             si ocurre un error en la consulta
     */
    public BigDecimal findPrice(int tollId, int categoryId) throws SQLException {
        return findById(tollId, categoryId).getPrice();
    }

    /**
     * Busca la tarifa de un peaje para una categoría.
     *
     * @param tollId     identificador del peaje
     * @param categoryId identificador de la categoría vehicular
     * @return la tarifa encontrada
     * @throws IllegalArgumentException si los identificadores no son válidos o la tarifa no existe
     * @throws SQLException             si ocurre un error en la consulta
     */
    public TollRate findById(int tollId, int categoryId) throws SQLException {
        validateIds(tollId, categoryId);
        TollRate rate = tollRateDAO.findById(tollId, categoryId);
        if (rate == null) {
            throw notFound(tollId, categoryId);
        }
        return rate;
    }

    /**
     * Consulta las tarifas de un peaje, una por categoría.
     * Sirve para mostrar la tabla de tarifas en la información del peaje.
     *
     * @param tollId identificador del peaje
     * @return tarifas del peaje ordenadas por categoría; vacía si no tiene
     * @throws IllegalArgumentException si el identificador no es válido
     * @throws SQLException             si ocurre un error en la consulta
     */
    public List<TollRate> findByTollId(int tollId) throws SQLException {
        validatePositive(tollId, "El peaje");
        return tollRateDAO.findByTollId(tollId);
    }

    /**
     * Consulta todas las tarifas registradas.
     *
     * @return lista de tarifas ordenadas por peaje y categoría
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<TollRate> findAll() throws SQLException {
        return tollRateDAO.findAll();
    }

    /**
     * Elimina la tarifa de un peaje para una categoría.
     *
     * @param tollId     identificador del peaje
     * @param categoryId identificador de la categoría vehicular
     * @throws IllegalArgumentException si los identificadores no son válidos o la tarifa no existe
     * @throws SQLException             si ocurre un error al eliminar
     */
    public void delete(int tollId, int categoryId) throws SQLException {
        validateIds(tollId, categoryId);
        if (!tollRateDAO.delete(tollId, categoryId)) {
            throw notFound(tollId, categoryId);
        }
    }

    private void validate(TollRate rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Los datos de la tarifa son obligatorios.");
        }
        validateIds(rate.getTollId(), rate.getCategoryId());
        if (rate.getPrice() == null) {
            throw new IllegalArgumentException("El precio de la tarifa es obligatorio.");
        }
        rate.setSource(normalizeSource(rate.getSource()));
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String value = source.trim().toUpperCase();
        if (!SOURCES.contains(value)) {
            throw new IllegalArgumentException("Fuente no válida. Use: " + String.join(", ", SOURCES) + ".");
        }
        return value;
    }

    private void validateIds(int tollId, int categoryId) {
        validatePositive(tollId, "El peaje");
        validatePositive(categoryId, "La categoría");
    }

    private void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " debe tener un identificador mayor que cero.");
        }
    }

    private IllegalArgumentException notFound(int tollId, int categoryId) {
        return new IllegalArgumentException("El peaje " + tollId
                + " no tiene tarifa registrada para la categoría " + categoryId + ".");
    }
}
