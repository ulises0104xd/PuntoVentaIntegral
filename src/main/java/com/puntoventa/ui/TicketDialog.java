package com.puntoventa.ui;

import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Venta;
import com.puntoventa.util.Formato;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/** Confirmación de la venta con el ticket; permite imprimirlo o guardarlo (CU06). */
final class TicketDialog extends JDialog {

    private final String texto;
    private final Venta venta;
    private final JTextArea area;

    TicketDialog(Window padre, Venta venta, String texto) {
        super(padre, "Venta registrada", ModalityType.APPLICATION_MODAL);
        this.venta = venta;
        this.texto = texto;
        this.area = new JTextArea(texto);
        construir();
    }

    private void construir() {
        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBorder(new EmptyBorder(20, 24, 16, 24));
        raiz.setBackground(Tema.SUPERFICIE);

        JPanel cab = Ui.columna();
        JLabel ok = Ui.titulo("✓  Venta #" + String.format("%06d", venta.id()) + " registrada", 18f);
        ok.setName("lblVentaRegistrada");
        ok.setForeground(Tema.EXITO);
        ok.setAlignmentX(LEFT_ALIGNMENT);
        cab.add(ok);
        String detalle = "Total " + Formato.dinero(venta.totales().total());
        if (venta.metodo() == MetodoPago.EFECTIVO) {
            detalle += "  ·  Cambio " + Formato.dinero(venta.cambio());
        } else {
            detalle += "  ·  Pago con tarjeta";
        }
        JLabel det = new JLabel(detalle);
        det.setName("lblDetalleVenta");
        det.setFont(Tema.fuente(16f, Font.BOLD));
        det.setAlignmentX(LEFT_ALIGNMENT);
        cab.add(det);
        raiz.add(cab, BorderLayout.NORTH);

        area.setName("areaTicket");
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new java.awt.Color(0xFFFEF8));
        area.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane sp = Ui.desplazable(area);
        sp.setPreferredSize(new Dimension(470, 430));
        raiz.add(sp, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton imprimir = Ui.boton("Imprimir", Ui.Tipo.SECUNDARIO);
        imprimir.setName("btnImprimir");
        JButton guardar = Ui.boton("Guardar como…", Ui.Tipo.SECUNDARIO);
        guardar.setName("btnGuardarTicket");
        JButton cerrar = Ui.boton("Cerrar", Ui.Tipo.PRIMARIO);
        cerrar.setName("btnCerrarTicket");
        botones.add(imprimir);
        botones.add(guardar);
        botones.add(cerrar);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);

        imprimir.addActionListener(e -> imprimir());
        guardar.addActionListener(e -> guardar());
        cerrar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(cerrar);
        javax.swing.SwingUtilities.invokeLater(cerrar::requestFocusInWindow);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void imprimir() {
        try {
            area.print();
        } catch (PrinterException e) {
            Ui.error(this, "No se pudo imprimir el ticket: " + e.getMessage());
        }
    }

    private void guardar() {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File(String.format("ticket-%06d.txt", venta.id())));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.writeString(selector.getSelectedFile().toPath(), texto, StandardCharsets.UTF_8);
            Ui.aviso(this, "Ticket guardado en " + selector.getSelectedFile().getName(), Ui.Aviso.EXITO);
        } catch (IOException e) {
            Ui.error(this, "No se pudo guardar el ticket: " + e.getMessage());
        }
    }
}
