package com.puntoventa.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CarritoTest {

    private static Producto producto(int id, String precio, int existencias) {
        return new Producto(id, "C" + id, "Producto " + id, "General", new BigDecimal(precio), existencias, 5, true);
    }

    @Test
    void calculaSubtotalYTotalSinDescuento() {
        Carrito c = new Carrito();
        c.agregar(producto(1, "10.00", 10), 2);
        c.agregar(producto(2, "5.50", 10), 1);
        Totales t = c.totales(BigDecimal.ZERO);
        assertEquals(new BigDecimal("25.50"), t.subtotal());
        assertEquals(new BigDecimal("25.50"), t.total());
    }

    @Test
    void descuentoDiezPorCientoDeVeinticincoCincuentaEsDosCincuentaYCinco() {
        Carrito c = new Carrito();
        c.agregar(producto(1, "10.00", 10), 2);
        c.agregar(producto(2, "5.50", 10), 1);
        Totales t = c.totales(BigDecimal.TEN);
        assertEquals(new BigDecimal("2.55"), t.descuento());
        assertEquals(new BigDecimal("22.95"), t.total());
    }

    @Test
    void agregarElMismoProductoSumaCantidades() {
        Carrito c = new Carrito();
        Producto p = producto(1, "10.00", 10);
        c.agregar(p, 1);
        c.agregar(p, 2);
        assertEquals(1, c.lineas().size());
        assertEquals(3, c.lineas().get(0).cantidad());
    }

    @Test
    void rechazaCantidadMayorALasExistencias() {
        Carrito c = new Carrito();
        Producto p = producto(1, "10.00", 3);
        c.agregar(p, 3);
        assertThrows(NegocioException.class, () -> c.agregar(p, 1));
    }

    @Test
    void rechazaCantidadNoPositivaYProductoInactivo() {
        Carrito c = new Carrito();
        assertThrows(NegocioException.class, () -> c.agregar(producto(1, "1.00", 5), 0));
        Producto inactivo = new Producto(2, "C2", "P2", "General", BigDecimal.ONE, 5, 5, false);
        assertThrows(NegocioException.class, () -> c.agregar(inactivo, 1));
    }

    @Test
    void descuentoFueraDeRangoSeRechaza() {
        Carrito c = new Carrito();
        c.agregar(producto(1, "10.00", 10), 1);
        assertThrows(NegocioException.class, () -> c.totales(new BigDecimal("-1")));
        assertThrows(NegocioException.class, () -> c.totales(new BigDecimal("100.01")));
    }

    @Test
    void cambiarCantidadACeroQuitaElRenglon() {
        Carrito c = new Carrito();
        c.agregar(producto(1, "10.00", 10), 2);
        c.cambiarCantidad(0, 0);
        assertTrue(c.estaVacio());
    }
}
