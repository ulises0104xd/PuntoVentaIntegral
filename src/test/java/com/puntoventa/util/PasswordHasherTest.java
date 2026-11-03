package com.puntoventa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void contrasenaCorrectaSeVerifica() {
        String hash = PasswordHasher.hash("admin123".toCharArray());
        assertTrue(PasswordHasher.verificar("admin123".toCharArray(), hash));
    }

    @Test
    void contrasenaIncorrectaSeRechaza() {
        String hash = PasswordHasher.hash("admin123".toCharArray());
        assertFalse(PasswordHasher.verificar("Admin123".toCharArray(), hash));
    }

    @Test
    void elHashNoContieneLaContrasenaYUsaSalAleatoria() {
        String h1 = PasswordHasher.hash("secreto".toCharArray());
        String h2 = PasswordHasher.hash("secreto".toCharArray());
        assertFalse(h1.contains("secreto"));
        assertNotEquals(h1, h2);
    }

    @Test
    void hashMalformadoOnuloNoVerifica() {
        assertFalse(PasswordHasher.verificar("x".toCharArray(), "no-es-un-hash"));
        assertFalse(PasswordHasher.verificar("x".toCharArray(), null));
        assertFalse(PasswordHasher.verificar(null, "pbkdf2_sha256$1$AA==$AA=="));
    }
}
