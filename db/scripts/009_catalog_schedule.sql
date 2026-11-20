-- Tarea: VET-31/32 (Fase 5 — catálogo de servicios y horarios)
-- Fecha: 2026-11-19
-- Qué hace: crea clinic_services, business_hours, employee_schedules y schedule_blocks
--           (bloque a de la Fase 5). No toca ninguna tabla existente.
--
-- Decisiones tomadas (ver docs/integracion/CALENDAR_REQUESTS_TO_VETERINARY.md y la
-- revisión del plan de Fase 5):
-- - veterinary.status NO se restringe aquí con CHECK: ya tiene 6 valores reales
--   (PENDING_DOCUMENTS, UNDER_REVIEW, NEEDS_CORRECTION, ACTIVE, REJECTED, SUSPENDED)
--   sin constraint hoy; agregarlo es un cambio aparte, no de este script.
-- - allowed_roles default '{OWNER,ADMIN,VETERINARIAN}': en una clínica de un solo
--   profesional, ese profesional suele ser el OWNER (o un ADMIN), no necesariamente
--   alguien con rol VETERINARIAN — un default de solo {VETERINARIAN} dejaría esa
--   clínica sin nadie elegible para sus propios servicios. La restricción médica real
--   se hace con requires_license, evaluado en la aplicación con el mismo dato de
--   licencia que ya devuelve el endpoint interno access (licensed == true), nunca con
--   el claim vet_licensed del token de clínica.
-- - employee_schedules.veterinary_id: no se agrega una FK compuesta contra
--   veterinary_employee(id, veterinary_id) porque esa tabla no tiene hoy un UNIQUE
--   sobre ese par exacto (solo PK sobre id) y agregarlo sería modificar una tabla
--   existente, fuera del alcance de este bloque. Que el empleado pertenezca a esa
--   clínica se valida en la aplicación (misma regla que ya usa el resto del código
--   para no confiar en claims obsoletos).
-- - APPOINTMENT_MANAGE se queda como está en RolePermissions (incluye ASSISTANT).
-- - Defaults de reserva de veterinary (allow_online_booking=true,
--   booking_requires_confirmation=true, cancellation_min_hours=4) no cambian: ya
--   existen y ya son editables por UpdateVeterinarySettingsService.
-- - El contrato de GET /internal/veterinaries/{id}/schedule-rules seguirá la forma de
--   CALENDAR_REQUESTS_TO_VETERINARY.md (timezone, businessHours[], employeeHours[],
--   blocks[]), no la de VETERINARY_SERVICE_PLAN.md §8 — se ajustará el plan en el
--   bloque correspondiente, no aquí.
-- - Rutas públicas bajo /api/veterinary/{id}/services|business-hours|schedule-blocks
--   (prefijo real que enruta el gateway), no /veterinaries/{id}/... como dice el plan.
-- - day_of_week es ISO: 1 = lunes … 7 = domingo.
--
-- Idempotente: CREATE TABLE/INDEX IF NOT EXISTS. Una vez aplicado en Supabase, este
-- script no se vuelve a editar — cualquier cambio posterior va en uno nuevo.

CREATE TABLE IF NOT EXISTS public.clinic_services (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL REFERENCES public.veterinary(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(30) NOT NULL,
  duration_minutes INT NOT NULL,
  price NUMERIC(12,2) NOT NULL,
  bookable_online BOOLEAN NOT NULL DEFAULT TRUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  requires_license BOOLEAN NOT NULL DEFAULT FALSE,
  allowed_roles TEXT[] NOT NULL DEFAULT '{OWNER,ADMIN,VETERINARIAN}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.clinic_services
  DROP CONSTRAINT IF EXISTS clinic_services_category_check;
ALTER TABLE public.clinic_services
  ADD CONSTRAINT clinic_services_category_check
  CHECK (category IN ('CONSULTATION', 'VACCINATION', 'SURGERY', 'GROOMING', 'LAB', 'OTHER'));

ALTER TABLE public.clinic_services
  DROP CONSTRAINT IF EXISTS clinic_services_duration_check;
ALTER TABLE public.clinic_services
  ADD CONSTRAINT clinic_services_duration_check
  CHECK (duration_minutes BETWEEN 5 AND 600);

ALTER TABLE public.clinic_services
  DROP CONSTRAINT IF EXISTS clinic_services_price_check;
ALTER TABLE public.clinic_services
  ADD CONSTRAINT clinic_services_price_check
  CHECK (price >= 0);

-- Cada elemento de allowed_roles debe ser uno de los 5 roles reales de EmployeeRole,
-- y el arreglo no puede quedar vacío (un servicio sin nadie elegible no tiene sentido).
ALTER TABLE public.clinic_services
  DROP CONSTRAINT IF EXISTS clinic_services_allowed_roles_check;
ALTER TABLE public.clinic_services
  ADD CONSTRAINT clinic_services_allowed_roles_check
  CHECK (
    cardinality(allowed_roles) > 0
    AND allowed_roles <@ ARRAY['OWNER','ADMIN','VETERINARIAN','ASSISTANT','RECEPTIONIST']::TEXT[]
  );

-- Un nombre de servicio activo no se repite dentro de la misma clínica.
CREATE UNIQUE INDEX IF NOT EXISTS ux_clinic_services_vet_name_active
  ON public.clinic_services (veterinary_id, lower(name))
  WHERE active;

-- Catálogo de una clínica (GET .../services) y su variante pública (solo active+bookable_online).
CREATE INDEX IF NOT EXISTS ix_clinic_services_vet_active
  ON public.clinic_services (veterinary_id, active);

CREATE TABLE IF NOT EXISTS public.business_hours (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL REFERENCES public.veterinary(id) ON DELETE CASCADE,
  day_of_week SMALLINT NOT NULL,
  opens_at TIME NOT NULL,
  closes_at TIME NOT NULL
);

ALTER TABLE public.business_hours
  DROP CONSTRAINT IF EXISTS business_hours_day_of_week_check;
ALTER TABLE public.business_hours
  ADD CONSTRAINT business_hours_day_of_week_check
  CHECK (day_of_week BETWEEN 1 AND 7);

ALTER TABLE public.business_hours
  DROP CONSTRAINT IF EXISTS business_hours_hours_check;
ALTER TABLE public.business_hours
  ADD CONSTRAINT business_hours_hours_check
  CHECK (closes_at > opens_at);

-- GetBusinessHours / ReplaceBusinessHours (reemplaza la semana completa) y schedule-rules.
CREATE INDEX IF NOT EXISTS ix_business_hours_vet_day
  ON public.business_hours (veterinary_id, day_of_week);

CREATE TABLE IF NOT EXISTS public.employee_schedules (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL REFERENCES public.veterinary(id) ON DELETE CASCADE,
  employee_id UUID NOT NULL REFERENCES public.veterinary_employee(id) ON DELETE CASCADE,
  day_of_week SMALLINT NOT NULL,
  starts_at TIME NOT NULL,
  ends_at TIME NOT NULL
);

ALTER TABLE public.employee_schedules
  DROP CONSTRAINT IF EXISTS employee_schedules_day_of_week_check;
ALTER TABLE public.employee_schedules
  ADD CONSTRAINT employee_schedules_day_of_week_check
  CHECK (day_of_week BETWEEN 1 AND 7);

ALTER TABLE public.employee_schedules
  DROP CONSTRAINT IF EXISTS employee_schedules_hours_check;
ALTER TABLE public.employee_schedules
  ADD CONSTRAINT employee_schedules_hours_check
  CHECK (ends_at > starts_at);

-- GetEmployeeSchedule / ReplaceEmployeeSchedule y employeeHours de schedule-rules
-- (filtrable por employeeId); veterinary_id queda para no tener que pasar por
-- veterinary_employee solo para filtrar por clínica.
CREATE INDEX IF NOT EXISTS ix_employee_schedules_employee_day
  ON public.employee_schedules (employee_id, day_of_week);
CREATE INDEX IF NOT EXISTS ix_employee_schedules_vet
  ON public.employee_schedules (veterinary_id);

CREATE TABLE IF NOT EXISTS public.schedule_blocks (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL REFERENCES public.veterinary(id) ON DELETE CASCADE,
  employee_id UUID NULL REFERENCES public.veterinary_employee(id) ON DELETE CASCADE,
  starts_at TIMESTAMPTZ NOT NULL,
  ends_at TIMESTAMPTZ NOT NULL,
  reason VARCHAR(200)
);

ALTER TABLE public.schedule_blocks
  DROP CONSTRAINT IF EXISTS schedule_blocks_range_check;
ALTER TABLE public.schedule_blocks
  ADD CONSTRAINT schedule_blocks_range_check
  CHECK (ends_at > starts_at);

-- ListScheduleBlocks?from=&to= y el cálculo de franjas libres (blocks[] de schedule-rules).
CREATE INDEX IF NOT EXISTS ix_schedule_blocks_vet_range
  ON public.schedule_blocks (veterinary_id, starts_at, ends_at);

-- RLS: igual que el resto del esquema (003_enable_rls.sql), sin POLICY. Esta capa entra
-- con el usuario de servicio de Postgres, no con las claves anon/auth de Supabase, así
-- que activar RLS sin políticas bloquea el acceso por la API pública de Supabase sin
-- afectar a esta app.
ALTER TABLE public.clinic_services ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.business_hours ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.employee_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.schedule_blocks ENABLE ROW LEVEL SECURITY;
