
package com.invpeajes.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Utilidades para leer y escribir columnas numéricas que admiten NULL,
 * compartidas por los DAO del módulo.
 *  @author Salomejimenez
 */
final class JdbcUtils {

    private JdbcUtils() {
    }

    /**
     * Asigna un entero o NULL a un parámetro.
     *
     * @param ps    sentencia preparada
     * @param index posición del parámetro
     * @param value valor a asignar (puede ser null)
     * @throws SQLException si falla la asignación
     */
    static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setInt(index, value);
        }
    }

    /**
     * Asigna un entero largo o NULL a un parámetro.
     *
     * @param ps    sentencia preparada
     * @param index posición del parámetro
     * @param value valor a asignar (puede ser null)
     * @throws SQLException si falla la asignación
     */
    static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setLong(index, value);
        }
    }

    /**
     * Asigna un decimal o NULL a un parámetro.
     *
     * @param ps    sentencia preparada
     * @param index posición del parámetro
     * @param value valor a asignar (puede ser null)
     * @throws SQLException si falla la asignación
     */
    static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setDouble(index, value);
        }
    }

    /**
     * Lee una columna entera que puede ser NULL.
     *
     * @param rs     fila actual
     * @param column nombre de la columna
     * @return el valor, o null si la columna es NULL
     * @throws SQLException si falla la lectura
     */
    static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Lee una columna entera larga que puede ser NULL.
     *
     * @param rs     fila actual
     * @param column nombre de la columna
     * @return el valor, o null si la columna es NULL
     * @throws SQLException si falla la lectura
     */
    static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Lee una columna decimal que puede ser NULL.
     *
     * @param rs     fila actual
     * @param column nombre de la columna
     * @return el valor, o null si la columna es NULL
     * @throws SQLException si falla la lectura
     */
    static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}