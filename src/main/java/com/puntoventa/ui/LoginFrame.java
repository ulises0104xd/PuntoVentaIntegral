package com.puntoventa.ui;

import com.puntoventa.model.Usuario;
import com.puntoventa.service.AuthService;
import com.puntoventa.util.DataAccessException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** CU01: pantalla de inicio de sesión (error visible en la propia pantalla, sin ventanas emergentes). */
public class LoginFrame extends JFrame {

    static {
        Tema.instalar();
    }

    private final AuthService auth = new AuthService();
    private final JTextField txtUsuario = Ui.campo("txtUsuario", "Su nombre de usuario", 20);
    private final JPasswordField txtContrasena = Ui.clave("txtContrasena", "Su contraseña", 20);
    private final JLabel lblError = new JLabel();
    private final javax.swing.JButton btnEntrar = Ui.boton("Iniciar sesión", Ui.Tipo.PRIMARIO);

    public LoginFrame() {
        super("Punto de Venta Integral");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(Tema.FONDO);

        Ui.Tarjeta tarjeta = new Ui.Tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 0));
        tarjeta.setBorder(new EmptyBorder(32, 36, 28, 36));
        tarjeta.setPreferredSize(new Dimension(420, 510));

        JPanel marca = Ui.columna();
        JLabel logo = new JLabel("PV", JLabel.CENTER);
        logo.setForeground(java.awt.Color.WHITE);
        logo.setFont(Tema.fuente(20f, java.awt.Font.BOLD));
        logo.setOpaque(true);
        logo.setBackground(Tema.PRIMARIO);
        logo.putClientProperty("FlatLaf.style", "arc:16");
        logo.setPreferredSize(new Dimension(56, 56));
        logo.setMaximumSize(new Dimension(56, 56));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        marca.add(logo);
        marca.add(javax.swing.Box.createVerticalStrut(16));
        JLabel titulo = Ui.titulo("Punto de Venta Integral", 22f);
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        marca.add(titulo);
        JLabel sub = Ui.suave("Inicie sesión para continuar");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        marca.add(sub);
        marca.add(javax.swing.Box.createVerticalStrut(22));
        tarjeta.add(marca, BorderLayout.NORTH);

        JPanel formulario = Ui.columna();
        formulario.add(Ui.filaCampo("Usuario", txtUsuario));
        formulario.add(Ui.filaCampo("Contraseña", txtContrasena));
        lblError.setName("lblError");
        lblError.setForeground(Tema.PELIGRO);
        lblError.setBackground(Tema.PELIGRO_FONDO);
        lblError.setOpaque(true);
        lblError.setBorder(new EmptyBorder(8, 10, 8, 10));
        lblError.putClientProperty("FlatLaf.style", "arc:8");
        lblError.setVisible(false);
        JPanel filaError = new JPanel(new BorderLayout());
        filaError.setOpaque(false);
        filaError.add(lblError, BorderLayout.CENTER);
        filaError.setBorder(new EmptyBorder(0, 0, 10, 0));
        filaError.setAlignmentX(LEFT_ALIGNMENT);
        formulario.add(filaError);
        tarjeta.add(formulario, BorderLayout.CENTER);

        btnEntrar.setName("btnEntrar");
        btnEntrar.setFont(Tema.fuente(15f, java.awt.Font.BOLD));
        btnEntrar.putClientProperty("FlatLaf.style",
                "background:#2F6FED; foreground:#FFFFFF; hoverBackground:#2459C9; borderWidth:0; arc:10; margin:10,16,10,16");
        JPanel sur = new JPanel(new BorderLayout(0, 12));
        sur.setOpaque(false);
        sur.add(btnEntrar, BorderLayout.NORTH);
        JLabel version = Ui.suave("Versión beta 0.9");
        version.setHorizontalAlignment(JLabel.CENTER);
        version.setFont(Tema.fuente(12f, java.awt.Font.PLAIN));
        sur.add(version, BorderLayout.SOUTH);
        tarjeta.add(sur, BorderLayout.SOUTH);

        fondo.add(tarjeta, new GridBagConstraints());
        setContentPane(fondo);

        btnEntrar.addActionListener(e -> entrar());
        txtContrasena.addActionListener(e -> entrar());
        txtUsuario.addActionListener(e -> txtContrasena.requestFocusInWindow());
        getRootPane().setDefaultButton(btnEntrar);

        setSize(560, 680);
        setMinimumSize(new Dimension(520, 640));
        setLocationRelativeTo(null);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            txtUsuario.requestFocusInWindow();
        }
    }

    private void entrar() {
        if (txtUsuario.getText().isBlank() || txtContrasena.getPassword().length == 0) {
            mostrarError("Escriba su usuario y su contraseña.");
            (txtUsuario.getText().isBlank() ? txtUsuario : txtContrasena).requestFocusInWindow();
            return;
        }
        char[] contrasena = txtContrasena.getPassword();
        try {
            Optional<Usuario> usuario = auth.login(txtUsuario.getText(), contrasena);
            if (usuario.isEmpty()) {
                mostrarError("Usuario o contraseña incorrectos.");
                txtContrasena.setText("");
                txtContrasena.requestFocusInWindow();
                return;
            }
            dispose();
            new MainFrame(usuario.get()).setVisible(true);
        } catch (DataAccessException e) {
            mostrarError("No se pudo conectar con la base de datos. Verifique que MySQL esté encendido.");
        } finally {
            Arrays.fill(contrasena, '\0');
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        txtUsuario.putClientProperty("JComponent.outline", mensaje.startsWith("Usuario o") ? "error" : null);
        txtContrasena.putClientProperty("JComponent.outline", "error");
        revalidate();
        repaint();
    }
}
