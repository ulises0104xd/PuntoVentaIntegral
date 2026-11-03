package com.puntoventa.service;

import static com.puntoventa.service.BaseDatosPrueba.escalar;
import static com.puntoventa.service.BaseDatosPrueba.existencias;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.puntoventa.model.Carrito;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Módulos 2 y 4 contra MySQL: CU08, CU09, CU10 y CU11 (se omiten si no hay base de datos). */
class ProductoInventarioIntegracionTest {

    private static boolean hayBd;
    private static Usuario admin;
    private static Usuario cajero;

    private final ProductoService productos = new ProductoService();
    private final InventarioService inventario = new InventarioService();

    @BeforeAll
    static void preparar() {
        hayBd = BaseDatosPrueba.disponible();
        if (hayBd) {
            admin = BaseDatosPrueba.crearUsuario(Rol.ADMIN);
            cajero = BaseDatosPrueba.crearUsuario(Rol.CAJERO);
        }
    }

    @Test
    void cu08_crearProductoConExistenciaInicialRegistraMovimientoYBitacora() {
        assumeTrue(hayBd, "Sin base de datos");
        String codigo = BaseDatosPrueba.codigoUnico();
        Producto p = productos.crear(admin, codigo, "Producto nuevo", "", new BigDecimal("12.345"), 8, 3);

        assertEquals("General", p.categoria(), "Sin categoría se usa General");
        assertEquals(new BigDecimal("12.35"), p.precio(), "El precio se redondea a 2 decimales");
        assertEquals(8, existencias(p));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE tipo = 'ENTRADA' AND producto_id = "
                + p.id() + " AND existencias_resultantes = 8"));
        assertEquals(1, escalar("SELECT COUNT(*) FROM bitacora WHERE accion = 'PRODUCTO_CREADO' AND detalle LIKE '"
                + codigo + "%'"));
    }

    @Test
    void cu08_codigoDuplicadoSeRechazaYNoDejaMovimientos() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto existente = BaseDatosPrueba.crearProducto("5.00", 1);
        assertThrows(NegocioException.class,
                () -> productos.crear(admin, existente.codigo(), "Otro", "G", BigDecimal.ONE, 4, 1));
        assertEquals(1, escalar("SELECT COUNT(*) FROM productos WHERE codigo = '" + existente.codigo() + "'"));
        assertEquals(0, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE existencias_resultantes = 4 "
                + "AND producto_id = " + existente.id()));
    }

    @Test
    void cu08_editarCambiaPrecioPeroNoLasExistencias() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto p = BaseDatosPrueba.crearProducto("5.00", 7);
        productos.editar(admin, p.id(), p.codigo(), "Nombre editado", "Bebidas", new BigDecimal("9.99"), 2);
        Producto despues = productos.listar(p.codigo(), true).get(0);
        assertEquals("Nombre editado", despues.nombre());
        assertEquals(new BigDecimal("9.99"), despues.precio());
        assertEquals(7, despues.existencias());
        assertEquals(2, despues.stockMinimo());
    }

    @Test
    void cu08_productoDesactivadoNoSeVendeYSeConservaElHistorial() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto p = BaseDatosPrueba.crearProducto("5.00", 3);
        Carrito c = new Carrito();
        c.agregar(p, 1);
        new VentaService().cobrar(c, BigDecimal.ZERO, cajero, null, MetodoPago.TARJETA, null);

        productos.cambiarEstado(admin, p.id(), false);
        assertTrue(productos.buscarPorCodigo(p.codigo()).isEmpty(), "No aparece para la venta");
        assertTrue(productos.buscar(p.codigo()).isEmpty());
        assertEquals(1, productos.listar(p.codigo(), true).size(), "Sigue consultable con inactivos");
        assertEquals(1, escalar("SELECT COUNT(*) FROM detalle_venta WHERE producto_id = " + p.id()),
                "El historial de ventas se conserva (RN05)");

        Carrito vieja = new Carrito();
        assertThrows(NegocioException.class, () -> vieja.agregar(new Producto(p.id(), p.codigo(), p.nombre(),
                p.categoria(), p.precio(), 2, 5, false), 1));
        productos.cambiarEstado(admin, p.id(), true);
        assertTrue(productos.buscarPorCodigo(p.codigo()).isPresent());
    }

    @Test
    void cu09_entradaSumaExistenciasYRegistraMovimiento() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto p = BaseDatosPrueba.crearProducto("5.00", 2);
        int nuevas = inventario.registrarEntrada(admin, p.codigo(), 10, "Compra a proveedor");
        assertEquals(12, nuevas);
        assertEquals(12, existencias(p));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE tipo = 'ENTRADA' AND cantidad = 10 "
                + "AND existencias_resultantes = 12 AND producto_id = " + p.id()));
        assertThrows(NegocioException.class, () -> inventario.registrarEntrada(admin, p.codigo(), 0, "x"));
        assertThrows(NegocioException.class, () -> inventario.registrarEntrada(admin, "NOEXISTE" + p.codigo(), 1, "x"));
        assertEquals(12, existencias(p), "Una entrada rechazada no cambia nada");
    }

    @Test
    void cu10_ajusteRegistraLaDiferenciaConSignoYExigeMotivo() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto p = BaseDatosPrueba.crearProducto("5.00", 10);
        assertEquals(-3, inventario.registrarAjuste(admin, p.codigo(), 7, "Merma por caducidad"));
        assertEquals(7, existencias(p));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE tipo = 'AJUSTE' AND cantidad = -3 "
                + "AND existencias_resultantes = 7 AND producto_id = " + p.id()));

        assertThrows(NegocioException.class, () -> inventario.registrarAjuste(admin, p.codigo(), 7, "Igual"));
        assertThrows(NegocioException.class, () -> inventario.registrarAjuste(admin, p.codigo(), 5, " "));
        assertThrows(NegocioException.class, () -> inventario.registrarAjuste(admin, p.codigo(), -1, "Negativa"));
        assertEquals(7, existencias(p));
    }

    @Test
    void cu11_historialYExistenciasBajas() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto p = BaseDatosPrueba.crearProducto("5.00", 1); // el mínimo por defecto es 5: existencias bajas
        inventario.registrarEntrada(admin, p.codigo(), 4, "Reposición");
        TablaDatos historial = inventario.historial(admin, p.codigo());
        assertEquals(1, historial.filas().size());
        assertEquals("ENTRADA", historial.filas().get(0)[3]);
        assertEquals("Reposición", historial.filas().get(0)[6]);

        assertTrue(productos.bajoStock().stream().anyMatch(b -> b.id() == p.id()), "5 <= mínimo 5");
        inventario.registrarEntrada(admin, p.codigo(), 1, "Una más");
        assertFalse(productos.bajoStock().stream().anyMatch(b -> b.id() == p.id()), "6 > mínimo 5");
    }

    @Test
    void elCajeroNoPuedeModificarProductosNiInventarioAunqueLlameAlServicio() {
        assumeTrue(hayBd, "Sin base de datos");
        Producto p = BaseDatosPrueba.crearProducto("5.00", 3);
        assertThrows(NegocioException.class, () -> productos.cambiarEstado(cajero, p.id(), false));
        assertThrows(NegocioException.class, () -> inventario.registrarAjuste(cajero, p.codigo(), 0, "x"));
        assertThrows(NegocioException.class, () -> inventario.historial(cajero, ""));
        assertEquals(3, existencias(p));
        assertTrue(productos.buscarPorCodigo(p.codigo()).isPresent());
    }
}
