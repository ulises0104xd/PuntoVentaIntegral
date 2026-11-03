package com.puntoventa.service;

import com.puntoventa.model.Rol;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.NegocioException;

/** Control de acceso en la capa de negocio: no depende de que la interfaz oculte los botones. */
final class Permisos {

    private Permisos() {
    }

    static void requerirAdministrador(Usuario actor) {
        if (actor == null || actor.rol() != Rol.ADMIN || !actor.activo()) {
            throw new NegocioException("Esta operación requiere permisos de administrador");
        }
    }
}
