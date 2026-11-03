package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BitacoraDao {

    public void registrar(Connection c, int usuarioId, String accion, String detalle) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO bitacora (usuario_id, accion, detalle) VALUES (?, ?, ?)")) {
            ps.setInt(1, usuarioId);
            ps.setString(2, accion);
            ps.setString(3, detalle.length() > 255 ? detalle.substring(0, 255) : detalle);
            ps.executeUpdate();
        }
    }
}
