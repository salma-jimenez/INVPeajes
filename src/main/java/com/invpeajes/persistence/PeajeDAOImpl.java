
package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Peaje;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la gestión de persistencia de la entidad Peaje en Oracle.
 * Cumple con el contrato definido en IPeajeDAO.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class PeajeDAOImpl implements IPeajeDAO {

    @Override
    public boolean insertar(Peaje peaje) {
        String sql = "INSERT INTO PEAJE (ID_PEAJE, NOMBRE, KILOMETRO, LATITUD, LONGITUD, "
                   + "ESTADO_OPERATIVO, FECHA, ID_RUTA, ID_CONCESIONARIO, ID_MUNICIPIOS) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                   
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, peaje.getIdPeaje());
            ps.setString(2, peaje.getNombrePeaje());
            ps.setString(3, peaje.getKilometro());
            ps.setString(4, peaje.getLatitud());
            ps.setString(5, peaje.getLongitud());
            ps.setString(6, peaje.getEstadoOperativo());
            
            if (peaje.getFecha() != null) {
                ps.setDate(7, new java.sql.Date(peaje.getFecha().getTime()));
            } else {
                ps.setDate(7, null);
            }
            
            ps.setString(8, peaje.getIdRuta());
            ps.setString(9, peaje.getIdConcesionario());
            ps.setString(10, peaje.getIdMunicipios());

            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al insertar peaje: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Peaje obtenerPorId(String idPeaje) {
        // búsqueda por ID
        return null;
    }

    @Override
    public List<Peaje> listarTodos() {
        // consulta masiva
        return new ArrayList<>();
    }

    @Override
    public boolean actualizar(Peaje peaje) {
        // modificación
        return false;
    }

    @Override
    public boolean eliminar(String idPeaje) {
        // borrado
        return false;
    }
}
