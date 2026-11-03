package com.puntoventa.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Venta ya registrada; con ella se genera el ticket. */
public record Venta(int id, LocalDateTime fecha, String cajero, List<LineaVenta> lineas, Totales totales,
                    MetodoPago metodo, BigDecimal recibido, BigDecimal cambio) {
}
