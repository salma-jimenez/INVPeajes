
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
                   + "URL_FICHA, ID_RUTA, ID_CONCESIONARIO, ID_SECTOR, ID_DEPARTAMENTO, ID_MUNICIPIOS"
                   + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                   
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // 1. Atributos de Identificación
            ps.setString(1, peaje.getIdPeaje());
            ps.setString(2, peaje.getCodigoPeaje());
            ps.setString(3, peaje.getIdAni());
            ps.setString(4, peaje.getNombrePeaje());
            
            // 2. Atributos de Ubicación Geográfica
            ps.setString(5, peaje.getKilometro());
            ps.setString(6, peaje.getLatitud());
            ps.setString(7, peaje.getLongitud());
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
            ps.setString(18, peaje.getIdConcesionario());
            ps.setString(19, peaje.getIdSector());
            ps.setString(20, peaje.getIdDepartamento());
            ps.setString(21, peaje.getIdMunicipios());

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
