package com.puntoventa.service;

import com.puntoventa.dao.BitacoraDao;
import com.puntoventa.dao.UsuarioDao;
import com.puntoventa.dao.UsuarioDao.Credencial;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.Database;
import com.puntoventa.util.PasswordHasher;
import java.util.Optional;

/** CU01: autenticación, roles y alta de los usuarios iniciales. */
public class AuthService {

    /** Hash de relleno para que verificar un usuario inexistente tarde lo mismo que uno existente. */
    private static final String HASH_RELLENO = PasswordHasher.hash("relleno".toCharArray());

    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final BitacoraDao bitacoraDao = new BitacoraDao();

    /** Devuelve el usuario si las credenciales son correctas y está activo; no revela cuál dato falló. */
    public Optional<Usuario> login(String username, char[] contrasena) {
        Optional<Usuario> usuario = verificar(username, contrasena);
        usuario.ifPresent(u -> registrar(u, "LOGIN", "Inicio de sesión de " + u.username()));
        return usuario;
    }

    /** Valida las credenciales de un administrador que autoriza un descuento (RN03). */
    public Optional<Usuario> autorizarAdministrador(String username, char[] contrasena) {
        Optional<Usuario> admin = verificar(username, contrasena).filter(u -> u.rol() == Rol.ADMIN);
        admin.ifPresent(u -> registrar(u, "AUTORIZACION", "Autorizó un descuento"));
        return admin;
    }

    private Optional<Usuario> verificar(String username, char[] contrasena) {
        if (username == null || username.isBlank() || contrasena == null) {
            return Optional.empty();
        }
        Optional<Credencial> credencial = usuarioDao.buscarPorUsername(username.trim());
        boolean correcta = PasswordHasher.verificar(contrasena,
                credencial.map(Credencial::hash).orElse(HASH_RELLENO));
        return credencial.filter(c -> correcta && c.usuario().activo()).map(Credencial::usuario);
    }

    private void registrar(Usuario u, String accion, String detalle) {
        Database.inTransaction(c -> {
            bitacoraDao.registrar(c, u.id(), accion, detalle);
            return null;
        });
    }

    /**
     * Crea los usuarios de ejemplo {@code admin} y {@code cajero} si no existen. Son credenciales del
     * prototipo (ver README); deben cambiarse antes de usar el sistema en producción.
     */
    public void sembrarUsuariosIniciales() {
        sembrar("admin", "Administrador", "admin123", Rol.ADMIN);
        sembrar("cajero", "Cajero de prueba", "cajero123", Rol.CAJERO);
    }

    private void sembrar(String username, String nombre, String contrasena, Rol rol) {
        if (usuarioDao.buscarPorUsername(username).isPresent()) {
            return;
        }
        Database.inTransaction(c ->
                usuarioDao.insertar(c, username, nombre, PasswordHasher.hash(contrasena.toCharArray()), rol));
    }
}
