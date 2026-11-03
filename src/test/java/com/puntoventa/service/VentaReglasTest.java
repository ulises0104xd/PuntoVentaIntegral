package com.puntoventa.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Reglas RN03 (descuentos) y RN04 (pago), sin base de datos. */
class VentaReglasTest {

    private static final Usuario CAJERO = new Usuario(2, "cajero", "Cajero", Rol.CAJERO, true);
    private static final Usuario ADMIN = new Usuario(1, "admin", "Admin", Rol.ADMIN, true);

    @Test
    void cajeroPuedeAplicarHastaDiezPorCiento() {
        assertNull(VentaService.validarDescuento(new BigDecimal("10"), CAJERO, null));
        assertNull(VentaService.validarDescuento(BigDecimal.ZERO, CAJERO, null));
    }

    @Test
    void cajeroNecesitaAdministradorParaMasDeDiezPorCiento() {
        BigDecimal veinte = new BigDecimal("20");
        assertThrows(NegocioException.class, () -> VentaService.validarDescuento(veinte, CAJERO, null));
        assertThrows(NegocioException.class, () -> VentaService.validarDescuento(veinte, CAJERO, CAJERO));
        assertEquals(1, VentaService.validarDescuento(veinte, CAJERO, ADMIN));
    }

    @Test
    void administradorNoNecesitaAutorizacion() {
        assertNull(VentaService.validarDescuento(new BigDecimal("50"), ADMIN, null));
    }

    @Test
    void efectivoCalculaElCambio() {
        BigDecimal[] pago = VentaService.calcularPago(new BigDecimal("22.95"), MetodoPago.EFECTIVO, new BigDecimal("50"));
        assertArrayEquals(new BigDecimal[]{new BigDecimal("50.00"), new BigDecimal("27.05")}, pago);
    }

    @Test
    void efectivoMenorAlTotalSeRechaza() {
        assertThrows(NegocioException.class,
                () -> VentaService.calcularPago(new BigDecimal("22.95"), MetodoPago.EFECTIVO, new BigDecimal("20")));
    }

    @Test
    void tarjetaNoTieneCambio() {
        BigDecimal[] pago = VentaService.calcularPago(new BigDecimal("22.95"), MetodoPago.TARJETA, null);
        assertEquals(new BigDecimal("22.95"), pago[0]);
        assertEquals(0, pago[1].signum());
    }
}
