package com.puntoventa.dao;

import com.puntoventa.model.LineaVenta;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Totales;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class VentaDao {

    public int insertar(Connection c, LocalDateTime fecha, int usuarioId, Totales t, MetodoPago metodo,
                        BigDecimal recibido, BigDecimal cambio, Integer autorizadoPor) throws SQLException {
        String sql = "INSERT INTO ventas (fecha, usuario_id, subtotal, descuento_pct, descuento_monto, total, "
                + "metodo_pago, monto_recibido, cambio, autorizado_por) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(fecha));
            ps.setInt(2, usuarioId);
            ps.setBigDecimal(3, t.subtotal());
            ps.setBigDecimal(4, t.descuentoPct());
            ps.setBigDecimal(5, t.descuento());
            ps.setBigDecimal(6, t.total());
            ps.setString(7, metodo.name());
            ps.setBigDecimal(8, recibido);
            ps.setBigDecimal(9, cambio);
            if (autorizadoPor == null) {
                ps.setNull(10, java.sql.Types.INTEGER);
            } else {
                ps.setInt(10, autorizadoPor);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void insertarDetalle(Connection c, int ventaId, LineaVenta linea) throws SQLException {
        String sql = "INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ps.setInt(2, linea.producto().id());
            ps.setInt(3, linea.cantidad());
            ps.setBigDecimal(4, linea.producto().precio());
            ps.setBigDecimal(5, linea.subtotal());
            ps.executeUpdate();
        }
    }
}
