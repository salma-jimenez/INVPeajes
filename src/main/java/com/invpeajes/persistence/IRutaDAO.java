package com.invpeajes.persistence;

import com.invpeajes.model.Ruta;
import java.util.List;

/**
 * Contrato DAO de la entidad Ruta.
 * Hereda el CRUD de {@link CrudDAO} y agrega solo las consultas propias de Ruta.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public interface IRutaDAO extends CrudDAO<Ruta, String> {

    /**
     * Lista las rutas que inician o terminan en un municipio.
     * @param idMunicipio Código del municipio (5 caracteres, ej. "20001").
     * @return Rutas que pasan por ese municipio como origen o destino, ordenadas por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    List<Ruta> findByMunicipality(String idMunicipio);
}