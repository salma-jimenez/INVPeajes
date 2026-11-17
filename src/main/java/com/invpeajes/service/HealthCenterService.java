package com.invpeajes.service;

import com.invpeajes.model.HealthCenter;
import com.invpeajes.model.Toll;
import com.invpeajes.persistence.HealthCenterDAO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reglas de negocio de los centros de salud públicos que se muestran en el mapa.
 * Valida los datos antes de enviarlos a la base de datos y calcula qué sedes
 * quedan cerca de un peaje (útil para emergencias en carretera).
 *
 * Recibe la interfaz del DAO por el constructor (Inversión de Dependencias).
 *
 * Errores que lanza:
 * <ul>
 *   <li>{@link IllegalArgumentException}: un dato no cumple las reglas.</li>
 *   <li>{@link NoSuchElementException}: el centro de salud no existe.</li>
 * </ul>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class HealthCenterService {

    public static final String TYPE_HOSPITAL = "Hospital";
    public static final String TYPE_HEALTH_CENTER = "Centro de salud";
    public static final String TYPE_HEALTH_POST = "Puesto de salud";
    public static final String TYPE_OTHER = "Otro / Unidad de salud";

    private static final List<String> TYPES =
            Arrays.asList(TYPE_HOSPITAL, TYPE_HEALTH_CENTER, TYPE_HEALTH_POST, TYPE_OTHER);
    private static final List<String> PRECISIONS =
            Arrays.asList(HealthCenter.PRECISION_EXACT, HealthCenter.PRECISION_MUNICIPALITY);

    /** Límites geográficos de Colombia, incluido San Andrés (los mismos que usa TollService). */
    private static final double MIN_LATITUDE = -5;
    private static final double MAX_LATITUDE = 14;
    private static final double MIN_LONGITUDE = -82;
    private static final double MAX_LONGITUDE = -66;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_PER_DEGREE = 111.0;
    private static final int MAX_NAME_LENGTH = 300;

    private final HealthCenterDAO healthCenterDAO;

    /** Crea el servicio con su DAO por defecto (base de datos Oracle). */
    public HealthCenterService() {
        this(new HealthCenterDAO());
    }

    /**
     * Crea el servicio con el DAO que usará (útil para pruebas).
     *
     * @param healthCenterDAO DAO de centros de salud
     */
    public HealthCenterService(HealthCenterDAO healthCenterDAO) {
        this.healthCenterDAO = healthCenterDAO;
    }

    /**
     * Registra un centro de salud nuevo después de validar sus datos.
     *
     * @param center centro a registrar
     * @throws IllegalArgumentException si algún dato no es válido
     */
    public void register(HealthCenter center) {
        validate(center);
        healthCenterDAO.insert(center);
    }

    /**
     * Actualiza un centro de salud existente.
     *
     * @param center centro con los datos nuevos
     * @throws IllegalArgumentException si algún dato no es válido
     * @throws NoSuchElementException   si el centro no existe
     */
    public void update(HealthCenter center) {
        validate(center);
        if (center.getHealthCenterId() == null || !healthCenterDAO.update(center)) {
            throw new NoSuchElementException("No existe el centro de salud " + center.getHealthCenterId());
        }
    }

    /**
     * Elimina un centro de salud.
     *
     * @param healthCenterId ID del centro
     * @throws NoSuchElementException si el centro no existe
     */
    public void delete(int healthCenterId) {
        if (!healthCenterDAO.delete(healthCenterId)) {
            throw new NoSuchElementException("No existe el centro de salud " + healthCenterId);
        }
    }

    /**
     * Busca un centro de salud por su ID.
     *
     * @param healthCenterId ID del centro
     * @return el centro encontrado
     * @throws NoSuchElementException si no existe
     */
    public HealthCenter findById(int healthCenterId) {
        return healthCenterDAO.findById(healthCenterId)
                .orElseThrow(() -> new NoSuchElementException("No existe el centro de salud " + healthCenterId));
    }

    /**
     * Lista todos los centros de salud.
     *
     * @return todos los centros
     */
    public List<HealthCenter> findAll() {
        return healthCenterDAO.findAll();
    }

    /**
     * Lista los centros de salud de un departamento.
     *
     * @param department nombre del departamento
     * @return centros del departamento
     * @throws IllegalArgumentException si el departamento está vacío
     */
    public List<HealthCenter> findByDepartment(String department) {
        requireText(department, "el departamento");
        return healthCenterDAO.findByDepartment(department.trim());
    }

    /**
     * Lista los centros de salud de un municipio.
     *
     * @param divipolaCode código DANE del municipio (5 dígitos)
     * @return centros del municipio
     * @throws IllegalArgumentException si el código no tiene 5 dígitos
     */
    public List<HealthCenter> findByMunicipality(String divipolaCode) {
        if (divipolaCode == null || !divipolaCode.matches("\\d{5}")) {
            throw new IllegalArgumentException("El código DANE del municipio debe tener 5 dígitos");
        }
        return healthCenterDAO.findByMunicipality(divipolaCode);
    }

    /**
     * Lista los centros de salud de un tipo.
     *
     * @param type uno de los tipos de {@code TYPE_*}
     * @return centros de ese tipo
     * @throws IllegalArgumentException si el tipo no es válido
     */
    public List<HealthCenter> findByType(String type) {
        requireAllowed(type, TYPES, "tipo de centro de salud");
        return healthCenterDAO.findByType(type);
    }

    /**
     * Lista los centros de salud que están a una distancia máxima de un punto,
     * ordenados del más cercano al más lejano.
     *
     * @param latitude  latitud del punto
     * @param longitude longitud del punto
     * @param radiusKm  distancia máxima en kilómetros (mayor que 0)
     * @return centros dentro del radio, del más cercano al más lejano
     * @throws IllegalArgumentException si las coordenadas o el radio no son válidos
     */
    public List<HealthCenter> findNearby(double latitude, double longitude, double radiusKm) {
        validateCoordinates(latitude, longitude);
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("El radio de búsqueda debe ser mayor que 0 km");
        }
        // Primero se filtra en Oracle con un rectángulo y luego se mide la distancia real
        double deltaLat = radiusKm / KM_PER_DEGREE;
        double deltaLon = radiusKm / (KM_PER_DEGREE * Math.max(0.1, Math.cos(Math.toRadians(latitude))));
        List<HealthCenter> candidates = healthCenterDAO.findInArea(
                latitude - deltaLat, latitude + deltaLat, longitude - deltaLon, longitude + deltaLon);

        List<HealthCenter> nearby = new ArrayList<>();
        for (HealthCenter c : candidates) {
            if (distanceKm(latitude, longitude, c.getLatitude(), c.getLongitude()) <= radiusKm) {
                nearby.add(c);
            }
        }
        nearby.sort(Comparator.comparingDouble(
                c -> distanceKm(latitude, longitude, c.getLatitude(), c.getLongitude())));
        return nearby;
    }

    /**
     * Lista los centros de salud cercanos a un peaje.
     *
     * @param toll    peaje de referencia
     * @param radiusKm distancia máxima en kilómetros
     * @return centros dentro del radio, del más cercano al más lejano
     */
    public List<HealthCenter> findNearToll(Toll toll, double radiusKm) {
        if (toll == null) {
            throw new IllegalArgumentException("Debe indicar el peaje");
        }
        return findNearby(toll.getLatitude(), toll.getLongitude(), radiusKm);
    }

    /**
     * Distancia en línea recta entre dos puntos (fórmula de Haversine).
     *
     * @param lat1 latitud del primer punto
     * @param lon1 longitud del primer punto
     * @param lat2 latitud del segundo punto
     * @param lon2 longitud del segundo punto
     * @return distancia en kilómetros
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    private void validate(HealthCenter c) {
        if (c == null) {
            throw new IllegalArgumentException("Debe indicar el centro de salud");
        }
        requireText(c.getName(), "el nombre de la sede");
        if (c.getName().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre de la sede no puede pasar de "
                    + MAX_NAME_LENGTH + " caracteres");
        }
        requireAllowed(c.getType(), TYPES, "tipo de centro de salud");
        requireAllowed(c.getPrecision(), PRECISIONS, "precisión de la coordenada");
        validateCoordinates(c.getLatitude(), c.getLongitude());
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE
                || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException("La coordenada (" + latitude + ", " + longitude
                    + ") está fuera de Colombia");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta " + fieldName);
        }
    }

    private void requireAllowed(String value, List<String> allowed, String fieldName) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("Valor no válido para " + fieldName + ": " + value
                    + ". Valores permitidos: " + allowed);
        }
    }
}
