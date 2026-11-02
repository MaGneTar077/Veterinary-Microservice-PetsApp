-- Tarea: Fase 3 (Personal) — VET-20/VET-21/VET-22/VET-23
-- Fecha: 2026-10-29
-- Qué hace: crea public.employee_invitations (invitaciones por correo al equipo de la
--   clínica) y agrega public.veterinary_employee.removed_at.
--   - employee_invitations guarda SOLO el hash SHA-256 (hex, 64 chars) del token de
--     invitación, nunca el token en claro: ese viaja únicamente en el evento
--     EMPLOYEE_INVITED hacia el correo del invitado (mismo patrón que los refresh
--     tokens del User service).
--   - role nunca puede ser 'OWNER': al OWNER solo se llega por transferencia de
--     propiedad (ownership-transfer), jamás por invitación.
--   - No existe un estado 'EXPIRED': el vencimiento se deriva de expires_at < now(),
--     así no hace falta un job que mueva filas. Una PENDING vencida la marca REVOKED
--     InviteEmployeeService al reinvitar (si no, bloquearía el índice único parcial).
--   - La FK a veterinary es ON DELETE CASCADE: borrar una clínica no debe fallar por
--     sus invitaciones (una invitación sin clínica no significa nada).
--   - removed_at en veterinary_employee registra cuándo se retiró a un empleado;
--     retirar = active=false + removed_at, nunca DELETE, para no perder la
--     trazabilidad de las historias clínicas que firmó.
-- Idempotente: CREATE TABLE/INDEX IF NOT EXISTS, ADD COLUMN IF NOT EXISTS y los CHECK
-- se recrean con DROP CONSTRAINT IF EXISTS antes de agregarse.

-- 1. employee_invitations
CREATE TABLE IF NOT EXISTS public.employee_invitations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  veterinary_id UUID NOT NULL REFERENCES public.veterinary(id) ON DELETE CASCADE,
  email VARCHAR(200) NOT NULL,
  role VARCHAR(20) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  invited_by UUID NOT NULL,
  accepted_user_id UUID,
  expires_at TIMESTAMPTZ NOT NULL,
  decided_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.employee_invitations DROP CONSTRAINT IF EXISTS employee_invitations_role_check;
ALTER TABLE public.employee_invitations ADD CONSTRAINT employee_invitations_role_check
  CHECK (role IN ('ADMIN', 'VETERINARIAN', 'ASSISTANT', 'RECEPTIONIST'));

ALTER TABLE public.employee_invitations DROP CONSTRAINT IF EXISTS employee_invitations_status_check;
ALTER TABLE public.employee_invitations ADD CONSTRAINT employee_invitations_status_check
  CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED'));

-- una sola invitación PENDING por clínica + email (el email se guarda ya normalizado
-- en minúsculas desde la aplicación; lower() aquí es defensa en profundidad)
CREATE UNIQUE INDEX IF NOT EXISTS ux_pending_invitation_per_vet_email
  ON public.employee_invitations(veterinary_id, lower(email))
  WHERE status = 'PENDING';

-- búsqueda por token al aceptar/rechazar/previsualizar
CREATE UNIQUE INDEX IF NOT EXISTS ux_employee_invitations_token_hash
  ON public.employee_invitations(token_hash);

-- GET /api/veterinary/me/invitations: invitaciones pendientes para el email del token
CREATE INDEX IF NOT EXISTS ix_employee_invitations_email
  ON public.employee_invitations(lower(email));

-- listado por clínica del panel de personal
CREATE INDEX IF NOT EXISTS ix_employee_invitations_veterinary_status
  ON public.employee_invitations(veterinary_id, status);

-- 2. RLS en la tabla nueva (regla de db/scripts/README.md)
ALTER TABLE public.employee_invitations ENABLE ROW LEVEL SECURITY;

-- 3. Fecha de retiro del empleado
ALTER TABLE public.veterinary_employee
  ADD COLUMN IF NOT EXISTS removed_at TIMESTAMPTZ;
