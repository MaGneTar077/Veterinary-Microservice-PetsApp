package com.MyAnimaLog.Veterinary.domain.patients.model;

import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vinculación entre un dueño de mascotas y una clínica. Es el requisito previo para compartir
 * mascotas: la clínica no ve ninguna mascota solo por estar vinculada (el consentimiento es por
 * mascota, ver {@link VeterinaryPatient}), pero sin vinculación no se puede compartir nada.
 *
 * <p>Los campos {@code ownerName}/{@code ownerEmail}/{@code ownerPhone} son un <strong>snapshot</strong>
 * del dueño tomado al vincularse: localmente solo tenemos {@code userId}, así que sin ellos no existe
 * búsqueda server-side en el listado de clientes de la clínica (es la excepción que autoriza
 * {@code VETERINARY_SERVICE_PLAN.md} §7.3). Son best-effort —el User service puede no responder o no
 * traer el nombre— y las respuestas <strong>prefieren el dato vivo</strong>: el snapshot sirve para
 * buscar y como fallback.
 *
 * <p>Al desvincularse, {@code ownerEmail} y {@code ownerPhone} se <strong>borran</strong> y solo
 * queda {@code ownerName} (Ley 1581 de 2012: la clínica deja de tener una razón para conservar el
 * contacto de alguien que cortó la relación, pero el histórico tiene que seguir siendo legible).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserVeterinaryLink {
    private UUID id;
    private UUID userId;
    private UUID veterinaryId;

    @Builder.Default
    private LinkStatus status = LinkStatus.LINKED;

    private LocalDateTime linkedAt;
    private LocalDateTime unlinkedAt;

    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;

    public boolean isLinked() {
        return status == LinkStatus.LINKED;
    }
}
