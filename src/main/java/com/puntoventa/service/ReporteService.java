package com.puntoventa.service;

import com.puntoventa.dao.ReporteDao;
import com.puntoventa.model.Agrupacion;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.util.NegocioException;
import java.time.LocalDate;

/** CU12 y CU13: reportes de ventas, productos más vendidos, ingresos, existencias e historial por usuario. */
public class ReporteService {

    private final ReporteDao reporteDao = new ReporteDao();

    public TablaDatos ventasPorPeriodo(Usuario actor, Agrupacion agrupacion, LocalDate desde, LocalDate hasta) {
        Permisos.requerirAdministrador(actor);
        validarRango(desde, hasta);
        return reporteDao.ventasPorPeriodo(agrupacion, desde, hasta);
    }

    public TablaDatos masVendidos(Usuario actor, LocalDate desde, LocalDate hasta) {
        Permisos.requerirAdministrador(actor);
        validarRango(desde, hasta);
        return reporteDao.masVendidos(desde, hasta, 20);
    }

    public TablaDatos ingresos(Usuario actor, LocalDate desde, LocalDate hasta) {
        Permisos.requerirAdministrador(actor);
        validarRango(desde, hasta);
        return reporteDao.ingresos(desde, hasta);
    }

    public TablaDatos existencias(Usuario actor) {
        Permisos.requerirAdministrador(actor);
        return reporteDao.existencias();
    }

    /** Con usuarioId 0 se incluyen las operaciones de todos los usuarios. */
    public TablaDatos operacionesPorUsuario(Usuario actor, int usuarioId, LocalDate desde, LocalDate hasta) {
        Permisos.requerirAdministrador(actor);
        validarRango(desde, hasta);
        return reporteDao.operacionesPorUsuario(usuarioId, desde, hasta);
    }

    static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new NegocioException("Indique la fecha inicial y la final");
        }
        if (desde.isAfter(hasta)) {
            throw new NegocioException("La fecha inicial no puede ser posterior a la final");
        }
    }
}
