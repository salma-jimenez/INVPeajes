
package com.invpeajes.persistence;

import com.invpeajes.model.Peaje;
import java.util.List;

/**
 * Interfaz que define las operaciones del patrón DAO para la entidad Peaje.
 * Aplica el principio de Inversión de Dependencias (DIP).
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public interface IPeajeDAO {
    boolean insertar(Peaje peaje);
    Peaje obtenerPorId(String idPeaje);
    List<Peaje> listarTodos();
    boolean actualizar(Peaje peaje);
    boolean eliminar(String idPeaje);
}