package com.invpeajes.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fábrica de conexiones para el módulo de vehículos y asistencias.
 * Delega en DatabaseConnection para que todo el proyecto use
 * una sola URL, usuario y contraseña.
 *
 * @author Salomejimenez
 */
public final class VehicleConnectionFactory {

    private VehicleConnectionFactory() {
    }

    /**
     * Abre una nueva conexión a la base de datos Oracle.
     *
     * @return conexión abierta; quien la recibe debe cerrarla
     * @throws SQLException si no se puede establecer la conexión
     */
    public static Connection open() throws SQLException {
        return DatabaseConnection.getConnection();
    }
}