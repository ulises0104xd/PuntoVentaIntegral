package com.puntoventa.model;

public record Usuario(int id, String username, String nombre, Rol rol, boolean activo) {
}
