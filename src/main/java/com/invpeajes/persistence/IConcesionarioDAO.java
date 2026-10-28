package com.invpeajes.persistence;

import com.invpeajes.model.Concesionario;
import java.util.List;
import java.util.Optional;

/**
 * Contrato DAO de la entidad Concesionario.
 * Hereda el CRUD de {@link CrudDAO} y agrega solo las consultas propias de Concesionario.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public interface IConcesionarioDAO extends CrudDAO<Concesionario, Integer> {

    /**
     * Busca un concesionario por su nombre exacto (el nombre es único en la BD).
     * @param nombre Nombre del concesionario.
     * @return El concesionario si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    Optional<Concesionario> findByName(String nombre);

    /**
     * Lista los concesionarios de un tipo.
     * @param tipoConcesionario INVIAS, CONCESION u OTRO.
     * @return Concesionarios de ese tipo, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    List<Concesionario> findByType(String tipoConcesionario);
}