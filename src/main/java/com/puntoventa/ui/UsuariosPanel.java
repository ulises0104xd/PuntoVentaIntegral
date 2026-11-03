package com.puntoventa.ui;

import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.service.UsuarioService;
import com.puntoventa.util.NegocioException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** CU07: gestión de usuarios (solo administrador). */
public class UsuariosPanel extends JPanel implements Refrescable {

    static {
        Tema.instalar();
    }

    private final Usuario actor;
    private final UsuarioService usuarios = new UsuarioService();
    private final DefaultTableModel modelo = Ui.modeloSoloLectura("Usuario", "Nombre", "Rol", "Estado");
    private final JTable tabla = Ui.tabla(modelo);
    private final JLabel lblResumen = Ui.suave(" ");
    private List<Usuario> mostrados = new ArrayList<>();

    public UsuariosPanel(Usuario actor) {
        this.actor = actor;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        JButton btnNuevo = Ui.boton("＋ Nuevo usuario", Ui.Tipo.PRIMARIO);
        btnNuevo.setName("btnNuevoUsuario");
        JButton btnClave = Ui.boton("Cambiar contraseña", Ui.Tipo.SECUNDARIO);
        btnClave.setName("btnCambiarClave");
        JButton btnEstado = Ui.boton("Activar / Desactivar", Ui.Tipo.SECUNDARIO);
        btnEstado.setName("btnEstadoUsuario");
        acciones.add(btnNuevo);
        acciones.add(btnClave);
        acciones.add(btnEstado);
        add(acciones, BorderLayout.NORTH);

        tabla.setName("tablaUsuarios");
        add(Ui.desplazable(tabla), BorderLayout.CENTER);
        lblResumen.setName("lblResumenUsuarios");
        lblResumen.setBorder(new EmptyBorder(2, 2, 0, 0));
        add(lblResumen, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> nuevo());
        btnEstado.addActionListener(e -> cambiarEstado());
        btnClave.addActionListener(e -> cambiarContrasena());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    cambiarContrasena();
                }
            }
        });
    }

    @Override
    public void refrescar() {
        Ui.ejecutar(this, () -> {
            mostrados = usuarios.listar(actor);
            modelo.setRowCount(0);
            for (Usuario u : mostrados) {
                modelo.addRow(new Object[]{u.username(), u.nombre(), u.rol(), u.activo() ? "Activo" : "Inactivo"});
            }
            Ui.ajustarAnchos(tabla);
            long activos = mostrados.stream().filter(Usuario::activo).count();
            lblResumen.setText(mostrados.size() + " usuarios  ·  " + activos + " activos  ·  Doble clic para cambiar la contraseña");
        });
    }

    private Usuario seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Ui.aviso(this, "Seleccione un usuario de la tabla.", Ui.Aviso.ERROR);
            lblResumen.setText("Seleccione un usuario de la tabla.");
            return null;
        }
        return mostrados.get(fila);
    }

    private static void borrar(char[]... claves) {
        for (char[] c : claves) {
            Arrays.fill(c, '\0');
        }
    }

    private static void validarCoincidencia(char[] a, char[] b) {
        if (!Arrays.equals(a, b)) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
    }

    private void nuevo() {
        boolean ok = new FormularioDialog(Ui.ventana(this), "Nuevo usuario", "Crear usuario")
                .texto("usuario", "Usuario (para iniciar sesión)", "", "Ej. maria.lopez")
                .texto("nombre", "Nombre completo", "", "Nombre y apellido")
                .clave("clave", "Contraseña (mínimo 6 caracteres)", "Contraseña")
                .clave("repetir", "Repetir contraseña", "Repita la contraseña")
                .combo("rol", "Rol", List.of("CAJERO", "ADMIN"), "CAJERO", false)
                .nota("<b>Cajero:</b> vende y consulta productos.<br><b>ADMIN:</b> además administra productos, inventario, reportes y usuarios.")
                .mostrar(v -> {
                    char[] clave = v.clave("clave");
                    char[] repetir = v.clave("repetir");
                    try {
                        validarCoincidencia(clave, repetir);
                        usuarios.crear(actor, v.texto("usuario"), v.texto("nombre"), clave, Rol.valueOf((String) v.seleccion("rol")));
                    } finally {
                        borrar(clave, repetir);
                    }
                });
        if (ok) {
            Ui.aviso(this, "Usuario creado.", Ui.Aviso.EXITO);
            refrescar();
        }
    }

    private void cambiarEstado() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        if (u.activo() && !Ui.confirmar(this, "Desactivar usuario",
                "«" + u.username() + "» ya no podrá iniciar sesión. Su historial se conserva.", "Desactivar")) {
            return;
        }
        Ui.ejecutar(this, () -> {
            usuarios.cambiarEstado(actor, u.id(), !u.activo());
            Ui.aviso(this, u.activo() ? "Usuario desactivado." : "Usuario activado.", Ui.Aviso.EXITO);
            refrescar();
        });
    }

    private void cambiarContrasena() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        boolean ok = new FormularioDialog(Ui.ventana(this), "Cambiar contraseña", "Guardar contraseña")
                .nota("Nueva contraseña para <b>" + u.username() + "</b> (" + u.nombre() + ").")
                .clave("clave", "Nueva contraseña (mínimo 6 caracteres)", "Nueva contraseña")
                .clave("repetir", "Repetir contraseña", "Repita la contraseña")
                .mostrar(v -> {
                    char[] clave = v.clave("clave");
                    char[] repetir = v.clave("repetir");
                    try {
                        validarCoincidencia(clave, repetir);
                        usuarios.cambiarContrasena(actor, u.id(), clave);
                    } finally {
                        borrar(clave, repetir);
                    }
                });
        if (ok) {
            Ui.aviso(this, "Contraseña actualizada.", Ui.Aviso.EXITO);
        }
    }
}
