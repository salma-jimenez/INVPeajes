package com.invpeajes.service;

import com.invpeajes.model.Caseta;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.CategoryRate;
import com.invpeajes.model.ChargeReceipt;
import com.invpeajes.model.Ticket;
import com.invpeajes.model.TollRate;
import com.invpeajes.model.Vehicle;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.persistence.CasetaDAO;
import com.invpeajes.persistence.CashierDAOImpl;
import com.invpeajes.persistence.DataAccessException;
import com.invpeajes.persistence.ICashierDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de la caja del peaje: apertura de turno, métodos de pago permitidos
 * por caseta, validación de placa y TAG, cálculo del cambio y registro del cobro.
 *
 * El precio siempre se toma de la base de datos, nunca de la interfaz, para que no se pueda
 * cobrar un valor distinto a la tarifa oficial.
 *
 * Reutiliza los servicios del equipo: {@link TollRateService} para las tarifas,
 * {@link VehicleCategoryService} para las categorías, {@link VehicleService} para registrar
 * la placa si es nueva y {@link TicketService} para guardar el tiquete.
 *
 * Errores que lanza:
 * <ul>
 *   <li>{@link IllegalArgumentException}: un dato no cumple las reglas.</li>
 *   <li>{@link IllegalStateException}: la operación no se permite en el estado actual.</li>
 *   <li>{@link NoSuchElementException}: el empleado o la tarifa no existe.</li>
 * </ul>
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class CashierService {

    public static final String ROLE_OPERATOR = "OPERADOR DE CASETA";
    public static final String TOLL_OPERATIONAL = "OPERATIVO";
    public static final String BOOTH_ACTIVE = "ACTIVA";

    public static final String CASH = "EFECTIVO";
    public static final String CARD = "TARJETA";
    public static final String TAG = "TAG_COLPASS";

    /**
     * Tiempo en el que un segundo cobro a la misma placa en la misma caseta se considera
     * un posible cobro repetido (doble clic o confusión del cajero). El viaje de vuelta pasa por
     * la caseta del sentido contrario y mucho después, así que no se ve afectado.
     */
    public static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(2);

    /** Placas de carro (ABC123), moto (ABC12D) y remolque (R12345). */
    private static final Pattern PLATE = Pattern.compile("[A-Z]{3}\\d{3}|[A-Z]{3}\\d{2}[A-Z]|[RS]\\d{5}");
    private static final Pattern TAG_CODE = Pattern.compile("[A-Z0-9]{6,30}");
    private static final Pattern OPERATOR_ID = Pattern.compile("\\d{6,15}");

    /** ORA-00001: otro cajero del mismo peaje tomó el mismo consecutivo al mismo tiempo. */
    private static final int ORA_UNIQUE_VIOLATION = 1;
    private static final int MAX_ATTEMPTS = 3;

    private final ICashierDAO cashierDAO;
    private final CasetaDAO boothDAO;
    private final TollRateService rateService;
    private final VehicleCategoryService categoryService;
    private final VehicleService vehicleService;
    private final TicketService ticketService;

    /**
     * Crea el servicio con las implementaciones de la base de datos.
     */
    public CashierService() {
        this(new CashierDAOImpl(), new CasetaDAO(), new TollRateService(),
                new VehicleCategoryService(), new VehicleService(), new TicketService());
    }

    /**
     * Crea el servicio con dependencias específicas (permite usar datos falsos en pruebas).
     * @param cashierDAO DAO de la caja (datos del operador y su peaje).
     * @param boothDAO DAO de casetas.
     * @param rateService Servicio de tarifas.
     * @param categoryService Servicio de categorías vehiculares.
     * @param vehicleService Servicio de vehículos.
     * @param ticketService Servicio de tiquetes.
     */
    public CashierService(ICashierDAO cashierDAO, CasetaDAO boothDAO, TollRateService rateService,
                          VehicleCategoryService categoryService, VehicleService vehicleService,
                          TicketService ticketService) {
        this.cashierDAO = cashierDAO;
        this.boothDAO = boothDAO;
        this.rateService = rateService;
        this.categoryService = categoryService;
        this.vehicleService = vehicleService;
        this.ticketService = ticketService;
    }

    /**
     * Valida la cédula de un operador y abre su sesión (todavía sin caseta).
     * @param operatorId Cédula del operador.
     * @return Sesión del operador con su peaje.
     * @throws IllegalArgumentException Si la cédula no tiene un formato válido.
     * @throws NoSuchElementException Si no existe un empleado con esa cédula.
     * @throws IllegalStateException Si no es operador de caseta o su peaje no está operativo.
     */
    public CashierSession login(String operatorId) {
        String id = operatorId == null ? "" : operatorId.trim();
        if (!OPERATOR_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("La cédula debe tener entre 6 y 15 dígitos");
        }
        CashierSession session = cashierDAO.findOperator(id).orElseThrow(() ->
                new NoSuchElementException("No existe un empleado con la cédula " + id
                        + " o no tiene un peaje asignado"));
        if (!ROLE_OPERATOR.equals(session.getRole())) {
            throw new IllegalStateException(session.getOperatorName() + " es " + session.getRole()
                    + "; solo un operador de caseta puede abrir la caja");
        }
        if (!TOLL_OPERATIONAL.equals(session.getTollStatus())) {
            throw new IllegalStateException("El peaje " + session.getTollName() + " está "
                    + session.getTollStatus() + "; no se puede abrir turno");
        }
        return session;
    }

    /**
     * Lista las casetas activas del peaje del operador, para elegir la del turno.
     * @param session Sesión del operador.
     * @return Casetas en estado ACTIVA, ordenadas por número.
     */
    public List<Caseta> availableBooths(CashierSession session) {
        return boothDAO.findByToll(session.getTollId()).stream()
                .filter(b -> BOOTH_ACTIVE.equals(b.getEstado()))
                .collect(Collectors.toList());
    }

    /**
     * Abre el turno en una caseta.
     * @param session Sesión del operador.
     * @param booth Caseta elegida.
     * @return Sesión con la caseta asignada.
     * @throws IllegalArgumentException Si la caseta es de otro peaje o no está activa.
     */
    public CashierSession openShift(CashierSession session, Caseta booth) {
        if (booth == null) {
            throw new IllegalArgumentException("Seleccione una caseta");
        }
        if (booth.getIdPeaje() != session.getTollId()) {
            throw new IllegalArgumentException("La caseta " + booth.getNumeroCaseta()
                    + " no pertenece al peaje " + session.getTollName());
        }
        if (!BOOTH_ACTIVE.equals(booth.getEstado())) {
            throw new IllegalArgumentException("La caseta " + booth.getNumeroCaseta() + " está "
                    + booth.getEstado());
        }
        return session.withBooth(booth);
    }

    /**
     * Lista los precios del peaje del turno.
     * @param session Sesión del operador.
     * @return Precios por categoría (solo las categorías con tarifa), ordenados por categoría.
     * @throws DataAccessException Si falla la consulta.
     */
    public List<CategoryRate> rates(CashierSession session) {
        List<TollRate> tollRates;
        try {
            tollRates = rateService.findByTollId(session.getTollId());
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar las tarifas del peaje "
                    + session.getTollName(), e);
        }
        Map<Integer, VehicleCategory> categories = categoryService.listAll().stream()
                .collect(Collectors.toMap(VehicleCategory::getCategoryId, Function.identity()));
        return tollRates.stream()
                .filter(r -> r.getPrice() != null && categories.containsKey(r.getCategoryId()))
                .map(r -> {
                    VehicleCategory c = categories.get(r.getCategoryId());
                    return new CategoryRate(r.getCategoryId(), c.getCategoryCode(), c.getDescription(), r.getPrice());
                })
                .collect(Collectors.toList());
    }

    /**
     * Métodos de pago que acepta la caseta según su tipo.
     * EFECTIVO acepta efectivo y tarjeta; COLPASS solo TAG; MIXTO acepta los tres.
     * @param booth Caseta del turno.
     * @return Métodos de pago permitidos.
     */
    public List<String> allowedMethods(Caseta booth) {
        if (booth == null) {
            return Collections.emptyList();
        }
        switch (booth.getTipoPago()) {
            case "EFECTIVO":
                return Arrays.asList(CASH, CARD);
            case "COLPASS":
                return Collections.singletonList(TAG);
            default:
                return Arrays.asList(CASH, CARD, TAG);
        }
    }

    /**
     * Pasa la placa a mayúsculas sin espacios ni guiones y valida su formato.
     * @param plate Placa escrita por el cajero.
     * @return Placa normalizada (ej. "ABC123").
     * @throws IllegalArgumentException Si la placa no es válida.
     */
    public String normalizePlate(String plate) {
        String value = plate == null ? "" : plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!PLATE.matcher(value).matches()) {
            throw new IllegalArgumentException("Placa no válida: use el formato ABC123 (carro) o ABC12D (moto)");
        }
        return value;
    }

    /**
     * Calcula el cambio a devolver.
     * @param total Valor a pagar.
     * @param received Valor que entregó el usuario.
     * @return Cambio (recibido menos total).
     * @throws IllegalArgumentException Si no se indicó el recibido o no alcanza para pagar.
     */
    public BigDecimal calculateChange(BigDecimal total, BigDecimal received) {
        if (received == null) {
            throw new IllegalArgumentException("Ingrese el valor recibido");
        }
        BigDecimal change = received.subtract(total);
        if (change.signum() < 0) {
            throw new IllegalArgumentException("El valor recibido no alcanza: faltan $"
                    + String.format(Locale.ROOT, "%,d", change.negate().longValue()).replace(',', '.'));
        }
        return change;
    }

    /**
     * Busca si la misma placa ya se cobró en la misma caseta hace menos de
     * {@link #DUPLICATE_WINDOW}. No bloquea el cobro: la interfaz solo pide confirmación.
     *
     * @param shiftReceipts Cobros del turno.
     * @param plate Placa escrita por el cajero.
     * @param booth Caseta del turno.
     * @param now Momento actual.
     * @return El cobro anterior si es un posible repetido, o vacío.
     */
    public Optional<ChargeReceipt> findRecentDuplicate(List<ChargeReceipt> shiftReceipts, String plate,
                                                       Caseta booth, LocalDateTime now) {
        String normalized;
        try {
            normalized = normalizePlate(plate);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        LocalDateTime limit = now.minus(DUPLICATE_WINDOW);
        return shiftReceipts.stream()
                .filter(r -> r.getPlate().equals(normalized))
                .filter(r -> r.getSession().getBooth().getIdCaseta().equals(booth.getIdCaseta()))
                .filter(r -> r.getDateTime().isAfter(limit))
                .findFirst();
    }

    /**
     * Valida y registra un cobro.
     *
     * @param session Turno abierto (con caseta).
     * @param categoryId Categoría del vehículo (1 a 7).
     * @param plate Placa escrita por el cajero.
     * @param paymentMethod EFECTIVO, TARJETA o TAG_COLPASS.
     * @param tagCode Código del TAG (obligatorio solo con TAG_COLPASS).
     * @param received Valor recibido (obligatorio solo en efectivo).
     * @return Cobro guardado, con número de tiquete, listo para imprimir.
     * @throws IllegalArgumentException Si algún dato no es válido.
     * @throws IllegalStateException Si no hay caseta abierta.
     * @throws NoSuchElementException Si el peaje no tiene tarifa para esa categoría.
     */
    public ChargeReceipt charge(CashierSession session, int categoryId, String plate,
                                String paymentMethod, String tagCode, BigDecimal received) {
        if (session == null || session.getBooth() == null) {
            throw new IllegalStateException("Abra un turno en una caseta antes de cobrar");
        }
        CategoryRate rate = rates(session).stream()
                .filter(r -> r.getCategoryId() == categoryId)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("El peaje " + session.getTollName()
                        + " no tiene tarifa para la categoría seleccionada"));

        if (!allowedMethods(session.getBooth()).contains(paymentMethod)) {
            throw new IllegalArgumentException("La caseta " + session.getBooth().getNumeroCaseta()
                    + " (" + session.getBooth().getTipoPago() + ") no acepta pago con " + paymentMethod);
        }
        String normalizedPlate = normalizePlate(plate);

        String tag = null;
        if (TAG.equals(paymentMethod)) {
            tag = tagCode == null ? "" : tagCode.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
            if (!TAG_CODE.matcher(tag).matches()) {
                throw new IllegalArgumentException("Código TAG no válido: entre 6 y 30 letras o números");
            }
        }

        BigDecimal total = rate.getPrice();
        BigDecimal paid = CASH.equals(paymentMethod) ? received : total;
        BigDecimal change = CASH.equals(paymentMethod) ? calculateChange(total, received) : BigDecimal.ZERO;

        ChargeReceipt draft = new ChargeReceipt(session, rate, normalizedPlate, paymentMethod, tag, paid, change);
        ensureVehicle(normalizedPlate, categoryId);
        Ticket ticket = saveTicket(session, categoryId, normalizedPlate, paymentMethod, tag);
        return draft.withTicketData(ticket.getId(), ticket.getPrefix(), ticket.getSequenceNumber(),
                ticket.getPaymentDate());
    }

    /**
     * TIQUETE.PLACA tiene llave foránea a VEHICULOS, y los carros que pasan por el peaje
     * no se conocen de antes: si la placa no existe se registra solo con su categoría.
     */
    private void ensureVehicle(String plate, int categoryId) {
        if (vehicleService.find(plate).isPresent()) {
            return;
        }
        try {
            vehicleService.register(new Vehicle(plate, null, null, null, null, categoryId));
        } catch (ValidationException e) {
            // Otra caseta la registró al mismo tiempo: ya existe, que es lo que se necesita.
            if (!vehicleService.find(plate).isPresent()) {
                throw e;
            }
        }
    }

    /** Guarda el tiquete; si dos cajeros toman el mismo consecutivo, reintenta con el siguiente. */
    private Ticket saveTicket(CashierSession session, int categoryId, String plate, String method, String tag) {
        for (int attempt = 1; ; attempt++) {
            try {
                return ticketService.registerPayment(session.getTollId(), session.getBooth().getIdCaseta(),
                        plate, categoryId, method, tag, BigDecimal.ZERO, session.getOperatorId());
            } catch (SQLException e) {
                if (e.getErrorCode() != ORA_UNIQUE_VIOLATION || attempt >= MAX_ATTEMPTS) {
                    throw new DataAccessException("No se pudo registrar el cobro", e);
                }
            }
        }
    }
}
