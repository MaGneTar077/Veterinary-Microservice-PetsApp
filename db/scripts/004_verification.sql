-- Tarea: Fase 2B, Parte 1 (bases) — FASE_2B_VERIFICACION.md secciones 2 (paso 3), 4 y D2/D4
-- Fecha: 2026-10-15
-- Qué hace: agrega las columnas del representante legal y el logo a public.veterinary,
--   y crea las tablas public.legal_consents, public.professional_profiles,
--   public.verification_requests y public.verification_documents, con sus CHECK,
--   índices únicos y RLS activado. Al final, crea una verification_request de tipo CLINIC
--   para cada clínica existente que todavía no tenga una (migración: ACTIVE -> APPROVED,
--   cualquier otro estado -> DRAFT). Todas las sentencias son seguras de reejecutar
--   (CREATE ... IF NOT EXISTS / ADD COLUMN IF NOT EXISTS / el backfill se protege con
--   NOT EXISTS por sujeto).

-- 1. Columnas nuevas en veterinary (representante legal + logo)
ALTER TABLE public.veterinary
  ADD COLUMN IF NOT EXISTS legal_rep_is_owner BOOLEAN,
  ADD COLUMN IF NOT EXISTS legal_rep_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS legal_rep_document_type VARCHAR(10),
  ADD COLUMN IF NOT EXISTS legal_rep_document_number VARCHAR(30),
  ADD COLUMN IF NOT EXISTS logo_url TEXT;

ALTER TABLE public.veterinary DROP CONSTRAINT IF EXISTS veterinary_legal_rep_document_type_check;
ALTER TABLE public.veterinary ADD CONSTRAINT veterinary_legal_rep_document_type_check
  CHECK (legal_rep_document_type IS NULL OR legal_rep_document_type IN ('CC', 'CE', 'PPT'));

-- 2. legal_consents (D4)
CREATE TABLE IF NOT EXISTS public.legal_consents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  veterinary_id UUID REFERENCES public.veterinary(id),
  consent_type VARCHAR(20) NOT NULL,
  version VARCHAR(20) NOT NULL,
  accepted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.legal_consents DROP CONSTRAINT IF EXISTS legal_consents_consent_type_check;
ALTER TABLE public.legal_consents ADD CONSTRAINT legal_consents_consent_type_check
  CHECK (consent_type IN ('DATA_TREATMENT', 'TERMS'));

CREATE INDEX IF NOT EXISTS ix_legal_consents_user ON public.legal_consents(user_id);

-- 3. professional_profiles (sección 3 V4 del plan + FASE_2B sección 4)
-- La licencia es de la persona, no del empleo: user_id es único.
CREATE TABLE IF NOT EXISTS public.professional_profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  full_name VARCHAR(200) NOT NULL,
  document_type VARCHAR(10) NOT NULL,
  document_number VARCHAR(30) NOT NULL,
  license_number VARCHAR(50) NOT NULL,
  profession VARCHAR(10) NOT NULL,
  specialty VARCHAR(120),
  university VARCHAR(200),
  signature_url TEXT,
  license_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
  rejection_reason TEXT,
  verified_at TIMESTAMPTZ,
  verified_by UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.professional_profiles DROP CONSTRAINT IF EXISTS professional_profiles_document_type_check;
ALTER TABLE public.professional_profiles ADD CONSTRAINT professional_profiles_document_type_check
  CHECK (document_type IN ('CC', 'CE', 'PPT'));

ALTER TABLE public.professional_profiles DROP CONSTRAINT IF EXISTS professional_profiles_profession_check;
ALTER TABLE public.professional_profiles ADD CONSTRAINT professional_profiles_profession_check
  CHECK (profession IN ('MV', 'MVZ'));

ALTER TABLE public.professional_profiles DROP CONSTRAINT IF EXISTS professional_profiles_license_status_check;
ALTER TABLE public.professional_profiles ADD CONSTRAINT professional_profiles_license_status_check
  CHECK (license_status IN ('NOT_SUBMITTED', 'PENDING', 'VERIFIED', 'REJECTED'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_professional_profiles_user ON public.professional_profiles(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_professional_profiles_license ON public.professional_profiles(license_number);

-- 4. verification_requests (sección 3 V5 del plan + FASE_2B: attempt_number,
--    truthfulness_declared_at, correction_document_types, correction_requested_at)
CREATE TABLE IF NOT EXISTS public.verification_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  subject_type VARCHAR(20) NOT NULL,
  subject_id UUID NOT NULL,
  status VARCHAR(30) NOT NULL,
  attempt_number INT NOT NULL DEFAULT 1,
  submitted_by UUID NOT NULL,
  submitted_at TIMESTAMPTZ,
  truthfulness_declared_at TIMESTAMPTZ,
  ai_status VARCHAR(20) NOT NULL DEFAULT 'NOT_RUN',
  ai_report JSONB,
  ai_risk_level VARCHAR(10),
  reviewer_id UUID,
  reviewer_notes TEXT,
  decided_at TIMESTAMPTZ,
  registry_checked BOOLEAN NOT NULL DEFAULT FALSE,
  correction_document_types TEXT[],
  correction_requested_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.verification_requests DROP CONSTRAINT IF EXISTS verification_requests_subject_type_check;
ALTER TABLE public.verification_requests ADD CONSTRAINT verification_requests_subject_type_check
  CHECK (subject_type IN ('CLINIC', 'PROFESSIONAL'));

ALTER TABLE public.verification_requests DROP CONSTRAINT IF EXISTS verification_requests_status_check;
ALTER TABLE public.verification_requests ADD CONSTRAINT verification_requests_status_check
  CHECK (status IN ('DRAFT', 'UNDER_REVIEW', 'NEEDS_CORRECTION', 'APPROVED', 'REJECTED'));

ALTER TABLE public.verification_requests DROP CONSTRAINT IF EXISTS verification_requests_ai_status_check;
ALTER TABLE public.verification_requests ADD CONSTRAINT verification_requests_ai_status_check
  CHECK (ai_status IN ('NOT_RUN', 'RUNNING', 'DONE', 'FAILED'));

ALTER TABLE public.verification_requests DROP CONSTRAINT IF EXISTS verification_requests_ai_risk_level_check;
ALTER TABLE public.verification_requests ADD CONSTRAINT verification_requests_ai_risk_level_check
  CHECK (ai_risk_level IS NULL OR ai_risk_level IN ('LOW', 'MEDIUM', 'HIGH'));

-- una sola solicitud no final (DRAFT, UNDER_REVIEW, NEEDS_CORRECTION) por sujeto
CREATE UNIQUE INDEX IF NOT EXISTS ux_verification_requests_open_subject
  ON public.verification_requests(subject_type, subject_id)
  WHERE status IN ('DRAFT', 'UNDER_REVIEW', 'NEEDS_CORRECTION');

CREATE INDEX IF NOT EXISTS ix_verification_requests_status ON public.verification_requests(status, submitted_at);

-- 5. verification_documents (sección 3 V5 del plan)
CREATE TABLE IF NOT EXISTS public.verification_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL REFERENCES public.verification_requests(id) ON DELETE CASCADE,
  document_type VARCHAR(40) NOT NULL,
  storage_path TEXT NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  file_size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_verification_documents_request_type
  ON public.verification_documents(request_id, document_type);

-- 6. RLS en las 4 tablas nuevas (regla de db/scripts/README.md)
ALTER TABLE public.legal_consents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.professional_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.verification_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.verification_documents ENABLE ROW LEVEL SECURITY;

-- 7. Backfill: una verification_request de tipo CLINIC para cada clínica existente
--    que todavía no tenga una. Protegido con NOT EXISTS, así que es seguro reejecutarlo
--    (no duplica filas si ya corrió). submitted_by usa created_by si existe, y si no,
--    cae al user_id del OWNER actual de la clínica (clínicas creadas antes de que
--    created_by existiera, en 001_clinic_status_and_profile.sql).
INSERT INTO public.verification_requests
  (subject_type, subject_id, status, attempt_number, submitted_by, submitted_at, decided_at, reviewer_notes, registry_checked)
SELECT
  'CLINIC',
  v.id,
  'APPROVED',
  1,
  COALESCE(v.created_by, (SELECT e.user_id FROM public.veterinary_employee e WHERE e.veterinary_id = v.id AND e.role = 'OWNER' LIMIT 1)),
  v.created_at,
  now(),
  'migrada antes de la verificación',
  TRUE
FROM public.veterinary v
WHERE v.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM public.verification_requests r WHERE r.subject_type = 'CLINIC' AND r.subject_id = v.id
  );

INSERT INTO public.verification_requests
  (subject_type, subject_id, status, attempt_number, submitted_by)
SELECT
  'CLINIC',
  v.id,
  'DRAFT',
  1,
  COALESCE(v.created_by, (SELECT e.user_id FROM public.veterinary_employee e WHERE e.veterinary_id = v.id AND e.role = 'OWNER' LIMIT 1))
FROM public.veterinary v
WHERE v.status != 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM public.verification_requests r WHERE r.subject_type = 'CLINIC' AND r.subject_id = v.id
  );
