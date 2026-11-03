package com.puntoventa.dao;

import com.puntoventa.model.Agrupacion;
import com.puntoventa.model.TablaDatos;
import java.sql.Timestamp;
import java.time.LocalDate;

/** Consultas de los reportes (CU12 y CU13): agregaciones hechas en SQL. */
public class ReporteDao {

    private static Timestamp inicio(LocalDate fecha) {
        return Timestamp.valueOf(fecha.atStartOfDay());
    }

    /** Límite superior exclusivo: el día siguiente a las 00:00. */
    private static Timestamp fin(LocalDate fecha) {
        return Timestamp.valueOf(fecha.plusDays(1).atStartOfDay());
    }

    public TablaDatos ventasPorPeriodo(Agrupacion agrupacion, LocalDate desde, LocalDate hasta) {
        return Consultas.consultar(
                "SELECT " + agrupacion.expresionSql() + " AS `Periodo`, COUNT(*) AS `Ventas`, "
                        + "SUM(subtotal) AS `Subtotal`, SUM(descuento_monto) AS `Descuentos`, SUM(total) AS `Ingresos` "
                        + "FROM ventas WHERE fecha >= ? AND fecha < ? GROUP BY 1 ORDER BY 1",
                inicio(desde), fin(hasta));
    }

    public TablaDatos masVendidos(LocalDate desde, LocalDate hasta, int limite) {
        return Consultas.consultar(
                "SELECT p.codigo AS `Código`, p.nombre AS `Producto`, SUM(d.cantidad) AS `Unidades`, "
                        + "SUM(d.subtotal) AS `Importe` "
                        + "FROM detalle_venta d JOIN ventas v ON v.id = d.venta_id "
                        + "JOIN productos p ON p.id = d.producto_id "
                        + "WHERE v.fecha >= ? AND v.fecha < ? GROUP BY p.id, p.codigo, p.nombre "
                        + "ORDER BY 3 DESC, 2 LIMIT " + limite,
                inicio(desde), fin(hasta));
    }

    /** Ingresos por método de pago más una fila con el total del periodo. */
    public TablaDatos ingresos(LocalDate desde, LocalDate hasta) {
        return Consultas.consultar(
                "SELECT metodo_pago AS `Método de pago`, COUNT(*) AS `Ventas`, SUM(total) AS `Ingresos` "
                        + "FROM ventas WHERE fecha >= ? AND fecha < ? GROUP BY metodo_pago "
                        + "UNION ALL "
                        + "SELECT 'TOTAL', COUNT(*), COALESCE(SUM(total), 0) FROM ventas WHERE fecha >= ? AND fecha < ?",
                inicio(desde), fin(hasta), inicio(desde), fin(hasta));
    }

    public TablaDatos existencias() {
        return Consultas.consultar(
                "SELECT codigo AS `Código`, nombre AS `Producto`, categoria AS `Categoría`, precio AS `Precio`, "
                        + "existencias AS `Existencias`, stock_minimo AS `Mínimo`, "
                        + "CASE WHEN existencias <= stock_minimo THEN 'BAJO' ELSE 'OK' END AS `Estado` "
                        + "FROM productos WHERE activo = TRUE ORDER BY nombre");
    }

    /** Operaciones de la bitácora; con usuarioId 0 se incluyen todos los usuarios. */
    public TablaDatos operacionesPorUsuario(int usuarioId, LocalDate desde, LocalDate hasta) {
        return Consultas.consultar(
                "SELECT b.fecha AS `Fecha`, u.username AS `Usuario`, b.accion AS `Acción`, b.detalle AS `Detalle` "
                        + "FROM bitacora b JOIN usuarios u ON u.id = b.usuario_id "
                        + "WHERE b.fecha >= ? AND b.fecha < ? AND (? = 0 OR b.usuario_id = ?) "
                        + "ORDER BY b.fecha DESC, b.id DESC LIMIT 1000",
                inicio(desde), fin(hasta), usuarioId, usuarioId);
    }
}
