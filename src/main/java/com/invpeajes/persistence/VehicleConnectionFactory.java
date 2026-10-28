package com.invpeajes.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexiones JDBC a Oracle para el módulo de vehículos y
 * asistencias. Cada llamada abre una conexión nueva que el DAO cierra con
 * try-with-resources.
 *
 * @author Salomejimenez
 */
public final class VehicleConnectionFactory {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER = "PEAJE";
    private static final String PASSWORD = "cambia_esta_clave";

    private VehicleConnectionFactory() {
    }

    /**
     * Abre una nueva conexión a la base de datos Oracle.
     *
     * @return conexión abierta; quien la recibe debe cerrarla
     * @throws SQLException si no se puede establecer la conexión
     */
    public static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

}
