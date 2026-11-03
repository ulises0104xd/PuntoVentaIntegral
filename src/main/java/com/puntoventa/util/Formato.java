package com.puntoventa.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formato {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    public static String dinero(BigDecimal valor) {
        return String.format(Locale.US, "$%,.2f", valor);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return FECHA_HORA.format(fecha);
    }
}
