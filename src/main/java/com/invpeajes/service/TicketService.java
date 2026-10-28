package com.invpeajes.service;
 
import com.invpeajes.model.Ticket;
import com.invpeajes.persistence.TicketDao;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
 
/**
 * Reglas de negocio para el cobro de peajes y la emisión de tiquetes.
 * <p>
 * Valida los datos del cobro, obtiene la tarifa vigente del peaje según la
 * categoría del vehículo, calcula el valor total y genera el número del tiquete
 * (prefijo y consecutivo) antes de guardarlo.
 * </p>
 * <p>
 * Los datos inválidos se reportan con {@link IllegalArgumentException} y los
 * errores de base de datos con {@link SQLException}; ambos llevan un mensaje
 * que se puede mostrar directamente al usuario.
 * </p>
 */
public class TicketService {
 
    /** Métodos de pago que acepta la tabla {@code TIQUETE}. */
    public static final List<String> PAYMENT_METHODS = List.of("EFECTIVO", "TARJETA", "TAG_COLPASS");
 
    private static final String TAG_PAYMENT = "TAG_COLPASS";
    private static final int MAX_PLATE_LENGTH = 10;
    private static final int MAX_TAG_LENGTH = 30;
    private static final int ORACLE_CHILD_RECORD_ERROR = 2292;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
 
    private final TicketDao ticketDAO;
    private final TollRateService tollRateService;
 
    /**
     * Crea el servicio con sus dependencias por defecto.
     */
    public TicketService() {
        this(new TicketDao(), new TollRateService());
    }
 
    /**
     * Crea el servicio con dependencias específicas.
     * Permite reemplazarlas en pruebas.
     *
     * @param ticketDAO       acceso a datos de tiquetes
     * @param tollRateService servicio que entrega las tarifas de los peajes
     */
    public TicketService(TicketDao ticketDAO, TollRateService tollRateService) {
        this.ticketDAO = ticketDAO;
        this.tollRateService = tollRateService;
    }
 
    /**
     * Calcula cuánto debe pagar un vehículo sin registrar el cobro.
     * Sirve para mostrar el valor al usuario antes de confirmar el pago.
     *
     * @param tollId        identificador del peaje
     * @param categoryId    identificador de la categoría vehicular
     * @param serviceAmount valor de servicios adicionales; {@code null} equivale a cero
     * @return valor total a pagar (tarifa del peaje más servicios)
     * @throws IllegalArgumentException si los datos no son válidos o el peaje
     *                                  no tiene tarifa para esa categoría
     * @throws SQLException             si ocurre un error al consultar la tarifa
     */
    public BigDecimal calculateCharge(int tollId, int categoryId, BigDecimal serviceAmount)
            throws SQLException {
        BigDecimal services = normalizeServiceAmount(serviceAmount);
        return findTollPrice(tollId, categoryId).add(services);
    }
 
    /**
     * Registra el cobro de un peaje y emite su tiquete.
     * <p>
     * La tarifa se toma de la base de datos, el consecutivo se calcula según el
     * prefijo del peaje (por ejemplo "P008" para el peaje 8) y la fecha y hora
     * del pago son las del momento del registro.
     * </p>
     *
     * @param tollId        identificador del peaje
     * @param boothId       identificador de la caseta que realiza el cobro
     * @param licensePlate  placa del vehículo
     * @param categoryId    identificador de la categoría vehicular
     * @param paymentMethod método de pago: EFECTIVO, TARJETA o TAG_COLPASS
     * @param tagCode       código del TAG; obligatorio solo si el pago es con TAG_COLPASS
     * @param serviceAmount valor de servicios adicionales; {@code null} equivale a cero
     * @param operatorId    cédula del operador que registra el cobro (opcional)
     * @return el tiquete registrado, con su identificador y número asignados
     * @throws IllegalArgumentException si algún dato no es válido
     * @throws SQLException             si ocurre un error al guardar el tiquete
     */
    public Ticket registerPayment(int tollId, long boothId, String licensePlate, int categoryId,
                                  String paymentMethod, String tagCode, BigDecimal serviceAmount,
                                  String operatorId) throws SQLException {
        validatePositive(boothId, "La caseta");
        String plate = normalizeLicensePlate(licensePlate);
        String method = normalizePaymentMethod(paymentMethod);
        String tag = normalizeTagCode(method, tagCode);
        BigDecimal services = normalizeServiceAmount(serviceAmount);
        BigDecimal tollPrice = findTollPrice(tollId, categoryId);
 
        String prefix = buildPrefix(tollId);
        long sequenceNumber = ticketDAO.findNextSequenceNumber(prefix);
        LocalDateTime now = LocalDateTime.now();
 
        Ticket ticket = new Ticket(prefix, sequenceNumber, now, tollPrice, services, method,
                tag, plate, categoryId, boothId, normalizeOptional(operatorId));
        ticket.setPaymentTime(now.format(TIME_FORMAT));
        return ticketDAO.create(ticket);
    }
 
    /**
     * Busca un tiquete por su identificador.
     *
     * @param id identificador del tiquete
     * @return el tiquete encontrado
     * @throws IllegalArgumentException si el tiquete no existe
     * @throws SQLException             si ocurre un error en la consulta
     */
    public Ticket findById(long id) throws SQLException {
        Ticket ticket = ticketDAO.findById(id);
        if (ticket == null) {
            throw new IllegalArgumentException("No existe un tiquete con el identificador " + id + ".");
        }
        return ticket;
    }
 
    /**
     * Consulta todos los tiquetes registrados.
     *
     * @return lista de tiquetes, del más reciente al más antiguo
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<Ticket> findAll() throws SQLException {
        return ticketDAO.findAll();
    }
 
    /**
     * Consulta el historial de cobros de un vehículo.
     *
     * @param licensePlate placa del vehículo
     * @return lista de tiquetes del vehículo; vacía si no tiene
     * @throws IllegalArgumentException si la placa no es válida
     * @throws SQLException             si ocurre un error en la consulta
     */
    public List<Ticket> findByLicensePlate(String licensePlate) throws SQLException {
        return ticketDAO.findByLicensePlate(normalizeLicensePlate(licensePlate));
    }
 
    /**
     * Calcula el total recaudado en una lista de tiquetes.
     *
     * @param tickets tiquetes a sumar
     * @return suma de los valores pagados
     */
    public BigDecimal calculateTotalCollected(List<Ticket> tickets) {
        BigDecimal total = BigDecimal.ZERO;
        for (Ticket ticket : tickets) {
            total = total.add(ticket.getTotalAmount());
        }
        return total;
    }
 
    /**
     * Elimina un tiquete.
     *
     * @param id identificador del tiquete
     * @throws IllegalArgumentException si el tiquete no existe
     * @throws IllegalStateException    si el tiquete tiene asistencias asociadas
     * @throws SQLException             si ocurre otro error al eliminar
     */
    public void delete(long id) throws SQLException {
        try {
            if (!ticketDAO.delete(id)) {
                throw new IllegalArgumentException("No existe un tiquete con el identificador " + id + ".");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == ORACLE_CHILD_RECORD_ERROR) {
                throw new IllegalStateException(
                        "No se puede eliminar el tiquete porque tiene asistencias asociadas.");
            }
            throw e;
        }
    }
 
    private BigDecimal findTollPrice(int tollId, int categoryId) throws SQLException {
        return tollRateService.findPrice(tollId, categoryId);
    }
 
    private String buildPrefix(int tollId) {
        return String.format("P%03d", tollId);
    }
 
    private String normalizeLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria.");
        }
        String plate = licensePlate.trim().toUpperCase().replace(" ", "").replace("-", "");
        if (plate.length() > MAX_PLATE_LENGTH || !plate.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("La placa solo puede tener letras y números, máximo "
                    + MAX_PLATE_LENGTH + " caracteres.");
        }
        return plate;
    }
 
    private String normalizePaymentMethod(String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();
        if (!PAYMENT_METHODS.contains(method)) {
            throw new IllegalArgumentException("Método de pago no válido. Use: "
                    + String.join(", ", PAYMENT_METHODS) + ".");
        }
        return method;
    }
 
    private String normalizeTagCode(String paymentMethod, String tagCode) {
        String tag = normalizeOptional(tagCode);
        if (TAG_PAYMENT.equals(paymentMethod)) {
            if (tag == null) {
                throw new IllegalArgumentException("El código del TAG es obligatorio para pagos con TAG_COLPASS.");
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("El código del TAG no puede superar "
                        + MAX_TAG_LENGTH + " caracteres.");
            }
            return tag;
        }
        return null;
    }
 
    private BigDecimal normalizeServiceAmount(BigDecimal serviceAmount) {
        if (serviceAmount == null) {
            return BigDecimal.ZERO;
        }
        if (serviceAmount.signum() < 0) {
            throw new IllegalArgumentException("El valor de servicios no puede ser negativo.");
        }
        return serviceAmount;
    }
 
    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
 
    private void validatePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " debe tener un identificador mayor que cero.");
        }
    }
}