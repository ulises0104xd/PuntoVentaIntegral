package com.puntoventa.dao;

import com.puntoventa.model.TablaDatos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InventarioDao {

    /** Registra un movimiento; la cantidad lleva signo (negativa cuando salen existencias). */
    public void registrarMovimiento(Connection c, int productoId, String tipo, int cantidad,
                                    int existenciasResultantes, String motivo, int usuarioId,
                                    Integer ventaId) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario (producto_id, tipo, cantidad, existencias_resultantes, "
                + "motivo, usuario_id, venta_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, tipo);
            ps.setInt(3, cantidad);
            ps.setInt(4, existenciasResultantes);
            ps.setString(5, motivo);
            ps.setInt(6, usuarioId);
            if (ventaId == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, ventaId);
            }
            ps.executeUpdate();
        }
    }

    /** Últimos movimientos (CU11); con código vacío se listan los de todos los productos. */
    public TablaDatos historial(String codigo, int limite) {
        String filtro = codigo == null ? "" : codigo.trim();
        return Consultas.consultar(
                "SELECT m.fecha AS `Fecha`, p.codigo AS `Código`, p.nombre AS `Producto`, m.tipo AS `Tipo`, "
                        + "m.cantidad AS `Cantidad`, m.existencias_resultantes AS `Existencia resultante`, "
                        + "m.motivo AS `Motivo`, u.username AS `Usuario` "
                        + "FROM movimientos_inventario m JOIN productos p ON p.id = m.producto_id "
                        + "JOIN usuarios u ON u.id = m.usuario_id "
                        + "WHERE (? = '' OR p.codigo = ?) ORDER BY m.fecha DESC, m.id DESC LIMIT " + limite,
                filtro, filtro);
    }
}
