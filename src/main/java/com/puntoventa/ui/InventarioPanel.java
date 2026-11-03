package com.puntoventa.ui;

import com.puntoventa.model.Producto;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.service.InventarioService;
import com.puntoventa.service.ProductoService;
import com.puntoventa.util.Formato;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** CU09 (entradas), CU10 (ajustes) y CU11 (existencias bajas e historial de movimientos). */
public class InventarioPanel extends JPanel implements Refrescable {

    static {
        Tema.instalar();
    }

    private final Usuario usuario;
    private final InventarioService inventario = new InventarioService();
    private final ProductoService productos = new ProductoService();
    private final JToggleButton tbBajo = new JToggleButton("Existencias bajas", true);
    private final JToggleButton tbHistorial = new JToggleButton("Historial de movimientos");
    private final JTextField txtCodigo = Ui.campo("txtFiltroCodigo", "Filtrar por código", 16);
    private final JButton btnBuscar = Ui.boton("Buscar", Ui.Tipo.SECUNDARIO);
    private final JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final DefaultTableModel modelo = Ui.modeloSoloLectura();
    private final javax.swing.JTable tabla = Ui.tabla(modelo);
    private final JLabel lblResumen = Ui.suave(" ");

    public InventarioPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        JPanel vistas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        vistas.setOpaque(false);
        ButtonGroup grupo = new ButtonGroup();
        tbBajo.setName("tbBajoStock");
        tbHistorial.setName("tbHistorial");
        for (JToggleButton b : new JToggleButton[]{tbBajo, tbHistorial}) {
            grupo.add(b);
            b.putClientProperty("FlatLaf.style",
                    "arc:10; margin:7,16,7,16; selectedBackground:#2F6FED; selectedForeground:#FFFFFF");
            vistas.add(b);
        }
        barra.add(vistas, BorderLayout.WEST);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        JButton btnAjuste = Ui.boton("Registrar ajuste", Ui.Tipo.SECUNDARIO);
        btnAjuste.setName("btnAjuste");
        JButton btnEntrada = Ui.boton("＋ Registrar entrada", Ui.Tipo.PRIMARIO);
        btnEntrada.setName("btnEntrada");
        acciones.add(btnAjuste);
        acciones.add(btnEntrada);
        barra.add(acciones, BorderLayout.EAST);

        filtro.setOpaque(false);
        filtro.setBorder(new EmptyBorder(8, 0, 0, 0));
        txtCodigo.putClientProperty("FlatLaf.style", "margin:6,10,6,10");
        btnBuscar.setName("btnBuscarHistorial");
        filtro.add(txtCodigo);
        filtro.add(btnBuscar);
        filtro.setVisible(false);
        JPanel norte = Ui.columna();
        norte.add(barra);
        JPanel filaFiltro = new JPanel(new BorderLayout());
        filaFiltro.setOpaque(false);
        filaFiltro.add(filtro, BorderLayout.WEST);
        norte.add(filaFiltro);
        add(norte, BorderLayout.NORTH);

        tabla.setName("tablaInventario");
        add(Ui.desplazable(tabla), BorderLayout.CENTER);
        lblResumen.setName("lblResumenInventario");
        lblResumen.setBorder(new EmptyBorder(2, 2, 0, 0));
        add(lblResumen, BorderLayout.SOUTH);

        btnEntrada.addActionListener(e -> entrada());
        btnAjuste.addActionListener(e -> ajuste());
        tbBajo.addActionListener(e -> cambiarVista());
        tbHistorial.addActionListener(e -> cambiarVista());
        btnBuscar.addActionListener(e -> refrescar());
        txtCodigo.addActionListener(e -> refrescar());
    }

    private void cambiarVista() {
        filtro.setVisible(tbHistorial.isSelected());
        refrescar();
    }

    @Override
    public void refrescar() {
        Ui.ejecutar(this, () -> {
            if (tbBajo.isSelected()) {
                List<Object[]> filas = new ArrayList<>();
                for (Producto p : productos.bajoStock()) {
                    filas.add(new Object[]{p.codigo(), p.nombre(), p.categoria(), Formato.dinero(p.precio()),
                            p.existencias(), p.stockMinimo(), p.existencias() == 0 ? "AGOTADO" : "BAJO"});
                }
                Ui.cargar(modelo, new TablaDatos(
                        List.of("Código", "Producto", "Categoría", "Precio", "Existencias", "Mínimo", "Estado"), filas));
                Ui.ajustarAnchos(tabla);
                lblResumen.setText(filas.isEmpty()
                        ? "Todo en orden: ningún producto está en o bajo su existencia mínima."
                        : filas.size() + (filas.size() == 1 ? " producto necesita" : " productos necesitan")
                        + " reposición (existencias menores o iguales al mínimo).");
            } else {
                TablaDatos datos = inventario.historial(usuario, txtCodigo.getText());
                Ui.cargar(modelo, formatear(datos));
                Ui.ajustarAnchos(tabla);
                lblResumen.setText(datos.filas().size() + (datos.filas().size() == 1 ? " movimiento" : " movimientos")
                        + (txtCodigo.getText().isBlank() ? " (los más recientes primero, hasta 500)"
                        : " del producto " + txtCodigo.getText().trim()));
            }
        });
    }

    /** Muestra la cantidad con signo explícito (+ o −) para leer el historial de un vistazo. */
    private static TablaDatos formatear(TablaDatos datos) {
        int indice = datos.columnas().indexOf("Cantidad");
        List<Object[]> filas = new ArrayList<>();
        for (Object[] f : datos.filas()) {
            Object[] n = f.clone();
            if (indice >= 0 && n[indice] instanceof Number num) {
                int v = num.intValue();
                n[indice] = (v > 0 ? "+" : "") + v;
            }
            filas.add(n);
        }
        return new TablaDatos(datos.columnas(), filas);
    }

    private JLabel etiquetaProducto() {
        JLabel l = new JLabel(" ");
        l.setName("lblProductoInfo");
        l.setForeground(Tema.SUAVE);
        return l;
    }

    private void vincularInfo(FormularioDialog dlg, JLabel info) {
        JTextField campo = dlg.campoTexto("codigo");
        Runnable actualizar = () -> {
            String codigo = campo.getText().trim();
            if (codigo.isEmpty()) {
                info.setText(" ");
                return;
            }
            Optional<Producto> p = productos.buscarPorCodigo(codigo);
            if (p.isPresent()) {
                info.setText(p.get().nombre() + "  ·  existencias actuales: " + p.get().existencias());
                info.setForeground(Tema.EXITO);
            } else {
                info.setText("No hay un producto activo con ese código");
                info.setForeground(Tema.AVISO);
            }
        };
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizar.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizar.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizar.run();
            }
        });
        actualizar.run();
    }

    private String codigoInicial() {
        return tbHistorial.isSelected() ? txtCodigo.getText().trim() : "";
    }

    private void entrada() {
        JLabel info = etiquetaProducto();
        FormularioDialog dlg = new FormularioDialog(Ui.ventana(this), "Registrar entrada de mercancía", "Registrar entrada")
                .texto("codigo", "Código del producto", codigoInicial(), "Escanee o escriba el código")
                .componente("", info)
                .entero("cantidad", "Cantidad recibida", 1, 1, 1_000_000)
                .texto("motivo", "Motivo (opcional)", "", "Compra de mercancía");
        vincularInfo(dlg, info);
        int[] resultado = new int[1];
        boolean ok = dlg.mostrar(v -> resultado[0] =
                inventario.registrarEntrada(usuario, v.texto("codigo"), v.entero("cantidad"), v.texto("motivo")));
        if (ok) {
            Ui.aviso(this, "Entrada registrada. Existencias actuales: " + resultado[0] + ".", Ui.Aviso.EXITO);
            refrescar();
        }
    }

    private void ajuste() {
        JLabel info = etiquetaProducto();
        FormularioDialog dlg = new FormularioDialog(Ui.ventana(this), "Registrar ajuste de inventario", "Registrar ajuste")
                .nota("Escriba la existencia <b>real</b> que contó. El sistema calcula la diferencia y la registra.")
                .texto("codigo", "Código del producto", codigoInicial(), "Escanee o escriba el código")
                .componente("", info)
                .entero("real", "Existencia real", 0, 0, 1_000_000)
                .texto("motivo", "Motivo (obligatorio)", "", "Ej. merma, caducidad, error de conteo");
        vincularInfo(dlg, info);
        int[] resultado = new int[1];
        boolean ok = dlg.mostrar(v -> resultado[0] =
                inventario.registrarAjuste(usuario, v.texto("codigo"), v.entero("real"), v.texto("motivo")));
        if (ok) {
            Ui.aviso(this, "Ajuste registrado (diferencia: " + (resultado[0] > 0 ? "+" : "") + resultado[0] + ").", Ui.Aviso.EXITO);
            refrescar();
        }
    }
}
