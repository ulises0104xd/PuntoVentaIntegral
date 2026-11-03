package com.puntoventa.ui;

import com.puntoventa.model.Producto;
import com.puntoventa.util.Formato;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Lista de coincidencias cuando una búsqueda por nombre devuelve varios productos. */
final class SelectorProductoDialog extends JDialog {

    private final JList<Producto> lista;
    private Producto elegido;

    private SelectorProductoDialog(Window padre, List<Producto> productos, String texto) {
        super(padre, "Elija el producto", ModalityType.APPLICATION_MODAL);
        DefaultListModel<Producto> modelo = new DefaultListModel<>();
        productos.forEach(modelo::addElement);
        lista = new JList<>(modelo);
        lista.setName("listaProductos");
        lista.setSelectedIndex(0);
        lista.setFixedCellHeight(52);
        lista.setCellRenderer((l, p, i, sel, foco) -> {
            JPanel fila = new JPanel(new BorderLayout());
            fila.setBorder(new EmptyBorder(6, 12, 6, 12));
            fila.setBackground(sel ? Tema.SELECCION : Tema.SUPERFICIE);
            JLabel nombre = new JLabel(p.nombre());
            nombre.setFont(Tema.fuente(14f, java.awt.Font.BOLD));
            JLabel detalle = Ui.suave(p.codigo() + "  ·  " + Formato.dinero(p.precio()) + "  ·  existencias: " + p.existencias());
            detalle.setFont(Tema.fuente(12.5f, java.awt.Font.PLAIN));
            fila.add(nombre, BorderLayout.NORTH);
            fila.add(detalle, BorderLayout.SOUTH);
            return fila;
        });

        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBorder(new EmptyBorder(18, 20, 14, 20));
        raiz.setBackground(Tema.SUPERFICIE);
        JPanel cab = Ui.columna();
        cab.add(Ui.titulo("Elija el producto", 17f));
        cab.add(Ui.suave("Varios productos coinciden con «" + texto + "». Elija uno con Enter o doble clic."));
        raiz.add(cab, BorderLayout.NORTH);
        raiz.add(Ui.desplazable(lista), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton cancelar = Ui.boton("Cancelar", Ui.Tipo.SECUNDARIO);
        cancelar.setName("btnCancelar");
        JButton agregar = Ui.boton("Agregar a la venta", Ui.Tipo.PRIMARIO);
        agregar.setName("btnAgregarSeleccion");
        botones.add(cancelar);
        botones.add(agregar);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);

        agregar.addActionListener(e -> aceptar());
        cancelar.addActionListener(e -> dispose());
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    aceptar();
                }
            }
        });
        getRootPane().setDefaultButton(agregar);
        getRootPane().registerKeyboardAction(e -> dispose(), javax.swing.KeyStroke.getKeyStroke("ESCAPE"),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        setPreferredSize(new Dimension(540, Math.min(520, 190 + 52 * productos.size())));
        pack();
        setLocationRelativeTo(padre);
        javax.swing.SwingUtilities.invokeLater(lista::requestFocusInWindow);
    }

    private void aceptar() {
        elegido = lista.getSelectedValue();
        if (elegido != null) {
            dispose();
        }
    }

    static Optional<Producto> elegir(Component padre, List<Producto> productos, String texto) {
        Window w = padre instanceof Window v ? v : javax.swing.SwingUtilities.getWindowAncestor(padre);
        SelectorProductoDialog d = new SelectorProductoDialog(w, productos, texto);
        d.setVisible(true);
        return Optional.ofNullable(d.elegido);
    }
}
