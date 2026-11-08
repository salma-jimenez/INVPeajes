package com.invpeajes.persistence;

import com.invpeajes.model.Toll;
import com.invpeajes.model.ElectronicToll;
import com.invpeajes.model.ManualToll;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de Peaje sobre Oracle (tabla PEAJE). Implementa
 * el CRUD genérico de {@link CrudDAO} y agrega las consultas propias de Peaje.
 *
 * @author Luis Guerrero
 * @version 2.0
 */
public class TollDAO implements CrudDAO<Toll, Integer> {

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
     * @param toll Peaje a guardar.
     * @throws DataAccessException Si falla la inserción.
     */
    @Override
    public void insert(Toll toll) {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, toll.getTollId());
            setFields(ps, toll, 2);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el peaje " + toll.getTollId(), e);
        }
    }

    /**
     * Busca un peaje por su ID.
     *
     * @param tollId ID del peaje.
     * @return El peaje si existe, o {@code Optional.empty()}.
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public Optional<Toll> findById(Integer tollId) {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, tollId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el peaje " + tollId, e);
        }
    }

    /**
     * Lista todos los peajes ordenados por nombre.
     *
     * @return Lista de peajes (vacía si no hay registros).
     * @throws DataAccessException Si falla la consulta.
     */
    @Override
    public List<Toll> findAll() {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL); ResultSet rs = ps.executeQuery()) {

            return mapList(rs);

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los peajes", e);
        }
    }

    /**
     * Actualiza todos los datos de un peaje existente.
     *
     * @param toll Peaje con los datos nuevos.
     * @return true si se actualizó; false si no existe un peaje con ese ID.
     * @throws DataAccessException Si falla la actualización.
     */
    @Override
    public boolean update(Toll toll) {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setFields(ps, toll, 1);
            ps.setInt(20, toll.getTollId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el peaje " + toll.getTollId(), e);
        }
    }

    /**
     * Elimina un peaje por su ID.
     *
     * @param tollId ID del peaje.
     * @return true si se eliminó; false si no existe un peaje con ese ID.
     * @throws DataAccessException Si falla el borrado (por ejemplo, si tiene
     * casetas asociadas).
     */
    @Override
    public boolean delete(Integer tollId) {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, tollId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el peaje " + tollId, e);
        }
    }

    // ------------------------------------------------------------------
    // Consultas propias de Peaje
    // ------------------------------------------------------------------
    /**
     * Lista los peajes de un departamento.
     * @param departmentId Código del departamento (2 caracteres, ej. "20").
     * @return Peajes del departamento, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public List<Toll> findByDepartment(String departmentId) {
        return findByText(SQL_FIND_BY_DEPARTMENT, departmentId,
                "No se pudo listar los peajes del departamento " + departmentId);
    }

    /**
     * Lista los peajes según su estado operativo.
     * @param operationalStatus OPERATIVO, NO_OPERATIVO, SINIESTRADO o SIN_TARIFA.
     * @return Peajes con ese estado, ordenados por nombre.
     * @throws DataAccessException Si falla la consulta en la base de datos.
     */
    public List<Toll> findByOperationalStatus(String operationalStatus) {
        return findByText(SQL_FIND_BY_STATUS, operationalStatus,
                "No se pudo listar los peajes con estado " + operationalStatus);
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------
    private List<Toll> findByText(String sql, String value, String operation) {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

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
    private void setFields(PreparedStatement ps, Toll p, int start) throws SQLException {
        int i = start;
        ps.setString(i++, p.getCode());
        setNullableInt(ps, i++, p.getAniId());
        ps.setString(i++, p.getName());
        ps.setString(i++, p.getKilometer());
        ps.setDouble(i++, p.getLatitude());
        ps.setDouble(i++, p.getLongitude());
        ps.setString(i++, p.getLocationDescription());
        ps.setString(i++, p.getDirection());
        ps.setString(i++, p.getOperationalStatus());
        ps.setString(i++, p.getLevel());
        ps.setString(i++, p.getAcceptsColpass());
        if (p.getDate() != null) {
            ps.setDate(i++, new java.sql.Date(p.getDate().getTime()));
        } else {
            ps.setNull(i++, Types.DATE);
        }
        ps.setString(i++, p.getPhone());
        ps.setString(i++, p.getTowPhone());
        ps.setString(i++, p.getInfoSheetUrl());
        ps.setString(i++, p.getRouteId());
        setNullableInt(ps, i++, p.getConcessionaireId());
        setNullableInt(ps, i++, p.getSectorId());
        ps.setString(i, p.getDepartmentId());
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

    private List<Toll> mapList(ResultSet rs) throws SQLException {
        List<Toll> tolls = new ArrayList<>();
        while (rs.next()) {
            tolls.add(mapRow(rs));
        }
        return tolls;
    }

    /**
     * Convierte la fila actual en un Peaje concreto (polimorfismo): si
     * ACEPTA_COLPASS = 'S' crea un PeajeElectronico; si no, un PeajeManual.
     */
    private Toll mapRow(ResultSet rs) throws SQLException {
        int tollId = rs.getInt("ID_PEAJE");
        String code = rs.getString("CODIGO_PEAJE");
        Integer aniId = getNullableInt(rs, "ID_ANI");
        String name = rs.getString("NOMBRE");
        String kilometer = rs.getString("KILOMETRO");
        double latitude = rs.getDouble("LATITUD");
        double longitude = rs.getDouble("LONGITUD");
        String locationDescription = rs.getString("UBICACION_DESCRIPCION");
        String direction = rs.getString("SENTIDO");
        String operationalStatus = rs.getString("ESTADO_OPERATIVO");
        String level = rs.getString("NIVEL");
        String acceptsColpass = rs.getString("ACEPTA_COLPASS");
        java.sql.Date date = rs.getDate("FECHA");
        String phone = rs.getString("TELEFONO");
        String towPhone = rs.getString("TELEFONO_GRUA");
        String infoSheetUrl = rs.getString("URL_FICHA");
        String routeId = rs.getString("ID_RUTA");
        Integer concessionaireId = getNullableInt(rs, "ID_CONCESIONARIO");
        Integer sectorId = getNullableInt(rs, "ID_SECTOR");
        String departmentId = rs.getString("ID_DEPARTAMENTO");

        if ("S".equals(acceptsColpass)) {
            return new ElectronicToll(tollId, code, aniId, name, kilometer,
                    latitude, longitude, locationDescription, direction, operationalStatus, level,
                    date, phone, towPhone, infoSheetUrl, routeId, concessionaireId,
                    sectorId, departmentId);
        }
        return new ManualToll(tollId, code, aniId, name, kilometer,
                latitude, longitude, locationDescription, direction, operationalStatus, level,
                date, phone, towPhone, infoSheetUrl, routeId, concessionaireId,
                sectorId, departmentId);
    }
}
