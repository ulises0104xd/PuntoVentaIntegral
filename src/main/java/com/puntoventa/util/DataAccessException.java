package com.puntoventa.util;

/** Envuelve los errores de JDBC para que las capas superiores no dependan de SQLException. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
