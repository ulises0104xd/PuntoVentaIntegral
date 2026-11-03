package com.puntoventa.model;

import java.math.BigDecimal;

public record LineaVenta(Producto producto, int cantidad) {

    public BigDecimal subtotal() {
        return producto.precio().multiply(BigDecimal.valueOf(cantidad));
    }
}
