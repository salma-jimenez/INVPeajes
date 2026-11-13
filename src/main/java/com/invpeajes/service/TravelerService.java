package com.invpeajes.service;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.Concessionaire;
import com.invpeajes.model.Department;
import com.invpeajes.model.Route;
import com.invpeajes.model.Toll;
import com.invpeajes.model.TollRate;
import com.invpeajes.model.Traveler;
import com.invpeajes.model.Vehicle;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.persistence.ConcessionaireDAO;
import com.invpeajes.persistence.DataAccessException;
import com.invpeajes.persistence.DepartmentDAO;
import com.invpeajes.persistence.RouteDAO;
import com.invpeajes.persistence.TravelerDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Servicio de la pantalla del viajero (con cuenta o invitado).
 *
 * Reúne lo que necesita el viajero en un solo lugar: el catálogo de peajes con sus
 * nombres de departamento, ruta y concesionario; las tarifas; el costo de un viaje;
 * pedir asistencia en la vía y seguir sus solicitudes; y actualizar su cuenta.
 * No tiene SQL propio: usa los DAO y servicios que ya existen.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class TravelerService {

    /** Ancho del corredor por defecto alrededor de la línea origen–destino. */
    public static final double DEFAULT_CORRIDOR_KM = 10;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final Pattern PHONE = Pattern.compile("^\\d{7,15}$");

    private final TollService tollService;
    private final TollRateService rateService;
    private final VehicleCategoryService categoryService;
    private final VehicleService vehicleService;
    private final AssistanceService assistanceService;
    private final AssistanceResourceService resourceService;
    private final DepartmentDAO departmentDAO;
    private final ConcessionaireDAO concessionaireDAO;
    private final RouteDAO routeDAO;
    private final TravelerDAO travelerDAO;

    /** Crea el servicio con sus dependencias por defecto (base de datos Oracle). */
    public TravelerService() {
        this(new TollService(), new TollRateService(), new VehicleCategoryService(), new VehicleService(),
                new AssistanceService(), new AssistanceResourceService(), new DepartmentDAO(),
                new ConcessionaireDAO(), new RouteDAO(), new TravelerDAO());
    }

    /**
     * Crea el servicio con dependencias inyectadas (útil para pruebas).
     *
     * @param tollService       servicio de peajes
     * @param rateService       servicio de tarifas
     * @param categoryService   servicio de categorías vehiculares
     * @param vehicleService    servicio de vehículos
     * @param assistanceService servicio de asistencias
     * @param resourceService   servicio de recursos de asistencia
     * @param departmentDAO     DAO de departamentos
     * @param concessionaireDAO DAO de concesionarios
     * @param routeDAO          DAO de rutas
     * @param travelerDAO       DAO de viajeros
     */
    public TravelerService(TollService tollService, TollRateService rateService,
                           VehicleCategoryService categoryService, VehicleService vehicleService,
                           AssistanceService assistanceService, AssistanceResourceService resourceService,
                           DepartmentDAO departmentDAO, ConcessionaireDAO concessionaireDAO,
                           RouteDAO routeDAO, TravelerDAO travelerDAO) {
        this.tollService = tollService;
        this.rateService = rateService;
        this.categoryService = categoryService;
        this.vehicleService = vehicleService;
        this.assistanceService = assistanceService;
        this.resourceService = resourceService;
        this.departmentDAO = departmentDAO;
        this.concessionaireDAO = concessionaireDAO;
        this.routeDAO = routeDAO;
        this.travelerDAO = travelerDAO;
    }

    // ------------------------------------------------------------------ Catálogo

    /**
     * Carga de una vez todo lo que la pantalla necesita para mostrar peajes.
     *
     * @return catálogo con peajes, departamentos, rutas, concesionarios y categorías
     * @throws DataAccessException si falla la base de datos
     */
    public Catalog loadCatalog() {
        Map<String, String> departments = new LinkedHashMap<>();
        for (Department d : departmentDAO.findAll()) {
            departments.put(d.getDepartmentId(), d.getName());
        }
        Map<String, String> routes = new LinkedHashMap<>();
        for (Route r : routeDAO.findAll()) {
            routes.put(r.getRouteId(), r.getName());
        }
        Map<Integer, Concessionaire> concessionaires = new LinkedHashMap<>();
        for (Concessionaire c : concessionaireDAO.findAll()) {
            concessionaires.put(c.getConcessionaireId(), c);
        }
        return new Catalog(tollService.listTolls(), departments, routes, concessionaires,
                categoryService.listAll());
    }

    /**
     * Tarifas de un peaje, una por categoría.
     *
     * @param tollId ID del peaje
     * @return tarifas ordenadas por categoría
     * @throws DataAccessException si falla la base de datos
     */
    public List<TollRate> rates(int tollId) {
        try {
            return rateService.findByTollId(tollId);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las tarifas del peaje " + tollId, e);
        }
    }

    /**
     * Precio de cada peaje para una categoría.
     *
     * @param categoryId categoría vehicular
     * @return mapa ID del peaje → precio (los peajes sin tarifa no aparecen)
     * @throws DataAccessException si falla la base de datos
     */
    public Map<Integer, BigDecimal> pricesFor(int categoryId) {
        try {
            Map<Integer, BigDecimal> prices = new HashMap<>();
            for (TollRate rate : rateService.findAll()) {
                if (rate.getCategoryId() == categoryId) {
                    prices.put(rate.getTollId(), rate.getPrice());
                }
            }
            return prices;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las tarifas", e);
        }
    }

    /**
     * Categoría del vehículo de una placa, para escoger de una vez su tarifa.
     *
     * @param plate placa, o null
     * @return ID de la categoría, o null si la placa no está registrada
     */
    public Integer categoryOfPlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return null;
        }
        return vehicleService.find(plate).map(Vehicle::getCategoryId).orElse(null);
    }

    // ------------------------------------------------------------------ Planificar viaje

    /**
     * Calcula los peajes de un viaje y lo que cuestan, de forma aproximada.
     *
     * Se usa cuando no se pudo obtener la ruta por carretera (ver {@link #planTripOnRoad}):
     * toma los peajes que están a menos de {@code corridorKm} de la línea recta entre origen
     * y destino, en el orden en que se encuentran. Si se indica una ruta, solo cuenta los
     * peajes de esa ruta.
     *
     * @param tolls      peajes entre los que se busca
     * @param prices     precio por peaje para la categoría del vehículo
     * @param originLat  latitud del origen
     * @param originLon  longitud del origen
     * @param destLat    latitud del destino
     * @param destLon    longitud del destino
     * @param corridorKm ancho del corredor en km
     * @param routeId    ruta a la que se limita, o null para todas
     * @return plan del viaje con peajes, total y distancia aproximada
     */
    public static TripPlan planTrip(List<Toll> tolls, Map<Integer, BigDecimal> prices,
                                    double originLat, double originLon, double destLat, double destLon,
                                    double corridorKm, String routeId) {
        // Proyección plana local: suficiente para distancias de cientos de km
        double midLat = Math.toRadians((originLat + destLat) / 2);
        double kmPerDegLat = 111.32;
        double kmPerDegLon = 111.32 * Math.cos(midLat);
        double bx = (destLon - originLon) * kmPerDegLon;
        double by = (destLat - originLat) * kmPerDegLat;
        double length2 = bx * bx + by * by;

        List<double[]> found = new ArrayList<>();
        List<Toll> onRoute = new ArrayList<>();
        for (Toll toll : tolls) {
            if (routeId != null && !routeId.equals(toll.getRouteId())) {
                continue;
            }
            double px = (toll.getLongitude() - originLon) * kmPerDegLon;
            double py = (toll.getLatitude() - originLat) * kmPerDegLat;
            double t = length2 == 0 ? 0 : (px * bx + py * by) / length2;
            if (t < 0 || t > 1) {
                continue;
            }
            double dx = px - t * bx;
            double dy = py - t * by;
            if (Math.sqrt(dx * dx + dy * dy) <= corridorKm) {
                found.add(new double[]{t, onRoute.size()});
                onRoute.add(toll);
            }
        }
        found.sort(Comparator.comparingDouble(a -> a[0]));

        List<TripStop> stops = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int withoutRate = 0;
        double distance = 0;
        double lastLat = originLat;
        double lastLon = originLon;
        for (double[] f : found) {
            Toll toll = onRoute.get((int) f[1]);
            BigDecimal price = prices.get(toll.getTollId());
            if (price == null) {
                withoutRate++;
            } else {
                total = total.add(price);
            }
            distance += distanceKm(lastLat, lastLon, toll.getLatitude(), toll.getLongitude());
            lastLat = toll.getLatitude();
            lastLon = toll.getLongitude();
            stops.add(new TripStop(toll, price, distance));
        }
        distance += distanceKm(lastLat, lastLon, destLat, destLon);
        return new TripPlan(stops, total, distance, withoutRate);
    }

    /**
     * Calcula los peajes de un viaje sobre la ruta real por carretera.
     *
     * Un peaje es "del camino" si está a menos de {@code maxMeters} de la vía. Los peajes se
     * ordenan por el punto de la vía donde quedan, y los km de cada uno son los recorridos
     * por carretera desde el origen hasta ese punto. Si se indica una ruta, solo cuenta los
     * peajes de esa ruta.
     *
     * @param tolls     peajes entre los que se busca
     * @param prices    precio por peaje para la categoría del vehículo
     * @param road      ruta por carretera (de {@link RouteService#findRoute})
     * @param maxMeters distancia máxima del peaje a la vía, en metros
     * @param routeId   ruta a la que se limita, o null para todas
     * @return plan del viaje con peajes, total y distancia por carretera
     */
    public static TripPlan planTripOnRoad(List<Toll> tolls, Map<Integer, BigDecimal> prices,
                                          RouteService.RoadRoute road, double maxMeters, String routeId) {
        List<double[]> points = road.getPoints();
        int n = points.size();
        if (n < 2) {
            throw new IllegalArgumentException("La ruta necesita al menos dos puntos");
        }

        // Km recorridos desde el origen hasta cada punto de la vía
        double[] kmAt = new double[n];
        for (int i = 1; i < n; i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            kmAt[i] = kmAt[i - 1] + distanceKm(a[0], a[1], b[0], b[1]);
        }
        // Se ajusta a la distancia que da el servicio de rutas para que el último km coincida
        double scale = kmAt[n - 1] > 0 ? road.getDistanceKm() / kmAt[n - 1] : 1;

        // Caja que rodea la ruta, para descartar rápido los peajes lejanos
        double marginDeg = maxMeters / 111_000.0 + 0.01;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        for (double[] p : points) {
            minLat = Math.min(minLat, p[0]);
            maxLat = Math.max(maxLat, p[0]);
            minLon = Math.min(minLon, p[1]);
            maxLon = Math.max(maxLon, p[1]);
        }

        List<TripStop> found = new ArrayList<>();
        for (Toll toll : tolls) {
            if (routeId != null && !routeId.equals(toll.getRouteId())) {
                continue;
            }
            double lat = toll.getLatitude();
            double lon = toll.getLongitude();
            if (lat < minLat - marginDeg || lat > maxLat + marginDeg
                    || lon < minLon - marginDeg || lon > maxLon + marginDeg) {
                continue;
            }
            double bestMeters = Double.MAX_VALUE;
            double bestKm = 0;
            for (int i = 0; i < n - 1; i++) {
                double[] a = points.get(i);
                double[] b = points.get(i + 1);
                double[] hit = closestOnSegment(lat, lon, a, b);
                if (hit[0] < bestMeters) {
                    bestMeters = hit[0];
                    bestKm = kmAt[i] + hit[1] * (kmAt[i + 1] - kmAt[i]);
                }
            }
            if (bestMeters <= maxMeters) {
                found.add(new TripStop(toll, prices.get(toll.getTollId()), bestKm * scale));
            }
        }
        found.sort(Comparator.comparingDouble(TripStop::getKmFromOrigin));

        BigDecimal total = BigDecimal.ZERO;
        int withoutRate = 0;
        for (TripStop stop : found) {
            if (stop.getPrice() == null) {
                withoutRate++;
            } else {
                total = total.add(stop.getPrice());
            }
        }
        return new TripPlan(found, total, road.getDistanceKm(), withoutRate, road);
    }

    /**
     * Punto del segmento a–b más cercano a (lat, lon), con una proyección plana local
     * (precisa para segmentos cortos como los de una vía).
     *
     * @return {distancia en metros, posición en el segmento de 0 (en a) a 1 (en b)}
     */
    private static double[] closestOnSegment(double lat, double lon, double[] a, double[] b) {
        double metersPerDegLat = 110_540.0;
        double metersPerDegLon = 111_320.0 * Math.cos(Math.toRadians(lat));
        // Coordenadas en metros con el peaje como origen
        double ax = (a[1] - lon) * metersPerDegLon;
        double ay = (a[0] - lat) * metersPerDegLat;
        double bx = (b[1] - lon) * metersPerDegLon;
        double by = (b[0] - lat) * metersPerDegLat;
        double dx = bx - ax;
        double dy = by - ay;
        double length2 = dx * dx + dy * dy;
        double t = length2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / length2));
        double px = ax + t * dx;
        double py = ay + t * dy;
        return new double[]{Math.sqrt(px * px + py * py), t};
    }

    /**
     * Distancia en línea recta entre dos puntos (fórmula de Haversine).
     *
     * @param lat1 latitud del primer punto
     * @param lon1 longitud del primer punto
     * @param lat2 latitud del segundo punto
     * @param lon2 longitud del segundo punto
     * @return distancia en km
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Peaje más cercano a un punto, para enviar allí la solicitud de asistencia.
     *
     * @param tolls     peajes entre los que se busca
     * @param latitude  latitud del punto
     * @param longitude longitud del punto
     * @return el peaje más cercano, o null si la lista está vacía
     */
    public static Toll nearestToll(List<Toll> tolls, double latitude, double longitude) {
        Toll best = null;
        double bestKm = Double.MAX_VALUE;
        for (Toll toll : tolls) {
            double km = distanceKm(latitude, longitude, toll.getLatitude(), toll.getLongitude());
            if (km < bestKm) {
                bestKm = km;
                best = toll;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ Asistencia

    /**
     * Registra una solicitud de asistencia. Le llega al Centro de asistencia del jefe de
     * estación del peaje indicado. Si la placa no está registrada, se registra el vehículo
     * con la categoría que indicó el viajero.
     *
     * @param traveler    viajero con cuenta, o null si es invitado
     * @param type        tipo de asistencia
     * @param tollId      peaje que la atiende (el más cercano)
     * @param plate       placa del vehículo
     * @param categoryId  categoría del vehículo (solo se usa si la placa es nueva)
     * @param phone       teléfono de contacto
     * @param description qué pasó
     * @param latitude    latitud del incidente, o null
     * @param longitude   longitud del incidente, o null
     * @return número de la solicitud
     * @throws ValidationException si algún dato no es válido
     * @throws DataAccessException si falla la base de datos
     */
    public int requestAssistance(Traveler traveler, AssistanceType type, int tollId, String plate,
                                 int categoryId, String phone, String description,
                                 Double latitude, Double longitude) {
        if (type == null) {
            throw new ValidationException("Escoja el tipo de asistencia");
        }
        if (tollId <= 0) {
            throw new ValidationException("Marque dónde está o escoja el peaje más cercano");
        }
        String cleanPlate = VehicleService.normalizePlate(plate);
        if (!VehicleService.isValidPlate(cleanPlate)) {
            throw new ValidationException("Placa no válida: use el formato ABC123 (carro) o ABC12D (moto)");
        }
        String cleanPhone = phone == null ? "" : phone.replaceAll("[\\s-]", "");
        if (!PHONE.matcher(cleanPhone).matches()) {
            throw new ValidationException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Cuéntenos brevemente qué pasó");
        }
        if (vehicleService.find(cleanPlate).isEmpty()) {
            vehicleService.register(new Vehicle(cleanPlate, null, null, null, null, categoryId));
        }
        Assistance assistance = new Assistance(type, tollId, cleanPlate, cleanPhone, description.trim());
        assistance.setIncidentLatitude(latitude);
        assistance.setIncidentLongitude(longitude);
        assistance.setTravelerId(traveler == null ? null : traveler.getTravelerId());
        return assistanceService.register(assistance);
    }

    /**
     * Solicitudes de un viajero con cuenta, más recientes primero.
     *
     * @param traveler viajero
     * @return sus solicitudes (vacía si es invitado)
     */
    public List<Assistance> myRequests(Traveler traveler) {
        if (traveler == null || traveler.getTravelerId() == null) {
            return Collections.emptyList();
        }
        return assistanceService.listByTraveler(traveler.getTravelerId());
    }

    /**
     * Detalle de una solicitud: sus cambios de estado y el recurso asignado.
     *
     * @param assistance solicitud
     * @return detalle listo para mostrar
     */
    public RequestDetail requestDetail(Assistance assistance) {
        List<AssistanceHistory> history = assistanceService.getHistory(assistance.getAssistanceId());
        AssistanceResource resource = assistance.getResourceId() == null ? null
                : resourceService.get(assistance.getResourceId());
        return new RequestDetail(assistance, history, resource);
    }

    // ------------------------------------------------------------------ Cuenta

    /**
     * Cambia el teléfono y la placa del viajero (cada viajero tiene una sola placa).
     *
     * @param traveler viajero con cuenta
     * @param phone    teléfono nuevo
     * @param plate    placa nueva, o vacía para quitarla
     * @return el viajero actualizado
     * @throws ValidationException si algún dato no es válido
     */
    public Traveler updateAccount(Traveler traveler, String phone, String plate) {
        String cleanPhone = phone == null ? "" : phone.replaceAll("[\\s-]", "");
        String cleanPlate = VehicleService.normalizePlate(plate).toUpperCase(Locale.ROOT);
        if (!PHONE.matcher(cleanPhone).matches()) {
            throw new ValidationException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        if (!cleanPlate.isEmpty() && !VehicleService.isValidPlate(cleanPlate)) {
            throw new ValidationException("Placa no válida: use el formato ABC123 (carro) o ABC12D (moto)");
        }
        String oldPhone = traveler.getPhone();
        String oldPlate = traveler.getPlate();
        traveler.setPhone(cleanPhone);
        traveler.setPlate(cleanPlate.isEmpty() ? null : cleanPlate);
        try {
            travelerDAO.update(traveler);
        } catch (RuntimeException e) {
            traveler.setPhone(oldPhone);
            traveler.setPlate(oldPlate);
            throw e;
        }
        return traveler;
    }

    // ------------------------------------------------------------------ Tipos de resultado

    /** Todo lo que la pantalla del viajero carga al abrir. */
    public static final class Catalog {
        private final List<Toll> tolls;
        private final Map<String, String> departments;
        private final Map<String, String> routes;
        private final Map<Integer, Concessionaire> concessionaires;
        private final List<VehicleCategory> categories;

        /**
         * @param tolls           peajes
         * @param departments     código → nombre del departamento
         * @param routes          código → nombre de la ruta
         * @param concessionaires ID → concesionario
         * @param categories      categorías vehiculares
         */
        public Catalog(List<Toll> tolls, Map<String, String> departments, Map<String, String> routes,
                       Map<Integer, Concessionaire> concessionaires, List<VehicleCategory> categories) {
            this.tolls = tolls;
            this.departments = departments;
            this.routes = routes;
            this.concessionaires = concessionaires;
            this.categories = categories;
        }

        public List<Toll> getTolls() {
            return tolls;
        }

        public Map<String, String> getDepartments() {
            return departments;
        }

        public Map<String, String> getRoutes() {
            return routes;
        }

        public Map<Integer, Concessionaire> getConcessionaires() {
            return concessionaires;
        }

        public List<VehicleCategory> getCategories() {
            return categories;
        }

        /**
         * @param departmentId código del departamento
         * @return nombre del departamento, o el código si no se conoce
         */
        public String departmentName(String departmentId) {
            return departments.getOrDefault(departmentId, departmentId == null ? "—" : departmentId);
        }

        /**
         * @param routeId código de la ruta
         * @return nombre de la ruta, o el código si no se conoce
         */
        public String routeName(String routeId) {
            return routes.getOrDefault(routeId, routeId == null ? "—" : routeId);
        }

        /**
         * @param concessionaireId ID del concesionario
         * @return nombre del concesionario, o "—"
         */
        public String concessionaireName(Integer concessionaireId) {
            Concessionaire c = concessionaireId == null ? null : concessionaires.get(concessionaireId);
            return c == null ? "—" : c.getName();
        }

        /**
         * @param tollId ID del peaje
         * @return el peaje, o null si no está
         */
        public Toll toll(int tollId) {
            for (Toll toll : tolls) {
                if (toll.getTollId() == tollId) {
                    return toll;
                }
            }
            return null;
        }
    }

    /** Un peaje del viaje con su precio y los km acumulados hasta él. */
    public static final class TripStop {
        private final Toll toll;
        private final BigDecimal price;
        private final double kmFromOrigin;

        public TripStop(Toll toll, BigDecimal price, double kmFromOrigin) {
            this.toll = toll;
            this.price = price;
            this.kmFromOrigin = kmFromOrigin;
        }

        public Toll getToll() {
            return toll;
        }

        /** @return precio para la categoría, o null si el peaje no tiene tarifa registrada */
        public BigDecimal getPrice() {
            return price;
        }

        public double getKmFromOrigin() {
            return kmFromOrigin;
        }
    }

    /** Resultado de planificar un viaje. */
    public static final class TripPlan {
        private final List<TripStop> stops;
        private final BigDecimal total;
        private final double distanceKm;
        private final int withoutRate;
        private final RouteService.RoadRoute road;

        /** Plan aproximado (línea recta), sin ruta por carretera. */
        public TripPlan(List<TripStop> stops, BigDecimal total, double distanceKm, int withoutRate) {
            this(stops, total, distanceKm, withoutRate, null);
        }

        /**
         * @param road ruta por carretera usada para el cálculo, o null si es aproximado
         */
        public TripPlan(List<TripStop> stops, BigDecimal total, double distanceKm, int withoutRate,
                        RouteService.RoadRoute road) {
            this.stops = stops;
            this.total = total;
            this.distanceKm = distanceKm;
            this.withoutRate = withoutRate;
            this.road = road;
        }

        /** @return true si se calculó sobre la ruta real por carretera */
        public boolean isOnRoad() {
            return road != null;
        }

        /** @return la ruta por carretera, o null si el plan es aproximado */
        public RouteService.RoadRoute getRoad() {
            return road;
        }

        public List<TripStop> getStops() {
            return stops;
        }

        public BigDecimal getTotal() {
            return total;
        }

        /**
         * @return distancia por carretera si {@link #isOnRoad()}; si no, distancia aproximada
         *         en línea recta pasando por cada peaje
         */
        public double getDistanceKm() {
            return distanceKm;
        }

        /** @return cuántos peajes del viaje no tienen tarifa para la categoría */
        public int getWithoutRate() {
            return withoutRate;
        }
    }

    /** Detalle de una solicitud de asistencia para el viajero. */
    public static final class RequestDetail {
        private final Assistance assistance;
        private final List<AssistanceHistory> history;
        private final AssistanceResource resource;

        public RequestDetail(Assistance assistance, List<AssistanceHistory> history, AssistanceResource resource) {
            this.assistance = assistance;
            this.history = history;
            this.resource = resource;
        }

        public Assistance getAssistance() {
            return assistance;
        }

        public List<AssistanceHistory> getHistory() {
            return history;
        }

        /** @return recurso asignado (grúa, ambulancia...), o null si aún no tiene */
        public AssistanceResource getResource() {
            return resource;
        }
    }
}
