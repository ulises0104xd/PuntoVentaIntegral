package com.puntoventa.model;

import java.util.List;

/** Resultado tabular genérico (reportes e historiales): encabezados y filas. */
public record TablaDatos(List<String> columnas, List<Object[]> filas) {
}
