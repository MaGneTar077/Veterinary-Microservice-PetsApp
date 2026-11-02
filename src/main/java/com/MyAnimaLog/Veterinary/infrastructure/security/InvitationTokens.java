package com.MyAnimaLog.Veterinary.infrastructure.security;

import com.MyAnimaLog.Veterinary.infrastructure.storage.Sha256;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tokens de invitación de personal: 256 bits de {@link SecureRandom} codificados en Base64
 * URL-safe sin padding (43 caracteres, alfabeto {@code A-Za-z0-9-_}, seguro en una URL sin
 * escapar). En la base solo se guarda {@link #hash(String)}, igual que los refresh tokens del
 * User service: si alguien lee la tabla, no puede aceptar ninguna invitación.
 *
 * <p>El token en claro solo existe en memoria durante la petición que lo crea y dentro del
 * payload del evento {@code EMPLOYEE_INVITED}. Nunca se loguea ni se devuelve en una respuesta HTTP.
 */
public final class InvitationTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private InvitationTokens() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        return Sha256.hex(token.getBytes(StandardCharsets.UTF_8));
    }
}
