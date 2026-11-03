package com.puntoventa.service;

import com.puntoventa.model.TablaDatos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exporta una tabla a CSV (UTF-8 con BOM para que Excel respete los acentos). */
public final class CsvExporter {

    private CsvExporter() {
    }

    public static String aTexto(TablaDatos tabla) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", tabla.columnas().stream().map(CsvExporter::escapar).toList())).append("\r\n");
        for (Object[] fila : tabla.filas()) {
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escapar(fila[i] == null ? "" : fila[i].toString()));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public static void guardar(TablaDatos tabla, Path destino) {
        try {
            Files.writeString(destino, "﻿" + aTexto(tabla), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo " + destino, e);
        }
    }

    static String escapar(String valor) {
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
