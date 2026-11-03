package com.puntoventa.dao;

import com.puntoventa.model.TablaDatos;
import com.puntoventa.util.Database;
import com.puntoventa.util.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Ejecuta una consulta de solo lectura y devuelve el resultado como tabla genérica. */
final class Consultas {

    private Consultas() {
    }

    static TablaDatos consultar(String sql, Object... parametros) {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<String> columnas = new ArrayList<>();
                for (int i = 1; i <= n; i++) {
                    columnas.add(md.getColumnLabel(i));
                }
                List<Object[]> filas = new ArrayList<>();
                while (rs.next()) {
                    Object[] fila = new Object[n];
                    for (int i = 0; i < n; i++) {
                        fila[i] = rs.getObject(i + 1);
                    }
                    filas.add(fila);
                }
                return new TablaDatos(columnas, filas);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar la base de datos", e);
        }
    }
}
