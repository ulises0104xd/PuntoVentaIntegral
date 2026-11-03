package com.puntoventa.service;

import static com.puntoventa.service.BaseDatosPrueba.CONTRASENA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.puntoventa.model.Agrupacion;
import com.puntoventa.model.Carrito;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.model.Venta;
import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** CU07 (usuarios), CU12 y CU13 (reportes) contra MySQL; se omiten si no hay base de datos. */
class UsuarioReporteIntegracionTest {

    private static boolean hayBd;
    private static Usuario admin;
    private static Usuario cajero;

    private final UsuarioService usuarios = new UsuarioService();
    private final ReporteService reportes = new ReporteService();
    private final AuthService auth = new AuthService();

    @BeforeAll
    static void preparar() {
        hayBd = BaseDatosPrueba.disponible();
        if (hayBd) {
            admin = BaseDatosPrueba.crearUsuario(Rol.ADMIN);
            cajero = BaseDatosPrueba.crearUsuario(Rol.CAJERO);
        }
    }

    private static String nombreNuevo() {
        return "u_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------------- CU07 ----------------
    @Test
    void cu07_crearUsuarioPermiteIniciarSesionYGuardaHash() {
        assumeTrue(hayBd, "Sin base de datos");
        String nombre = nombreNuevo();
        usuarios.crear(admin, nombre, "Usuario nuevo", "secreto1".toCharArray(), Rol.CAJERO);
        assertTrue(auth.login(nombre, "secreto1".toCharArray()).isPresent());
        assertFalse(auth.login(nombre, "secreto2".toCharArray()).isPresent());
        assertEquals(0, BaseDatosPrueba.escalar("SELECT COUNT(*) FROM usuarios WHERE username = '" + nombre
                + "' AND password_hash LIKE '%secreto1%'"));
        assertThrows(NegocioException.class,
                () -> usuarios.crear(admin, nombre, "Duplicado", "secreto1".toCharArray(), Rol.CAJERO));
    }

    @Test
    void cu07_usuarioDesactivadoNoPuedeIniciarSesionYPuedeReactivarse() {
        assumeTrue(hayBd, "Sin base de datos");
        String nombre = nombreNuevo();
        usuarios.crear(admin, nombre, "Usuario", "secreto1".toCharArray(), Rol.CAJERO);
        Usuario creado = usuarios.listar(admin).stream().filter(u -> u.username().equals(nombre)).findFirst().orElseThrow();

        usuarios.cambiarEstado(admin, creado.id(), false);
        assertFalse(auth.login(nombre, "secreto1".toCharArray()).isPresent());
        usuarios.cambiarEstado(admin, creado.id(), true);
        assertTrue(auth.login(nombre, "secreto1".toCharArray()).isPresent());
    }

    @Test
    void cu07_nadiePuedeDesactivarseASiMismo() {
        assumeTrue(hayBd, "Sin base de datos");
        assertThrows(NegocioException.class, () -> usuarios.cambiarEstado(admin, admin.id(), false));
        assertTrue(auth.login(admin.username(), CONTRASENA.toCharArray()).isPresent());
    }

    @Test
    void cu07_cambiarContrasenaInvalidaLaAnterior() {
        assumeTrue(hayBd, "Sin base de datos");
        String nombre = nombreNuevo();
        usuarios.crear(admin, nombre, "Usuario", "secreto1".toCharArray(), Rol.CAJERO);
        Usuario creado = usuarios.listar(admin).stream().filter(u -> u.username().equals(nombre)).findFirst().orElseThrow();
        assertThrows(NegocioException.class, () -> usuarios.cambiarContrasena(admin, creado.id(), "corta".toCharArray()));
        usuarios.cambiarContrasena(admin, creado.id(), "nueva-clave".toCharArray());
        assertFalse(auth.login(nombre, "secreto1".toCharArray()).isPresent());
        assertTrue(auth.login(nombre, "nueva-clave".toCharArray()).isPresent());
    }

    @Test
    void cu07_elCajeroNoPuedeAdministrarUsuarios() {
        assumeTrue(hayBd, "Sin base de datos");
        assertThrows(NegocioException.class,
                () -> usuarios.crear(cajero, nombreNuevo(), "X", "secreto1".toCharArray(), Rol.ADMIN));
        assertThrows(NegocioException.class, () -> usuarios.cambiarEstado(cajero, admin.id(), false));
    }

    // ---------------- CU12 y CU13 ----------------
    @Test
    void cu12_cu13_reportesReflejanUnaVentaRecienRegistrada() {
        assumeTrue(hayBd, "Sin base de datos");
        // Cantidad creciente para quedar siempre en el primer lugar de "más vendidos" aunque la base acumule datos.
        int cantidad = (int) (System.currentTimeMillis() / 1000 % 1_000_000) + 1000;
        Producto p = BaseDatosPrueba.crearProducto("1.00", cantidad + 5);
        Carrito carrito = new Carrito();
        carrito.agregar(p, cantidad);
        Venta venta = new VentaService().cobrar(carrito, BigDecimal.ZERO, cajero, null, MetodoPago.TARJETA, null);
        LocalDate hoy = LocalDate.now();

        for (Agrupacion a : Agrupacion.values()) {
            TablaDatos t = reportes.ventasPorPeriodo(admin, a, hoy, hoy);
            assertEquals(5, t.columnas().size());
            assertFalse(t.filas().isEmpty(), "Ventas por " + a);
        }
        TablaDatos porDia = reportes.ventasPorPeriodo(admin, Agrupacion.DIA, hoy, hoy);
        BigDecimal ingresosDelDia = (BigDecimal) porDia.filas().get(0)[4];
        assertTrue(ingresosDelDia.compareTo(venta.totales().total()) >= 0);

        TablaDatos masVendidos = reportes.masVendidos(admin, hoy, hoy);
        Object[] fila = masVendidos.filas().stream().filter(f -> p.codigo().equals(f[0])).findFirst().orElseThrow();
        assertEquals(cantidad, ((Number) fila[2]).intValue());
        assertEquals(0, new BigDecimal(cantidad).setScale(2).compareTo((BigDecimal) fila[3]));

        TablaDatos ingresos = reportes.ingresos(admin, hoy, hoy);
        Object[] total = ingresos.filas().get(ingresos.filas().size() - 1);
        assertEquals("TOTAL", total[0]);
        assertTrue(((Number) total[1]).intValue() >= 1);

        TablaDatos existencias = reportes.existencias(admin);
        Object[] filaProducto = existencias.filas().stream().filter(f -> p.codigo().equals(f[0])).findFirst().orElseThrow();
        assertEquals(5, ((Number) filaProducto[4]).intValue());
        assertEquals("BAJO", filaProducto[6], "5 existencias con mínimo 5");

        TablaDatos operaciones = reportes.operacionesPorUsuario(admin, cajero.id(), hoy, hoy);
        assertTrue(operaciones.filas().stream().anyMatch(f -> "VENTA".equals(f[2])
                && f[3].toString().startsWith("Venta #" + venta.id() + " ")));
        assertTrue(operaciones.filas().stream().allMatch(f -> cajero.username().equals(f[1])),
                "El filtro por usuario solo muestra sus operaciones");
        assertFalse(reportes.operacionesPorUsuario(admin, 0, hoy, hoy).filas().isEmpty());
    }

    @Test
    void cu12_rangoSinVentasDevuelveTablaVaciaYRangoInvalidoSeRechaza() {
        assumeTrue(hayBd, "Sin base de datos");
        LocalDate remoto = LocalDate.of(2000, 1, 1);
        assertTrue(reportes.ventasPorPeriodo(admin, Agrupacion.MES, remoto, remoto).filas().isEmpty());
        assertThrows(NegocioException.class,
                () -> reportes.ventasPorPeriodo(admin, Agrupacion.DIA, LocalDate.now(), remoto));
    }

    /** Regresión del defecto D01: los usuarios iniciales no se creaban si la tabla ya tenía otros usuarios. */
    @Test
    void d01_sembrarCreaAdminYCajeroAunqueYaExistanOtrosUsuarios() {
        assumeTrue(hayBd, "Sin base de datos");
        assertTrue(BaseDatosPrueba.escalar("SELECT COUNT(*) FROM usuarios") > 0, "Ya hay otros usuarios (los de prueba)");
        auth.sembrarUsuariosIniciales();
        auth.sembrarUsuariosIniciales(); // es idempotente
        assertTrue(auth.login("admin", "admin123".toCharArray()).isPresent());
        assertTrue(auth.login("cajero", "cajero123".toCharArray()).isPresent());
        assertEquals(1, BaseDatosPrueba.escalar("SELECT COUNT(*) FROM usuarios WHERE username = 'admin'"));
    }

    @Test
    void elCajeroNoPuedeConsultarReportes() {
        assumeTrue(hayBd, "Sin base de datos");
        LocalDate hoy = LocalDate.now();
        assertThrows(NegocioException.class, () -> reportes.masVendidos(cajero, hoy, hoy));
        assertThrows(NegocioException.class, () -> reportes.operacionesPorUsuario(cajero, 0, hoy, hoy));
    }
}
