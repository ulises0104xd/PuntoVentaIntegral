package com.puntoventa.ui;

import com.puntoventa.util.DataAccessException;
import com.puntoventa.util.NegocioException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/**
 * Cuadro de formulario modal: etiquetas sobre cada campo, error visible en el propio cuadro,
 * Enter acepta y Esc cancela. Los errores de negocio no cierran el cuadro: se muestran y se corrige.
 */
final class FormularioDialog extends JDialog {

    /** Acción que se ejecuta al aceptar; si lanza una excepción de negocio el cuadro sigue abierto. */
    @FunctionalInterface
    interface Aceptar {
        void aplicar(Valores valores);
    }

    /** Acceso a lo capturado en el formulario. */
    static final class Valores {
        private final Map<String, JComponent> campos;

        private Valores(Map<String, JComponent> campos) {
            this.campos = campos;
        }

        String texto(String nombre) {
            JComponent c = campos.get(nombre);
            if (c instanceof JComboBox<?> combo) {
                Object sel = combo.getEditor().getItem();
                return sel == null ? "" : sel.toString().trim();
            }
            return ((JTextField) c).getText().trim();
        }

        char[] clave(String nombre) {
            return ((JPasswordField) campos.get(nombre)).getPassword();
        }

        int entero(String nombre) {
            JSpinner s = (JSpinner) campos.get(nombre);
            try {
                s.commitEdit();
            } catch (java.text.ParseException e) {
                throw new NegocioException("Escriba un número entero válido");
            }
            return ((Number) s.getValue()).intValue();
        }

        Object seleccion(String nombre) {
            return ((JComboBox<?>) campos.get(nombre)).getSelectedItem();
        }
    }

    private final Map<String, JComponent> campos = new LinkedHashMap<>();
    private final JPanel cuerpo = Ui.columna();
    private final JLabel lblError = new JLabel();
    private final JButton btnAceptar;
    private final JButton btnCancelar = Ui.boton("Cancelar", Ui.Tipo.SECUNDARIO);
    private boolean aceptado;
    private JComponent primero;

    FormularioDialog(Window padre, String titulo, String textoAceptar) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        btnAceptar = Ui.boton(textoAceptar, Ui.Tipo.PRIMARIO);
        btnAceptar.setName("btnAceptar");
        btnCancelar.setName("btnCancelar");
        lblError.setName("lblError");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // ------------------------------------------------------------------ constructores de campos
    FormularioDialog texto(String nombre, String etiqueta, String valor, String placeholder) {
        JTextField c = Ui.campo("campo_" + nombre, placeholder, 24);
        c.setText(valor == null ? "" : valor);
        return agregar(nombre, etiqueta, c);
    }

    FormularioDialog clave(String nombre, String etiqueta, String placeholder) {
        return agregar(nombre, etiqueta, Ui.clave("campo_" + nombre, placeholder, 24));
    }

    FormularioDialog entero(String nombre, String etiqueta, int valor, int minimo, int maximo) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(valor, minimo, maximo, 1));
        s.setName("campo_" + nombre);
        return agregar(nombre, etiqueta, s);
    }

    FormularioDialog combo(String nombre, String etiqueta, List<String> opciones, String seleccion, boolean editable) {
        JComboBox<String> c = new JComboBox<>(opciones.toArray(new String[0]));
        c.setEditable(editable);
        c.setName("campo_" + nombre);
        c.setSelectedItem(seleccion);
        return agregar(nombre, etiqueta, c);
    }

    /** Fila libre (por ejemplo, una etiqueta que se actualiza mientras se escribe). */
    FormularioDialog componente(String etiqueta, JComponent componente) {
        cuerpo.add(Ui.filaCampo(etiqueta, componente));
        return this;
    }

    FormularioDialog nota(String texto) {
        JLabel l = new JLabel("<html><div style='width:340px'>" + texto + "</div></html>");
        l.setForeground(Tema.SUAVE);
        l.setBorder(new EmptyBorder(0, 0, 10, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        cuerpo.add(l);
        return this;
    }

    JTextField campoTexto(String nombre) {
        return (JTextField) campos.get(nombre);
    }

    private FormularioDialog agregar(String nombre, String etiqueta, JComponent c) {
        campos.put(nombre, c);
        if (primero == null) {
            primero = c;
        }
        cuerpo.add(Ui.filaCampo(etiqueta, c));
        return this;
    }

    // ------------------------------------------------------------------ mostrar
    /** Muestra el cuadro; devuelve true si se aceptó y la acción se aplicó sin errores. */
    boolean mostrar(Aceptar aceptar) {
        lblError.setVisible(false);
        lblError.setOpaque(true);
        lblError.setForeground(Tema.PELIGRO);
        lblError.setBackground(Tema.PELIGRO_FONDO);
        lblError.setBorder(new EmptyBorder(8, 10, 8, 10));
        lblError.putClientProperty("FlatLaf.style", "arc:8");

        JPanel contenido = new JPanel(new BorderLayout(0, 6));
        contenido.setBorder(new EmptyBorder(20, 24, 16, 24));
        contenido.setBackground(Tema.SUPERFICIE);
        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(cuerpo, BorderLayout.NORTH);
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setOpaque(false);
        errorPanel.add(lblError, BorderLayout.CENTER);
        centro.add(errorPanel, BorderLayout.CENTER);
        contenido.add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnAceptar);
        contenido.add(botones, BorderLayout.SOUTH);
        setContentPane(contenido);

        Valores valores = new Valores(campos);
        btnAceptar.addActionListener(e -> {
            try {
                aceptar.aplicar(valores);
                aceptado = true;
                dispose();
            } catch (NegocioException ex) {
                error(ex.getMessage());
            } catch (DataAccessException ex) {
                error("Ocurrió un problema con la base de datos: " + ex.getMessage());
            }
        });
        btnCancelar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnAceptar);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        getRootPane().getActionMap().put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setMinimumSize(new Dimension(440, 0));
        pack();
        setLocationRelativeTo(getOwner());
        if (primero != null) {
            javax.swing.SwingUtilities.invokeLater(primero::requestFocusInWindow);
        }
        setVisible(true);
        return aceptado;
    }

    private void error(String mensaje) {
        lblError.setText("<html><div style='width:340px'>" + mensaje + "</div></html>");
        lblError.setVisible(true);
        pack();
    }
}
