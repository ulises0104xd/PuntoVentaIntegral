package com.puntoventa.ui;

import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/** Ventana principal: encabezado con el usuario, pestañas según el rol y barra de avisos. */
public class MainFrame extends JFrame implements Ui.Avisador {

    static {
        Tema.instalar();
    }

    private final JTabbedPane pestanas = new JTabbedPane();
    private final JLabel lblAviso = new JLabel(" ");
    private Timer temporizadorAviso;

    public MainFrame(Usuario usuario) {
        super("Punto de Venta Integral - " + usuario.nombre() + " (" + usuario.rol() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout());

        add(encabezado(usuario), BorderLayout.NORTH);

        pestanas.setName("pestanas");
        pestanas.putClientProperty("JTabbedPane.tabType", "underlined");
        pestanas.putClientProperty("JTabbedPane.showTabSeparators", false);
        pestanas.setFont(Tema.fuente(14f, java.awt.Font.BOLD));
        agregarPestana("Ventas", new VentaPanel(usuario));
        agregarPestana("Productos", new ProductosPanel(usuario));
        if (usuario.rol() == Rol.ADMIN) {
            agregarPestana("Inventario", new InventarioPanel(usuario));
            agregarPestana("Reportes", new ReportesPanel(usuario));
            agregarPestana("Usuarios", new UsuariosPanel(usuario));
        }
        pestanas.addChangeListener(e -> alCambiarPestana());
        add(pestanas, BorderLayout.CENTER);
        add(barraEstado(), BorderLayout.SOUTH);

        for (int i = 0; i < pestanas.getTabCount() && i < 9; i++) {
            final int indice = i;
            String clave = "pestana" + i;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, KeyEvent.CTRL_DOWN_MASK), clave);
            getRootPane().getActionMap().put(clave, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pestanas.setSelectedIndex(indice);
                }
            });
        }

        setMinimumSize(new Dimension(1100, 680));
        setSize(1280, 780);
        setLocationRelativeTo(null);
    }

    private void agregarPestana(String titulo, JComponent contenido) {
        JPanel marco = new JPanel(new BorderLayout());
        marco.setBackground(Tema.FONDO);
        marco.setBorder(new EmptyBorder(14, 18, 10, 18));
        marco.add(contenido, BorderLayout.CENTER);
        marco.setName("pestana_" + titulo);
        pestanas.addTab(titulo, marco);
    }

    private JPanel encabezado(Usuario usuario) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.SUPERFICIE);
        p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), new EmptyBorder(10, 20, 10, 20)));

        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        marca.setOpaque(false);
        JLabel logo = new JLabel("PV", JLabel.CENTER);
        logo.setForeground(Color.WHITE);
        logo.setFont(Tema.fuente(14f, java.awt.Font.BOLD));
        logo.setOpaque(true);
        logo.setBackground(Tema.PRIMARIO);
        logo.putClientProperty("FlatLaf.style", "arc:12");
        logo.setPreferredSize(new Dimension(38, 38));
        marca.add(logo);
        JLabel nombreApp = Ui.titulo("Punto de Venta Integral", 17f);
        nombreApp.setBorder(new EmptyBorder(0, 0, 0, 10));
        marca.add(nombreApp);
        p.add(marca, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        derecha.setOpaque(false);
        JLabel avatar = new JLabel(iniciales(usuario.nombre()), JLabel.CENTER);
        avatar.setName("avatar");
        avatar.setForeground(Tema.PRIMARIO);
        avatar.setFont(Tema.fuente(13f, java.awt.Font.BOLD));
        avatar.setOpaque(true);
        avatar.setBackground(Tema.SELECCION);
        avatar.putClientProperty("FlatLaf.style", "arc:999");
        avatar.setPreferredSize(new Dimension(36, 36));
        derecha.add(avatar);
        JLabel nombre = new JLabel(usuario.nombre());
        nombre.setName("lblUsuario");
        nombre.setFont(Tema.fuente(14f, java.awt.Font.BOLD));
        derecha.add(nombre);
        JLabel rol = usuario.rol() == Rol.ADMIN
                ? Ui.chip("Administrador", Tema.INFO, Tema.INFO_FONDO)
                : Ui.chip("Cajero", Tema.SUAVE, new Color(0xF2F4F7));
        rol.setName("lblRol");
        derecha.add(rol);
        javax.swing.JButton salir = Ui.boton("Cerrar sesión", Ui.Tipo.SECUNDARIO);
        salir.setName("btnCerrarSesion");
        salir.addActionListener(e -> cerrarSesion());
        derecha.add(salir);
        p.add(derecha, BorderLayout.EAST);
        return p;
    }

    private JPanel barraEstado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.SUPERFICIE);
        p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), new EmptyBorder(4, 20, 4, 20)));
        lblAviso.setName("lblAviso");
        lblAviso.setOpaque(true);
        lblAviso.setBorder(new EmptyBorder(5, 10, 5, 10));
        lblAviso.putClientProperty("FlatLaf.style", "arc:8");
        lblAviso.setBackground(Tema.SUPERFICIE);
        p.add(lblAviso, BorderLayout.WEST);
        JLabel ayuda = Ui.suave("Ctrl+1…" + pestanas.getTabCount() + " cambian de pestaña");
        ayuda.setFont(Tema.fuente(12f, java.awt.Font.PLAIN));
        p.add(ayuda, BorderLayout.EAST);
        return p;
    }

    private void alCambiarPestana() {
        Component marco = pestanas.getSelectedComponent();
        if (marco instanceof JPanel p && p.getComponentCount() > 0) {
            Component contenido = p.getComponent(0);
            if (contenido instanceof Refrescable r) {
                r.refrescar();
            }
            if (contenido instanceof VentaPanel v) {
                v.enfocarBusqueda();
            }
        }
    }

    /** Muestra un aviso breve (se oculta solo a los 5 segundos). */
    @Override
    public void avisar(String mensaje, Ui.Aviso tipo) {
        lblAviso.setText(mensaje);
        switch (tipo) {
            case EXITO -> {
                lblAviso.setForeground(Tema.EXITO);
                lblAviso.setBackground(Tema.EXITO_FONDO);
            }
            case ERROR -> {
                lblAviso.setForeground(Tema.PELIGRO);
                lblAviso.setBackground(Tema.PELIGRO_FONDO);
            }
            default -> {
                lblAviso.setForeground(Tema.INFO);
                lblAviso.setBackground(Tema.INFO_FONDO);
            }
        }
        if (temporizadorAviso != null) {
            temporizadorAviso.stop();
        }
        temporizadorAviso = new Timer(5000, e -> {
            lblAviso.setText(" ");
            lblAviso.setBackground(Tema.SUPERFICIE);
        });
        temporizadorAviso.setRepeats(false);
        temporizadorAviso.start();
    }

    /** Selecciona una pestaña por su título (útil para las pruebas y los atajos). */
    void irA(String titulo) {
        for (int i = 0; i < pestanas.getTabCount(); i++) {
            if (pestanas.getTitleAt(i).equals(titulo)) {
                pestanas.setSelectedIndex(i);
                return;
            }
        }
    }

    private void cerrarSesion() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    private static String iniciales(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        String a = partes.length > 0 && !partes[0].isEmpty() ? partes[0].substring(0, 1) : "?";
        String b = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }
}
