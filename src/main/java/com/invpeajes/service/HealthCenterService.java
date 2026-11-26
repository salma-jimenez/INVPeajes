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
    /** Separación mínima entre puntos del recorrido al simplificarlo. */
    private static final double MIN_STEP_KM = 0.3;

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
        // La base filtra y mide la distancia (función fun_centros_cercanos con fun_distancia_km).
        return healthCenterDAO.findNearby(latitude, longitude, radiusKm);
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
     * Centros de salud que quedan cerca de un recorrido (por ejemplo, la ruta de un viaje),
     * ordenados en el orden en que se encuentran al viajar.
     *
     * Primero se traen de Oracle los centros del rectángulo que encierra el recorrido y luego
     * se mide la distancia de cada uno a la línea del recorrido, tramo por tramo.
     *
     * @param path  puntos del recorrido en orden, cada uno {latitud, longitud}
     * @param maxKm distancia máxima a la vía en kilómetros (mayor que 0)
     * @return centros cercanos con los km del viaje en que aparecen y su distancia a la vía
     * @throws IllegalArgumentException si el recorrido tiene menos de 2 puntos o maxKm no es válido
     */
    public List<CenterOnRoute> findAlongRoute(List<double[]> path, double maxKm) {
        if (path == null || path.size() < 2) {
            throw new IllegalArgumentException("El recorrido debe tener al menos dos puntos");
        }
        if (maxKm <= 0) {
            throw new IllegalArgumentException("La distancia máxima debe ser mayor que 0 km");
        }
        List<double[]> points = simplify(path);
        double[] cumulativeKm = new double[points.size()];
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (int i = 0; i < points.size(); i++) {
            double[] p = points.get(i);
            minLat = Math.min(minLat, p[0]);
            maxLat = Math.max(maxLat, p[0]);
            minLon = Math.min(minLon, p[1]);
            maxLon = Math.max(maxLon, p[1]);
            if (i > 0) {
                double[] q = points.get(i - 1);
                cumulativeKm[i] = cumulativeKm[i - 1] + distanceKm(q[0], q[1], p[0], p[1]);
            }
        }
        double deltaLat = maxKm / KM_PER_DEGREE;
        double deltaLon = maxKm / (KM_PER_DEGREE * Math.max(0.1, Math.cos(Math.toRadians((minLat + maxLat) / 2))));
        List<HealthCenter> candidates = healthCenterDAO.findInArea(
                minLat - deltaLat, maxLat + deltaLat, minLon - deltaLon, maxLon + deltaLon);

        List<CenterOnRoute> result = new ArrayList<>();
        for (HealthCenter c : candidates) {
            CenterOnRoute nearest = nearestOnPath(c, points, cumulativeKm);
            if (nearest.getKmFromRoute() <= maxKm) {
                result.add(nearest);
            }
        }
        result.sort(Comparator.comparingDouble(CenterOnRoute::getKmFromOrigin));
        return result;
    }

    /** Quita puntos muy seguidos (menos de 300 m) para que el cálculo sea rápido. */
    private static List<double[]> simplify(List<double[]> path) {
        List<double[]> points = new ArrayList<>();
        points.add(path.get(0));
        for (int i = 1; i < path.size() - 1; i++) {
            double[] last = points.get(points.size() - 1);
            double[] p = path.get(i);
            if (distanceKm(last[0], last[1], p[0], p[1]) >= MIN_STEP_KM) {
                points.add(p);
            }
        }
        points.add(path.get(path.size() - 1));
        return points;
    }

    /** Punto de la vía más cercano a un centro: distancia a la vía y km del viaje en ese punto. */
    private static CenterOnRoute nearestOnPath(HealthCenter c, List<double[]> points, double[] cumulativeKm) {
        // Proyección plana local alrededor del centro: suficiente para pocos kilómetros
        double kmPerDegLon = KM_PER_DEGREE * Math.cos(Math.toRadians(c.getLatitude()));
        double bestDistance = Double.MAX_VALUE;
        double bestKm = 0;
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            double ax = (a[1] - c.getLongitude()) * kmPerDegLon;
            double ay = (a[0] - c.getLatitude()) * KM_PER_DEGREE;
            double bx = (b[1] - c.getLongitude()) * kmPerDegLon;
            double by = (b[0] - c.getLatitude()) * KM_PER_DEGREE;
            double dx = bx - ax;
            double dy = by - ay;
            double length2 = dx * dx + dy * dy;
            double t = length2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / length2));
            double px = ax + t * dx;
            double py = ay + t * dy;
            double distance = Math.sqrt(px * px + py * py);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestKm = cumulativeKm[i - 1] + t * (cumulativeKm[i] - cumulativeKm[i - 1]);
            }
        }
        return new CenterOnRoute(c, bestKm, bestDistance);
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

    /** Centro de salud ubicado sobre un recorrido. */
    public static final class CenterOnRoute {

        private final HealthCenter center;
        private final double kmFromOrigin;
        private final double kmFromRoute;

        /**
         * @param center       centro de salud
         * @param kmFromOrigin km del viaje (desde el origen) en que queda el centro
         * @param kmFromRoute  distancia del centro a la vía, en km
         */
        public CenterOnRoute(HealthCenter center, double kmFromOrigin, double kmFromRoute) {
            this.center = center;
            this.kmFromOrigin = kmFromOrigin;
            this.kmFromRoute = kmFromRoute;
        }

        /** @return centro de salud */
        public HealthCenter getCenter() {
            return center;
        }

        /** @return km del viaje (desde el origen) en que queda el centro */
        public double getKmFromOrigin() {
            return kmFromOrigin;
        }

        /** @return distancia del centro a la vía, en km */
        public double getKmFromRoute() {
            return kmFromRoute;
        }
    }
}
