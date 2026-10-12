package com.invpeajes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

public class ConexionBD {

    private static final String URL = "jdbc:oracle:thin:@//localhost:1521/xepdb1";
    private static final String USUARIO = "PEAJE";
    private static final String PASSWORD = "PEAJEX";

    public static Connection conectar() {
        Connection conexion = null;
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);

            // --- ESTAS LÍNEAS TE CONFIRMAN LA CONEXIÓN EXACTA ---
            DatabaseMetaData meta = conexion.getMetaData();
            System.out.println("¡Conexión exitosa!");
            System.out.println("Conectado a la BD: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("Usuario de BD: " + meta.getUserName());
            System.out.println("URL usada: " + meta.getURL());

        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conexion;
    }
}