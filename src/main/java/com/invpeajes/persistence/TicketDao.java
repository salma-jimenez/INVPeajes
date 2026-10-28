package com.invpeajes.persistence;
 
import com.invpeajes.model.Ticket;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Acceso a datos de la tabla {@code PEAJE.TIQUETE}.
 * <p>
 * Ofrece las operaciones CRUD (crear, consultar, actualizar y eliminar) y
 * consultas de apoyo para la emisión de tiquetes. Los errores de base de datos
 * se propagan como {@link SQLException} para que la capa de servicio los maneje.
 * </p>
 */
public class TicketDao {
 
    private static final String SELECT_COLUMNS =
            "SELECT ID_TIQUETE, PREFIJO, CONSECUTIVO, FECHA_PAGO, HORA_PAGO, VALOR_PEAJE, "
            + "VALOR_SERVICIOS, VALOR_PAGO, METODO_PAGO, CODIGO_TAG, PLACA, ID_CATEGORIA, "
            + "ID_CASETA, CEDULA_OPERADOR FROM PEAJE.TIQUETE";
 
    private static final String INSERT =
            "INSERT INTO PEAJE.TIQUETE (PREFIJO, CONSECUTIVO, FECHA_PAGO, HORA_PAGO, VALOR_PEAJE, "
            + "VALOR_SERVICIOS, VALOR_PAGO, METODO_PAGO, CODIGO_TAG, PLACA, ID_CATEGORIA, "
            + "ID_CASETA, CEDULA_OPERADOR) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
 
    private static final String UPDATE =
            "UPDATE PEAJE.TIQUETE SET PREFIJO = ?, CONSECUTIVO = ?, FECHA_PAGO = ?, HORA_PAGO = ?, "
            + "VALOR_PEAJE = ?, VALOR_SERVICIOS = ?, VALOR_PAGO = ?, METODO_PAGO = ?, "
            + "CODIGO_TAG = ?, PLACA = ?, ID_CATEGORIA = ?, ID_CASETA = ?, CEDULA_OPERADOR = ? "
            + "WHERE ID_TIQUETE = ?";
 
    private static final String DELETE = "DELETE FROM PEAJE.TIQUETE WHERE ID_TIQUETE = ?";
 
    private static final String NEXT_SEQUENCE =
            "SELECT NVL(MAX(CONSECUTIVO), 0) + 1 FROM PEAJE.TIQUETE WHERE PREFIJO = ?";
 
    private static final String TOLL_PRICE =
            "SELECT PRECIO FROM PEAJE.PEAJE_CATEGORIA WHERE ID_PEAJE = ? AND ID_CATEGORIA = ?";
 
    /**
     * Registra un tiquete nuevo. Oracle genera el identificador y se asigna
     * al objeto recibido.
     *
     * @param ticket tiquete a registrar
     * @return el mismo tiquete con su identificador asignado
     * @throws SQLException si ocurre un error al guardar
     */
    public Ticket create(Ticket ticket) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, new String[]{"ID_TIQUETE"})) {
            fillStatement(statement, ticket);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    ticket.setId(keys.getLong(1));
                }
            }
            return ticket;
        }
    }
 
    /**
     * Busca un tiquete por su identificador.
     *
     * @param id identificador del tiquete
     * @return el tiquete encontrado, o {@code null} si no existe
     * @throws SQLException si ocurre un error en la consulta
     */
    public Ticket findById(long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE ID_TIQUETE = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRow(result) : null;
            }
        }
    }
 
    /**
     * Consulta todos los tiquetes, del más reciente al más antiguo.
     *
     * @return lista de tiquetes; vacía si no hay registros
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<Ticket> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY FECHA_PAGO DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return mapRows(result);
        }
    }
 
    /**
     * Consulta los tiquetes emitidos a un vehículo.
     *
     * @param licensePlate placa del vehículo
     * @return lista de tiquetes del vehículo; vacía si no tiene
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<Ticket> findByLicensePlate(String licensePlate) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE PLACA = ? ORDER BY FECHA_PAGO DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, licensePlate);
            try (ResultSet result = statement.executeQuery()) {
                return mapRows(result);
            }
        }
    }
 
    /**
     * Actualiza todos los datos de un tiquete existente.
     *
     * @param ticket tiquete con los datos nuevos; debe tener identificador
     * @return {@code true} si se actualizó el registro
     * @throws SQLException si ocurre un error al actualizar
     */
    public boolean update(Ticket ticket) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            fillStatement(statement, ticket);
            statement.setLong(14, ticket.getId());
            return statement.executeUpdate() > 0;
        }
    }
 
    /**
     * Elimina un tiquete por su identificador.
     *
     * @param id identificador del tiquete
     * @return {@code true} si se eliminó el registro
     * @throws SQLException si ocurre un error al eliminar, por ejemplo si
     *                      una asistencia hace referencia al tiquete
     */
    public boolean delete(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }
 
    /**
     * Calcula el siguiente consecutivo disponible para un prefijo.
     *
     * @param prefix prefijo del tiquete
     * @return siguiente consecutivo; 1 si el prefijo aún no tiene tiquetes
     * @throws SQLException si ocurre un error en la consulta
     */
    public long findNextSequenceNumber(String prefix) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(NEXT_SEQUENCE)) {
            statement.setString(1, prefix);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }
 
    /**
     * Consulta la tarifa vigente de un peaje para una categoría vehicular.
     *
     * @param tollId     identificador del peaje
     * @param categoryId identificador de la categoría vehicular
     * @return precio de la tarifa, o {@code null} si el peaje no tiene tarifa para esa categoría
     * @throws SQLException si ocurre un error en la consulta
     */
    public BigDecimal findTollPrice(int tollId, int categoryId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(TOLL_PRICE)) {
            statement.setInt(1, tollId);
            statement.setInt(2, categoryId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBigDecimal("PRECIO") : null;
            }
        }
    }
 
    private void fillStatement(PreparedStatement statement, Ticket ticket) throws SQLException {
        statement.setString(1, ticket.getPrefix());
        statement.setLong(2, ticket.getSequenceNumber());
        statement.setTimestamp(3, ticket.getPaymentDate() == null
                ? null : Timestamp.valueOf(ticket.getPaymentDate()));
        statement.setString(4, ticket.getPaymentTime());
        statement.setBigDecimal(5, ticket.getTollAmount());
        statement.setBigDecimal(6, ticket.getServiceAmount());
        statement.setBigDecimal(7, ticket.getTotalAmount());
        statement.setString(8, ticket.getPaymentMethod());
        statement.setString(9, ticket.getTagCode());
        statement.setString(10, ticket.getLicensePlate());
        statement.setInt(11, ticket.getCategoryId());
        statement.setLong(12, ticket.getBoothId());
        statement.setString(13, ticket.getOperatorId());
    }
 
    private List<Ticket> mapRows(ResultSet result) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        while (result.next()) {
            tickets.add(mapRow(result));
        }
        return tickets;
    }
 
    private Ticket mapRow(ResultSet result) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(result.getLong("ID_TIQUETE"));
        ticket.setPrefix(result.getString("PREFIJO"));
        ticket.setSequenceNumber(result.getLong("CONSECUTIVO"));
        Timestamp paymentDate = result.getTimestamp("FECHA_PAGO");
        ticket.setPaymentDate(paymentDate == null ? null : paymentDate.toLocalDateTime());
        ticket.setPaymentTime(result.getString("HORA_PAGO"));
        ticket.setTollAmount(result.getBigDecimal("VALOR_PEAJE"));
        ticket.setServiceAmount(result.getBigDecimal("VALOR_SERVICIOS"));
        ticket.setPaymentMethod(result.getString("METODO_PAGO"));
        ticket.setTagCode(result.getString("CODIGO_TAG"));
        ticket.setLicensePlate(result.getString("PLACA"));
        ticket.setCategoryId(result.getInt("ID_CATEGORIA"));
        ticket.setBoothId(result.getLong("ID_CASETA"));
        ticket.setOperatorId(result.getString("CEDULA_OPERADOR"));
        return ticket;
    }
}