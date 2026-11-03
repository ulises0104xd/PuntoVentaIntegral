package com.puntoventa.util;

/** Se lanza cuando se incumple una regla de negocio; su mensaje se puede mostrar al usuario. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
