package com.invpeajes.persistence;

import com.invpeajes.model.Intermediador;
import java.util.Optional;

/**
 * Contrato DAO de la entidad Intermediador.
 * Hereda el CRUD de {@link CrudDAO} y agrega solo las consultas propias de Intermediador.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public interface IIntermediadorDAO extends CrudDAO<Intermediador, Integer> {

    /**
     * Busca un intermediador por su nombre, sin distinguir mayúsculas.
     * @param nombre Nombre del intermediador (ej. "flypass").
     * @return El intermediador si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    Optional<Intermediador> findByName(String nombre);
}