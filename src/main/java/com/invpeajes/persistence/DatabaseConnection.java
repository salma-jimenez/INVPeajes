package com.invpeajes.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

/**
 * Administra el pool de conexiones JDBC con la base de datos Oracle Cloud.
 * Usa UCP (Universal Connection Pool), el pool oficial de Oracle.
 * Todas las clases DAO obtienen su conexión desde aquí.
 */
public final class DatabaseConnection {

    // Carpeta del wallet (usa / aunque sea Windows)
    private static final String WALLET_PATH = "C:/Oracle/Wallet_INVPeajes";

    // invpeajes_tp es uno de los nombres que vienen en tnsnames.ora del wallet
    private static final String URL = "jdbc:oracle:thin:@invpeajes_tp?TNS_ADMIN=" + WALLET_PATH;
    private static final String USER = "PEAJE";
    private static final String PASSWORD = System.getenv("INVPEAJES_DB_PASSWORD");

    private static final int INITIAL_POOL_SIZE = 1;
    private static final int MIN_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 5;
    private static final int WAIT_TIMEOUT_SECONDS = 30;
    private static final int INACTIVE_TIMEOUT_SECONDS = 300;

    private static PoolDataSource pool;

    private DatabaseConnection() {
    }

    /**
     * Crea el pool la primera vez que se pide una conexión y lo reutiliza después.
     *
     * @return pool de conexiones listo para usar
     * @throws SQLException si falta la contraseña o no se puede configurar el pool
     */
    private static synchronized PoolDataSource getPool() throws SQLException {
        if (pool == null) {
            if (PASSWORD == null) {
                throw new SQLException("Falta la variable de entorno INVPEAJES_DB_PASSWORD");
            }
            PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
            pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            pds.setURL(URL);
            pds.setUser(USER);
            pds.setPassword(PASSWORD);
            pds.setConnectionPoolName("INVPeajesPool");
            pds.setInitialPoolSize(INITIAL_POOL_SIZE);
            pds.setMinPoolSize(MIN_POOL_SIZE);
            pds.setMaxPoolSize(MAX_POOL_SIZE);
            pds.setConnectionWaitTimeout(WAIT_TIMEOUT_SECONDS);
            pds.setInactiveConnectionTimeout(INACTIVE_TIMEOUT_SECONDS);
            pds.setValidateConnectionOnBorrow(true);
            pool = pds;
        }
        return pool;
    }

    /**
     * Entrega una conexión del pool.
     * Quien la recibe debe cerrarla, preferiblemente con try-with-resources:
     * al cerrarla vuelve al pool en lugar de cerrarse de verdad.
     *
     * @return conexión lista para usar
     * @throws SQLException si no es posible obtener una conexión
     */
    public static Connection getConnection() throws SQLException {
        return getPool().getConnection();
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
