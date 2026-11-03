package com.puntoventa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.puntoventa.dao.UsuarioDao;
import com.puntoventa.model.Carrito;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.model.Venta;
import com.puntoventa.util.Database;
import com.puntoventa.util.NegocioException;
import com.puntoventa.util.PasswordHasher;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Criterios CA1, CA2, CA3 y CA5 del prototipo contra MySQL. Se omiten si no hay conexión.
 * Usar una base de datos de pruebas: DB_URL=jdbc:mysql://localhost:3306/punto_venta_test ...
 */
class VentaIntegracionTest {

    private static boolean hayBaseDeDatos;
    private static Usuario cajero;
    private static Usuario admin;

    private final VentaService ventas = new VentaService();
    private final ProductoService productos = new ProductoService();

    @BeforeAll
    static void prepararUsuarios() {
        try (Connection ignorada = Database.getConnection()) {
            hayBaseDeDatos = true;
        } catch (RuntimeException | SQLException e) {
            hayBaseDeDatos = false;
            return;
        }
        cajero = crearUsuario(Rol.CAJERO);
        admin = crearUsuario(Rol.ADMIN);
    }

    private static Usuario crearUsuario(Rol rol) {
        String username = rol.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        int id = Database.inTransaction(c -> new UsuarioDao().insertar(c, username, "Prueba " + rol,
                PasswordHasher.hash("clave123".toCharArray()), rol));
        return new Usuario(id, username, "Prueba " + rol, rol, true);
    }

    private Producto crearProducto(String precio, int existencias) {
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
        return productos.buscarPorCodigo(codigo).orElseThrow();
    }

    private static int existencias(Producto p) {
        return escalar("SELECT existencias FROM productos WHERE id = " + p.id());
    }

    private static int escalar(String sql) {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void ca2_ventaGuardaDetalleMovimientosYDescuentaExistencias() {
        assumeTrue(hayBaseDeDatos, "Sin base de datos");
        Producto a = crearProducto("10.00", 5);
        Producto b = crearProducto("5.50", 5);
        Carrito carrito = new Carrito();
        carrito.agregar(a, 2);
        carrito.agregar(b, 1);

        Venta venta = ventas.cobrar(carrito, BigDecimal.TEN, cajero, null, MetodoPago.EFECTIVO, new BigDecimal("50"));

        assertEquals(new BigDecimal("22.95"), venta.totales().total());
        assertEquals(new BigDecimal("27.05"), venta.cambio());
        assertEquals(3, existencias(a));
        assertEquals(4, existencias(b));
        assertEquals(2, escalar("SELECT COUNT(*) FROM detalle_venta WHERE venta_id = " + venta.id()));
        assertEquals(2, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE tipo = 'VENTA' AND venta_id = " + venta.id()));
        assertEquals(1, escalar("SELECT COUNT(*) FROM bitacora WHERE accion = 'VENTA' AND usuario_id = " + cajero.id()
                + " AND detalle LIKE 'Venta #" + venta.id() + " %'"));
    }

    @Test
    void ca3_siFallaUnRenglonNoQuedaNingunDatoParcial() {
        assumeTrue(hayBaseDeDatos, "Sin base de datos");
        Producto bueno = crearProducto("10.00", 5);
        Producto escaso = crearProducto("10.00", 5);
        Carrito carrito = new Carrito();
        carrito.agregar(bueno, 1);
        carrito.agregar(escaso, 5);
        // Otro usuario deja el producto escaso sin existencias antes de cobrar.
        Carrito ajeno = new Carrito();
        ajeno.agregar(escaso, 5);
        ventas.cobrar(ajeno, BigDecimal.ZERO, cajero, null, MetodoPago.TARJETA, null);
        int ventasAntes = escalar("SELECT COUNT(*) FROM ventas");

        assertThrows(NegocioException.class,
                () -> ventas.cobrar(carrito, BigDecimal.ZERO, cajero, null, MetodoPago.TARJETA, null));

        assertEquals(5, existencias(bueno), "El primer renglón no debe quedar descontado");
        assertEquals(0, existencias(escaso));
        assertEquals(ventasAntes, escalar("SELECT COUNT(*) FROM ventas"), "No debe quedar venta parcial");
        assertEquals(0, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE producto_id = " + bueno.id()));
    }

    @Test
    void ca4_descuentoMayorAlLimiteGuardaQuienAutorizo() {
        assumeTrue(hayBaseDeDatos, "Sin base de datos");
        Producto p = crearProducto("100.00", 5);
        Carrito carrito = new Carrito();
        carrito.agregar(p, 1);
        assertThrows(NegocioException.class,
                () -> ventas.cobrar(carrito, new BigDecimal("20"), cajero, null, MetodoPago.TARJETA, null));
        assertEquals(5, existencias(p));

        Venta venta = ventas.cobrar(carrito, new BigDecimal("20"), cajero, admin, MetodoPago.TARJETA, null);
        assertEquals(new BigDecimal("80.00"), venta.totales().total());
        assertEquals(admin.id(), escalar("SELECT autorizado_por FROM ventas WHERE id = " + venta.id()));
    }

    @Test
    void ca5_dosVentasSimultaneasDelUltimoProductoSoloConcretaUna() throws Exception {
        assumeTrue(hayBaseDeDatos, "Sin base de datos");
        Producto ultimo = crearProducto("10.00", 1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> venta = () -> {
                Carrito c = new Carrito();
                c.agregar(ultimo, 1);
                try {
                    ventas.cobrar(c, BigDecimal.ZERO, cajero, null, MetodoPago.TARJETA, null);
                    return true;
                } catch (NegocioException e) {
                    return false;
                }
            };
            Future<Boolean> f1 = pool.submit(venta);
            Future<Boolean> f2 = pool.submit(venta);
            boolean r1 = f1.get();
            boolean r2 = f2.get();
            assertTrue(r1 ^ r2, "Exactamente una venta debe concretarse");
            assertEquals(0, existencias(ultimo));
            assertEquals(1, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE producto_id = " + ultimo.id()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void ca1_loginConContrasenaCorrectaEIncorrectaYSinTextoPlanoEnLaBase() {
        assumeTrue(hayBaseDeDatos, "Sin base de datos");
        AuthService auth = new AuthService();
        assertTrue(auth.login(cajero.username(), "clave123".toCharArray()).isPresent());
        assertFalse(auth.login(cajero.username(), "otra".toCharArray()).isPresent());
        assertFalse(auth.login("no_existe_" + UUID.randomUUID(), "clave123".toCharArray()).isPresent());
        assertFalse(auth.autorizarAdministrador(cajero.username(), "clave123".toCharArray()).isPresent(),
                "Un cajero no puede autorizar descuentos");
        assertTrue(auth.autorizarAdministrador(admin.username(), "clave123".toCharArray()).isPresent());
        assertEquals(0, escalar("SELECT COUNT(*) FROM usuarios WHERE password_hash = 'clave123'"));
        assertEquals(0, escalar("SELECT COUNT(*) FROM usuarios WHERE password_hash NOT LIKE 'pbkdf2_sha256$%'"));
    }
}
