package com.puntoventa.ui;

import static com.puntoventa.service.BaseDatosPrueba.escalar;
import static com.puntoventa.service.BaseDatosPrueba.existencias;
import static com.puntoventa.ui.UiRobot.buscar;
import static com.puntoventa.ui.UiRobot.buscarOpcional;
import static com.puntoventa.ui.UiRobot.celda;
import static com.puntoventa.ui.UiRobot.enEdt;
import static com.puntoventa.ui.UiRobot.esperar;
import static com.puntoventa.ui.UiRobot.esperarDialogo;
import static com.puntoventa.ui.UiRobot.esperarVentana;
import static com.puntoventa.ui.UiRobot.escribir;
import static com.puntoventa.ui.UiRobot.filas;
import static com.puntoventa.ui.UiRobot.pulsar;
import static com.puntoventa.ui.UiRobot.seleccionarFila;
import static com.puntoventa.ui.UiRobot.seleccionarPestana;
import static com.puntoventa.ui.UiRobot.titulosPestanas;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.puntoventa.model.Carrito;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Producto;
import com.puntoventa.service.BaseDatosPrueba;
import com.puntoventa.service.VentaService;
import java.awt.Window;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de aceptación de la interfaz (AC-01 a AC-15 del documento de Construcción), ejecutadas con
 * clics y texto reales sobre las pantallas Swing y la base de datos.
 */
class InterfazAceptacionTest extends BaseInterfazTest {

    private static LoginFrame abrirLogin() {
        LoginFrame lf = UiRobot.enEdtGet(LoginFrame::new);
        enEdt(() -> lf.setVisible(true));
        return lf;
    }

    private static void iniciarSesion(LoginFrame lf, String usuario, String clave) {
        escribir(campo(lf, "txtUsuario"), usuario);
        escribir(buscar(lf, "txtContrasena", javax.swing.JPasswordField.class), clave);
        pulsar(boton(lf, "btnEntrar"));
    }

    @Test
    void ac01_administradorVeCincoPestanas() {
        LoginFrame lf = abrirLogin();
        iniciarSesion(lf, admin.username(), BaseDatosPrueba.CONTRASENA);
        Window principal = esperarVentana(w -> w instanceof MainFrame, 6000);
        assertEquals(List.of("Ventas", "Productos", "Inventario", "Reportes", "Usuarios"), titulosPestanas(principal));
        assertFalse(UiRobot.hayVentana(LoginFrame.class), "El inicio de sesión se cierra al entrar");
    }

    @Test
    void ac02_cajeroVeSoloVentasYProductosSinBotonesDeEdicion() {
        LoginFrame lf = abrirLogin();
        iniciarSesion(lf, cajero.username(), BaseDatosPrueba.CONTRASENA);
        Window principal = esperarVentana(w -> w instanceof MainFrame, 6000);
        assertEquals(List.of("Ventas", "Productos"), titulosPestanas(principal));
        assertNull(buscarOpcional((java.awt.Container) principal, "btnNuevoProducto", JButton.class));
        assertNull(buscarOpcional((java.awt.Container) principal, "btnEditarProducto", JButton.class));
        assertNull(buscarOpcional((java.awt.Container) principal, "btnEstadoProducto", JButton.class));
    }

    @Test
    void ac03_contrasenaIncorrectaMuestraErrorGenericoEnLaPantalla() {
        LoginFrame lf = abrirLogin();
        iniciarSesion(lf, admin.username(), "mala-contrasena");
        pausa(300);
        assertTrue(UiRobot.visible(etiqueta(lf, "lblError")));
        assertEquals("Usuario o contraseña incorrectos.", texto(etiqueta(lf, "lblError")));
        assertFalse(UiRobot.hayVentana(MainFrame.class));
        assertEquals("", new String(buscar(lf, "txtContrasena", javax.swing.JPasswordField.class).getPassword()),
                "La contraseña se borra tras el error");
        // el mensaje no revela cuál dato falló: con un usuario inexistente es el mismo texto
        iniciarSesion(lf, "no_existe_" + UUID.randomUUID(), "x");
        pausa(300);
        assertEquals("Usuario o contraseña incorrectos.", texto(etiqueta(lf, "lblError")));
    }

    @Test
    void ac04_agregarProductosPorCodigoYPorNombre() {
        Producto a = BaseDatosPrueba.crearProducto("18.50", 20);
        Producto b = BaseDatosPrueba.crearProducto("26.00", 20);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, a.codigo());
        agregarAVenta(f, b.nombre());
        assertEquals(2, filas(tabla(f, "tablaVenta")));
        assertEquals(a.codigo(), celda(tabla(f, "tablaVenta"), 0, 0));
        assertEquals(b.codigo(), celda(tabla(f, "tablaVenta"), 1, 0));
        assertEquals("$44.50", texto(etiqueta(f, "lblSubtotal")));
        assertEquals("$44.50", texto(etiqueta(f, "lblTotal")));
    }

    @Test
    void ac05_cantidadMayorALasExistenciasMuestraErrorEnElCuadro() {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 3);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        seleccionarFila(tabla(f, "tablaVenta"), 0);
        conCuadro(boton(f, "btnCantidad"), "Cambiar cantidad", d -> {
            ponerEntero(d, "campo_cantidad", 50);
            pulsar(boton(d, "btnAceptar"));
            assertTrue(UiRobot.visible(etiqueta(d, "lblError")));
            assertTrue(texto(etiqueta(d, "lblError")).contains("Existencias insuficientes"));
            pulsar(boton(d, "btnCancelar"));
        });
        assertEquals(1, celda(tabla(f, "tablaVenta"), 0, 3));
    }

    @Test
    void ac06_cobrarEnEfectivoGuardaLaVentaDescuentaYMuestraElTicket() {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 5);
        int ventasAntes = escalar("SELECT COUNT(*) FROM ventas");
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        agregarAVenta(f, p.codigo());
        assertEquals("$20.00", texto(etiqueta(f, "lblTotal")));

        CountDownLatch cobrando = clic(boton(f, "btnCobrar"));
        JDialog cobro = esperarDialogo("Cobrar");
        escribir(campo(cobro, "txtRecibido"), "50");
        assertEquals("Cambio: $30.00", texto(etiqueta(cobro, "lblCambio")));
        pulsar(boton(cobro, "btnConfirmar"));
        JDialog ticket = esperarDialogo("Venta registrada");
        String texto = UiRobot.enEdtGet(() -> buscar(ticket, "areaTicket", JTextArea.class).getText());
        assertTrue(texto.contains("TOTAL"));
        assertTrue(texto.contains("$20.00"));
        assertTrue(texto.contains("Cambio"));
        assertTrue(texto(etiqueta(ticket, "lblDetalleVenta")).contains("Cambio $30.00"));
        pulsar(boton(ticket, "btnCerrarTicket"));
        esperar(cobrando);

        assertEquals(3, existencias(p));
        assertEquals(ventasAntes + 1, escalar("SELECT COUNT(*) FROM ventas"));
        assertEquals(0, filas(tabla(f, "tablaVenta")), "La pantalla queda lista para otra venta");
        assertFalse(UiRobot.habilitado(boton(f, "btnCobrar")));
    }

    @Test
    void ac07_descuentoMayorAlLimiteExigeAdministradorYElErrorSeMuestraEnElCuadro() {
        Producto p = BaseDatosPrueba.crearProducto("100.00", 5);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        escribir(campo(f, "txtDescuento"), "15");
        conCuadro(boton(f, "btnAplicarDescuento"), "Autorización", d -> {
            escribir(campo(d, "campo_usuario"), admin.username());
            escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), "mala");
            pulsar(boton(d, "btnAceptar"));
            assertTrue(UiRobot.visible(etiqueta(d, "lblError")));
            assertTrue(texto(etiqueta(d, "lblError")).contains("incorrectas"));
            escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), BaseDatosPrueba.CONTRASENA);
            pulsar(boton(d, "btnAceptar"));
        });
        assertEquals("$85.00", texto(etiqueta(f, "lblTotal")));
        assertTrue(texto(etiqueta(f, "lblDescuentoInfo")).contains("autorizado por"));
    }

    @Test
    void ac07b_descuentoHastaElLimiteLoAplicaElCajeroSinAutorizacion() {
        Producto p = BaseDatosPrueba.crearProducto("100.00", 5);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        escribir(campo(f, "txtDescuento"), "10");
        pulsar(boton(f, "btnAplicarDescuento"));
        assertEquals("$90.00", texto(etiqueta(f, "lblTotal")));
        assertFalse(UiRobot.hayVentana(JDialog.class), "No pide autorización hasta 10 %");
    }

    @Test
    void ac08_crearProductoConExistenciaInicial() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Productos");
        String codigo = BaseDatosPrueba.codigoUnico();
        conCuadro(boton(f, "btnNuevoProducto"), "Nuevo producto", d -> {
            escribir(campo(d, "campo_codigo"), codigo);
            escribir(campo(d, "campo_nombre"), "Producto de prueba " + codigo);
            escribir(campo(d, "campo_precio"), "15.5");
            ponerEntero(d, "campo_existencias", 7);
            pulsar(boton(d, "btnAceptar"));
        });
        assertEquals(1, escalar("SELECT COUNT(*) FROM productos WHERE codigo = '" + codigo + "' AND existencias = 7"));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimientos_inventario m JOIN productos p ON p.id = m.producto_id "
                + "WHERE p.codigo = '" + codigo + "' AND m.tipo = 'ENTRADA' AND m.motivo = 'Existencia inicial'"));
        escribir(campo(f, "txtBuscarProducto"), codigo);
        pausa(700);
        assertEquals(1, filas(tabla(f, "tablaProductos")));
        assertEquals("$15.50", celda(tabla(f, "tablaProductos"), 0, 3));
    }

    @Test
    void ac09_desactivarProductoLoQuitaDeVentasPeroConservaElHistorial() {
        Producto p = BaseDatosPrueba.crearProducto("5.00", 4);
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Productos");
        escribir(campo(f, "txtBuscarProducto"), p.codigo());
        pausa(700);
        assertEquals(1, filas(tabla(f, "tablaProductos")));
        seleccionarFila(tabla(f, "tablaProductos"), 0);
        CountDownLatch fin = clic(boton(f, "btnEstadoProducto"));
        responderOpcion("Desactivar producto", "Desactivar");
        esperar(fin);
        assertEquals(0, escalar("SELECT activo FROM productos WHERE id = " + p.id()));
        pausa(300);
        assertEquals(0, filas(tabla(f, "tablaProductos")), "Los inactivos no se muestran por omisión");
        pulsar(buscar(f, "chkInactivos", javax.swing.JCheckBox.class));
        pausa(300);
        assertEquals(1, filas(tabla(f, "tablaProductos")));
        assertEquals("Inactivo", celda(tabla(f, "tablaProductos"), 0, 6));

        seleccionarPestana(f, "Ventas");
        agregarAVenta(f, p.codigo());
        assertTrue(texto(etiqueta(f, "lblMensaje")).contains("No se encontró"));
        assertEquals(0, filas(tabla(f, "tablaVenta")));
    }

    @Test
    void ac10_entradaYAjusteConMotivo() {
        Producto p = BaseDatosPrueba.crearProducto("5.00", 2);
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Inventario");
        conCuadro(boton(f, "btnEntrada"), "Registrar entrada", d -> {
            escribir(campo(d, "campo_codigo"), p.codigo());
            pausa(200);
            assertTrue(texto(etiqueta(d, "lblProductoInfo")).contains(p.nombre()));
            ponerEntero(d, "campo_cantidad", 10);
            pulsar(boton(d, "btnAceptar"));
        });
        assertEquals(12, existencias(p));
        conCuadro(boton(f, "btnAjuste"), "Registrar ajuste", d -> {
            escribir(campo(d, "campo_codigo"), p.codigo());
            ponerEntero(d, "campo_real", 4);
            pulsar(boton(d, "btnAceptar"));
            assertTrue(UiRobot.visible(etiqueta(d, "lblError")), "Sin motivo debe mostrar el error en el cuadro");
            assertTrue(texto(etiqueta(d, "lblError")).contains("motivo"));
            escribir(campo(d, "campo_motivo"), "Conteo físico");
            pulsar(boton(d, "btnAceptar"));
        });
        assertEquals(4, existencias(p));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimientos_inventario WHERE producto_id = " + p.id()
                + " AND tipo = 'AJUSTE' AND cantidad = -8 AND motivo = 'Conteo físico'"));
    }

    @Test
    void ac11_existenciasBajasListaLosProductosEnElMinimo() {
        Producto bajo = BaseDatosPrueba.crearProducto("5.00", 1); // el mínimo por defecto es 5
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Inventario");
        pausa(400);
        boolean encontrado = false;
        for (int i = 0; i < filas(tabla(f, "tablaInventario")); i++) {
            if (bajo.codigo().equals(celda(tabla(f, "tablaInventario"), i, 0))) {
                encontrado = true;
            }
        }
        assertTrue(encontrado, "El producto con 1 pieza debe aparecer en existencias bajas");
        assertTrue(texto(etiqueta(f, "lblResumenInventario")).contains("reposición"));
    }

    @Test
    void ac12_reporteDeVentasYExportacionACsv() throws Exception {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 10);
        Carrito c = new Carrito();
        c.agregar(p, 2);
        new VentaService().cobrar(c, BigDecimal.ZERO, cajero, null, MetodoPago.TARJETA, null);
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Reportes");
        pausa(500);
        pulsar(boton(f, "btnGenerar"));
        assertTrue(filas(tabla(f, "tablaReportes")) >= 1);
        assertTrue(texto(etiqueta(f, "lblResumenReportes")).contains("Ingresos del periodo"));

        File csv = File.createTempFile("reporte-ui-", ".csv");
        csv.delete();
        CountDownLatch exportando = clic(boton(f, "btnExportar"));
        UiRobot.guardarEn(UiRobot.selectorDeArchivos(UiRobot.esperarSelectorDeArchivos()), csv);
        esperar(exportando);
        assertTrue(csv.exists());
        String contenido = Files.readString(csv.toPath(), StandardCharsets.UTF_8);
        assertTrue(contenido.startsWith("﻿Periodo,Ventas,Subtotal,Descuentos,Ingresos"), contenido.substring(0, 40));
        csv.delete();
    }

    @Test
    void ac13_crearUnCajeroDesdeLaPantallaYEntrarConEl() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Usuarios");
        String nombre = "ui_" + UUID.randomUUID().toString().substring(0, 8);
        conCuadro(boton(f, "btnNuevoUsuario"), "Nuevo usuario", d -> {
            escribir(campo(d, "campo_usuario"), nombre);
            escribir(campo(d, "campo_nombre"), "Usuario de prueba");
            escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), "secreto1");
            escribir(buscar(d, "campo_repetir", javax.swing.JPasswordField.class), "secreto1");
            pulsar(boton(d, "btnAceptar"));
        });
        assertEquals(1, escalar("SELECT COUNT(*) FROM usuarios WHERE username = '" + nombre + "' AND rol = 'CAJERO'"));
        enEdt(f::dispose);
        LoginFrame lf = abrirLogin();
        iniciarSesion(lf, nombre, "secreto1");
        Window principal = esperarVentana(w -> w instanceof MainFrame, 6000);
        assertEquals(List.of("Ventas", "Productos"), titulosPestanas(principal));
    }

    @Test
    void ac14_unAdministradorNoPuedeDesactivarseASiMismo() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Usuarios");
        pausa(400);
        int fila = -1;
        for (int i = 0; i < filas(tabla(f, "tablaUsuarios")); i++) {
            if (admin.username().equals(celda(tabla(f, "tablaUsuarios"), i, 0))) {
                fila = i;
            }
        }
        assertTrue(fila >= 0, "El administrador debe aparecer en la lista");
        seleccionarFila(tabla(f, "tablaUsuarios"), fila);
        CountDownLatch fin = clic(boton(f, "btnEstadoUsuario"));
        responderOpcion("Desactivar usuario", "Desactivar");
        JDialog error = esperarDialogo("No se pudo completar");
        assertNotNull(error);
        responderOpcion("No se pudo completar", null);
        esperar(fin);
        assertEquals(1, escalar("SELECT activo FROM usuarios WHERE id = " + admin.id()));
    }

    @Test
    void ac15_cerrarSesionRegresaAlInicioDeSesion() {
        MainFrame f = abrir(cajero);
        enEdt(() -> f.setVisible(true));
        pulsar(boton(f, "btnCerrarSesion"));
        esperarVentana(w -> w instanceof LoginFrame, 5000);
        assertFalse(UiRobot.hayVentana(MainFrame.class));
    }
}
