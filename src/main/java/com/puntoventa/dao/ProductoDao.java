package com.puntoventa.dao;

import com.puntoventa.model.Producto;
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

public class ProductoDao {

    private static final String COLUMNAS = "id, codigo, nombre, categoria, precio, existencias, stock_minimo, activo";

    private static Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("categoria"), rs.getBigDecimal("precio"), rs.getInt("existencias"),
                rs.getInt("stock_minimo"), rs.getBoolean("activo"));
    }

    /** Productos cuyo código, nombre o categoría contienen el texto (CU02). */
    public List<Producto> buscar(String texto, boolean incluirInactivos) {
        String sql = "SELECT " + COLUMNAS + " FROM productos WHERE (codigo LIKE ? OR nombre LIKE ? OR categoria LIKE ?)"
                + (incluirInactivos ? "" : " AND activo = TRUE") + " ORDER BY nombre";
        String patron = "%" + texto.trim() + "%";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            return leerLista(ps);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar productos", e);
        }
    }

    public List<Producto> buscarActivos(String texto) {
        return buscar(texto, false);
    }

    /** Productos activos con existencias menores o iguales a su mínimo (CU11). */
    public List<Producto> bajoStock() {
        String sql = "SELECT " + COLUMNAS + " FROM productos WHERE activo = TRUE AND existencias <= stock_minimo "
                + "ORDER BY existencias, nombre";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            return leerLista(ps);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar las existencias bajas", e);
        }
    }

    private static List<Producto> leerLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Producto> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        }
    }

    public Optional<Producto> buscarActivoPorCodigo(String codigo) {
        String sql = "SELECT " + COLUMNAS + " FROM productos WHERE codigo = ? AND activo = TRUE";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo buscar el producto", e);
        }
    }

    /** Lee la fila y la bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE). */
    public Optional<Producto> buscarPorIdParaActualizar(Connection c, int id) throws SQLException {
        return leerUno(c, "SELECT " + COLUMNAS + " FROM productos WHERE id = ? FOR UPDATE", id);
    }

    public Optional<Producto> buscarPorCodigoParaActualizar(Connection c, String codigo) throws SQLException {
        return leerUno(c, "SELECT " + COLUMNAS + " FROM productos WHERE codigo = ? FOR UPDATE", codigo.trim());
    }

    private static Optional<Producto> leerUno(Connection c, String sql, Object parametro) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public int insertar(Connection c, Producto p) throws SQLException {
        String sql = "INSERT INTO productos (codigo, nombre, categoria, precio, existencias, stock_minimo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.codigo());
            ps.setString(2, p.nombre());
            ps.setString(3, p.categoria());
            ps.setBigDecimal(4, p.precio());
            ps.setInt(5, p.existencias());
            ps.setInt(6, p.stockMinimo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new NegocioException("Ya existe un producto con el código " + p.codigo());
        }
    }

    /** Actualiza los datos del producto; las existencias solo cambian mediante movimientos de inventario. */
    public void actualizar(Connection c, Producto p) throws SQLException {
        String sql = "UPDATE productos SET codigo = ?, nombre = ?, categoria = ?, precio = ?, stock_minimo = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.codigo());
            ps.setString(2, p.nombre());
            ps.setString(3, p.categoria());
            ps.setBigDecimal(4, p.precio());
            ps.setInt(5, p.stockMinimo());
            ps.setInt(6, p.id());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new NegocioException("Ya existe un producto con el código " + p.codigo());
        }
    }

    public void cambiarEstado(Connection c, int id, boolean activo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE productos SET activo = ? WHERE id = ?")) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void actualizarExistencias(Connection c, int id, int existencias) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE productos SET existencias = ? WHERE id = ?")) {
            ps.setInt(1, existencias);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
