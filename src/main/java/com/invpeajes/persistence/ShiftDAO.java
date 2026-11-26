package com.invpeajes.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Turnos de los operadores en las casetas (tabla {@code PEAJE.TURNO_CASETA}).
 *
 * <p>Usa los procedimientos del paquete {@code pkg_tiquetes} de la base de datos.
 * Desde la etapa 4 el trigger {@code trg_validar_cobro} rechaza cualquier tiquete
 * de un operador que no tenga el turno abierto en esa caseta, así que la caja
 * debe abrir el turno antes de cobrar y cerrarlo al salir.</p>
 *
 * @author Salomejimenez
 */
public class ShiftDAO {

    /**
     * Si el operador quedó con un turno abierto (por ejemplo, la aplicación se cerró
     * sin cerrar turno), primero se cierra ese turno y luego se abre el nuevo.
     */
    private static final String OPEN_SQL =
            "BEGIN "
          + "  IF fun_caseta_turno(?) IS NOT NULL THEN pkg_tiquetes.Pr_cerrar_turno(?); END IF; "
          + "  pkg_tiquetes.Pr_abrir_turno(?, ?); "
          + "END;";

    /** Cierra el turno abierto del operador; si no tiene, no hace nada. */
    private static final String CLOSE_SQL =
            "BEGIN "
          + "  IF fun_caseta_turno(?) IS NOT NULL THEN pkg_tiquetes.Pr_cerrar_turno(?); END IF; "
          + "END;";

    /**
     * Abre el turno de un operador en una caseta.
     *
     * @param operatorId cédula del operador
     * @param boothId    ID de la caseta
     * @throws DataAccessException si la base de datos no permite abrir el turno
     *                             (caseta inactiva, de otro peaje u ocupada por otro operador)
     */
    public void open(String operatorId, long boothId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(OPEN_SQL)) {
            cs.setString(1, operatorId);
            cs.setString(2, operatorId);
            cs.setString(3, operatorId);
            cs.setLong(4, boothId);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo abrir el turno", e);
        }
    }

    /**
     * Cierra el turno abierto de un operador.
     *
     * @param operatorId cédula del operador
     * @throws DataAccessException si falla la base de datos
     */
    public void close(String operatorId) {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall(CLOSE_SQL)) {
            cs.setString(1, operatorId);
            cs.setString(2, operatorId);
            cs.execute();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cerrar el turno", e);
        }
    }
}
