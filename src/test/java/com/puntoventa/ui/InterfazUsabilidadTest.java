package com.puntoventa.ui;

import static com.puntoventa.ui.UiRobot.buscar;
import static com.puntoventa.ui.UiRobot.celda;
import static com.puntoventa.ui.UiRobot.enEdt;
import static com.puntoventa.ui.UiRobot.enEdtGet;
import static com.puntoventa.ui.UiRobot.esperar;
import static com.puntoventa.ui.UiRobot.esperarDialogo;
import static com.puntoventa.ui.UiRobot.escribir;
import static com.puntoventa.ui.UiRobot.filas;
import static com.puntoventa.ui.UiRobot.pulsar;
import static com.puntoventa.ui.UiRobot.seleccionarFila;
import static com.puntoventa.ui.UiRobot.seleccionarPestana;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.puntoventa.model.Producto;
import com.puntoventa.service.BaseDatosPrueba;
import com.puntoventa.service.ProductoService;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;

/** Pruebas de usabilidad: teclado, estados vacíos, validación en el propio cuadro y búsquedas en vivo. */
class InterfazUsabilidadTest extends BaseInterfazTest {

    @Test
    void laVentaVaciaMuestraAyudaYNoPermiteCobrar() {
        MainFrame f = abrir(cajero);
        assertFalse(UiRobot.habilitado(boton(f, "btnCobrar")));
        assertFalse(UiRobot.habilitado(boton(f, "btnQuitar")));
        assertEquals("$0.00", texto(etiqueta(f, "lblTotal")));
        assertTrue(texto(etiqueta(f, "lblArticulos")).startsWith("0 "));
        assertTrue(texto(etiqueta(f, "lblDescuentoInfo")).contains("10"), "El cajero ve su límite de descuento");
    }

    @Test
    void unaBusquedaSinResultadosMarcaElCampoYExplicaQueHacer() {
        MainFrame f = abrir(cajero);
        agregarAVenta(f, "NO-EXISTE-" + UUID.randomUUID());
        assertTrue(texto(etiqueta(f, "lblMensaje")).startsWith("No se encontró"));
        assertEquals("error", enEdtGet(() -> campo(f, "txtBuscar").getClientProperty("JComponent.outline")));
        // una búsqueda correcta quita la marca de error
        Producto p = BaseDatosPrueba.crearProducto("5.00", 5);
        agregarAVenta(f, p.codigo());
        assertEquals(null, enEdtGet(() -> campo(f, "txtBuscar").getClientProperty("JComponent.outline")));
        assertTrue(texto(etiqueta(f, "lblMensaje")).startsWith("Agregado"));
    }

    @Test
    void variasCoincidenciasAbrenUnSelectorYElegidoSeAgrega() {
        String marca = "Zeta" + UUID.randomUUID().toString().substring(0, 6);
        ProductoService ps = new ProductoService();
        ps.crear(admin, BaseDatosPrueba.codigoUnico(), marca + " uno", "Prueba", new BigDecimal("3.00"), 5, 1);
        ps.crear(admin, BaseDatosPrueba.codigoUnico(), marca + " dos", "Prueba", new BigDecimal("4.00"), 5, 1);
        MainFrame f = abrir(cajero);
        CountDownLatch fin = UiRobot.lanzar(() -> {
            campo(f, "txtBuscar").setText(marca);
            campo(f, "txtBuscar").postActionEvent();
        });
        JDialog selector = esperarDialogo("Elija el producto");
        assertNotNull(buscar(selector, "listaProductos", javax.swing.JList.class));
        pulsar(boton(selector, "btnAgregarSeleccion"));
        esperar(fin);
        assertEquals(1, filas(tabla(f, "tablaVenta")));
    }

    @Test
    void masYMenosAjustanLaCantidadYMenosEnUnoQuitaElRenglon() {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 9);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        assertEquals(0, enEdtGet(() -> tabla(f, "tablaVenta").getSelectedRow()), "El renglón agregado queda seleccionado");
        pulsar(boton(f, "btnMas"));
        pulsar(boton(f, "btnMas"));
        assertEquals(3, celda(tabla(f, "tablaVenta"), 0, 3));
        assertEquals("$30.00", texto(etiqueta(f, "lblTotal")));
        pulsar(boton(f, "btnMenos"));
        assertEquals(2, celda(tabla(f, "tablaVenta"), 0, 3));
        pulsar(boton(f, "btnMenos"));
        pulsar(boton(f, "btnMenos"));
        assertEquals(0, filas(tabla(f, "tablaVenta")));
    }

    @Test
    void elAtajoF9AbreElCobroSoloSiHayProductos() {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 9);
        MainFrame f = abrir(cajero);
        VentaPanel panel = UiRobot.todos(f, VentaPanel.class).get(0);
        UiRobot.teclear(panel, KeyEvent.VK_F9, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(texto(etiqueta(f, "lblMensaje")).contains("Agregue al menos un producto"));
        assertFalse(UiRobot.hayVentana(JDialog.class));

        agregarAVenta(f, p.codigo());
        CountDownLatch fin = UiRobot.lanzar(() -> UiRobot.teclear(panel, KeyEvent.VK_F9, JComponent.WHEN_IN_FOCUSED_WINDOW));
        JDialog cobro = esperarDialogo("Cobrar");
        pulsar(boton(cobro, "btnCancelar"));
        esperar(fin);
        assertEquals(1, filas(tabla(f, "tablaVenta")), "Cancelar el cobro no pierde la venta");
    }

    @Test
    void elCobroEnEfectivoNoPermiteConfirmarSiElMontoNoAlcanzaYLosBotonesRapidosAyudan() {
        Producto p = BaseDatosPrueba.crearProducto("90.50", 9);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        CountDownLatch fin = clic(boton(f, "btnCobrar"));
        JDialog d = esperarDialogo("Cobrar");
        assertEquals("$90.50", texto(etiqueta(d, "lblTotalCobro")));
        assertEquals("90.50", texto(campo(d, "txtRecibido")), "Por omisión propone el monto exacto");
        escribir(campo(d, "txtRecibido"), "50");
        assertEquals("Faltan $40.50", texto(etiqueta(d, "lblCambio")));
        assertFalse(UiRobot.habilitado(boton(d, "btnConfirmar")));
        escribir(campo(d, "txtRecibido"), "abc");
        assertFalse(UiRobot.habilitado(boton(d, "btnConfirmar")));
        pulsar(boton(d, "btnBillete100"));
        assertEquals("100.00", texto(campo(d, "txtRecibido")));
        assertEquals("Cambio: $9.50", texto(etiqueta(d, "lblCambio")));
        assertTrue(UiRobot.habilitado(boton(d, "btnConfirmar")));
        pulsar(boton(d, "btnExacto"));
        assertEquals("90.50", texto(campo(d, "txtRecibido")));
        pulsar(buscar(d, "tbTarjeta", JToggleButton.class));
        assertFalse(UiRobot.habilitado(campo(d, "txtRecibido")), "Con tarjeta no se captura monto");
        assertTrue(UiRobot.habilitado(boton(d, "btnConfirmar")));
        pulsar(boton(d, "btnCancelar"));
        esperar(fin);
    }

    @Test
    void unProductoDuplicadoMuestraElErrorEnElMismoCuadroYPermiteCorregir() {
        Producto existente = BaseDatosPrueba.crearProducto("5.00", 1);
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Productos");
        conCuadro(boton(f, "btnNuevoProducto"), "Nuevo producto", d -> {
            escribir(campo(d, "campo_codigo"), existente.codigo());
            escribir(campo(d, "campo_nombre"), "Repetido");
            escribir(campo(d, "campo_precio"), "9");
            pulsar(boton(d, "btnAceptar"));
            assertTrue(UiRobot.visible(etiqueta(d, "lblError")));
            assertTrue(texto(etiqueta(d, "lblError")).contains("Ya existe un producto con el código"));
            assertTrue(enEdtGet(d::isShowing), "El cuadro sigue abierto para corregir");
            escribir(campo(d, "campo_precio"), "no-es-numero");
            pulsar(boton(d, "btnAceptar"));
            assertTrue(texto(etiqueta(d, "lblError")).contains("debe ser un número"));
            pulsar(boton(d, "btnCancelar"));
        });
        assertEquals(1, BaseDatosPrueba.escalar("SELECT COUNT(*) FROM productos WHERE codigo = '" + existente.codigo() + "'"));
    }

    @Test
    void laBusquedaDeProductosFiltraMientrasSeEscribe() {
        Producto a = BaseDatosPrueba.crearProducto("5.00", 5);
        BaseDatosPrueba.crearProducto("5.00", 5);
        MainFrame f = abrir(cajero);
        seleccionarPestana(f, "Productos");
        escribir(campo(f, "txtBuscarProducto"), a.codigo());
        pausa(700);
        assertEquals(1, filas(tabla(f, "tablaProductos")));
        assertEquals(a.nombre(), celda(tabla(f, "tablaProductos"), 0, 1));
        escribir(campo(f, "txtBuscarProducto"), "");
        pausa(700);
        assertTrue(filas(tabla(f, "tablaProductos")) > 1);
    }

    @Test
    void desactivarUnProductoPideConfirmacionYCancelarNoHaceNada() {
        Producto p = BaseDatosPrueba.crearProducto("5.00", 3);
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Productos");
        escribir(campo(f, "txtBuscarProducto"), p.codigo());
        pausa(700);
        seleccionarFila(tabla(f, "tablaProductos"), 0);
        CountDownLatch fin = clic(boton(f, "btnEstadoProducto"));
        responderOpcion("Desactivar producto", "Cancelar");
        esperar(fin);
        assertEquals(1, BaseDatosPrueba.escalar("SELECT activo FROM productos WHERE id = " + p.id()));
    }

    @Test
    void sinSeleccionLosBotonesDeFilaAvisanEnLugarDeFallar() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Productos");
        pulsar(boton(f, "btnEditarProducto"));
        assertTrue(texto(etiqueta(f, "lblResumenProductos")).contains("Seleccione un producto"));
        assertFalse(UiRobot.hayVentana(JDialog.class));
    }

    @Test
    void lasContrasenasQueNoCoincidenSeRechazanEnElCuadro() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Usuarios");
        conCuadro(boton(f, "btnNuevoUsuario"), "Nuevo usuario", d -> {
            escribir(campo(d, "campo_usuario"), "xx_" + UUID.randomUUID().toString().substring(0, 6));
            escribir(campo(d, "campo_nombre"), "Prueba");
            escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), "secreto1");
            escribir(buscar(d, "campo_repetir", javax.swing.JPasswordField.class), "secreto2");
            pulsar(boton(d, "btnAceptar"));
            assertTrue(texto(etiqueta(d, "lblError")).contains("no coinciden"));
            escribir(buscar(d, "campo_repetir", javax.swing.JPasswordField.class), "corta");
            escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), "corta");
            pulsar(boton(d, "btnAceptar"));
            assertTrue(texto(etiqueta(d, "lblError")).contains("al menos 6"));
            pulsar(boton(d, "btnCancelar"));
        });
    }

    @Test
    void enReportesElTipoDeReporteActivaSoloLosFiltrosQueAplican() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Reportes");
        JComboBox<?> tipo = buscar(f, "cmbTipo", JComboBox.class);
        JSpinner desde = buscar(f, "spDesde", JSpinner.class);
        JComboBox<?> usuario = buscar(f, "cmbUsuario", JComboBox.class);
        enEdt(() -> tipo.setSelectedIndex(5)); // existencias actuales
        assertFalse(UiRobot.habilitado(desde));
        assertFalse(UiRobot.habilitado(usuario));
        enEdt(() -> tipo.setSelectedIndex(6)); // historial por usuario
        assertTrue(UiRobot.habilitado(desde));
        assertTrue(UiRobot.habilitado(usuario));
        enEdt(() -> tipo.setSelectedIndex(0));
        assertFalse(UiRobot.habilitado(usuario));
    }

    @Test
    void unRangoDeFechasInvertidoSeExplicaSinVentanasEmergentes() {
        MainFrame f = abrir(admin);
        seleccionarPestana(f, "Reportes");
        pausa(400);
        JSpinner desde = buscar(f, "spDesde", JSpinner.class);
        enEdt(() -> desde.setValue(Ui.aDate(java.time.LocalDate.now().plusDays(3))));
        pulsar(boton(f, "btnGenerar"));
        assertTrue(texto(etiqueta(f, "lblMensajeReportes")).contains("no puede ser posterior"));
        assertFalse(UiRobot.hayVentana(JDialog.class));
        pulsar(boton(f, "btnPeriodo_mes"));
        assertEquals("", texto(etiqueta(f, "lblMensajeReportes")).trim(), "Un periodo rápido corrige el rango");
    }

    @Test
    void elTicketSePuedeGuardarComoArchivoDeTexto() throws Exception {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 5);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        CountDownLatch cobrando = clic(boton(f, "btnCobrar"));
        JDialog cobro = esperarDialogo("Cobrar");
        pulsar(buscar(cobro, "tbTarjeta", JToggleButton.class));
        pulsar(boton(cobro, "btnConfirmar"));
        JDialog ticket = esperarDialogo("Venta registrada");
        File archivo = File.createTempFile("ticket-ui-", ".txt");
        archivo.delete();
        CountDownLatch guardando = clic(boton(ticket, "btnGuardarTicket"));
        JDialog selector = UiRobot.esperarSelectorDeArchivos();
        UiRobot.guardarEn(UiRobot.selectorDeArchivos(selector), archivo);
        esperar(guardando);
        assertTrue(Files.readString(archivo.toPath(), StandardCharsets.UTF_8).contains("PUNTO DE VENTA INTEGRAL"));
        archivo.delete();
        pulsar(boton(ticket, "btnCerrarTicket"));
        esperar(cobrando);
    }

    @Test
    void despuesDeVenderLaBarraDeEstadoConfirmaLaVenta() {
        Producto p = BaseDatosPrueba.crearProducto("10.00", 5);
        MainFrame f = abrir(cajero);
        agregarAVenta(f, p.codigo());
        CountDownLatch cobrando = clic(boton(f, "btnCobrar"));
        JDialog cobro = esperarDialogo("Cobrar");
        pulsar(buscar(cobro, "tbTarjeta", JToggleButton.class));
        pulsar(boton(cobro, "btnConfirmar"));
        JDialog ticket = esperarDialogo("Venta registrada");
        assertTrue(texto(etiqueta(f, "lblAviso")).contains("registrada"));
        pulsar(boton(ticket, "btnCerrarTicket"));
        esperar(cobrando);
    }

    @Test
    void losAtajosDeTecladoCambianDePestana() {
        MainFrame f = abrir(admin);
        javax.swing.JTabbedPane pestanas = buscar(f, "pestanas", javax.swing.JTabbedPane.class);
        javax.swing.KeyStroke ctrl3 = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.CTRL_DOWN_MASK);
        enEdt(() -> {
            Object clave = f.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ctrl3);
            assertNotNull(clave, "Ctrl+3 debe tener acción");
            f.getRootPane().getActionMap().get(clave).actionPerformed(null);
        });
        assertEquals("Inventario", enEdtGet(() -> pestanas.getTitleAt(pestanas.getSelectedIndex())));
    }

    @Test
    void lasPantallasSeDibujanEnElTamanoMinimoSinPartesEnBlanco() {
        MainFrame f = abrir(admin);
        for (String pestana : new String[]{"Ventas", "Productos", "Inventario", "Reportes", "Usuarios"}) {
            seleccionarPestana(f, pestana);
            pausa(150);
            BufferedImage img = UiRobot.capturar(f, 1100, 680);
            assertTrue(colores(img) > 12, "La pestaña " + pestana + " parece vacía");
        }
    }

    private static int colores(BufferedImage img) {
        java.util.Set<Integer> distintos = new java.util.HashSet<>();
        for (int x = 0; x < img.getWidth(); x += 7) {
            for (int y = 0; y < img.getHeight(); y += 7) {
                distintos.add(img.getRGB(x, y));
            }
        }
        return distintos.size();
    }
}
