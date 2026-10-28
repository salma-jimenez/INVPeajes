
package com.invpeajes.persistence;
import java.sql.SQLException;

/**
 *
 * Error de acceso a datos con un mensaje entendible para el usuario.
 * Traduce los códigos de error más comunes de Oracle.
 * @author Salomejimenez
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final int ORA_UNIQUE_VIOLATION = 1;
    private static final int ORA_NOT_NULL_VIOLATION = 1400;
    private static final int ORA_CHECK_VIOLATION = 2290;
    private static final int ORA_PARENT_NOT_FOUND = 2291;
    private static final int ORA_CHILD_RECORD_FOUND = 2292;
    private static final int ORA_NO_CONNECTION = 17002;

    private final int oracleErrorCode;

    /**
     * Crea la excepción a partir de una SQLException de Oracle.
     *
     * @param operation descripción de lo que se intentaba hacer
     * @param cause     excepción original de JDBC
     */
    public DataAccessException(String operation, SQLException cause) {
        super(operation + ": " + translate(cause), cause);
        this.oracleErrorCode = cause.getErrorCode();
    }

    /**
     * Código de error de Oracle (ej. 1 para ORA-00001).
     *
     * @return código numérico del error
     */
    public int getOracleErrorCode() {
        return oracleErrorCode;
    }

    private static String translate(SQLException e) {
        switch (e.getErrorCode()) {
            case ORA_UNIQUE_VIOLATION:
                return "ya existe un registro con ese valor único";
            case ORA_NOT_NULL_VIOLATION:
                return "falta un dato obligatorio";
            case ORA_CHECK_VIOLATION:
                return "un valor no cumple las reglas de la base de datos";
            case ORA_PARENT_NOT_FOUND:
                return "un registro referenciado no existe";
            case ORA_CHILD_RECORD_FOUND:
                return "no se puede eliminar porque tiene registros asociados";
            case ORA_NO_CONNECTION:
                return "no hay conexión con el servidor Oracle";
            default:
                return e.getMessage();
        }
    }
    
}
