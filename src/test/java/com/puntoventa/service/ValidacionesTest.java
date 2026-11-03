package com.puntoventa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.puntoventa.model.Rol;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias (sin base de datos) de validaciones, permisos y exportación. */
class ValidacionesTest {

    private static final Usuario ADMIN = new Usuario(1, "admin", "Admin", Rol.ADMIN, true);
    private static final Usuario CAJERO = new Usuario(2, "cajero", "Cajero", Rol.CAJERO, true);

    // ---- Productos (RN01, RN02, CU08)
    @Test
    void productoValidoSePermite() {
        assertDoesNotThrow(() -> ProductoService.validar("P1", "Nombre", new BigDecimal("10.50"), 0, 5));
    }

    @Test
    void productoSinCodigoONombreSeRechaza() {
        assertThrows(NegocioException.class, () -> ProductoService.validar(" ", "N", BigDecimal.ONE, 0, 0));
        assertThrows(NegocioException.class, () -> ProductoService.validar("P1", "", BigDecimal.ONE, 0, 0));
    }

    @Test
    void productoConValoresNegativosSeRechaza() {
        assertThrows(NegocioException.class, () -> ProductoService.validar("P1", "N", new BigDecimal("-1"), 0, 0));
        assertThrows(NegocioException.class, () -> ProductoService.validar("P1", "N", BigDecimal.ONE, -1, 0));
        assertThrows(NegocioException.class, () -> ProductoService.validar("P1", "N", BigDecimal.ONE, 0, -1));
    }

    @Test
    void codigoMuyLargoSeRechaza() {
        assertThrows(NegocioException.class, () -> ProductoService.validar("X".repeat(31), "N", BigDecimal.ONE, 0, 0));
    }

    // ---- Inventario (RN02, RN08, CU09, CU10)
    @Test
    void entradaDebeSerPositiva() {
        assertDoesNotThrow(() -> InventarioService.validarEntrada(1));
        assertThrows(NegocioException.class, () -> InventarioService.validarEntrada(0));
        assertThrows(NegocioException.class, () -> InventarioService.validarEntrada(-3));
    }

    @Test
    void ajusteExigeMotivoYExistenciaNoNegativa() {
        assertDoesNotThrow(() -> InventarioService.validarAjuste(0, "Merma"));
        assertThrows(NegocioException.class, () -> InventarioService.validarAjuste(5, " "));
        assertThrows(NegocioException.class, () -> InventarioService.validarAjuste(5, null));
        assertThrows(NegocioException.class, () -> InventarioService.validarAjuste(-1, "Merma"));
    }

    // ---- Usuarios (RN07)
    @Test
    void usuarioNuevoValidoSePermite() {
        assertDoesNotThrow(() -> UsuarioService.validarNuevo("ana.lopez", "Ana López", "secreto".toCharArray(), Rol.CAJERO));
    }

    @Test
    void usuarioConNombreInvalidoOContrasenaCortaSeRechaza() {
        assertThrows(NegocioException.class, () -> UsuarioService.validarNuevo("a", "Ana", "secreto".toCharArray(), Rol.CAJERO));
        assertThrows(NegocioException.class, () -> UsuarioService.validarNuevo("ana lopez", "Ana", "secreto".toCharArray(), Rol.CAJERO));
        assertThrows(NegocioException.class, () -> UsuarioService.validarNuevo("ana", "", "secreto".toCharArray(), Rol.CAJERO));
        assertThrows(NegocioException.class, () -> UsuarioService.validarNuevo("ana", "Ana", "12345".toCharArray(), Rol.CAJERO));
        assertThrows(NegocioException.class, () -> UsuarioService.validarNuevo("ana", "Ana", "secreto".toCharArray(), null));
    }

    // ---- Permisos por rol
    @Test
    void soloElAdministradorActivoTienePermiso() {
        assertDoesNotThrow(() -> Permisos.requerirAdministrador(ADMIN));
        assertThrows(NegocioException.class, () -> Permisos.requerirAdministrador(CAJERO));
        assertThrows(NegocioException.class, () -> Permisos.requerirAdministrador(null));
        assertThrows(NegocioException.class,
                () -> Permisos.requerirAdministrador(new Usuario(3, "x", "X", Rol.ADMIN, false)));
    }

    @Test
    void cajeroNoPuedeEjecutarOperacionesDeAdministrador() {
        assertThrows(NegocioException.class, () -> new ProductoService()
                .crear(CAJERO, "P1", "N", "G", BigDecimal.ONE, 0, 0));
        assertThrows(NegocioException.class, () -> new InventarioService().registrarEntrada(CAJERO, "P1", 1, ""));
        assertThrows(NegocioException.class, () -> new UsuarioService().listar(CAJERO));
        assertThrows(NegocioException.class, () -> new ReporteService().existencias(CAJERO));
    }

    // ---- Reportes
    @Test
    void rangoDeFechasInvalidoSeRechaza() {
        LocalDate hoy = LocalDate.now();
        assertDoesNotThrow(() -> ReporteService.validarRango(hoy, hoy));
        assertThrows(NegocioException.class, () -> ReporteService.validarRango(hoy, hoy.minusDays(1)));
        assertThrows(NegocioException.class, () -> ReporteService.validarRango(null, hoy));
    }

    // ---- CSV
    @Test
    void csvEscapaComasComillasYSaltosDeLinea() {
        TablaDatos t = new TablaDatos(List.of("Código", "Nombre"),
                List.of(new Object[]{"A1", "Leche, entera"}, new Object[]{"A2", "Dijo \"hola\""}, new Object[]{"A3", null}));
        assertEquals("Código,Nombre\r\nA1,\"Leche, entera\"\r\nA2,\"Dijo \"\"hola\"\"\"\r\nA3,\r\n", CsvExporter.aTexto(t));
    }
}
