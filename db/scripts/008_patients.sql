-- Tarea: Fase 4A (Vinculación y pacientes) — VET-25/VET-26/VET-27/VET-28(parcial)/VET-29
-- Fecha: 2026-11-08
-- Qué hace: prepara public.user_veterinary_link para la vinculación con ciclo de vida
--   (desvincular ya no borra la fila) y crea public.veterinary_patients (consentimiento
--   por mascota: qué mascotas ve cada clínica).
--
--   1) user_veterinary_link
--      - unlinked_at: desvincularse pasa a ser status='UNLINKED' + esta fecha, nunca un
--        DELETE (hasta hoy UnlinkService borraba la fila). Se conserva para trazabilidad:
--        una clínica tiene que poder explicar por qué dejó de ver a un cliente.
--      - owner_name / owner_email / owner_phone: SNAPSHOT del dueño. Es la excepción
--        deliberada que autoriza VETERINARY_SERVICE_PLAN.md §7.3 (línea 24): localmente
--        solo tenemos user_id, así que sin snapshot no existe búsqueda server-side en
--        GET /api/veterinary/{id}/clients. Se llena al vincular (best-effort desde el
--        User service, por eso nullable) y la respuesta PREFIERE el dato vivo de User:
--        el snapshot solo se usa para buscar y como fallback si User no responde.
--        DATOS PERSONALES (Ley 1581 de 2012): al desvincular, la aplicación BORRA
--        owner_email y owner_phone y conserva solo owner_name (lo mínimo para que el
--        histórico siga siendo legible). Esa limpieza la hace el servicio de
--        desvinculación, no este script.
--      - status pasa a tener CHECK IN ('LINKED','UNLINKED'). Era texto libre sin
--        restricción; los valores existentes se normalizan ANTES de agregar el CHECK.
--      - ux_link_active_user_vet: una sola vinculación ACTIVA por (usuario, clínica).
--        Es parcial (WHERE status='LINKED') y no un UNIQUE pleno a propósito: la tabla
--        no tenía ningún índice único, así que puede traer filas duplicadas del par
--        (user_id, veterinary_id), y las que este script marque UNLINKED se quedan.
--        Por lo mismo, el código NO puede asumir una sola fila por par: tiene que
--        buscar la LINKED, y al re-vincular reactivar la más reciente.
--
--   2) veterinary_patients: una fila por (clínica, mascota). El consentimiento es
--      explícito y revocable: revocar es consent_status='REVOKED' + revoked_at, nunca un
--      DELETE, y volver a compartir reactiva LA MISMA fila (por eso el único es pleno
--      sobre el par y no parcial sobre GRANTED: un único parcial permitiría varias filas
--      REVOKED del mismo par, que es un diseño distinto —histórico completo— y no el que
--      pidió la Fase 4A).
--      - origin ya admite 'REGISTERED_BY_CLINIC' aunque los pacientes walk-in son 4B,
--        para que esa fase no necesite otra migración.
--      - granted_by es el dueño que compartió; queda NULL solo en el walk-in de 4B (la
--        clínica registra la mascota de un dueño que no tiene app).
--      - pet_name / species son SNAPSHOT de trazabilidad (misma excepción del §7.3) y se
--        llenan con la misma llamada en lote a Pets que valida la propiedad. NUNCA se
--        sirven en lugar del dato vivo de Pets: si Pets no responde, los endpoints de
--        pacientes devuelven 503, no datos viejos.
--      - updated_at: la fila se REACTIVA en vez de insertarse de nuevo al volver a
--        compartir una mascota revocada, así que created_at no alcanza para saber cuándo
--        cambió por última vez (revocación o reactivación). El servicio que escribe esta
--        tabla debe fijarla explícitamente en cada UPDATE (Hibernate no la actualiza solo).
--      - Sin FK a pet_id ni a los user_id: esas entidades viven en las bases de Pets y
--        User, no en esta.
--
-- Idempotente: ADD COLUMN IF NOT EXISTS, CREATE TABLE/INDEX IF NOT EXISTS, los CHECK se
-- recrean con DROP CONSTRAINT IF EXISTS antes de agregarse, y los UPDATE de normalización
-- y de deduplicación están acotados con WHERE para que una segunda corrida no toque nada
-- (no hay filas que cumplan la condición).

-- ============================================================
-- 1. user_veterinary_link
-- ============================================================

ALTER TABLE public.user_veterinary_link ADD COLUMN IF NOT EXISTS unlinked_at TIMESTAMPTZ;
ALTER TABLE public.user_veterinary_link ADD COLUMN IF NOT EXISTS owner_name VARCHAR(200);
ALTER TABLE public.user_veterinary_link ADD COLUMN IF NOT EXISTS owner_email VARCHAR(200);
ALTER TABLE public.user_veterinary_link ADD COLUMN IF NOT EXISTS owner_phone VARCHAR(30);

-- 1.1 Normalizar status para que cumpla el CHECK que se agrega abajo.
--     'status' era texto libre; el código solo escribía 'LINKED', pero no había nada que
--     lo garantizara. Cualquier valor que no sea reconocible como UNLINKED se trata como
--     LINKED, que es lo correcto con la semántica vieja: desvincularse BORRABA la fila,
--     así que toda fila que exista hoy es una vinculación activa.
UPDATE public.user_veterinary_link
SET status = 'UNLINKED'
WHERE upper(btrim(status)) = 'UNLINKED'
  AND status <> 'UNLINKED';

UPDATE public.user_veterinary_link
SET status = 'LINKED'
WHERE upper(btrim(status)) <> 'UNLINKED'
  AND status <> 'LINKED';

-- Una fila ya UNLINKED sin fecha no debería existir, pero si existe se le pone una:
-- la aplicación asume que UNLINKED siempre trae unlinked_at.
UPDATE public.user_veterinary_link
SET unlinked_at = now()
WHERE status = 'UNLINKED'
  AND unlinked_at IS NULL;

ALTER TABLE public.user_veterinary_link DROP CONSTRAINT IF EXISTS user_veterinary_link_status_check;
ALTER TABLE public.user_veterinary_link ADD CONSTRAINT user_veterinary_link_status_check
  CHECK (status IN ('LINKED', 'UNLINKED'));

-- 1.2 Resolver duplicados ANTES de crear el índice único parcial.
--     La tabla nunca tuvo un único, así que puede haber varias filas LINKED del mismo par
--     (user_id, veterinary_id) y el CREATE UNIQUE INDEX fallaría. Se conserva como LINKED
--     la más reciente (linked_at más alto; el id desempata para que el resultado sea
--     determinista) y las demás quedan UNLINKED con unlinked_at = now().
--     Idempotente: en una segunda corrida ya no hay particiones con más de una LINKED.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, veterinary_id
               ORDER BY linked_at DESC NULLS LAST, id DESC
           ) AS rn
    FROM public.user_veterinary_link
    WHERE status = 'LINKED'
)
UPDATE public.user_veterinary_link AS link
SET status = 'UNLINKED',
    unlinked_at = COALESCE(link.unlinked_at, now())
FROM ranked
WHERE link.id = ranked.id
  AND ranked.rn > 1;

-- 1.3 Índices
CREATE UNIQUE INDEX IF NOT EXISTS ux_link_active_user_vet
  ON public.user_veterinary_link (user_id, veterinary_id)
  WHERE status = 'LINKED';

-- Listado de clientes de una clínica (GET /api/veterinary/{id}/clients).
CREATE INDEX IF NOT EXISTS ix_link_vet_status
  ON public.user_veterinary_link (veterinary_id, status);

-- Mis clínicas vinculadas (GET /api/veterinary/me/linked-veterinaries).
CREATE INDEX IF NOT EXISTS ix_link_user_status
  ON public.user_veterinary_link (user_id, status);

-- ============================================================
-- 2. veterinary_patients
-- ============================================================

CREATE TABLE IF NOT EXISTS public.veterinary_patients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  veterinary_id UUID NOT NULL REFERENCES public.veterinary(id) ON DELETE CASCADE,
  pet_id UUID NOT NULL,
  origin VARCHAR(20) NOT NULL DEFAULT 'SHARED_BY_OWNER',
  consent_status VARCHAR(20) NOT NULL DEFAULT 'GRANTED',
  granted_by UUID,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at TIMESTAMPTZ,
  pet_name VARCHAR(120),
  species VARCHAR(50),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.veterinary_patients ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE public.veterinary_patients DROP CONSTRAINT IF EXISTS veterinary_patients_origin_check;
ALTER TABLE public.veterinary_patients ADD CONSTRAINT veterinary_patients_origin_check
  CHECK (origin IN ('SHARED_BY_OWNER', 'REGISTERED_BY_CLINIC'));

ALTER TABLE public.veterinary_patients DROP CONSTRAINT IF EXISTS veterinary_patients_consent_status_check;
ALTER TABLE public.veterinary_patients ADD CONSTRAINT veterinary_patients_consent_status_check
  CHECK (consent_status IN ('GRANTED', 'REVOKED'));

-- Una sola fila por (clínica, mascota): compartir de nuevo reactiva esta fila en vez de
-- insertar otra. Este índice es además el que sirve la consulta del endpoint interno
-- `access` (que busca por el par exacto), el más llamado de todo el ecosistema.
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_vet_pet
  ON public.veterinary_patients (veterinary_id, pet_id);

-- Pacientes activos de una clínica, paginado (GET /api/veterinary/{id}/patients).
CREATE INDEX IF NOT EXISTS ix_patients_vet_consent
  ON public.veterinary_patients (veterinary_id, consent_status);

-- Con qué clínicas comparto una mascota (GET /api/veterinary/me/pets/{petId}/veterinaries).
CREATE INDEX IF NOT EXISTS ix_patients_pet_consent
  ON public.veterinary_patients (pet_id, consent_status);

-- Revocación en cascada al desvincularse (todas las mascotas que ESE dueño compartió con
-- ESA clínica) y conteo de mascotas compartidas por cliente.
CREATE INDEX IF NOT EXISTS ix_patients_owner
  ON public.veterinary_patients (granted_by, veterinary_id, consent_status);

-- RLS: igual que el resto del esquema (003_enable_rls.sql), sin POLICY. Esta capa entra
-- con el usuario de servicio de Postgres, no con las claves anon/auth de Supabase, así
-- que activar RLS sin políticas bloquea el acceso por la API pública de Supabase sin
-- afectar a esta app.
ALTER TABLE public.veterinary_patients ENABLE ROW LEVEL SECURITY;
