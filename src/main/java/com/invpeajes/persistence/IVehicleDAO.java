package com.invpeajes.persistence;

import com.invpeajes.model.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Salomejimenez Contrato de acceso a datos para los vehículos.
 */
public interface IVehicleDAO extends CrudDAO<Vehicle, String> {

    /**
     * Busca el vehículo asociado a un código de tag COLPASS.
     *
     * @param tagCode código del tag
     * @return el vehículo si existe
     */
    Optional<Vehicle> findByTag(String tagCode);

    /**
     * Lista los vehículos de una categoría.
     *
     * @param categoryId categoría a filtrar
     * @return vehículos de esa categoría
     */
    List<Vehicle> findByCategory(int categoryId);

    /**
     * Asigna o retira el tag COLPASS de un vehículo.
     *
     * @param plate placa del vehículo
     * @param tagCode código del tag (null para retirarlo)
     * @param intermediaryId intermediador emisor (null para retirarlo)
     * @return true si se actualizó el vehículo
     */
    boolean updateTag(String plate, String tagCode, Integer intermediaryId);

}
