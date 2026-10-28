package com.invpeajes.persistence;

import com.invpeajes.model.Sector;
import java.util.List;

/**
 * Contrato DAO de la entidad Sector.
 * Hereda el CRUD de {@link CrudDAO} y agrega solo las consultas propias de Sector.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public interface ISectorDAO extends CrudDAO<Sector, Integer> {

    /**
     * Busca sectores cuyo nombre contenga un texto (sin distinguir mayúsculas).
     * @param text Texto a buscar (ej. "armenia").
     * @return Sectores que coinciden, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    List<Sector> findByNameContaining(String text);
}