package com.puntoventa.ui;

import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.service.ProductoService;
import com.puntoventa.util.Formato;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** CU02 (consulta, para todos) y CU08 (alta, edición y baja lógica, solo administrador). */
public class ProductosPanel extends JPanel implements Refrescable {

    static {
        Tema.instalar();
    }

    private final Usuario usuario;
    private final boolean admin;
    private final ProductoService productos = new ProductoService();
    private final JTextField txtBuscar = Ui.campo("txtBuscarProducto", "Buscar por código, nombre o categoría", 30);
    private final JCheckBox chkInactivos = new JCheckBox("Mostrar inactivos");
    private final DefaultTableModel modelo =
            Ui.modeloSoloLectura("Código", "Nombre", "Categoría", "Precio", "Existencias", "Mínimo", "Estado");
    private final JTable tabla = Ui.tabla(modelo);
    private final JLabel lblResumen = Ui.suave(" ");
    private final JLabel lblMensaje = new JLabel(" ");
    private List<Producto> mostrados = new ArrayList<>();
    private final Timer retardo;

    public ProductosPanel(Usuario usuario) {
        this.usuario = usuario;
        this.admin = usuario.rol() == Rol.ADMIN;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izquierda.setOpaque(false);
        txtBuscar.putClientProperty("FlatLaf.style", "margin:7,12,7,12");
        izquierda.add(txtBuscar);
        chkInactivos.setName("chkInactivos");
        chkInactivos.setOpaque(false);
        izquierda.add(chkInactivos);
        barra.add(izquierda, BorderLayout.WEST);

        if (admin) {
            JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            derecha.setOpaque(false);
            JButton btnEditar = Ui.boton("Editar", Ui.Tipo.SECUNDARIO);
            btnEditar.setName("btnEditarProducto");
            JButton btnEstado = Ui.boton("Activar / Desactivar", Ui.Tipo.SECUNDARIO);
            btnEstado.setName("btnEstadoProducto");
            JButton btnNuevo = Ui.boton("＋ Nuevo producto", Ui.Tipo.PRIMARIO);
            btnNuevo.setName("btnNuevoProducto");
            derecha.add(btnEstado);
            derecha.add(btnEditar);
            derecha.add(btnNuevo);
            barra.add(derecha, BorderLayout.EAST);
            btnNuevo.addActionListener(e -> nuevo());
            btnEditar.addActionListener(e -> editar());
            btnEstado.addActionListener(e -> cambiarEstado());
        }
        lblMensaje.setName("lblMensajeProductos");
        JPanel norte = Ui.columna();
        norte.add(barra);
        add(norte, BorderLayout.NORTH);

        tabla.setName("tablaProductos");
        tabla.setDefaultRenderer(Object.class, new Ui.CeldaRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
                if (fila < mostrados.size()) {
                    Producto p = mostrados.get(fila);
                    if (!p.activo()) {
                        c.setForeground(Tema.SUAVE);
                    } else if (col == 4 && p.existencias() <= p.stockMinimo()) {
                        c.setForeground(p.existencias() == 0 ? Tema.PELIGRO : Tema.AVISO);
                        c.setFont(c.getFont().deriveFont(java.awt.Font.BOLD));
                    }
                }
                return c;
            }
        });
        add(Ui.desplazable(tabla), BorderLayout.CENTER);
        lblResumen.setName("lblResumenProductos");
        add(lblResumen, BorderLayout.SOUTH);
        lblResumen.setBorder(new EmptyBorder(2, 2, 0, 0));

        retardo = new Timer(250, e -> refrescar());
        retardo.setRepeats(false);
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                retardo.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                retardo.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                retardo.restart();
            }
        });
        chkInactivos.addActionListener(e -> refrescar());
        if (admin) {
            tabla.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                        editar();
                    }
                }
            });
        }
    }

    @Override
    public void refrescar() {
        Ui.ejecutar(this, () -> {
            mostrados = productos.listar(txtBuscar.getText(), chkInactivos.isSelected());
            modelo.setRowCount(0);
            int bajos = 0;
            for (Producto p : mostrados) {
                modelo.addRow(new Object[]{p.codigo(), p.nombre(), p.categoria(), Formato.dinero(p.precio()),
                        p.existencias(), p.stockMinimo(), p.activo() ? "Activo" : "Inactivo"});
                if (p.activo() && p.existencias() <= p.stockMinimo()) {
                    bajos++;
                }
            }
            Ui.ajustarAnchos(tabla);
            lblResumen.setText(mostrados.size() + (mostrados.size() == 1 ? " producto" : " productos")
                    + (bajos > 0 ? "  ·  " + bajos + " con existencias bajas (en amarillo)" : "")
                    + (admin ? "  ·  Doble clic para editar" : ""));
        });
    }

    private Producto seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Ui.aviso(this, "Seleccione un producto de la tabla.", Ui.Aviso.ERROR);
            lblResumen.setText("Seleccione un producto de la tabla.");
            return null;
        }
        return mostrados.get(fila);
    }

    private List<String> categorias() {
        TreeSet<String> set = new TreeSet<>();
        set.add("General");
        productos.listar("", true).forEach(p -> set.add(p.categoria()));
        return new ArrayList<>(set);
    }

    private void nuevo() {
        boolean ok = new FormularioDialog(Ui.ventana(this), "Nuevo producto", "Guardar producto")
                .texto("codigo", "Código", "", "Código de barras o clave")
                .texto("nombre", "Nombre", "", "Nombre del producto")
                .combo("categoria", "Categoría", categorias(), "General", true)
                .texto("precio", "Precio de venta", "", "0.00")
                .entero("existencias", "Existencia inicial", 0, 0, 1_000_000)
                .entero("minimo", "Stock mínimo (para avisar cuando se esté acabando)", 5, 0, 1_000_000)
                .mostrar(v -> productos.crear(usuario, v.texto("codigo"), v.texto("nombre"), v.texto("categoria"),
                        Ui.decimal(v.texto("precio"), "El precio"), v.entero("existencias"), v.entero("minimo")));
        if (ok) {
            Ui.aviso(this, "Producto guardado.", Ui.Aviso.EXITO);
            refrescar();
        }
    }

    private void editar() {
        Producto p = seleccionado();
        if (p == null) {
            return;
        }
        boolean ok = new FormularioDialog(Ui.ventana(this), "Editar producto", "Guardar cambios")
                .nota("Las existencias no se editan aquí: use «Inventario» para registrar entradas o ajustes.")
                .texto("codigo", "Código", p.codigo(), "Código")
                .texto("nombre", "Nombre", p.nombre(), "Nombre")
                .combo("categoria", "Categoría", categorias(), p.categoria(), true)
                .texto("precio", "Precio de venta", p.precio().setScale(2).toPlainString(), "0.00")
                .entero("minimo", "Stock mínimo", p.stockMinimo(), 0, 1_000_000)
                .mostrar(v -> productos.editar(usuario, p.id(), v.texto("codigo"), v.texto("nombre"), v.texto("categoria"),
                        Ui.decimal(v.texto("precio"), "El precio"), v.entero("minimo")));
        if (ok) {
            Ui.aviso(this, "Cambios guardados.", Ui.Aviso.EXITO);
            refrescar();
        }
    }

    private void cambiarEstado() {
        Producto p = seleccionado();
        if (p == null) {
            return;
        }
        if (p.activo() && !Ui.confirmar(this, "Desactivar producto",
                "«" + p.nombre() + "» dejará de aparecer en Ventas, pero conservará su historial.", "Desactivar")) {
            return;
        }
        Ui.ejecutar(this, () -> {
            productos.cambiarEstado(usuario, p.id(), !p.activo());
            Ui.aviso(this, p.activo() ? "Producto desactivado." : "Producto activado.", Ui.Aviso.EXITO);
            refrescar();
        });
    }
}
