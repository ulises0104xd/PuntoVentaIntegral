package com.puntoventa.ui;

import com.puntoventa.model.MetodoPago;
import com.puntoventa.util.Formato;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Cobro: método de pago, monto recibido con botones rápidos y cambio calculado en vivo (CU05). */
final class CobroDialog extends JDialog {

    record Pago(MetodoPago metodo, BigDecimal recibido) {
    }

    private static final int[] BILLETES = {20, 50, 100, 200, 500, 1000};

    private final BigDecimal total;
    private final JToggleButton tbEfectivo = new JToggleButton("Efectivo", true);
    private final JToggleButton tbTarjeta = new JToggleButton("Tarjeta");
    private final JTextField txtRecibido;
    private final JLabel lblCambio = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel rapidos = new JPanel(new GridLayout(1, 0, 6, 0));
    private final JButton btnConfirmar = Ui.boton("Confirmar cobro", Ui.Tipo.PRIMARIO);
    private Pago resultado;

    private CobroDialog(Window padre, BigDecimal total) {
        super(padre, "Cobrar", ModalityType.APPLICATION_MODAL);
        this.total = total;
        txtRecibido = Ui.campo("txtRecibido", "Monto recibido", 12);
        txtRecibido.setText(total.setScale(2).toPlainString());
        construir();
    }

    static Optional<Pago> pedir(Window padre, BigDecimal total) {
        CobroDialog d = new CobroDialog(padre, total);
        d.setVisible(true);
        return Optional.ofNullable(d.resultado);
    }

    private void construir() {
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBorder(new EmptyBorder(22, 26, 18, 26));
        raiz.setBackground(Tema.SUPERFICIE);

        JPanel cab = Ui.columna();
        JLabel et = Ui.suave("Total a cobrar");
        et.setAlignmentX(LEFT_ALIGNMENT);
        cab.add(et);
        JLabel lblTotal = Ui.titulo(Formato.dinero(total), 34f);
        lblTotal.setName("lblTotalCobro");
        lblTotal.setAlignmentX(LEFT_ALIGNMENT);
        cab.add(lblTotal);
        raiz.add(cab, BorderLayout.NORTH);

        JPanel centro = Ui.columna();
        ButtonGroup grupo = new ButtonGroup();
        tbEfectivo.setName("tbEfectivo");
        tbTarjeta.setName("tbTarjeta");
        for (JToggleButton b : new JToggleButton[]{tbEfectivo, tbTarjeta}) {
            grupo.add(b);
            b.putClientProperty("FlatLaf.style",
                    "arc:10; margin:8,18,8,18; selectedBackground:#2F6FED; selectedForeground:#FFFFFF");
            b.setFocusable(true);
        }
        JPanel metodo = new JPanel(new GridLayout(1, 2, 8, 0));
        metodo.setOpaque(false);
        metodo.add(tbEfectivo);
        metodo.add(tbTarjeta);
        centro.add(Ui.filaCampo("Método de pago", metodo));

        centro.add(Ui.filaCampo("Monto recibido", txtRecibido));
        rapidos.setOpaque(false);
        rapidos.setBorder(new EmptyBorder(0, 0, 0, 0));
        JButton exacto = Ui.boton("Exacto", Ui.Tipo.SECUNDARIO);
        exacto.setName("btnExacto");
        exacto.addActionListener(e -> fijarMonto(total));
        rapidos.add(exacto);
        int agregados = 0;
        for (int b : BILLETES) {
            if (agregados < 3 && BigDecimal.valueOf(b).compareTo(total) >= 0) {
                JButton boton = Ui.boton("$" + b, Ui.Tipo.SECUNDARIO);
                boton.setName("btnBillete" + b);
                boton.addActionListener(e -> fijarMonto(BigDecimal.valueOf(b)));
                rapidos.add(boton);
                agregados++;
            }
        }
        JPanel filaRapidos = new JPanel(new BorderLayout());
        filaRapidos.setOpaque(false);
        filaRapidos.add(rapidos, BorderLayout.CENTER);
        filaRapidos.setBorder(new EmptyBorder(0, 0, 12, 0));
        filaRapidos.setAlignmentX(LEFT_ALIGNMENT);
        centro.add(filaRapidos);

        lblCambio.setName("lblCambio");
        lblCambio.setOpaque(true);
        lblCambio.setFont(Tema.fuente(18f, java.awt.Font.BOLD));
        lblCambio.setBorder(new EmptyBorder(12, 10, 12, 10));
        lblCambio.putClientProperty("FlatLaf.style", "arc:10");
        JPanel envolturaCambio = new JPanel(new BorderLayout());
        envolturaCambio.setOpaque(false);
        envolturaCambio.add(lblCambio, BorderLayout.CENTER);
        envolturaCambio.setAlignmentX(LEFT_ALIGNMENT);
        centro.add(envolturaCambio);
        raiz.add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton cancelar = Ui.boton("Cancelar", Ui.Tipo.SECUNDARIO);
        cancelar.setName("btnCancelar");
        btnConfirmar.setName("btnConfirmar");
        botones.add(cancelar);
        botones.add(btnConfirmar);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);

        txtRecibido.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizar();
            }
        });
        tbEfectivo.addActionListener(e -> actualizar());
        tbTarjeta.addActionListener(e -> actualizar());
        cancelar.addActionListener(e -> dispose());
        btnConfirmar.addActionListener(e -> confirmar());
        getRootPane().setDefaultButton(btnConfirmar);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        getRootPane().getActionMap().put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        actualizar();
        setMinimumSize(new Dimension(460, 0));
        pack();
        setLocationRelativeTo(getOwner());
        javax.swing.SwingUtilities.invokeLater(() -> {
            txtRecibido.requestFocusInWindow();
            txtRecibido.selectAll();
        });
    }

    private void fijarMonto(BigDecimal monto) {
        tbEfectivo.setSelected(true);
        txtRecibido.setText(monto.setScale(2).toPlainString());
        actualizar();
    }

    private BigDecimal recibido() {
        try {
            return new BigDecimal(txtRecibido.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void actualizar() {
        boolean efectivo = tbEfectivo.isSelected();
        txtRecibido.setEnabled(efectivo);
        rapidos.setVisible(efectivo);
        if (!efectivo) {
            mostrar("Cobre en la terminal y confirme", Tema.INFO, Tema.INFO_FONDO, true);
            return;
        }
        BigDecimal r = recibido();
        if (r == null) {
            mostrar("Escriba el monto recibido", Tema.SUAVE, new java.awt.Color(0xF2F4F7), false);
        } else if (r.compareTo(total) >= 0) {
            mostrar("Cambio: " + Formato.dinero(r.subtract(total)), Tema.EXITO, Tema.EXITO_FONDO, true);
        } else {
            mostrar("Faltan " + Formato.dinero(total.subtract(r)), Tema.PELIGRO, Tema.PELIGRO_FONDO, false);
        }
    }

    private void mostrar(String texto, java.awt.Color letra, java.awt.Color fondo, boolean permitir) {
        lblCambio.setText(texto);
        lblCambio.setForeground(letra);
        lblCambio.setBackground(fondo);
        btnConfirmar.setEnabled(permitir);
    }

    private void confirmar() {
        if (tbTarjeta.isSelected()) {
            resultado = new Pago(MetodoPago.TARJETA, total);
        } else {
            BigDecimal r = recibido();
            if (r == null || r.compareTo(total) < 0) {
                return;
            }
            resultado = new Pago(MetodoPago.EFECTIVO, r);
        }
        dispose();
    }
}
