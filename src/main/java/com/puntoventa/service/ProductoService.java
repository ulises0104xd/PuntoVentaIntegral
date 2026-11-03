package com.puntoventa.service;

import com.puntoventa.dao.BitacoraDao;
import com.puntoventa.dao.InventarioDao;
import com.puntoventa.dao.ProductoDao;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.Database;
import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** CU02 (consulta) y CU08 (alta, edición y baja lógica de productos). */
public class ProductoService {

    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final BitacoraDao bitacoraDao = new BitacoraDao();

    public Optional<Producto> buscarPorCodigo(String codigo) {
        return productoDao.buscarActivoPorCodigo(codigo);
    }

    /** Productos activos (para la venta). */
    public List<Producto> buscar(String texto) {
        return productoDao.buscarActivos(texto);
    }

    public List<Producto> listar(String texto, boolean incluirInactivos) {
        return productoDao.buscar(texto, incluirInactivos);
    }

    public List<Producto> bajoStock() {
        return productoDao.bajoStock();
    }

    public Producto crear(Usuario actor, String codigo, String nombre, String categoria, BigDecimal precio,
                          int existencias, int stockMinimo) {
        Permisos.requerirAdministrador(actor);
        validar(codigo, nombre, precio, existencias, stockMinimo);
        Producto nuevo = new Producto(0, codigo.trim(), nombre.trim(), categoriaONormal(categoria),
                precio.setScale(2, RoundingMode.HALF_UP), existencias, stockMinimo, true);
        return Database.inTransaction(c -> {
            int id = productoDao.insertar(c, nuevo);
            if (existencias > 0) {
                inventarioDao.registrarMovimiento(c, id, "ENTRADA", existencias, existencias,
                        "Existencia inicial", actor.id(), null);
            }
            bitacoraDao.registrar(c, actor.id(), "PRODUCTO_CREADO", nuevo.codigo() + " - " + nuevo.nombre());
            return new Producto(id, nuevo.codigo(), nuevo.nombre(), nuevo.categoria(), nuevo.precio(),
                    nuevo.existencias(), nuevo.stockMinimo(), true);
        });
    }

    /** Edita los datos del producto; las existencias solo cambian con entradas, ajustes o ventas. */
    public void editar(Usuario actor, int id, String codigo, String nombre, String categoria, BigDecimal precio,
                       int stockMinimo) {
        Permisos.requerirAdministrador(actor);
        validar(codigo, nombre, precio, 0, stockMinimo);
        Database.inTransaction(c -> {
            Producto actual = productoDao.buscarPorIdParaActualizar(c, id)
                    .orElseThrow(() -> new NegocioException("El producto ya no existe"));
            Producto editado = new Producto(id, codigo.trim(), nombre.trim(), categoriaONormal(categoria),
                    precio.setScale(2, RoundingMode.HALF_UP), actual.existencias(), stockMinimo, actual.activo());
            productoDao.actualizar(c, editado);
            bitacoraDao.registrar(c, actor.id(), "PRODUCTO_EDITADO", editado.codigo() + " - " + editado.nombre());
            return null;
        });
    }

    /** RN05: los productos no se borran; se desactivan para conservar el historial. */
    public void cambiarEstado(Usuario actor, int id, boolean activo) {
        Permisos.requerirAdministrador(actor);
        Database.inTransaction(c -> {
            Producto actual = productoDao.buscarPorIdParaActualizar(c, id)
                    .orElseThrow(() -> new NegocioException("El producto ya no existe"));
            productoDao.cambiarEstado(c, id, activo);
            bitacoraDao.registrar(c, actor.id(), "PRODUCTO_ESTADO",
                    actual.codigo() + (activo ? " activado" : " desactivado"));
            return null;
        });
    }

    private static String categoriaONormal(String categoria) {
        return categoria == null || categoria.isBlank() ? "General" : categoria.trim();
    }

    static void validar(String codigo, String nombre, BigDecimal precio, int existencias, int stockMinimo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El código es obligatorio");
        }
        if (codigo.trim().length() > 30) {
            throw new NegocioException("El código no puede tener más de 30 caracteres");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre es obligatorio");
        }
        if (nombre.trim().length() > 120) {
            throw new NegocioException("El nombre no puede tener más de 120 caracteres");
        }
        if (precio == null || precio.signum() < 0) {
            throw new NegocioException("El precio no puede ser negativo");
        }
        if (precio.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new NegocioException("El precio es demasiado grande");
        }
        if (existencias < 0) {
            throw new NegocioException("Las existencias no pueden ser negativas");
        }
        if (stockMinimo < 0) {
            throw new NegocioException("El stock mínimo no puede ser negativo");
        }
    }
}
