package com.invpeajes.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Administra la conexión JDBC con la base de datos Oracle del proyecto.
 * Todas las clases DAO obtienen su conexión desde aquí.
 */
public final class DatabaseConnection {

    private static final String URL = "jdbc:oracle:thin:@//localhost:1521/xepdb1";
    private static final String USER = "PEAJE";
    private static final String PASSWORD = "PEAJEX";

    private DatabaseConnection() {
    }

    /**
     * Abre una nueva conexión con Oracle.
     * Quien la recibe debe cerrarla, preferiblemente con try-with-resources.
     *
     * @return conexión abierta con la base de datos
     * @throws SQLException si no es posible conectarse
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Verifica la conexión e imprime los datos de la base de datos.
     * Se usa solo para pruebas.
     *
     * @return {@code true} si la conexión fue exitosa
     */
    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            System.out.println("Conexión exitosa a " + meta.getDatabaseProductVersion());
            System.out.println("Usuario: " + meta.getUserName());
            return true;
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }
}