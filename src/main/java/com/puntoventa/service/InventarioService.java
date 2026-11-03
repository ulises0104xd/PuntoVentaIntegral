package com.puntoventa.service;

import com.puntoventa.dao.BitacoraDao;
import com.puntoventa.dao.InventarioDao;
import com.puntoventa.dao.ProductoDao;
import com.puntoventa.model.Producto;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.Database;
import com.puntoventa.util.NegocioException;

/** CU09 (entradas), CU10 (ajustes) y CU11 (historial): cada cambio de existencias deja un movimiento. */
public class InventarioService {

    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final BitacoraDao bitacoraDao = new BitacoraDao();

    public int registrarEntrada(Usuario actor, String codigo, int cantidad, String motivo) {
        Permisos.requerirAdministrador(actor);
        validarEntrada(cantidad);
        String detalle = motivo == null || motivo.isBlank() ? "Entrada de mercancía" : motivo.trim();
        return Database.inTransaction(c -> {
            Producto p = productoActivoBloqueado(c, codigo);
            int nuevas = p.existencias() + cantidad;
            productoDao.actualizarExistencias(c, p.id(), nuevas);
            inventarioDao.registrarMovimiento(c, p.id(), "ENTRADA", cantidad, nuevas, detalle, actor.id(), null);
            bitacoraDao.registrar(c, actor.id(), "ENTRADA", p.codigo() + " +" + cantidad + " (" + detalle + ")");
            return nuevas;
        });
    }

    /** RN08: todo ajuste exige motivo; la existencia resultante nunca es negativa (RN02). */
    public int registrarAjuste(Usuario actor, String codigo, int nuevaExistencia, String motivo) {
        Permisos.requerirAdministrador(actor);
        validarAjuste(nuevaExistencia, motivo);
        return Database.inTransaction(c -> {
            Producto p = productoActivoBloqueado(c, codigo);
            int diferencia = nuevaExistencia - p.existencias();
            if (diferencia == 0) {
                throw new NegocioException("La existencia indicada es igual a la actual");
            }
            productoDao.actualizarExistencias(c, p.id(), nuevaExistencia);
            inventarioDao.registrarMovimiento(c, p.id(), "AJUSTE", diferencia, nuevaExistencia, motivo.trim(),
                    actor.id(), null);
            bitacoraDao.registrar(c, actor.id(), "AJUSTE",
                    p.codigo() + " " + p.existencias() + " -> " + nuevaExistencia + " (" + motivo.trim() + ")");
            return diferencia;
        });
    }

    public TablaDatos historial(Usuario actor, String codigo) {
        Permisos.requerirAdministrador(actor);
        return inventarioDao.historial(codigo, 500);
    }

    private Producto productoActivoBloqueado(java.sql.Connection c, String codigo) throws java.sql.SQLException {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("Indique el código del producto");
        }
        Producto p = productoDao.buscarPorCodigoParaActualizar(c, codigo)
                .orElseThrow(() -> new NegocioException("No existe un producto con el código " + codigo.trim()));
        if (!p.activo()) {
            throw new NegocioException("El producto " + p.nombre() + " está desactivado");
        }
        return p;
    }

    static void validarEntrada(int cantidad) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad de la entrada debe ser mayor a cero");
        }
    }

    static void validarAjuste(int nuevaExistencia, String motivo) {
        if (nuevaExistencia < 0) {
            throw new NegocioException("La existencia no puede ser negativa");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException("El motivo del ajuste es obligatorio");
        }
    }
}
