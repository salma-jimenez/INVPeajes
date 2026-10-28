
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
        String sql = "INSERT INTO PEAJE ("
                   + "ID_PEAJE, CODIGO_PEAJE, ID_ANI, NOMBRE, KILOMETRO, "
                   + "LATITUD, LONGITUD, UBICACION_DESCRIPCION, SENTIDO, ESTADO_OPERATIVO, "
                   + "NIVEL, ACEPTA_COLPASS, FECHA, TELEFONO, TELEFONO_GRUA, "
                   + "URL_FICHA, ID_RUTA, ID_CONCESIONARIO, ID_SECTOR, ID_DEPARTAMENTO"
                   + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                   
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // 1. Atributos de Identificación
            ps.setInt(1, peaje.getIdPeaje());
            ps.setString(2, peaje.getCodigoPeaje());
            setNullableInt(ps, 3, peaje.getIdAni());
            ps.setString(4, peaje.getNombrePeaje());
            
            // 2. Atributos de Ubicación Geográfica
            ps.setString(5, peaje.getKilometro());
            ps.setDouble(6, peaje.getLatitud());
            ps.setDouble(7, peaje.getLongitud());
            ps.setString(8, peaje.getUbicacionDescripcion());
            ps.setString(9, peaje.getSentido());
            
            // 3. Atributos Operativos
            ps.setString(10, peaje.getEstadoOperativo());
            ps.setString(11, peaje.getNivel());
            ps.setString(12, peaje.getAceptaColpass());
            
            if (peaje.getFecha() != null) {
                ps.setDate(13, new java.sql.Date(peaje.getFecha().getTime()));
            } else {
                ps.setDate(13, null);
            }
            
            // 4. Atributos de Contacto y Referencia
            ps.setString(14, peaje.getTelefono());
            ps.setString(15, peaje.getTelefonoGrua());
            ps.setString(16, peaje.getUrlFicha());
            
            // 5. Llaves Foráneas (Relaciones)
            ps.setString(17, peaje.getIdRuta());
            setNullableInt(ps, 18, peaje.getIdConcesionario());
            setNullableInt(ps, 19, peaje.getIdSector());
            ps.setString(20, peaje.getIdDepartamento());

            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al insertar peaje: " + e.getMessage());
            return false;
        }
    }
    
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, java.sql.Types.NUMERIC);
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
