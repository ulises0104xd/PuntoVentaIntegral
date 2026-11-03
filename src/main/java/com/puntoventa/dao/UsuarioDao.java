package com.puntoventa.dao;

import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.Database;
import com.puntoventa.util.DataAccessException;
import com.puntoventa.util.NegocioException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDao {

    /** Usuario junto con el hash almacenado; el hash nunca sale de la capa de acceso y autenticación. */
    public record Credencial(Usuario usuario, String hash) {
    }

    private static Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id"), rs.getString("username"), rs.getString("nombre"),
                Rol.valueOf(rs.getString("rol")), rs.getBoolean("activo"));
    }

    public Optional<Credencial> buscarPorUsername(String username) {
        String sql = "SELECT id, username, nombre, password_hash, rol, activo FROM usuarios WHERE username = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Credencial(mapear(rs), rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el usuario", e);
        }
    }

    public List<Usuario> listar() {
        String sql = "SELECT id, username, nombre, rol, activo FROM usuarios ORDER BY username";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los usuarios", e);
        }
    }

    public Optional<Usuario> buscarPorIdParaActualizar(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, username, nombre, rol, activo FROM usuarios WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public int contarAdministradoresActivos(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = TRUE")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int insertar(Connection c, String username, String nombre, String hash, Rol rol) throws SQLException {
        String sql = "INSERT INTO usuarios (username, nombre, password_hash, rol) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, nombre);
            ps.setString(3, hash);
            ps.setString(4, rol.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new NegocioException("Ya existe un usuario con el nombre " + username);
        }
    }

    public void cambiarEstado(Connection c, int id, boolean activo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET activo = ? WHERE id = ?")) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void actualizarPassword(Connection c, int id, String hash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
