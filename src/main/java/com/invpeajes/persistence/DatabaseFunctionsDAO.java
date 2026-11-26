package com.invpeajes.persistence;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Llama a las funciones sueltas de la base ({@code fun_...}) con la sintaxis
 * {@code {? = call fun_x(?)}} de JDBC: el primer {@code ?} recibe lo que devuelve la función.
 *
 * <p>Así la regla de negocio queda en un solo lugar (la base) y la usan por igual los
 * paquetes de PL/SQL y el Java.</p>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class DatabaseFunctionsDAO {

    private static final String RATE_SQL = "{? = call fun_tarifa(?, ?)}";
    private static final String ROUTE_COST_SQL = "{? = call fun_costo_ruta(?, ?)}";
    private static final String DAY_REVENUE_SQL = "{? = call fun_recaudo_dia(?, ?)}";
    private static final String TOLL_REVENUE_SQL = "{? = call fun_recaudo_peaje(?, ?, ?)}";
    private static final String TOLL_PASSES_SQL = "{? = call fun_pasos_peaje(?, ?, ?)}";
    private static final String PENDING_ASSISTANCES_SQL = "{? = call fun_asistencias_pendientes(?)}";
    private static final String TOLLS_OF_ENTITY_SQL = "{? = call fun_total_peajes_entidad(?)}";
    private static final String PLATE_CATEGORY_SQL = "{? = call fun_categoria_placa(?)}";
    private static final String DISTANCE_SQL = "{? = call fun_distancia_km(?, ?, ?, ?)}";
    private static final String RECENT_FAILURES_SQL = "{? = call fun_fallos_recientes(?, ?)}";

    /**
     * Precio de un peaje para una categoría ({@code fun_tarifa}).
     *
     * @param tollId     ID del peaje
     * @param categoryId categoría
     * @return el precio, o null si no tiene tarifa
     */
    public BigDecimal rate(int tollId, int categoryId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(RATE_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, tollId);
            cs.setInt(3, categoryId);
            cs.execute();
            return cs.getBigDecimal(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar la tarifa", e);
        }
    }

    /**
     * Lo que cuesta pasar por todos los peajes operativos de una ruta ({@code fun_costo_ruta}).
     *
     * @param routeId    ruta
     * @param categoryId categoría
     * @return costo total (0 si no hay peajes con tarifa)
     */
    public BigDecimal routeCost(String routeId, int categoryId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(ROUTE_COST_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setString(2, routeId);
            cs.setInt(3, categoryId);
            cs.execute();
            return orZero(cs.getBigDecimal(1));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el costo de la ruta " + routeId, e);
        }
    }

    /**
     * Dinero recaudado por un peaje en un día ({@code fun_recaudo_dia}).
     *
     * @param tollId ID del peaje
     * @param day    día, o null para hoy en Colombia (lo decide la base con {@code fun_ahora})
     * @return recaudo del día
     */
    public BigDecimal dayRevenue(int tollId, LocalDate day) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(DAY_REVENUE_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, tollId);
            setNullableDate(cs, 3, day);
            cs.execute();
            return orZero(cs.getBigDecimal(1));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el recaudo del día", e);
        }
    }

    /**
     * Dinero recaudado por un peaje entre dos días, ambos incluidos ({@code fun_recaudo_peaje}).
     *
     * @param tollId ID del peaje
     * @param from   primer día
     * @param to     último día
     * @return recaudo del periodo
     */
    public BigDecimal tollRevenue(int tollId, LocalDate from, LocalDate to) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(TOLL_REVENUE_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, tollId);
            cs.setDate(3, Date.valueOf(from));
            cs.setDate(4, Date.valueOf(to));
            cs.execute();
            return orZero(cs.getBigDecimal(1));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el recaudo del peaje", e);
        }
    }

    /**
     * Vehículos que pagaron en un peaje entre dos días, ambos incluidos ({@code fun_pasos_peaje}).
     *
     * @param tollId ID del peaje
     * @param from   primer día
     * @param to     último día
     * @return número de pasos
     */
    public int tollPasses(int tollId, LocalDate from, LocalDate to) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(TOLL_PASSES_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, tollId);
            cs.setDate(3, Date.valueOf(from));
            cs.setDate(4, Date.valueOf(to));
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los pasos del peaje", e);
        }
    }

    /**
     * Asistencias sin terminar de un peaje ({@code fun_asistencias_pendientes}).
     *
     * @param tollId ID del peaje
     * @return asistencias que no están ATENDIDA ni CANCELADA
     */
    public int pendingAssistances(int tollId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(PENDING_ASSISTANCES_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, tollId);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar las asistencias pendientes", e);
        }
    }

    /**
     * Cuántos peajes tiene una entidad ({@code fun_total_peajes_entidad}).
     *
     * @param entity ANI, INVIAS, ...
     * @return número de peajes
     */
    public int tollsOfEntity(String entity) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(TOLLS_OF_ENTITY_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setString(2, entity);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar los peajes de " + entity, e);
        }
    }

    /**
     * Categoría del vehículo de una placa ({@code fun_categoria_placa}).
     *
     * @param plate placa
     * @return la categoría, o null si la placa no está registrada
     */
    public Integer plateCategory(String plate) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(PLATE_CATEGORY_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setString(2, plate);
            cs.execute();
            int category = cs.getInt(1);
            return cs.wasNull() ? null : category;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar la categoría de la placa " + plate, e);
        }
    }

    /**
     * Distancia en línea recta entre dos puntos, en km ({@code fun_distancia_km}).
     *
     * @param lat1 latitud del primer punto
     * @param lon1 longitud del primer punto
     * @param lat2 latitud del segundo punto
     * @param lon2 longitud del segundo punto
     * @return distancia en km
     */
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(DISTANCE_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setDouble(2, lat1);
            cs.setDouble(3, lon1);
            cs.setDouble(4, lat2);
            cs.setDouble(5, lon2);
            cs.execute();
            return cs.getDouble(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular la distancia", e);
        }
    }

    /**
     * Intentos fallidos seguidos en los últimos minutos ({@code fun_fallos_recientes}).
     *
     * @param identifier cédula o correo
     * @param minutes    ventana en minutos
     * @return número de fallos
     */
    public int recentFailures(String identifier, int minutes) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(RECENT_FAILURES_SQL)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setString(2, identifier);
            cs.setInt(3, minutes);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los intentos fallidos", e);
        }
    }

    private static void setNullableDate(CallableStatement cs, int index, LocalDate day) throws SQLException {
        if (day == null) {
            cs.setNull(index, Types.DATE);
        } else {
            cs.setDate(index, Date.valueOf(day));
        }
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
