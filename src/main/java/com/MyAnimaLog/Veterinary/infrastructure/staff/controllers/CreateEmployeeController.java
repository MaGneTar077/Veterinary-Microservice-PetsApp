package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.CreateEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agrega a alguien al equipo por {@code userId}, <strong>sin su consentimiento</strong>. Lo
 * reemplazan las invitaciones por correo de la Fase 3
 * ({@code POST /api/veterinary/{id}/invitations} + {@code .../invitations/accept}), que exigen que
 * la persona acepte desde su propia cuenta.
 *
 * @deprecated el frontend debe migrar a invitaciones. <strong>Eliminar este endpoint antes del
 *             despliegue</strong> — ver "Deuda técnica conocida" en CLAUDE.md y FASE_3_PERSONAL.md.
 */
@Deprecated(since = "Fase 3", forRemoval = true)
@RestController
@RequestMapping("/api/veterinary/employees")
@RequiredArgsConstructor
public class CreateEmployeeController {

    private final CreateEmployeeUseCase createEmployeeUseCase;

    @PostMapping
    public ResponseEntity<CreateEmployeeResponse> create(@RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createEmployeeUseCase.create(request));
    }
}
