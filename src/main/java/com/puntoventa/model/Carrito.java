package com.puntoventa.model;

import com.puntoventa.util.NegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Venta en curso: renglones, cantidades y cálculo de subtotal, descuento y total. */
public class Carrito {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final List<LineaVenta> lineas = new ArrayList<>();

    public List<LineaVenta> lineas() {
        return Collections.unmodifiableList(lineas);
    }

    public boolean estaVacio() {
        return lineas.isEmpty();
    }

    public void agregar(Producto producto, int cantidad) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a cero");
        }
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).producto().id() == producto.id()) {
                int nueva = lineas.get(i).cantidad() + cantidad;
                validarExistencias(producto, nueva);
                lineas.set(i, new LineaVenta(producto, nueva));
                return;
            }
        }
        validarExistencias(producto, cantidad);
        lineas.add(new LineaVenta(producto, cantidad));
    }

    public void cambiarCantidad(int indice, int cantidad) {
        if (cantidad <= 0) {
            quitar(indice);
            return;
        }
        Producto producto = lineas.get(indice).producto();
        validarExistencias(producto, cantidad);
        lineas.set(indice, new LineaVenta(producto, cantidad));
    }

    public void quitar(int indice) {
        lineas.remove(indice);
    }

    public void vaciar() {
        lineas.clear();
    }

    public BigDecimal subtotal() {
        return lineas.stream().map(LineaVenta::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public Totales totales(BigDecimal descuentoPct) {
        if (descuentoPct == null || descuentoPct.signum() < 0 || descuentoPct.compareTo(CIEN) > 0) {
            throw new NegocioException("El descuento debe estar entre 0 y 100 %");
        }
        BigDecimal subtotal = subtotal();
        BigDecimal descuento = subtotal.multiply(descuentoPct).divide(CIEN, 2, RoundingMode.HALF_UP);
        return new Totales(subtotal, descuentoPct, descuento, subtotal.subtract(descuento));
    }

    private static void validarExistencias(Producto producto, int cantidad) {
        if (!producto.activo()) {
            throw new NegocioException("El producto " + producto.nombre() + " está desactivado");
        }
        if (cantidad > producto.existencias()) {
            throw new NegocioException("Existencias insuficientes de " + producto.nombre()
                    + " (disponibles: " + producto.existencias() + ")");
        }
    }
}
