package com.invpeajes.persistence;
 
import com.invpeajes.model.TollRate;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Acceso a datos de la tabla {@code PEAJE.PEAJE_CATEGORIA}.
 * <p>
 * Ofrece las operaciones CRUD (crear, consultar, actualizar y eliminar) de las
 * tarifas de los peajes. Una tarifa se identifica por el peaje y la categoría.
 * Los errores de base de datos se propagan como {@link SQLException} para que
 * la capa de servicio los maneje.
 * </p>
 */
public class TollRateDao {
 
    private static final String SELECT_COLUMNS =
            "SELECT ID_PEAJE, ID_CATEGORIA, PRECIO, VIGENTE_DESDE, FUENTE FROM PEAJE.PEAJE_CATEGORIA";
 
    private static final String INSERT =
            "INSERT INTO PEAJE.PEAJE_CATEGORIA (PRECIO, VIGENTE_DESDE, FUENTE, ID_PEAJE, ID_CATEGORIA) "
            + "VALUES (?, ?, ?, ?, ?)";
 
    private static final String UPDATE =
            "UPDATE PEAJE.PEAJE_CATEGORIA SET PRECIO = ?, VIGENTE_DESDE = ?, FUENTE = ? "
            + "WHERE ID_PEAJE = ? AND ID_CATEGORIA = ?";
 
    private static final String DELETE =
            "DELETE FROM PEAJE.PEAJE_CATEGORIA WHERE ID_PEAJE = ? AND ID_CATEGORIA = ?";
 
    /**
     * Registra una tarifa nueva.
     *
     * @param rate tarifa a registrar
     * @throws SQLException si ocurre un error al guardar, por ejemplo si ya
     *                      existe una tarifa para ese peaje y categoría
     */
    public void create(TollRate rate) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            fillStatement(statement, rate);
            statement.executeUpdate();
        }
    }
 
    /**
     * Busca la tarifa de un peaje para una categoría.
     *
     * @param tollId     identificador del peaje
     * @param categoryId identificador de la categoría vehicular
     * @return la tarifa encontrada, o {@code null} si no existe
     * @throws SQLException si ocurre un error en la consulta
     */
    public TollRate findById(int tollId, int categoryId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE ID_PEAJE = ? AND ID_CATEGORIA = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tollId);
            statement.setInt(2, categoryId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRow(result) : null;
            }
        }
    }
 
    /**
     * Consulta todas las tarifas ordenadas por peaje y categoría.
     *
     * @return lista de tarifas; vacía si no hay registros
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<TollRate> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY ID_PEAJE, ID_CATEGORIA";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return mapRows(result);
        }
    }
 
    /**
     * Consulta todas las tarifas de un peaje, una por categoría.
     *
     * @param tollId identificador del peaje
     * @return tarifas del peaje ordenadas por categoría; vacía si no tiene
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<TollRate> findByTollId(int tollId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE ID_PEAJE = ? ORDER BY ID_CATEGORIA";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tollId);
            try (ResultSet result = statement.executeQuery()) {
                return mapRows(result);
            }
        }
    }
 
    /**
     * Actualiza el precio, la fecha de vigencia y la fuente de una tarifa.
     * El peaje y la categoría no se modifican.
     *
     * @param rate tarifa con los datos nuevos
     * @return {@code true} si se actualizó el registro
     * @throws SQLException si ocurre un error al actualizar
     */
    public boolean update(TollRate rate) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            fillStatement(statement, rate);
            return statement.executeUpdate() > 0;
        }
    }
 
    /**
     * Elimina la tarifa de un peaje para una categoría.
     *
     * @param tollId     identificador del peaje
     * @param categoryId identificador de la categoría vehicular
     * @return {@code true} si se eliminó el registro
     * @throws SQLException si ocurre un error al eliminar
     */
    public boolean delete(int tollId, int categoryId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setInt(1, tollId);
            statement.setInt(2, categoryId);
            return statement.executeUpdate() > 0;
        }
    }
 
    private void fillStatement(PreparedStatement statement, TollRate rate) throws SQLException {
        statement.setBigDecimal(1, rate.getPrice());
        statement.setDate(2, rate.getEffectiveDate() == null ? null : Date.valueOf(rate.getEffectiveDate()));
        statement.setString(3, rate.getSource());
        statement.setInt(4, rate.getTollId());
        statement.setInt(5, rate.getCategoryId());
    }
 
    private List<TollRate> mapRows(ResultSet result) throws SQLException {
        List<TollRate> rates = new ArrayList<>();
        while (result.next()) {
            rates.add(mapRow(result));
        }
        return rates;
    }
 
    private TollRate mapRow(ResultSet result) throws SQLException {
        TollRate rate = new TollRate();
        rate.setTollId(result.getInt("ID_PEAJE"));
        rate.setCategoryId(result.getInt("ID_CATEGORIA"));
        rate.setPrice(result.getBigDecimal("PRECIO"));
        Date effectiveDate = result.getDate("VIGENTE_DESDE");
        rate.setEffectiveDate(effectiveDate == null ? null : effectiveDate.toLocalDate());
        rate.setSource(result.getString("FUENTE"));
        return rate;
    }
}