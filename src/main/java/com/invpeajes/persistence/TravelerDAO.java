package com.invpeajes.persistence;

import com.invpeajes.model.Traveler;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla VIAJERO (cuentas de los viajeros).
 * Implementa el CRUD genérico de {@link CrudDAO} y agrega la búsqueda por correo
 * y el cambio de contraseña.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class TravelerDAO implements CrudDAO<Traveler, Integer> {

    private static final String COLUMNS =
            "ID_VIAJERO, NOMBRE, APELLIDO, CORREO, TELEFONO, PLACA, CONTRASENA_HASH, FECHA_REGISTRO";
    private static final String SQL_INSERT =
            "INSERT INTO PEAJE.VIAJERO (NOMBRE, APELLIDO, CORREO, TELEFONO, PLACA, CONTRASENA_HASH) "
          + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT = "SELECT " + COLUMNS + " FROM PEAJE.VIAJERO ";
    private static final String SQL_UPDATE =
            "UPDATE PEAJE.VIAJERO SET NOMBRE = ?, APELLIDO = ?, CORREO = ?, TELEFONO = ?, PLACA = ? "
          + "WHERE ID_VIAJERO = ?";
    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE PEAJE.VIAJERO SET CONTRASENA_HASH = ? WHERE ID_VIAJERO = ?";
    private static final String SQL_DELETE = "DELETE FROM PEAJE.VIAJERO WHERE ID_VIAJERO = ?";

    @Override
    public void insert(Traveler traveler) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, new String[]{"ID_VIAJERO"})) {

            ps.setString(1, traveler.getFirstName());
            ps.setString(2, traveler.getLastName());
            ps.setString(3, traveler.getEmail());
            ps.setString(4, traveler.getPhone());
            ps.setString(5, traveler.getPlate());
            ps.setString(6, traveler.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    traveler.setTravelerId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el viajero " + traveler.getEmail(), e);
        }
    }

    @Override
    public Optional<Traveler> findById(Integer id) {
        List<Traveler> result = query(SQL_SELECT + "WHERE ID_VIAJERO = ?", id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Traveler> findAll() {
        return query(SQL_SELECT + "ORDER BY APELLIDO, NOMBRE", null);
    }

    @Override
    public boolean update(Traveler traveler) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, traveler.getFirstName());
            ps.setString(2, traveler.getLastName());
            ps.setString(3, traveler.getEmail());
            ps.setString(4, traveler.getPhone());
            ps.setString(5, traveler.getPlate());
            ps.setInt(6, traveler.getTravelerId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el viajero " + traveler.getTravelerId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el viajero " + id, e);
        }
    }

    /**
     * Busca un viajero por su correo (sin distinguir mayúsculas).
     * @param email Correo del viajero.
     * @return El viajero, o vacío si no existe.
     * @throws DataAccessException Si falla la consulta.
     */
    public Optional<Traveler> findByEmail(String email) {
        List<Traveler> result = query(SQL_SELECT + "WHERE LOWER(CORREO) = LOWER(?)", email);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Cambia la contraseña de un viajero.
     * @param travelerId ID del viajero.
     * @param passwordHash Hash de la nueva contraseña.
     * @return true si se actualizó.
     * @throws DataAccessException Si falla la actualización.
     */
    public boolean updatePassword(int travelerId, String passwordHash) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {

            ps.setString(1, passwordHash);
            ps.setInt(2, travelerId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar la contraseña del viajero " + travelerId, e);
        }
    }

    private List<Traveler> query(String sql, Object param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (param != null) {
                ps.setObject(1, param);
            }
            List<Traveler> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los viajeros", e);
        }
    }

    private static Traveler map(ResultSet rs) throws SQLException {
        Date registered = rs.getDate("FECHA_REGISTRO");
        return new Traveler(rs.getInt("ID_VIAJERO"), rs.getString("NOMBRE"), rs.getString("APELLIDO"),
                rs.getString("CORREO"), rs.getString("TELEFONO"), rs.getString("PLACA"),
                rs.getString("CONTRASENA_HASH"), registered == null ? null : registered.toLocalDate());
    }
}
