package com.puntoventa.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RootPaneContainer;
import javax.swing.text.JTextComponent;

/**
 * Ayuda para probar la interfaz Swing de verdad: busca componentes por nombre, hace clic, escribe,
 * espera cuadros modales y puede tomar capturas de las pantallas reales.
 */
public final class UiRobot {

    private UiRobot() {
    }

    // ------------------------------------------------------------------ hilo de eventos
    public static void enEdt(Runnable accion) {
        if (EventQueue.isDispatchThread()) {
            accion.run();
            return;
        }
        try {
            EventQueue.invokeAndWait(accion);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException r) {
                throw r;
            }
            if (causa instanceof Error err) {
                throw err;
            }
            throw new IllegalStateException(causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public static <T> T enEdtGet(Supplier<T> proveedor) {
        AtomicReference<T> resultado = new AtomicReference<>();
        enEdt(() -> resultado.set(proveedor.get()));
        return resultado.get();
    }

    /** Lanza una acción que puede abrir un cuadro modal sin bloquear a quien prueba. */
    public static CountDownLatch lanzar(Runnable accion) {
        CountDownLatch fin = new CountDownLatch(1);
        EventQueue.invokeLater(() -> {
            try {
                accion.run();
            } finally {
                fin.countDown();
            }
        });
        return fin;
    }

    public static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("La acción no terminó en 15 segundos (¿quedó un cuadro modal abierto?)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ búsqueda
    public static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        T encontrado = buscarOpcional(raiz, nombre, tipo);
        if (encontrado == null) {
            throw new AssertionError("No se encontró el componente «" + nombre + "» (" + tipo.getSimpleName() + ")");
        }
        return encontrado;
    }

    public static <T extends Component> T buscarOpcional(Container raiz, String nombre, Class<T> tipo) {
        return enEdtGet(() -> recorrer(raiz, nombre, tipo));
    }

    private static <T extends Component> T recorrer(Container c, String nombre, Class<T> tipo) {
        for (Component hijo : c.getComponents()) {
            if (nombre.equals(hijo.getName()) && tipo.isInstance(hijo)) {
                return tipo.cast(hijo);
            }
            if (hijo instanceof Container cont) {
                T r = recorrer(cont, nombre, tipo);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /** Todos los componentes de un tipo (por ejemplo, todas las etiquetas). */
    public static <T extends Component> List<T> todos(Container raiz, Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        enEdt(() -> acumular(raiz, tipo, lista));
        return lista;
    }

    private static <T extends Component> void acumular(Container c, Class<T> tipo, List<T> lista) {
        for (Component hijo : c.getComponents()) {
            if (tipo.isInstance(hijo)) {
                lista.add(tipo.cast(hijo));
            }
            if (hijo instanceof Container cont) {
                acumular(cont, tipo, lista);
            }
        }
    }

    // ------------------------------------------------------------------ acciones
    public static void escribir(JTextComponent campo, String texto) {
        enEdt(() -> campo.setText(texto));
    }

    /** Escribe y pulsa Enter en el campo. */
    public static void escribirYEnter(javax.swing.JTextField campo, String texto) {
        enEdt(() -> {
            campo.setText(texto);
            campo.postActionEvent();
        });
    }

    public static void pulsar(AbstractButton boton) {
        enEdt(boton::doClick);
    }

    public static void seleccionarFila(JTable tabla, int fila) {
        enEdt(() -> tabla.setRowSelectionInterval(fila, fila));
    }

    public static void seleccionarPestana(Container ventana, String titulo) {
        JTabbedPane pestanas = buscar(ventana, "pestanas", JTabbedPane.class);
        enEdt(() -> {
            for (int i = 0; i < pestanas.getTabCount(); i++) {
                if (pestanas.getTitleAt(i).equals(titulo)) {
                    pestanas.setSelectedIndex(i);
                    return;
                }
            }
            throw new AssertionError("No existe la pestaña " + titulo);
        });
    }

    public static List<String> titulosPestanas(Container ventana) {
        JTabbedPane pestanas = buscar(ventana, "pestanas", JTabbedPane.class);
        return enEdtGet(() -> {
            List<String> t = new ArrayList<>();
            for (int i = 0; i < pestanas.getTabCount(); i++) {
                t.add(pestanas.getTitleAt(i));
            }
            return t;
        });
    }

    public static String texto(javax.swing.JLabel etiqueta) {
        return enEdtGet(etiqueta::getText);
    }

    public static Object celda(JTable tabla, int fila, int columna) {
        return enEdtGet(() -> tabla.getValueAt(fila, columna));
    }

    public static int filas(JTable tabla) {
        return enEdtGet(tabla::getRowCount);
    }

    // ------------------------------------------------------------------ ventanas
    /** Espera a que haya una ventana visible que cumpla la condición. */
    public static Window esperarVentana(Predicate<Window> condicion, long milis) {
        long limite = System.currentTimeMillis() + milis;
        while (System.currentTimeMillis() < limite) {
            Window w = enEdtGet(() -> {
                for (Window v : Window.getWindows()) {
                    if (v.isShowing() && condicion.test(v)) {
                        return v;
                    }
                }
                return null;
            });
            if (w != null) {
                return w;
            }
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        throw new AssertionError("No apareció la ventana esperada en " + milis + " ms");
    }

    public static JDialog esperarDialogo(String tituloContiene) {
        return (JDialog) esperarVentana(w -> w instanceof JDialog d && d.getTitle() != null
                && d.getTitle().contains(tituloContiene), 8000);
    }

    /** Cierra todas las ventanas abiertas (para no dejar procesos colgados entre pruebas). */
    public static void cerrarTodo() {
        enEdt(() -> {
            for (Window w : Window.getWindows()) {
                w.dispose();
            }
        });
    }

    // ------------------------------------------------------------------ ayudas para cuadros estándar
    /** Busca un botón por el texto que muestra (por ejemplo, en un cuadro de confirmación). */
    public static AbstractButton botonPorTexto(Container raiz, String texto) {
        List<AbstractButton> botones = todos(raiz, AbstractButton.class);
        for (AbstractButton b : botones) {
            if (texto.equals(enEdtGet(b::getText))) {
                return b;
            }
        }
        throw new AssertionError("No hay un botón con el texto «" + texto + "»");
    }

    /** Espera el cuadro que contiene un selector de archivos (guardar como…). */
    public static JDialog esperarSelectorDeArchivos() {
        return (JDialog) esperarVentana(w -> w instanceof JDialog d
                && !todos(d, javax.swing.JFileChooser.class).isEmpty(), 8000);
    }

    public static javax.swing.JFileChooser selectorDeArchivos(Container raiz) {
        List<javax.swing.JFileChooser> l = todos(raiz, javax.swing.JFileChooser.class);
        if (l.isEmpty()) {
            throw new AssertionError("El cuadro no contiene un selector de archivos");
        }
        return l.get(0);
    }

    /** Elige el archivo en un selector de archivos y lo acepta. */
    public static void guardarEn(javax.swing.JFileChooser selector, java.io.File archivo) {
        enEdt(() -> {
            selector.setSelectedFile(archivo);
            selector.approveSelection();
        });
    }

    /** ¿Alguna ventana visible es de este tipo? */
    public static boolean hayVentana(Class<? extends Window> tipo) {
        return enEdtGet(() -> {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && tipo.isInstance(w)) {
                    return true;
                }
            }
            return false;
        });
    }

    public static boolean visible(Component c) {
        return enEdtGet(c::isVisible);
    }

    public static boolean habilitado(Component c) {
        return enEdtGet(c::isEnabled);
    }

    /** Ejecuta la acción asociada a una tecla en el panel (por ejemplo F9) como si se hubiera pulsado. */
    public static void teclear(javax.swing.JComponent componente, int tecla, int condicion) {
        enEdt(() -> {
            javax.swing.KeyStroke ks = javax.swing.KeyStroke.getKeyStroke(tecla, 0);
            Object clave = componente.getInputMap(condicion).get(ks);
            if (clave == null) {
                throw new AssertionError("La tecla " + ks + " no tiene acción asociada");
            }
            componente.getActionMap().get(clave).actionPerformed(
                    new java.awt.event.ActionEvent(componente, java.awt.event.ActionEvent.ACTION_PERFORMED, "tecla"));
        });
    }

    // ------------------------------------------------------------------ capturas
    public static BufferedImage capturar(Window ventana, int ancho, int alto) {
        enEdt(() -> {
            ventana.pack();
            ventana.setSize(ancho, alto);
            ventana.validate();
            ventana.doLayout();
        });
        return enEdtGet(() -> pintar(ventana));
    }

    /** Captura una ventana ya visible (por ejemplo, un cuadro modal) con su tamaño actual. */
    public static BufferedImage capturarVisible(Window ventana) {
        return enEdtGet(() -> pintar(ventana));
    }

    private static BufferedImage pintar(Window ventana) {
        int ancho = ventana.getWidth();
        int alto = ventana.getHeight();
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, ancho, alto);
        ((RootPaneContainer) ventana).getRootPane().paint(g);
        g.dispose();
        return img;
    }
}
