package com.puntoventa.ui;

import com.puntoventa.model.Carrito;
import com.puntoventa.model.LineaVenta;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Totales;
import com.puntoventa.model.Usuario;
import com.puntoventa.model.Venta;
import com.puntoventa.service.AuthService;
import com.puntoventa.service.ProductoService;
import com.puntoventa.service.TicketService;
import com.puntoventa.service.VentaService;
import com.puntoventa.util.DataAccessException;
import com.puntoventa.util.Formato;
import com.puntoventa.util.NegocioException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** CU03, CU04, CU05 y CU06: pantalla de venta pensada para operar con teclado y lector de códigos. */
public class VentaPanel extends JPanel {

    static {
        Tema.instalar();
    }

    private final Usuario usuario;
    private final ProductoService productos = new ProductoService();
    private final VentaService ventas = new VentaService();
    private final AuthService auth = new AuthService();
    private final TicketService tickets = new TicketService();

    private Carrito carrito = new Carrito();
    private BigDecimal descuentoPct = BigDecimal.ZERO;
    private Usuario autorizador;

    private final JTextField txtBuscar = Ui.campo("txtBuscar", "Escanee o escriba el código o el nombre y presione Enter", 40);
    private final JLabel lblMensaje = new JLabel(" ");
    private final DefaultTableModel modelo = Ui.modeloSoloLectura("Código", "Producto", "Precio", "Cantidad", "Subtotal");
    private final JTable tabla = Ui.tabla(modelo);
    private final CardLayout cartas = new CardLayout();
    private final JPanel zonaTabla = new JPanel(cartas);
    private final JLabel lblArticulos = Ui.suave("0 artículos");
    private final JLabel lblSubtotal = new JLabel("$0.00", SwingConstants.RIGHT);
    private final JLabel lblDescuento = new JLabel("-$0.00", SwingConstants.RIGHT);
    private final JLabel lblDescuentoInfo = Ui.suave(" ");
    private final JLabel lblTotal = new JLabel("$0.00", SwingConstants.RIGHT);
    private final JTextField txtDescuento = Ui.campo("txtDescuento", "0", 5);
    private final JButton btnAplicarDescuento = Ui.boton("Aplicar", Ui.Tipo.SECUNDARIO);
    private final JButton btnCobrar = Ui.boton("Cobrar  (F9)", Ui.Tipo.PRIMARIO);
    private final JButton btnMenos = Ui.boton("−", Ui.Tipo.SECUNDARIO);
    private final JButton btnMas = Ui.boton("+", Ui.Tipo.SECUNDARIO);
    private final JButton btnCantidad = Ui.boton("Cantidad (F2)", Ui.Tipo.SECUNDARIO);
    private final JButton btnQuitar = Ui.boton("Quitar (Supr)", Ui.Tipo.SECUNDARIO);
    private final JButton btnVaciar = Ui.boton("Vaciar venta", Ui.Tipo.TEXTO);

    public VentaPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(18, 12));
        setOpaque(false);

        add(construirIzquierda(), BorderLayout.CENTER);
        add(construirResumen(), BorderLayout.EAST);

        tabla.setName("tablaVenta");
        btnCobrar.setName("btnCobrar");
        btnAplicarDescuento.setName("btnAplicarDescuento");
        btnMenos.setName("btnMenos");
        btnMas.setName("btnMas");
        btnCantidad.setName("btnCantidad");
        btnQuitar.setName("btnQuitar");
        btnVaciar.setName("btnVaciar");
        lblMensaje.setName("lblMensaje");
        lblTotal.setName("lblTotal");
        lblSubtotal.setName("lblSubtotal");
        lblDescuento.setName("lblDescuento");
        lblDescuentoInfo.setName("lblDescuentoInfo");
        lblArticulos.setName("lblArticulos");

        txtBuscar.addActionListener(e -> agregar());
        btnMenos.addActionListener(e -> ajustar(-1));
        btnMas.addActionListener(e -> ajustar(1));
        btnCantidad.addActionListener(e -> cambiarCantidad());
        btnQuitar.addActionListener(e -> quitar());
        btnVaciar.addActionListener(e -> vaciar());
        btnAplicarDescuento.addActionListener(e -> aplicarDescuento());
        txtDescuento.addActionListener(e -> aplicarDescuento());
        btnCobrar.addActionListener(e -> cobrar());
        atajos();
        refrescarVista();
    }

    // ------------------------------------------------------------------ construcción
    private JPanel construirIzquierda() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel buscador = Ui.columna();
        JPanel fila = new JPanel(new BorderLayout(10, 0));
        fila.setOpaque(false);
        fila.add(txtBuscar, BorderLayout.CENTER);
        JButton agregar = Ui.boton("Agregar", Ui.Tipo.PRIMARIO);
        agregar.setName("btnAgregar");
        agregar.addActionListener(e -> agregar());
        fila.add(agregar, BorderLayout.EAST);
        txtBuscar.putClientProperty("FlatLaf.style", "margin:8,12,8,12");
        buscador.add(fila);
        lblMensaje.setBorder(new EmptyBorder(6, 2, 0, 0));
        lblMensaje.setAlignmentX(LEFT_ALIGNMENT);
        JPanel filaMensaje = new JPanel(new BorderLayout());
        filaMensaje.setOpaque(false);
        filaMensaje.add(lblMensaje, BorderLayout.CENTER);
        buscador.add(filaMensaje);
        p.add(buscador, BorderLayout.NORTH);

        zonaTabla.setOpaque(false);
        zonaTabla.add(Ui.desplazable(tabla), "tabla");
        JPanel vacio = new JPanel(new GridBagLayout());
        vacio.setBackground(Tema.SUPERFICIE);
        vacio.setBorder(javax.swing.BorderFactory.createLineBorder(Tema.BORDE));
        JPanel textoVacio = Ui.columna();
        JLabel t1 = Ui.titulo("La venta está vacía", 18f);
        t1.setAlignmentX(CENTER_ALIGNMENT);
        JLabel t2 = Ui.suave("Escanee un producto o escriba su código o nombre arriba y presione Enter.");
        t2.setAlignmentX(CENTER_ALIGNMENT);
        textoVacio.add(t1);
        textoVacio.add(javax.swing.Box.createVerticalStrut(6));
        textoVacio.add(t2);
        vacio.add(textoVacio);
        zonaTabla.add(vacio, "vacio");
        p.add(zonaTabla, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnMenos);
        acciones.add(btnMas);
        acciones.add(btnCantidad);
        acciones.add(btnQuitar);
        acciones.add(btnVaciar);
        p.add(acciones, BorderLayout.SOUTH);
        return p;
    }

    private JPanel construirResumen() {
        Ui.Tarjeta card = new Ui.Tarjeta();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(350, 0));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 8, 0);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.gridx = 0;
        g.gridwidth = 2;
        g.gridy = 0;
        card.add(Ui.titulo("Resumen de la venta", 17f), g);
        g.gridy = 1;
        card.add(lblArticulos, g);

        g.gridwidth = 1;
        g.gridy = 2;
        g.insets = new Insets(14, 0, 6, 0);
        g.weightx = 0;
        card.add(new JLabel("Subtotal"), g);
        g.gridx = 1;
        g.weightx = 1;
        lblSubtotal.setFont(Tema.fuente(15f, java.awt.Font.BOLD));
        card.add(lblSubtotal, g);

        g.gridx = 0;
        g.gridy = 3;
        g.weightx = 0;
        g.insets = new Insets(0, 0, 6, 0);
        card.add(new JLabel("Descuento"), g);
        g.gridx = 1;
        g.weightx = 1;
        lblDescuento.setFont(Tema.fuente(15f, java.awt.Font.BOLD));
        lblDescuento.setForeground(Tema.EXITO);
        card.add(lblDescuento, g);

        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        g.insets = new Insets(10, 0, 0, 0);
        card.add(new JLabel("Descuento (%)  ·  F4"), g);
        JPanel filaDescuento = new JPanel(new BorderLayout(8, 0));
        filaDescuento.setOpaque(false);
        txtDescuento.setText("0");
        filaDescuento.add(txtDescuento, BorderLayout.CENTER);
        filaDescuento.add(btnAplicarDescuento, BorderLayout.EAST);
        g.gridy = 5;
        g.insets = new Insets(4, 0, 0, 0);
        card.add(filaDescuento, g);
        g.gridy = 6;
        g.insets = new Insets(4, 0, 12, 0);
        lblDescuentoInfo.setFont(Tema.fuente(12f, java.awt.Font.PLAIN));
        card.add(lblDescuentoInfo, g);

        g.gridy = 7;
        g.insets = new Insets(4, 0, 4, 0);
        card.add(new javax.swing.JSeparator(), g);
        g.gridy = 8;
        g.insets = new Insets(10, 0, 0, 0);
        card.add(Ui.suave("TOTAL"), g);
        g.gridy = 9;
        g.insets = new Insets(0, 0, 16, 0);
        lblTotal.setFont(Tema.fuente(38f, java.awt.Font.BOLD));
        lblTotal.setForeground(Tema.TEXTO);
        card.add(lblTotal, g);

        g.gridy = 10;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        card.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, g);
        g.weighty = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 11;
        g.insets = new Insets(0, 0, 0, 0);
        btnCobrar.setFont(Tema.fuente(17f, java.awt.Font.BOLD));
        btnCobrar.putClientProperty("FlatLaf.style",
                "background:#2F6FED; foreground:#FFFFFF; hoverBackground:#2459C9; borderWidth:0; arc:12; margin:14,16,14,16");
        card.add(btnCobrar, g);
        g.gridy = 12;
        g.insets = new Insets(10, 0, 0, 0);
        JLabel ayuda = Ui.suave("F2 cantidad · Supr quitar · F3 buscar");
        ayuda.setFont(Tema.fuente(12f, java.awt.Font.PLAIN));
        ayuda.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(ayuda, g);
        return card;
    }

    private void atajos() {
        asociar(this, KeyEvent.VK_F9, "cobrar", this::cobrar);
        asociar(this, KeyEvent.VK_F3, "buscar", () -> {
            txtBuscar.requestFocusInWindow();
            txtBuscar.selectAll();
        });
        asociar(this, KeyEvent.VK_F4, "descuento", () -> {
            txtDescuento.requestFocusInWindow();
            txtDescuento.selectAll();
        });
        asociar(this, KeyEvent.VK_F2, "cantidad", this::cambiarCantidad);
        asociar(tabla, KeyEvent.VK_F2, "cantidad", this::cambiarCantidad);
        asociar(tabla, KeyEvent.VK_DELETE, "quitar", this::quitar);
        asociar(tabla, KeyEvent.VK_ADD, "mas", () -> ajustar(1));
        asociar(tabla, KeyEvent.VK_SUBTRACT, "menos", () -> ajustar(-1));
        asociar(txtBuscar, KeyEvent.VK_ESCAPE, "limpiar", () -> {
            txtBuscar.setText("");
            mensaje(" ", null);
        });
    }

    private static void asociar(JComponent c, int tecla, String nombre, Runnable accion) {
        int condicion = c instanceof JTable || c instanceof JTextField
                ? JComponent.WHEN_FOCUSED : JComponent.WHEN_IN_FOCUSED_WINDOW;
        c.getInputMap(condicion).put(KeyStroke.getKeyStroke(tecla, 0), nombre);
        c.getActionMap().put(nombre, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accion.run();
            }
        });
    }

    void enfocarBusqueda() {
        SwingUtilities.invokeLater(() -> txtBuscar.requestFocusInWindow());
    }

    // ------------------------------------------------------------------ acciones
    private Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void mensaje(String texto, Ui.Aviso tipo) {
        lblMensaje.setText(texto);
        if (tipo == Ui.Aviso.ERROR) {
            lblMensaje.setForeground(Tema.PELIGRO);
        } else if (tipo == Ui.Aviso.EXITO) {
            lblMensaje.setForeground(Tema.EXITO);
        } else {
            lblMensaje.setForeground(Tema.SUAVE);
        }
        txtBuscar.putClientProperty("JComponent.outline", tipo == Ui.Aviso.ERROR ? "error" : null);
    }

    private void agregar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            return;
        }
        try {
            Optional<Producto> elegido = productos.buscarPorCodigo(texto);
            if (elegido.isEmpty()) {
                List<Producto> encontrados = productos.buscar(texto);
                if (encontrados.isEmpty()) {
                    mensaje("No se encontró ningún producto con «" + texto + "».", Ui.Aviso.ERROR);
                    txtBuscar.selectAll();
                    return;
                }
                elegido = encontrados.size() == 1
                        ? Optional.of(encontrados.get(0))
                        : SelectorProductoDialog.elegir(this, encontrados, texto);
            }
            if (elegido.isPresent()) {
                carrito.agregar(elegido.get(), 1);
                txtBuscar.setText("");
                mensaje("Agregado: " + elegido.get().nombre(), Ui.Aviso.EXITO);
                refrescarVista();
                seleccionar(indiceDe(elegido.get()));
            }
        } catch (NegocioException e) {
            mensaje(e.getMessage(), Ui.Aviso.ERROR);
            txtBuscar.selectAll();
        } catch (DataAccessException e) {
            mensaje("Problema con la base de datos: " + e.getMessage(), Ui.Aviso.ERROR);
        }
        txtBuscar.requestFocusInWindow();
    }

    private int indiceDe(Producto p) {
        List<LineaVenta> l = carrito.lineas();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).producto().id() == p.id()) {
                return i;
            }
        }
        return -1;
    }

    private void seleccionar(int fila) {
        if (fila >= 0 && fila < tabla.getRowCount()) {
            tabla.setRowSelectionInterval(fila, fila);
            tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
        }
    }

    private int filaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            mensaje("Seleccione un renglón de la venta.", Ui.Aviso.ERROR);
        }
        return fila;
    }

    private void ajustar(int delta) {
        int fila = filaSeleccionada();
        if (fila < 0) {
            return;
        }
        Ui.ejecutarSilencioso(this, () -> {
            int nueva = carrito.lineas().get(fila).cantidad() + delta;
            carrito.cambiarCantidad(fila, nueva);
            refrescarVista();
            seleccionar(Math.min(fila, tabla.getRowCount() - 1));
        }, m -> mensaje(m, Ui.Aviso.ERROR));
    }

    private void cambiarCantidad() {
        int fila = filaSeleccionada();
        if (fila < 0) {
            return;
        }
        LineaVenta linea = carrito.lineas().get(fila);
        boolean ok = new FormularioDialog(ventana(), "Cambiar cantidad", "Aceptar")
                .nota("<b>" + linea.producto().nombre() + "</b><br>Existencias disponibles: " + linea.producto().existencias()
                        + ". Con 0 se quita el renglón.")
                .entero("cantidad", "Cantidad", linea.cantidad(), 0, 9999)
                .mostrar(v -> carrito.cambiarCantidad(fila, v.entero("cantidad")));
        if (ok) {
            refrescarVista();
            seleccionar(Math.min(fila, tabla.getRowCount() - 1));
        }
    }

    private void quitar() {
        int fila = filaSeleccionada();
        if (fila < 0) {
            return;
        }
        String nombre = carrito.lineas().get(fila).producto().nombre();
        carrito.quitar(fila);
        mensaje("Quitado: " + nombre, Ui.Aviso.INFO);
        refrescarVista();
        seleccionar(Math.min(fila, tabla.getRowCount() - 1));
    }

    private void vaciar() {
        if (carrito.estaVacio()) {
            return;
        }
        if (Ui.confirmar(this, "Vaciar la venta", "Se quitarán todos los productos de la venta actual.", "Vaciar venta")) {
            nuevaVenta();
            mensaje("Venta vaciada.", Ui.Aviso.INFO);
        }
    }

    private void nuevaVenta() {
        carrito = new Carrito();
        descuentoPct = BigDecimal.ZERO;
        autorizador = null;
        txtDescuento.setText("0");
        refrescarVista();
        txtBuscar.requestFocusInWindow();
    }

    private void aplicarDescuento() {
        try {
            BigDecimal pct = Ui.decimal(txtDescuento.getText(), "El descuento");
            carrito.totales(pct); // valida el rango 0-100
            Usuario nuevoAutorizador = null;
            if (usuario.rol() != Rol.ADMIN && pct.compareTo(VentaService.LIMITE_DESCUENTO_CAJERO) > 0) {
                nuevoAutorizador = pedirAutorizacion(pct);
                if (nuevoAutorizador == null) {
                    return;
                }
            }
            descuentoPct = pct;
            autorizador = nuevoAutorizador;
            txtDescuento.putClientProperty("JComponent.outline", null);
            refrescarVista();
            mensaje(pct.signum() == 0 ? "Descuento quitado." : "Descuento de " + pct.stripTrailingZeros().toPlainString() + " % aplicado.",
                    Ui.Aviso.EXITO);
        } catch (NegocioException e) {
            txtDescuento.putClientProperty("JComponent.outline", "error");
            mensaje(e.getMessage(), Ui.Aviso.ERROR);
        }
    }

    private Usuario pedirAutorizacion(BigDecimal pct) {
        Usuario[] resultado = new Usuario[1];
        boolean ok = new FormularioDialog(ventana(), "Autorización de administrador", "Autorizar")
                .nota("Un descuento de <b>" + pct.stripTrailingZeros().toPlainString()
                        + " %</b> es mayor al límite del cajero. Pida a un administrador que se identifique.")
                .texto("usuario", "Usuario del administrador", "", "Usuario")
                .clave("clave", "Contraseña", "Contraseña")
                .mostrar(v -> {
                    char[] clave = v.clave("clave");
                    try {
                        resultado[0] = auth.autorizarAdministrador(v.texto("usuario"), clave)
                                .orElseThrow(() -> new NegocioException("Credenciales de administrador incorrectas."));
                    } finally {
                        Arrays.fill(clave, '\0');
                    }
                });
        return ok ? resultado[0] : null;
    }

    private void cobrar() {
        if (carrito.estaVacio()) {
            mensaje("Agregue al menos un producto antes de cobrar.", Ui.Aviso.ERROR);
            txtBuscar.requestFocusInWindow();
            return;
        }
        Totales t = carrito.totales(descuentoPct);
        Optional<CobroDialog.Pago> pago = CobroDialog.pedir(ventana(), t.total());
        if (pago.isEmpty()) {
            txtBuscar.requestFocusInWindow();
            return;
        }
        try {
            Venta venta = ventas.cobrar(carrito, descuentoPct, usuario, autorizador, pago.get().metodo(), pago.get().recibido());
            nuevaVenta();
            mensaje(" ", null);
            Ui.aviso(this, "Venta #" + String.format("%06d", venta.id()) + " registrada por " + Formato.dinero(venta.totales().total()),
                    Ui.Aviso.EXITO);
            new TicketDialog(ventana(), venta, tickets.generar(venta)).setVisible(true);
            txtBuscar.requestFocusInWindow();
        } catch (NegocioException e) {
            Ui.error(this, e.getMessage());
        } catch (DataAccessException e) {
            Ui.error(this, "Ocurrió un problema con la base de datos. La venta no se registró.\n" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ vista
    private void refrescarVista() {
        modelo.setRowCount(0);
        int piezas = 0;
        for (LineaVenta l : carrito.lineas()) {
            modelo.addRow(new Object[]{l.producto().codigo(), l.producto().nombre(),
                    Formato.dinero(l.producto().precio()), l.cantidad(), Formato.dinero(l.subtotal())});
            piezas += l.cantidad();
        }
        Ui.ajustarAnchos(tabla);
        Totales t = carrito.totales(descuentoPct);
        lblSubtotal.setText(Formato.dinero(t.subtotal()));
        lblDescuento.setText("-" + Formato.dinero(t.descuento()));
        lblTotal.setText(Formato.dinero(t.total()));
        lblArticulos.setText(piezas + (piezas == 1 ? " artículo" : " artículos") + " en " + carrito.lineas().size()
                + (carrito.lineas().size() == 1 ? " renglón" : " renglones"));
        if (descuentoPct.signum() > 0) {
            lblDescuentoInfo.setText(descuentoPct.stripTrailingZeros().toPlainString() + " %"
                    + (autorizador != null ? " · autorizado por " + autorizador.nombre() : ""));
        } else {
            lblDescuentoInfo.setText(usuario.rol() == Rol.ADMIN ? "Sin límite de descuento (administrador)."
                    : "Hasta " + VentaService.LIMITE_DESCUENTO_CAJERO + " % sin autorización.");
        }
        boolean hay = !carrito.estaVacio();
        cartas.show(zonaTabla, hay ? "tabla" : "vacio");
        btnCobrar.setEnabled(hay);
        for (JButton b : new JButton[]{btnMenos, btnMas, btnCantidad, btnQuitar, btnVaciar}) {
            b.setEnabled(hay);
        }
    }
}
