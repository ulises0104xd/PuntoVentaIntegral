package com.puntoventa.ui;

import com.puntoventa.model.TablaDatos;
import com.puntoventa.util.DataAccessException;
import com.puntoventa.util.Formato;
import com.puntoventa.util.NegocioException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Componentes y utilidades comunes de la capa de presentación. */
final class Ui {

    enum Tipo { PRIMARIO, SECUNDARIO, PELIGRO, TEXTO }

    enum Aviso { EXITO, INFO, ERROR }

    /** Quien puede mostrar avisos breves (la ventana principal). */
    interface Avisador {
        void avisar(String mensaje, Aviso tipo);
    }

    private static final Set<String> COLUMNAS_MONEDA =
            Set.of("Precio", "Subtotal", "Descuentos", "Ingresos", "Importe");
    private static final Set<String> COLUMNAS_DERECHA = Set.of("Precio", "Cantidad", "Subtotal", "Existencias", "Mínimo",
            "Ventas", "Descuentos", "Ingresos", "Importe", "Unidades", "Existencia resultante");

    private Ui() {
    }

    // ------------------------------------------------------------------ componentes
    static JButton boton(String texto, Tipo tipo) {
        JButton b = new JButton(texto);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switch (tipo) {
            case PRIMARIO -> b.putClientProperty("FlatLaf.style",
                    "background:#2F6FED; foreground:#FFFFFF; hoverBackground:#2459C9; pressedBackground:#1D49A6; "
                            + "focusedBackground:#2459C9; borderWidth:0; arc:10; margin:7,18,7,18");
            case PELIGRO -> b.putClientProperty("FlatLaf.style",
                    "foreground:#C62828; borderColor:#E5B4B1; arc:10; margin:7,16,7,16");
            case TEXTO -> b.putClientProperty("JButton.buttonType", "borderless");
            default -> b.putClientProperty("FlatLaf.style", "arc:10; margin:7,16,7,16");
        }
        return b;
    }

    static JTextField campo(String nombre, String placeholder, int columnas) {
        JTextField c = new JTextField(columnas);
        c.setName(nombre);
        c.putClientProperty("JTextField.placeholderText", placeholder);
        c.putClientProperty("FlatLaf.style", "margin:5,10,5,10");
        return c;
    }

    static JPasswordField clave(String nombre, String placeholder, int columnas) {
        JPasswordField c = new JPasswordField(columnas);
        c.setName(nombre);
        c.putClientProperty("JTextField.placeholderText", placeholder);
        c.putClientProperty("JPasswordField.showRevealButton", true);
        c.putClientProperty("FlatLaf.style", "margin:5,10,5,10");
        return c;
    }

    static JLabel titulo(String texto, float tamano) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fuente(tamano, java.awt.Font.BOLD));
        l.setForeground(Tema.TEXTO);
        l.setBorder(new EmptyBorder(0, 0, 0, 4));
        return l;
    }

    static JLabel suave(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.SUAVE);
        return l;
    }

    /** Etiqueta con fondo de color (para roles y estados). */
    static JLabel chip(String texto, Color letra, Color fondo) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setForeground(letra);
        l.setFont(Tema.fuente(12f, java.awt.Font.BOLD));
        l.putClientProperty("FlatLaf.style", "background:" + hex(fondo) + "; arc:999; border:2,10,2,10");
        l.setOpaque(true);
        return l;
    }

    static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Panel blanco con esquinas redondeadas y borde fino. */
    static class Tarjeta extends JPanel {
        Tarjeta() {
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.SUPERFICIE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(Tema.BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static JPanel columna() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    // ------------------------------------------------------------------ tablas
    static DefaultTableModel modeloSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setFillsViewportHeight(true);
        t.setRowHeight(34);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(Tema.LINEA);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultRenderer(Object.class, new CeldaRenderer());
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new EncabezadoRenderer());
        h.setPreferredSize(new Dimension(100, 36));
        return t;
    }

    /** Ajusta el ancho de cada columna a su contenido (con tope) y reparte el espacio sobrante. */
    static void ajustarAnchos(JTable t) {
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        java.awt.FontMetrics fm = t.getFontMetrics(t.getFont());
        for (int c = 0; c < t.getColumnCount(); c++) {
            int ancho = fm.stringWidth(String.valueOf(t.getColumnName(c))) + 44;
            for (int r = 0; r < Math.min(t.getRowCount(), 120); r++) {
                Object v = t.getValueAt(r, c);
                ancho = Math.max(ancho, fm.stringWidth(v == null ? "" : v.toString()) + 36);
            }
            t.getColumnModel().getColumn(c).setPreferredWidth(Math.min(ancho, 420));
        }
    }

    static JScrollPane desplazable(JComponent contenido) {
        JScrollPane sp = new JScrollPane(contenido);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        sp.getViewport().setBackground(Tema.SUPERFICIE);
        return sp;
    }

    /** Carga una tabla genérica dándole formato de moneda a las columnas de importes. */
    static void cargar(DefaultTableModel modelo, TablaDatos datos) {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(datos.columnas().toArray());
        for (Object[] fila : datos.filas()) {
            Object[] nueva = fila.clone();
            for (int i = 0; i < nueva.length; i++) {
                String columna = datos.columnas().get(i);
                if (COLUMNAS_MONEDA.contains(columna) && nueva[i] instanceof BigDecimal d) {
                    nueva[i] = Formato.dinero(d);
                } else if (nueva[i] instanceof java.sql.Timestamp ts) {
                    nueva[i] = Formato.fechaHora(ts.toLocalDateTime());
                } else if (nueva[i] instanceof java.time.LocalDateTime ldt) {
                    nueva[i] = Formato.fechaHora(ldt);
                }
            }
            modelo.addRow(nueva);
        }
    }

    /** Pinta zebra, alinea cifras y da color a estados y tipos. */
    static class CeldaRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            String texto = valor == null ? "" : valor.toString();
            String nombre = tabla.getColumnName(columna);
            super.getTableCellRendererComponent(tabla, texto, seleccionada, false, fila, columna);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(COLUMNAS_DERECHA.contains(nombre) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setForeground(Tema.TEXTO);
            setFont(tabla.getFont());
            if (!seleccionada) {
                setBackground(fila % 2 == 0 ? Tema.SUPERFICIE : Tema.ZEBRA);
            }
            switch (texto) {
                case "Activo", "OK", "ENTRADA" -> estilo(Tema.EXITO, true);
                case "Inactivo" -> estilo(Tema.SUAVE, true);
                case "BAJO", "AJUSTE" -> estilo(Tema.AVISO, true);
                case "AGOTADO" -> estilo(Tema.PELIGRO, true);
                case "ADMIN" -> estilo(Tema.INFO, true);
                case "VENTA", "CAJERO" -> estilo(Tema.SUAVE, true);
                default -> {
                }
            }
            if ("Cantidad".equals(nombre) && texto.startsWith("-")) {
                setForeground(Tema.PELIGRO);
            } else if ("Cantidad".equals(nombre) && texto.startsWith("+")) {
                setForeground(Tema.EXITO);
            }
            return this;
        }

        private void estilo(Color color, boolean negrita) {
            setForeground(color);
            setFont(getFont().deriveFont(negrita ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
        }
    }

    static final class EncabezadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            setBackground(new Color(0xF2F4F7));
            setForeground(Tema.SUAVE);
            setFont(tabla.getFont().deriveFont(java.awt.Font.BOLD, 12.5f));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), new EmptyBorder(0, 12, 0, 12)));
            setHorizontalAlignment(COLUMNAS_DERECHA.contains(String.valueOf(valor))
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }

    // ------------------------------------------------------------------ mensajes
    static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "No se pudo completar la acción", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirmar(Component padre, String titulo, String mensaje, String textoSi) {
        Object[] opciones = {textoSi, "Cancelar"};
        return JOptionPane.showOptionDialog(padre, mensaje, titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[1]) == 0;
    }

    /** Aviso breve y no intrusivo en la barra de estado de la ventana principal. */
    static void aviso(Component origen, String mensaje, Aviso tipo) {
        Window w = origen instanceof Window v ? v : SwingUtilities.getWindowAncestor(origen);
        while (w != null && !(w instanceof Avisador)) {
            w = w.getOwner();
        }
        if (w instanceof Avisador a) {
            a.avisar(mensaje, tipo);
        }
    }

    /** Ejecuta una acción y muestra al usuario el mensaje de los errores esperados. */
    static void ejecutar(Component padre, Runnable accion) {
        try {
            accion.run();
        } catch (NegocioException e) {
            error(padre, e.getMessage());
        } catch (DataAccessException e) {
            error(padre, "Ocurrió un problema con la base de datos.\n" + e.getMessage());
        }
    }

    /** Ejecuta una acción y entrega el mensaje de error esperado a quien la llamó (sin ventanas emergentes). */
    static void ejecutarSilencioso(Component padre, Runnable accion, java.util.function.Consumer<String> alError) {
        try {
            accion.run();
        } catch (NegocioException e) {
            alError.accept(e.getMessage());
        } catch (DataAccessException e) {
            alError.accept("Ocurrió un problema con la base de datos: " + e.getMessage());
        }
    }

    /** Ventana propietaria de un componente (para abrir cuadros modales sobre ella). */
    static Window ventana(Component c) {
        return c instanceof Window w ? w : SwingUtilities.getWindowAncestor(c);
    }

    // ------------------------------------------------------------------ conversiones
    static int entero(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException(campo + " debe ser un número entero");
        }
    }

    static BigDecimal decimal(String texto, String campo) {
        try {
            return new BigDecimal(texto.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new NegocioException(campo + " debe ser un número");
        }
    }

    // ------------------------------------------------------------------ fechas
    static JSpinner selectorFecha(String nombre, LocalDate inicial) {
        SpinnerDateModel modelo = new SpinnerDateModel(aDate(inicial), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner s = new JSpinner(modelo);
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        s.setName(nombre);
        s.setPreferredSize(new Dimension(130, 34));
        return s;
    }

    static Date aDate(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    static LocalDate fechaDe(JSpinner selector, String campo) {
        try {
            selector.commitEdit();
        } catch (java.text.ParseException e) {
            throw new NegocioException(campo + " debe tener el formato AAAA-MM-DD");
        }
        return ((Date) selector.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Fila de formulario: etiqueta arriba y componente debajo. */
    static JPanel filaCampo(String etiqueta, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        if (etiqueta != null && !etiqueta.isEmpty()) {
            JLabel l = new JLabel(etiqueta);
            l.setFont(Tema.fuente(12.5f, java.awt.Font.BOLD));
            l.setForeground(Tema.SUAVE);
            p.add(l, BorderLayout.NORTH);
        }
        p.add(componente, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        return p;
    }
}
