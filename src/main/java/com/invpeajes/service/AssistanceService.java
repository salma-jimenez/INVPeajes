package com.invpeajes.service;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.persistence.IAssistanceDAO;
import com.invpeajes.persistence.AssistanceDAOImpl;
import com.invpeajes.persistence.IAssistanceHistoryDAO;
import com.invpeajes.persistence.AssistanceHistoryDAOImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de negocio para la gestión de auxilios viales: registro, asignación de
 * recursos, avance de estados, cierre y cancelación.
 *
 * @author Salomejimenez
 */
public class AssistanceService {

    private static final double MIN_LATITUDE = -5;
    private static final double MAX_LATITUDE = 14;
    private static final double MIN_LONGITUDE = -82;
    private static final double MAX_LONGITUDE = -66;
    private static final Pattern PHONE_FORMAT = Pattern.compile("^\\d{7,15}$");

    private final IAssistanceDAO assistanceDAO;
    private final IAssistanceHistoryDAO historyDAO;
    private final VehicleService vehicleService;

    /**
     * Crea el servicio con sus dependencias por defecto.
     */
    public AssistanceService() {
        this(new AssistanceDAOImpl(), new AssistanceHistoryDAOImpl(), new VehicleService());
    }

    /**
     * Crea el servicio con dependencias inyectadas.
     *
     * @param assistanceDAO DAO de asistencias
     * @param historyDAO DAO de historial para consultas
     * @param vehicleService servicio para verificar placas
     */
    public AssistanceService(IAssistanceDAO assistanceDAO, IAssistanceHistoryDAO historyDAO,
            VehicleService vehicleService) {
        this.assistanceDAO = assistanceDAO;
        this.historyDAO = historyDAO;
        this.vehicleService = vehicleService;
    }

    /**
     * Registra una nueva solicitud de asistencia en estado REGISTERED.
     *
     * @param assistance datos de la solicitud
     * @return id asignado a la asistencia
     */
    public int register(Assistance assistance) {
        validateRequest(assistance);
        assistance.setCurrentStatus(AssistanceStatus.REGISTERED);
        assistanceDAO.insert(assistance);
        return assistance.getAssistanceId();
    }

    /**
     * Asigna un recurso (grúa, ambulancia...) y pasa la asistencia a ASSIGNED.
     *
     * @param assistanceId asistencia a atender
     * @param resourceId recurso de asistencia asignado
     */
    public void assignResource(int assistanceId, int resourceId) {
        Assistance assistance = get(assistanceId);
        assistance.setResourceId(resourceId);
        applyChange(assistance, AssistanceStatus.ASSIGNED, "Recurso " + resourceId + " asignado");
    }

    /**
     * Marca que el recurso va en camino al incidente.
     *
     * @param assistanceId asistencia en curso
     */
    public void markEnRoute(int assistanceId) {
        applyChange(get(assistanceId), AssistanceStatus.EN_ROUTE, "Recurso en camino");
    }

    /**
     * Marca que el recurso llegó y está atendiendo el incidente.
     *
     * @param assistanceId asistencia en curso
     */
    public void startService(int assistanceId) {
        applyChange(get(assistanceId), AssistanceStatus.IN_PROGRESS, "Atención iniciada");
    }

    /**
     * Cierra la asistencia como ATTENDED registrando su costo final.
     *
     * @param assistanceId asistencia a cerrar
     * @param cost costo del servicio (0 si es gratuito)
     * @param notes detalle del cierre
     */
    public void complete(int assistanceId, BigDecimal cost, String notes) {
        if (cost == null || cost.signum() < 0) {
            throw new ValidationException("El costo no puede ser negativo");
        }
        Assistance assistance = get(assistanceId);
        assistance.setCost(cost);
        applyChange(assistance, AssistanceStatus.ATTENDED, textOrDefault(notes, "Asistencia atendida"));
    }

    /**
     * Cancela una asistencia que aún no ha empezado a atenderse.
     *
     * @param assistanceId asistencia a cancelar
     * @param reason razón de la cancelación (obligatoria)
     */
    public void cancel(int assistanceId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Debe indicar el motivo de la cancelación");
        }
        applyChange(get(assistanceId), AssistanceStatus.CANCELLED, reason.trim());
    }

    /**
     * Obtiene una asistencia o lanza error si no existe.
     *
     * @param assistanceId id de la asistencia
     * @return la asistencia encontrada
     */
    public Assistance get(int assistanceId) {
        return assistanceDAO.findById(assistanceId)
                .orElseThrow(() -> new ValidationException("No existe la asistencia " + assistanceId));
    }

    /**
     * Lista todas las asistencias, más recientes primero.
     *
     * @return asistencias registradas
     */
    public List<Assistance> listAll() {
        return assistanceDAO.findAll();
    }

    /**
     * Lista las asistencias en un estado dado (ej. pendientes de asignar).
     *
     * @param status estado a filtrar
     * @return asistencias en ese estado
     */
    public List<Assistance> listByStatus(AssistanceStatus status) {
        return assistanceDAO.findByStatus(status);
    }

    /**
     * Lista las asistencias de un vehículo.
     *
     * @param plate placa del vehículo (se normaliza)
     * @return asistencias del vehículo
     */
    public List<Assistance> listByPlate(String plate) {
        return assistanceDAO.findByPlate(VehicleService.normalizePlate(plate));
    }

    /**
     * Lista las asistencias atendidas desde un peaje.
     *
     * @param tollId peaje a filtrar
     * @return asistencias del peaje
     */
    public List<Assistance> listByToll(int tollId) {
        return assistanceDAO.findByToll(tollId);
    }

    /**
     * Consulta la trazabilidad de cambios de estado de una asistencia.
     *
     * @param assistanceId asistencia a consultar
     * @return historial en orden cronológico
     */
    public List<AssistanceHistory> getHistory(int assistanceId) {
        get(assistanceId);
        return historyDAO.findByAssistance(assistanceId);
    }

    /**
     * Elimina una asistencia y su historial. Solo se permite si fue cancelada.
     *
     * @param assistanceId asistencia a eliminar
     */
    public void delete(int assistanceId) {
        if (get(assistanceId).getCurrentStatus() != AssistanceStatus.CANCELLED) {
            throw new ValidationException("Solo se pueden eliminar asistencias canceladas");
        }
        assistanceDAO.delete(assistanceId);
    }

    private void applyChange(Assistance assistance, AssistanceStatus next, String notes) {
        AssistanceStatus previous = assistance.getCurrentStatus();
        if (!previous.canChangeTo(next)) {
            throw new ValidationException(String.format(
                    "No se puede pasar de %s a %s. Estados permitidos: %s",
                    previous.getDbValue(), next.getDbValue(), describeAllowed(previous)));
        }
        if (next == AssistanceStatus.ASSIGNED && assistance.getResourceId() == null) {
            throw new ValidationException("Debe asignar un recurso antes de pasar a ASIGNADA");
        }
        assistance.setCurrentStatus(next);
        assistanceDAO.changeStatus(assistance, previous, notes);
    }

    private String describeAllowed(AssistanceStatus status) {
        if (status.isFinal()) {
            return "ninguno (estado final)";
        }
        StringBuilder allowed = new StringBuilder();
        for (AssistanceStatus next : status.allowedNext()) {
            allowed.append(allowed.length() == 0 ? "" : ", ").append(next.getDbValue());
        }
        return allowed.toString();
    }

    private void validateRequest(Assistance assistance) {
        if (assistance.getType() == null) {
            throw new ValidationException("El tipo de asistencia es obligatorio");
        }
        if (assistance.getTollId() <= 0) {
            throw new ValidationException("Debe indicar el peaje que atiende la asistencia");
        }
        String plate = assistance.getPlate();
        assistance.setPlate(plate == null || plate.trim().isEmpty() ? null : vehicleService.get(plate).getPlate());
        String phone = assistance.getContactPhone();
        if (phone != null && !PHONE_FORMAT.matcher(phone).matches()) {
            throw new ValidationException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        validateCoordinates(assistance.getIncidentLatitude(), assistance.getIncidentLongitude());
        if (assistance.getCost() == null) {
            assistance.setCost(BigDecimal.ZERO);
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new ValidationException("Debe indicar latitud y longitud juntas, o ninguna");
        }
        if (latitude != null && (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE
                || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE)) {
            throw new ValidationException("Las coordenadas del incidente están fuera de Colombia");
        }
    }

    private String textOrDefault(String text, String defaultText) {
        return text == null || text.trim().isEmpty() ? defaultText : text.trim();
    }

}
