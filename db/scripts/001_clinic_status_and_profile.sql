-- Tarea: VET-09, VET-10, VET-12
-- Fecha: 2026-10-07
-- Qué hace: agrega el estado persistido (status) y los campos de perfil/configuración a
--           public.veterinary. El logo (logo_url) y la tabla de verificación de documentos
--           quedan para la Fase 2B, no se tocan aquí. Migra status desde el booleano
--           active existente (active = true -> ACTIVE, active = false -> SUSPENDED).
--           NO se borra la columna active: el código la mantiene sincronizada
--           (active = (status = 'ACTIVE')) hasta un script de limpieza posterior.
--           city y phone ya existen en la tabla real; no se tocan.

ALTER TABLE public.veterinary
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING_DOCUMENTS',
  ADD COLUMN IF NOT EXISTS legal_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS nit VARCHAR(20),
  ADD COLUMN IF NOT EXISTS address VARCHAR(250),
  ADD COLUMN IF NOT EXISTS department VARCHAR(100),
  ADD COLUMN IF NOT EXISTS latitude NUMERIC(9,6),
  ADD COLUMN IF NOT EXISTS longitude NUMERIC(9,6),
  ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) NOT NULL DEFAULT 'America/Bogota',
  ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'COP',
  ADD COLUMN IF NOT EXISTS default_appointment_minutes INT NOT NULL DEFAULT 30,
  ADD COLUMN IF NOT EXISTS allow_online_booking BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS booking_requires_confirmation BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS cancellation_min_hours INT NOT NULL DEFAULT 4,
  ADD COLUMN IF NOT EXISTS directory_visible BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS created_by UUID,
  ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;

-- Backfill de status desde el booleano active existente.
-- NO idempotente: si se reejecuta después de que alguna clínica ya haya avanzado a otro
-- estado (p. ej. PENDING_DOCUMENTS -> UNDER_REVIEW), esta sentencia la pisaría de vuelta
-- a ACTIVE/SUSPENDED según el booleano. No reejecutar este UPDATE una vez aplicado.
UPDATE public.veterinary SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'SUSPENDED' END;

CREATE UNIQUE INDEX IF NOT EXISTS ux_veterinary_nit ON public.veterinary(nit) WHERE nit IS NOT NULL;
