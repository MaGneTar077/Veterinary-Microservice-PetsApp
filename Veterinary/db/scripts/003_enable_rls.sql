-- Tarea: VET-09 (mantenimiento de seguridad, no ligado a una tarea funcional)
-- Fecha: 2026-10-08
-- Qué hace: documenta en el repo el RLS que ya se activó a mano en el SQL Editor de
--           Supabase para las 4 tablas existentes (estaban desactivado, lo que las
--           exponía sin filtro vía la API de Supabase). Deja el esquema y este
--           directorio en sincronía, como exige el README de esta carpeta.
--           No agrega políticas (POLICY): esta capa accede siempre con el usuario de
--           servicio de Postgres (DB_USERNAME en .env), no con claves anon/auth de
--           Supabase, así que activar RLS sin políticas bloquea el acceso por la API
--           pública de Supabase sin afectar a esta app.

ALTER TABLE public.veterinary ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.veterinary_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.veterinary_subscription ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_veterinary_link ENABLE ROW LEVEL SECURITY;
