package com.puntoventa.model;

import com.puntoventa.util.Formato;
import java.math.BigDecimal;

public record Producto(int id, String codigo, String nombre, String categoria,
                       BigDecimal precio, int existencias, int stockMinimo, boolean activo) {

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + Formato.dinero(precio) + ", existencias: " + existencias + ")";
    }
}
