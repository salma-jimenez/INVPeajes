
package com.invpeajes.persistence;


import java.util.List;
import java.util.Optional;


/**
 * Contrato CRUD genérico para los DAO del módulo.
 *
 * @param <T> tipo de entidad
 * @param <K> tipo de la llave primaria
 * @author Salomejimenez
 */
public interface CrudDAO<T, K> {

    /**
     * Inserta una entidad nueva.
     *
     * @param entity entidad a guardar
     */
    void insert(T entity);

    /**
     * Busca una entidad por su llave primaria.
     *
     * @param id llave primaria
     * @return la entidad si existe
     */
    Optional<T> findById(K id);

    /**
     * Lista todas las entidades.
     *
     * @return lista (vacía si no hay registros)
     */
    List<T> findAll();

    /**
     * Actualiza una entidad existente.
     *
     * @param entity entidad con los datos nuevos
     * @return true si se actualizó algún registro
     */
    boolean update(T entity);

    /**
     * Elimina una entidad por su llave primaria.
     *
     * @param id llave primaria
     * @return true si se eliminó algún registro
     */
    boolean delete(K id);
    
}
