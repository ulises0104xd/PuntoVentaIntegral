package com.puntoventa.service;

import com.puntoventa.dao.BitacoraDao;
import com.puntoventa.dao.UsuarioDao;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.Database;
import com.puntoventa.util.NegocioException;
import com.puntoventa.util.PasswordHasher;
import java.util.List;
import java.util.regex.Pattern;

/** CU07: gestión de usuarios (RN07 y RN10). */
public class UsuarioService {

    private static final Pattern NOMBRE_USUARIO = Pattern.compile("[A-Za-z0-9_.-]{3,50}");
    static final int LONGITUD_MINIMA_CONTRASENA = 6;

    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final BitacoraDao bitacoraDao = new BitacoraDao();

    public List<Usuario> listar(Usuario actor) {
        Permisos.requerirAdministrador(actor);
        return usuarioDao.listar();
    }

    public void crear(Usuario actor, String username, String nombre, char[] contrasena, Rol rol) {
        Permisos.requerirAdministrador(actor);
        validarNuevo(username, nombre, contrasena, rol);
        String hash = PasswordHasher.hash(contrasena);
        Database.inTransaction(c -> {
            usuarioDao.insertar(c, username.trim(), nombre.trim(), hash, rol);
            bitacoraDao.registrar(c, actor.id(), "USUARIO_CREADO", username.trim() + " (" + rol + ")");
            return null;
        });
    }

    /** RN10: nadie se desactiva a sí mismo y siempre debe quedar un administrador activo. */
    public void cambiarEstado(Usuario actor, int usuarioId, boolean activo) {
        Permisos.requerirAdministrador(actor);
        if (!activo && usuarioId == actor.id()) {
            throw new NegocioException("No puede desactivar su propio usuario");
        }
        Database.inTransaction(c -> {
            Usuario objetivo = usuarioDao.buscarPorIdParaActualizar(c, usuarioId)
                    .orElseThrow(() -> new NegocioException("El usuario ya no existe"));
            if (!activo && objetivo.rol() == Rol.ADMIN && objetivo.activo()
                    && usuarioDao.contarAdministradoresActivos(c) <= 1) {
                throw new NegocioException("Debe quedar al menos un administrador activo");
            }
            usuarioDao.cambiarEstado(c, usuarioId, activo);
            bitacoraDao.registrar(c, actor.id(), "USUARIO_ESTADO",
                    objetivo.username() + (activo ? " activado" : " desactivado"));
            return null;
        });
    }

    public void cambiarContrasena(Usuario actor, int usuarioId, char[] contrasena) {
        Permisos.requerirAdministrador(actor);
        validarContrasena(contrasena);
        String hash = PasswordHasher.hash(contrasena);
        Database.inTransaction(c -> {
            Usuario objetivo = usuarioDao.buscarPorIdParaActualizar(c, usuarioId)
                    .orElseThrow(() -> new NegocioException("El usuario ya no existe"));
            usuarioDao.actualizarPassword(c, usuarioId, hash);
            bitacoraDao.registrar(c, actor.id(), "USUARIO_PASSWORD", "Contraseña de " + objetivo.username());
            return null;
        });
    }

    static void validarNuevo(String username, String nombre, char[] contrasena, Rol rol) {
        if (username == null || !NOMBRE_USUARIO.matcher(username.trim()).matches()) {
            throw new NegocioException("El usuario debe tener de 3 a 50 caracteres: letras, números, punto, guion o guion bajo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre es obligatorio");
        }
        if (rol == null) {
            throw new NegocioException("Seleccione el rol");
        }
        validarContrasena(contrasena);
    }

    static void validarContrasena(char[] contrasena) {
        if (contrasena == null || contrasena.length < LONGITUD_MINIMA_CONTRASENA) {
            throw new NegocioException("La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres");
        }
    }
}
