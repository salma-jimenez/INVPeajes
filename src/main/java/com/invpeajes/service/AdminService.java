package com.invpeajes.service;

import com.invpeajes.model.AdminSummary;
import com.invpeajes.model.AdminSummary.Pending;
import com.invpeajes.model.ElectronicToll;
import com.invpeajes.model.ManualToll;
import com.invpeajes.model.Toll;
import com.invpeajes.model.TollBooth;
import com.invpeajes.model.TollRate;
import com.invpeajes.model.TollUser;
import com.invpeajes.persistence.AdminDAO;
import com.invpeajes.persistence.TollBoothDAO;
import com.invpeajes.service.TravelerService.Catalog;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Reglas del panel de Administración. Cada administrador pertenece a una entidad (ANI, INVÍAS,
 * concesión territorial...) y solo puede ver y cambiar lo de los peajes de su entidad:
 *
 * <ul>
 *   <li>los datos del peaje, sus tarifas por categoría y sus casetas (agregar, quitar y forma de
 *       pago; el estado de cada caseta lo cambia el jefe de estación);</li>
 *   <li>los jefes de estación de sus peajes (los operadores los maneja cada jefe);</li>
 *   <li>sus propios datos.</li>
 * </ul>
 *
 * Un peaje con cobros o empleados no se borra: se pasa a NO_OPERATIVO para conservar su historial.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AdminService {

    /** Distancia por debajo de la cual dos peajes de una misma ruta se marcan como "muy cercanos". */
    public static final double CLOSE_KM = 40;
    /** Número de categorías vehiculares que debe tener un peaje para quedar con todas sus tarifas. */
    public static final int CATEGORY_COUNT = 7;

    private static final String NOT_OPERATIONAL = "NO_OPERATIVO";
    private static final String NO_RATES = "SIN_TARIFA";
    private static final String DAMAGED = "SINIESTRADO";

    private final TollService tollService;
    private final TollRateService rateService;
    private final TravelerService catalogService;
    private final AdminDAO adminDAO;
    private final StaffService staffService;
    private final TollBoothDAO boothDAO;

    /** Dos peajes de una misma ruta que están muy cerca. */
    public static final class ClosePair {
        private final Toll first;
        private final Toll second;
        private final double km;

        /**
         * @param first Un peaje.
         * @param second El otro.
         * @param km Distancia en línea recta entre los dos.
         */
        public ClosePair(Toll first, Toll second, double km) {
            this.first = first;
            this.second = second;
            this.km = km;
        }

        /** @return Un peaje. */
        public Toll getFirst() {
            return first;
        }

        /** @return El otro peaje. */
        public Toll getSecond() {
            return second;
        }

        /** @return Distancia en kilómetros. */
        public double getKm() {
            return km;
        }
    }

    /** Lo que tiene registrado un peaje y que impide borrarlo. */
    public static final class TollUsage {
        private final long tickets;
        private final long users;
        private final int booths;

        /**
         * @param tickets Tiquetes cobrados.
         * @param users Empleados asignados.
         * @param booths Casetas.
         */
        public TollUsage(long tickets, long users, int booths) {
            this.tickets = tickets;
            this.users = users;
            this.booths = booths;
        }

        /** @return Tiquetes cobrados. */
        public long getTickets() {
            return tickets;
        }

        /** @return Empleados asignados. */
        public long getUsers() {
            return users;
        }

        /** @return Casetas. */
        public int getBooths() {
            return booths;
        }

        /** @return true si se puede borrar (sin cobros ni empleados). */
        public boolean canDelete() {
            return tickets == 0 && users == 0;
        }
    }

    /** Crea el servicio con la base de datos. */
    public AdminService() {
        this(new TollService(), new TollRateService(), new TravelerService(), new AdminDAO(),
                new StaffService(), new TollBoothDAO());
    }

    /**
     * @param tollService Peajes y casetas.
     * @param rateService Tarifas.
     * @param catalogService De aquí sale el catálogo (peajes, departamentos, rutas, concesionarios,
     *                       categorías), el mismo que usa el viajero.
     * @param adminDAO Consultas del administrador.
     * @param staffService Empleados.
     * @param boothDAO Casetas.
     */
    public AdminService(TollService tollService, TollRateService rateService, TravelerService catalogService,
                        AdminDAO adminDAO, StaffService staffService, TollBoothDAO boothDAO) {
        this.tollService = tollService;
        this.rateService = rateService;
        this.catalogService = catalogService;
        this.adminDAO = adminDAO;
        this.staffService = staffService;
        this.boothDAO = boothDAO;
    }

    /** @return Servicio de empleados (para cambiar la contraseña propia). */
    public StaffService staff() {
        return staffService;
    }

    // ------------------------------------------------------------------ Administrador

    /**
     * Carga al administrador con su entidad.
     * @param idNumber Cédula del administrador que inició sesión.
     * @return El administrador.
     * @throws ValidationException Si no es administrador o no tiene entidad.
     */
    public TollUser admin(String idNumber) {
        TollUser admin = staffService.find(idNumber);
        if (!AuthService.ROLE_ADMIN.equals(admin.getPosition()) || admin.getEntity() == null) {
            throw new ValidationException("Tu usuario no tiene una entidad asignada. "
                    + "Pide que la registren en la base de datos.");
        }
        return admin;
    }

    /**
     * Guarda los datos propios del administrador (nombres, teléfono y correo).
     * @param admin Administrador con los datos nuevos.
     */
    public void updateOwnAccount(TollUser admin) {
        admin.setTollId(null);
        staffService.update(admin);
    }

    // ------------------------------------------------------------------ Catálogo y resumen

    /**
     * @return Todos los peajes con los nombres de departamentos, rutas, concesionarios y categorías.
     */
    public Catalog catalog() {
        return catalogService.loadCatalog();
    }

    /**
     * @param catalog Catálogo.
     * @param level Entidad.
     * @return Los peajes de la entidad, ordenados por nombre.
     */
    public static List<Toll> tollsOf(Catalog catalog, String level) {
        return catalog.getTolls().stream().filter(t -> level.equals(t.getLevel()))
                .sorted(Comparator.comparing(Toll::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Resumen del inicio del administrador, con los pendientes de sus peajes.
     * @param level Entidad.
     * @param mine Peajes de la entidad.
     * @return Resumen.
     */
    public AdminSummary summary(String level, List<Toll> mine) {
        LocalDateTime now = LocalDateTime.now();
        AdminSummary summary = adminDAO.summary(level, now);
        summary.setOperatingTolls((int) mine.stream()
                .filter(t -> TollService.STATUS_OPERATIONAL.equals(t.getOperationalStatus())).count());
        AdminDAO.PendingData data = adminDAO.pending(level, now.toLocalDate().withDayOfYear(1));
        List<Pending> pendings = new ArrayList<>();
        if (!data.getWithoutRates().isEmpty()) {
            pendings.add(new Pending(Pending.Kind.NO_RATES, plural(data.getWithoutRates().size(),
                    "peaje sin tarifa", "peajes sin tarifa"),
                    names(data.getWithoutRates()) + (data.getWithoutRates().size() == 1 ? " no puede" : " no pueden")
                            + " cobrar. Carga sus tarifas."));
        }
        List<String> damaged = mine.stream().filter(t -> DAMAGED.equals(t.getOperationalStatus()))
                .map(Toll::getName).collect(Collectors.toList());
        if (!damaged.isEmpty()) {
            pendings.add(new Pending(Pending.Kind.DAMAGED, plural(damaged.size(), "peaje siniestrado",
                    "peajes siniestrados"), names(damaged) + ": no cobran hasta que se reparen."));
        }
        if (!data.getWithoutManager().isEmpty()) {
            pendings.add(new Pending(Pending.Kind.NO_MANAGER, plural(data.getWithoutManager().size(),
                    "peaje sin jefe de estación", "peajes sin jefe de estación"),
                    names(data.getWithoutManager()) + ". Nadie atiende sus solicitudes de asistencia."));
        }
        List<ClosePair> pairs = closePairs(mine, CLOSE_KM);
        if (!pairs.isEmpty()) {
            ClosePair first = pairs.get(0);
            pendings.add(new Pending(Pending.Kind.CLOSE_TOLLS, plural(pairs.size(), "par de peajes muy cercanos",
                    "pares de peajes muy cercanos"), String.format(java.util.Locale.forLanguageTag("es-CO"), "El más cercano: %s ↔ %s, %.1f km.",
                    first.getFirst().getName(), first.getSecond().getName(), first.getKm())));
        }
        if (data.getWithOldRates() > 0) {
            pendings.add(new Pending(Pending.Kind.OLD_RATES, "Tarifas por actualizar",
                    plural(data.getWithOldRates(), "peaje tiene", "peajes tienen")
                            + " tarifas de un año anterior a " + now.getYear() + "."));
        }
        summary.setPendings(pendings);
        return summary;
    }

    /**
     * Pares de peajes de una misma ruta a menos de {@code maxKm} kilómetros, del más cercano al
     * más lejano. Usa la distancia en línea recta entre sus coordenadas.
     * @param tolls Peajes a revisar.
     * @param maxKm Distancia máxima.
     * @return Pares.
     */
    public static List<ClosePair> closePairs(List<Toll> tolls, double maxKm) {
        Map<String, List<Toll>> byRoute = new HashMap<>();
        for (Toll t : tolls) {
            if (t.getRouteId() != null && !NOT_OPERATIONAL.equals(t.getOperationalStatus())) {
                byRoute.computeIfAbsent(t.getRouteId(), k -> new ArrayList<>()).add(t);
            }
        }
        List<ClosePair> pairs = new ArrayList<>();
        for (List<Toll> route : byRoute.values()) {
            for (int i = 0; i < route.size(); i++) {
                for (int j = i + 1; j < route.size(); j++) {
                    Toll a = route.get(i);
                    Toll b = route.get(j);
                    double km = TravelerService.distanceKm(a.getLatitude(), a.getLongitude(),
                            b.getLatitude(), b.getLongitude());
                    if (km < maxKm && !sameName(a, b)) {
                        pairs.add(new ClosePair(a, b, km));
                    }
                }
            }
        }
        pairs.sort(Comparator.comparingDouble(ClosePair::getKm));
        return pairs;
    }

    /**
     * El peaje más cercano a un punto sobre la misma ruta (para avisar al registrar uno nuevo).
     * @param tolls Peajes.
     * @param routeId Ruta, o null para cualquiera.
     * @param latitude Latitud del punto.
     * @param longitude Longitud del punto.
     * @param exceptId Peaje a ignorar (el que se edita), o 0.
     * @return El par (peaje, distancia), o null si no hay ninguno a menos de {@link #CLOSE_KM}.
     */
    public static ClosePair nearestOnRoute(List<Toll> tolls, String routeId, double latitude, double longitude,
                                           int exceptId) {
        ClosePair best = null;
        for (Toll t : tolls) {
            if (t.getTollId() == exceptId || (routeId != null && !routeId.equals(t.getRouteId()))) {
                continue;
            }
            double km = TravelerService.distanceKm(latitude, longitude, t.getLatitude(), t.getLongitude());
            if (km < CLOSE_KM && (best == null || km < best.getKm())) {
                best = new ClosePair(t, t, km);
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ Peajes

    /**
     * Registra un peaje nuevo de la entidad. Queda "Sin tarifa" hasta que se le carguen sus
     * tarifas.
     * @param level Entidad del administrador.
     * @param data Datos del peaje (el ID, el nivel y el estado se ponen aquí).
     * @param acceptsColpass true si tiene carril de TAG.
     * @return El peaje guardado, con su ID.
     */
    public Toll registerToll(String level, Toll data, boolean acceptsColpass) {
        int id = adminDAO.nextTollId();
        Toll toll = copy(data, id, level, NO_RATES, acceptsColpass);
        tollService.registerToll(toll);
        return toll;
    }

    /**
     * Guarda los cambios de un peaje de la entidad. El nivel no cambia.
     * @param level Entidad del administrador.
     * @param data Datos nuevos (con su ID).
     * @param acceptsColpass true si tiene carril de TAG.
     * @return El peaje guardado.
     */
    public Toll updateToll(String level, Toll data, boolean acceptsColpass) {
        Toll current = ownToll(level, data.getTollId());
        // Datos que el formulario no muestra: se conservan los que ya tenía.
        if (data.getAniId() == null) {
            data.setAniId(current.getAniId());
        }
        if (data.getSectorId() == null) {
            data.setSectorId(current.getSectorId());
        }
        if (data.getInfoSheetUrl() == null) {
            data.setInfoSheetUrl(current.getInfoSheetUrl());
        }
        Toll toll = copy(data, current.getTollId(), level, data.getOperationalStatus(), acceptsColpass);
        if (TollService.STATUS_OPERATIONAL.equals(toll.getOperationalStatus()) && rates(toll.getTollId()).isEmpty()) {
            throw new ValidationException("No se puede poner Operativo: el peaje todavía no tiene tarifas");
        }
        if (!toll.getOperationalStatus().equals(current.getOperationalStatus())) {
            tollService.updateToll(toll);
            tollService.changeTollStatus(toll.getTollId(), toll.getOperationalStatus());
        } else {
            tollService.updateToll(toll);
        }
        return toll;
    }

    /**
     * @param tollId ID del peaje.
     * @return Cobros, empleados y casetas del peaje (para decidir si se puede borrar).
     */
    public TollUsage usage(int tollId) {
        return new TollUsage(adminDAO.ticketsOfToll(tollId), adminDAO.usersOfToll(tollId),
                boothDAO.findByToll(tollId).size());
    }

    /**
     * Borra un peaje sin cobros ni empleados, con sus tarifas y casetas.
     * @param level Entidad del administrador.
     * @param tollId ID del peaje.
     * @throws ValidationException Si tiene cobros o empleados (debe pasarse a No operativo).
     */
    public void deleteToll(String level, int tollId) {
        ownToll(level, tollId);
        TollUsage usage = usage(tollId);
        if (!usage.canDelete()) {
            throw new ValidationException("El peaje tiene cobros o empleados registrados: pásalo a No operativo");
        }
        adminDAO.deleteToll(tollId);
    }

    /**
     * Cambia el estado de un peaje (ej. a NO_OPERATIVO). Si deja de estar operativo, sus casetas
     * activas se desactivan.
     * @param level Entidad del administrador.
     * @param tollId ID del peaje.
     * @param status Estado nuevo.
     */
    public void changeTollStatus(String level, int tollId, String status) {
        ownToll(level, tollId);
        if (TollService.STATUS_OPERATIONAL.equals(status) && rates(tollId).isEmpty()) {
            throw new ValidationException("No se puede poner Operativo: el peaje todavía no tiene tarifas");
        }
        tollService.changeTollStatus(tollId, status);
    }

    // ------------------------------------------------------------------ Tarifas

    /**
     * @param tollId ID del peaje.
     * @return Tarifas del peaje, por categoría.
     */
    public List<TollRate> rates(int tollId) {
        try {
            return rateService.findByTollId(tollId);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las tarifas: " + e.getMessage(), e);
        }
    }

    /**
     * Guarda las tarifas de un peaje. Las categorías que ya tenían precio se actualizan y las
     * nuevas se registran. Si el peaje estaba "Sin tarifa" y quedan las 7 categorías, pasa a
     * "Operativo". La fuente es la entidad (solo ANI o INVIAS; las demás quedan vacías).
     * @param level Entidad del administrador.
     * @param tollId ID del peaje.
     * @param prices Precio por categoría (solo las que cambian o se agregan).
     * @param effectiveDate Desde cuándo rigen.
     * @return true si el peaje pasó a Operativo.
     */
    public boolean saveRates(String level, int tollId, Map<Integer, BigDecimal> prices, LocalDate effectiveDate) {
        Toll toll = ownToll(level, tollId);
        if (effectiveDate == null) {
            throw new ValidationException("Escribe desde qué fecha rigen las tarifas");
        }
        for (BigDecimal price : prices.values()) {
            if (price == null || price.signum() <= 0) {
                throw new ValidationException("Las tarifas deben ser mayores que cero");
            }
        }
        String source = "ANI".equals(level) || "INVIAS".equals(level) ? level : null;
        try {
            Map<Integer, TollRate> current = new HashMap<>();
            for (TollRate r : rateService.findByTollId(tollId)) {
                current.put(r.getCategoryId(), r);
            }
            for (Map.Entry<Integer, BigDecimal> e : prices.entrySet()) {
                TollRate rate = new TollRate(tollId, e.getKey(), e.getValue(), effectiveDate, source);
                if (current.containsKey(e.getKey())) {
                    rateService.update(rate);
                } else {
                    rateService.register(rate);
                }
                current.put(e.getKey(), rate);
            }
            if (NO_RATES.equals(toll.getOperationalStatus()) && current.size() >= CATEGORY_COUNT) {
                tollService.changeTollStatus(tollId, TollService.STATUS_OPERATIONAL);
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron guardar las tarifas: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ Casetas

    /**
     * @param tollId ID del peaje.
     * @return Casetas del peaje, por número.
     */
    public List<TollBooth> booths(int tollId) {
        List<TollBooth> booths = new ArrayList<>(tollService.listBooths(tollId));
        booths.sort(Comparator.comparingInt(TollBooth::getBoothNumber));
        return booths;
    }

    /**
     * Agrega una caseta con el siguiente número libre. Empieza activa si el peaje está operativo;
     * si no, inactiva.
     * @param level Entidad del administrador.
     * @param tollId ID del peaje.
     * @param direction Sentido (ej. ORIENTE_OCCIDENTE), o null.
     * @param paymentType EFECTIVO, MIXTO o COLPASS.
     * @return La caseta agregada.
     */
    public TollBooth addBooth(String level, int tollId, String direction, String paymentType) {
        Toll toll = ownToll(level, tollId);
        int number = booths(tollId).stream().mapToInt(TollBooth::getBoothNumber).max().orElse(0) + 1;
        String status = TollService.STATUS_OPERATIONAL.equals(toll.getOperationalStatus())
                ? TollService.BOOTH_ACTIVE : TollService.BOOTH_INACTIVE;
        TollBooth booth = new TollBooth(null, number, paymentType, direction, status, tollId);
        tollService.addBooth(booth);
        return booth;
    }

    /**
     * Cambia el sentido y la forma de pago de una caseta (el estado lo maneja el jefe de estación).
     * @param level Entidad del administrador.
     * @param booth Caseta con los datos nuevos.
     */
    public void updateBooth(String level, TollBooth booth) {
        ownToll(level, booth.getTollId());
        TollBooth current = boothDAO.findById(booth.getBoothId())
                .orElseThrow(() -> new NoSuchElementException("No existe la caseta " + booth.getBoothId()));
        booth.setStatus(current.getStatus());
        tollService.updateBooth(booth);
    }

    /**
     * Borra una caseta sin cobros.
     * @param level Entidad del administrador.
     * @param booth Caseta.
     * @throws ValidationException Si ya tiene cobros.
     */
    public void deleteBooth(String level, TollBooth booth) {
        ownToll(level, booth.getTollId());
        long tickets = adminDAO.ticketsOfBooth(booth.getBoothId());
        if (tickets > 0) {
            throw new ValidationException("La caseta " + booth.getBoothNumber() + " tiene " + tickets
                    + " cobros registrados y no se puede eliminar. El jefe de estación puede desactivarla.");
        }
        tollService.deleteBooth(booth.getBoothId());
    }

    // ------------------------------------------------------------------ Jefes de estación

    /**
     * @param level Entidad.
     * @return Jefes y operadores de los peajes de la entidad.
     */
    public List<TollUser> staffOf(String level) {
        return staffService.staffOfLevel(level);
    }

    /**
     * Registra un jefe de estación en un peaje de la entidad.
     * @param level Entidad del administrador.
     * @param user Datos del jefe.
     * @param tollId Peaje.
     * @return El jefe registrado.
     */
    public TollUser registerManager(String level, TollUser user, int tollId) {
        ownToll(level, tollId);
        return staffService.register(user, AuthService.ROLE_MANAGER, tollId);
    }

    /**
     * Guarda los datos de un jefe de estación de la entidad (puede cambiarlo de peaje, dentro de
     * la entidad).
     * @param level Entidad del administrador.
     * @param user Jefe con los datos nuevos.
     */
    public void updateManager(String level, TollUser user) {
        ownManager(level, user.getIdNumber());
        if (user.getTollId() == null) {
            throw new ValidationException("Escoge el peaje del jefe de estación");
        }
        ownToll(level, user.getTollId());
        staffService.update(user);
    }

    /**
     * Activa o desactiva a un jefe de estación de la entidad.
     * @param level Entidad del administrador.
     * @param idNumber Cédula del jefe.
     * @param active true para activar.
     */
    public void setManagerActive(String level, String idNumber, boolean active) {
        ownManager(level, idNumber);
        staffService.setActive(idNumber, active);
    }

    /**
     * Vuelve a poner la cédula como contraseña de un jefe de estación de la entidad.
     * @param level Entidad del administrador.
     * @param idNumber Cédula del jefe.
     */
    public void resetManagerPassword(String level, String idNumber) {
        ownManager(level, idNumber);
        staffService.resetPassword(idNumber);
    }

    // ------------------------------------------------------------------ Apoyo

    private Toll ownToll(String level, int tollId) {
        Toll toll = tollService.findToll(tollId);
        if (!level.equals(toll.getLevel())) {
            throw new ValidationException("El peaje " + toll.getName() + " no es de tu entidad");
        }
        return toll;
    }

    private TollUser ownManager(String level, String idNumber) {
        TollUser user = staffService.find(idNumber);
        if (!AuthService.ROLE_MANAGER.equals(user.getPosition())) {
            throw new ValidationException("Solo puedes editar jefes de estación; los operadores los maneja "
                    + "el jefe de su peaje");
        }
        if (user.getTollId() == null) {
            throw new ValidationException("El jefe de estación no tiene peaje asignado");
        }
        ownToll(level, user.getTollId());
        return user;
    }

    private static Toll copy(Toll d, int id, String level, String status, boolean colpass) {
        Date date = new Date();
        if (colpass) {
            return new ElectronicToll(id, d.getCode(), d.getAniId(), d.getName(), d.getKilometer(), d.getLatitude(),
                    d.getLongitude(), d.getLocationDescription(), d.getDirection(), status, level, date, d.getPhone(),
                    d.getTowPhone(), d.getInfoSheetUrl(), d.getRouteId(), d.getConcessionaireId(), d.getSectorId(),
                    d.getDepartmentId());
        }
        return new ManualToll(id, d.getCode(), d.getAniId(), d.getName(), d.getKilometer(), d.getLatitude(),
                d.getLongitude(), d.getLocationDescription(), d.getDirection(), status, level, date, d.getPhone(),
                d.getTowPhone(), d.getInfoSheetUrl(), d.getRouteId(), d.getConcessionaireId(), d.getSectorId(),
                d.getDepartmentId());
    }

    private static boolean sameName(Toll a, Toll b) {
        return normalize(a.getName()).equals(normalize(b.getName()));
    }

    private static String normalize(String name) {
        return java.text.Normalizer.normalize(name == null ? "" : name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).trim();
    }

    private static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    private static String names(List<String> names) {
        if (names.size() <= 3) {
            return joinNames(names);
        }
        return String.join(", ", names.subList(0, 3)) + " y " + (names.size() - 3) + " más";
    }

    private static String joinNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " y " + names.get(names.size() - 1);
    }
}
