-- Tarea: Fase 2B, Parte 3 (perfil profesional)
-- Fecha: 2026-10-21
-- Qué hace: agrega original_file_name a public.verification_documents para mostrar
--   el nombre de archivo que subió el usuario (hoy fileName devuelve el id generado,
--   p. ej. "deaf6b6f-....png"). Las filas existentes quedan en NULL — no hay nombre
--   original que backfillear para documentos ya subidos antes de este script; el
--   código cae al nombre generado cuando esta columna es NULL. Idempotente.

ALTER TABLE public.verification_documents
  ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255);
