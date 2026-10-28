package com.invpeajes.persistence;

import com.invpeajes.model.Caseta;
import java.util.List;

/**
 * Contrato DAO de la entidad Caseta.
 * Hereda el CRUD de {@link CrudDAO} y agrega solo las operaciones propias de Caseta.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public interface ICasetaDAO extends CrudDAO<Caseta, Integer> {

    /**
     * Lista las casetas de una estación de peaje.
     * @param idPeaje ID del peaje.
     * @return Casetas del peaje, ordenadas por número.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    List<Caseta> findByToll(int idPeaje);

    /**
     * Cambia solo el estado de una caseta.
     * @param idCaseta ID de la caseta.
     * @param estado ACTIVA, INACTIVA o MANTENIMIENTO.
     * @return true si se actualizó; false si no existe una caseta con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    boolean updateStatus(int idCaseta, String estado);
}