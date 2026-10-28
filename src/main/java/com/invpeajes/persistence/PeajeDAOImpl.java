package com.invpeajes.persistence;

import com.invpeajes.ConexionBD;
import com.invpeajes.model.Peaje;
import com.invpeajes.model.PeajeElectronico;
import com.invpeajes.model.PeajeManual;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Peaje sobre Oracle (tabla PEAJE). Cumple con
 * el contrato definido en {@link IPeajeDAO}.
 *
 * @author Luis Guerrero
 * @version 2.0
 */
public class PeajeDAOImpl implements IPeajeDAO {

    private static final String COLUMNS
            = "ID_PEAJE, CODIGO_PEAJE, ID_ANI, NOMBRE, KILOMETRO, "
            + "LATITUD, LONGITUD, UBICACION_DESCRIPCION, SENTIDO, ESTADO_OPERATIVO, "
            + "NIVEL, ACEPTA_COLPASS, FECHA, TELEFONO, TELEFONO_GRUA, "
            + "URL_FICHA, ID_RUTA, ID_CONCESIONARIO, ID_SECTOR, ID_DEPARTAMENTO";

    private static final String SQL_INSERT
            = "INSERT INTO PEAJE (" + COLUMNS + ") "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM PEAJE";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + " WHERE ID_PEAJE = ?";

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY NOMBRE";

    private static final String SQL_FIND_BY_DEPARTMENT
            = SQL_SELECT + " WHERE ID_DEPARTAMENTO = ? ORDER BY NOMBRE";

    private static final String SQL_FIND_BY_STATUS
            = SQL_SELECT + " WHERE ESTADO_OPERATIVO = ? ORDER BY NOMBRE";

    private static final String SQL_UPDATE
            = "UPDATE PEAJE SET "
            + "CODIGO_PEAJE = ?, ID_ANI = ?, NOMBRE = ?, KILOMETRO = ?, "
            + "LATITUD = ?, LONGITUD = ?, UBICACION_DESCRIPCION = ?, SENTIDO = ?, "
            + "ESTADO_OPERATIVO = ?, NIVEL = ?, ACEPTA_COLPASS = ?, FECHA = ?, "
            + "TELEFONO = ?, TELEFONO_GRUA = ?, URL_FICHA = ?, ID_RUTA = ?, "
            + "ID_CONCESIONARIO = ?, ID_SECTOR = ?, ID_DEPARTAMENTO = ? "
            + "WHERE ID_PEAJE = ?";

    private static final String SQL_DELETE = "DELETE FROM PEAJE WHERE ID_PEAJE = ?";

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------
    /**
     * Inserta un nuevo peaje en la base de datos.
     *
     * @param peaje Peaje a guardar.
     * @throws DataAccessException Si falla la inserción.
     */
    @Override
    public void insert(Peaje peaje) {
        try (Connection conn = ConexionBD.conectar(); PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, peaje.getIdPeaje());
            setFields(ps, peaje, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el peaje " + peaje.getIdPeaje(), e);
        }
    }

    /**
     * Busca un peaje por su ID.
     *
     * @param idPeaje ID del peaje.
     * @return El peaje si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Peaje> findById(Integer idPeaje) {
        try (Connection conn = ConexionBD.conectar(); PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, idPeaje);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el peaje " + idPeaje, e);
        }
    }

    /**
     * Lista todos los peajes ordenados por nombre.
     *
     * @return Lista de peajes (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Peaje> findAll() {
        try (Connection conn = ConexionBD.conectar(); PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL); ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los peajes", e);
        }
    }

    /**
     * Actualiza todos los datos de un peaje existente.
     *
     * @param peaje Peaje con los datos nuevos.
     * @return true si se actualizó; false si no existe un peaje con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Peaje peaje) {
        try (Connection conn = ConexionBD.conectar(); PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, peaje, 1);
            ps.setInt(20, peaje.getIdPeaje());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el peaje " + peaje.getIdPeaje(), e);
        }
    }

    /**
     * Elimina un peaje por su ID.
     *
     * @param idPeaje ID del peaje.
     * @return true si se eliminó; false si no existe un peaje con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene
     * casetas asociadas).
     */
    @Override
    public boolean delete(Integer idPeaje) {
        try (Connection conn = ConexionBD.conectar(); PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idPeaje);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el peaje " + idPeaje, e);
        }
    }

    // ------------------------------------------------------------------
    // Consultas propias de Peaje
    // ------------------------------------------------------------------
    @Override
    public List<Peaje> findByDepartment(String idDepartamento) {
        return findByText(SQL_FIND_BY_DEPARTMENT, idDepartamento,
                "No se pudo listar los peajes del departamento " + idDepartamento);
    }

    @Override
    public List<Peaje> findByOperationalStatus(String estadoOperativo) {
        return findByText(SQL_FIND_BY_STATUS, estadoOperativo,
                "No se pudo listar los peajes con estado " + estadoOperativo);
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------
    private List<Peaje> findByText(String sql, String value, String operation) {
        try (Connection conn = ConexionBD.conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException(operation, e);
        }
    }

    /**
     * Asigna los 19 campos que no son la llave, empezando en el índice
     * {@code start}.
     */
    private void setFields(PreparedStatement ps, Peaje p, int start) throws SQLException {
        int i = start;
        ps.setString(i++, p.getCodigoPeaje());
        setNullableInt(ps, i++, p.getIdAni());
        ps.setString(i++, p.getNombrePeaje());
        ps.setString(i++, p.getKilometro());
        ps.setDouble(i++, p.getLatitud());
        ps.setDouble(i++, p.getLongitud());
        ps.setString(i++, p.getUbicacionDescripcion());
        ps.setString(i++, p.getSentido());
        ps.setString(i++, p.getEstadoOperativo());
        ps.setString(i++, p.getNivel());
        ps.setString(i++, p.getAceptaColpass());
        if (p.getFecha() != null) {
            ps.setDate(i++, new java.sql.Date(p.getFecha().getTime()));
        } else {
            ps.setNull(i++, Types.DATE);
        }
        ps.setString(i++, p.getTelefono());
        ps.setString(i++, p.getTelefonoGrua());
        ps.setString(i++, p.getUrlFicha());
        ps.setString(i++, p.getIdRuta());
        setNullableInt(ps, i++, p.getIdConcesionario());
        setNullableInt(ps, i++, p.getIdSector());
        ps.setString(i, p.getIdDepartamento());
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.NUMERIC);
        }
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private List<Peaje> mapList(ResultSet rs) throws SQLException {
        List<Peaje> peajes = new ArrayList<>();
        while (rs.next()) {
            peajes.add(mapRow(rs));
        }
        return peajes;
    }

    /**
     * Convierte la fila actual en un Peaje concreto (polimorfismo): si
     * ACEPTA_COLPASS = 'S' crea un PeajeElectronico; si no, un PeajeManual.
     */
    private Peaje mapRow(ResultSet rs) throws SQLException {
        int idPeaje = rs.getInt("ID_PEAJE");
        String codigoPeaje = rs.getString("CODIGO_PEAJE");
        Integer idAni = getNullableInt(rs, "ID_ANI");
        String nombre = rs.getString("NOMBRE");
        String kilometro = rs.getString("KILOMETRO");
        double latitud = rs.getDouble("LATITUD");
        double longitud = rs.getDouble("LONGITUD");
        String ubicacionDescripcion = rs.getString("UBICACION_DESCRIPCION");
        String sentido = rs.getString("SENTIDO");
        String estadoOperativo = rs.getString("ESTADO_OPERATIVO");
        String nivel = rs.getString("NIVEL");
        String aceptaColpass = rs.getString("ACEPTA_COLPASS");
        java.sql.Date fecha = rs.getDate("FECHA");
        String telefono = rs.getString("TELEFONO");
        String telefonoGrua = rs.getString("TELEFONO_GRUA");
        String urlFicha = rs.getString("URL_FICHA");
        String idRuta = rs.getString("ID_RUTA");
        Integer idConcesionario = getNullableInt(rs, "ID_CONCESIONARIO");
        Integer idSector = getNullableInt(rs, "ID_SECTOR");
        String idDepartamento = rs.getString("ID_DEPARTAMENTO");

        if ("S".equals(aceptaColpass)) {
            return new PeajeElectronico(idPeaje, codigoPeaje, idAni, nombre, kilometro,
                    latitud, longitud, ubicacionDescripcion, sentido, estadoOperativo, nivel,
                    fecha, telefono, telefonoGrua, urlFicha, idRuta, idConcesionario,
                    idSector, idDepartamento);
        }
        return new PeajeManual(idPeaje, codigoPeaje, idAni, nombre, kilometro,
                latitud, longitud, ubicacionDescripcion, sentido, estadoOperativo, nivel,
                fecha, telefono, telefonoGrua, urlFicha, idRuta, idConcesionario,
                idSector, idDepartamento);
    }
}
