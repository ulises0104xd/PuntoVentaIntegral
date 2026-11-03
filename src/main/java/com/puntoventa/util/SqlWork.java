package com.puntoventa.util;

import java.sql.Connection;
import java.sql.SQLException;

/** Unidad de trabajo que se ejecuta dentro de una transacción. */
@FunctionalInterface
public interface SqlWork<T> {
    T apply(Connection conexion) throws SQLException;
}
