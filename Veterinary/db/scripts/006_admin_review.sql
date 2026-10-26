-- Tarea: Fase 2B, Parte 4A (cola de revisión, detalle, decisiones e IA)
-- Fecha: 2026-10-26
-- Qué hace: agrega las columnas que necesita el panel admin de verificación.
--   - verification_requests.rejection_reason_code: motivo estructurado del rechazo
--     (hoy solo existe reviewer_notes, texto libre; el frontend necesita un código
--     fijo para mostrar el motivo sin parsear texto). CHECK con los 4 valores del
--     enum RejectionReasonCode. Filas existentes quedan en NULL.
--   - veterinary.suspended_at / veterinary.suspension_reason: las usa la Parte 4B
--     (pantalla de clínicas del admin), pero se agregan en este script porque son
--     el mismo tipo de cambio de esquema y no amerita un script nuevo para la 4B.
-- Idempotente.

ALTER TABLE public.verification_requests
  ADD COLUMN IF NOT EXISTS rejection_reason_code VARCHAR(40);

ALTER TABLE public.verification_requests
  DROP CONSTRAINT IF EXISTS verification_requests_rejection_reason_code_check;

ALTER TABLE public.verification_requests
  ADD CONSTRAINT verification_requests_rejection_reason_code_check
  CHECK (rejection_reason_code IS NULL OR rejection_reason_code IN (
    'NOT_IN_REGISTRY', 'DATA_MISMATCH', 'DOCUMENT_INVALID', 'OTHER'
  ));

ALTER TABLE public.veterinary
  ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;

ALTER TABLE public.veterinary
  ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
