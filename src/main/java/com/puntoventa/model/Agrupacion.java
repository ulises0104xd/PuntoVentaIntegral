package com.puntoventa.model;

/** Forma de agrupar las ventas en el reporte por periodo (CU12). */
public enum Agrupacion {
    DIA("DATE(fecha)"),
    SEMANA("DATE_FORMAT(fecha, '%x-S%v')"),
    MES("DATE_FORMAT(fecha, '%Y-%m')");

    private final String expresionSql;

    Agrupacion(String expresionSql) {
        this.expresionSql = expresionSql;
    }

    /** Fragmento SQL fijo (nunca proviene del usuario). */
    public String expresionSql() {
        return expresionSql;
    }
}
