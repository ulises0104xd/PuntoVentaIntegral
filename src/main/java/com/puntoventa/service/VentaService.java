package com.puntoventa.service;

import com.puntoventa.dao.BitacoraDao;
import com.puntoventa.dao.InventarioDao;
import com.puntoventa.dao.ProductoDao;
import com.puntoventa.dao.VentaDao;
import com.puntoventa.model.Carrito;
import com.puntoventa.model.LineaVenta;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Producto;
import com.puntoventa.model.Rol;
import com.puntoventa.model.Totales;
import com.puntoventa.model.Usuario;
import com.puntoventa.model.Venta;
import com.puntoventa.util.Database;
import com.puntoventa.util.Formato;
import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** CU03, CU04 y CU05: cobro de una venta como una sola transacción (RN03, RN04, RN06). */
public class VentaService {

    public static final BigDecimal LIMITE_DESCUENTO_CAJERO = BigDecimal.TEN;

    private final VentaDao ventaDao = new VentaDao();
    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final BitacoraDao bitacoraDao = new BitacoraDao();

    /**
     * Guarda la venta, descuenta el inventario, registra los movimientos y la bitácora. Si algo falla
     * se revierte todo.
     *
     * @param autorizador administrador que autorizó el descuento (puede ser nulo)
     */
    public Venta cobrar(Carrito carrito, BigDecimal descuentoPct, Usuario cajero, Usuario autorizador,
                        MetodoPago metodo, BigDecimal recibido) {
        if (carrito.estaVacio()) {
            throw new NegocioException("La venta no tiene productos");
        }
        Integer autorizadoPor = validarDescuento(descuentoPct, cajero, autorizador);
        Totales totales = carrito.totales(descuentoPct);
        BigDecimal[] pago = calcularPago(totales.total(), metodo, recibido);
        LocalDateTime fecha = LocalDateTime.now();
        // Se bloquean las filas siempre en el mismo orden (por id) para evitar interbloqueos (R12).
        List<LineaVenta> lineas = carrito.lineas().stream()
                .sorted(Comparator.comparingInt(l -> l.producto().id())).toList();

        return Database.inTransaction(c -> {
            int ventaId = ventaDao.insertar(c, fecha, cajero.id(), totales, metodo, pago[0], pago[1], autorizadoPor);
            for (LineaVenta linea : lineas) {
                Producto actual = productoDao.buscarPorIdParaActualizar(c, linea.producto().id())
                        .orElseThrow(() -> new NegocioException(
                                "El producto " + linea.producto().nombre() + " ya no existe"));
                if (!actual.activo()) {
                    throw new NegocioException("El producto " + actual.nombre() + " está desactivado");
                }
                if (actual.precio().compareTo(linea.producto().precio()) != 0) {
                    throw new NegocioException("El precio de " + actual.nombre()
                            + " cambió; vuelva a agregarlo a la venta");
                }
                if (actual.existencias() < linea.cantidad()) {
                    throw new NegocioException("Existencias insuficientes de " + actual.nombre()
                            + " (disponibles: " + actual.existencias() + ")");
                }
                int nuevas = actual.existencias() - linea.cantidad();
                productoDao.actualizarExistencias(c, actual.id(), nuevas);
                ventaDao.insertarDetalle(c, ventaId, linea);
                inventarioDao.registrarMovimiento(c, actual.id(), "VENTA", -linea.cantidad(), nuevas,
                        "Venta #" + ventaId, cajero.id(), ventaId);
            }
            bitacoraDao.registrar(c, cajero.id(), "VENTA",
                    "Venta #" + ventaId + " por " + Formato.dinero(totales.total()));
            return new Venta(ventaId, fecha, cajero.nombre(), lineas, totales, metodo, pago[0], pago[1]);
        });
    }

    /** RN03: hasta 10 % lo aplica el cajero; más requiere un administrador. Devuelve quién autorizó. */
    static Integer validarDescuento(BigDecimal descuentoPct, Usuario cajero, Usuario autorizador) {
        if (descuentoPct == null || descuentoPct.signum() <= 0) {
            return null;
        }
        if (cajero.rol() == Rol.ADMIN || descuentoPct.compareTo(LIMITE_DESCUENTO_CAJERO) <= 0) {
            return null;
        }
        if (autorizador == null || autorizador.rol() != Rol.ADMIN) {
            throw new NegocioException("Un descuento mayor a " + LIMITE_DESCUENTO_CAJERO
                    + " % requiere la autorización de un administrador");
        }
        return autorizador.id();
    }

    /** RN04: devuelve {monto recibido, cambio}. */
    static BigDecimal[] calcularPago(BigDecimal total, MetodoPago metodo, BigDecimal recibido) {
        if (metodo == MetodoPago.TARJETA) {
            return new BigDecimal[]{total, BigDecimal.ZERO.setScale(2)};
        }
        if (recibido == null || recibido.compareTo(total) < 0) {
            throw new NegocioException("El monto recibido es menor al total");
        }
        return new BigDecimal[]{recibido.setScale(2, RoundingMode.HALF_UP),
                recibido.subtract(total).setScale(2, RoundingMode.HALF_UP)};
    }
}
