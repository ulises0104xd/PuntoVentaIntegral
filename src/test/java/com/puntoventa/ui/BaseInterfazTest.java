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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.service.BaseDatosPrueba;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/** Base de las pruebas de interfaz: requieren pantalla y base de datos; si faltan, se omiten. */
abstract class BaseInterfazTest {

    static boolean entorno;
    static Usuario admin;
    static Usuario cajero;

    @BeforeAll
    static void prepararEntorno() {
        entorno = !GraphicsEnvironment.isHeadless() && BaseDatosPrueba.disponible();
        if (entorno) {
            admin = BaseDatosPrueba.crearUsuario(Rol.ADMIN);
            cajero = BaseDatosPrueba.crearUsuario(Rol.CAJERO);
        }
    }

    @BeforeEach
    void requisitos() {
        assumeTrue(entorno, "Requiere pantalla y base de datos");
    }

    @AfterEach
    void limpiar() {
        if (entorno) {
            UiRobot.cerrarTodo();
        }
    }

    // ------------------------------------------------------------------ atajos para las pruebas
    static MainFrame abrir(Usuario usuario) {
        return enEdtGet(() -> new MainFrame(usuario));
    }

    static CountDownLatch clic(JButton boton) {
        return lanzar(boton::doClick);
    }

    static String texto(JTextComponent c) {
        return enEdtGet(c::getText);
    }

    static String texto(JLabel l) {
        return UiRobot.texto(l);
    }

    static JTextField campo(java.awt.Container raiz, String nombre) {
        return buscar(raiz, nombre, JTextField.class);
    }

    static JButton boton(java.awt.Container raiz, String nombre) {
        return buscar(raiz, nombre, JButton.class);
    }

    static JTable tabla(java.awt.Container raiz, String nombre) {
        return buscar(raiz, nombre, JTable.class);
    }

    static JLabel etiqueta(java.awt.Container raiz, String nombre) {
        return buscar(raiz, nombre, JLabel.class);
    }

    static void agregarAVenta(MainFrame f, String texto) {
        escribirYEnter(campo(f, "txtBuscar"), texto);
    }

    static void ponerEntero(JDialog d, String nombre, int valor) {
        javax.swing.JSpinner s = buscar(d, nombre, javax.swing.JSpinner.class);
        enEdt(() -> s.setValue(valor));
    }

    static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Abre un cuadro con el botón dado, lo deja ejecutar la acción y espera a que se cierre. */
    static void conCuadro(JButton disparador, String titulo, java.util.function.Consumer<JDialog> accion) {
        CountDownLatch fin = clic(disparador);
        JDialog d = esperarDialogo(titulo);
        accion.accept(d);
        esperar(fin);
    }

    /** Cierra un cuadro de confirmación o de error de JOptionPane pulsando el botón indicado (o el primero si es nulo). */
    static void responderOpcion(String titulo, String textoBoton) {
        JDialog d = esperarDialogo(titulo);
        if (textoBoton == null) {
            pulsar(UiRobot.todos(d, javax.swing.AbstractButton.class).get(0));
        } else {
            pulsar(UiRobot.botonPorTexto(d, textoBoton));
        }
    }
}
