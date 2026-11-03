package com.puntoventa.ui;

import static com.puntoventa.ui.UiRobot.buscar;
import static com.puntoventa.ui.UiRobot.enEdt;
import static com.puntoventa.ui.UiRobot.enEdtGet;
import static com.puntoventa.ui.UiRobot.esperar;
import static com.puntoventa.ui.UiRobot.esperarDialogo;
import static com.puntoventa.ui.UiRobot.escribir;
import static com.puntoventa.ui.UiRobot.escribirYEnter;
import static com.puntoventa.ui.UiRobot.lanzar;
import static com.puntoventa.ui.UiRobot.pulsar;
import static com.puntoventa.ui.UiRobot.seleccionarFila;

import com.puntoventa.model.Carrito;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.service.AuthService;
import com.puntoventa.service.InventarioService;
import com.puntoventa.service.ProductoService;
import com.puntoventa.service.UsuarioService;
import com.puntoventa.service.VentaService;
import com.puntoventa.util.Database;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/** Genera capturas reales de todas las pantallas con datos de demostración (uso manual, no es una prueba). */
public final class Capturas {

    private static String carpeta;

    private Capturas() {
    }

    public static void main(String[] args) throws Exception {
        carpeta = args[0];
        new File(carpeta).mkdirs();
        System.setProperty("java.awt.headless", "false");
        Usuario admin = prepararDatos();
        Usuario cajero = new AuthService().login("cajero", "cajero123".toCharArray()).orElseThrow();

        // ---- Login
        LoginFrame login = enEdtGet(LoginFrame::new);
        guardar(UiRobot.capturar(login, 560, 600), "01_login");
        escribir(buscar(login, "txtUsuario", JTextField.class), "admin");
        escribir(buscar(login, "txtContrasena", javax.swing.JPasswordField.class), "incorrecta");
        pulsar(buscar(login, "btnEntrar", JButton.class));
        guardar(UiRobot.capturar(login, 560, 600), "02_login_error");
        enEdt(login::dispose);

        // ---- Administrador
        MainFrame adm = enEdtGet(() -> new MainFrame(admin));
        ventas(adm);
        productos(adm, true);
        inventario(adm);
        reportes(adm);
        usuarios(adm);

        // ---- Cajero
        MainFrame caj = enEdtGet(() -> new MainFrame(cajero));
        productos(caj, false);
        guardar(UiRobot.capturar(caj, 1280, 780), "30_cajero_productos");
        UiRobot.seleccionarPestana(caj, "Ventas");
        guardar(UiRobot.capturar(caj, 1280, 780), "31_cajero_ventas");
        descuentoCajero(caj);

        UiRobot.cerrarTodo();
        System.out.println("Capturas guardadas en " + carpeta);
        System.exit(0);
    }

    // ------------------------------------------------------------------ datos
    private static Usuario prepararDatos() {
        AuthService auth = new AuthService();
        auth.sembrarUsuariosIniciales();
        Usuario admin = auth.login("admin", "admin123".toCharArray()).orElseThrow();
        Usuario cajero = auth.login("cajero", "cajero123".toCharArray()).orElseThrow();
        ProductoService ps = new ProductoService();
        InventarioService inv = new InventarioService();
        UsuarioService us = new UsuarioService();
        if (ps.buscarPorCodigo("7506001").isEmpty()) {
            ps.crear(admin, "7506001", "Pan integral 500 g", "Panadería", new BigDecimal("42.00"), 2, 6);
            ps.crear(admin, "7506002", "Café soluble 200 g", "Abarrotes", new BigDecimal("89.90"), 0, 4);
            ps.crear(admin, "7506003", "Atún en agua lata", "Abarrotes", new BigDecimal("19.50"), 60, 12);
            Producto detergente = ps.buscarPorCodigo("7504002").orElseThrow();
            ps.cambiarEstado(admin, detergente.id(), false);
            ps.editar(admin, ps.buscarPorCodigo("7503002").orElseThrow().id(), "7503002", "Queso panela 400 g", "Lácteos", new BigDecimal("58.00"), 12);
            us.crear(admin, "maria.lopez", "María López", "secreto1".toCharArray(), Rol.CAJERO);
            us.crear(admin, "carlos.ruiz", "Carlos Ruiz", "secreto1".toCharArray(), Rol.CAJERO);
            Usuario carlos = us.listar(admin).stream().filter(u -> u.username().equals("carlos.ruiz")).findFirst().orElseThrow();
            us.cambiarEstado(admin, carlos.id(), false);
            inv.registrarEntrada(admin, "7501001", 24, "Compra a proveedor");
            inv.registrarAjuste(admin, "7502002", 38, "Merma por empaque dañado");
            Random azar = new Random(7);
            String[] codigos = {"7501001", "7501002", "7502001", "7502002", "7502003", "7503001", "7504001", "7506003", "7505001"};
            for (int i = 0; i < 28; i++) {
                Carrito c = new Carrito();
                int renglones = 1 + azar.nextInt(4);
                for (int r = 0; r < renglones; r++) {
                    c.agregar(ps.buscarPorCodigo(codigos[azar.nextInt(codigos.length)]).orElseThrow(), 1 + azar.nextInt(3));
                }
                new VentaService().cobrar(c, i % 7 == 0 ? new BigDecimal("5") : BigDecimal.ZERO, cajero, null,
                        i % 3 == 0 ? MetodoPago.TARJETA : MetodoPago.EFECTIVO, new BigDecimal("1000"));
            }
            try (Connection con = Database.getConnection(); Statement st = con.createStatement()) {
                st.executeUpdate("UPDATE ventas SET fecha = fecha - INTERVAL (id % 12) DAY");
                st.executeUpdate("UPDATE bitacora SET fecha = fecha - INTERVAL (id % 9) DAY WHERE accion = 'VENTA'");
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        return admin;
    }

    private static void guardar(BufferedImage img, String nombre) {
        try {
            ImageIO.write(img, "png", new File(carpeta, nombre + ".png"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void capturarDialogo(JDialog d, String nombre) {
        guardar(UiRobot.capturarVisible(d), nombre);
    }

    // ------------------------------------------------------------------ pantallas
    private static void ventas(MainFrame f) {
        guardar(UiRobot.capturar(f, 1280, 780), "10_ventas_vacia");
        JTextField buscar = buscar(f, "txtBuscar", JTextField.class);
        escribirYEnter(buscar, "7501001");
        escribirYEnter(buscar, "7501001");
        escribirYEnter(buscar, "7503001");
        escribirYEnter(buscar, "7502002");
        escribirYEnter(buscar, "9999999");
        guardar(UiRobot.capturar(f, 1280, 780), "11_ventas_error_busqueda");
        escribirYEnter(buscar, "7502003");
        seleccionarFila(buscar(f, "tablaVenta", JTable.class), 1);
        pulsar(buscar(f, "btnMas", JButton.class));
        escribir(buscar(f, "txtDescuento", JTextField.class), "10");
        pulsar(buscar(f, "btnAplicarDescuento", JButton.class));
        guardar(UiRobot.capturar(f, 1280, 780), "12_ventas_con_productos");
        guardar(UiRobot.capturar(f, 1100, 680), "13_ventas_tamano_minimo");

        // selector (varias coincidencias)
        CountDownLatch sel = lanzar(() -> {
            buscar(f, "txtBuscar", JTextField.class).setText("a");
            buscar(f, "txtBuscar", JTextField.class).postActionEvent();
        });
        JDialog dsel = esperarDialogo("Elija el producto");
        capturarDialogo(dsel, "14_selector_producto");
        enEdt(dsel::dispose);
        esperar(sel);

        // cambiar cantidad
        seleccionarFila(buscar(f, "tablaVenta", JTable.class), 0);
        CountDownLatch c1 = lanzar(() -> buscar(f, "btnCantidad", JButton.class).doClick());
        JDialog dc = esperarDialogo("Cambiar cantidad");
        capturarDialogo(dc, "15_cambiar_cantidad");
        enEdt(dc::dispose);
        esperar(c1);

        // cobro en efectivo
        CountDownLatch c2 = lanzar(() -> buscar(f, "btnCobrar", JButton.class).doClick());
        JDialog cobro = esperarDialogo("Cobrar");
        escribir(buscar(cobro, "txtRecibido", JTextField.class), "100");
        capturarDialogo(cobro, "16_cobro_faltan");
        pulsar(buscar(cobro, "btnBillete200", JButton.class));
        capturarDialogo(cobro, "17_cobro_efectivo");
        pulsar(buscar(cobro, "tbTarjeta", JToggleButton.class));
        capturarDialogo(cobro, "18_cobro_tarjeta");
        pulsar(buscar(cobro, "tbEfectivo", JToggleButton.class));
        pulsar(buscar(cobro, "btnBillete200", JButton.class));
        pulsar(buscar(cobro, "btnConfirmar", JButton.class));
        JDialog ticket = esperarDialogo("Venta registrada");
        capturarDialogo(ticket, "19_ticket");
        enEdt(ticket::dispose);
        esperar(c2);
        guardar(UiRobot.capturar(f, 1280, 780), "20_ventas_despues_de_cobrar");
    }

    private static void productos(MainFrame f, boolean admin) {
        UiRobot.seleccionarPestana(f, "Productos");
        pausa(300);
        if (!admin) {
            return;
        }
        guardar(UiRobot.capturar(f, 1280, 780), "40_productos_admin");
        escribir(buscar(f, "txtBuscarProducto", JTextField.class), "pan");
        pausa(500);
        guardar(UiRobot.capturar(f, 1280, 780), "41_productos_busqueda");
        escribir(buscar(f, "txtBuscarProducto", JTextField.class), "");
        pausa(400);
        // formulario con error inline
        CountDownLatch l = lanzar(() -> buscar(f, "btnNuevoProducto", JButton.class).doClick());
        JDialog d = esperarDialogo("Nuevo producto");
        capturarDialogo(d, "42_producto_nuevo");
        escribir(buscar(d, "campo_codigo", JTextField.class), "7501001");
        escribir(buscar(d, "campo_nombre", JTextField.class), "Producto repetido");
        escribir(buscar(d, "campo_precio", JTextField.class), "12.5");
        pulsar(buscar(d, "btnAceptar", JButton.class));
        capturarDialogo(d, "43_producto_error_inline");
        enEdt(d::dispose);
        esperar(l);
    }

    private static void inventario(MainFrame f) {
        UiRobot.seleccionarPestana(f, "Inventario");
        pausa(300);
        guardar(UiRobot.capturar(f, 1280, 780), "50_inventario_bajo_stock");
        pulsar(buscar(f, "tbHistorial", JToggleButton.class));
        pausa(300);
        guardar(UiRobot.capturar(f, 1280, 780), "51_inventario_historial");
        CountDownLatch l = lanzar(() -> buscar(f, "btnEntrada", JButton.class).doClick());
        JDialog d = esperarDialogo("Registrar entrada");
        escribir(buscar(d, "campo_codigo", JTextField.class), "7506001");
        pausa(200);
        capturarDialogo(d, "52_entrada");
        enEdt(d::dispose);
        esperar(l);
        CountDownLatch l2 = lanzar(() -> buscar(f, "btnAjuste", JButton.class).doClick());
        JDialog d2 = esperarDialogo("Registrar ajuste");
        escribir(buscar(d2, "campo_codigo", JTextField.class), "7501002");
        pausa(200);
        pulsar(buscar(d2, "btnAceptar", JButton.class));
        capturarDialogo(d2, "53_ajuste_error_motivo");
        enEdt(d2::dispose);
        esperar(l2);
        pulsar(buscar(f, "tbBajoStock", JToggleButton.class));
    }

    private static void reportes(MainFrame f) {
        UiRobot.seleccionarPestana(f, "Reportes");
        pausa(600);
        pulsar(buscar(f, "btnPeriodo_30", JButton.class));
        pausa(300);
        guardar(UiRobot.capturar(f, 1280, 780), "60_reportes_ventas_por_dia");
        enEdt(() -> buscar(f, "cmbTipo", javax.swing.JComboBox.class).setSelectedIndex(3));
        pulsar(buscar(f, "btnGenerar", JButton.class));
        guardar(UiRobot.capturar(f, 1280, 780), "61_reportes_mas_vendidos");
        enEdt(() -> buscar(f, "cmbTipo", javax.swing.JComboBox.class).setSelectedIndex(5));
        pulsar(buscar(f, "btnGenerar", JButton.class));
        guardar(UiRobot.capturar(f, 1280, 780), "62_reportes_existencias");
    }

    private static void usuarios(MainFrame f) {
        UiRobot.seleccionarPestana(f, "Usuarios");
        pausa(300);
        seleccionarFila(buscar(f, "tablaUsuarios", JTable.class), 1);
        guardar(UiRobot.capturar(f, 1280, 780), "70_usuarios");
        CountDownLatch l = lanzar(() -> buscar(f, "btnNuevoUsuario", JButton.class).doClick());
        JDialog d = esperarDialogo("Nuevo usuario");
        escribir(buscar(d, "campo_usuario", JTextField.class), "ana");
        escribir(buscar(d, "campo_nombre", JTextField.class), "Ana Torres");
        escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), "secreto1");
        escribir(buscar(d, "campo_repetir", javax.swing.JPasswordField.class), "otra-cosa");
        pulsar(buscar(d, "btnAceptar", JButton.class));
        capturarDialogo(d, "71_usuario_nuevo_error");
        enEdt(d::dispose);
        esperar(l);
    }

    private static void descuentoCajero(MainFrame f) {
        JTextField buscar = buscar(f, "txtBuscar", JTextField.class);
        escribirYEnter(buscar, "7501001");
        escribir(buscar(f, "txtDescuento", JTextField.class), "15");
        CountDownLatch l = lanzar(() -> buscar(f, "btnAplicarDescuento", JButton.class).doClick());
        JDialog d = esperarDialogo("Autorización");
        escribir(buscar(d, "campo_usuario", JTextField.class), "admin");
        escribir(buscar(d, "campo_clave", javax.swing.JPasswordField.class), "mala");
        pulsar(buscar(d, "btnAceptar", JButton.class));
        capturarDialogo(d, "32_autorizacion_error");
        enEdt(d::dispose);
        esperar(l);
    }
}
