package com.puntoventa.service;

import com.puntoventa.dao.UsuarioDao;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.Database;
import com.puntoventa.util.PasswordHasher;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/** Apoyo para las pruebas de integración: datos únicos por prueba contra la base configurada. */
public final class BaseDatosPrueba {

    public static final String CONTRASENA = "clave123";

    private BaseDatosPrueba() {
    }

    public static boolean disponible() {
        try (Connection ignorada = Database.getConnection()) {
            return true;
        } catch (RuntimeException | SQLException e) {
            return false;
        }
    }

    public static Usuario crearUsuario(Rol rol) {
        String username = rol.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        int id = Database.inTransaction(c -> new UsuarioDao().insertar(c, username, "Prueba " + rol,
                PasswordHasher.hash(CONTRASENA.toCharArray()), rol));
        return new Usuario(id, username, "Prueba " + rol, rol, true);
    }

    public static Producto crearProducto(String precio, int existencias) {
        String codigo = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO productos (codigo, nombre, categoria, precio, existencias) VALUES (?, ?, 'Prueba', ?, ?)")) {
                ps.setString(1, codigo);
                ps.setString(2, "Producto " + codigo);
                ps.setBigDecimal(3, new BigDecimal(precio));
                ps.setInt(4, existencias);
                ps.executeUpdate();
            }
            return null;
        });
        return new ProductoService().buscarPorCodigo(codigo).orElseThrow();
    }

    public static String codigoUnico() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static int escalar(String sql) {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int existencias(Producto p) {
        return escalar("SELECT existencias FROM productos WHERE id = " + p.id());
    }
}
