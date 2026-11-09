package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;

import java.security.SecureRandom;

/**
 * El código de vinculación de una clínica: generarlo, armar su enlace y resolverlo. Utilidad
 * estática (no es un puerto ni un servicio), en un solo archivo a propósito: lo que se genera acá
 * tiene que ser exactamente lo que se resuelve acá, y separarlo invitaría a que un lado normalice
 * distinto que el otro.
 *
 * <p>Lo usan la clínica para generar/regenerar su código ({@code GenerateInviteCodeService}, del
 * módulo clinic, porque la columna es de {@code veterinary}) y el dueño para resolverlo
 * ({@code PreviewVeterinaryLinkService}, {@code LinkToVeterinaryService} y los dos alias
 * {@code @Deprecated}) — misma dirección de reuso que {@code LicenseStatusResolver}.
 */
public final class LinkCodes {

    /**
     * Sin caracteres ambiguos: no están {@code I}, {@code L}, {@code O}, {@code 0} ni {@code 1}.
     * El código se dicta por teléfono y se copia de un mostrador, así que confundir O con 0 es un
     * error real, no teórico. Quedan 31 símbolos; con 8 posiciones son ~8.5e11 combinaciones,
     * suficientes para que no haga falta más longitud.
     */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LinkCodes() {
    }

    /**
     * Un código nuevo que no choque con el de otra clínica. Reintenta ante colisión: la columna
     * {@code veterinary.invite_code} es única y perder esa carrera dejaría el insert en un error de
     * constraint sin explicación.
     */
    public static String generateUnique(VeterinaryRepositoryPort veterinaryRepositoryPort) {
        String code;
        do {
            code = random();
        } while (veterinaryRepositoryPort.existsByInviteCode(code));
        return code;
    }

    /**
     * El enlace que se comparte con los dueños. Apunta a la <strong>pantalla del frontend</strong>
     * ({@code LINK_BASE_URL}), no a una ruta de este servicio: el dueño necesita una pantalla que le
     * muestre la clínica y le explique qué datos va a ver, no un endpoint.
     */
    public static String buildLink(String linkBaseUrl, String code) {
        String separator = linkBaseUrl != null && linkBaseUrl.contains("?") ? "&" : "?";
        return linkBaseUrl + separator + "code=" + code;
    }

    /** Mayúsculas y sin espacios: el dueño lo escribe a mano y nadie debería fallar por eso. */
    public static String normalize(String rawCode) {
        return rawCode == null ? null : rawCode.trim().toUpperCase();
    }

    /**
     * El código → la clínica que puede recibir clientes.
     *
     * <p>Un código ausente o vacío es una petición malformada ({@link InvalidInviteCodeException},
     * 400). Un código que no existe <strong>y</strong> uno que existe pero cuya clínica no está
     * {@code ACTIVE} dan el mismo {@link LinkCodeNotFoundException} (404): distinguirlos permitiría
     * ir probando códigos para enumerar qué clínicas están registradas y en qué estado.
     */
    public static Veterinary resolveActiveClinic(VeterinaryRepositoryPort veterinaryRepositoryPort,
                                                  String rawCode) {
        String code = normalize(rawCode);
        if (code == null || code.isBlank()) {
            throw new InvalidInviteCodeException("Link code is required");
        }
        return veterinaryRepositoryPort.findByInviteCode(code)
                .filter(veterinary -> veterinary.getStatus() == VeterinaryStatus.ACTIVE)
                .orElseThrow(LinkCodeNotFoundException::new);
    }

    private static String random() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
