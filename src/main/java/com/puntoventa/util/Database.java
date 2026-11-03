package com.puntoventa.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Acceso a la conexión JDBC y manejo de transacciones (commit / rollback). */
public final class Database {

    private static final Properties PROPS = cargar();

    private Database() {
    }

    private static Properties cargar() {
        Properties p = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }

    private static String valor(String clave, String variableEntorno, String porDefecto) {
        String env = System.getenv(variableEntorno);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return PROPS.getProperty(clave, porDefecto);
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    valor("db.url", "DB_URL", "jdbc:mysql://localhost:3306/punto_venta"),
                    valor("db.user", "DB_USER", "root"),
                    valor("db.password", "DB_PASSWORD", ""));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Ejecuta el trabajo en una sola transacción. Si el trabajo lanza cualquier excepción se
     * revierten todos los cambios y la excepción se vuelve a lanzar.
     */
    public static <T> T inTransaction(SqlWork<T> trabajo) {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                T resultado = trabajo.apply(c);
                c.commit();
                return resultado;
            } catch (SQLException e) {
                c.rollback();
                throw new DataAccessException("Error de base de datos: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error de base de datos: " + e.getMessage(), e);
        }
    }
}
