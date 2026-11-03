package com.puntoventa.model;

import java.math.BigDecimal;

public record Totales(BigDecimal subtotal, BigDecimal descuentoPct, BigDecimal descuento, BigDecimal total) {
}
