
package com.invpeajes.persistence;

import com.invpeajes.model.Peaje;
import java.util.List;

/**
 * Interfaz que define las operaciones del patrón DAO para la entidad Peaje.
 * Hereda el CRUD de {@link CrudDAO} y agrega solo las consultas propias de Peaje.
 * Los services dependen de esta interfaz, no de la implementación (Inversión de Dependencias).
 * 
 * @author Luis Guerrero
 * @version 2.0
 */
public interface IPeajeDAO extends CrudDAO<Peaje, Integer>{
    /**
     * Lista los peajes de un departamento.
     * @param idDepartamento Código del departamento (2 caracteres, ej. "20").
     * @return Peajes del departamento, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    List<Peaje> findByDepartment(String idDepartamento);
 
    /**
     * Lista los peajes según su estado operativo.
     * @param estadoOperativo OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA.
     * @return Peajes con ese estado, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    List<Peaje> findByOperationalStatus(String estadoOperativo);
}