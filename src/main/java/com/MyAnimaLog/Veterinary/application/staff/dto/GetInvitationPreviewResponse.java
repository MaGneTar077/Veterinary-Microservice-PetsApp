package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pantalla previa a aceptar. Responde {@code 200} también cuando la invitación ya no sirve
 * (vencida, revocada o usada) para que el frontend pinte "esta invitación venció" en vez de tener
 * que interpretar un error: {@code status}, {@code expired} y {@code usable} dicen qué mostrar.
 * El único caso de error es un token que no existe → {@code 404 INVITATION_NOT_FOUND}.
 *
 * <p>{@code email} es el correo al que se envió la invitación y {@code emailMatchesCaller} dice si
 * coincide con el de la sesión actual, para poder avisar "iniciaste sesión con otra cuenta" antes
 * de que el usuario intente aceptar y reciba un 403. Mostrar ese correo es seguro porque para
 * llegar aquí hay que tener el token, que solo llegó a ese buzón.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetInvitationPreviewResponse {
    private UUID invitationId;
    private UUID veterinaryId;
    private String veterinaryName;
    private String veterinaryCity;
    private String logoUrl;
    private EmployeeRole role;
    private String invitedByName;
    private String email;
    private boolean emailMatchesCaller;
    private InvitationStatus status;
    private boolean expired;
    /** {@code true} solo si el invitado puede aceptarla ahora mismo con esta sesión. */
    private boolean usable;
    private LocalDateTime expiresAt;
}
