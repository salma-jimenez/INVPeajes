
package com.invpeajes.model;
import java.util.Date;

/**
 * Peaje que acepta pago electrónico con TAG Colpass, además del cobro en caseta.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class PeajeElectronico extends Peaje {

    /**
     * Crea un peaje electrónico. El indicador de Colpass se fija en "S".
     *
     * @param idPeaje Identificador interno de la estación.
     * @param codigoPeaje Código oficial de la estación.
     * @param idAni Identificador asignado por la ANI (puede ser null).
     * @param nombrePeaje Nombre comercial o geográfico.
     * @param kilometro Punto kilométrico en la ruta.
     * @param latitud Coordenada de latitud.
     * @param longitud Coordenada de longitud.
     * @param ubicacionDescripcion Descripción textual de la ubicación.
     * @param sentido Sentido del tráfico de la estación.
     * @param estadoOperativo Estado actual del peaje.
     * @param nivel Nivel operativo de la estación.
     * @param fecha Fecha de registro o actualización.
     * @param telefono Teléfono de atención de la estación.
     * @param telefonoGrua Teléfono de servicio de grúa.
     * @param urlFicha URL al documento de ficha técnica.
     * @param idRuta ID de la ruta (Llave foránea).
     * @param idConcesionario ID del concesionario (Llave foránea, puede ser null).
     * @param idSector ID del sector vial (Llave foránea, puede ser null).
     * @param idDepartamento ID del departamento (Llave foránea).
     */
    public PeajeElectronico(int idPeaje, String codigoPeaje, Integer idAni, String nombrePeaje,
                            String kilometro, double latitud, double longitud, String ubicacionDescripcion,
                            String sentido, String estadoOperativo, String nivel,
                            Date fecha, String telefono, String telefonoGrua, String urlFicha,
                            String idRuta, Integer idConcesionario, Integer idSector,
                            String idDepartamento) {
        super(idPeaje, codigoPeaje, idAni, nombrePeaje, kilometro, latitud, longitud,
              ubicacionDescripcion, sentido, estadoOperativo, nivel, "S", fecha,
              telefono, telefonoGrua, urlFicha, idRuta, idConcesionario, idSector, idDepartamento);
    }

    /**
     * Describe el estado del peaje indicando que acepta Colpass.
     * @return Texto con nombre, estado y tipo de cobro.
     */
    @Override
    public String reportarEstadoOperativo() {
        return "Peaje " + getNombrePeaje() + " [" + getEstadoOperativo()
             + "] - acepta TAG Colpass y cobro en caseta";
    }
}