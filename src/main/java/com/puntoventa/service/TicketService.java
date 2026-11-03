package com.puntoventa.service;

import com.puntoventa.model.LineaVenta;
import com.puntoventa.model.MetodoPago;
import com.puntoventa.model.Venta;
import com.puntoventa.util.Formato;

/** CU06 (versión mínima): ticket en texto plano de 40 columnas. */
public class TicketService {

    private static final int ANCHO = 40;

    public String generar(Venta v) {
        StringBuilder sb = new StringBuilder();
        sb.append(centrar("PUNTO DE VENTA INTEGRAL")).append('\n');
        sb.append(String.format("Ticket #%06d%n", v.id()));
        sb.append("Fecha: ").append(Formato.fechaHora(v.fecha())).append('\n');
        sb.append("Cajero: ").append(v.cajero()).append('\n');
        sb.append("-".repeat(ANCHO)).append('\n');
        for (LineaVenta l : v.lineas()) {
            String nombre = l.producto().nombre();
            sb.append(nombre.length() > ANCHO ? nombre.substring(0, ANCHO) : nombre).append('\n');
            sb.append(fila("  " + l.cantidad() + " x " + Formato.dinero(l.producto().precio()),
                    Formato.dinero(l.subtotal()))).append('\n');
        }
        sb.append("-".repeat(ANCHO)).append('\n');
        sb.append(fila("Subtotal", Formato.dinero(v.totales().subtotal()))).append('\n');
        if (v.totales().descuento().signum() > 0) {
            sb.append(fila("Descuento (" + v.totales().descuentoPct().stripTrailingZeros().toPlainString() + " %)",
                    "-" + Formato.dinero(v.totales().descuento()))).append('\n');
        }
        sb.append(fila("TOTAL", Formato.dinero(v.totales().total()))).append('\n');
        sb.append(fila("Pago (" + v.metodo() + ")", Formato.dinero(v.recibido()))).append('\n');
        if (v.metodo() == MetodoPago.EFECTIVO) {
            sb.append(fila("Cambio", Formato.dinero(v.cambio()))).append('\n');
        }
        sb.append("-".repeat(ANCHO)).append('\n');
        sb.append(centrar("¡Gracias por su compra!")).append('\n');
        return sb.toString();
    }

    private static String fila(String izquierda, String derecha) {
        int relleno = Math.max(1, ANCHO - izquierda.length() - derecha.length());
        return izquierda + " ".repeat(relleno) + derecha;
    }

    private static String centrar(String texto) {
        int relleno = Math.max(0, (ANCHO - texto.length()) / 2);
        return " ".repeat(relleno) + texto;
    }
}
