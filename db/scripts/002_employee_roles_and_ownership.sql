-- Tarea: VET-09, VET-10
-- Fecha: 2026-10-07
-- Qué hace: agrega OWNER y RECEPTIONIST al CHECK de role en public.veterinary_employee,
--           promueve al ADMIN activo más antiguo de cada clínica a OWNER, y crea los
--           índices únicos que exigen un solo OWNER por clínica y un solo registro por
--           (veterinary_id, user_id). Incluye diagnósticos comentados para correr a mano
--           ANTES de los índices (ver paso 2) y una consulta real (paso 4) para detectar
--           clínicas que quedarían sin OWNER tras la migración.

-- 1) Permitir OWNER y RECEPTIONIST en el CHECK de role (reemplaza el CHECK anterior si existe).
ALTER TABLE public.veterinary_employee DROP CONSTRAINT IF EXISTS veterinary_employee_role_check;
ALTER TABLE public.veterinary_employee ADD CONSTRAINT veterinary_employee_role_check
  CHECK (role IN ('OWNER','ADMIN','VETERINARIAN','ASSISTANT','RECEPTIONIST'));

-- 2) DIAGNÓSTICO -- correr esto a mano ANTES del paso 5 (los índices únicos). Si cualquiera
--    de las dos consultas devuelve filas, los índices de abajo fallarán al crearse y hay que
--    resolver esos duplicados primero (desactivar o eliminar las filas sobrantes).
--
-- Duplicados de (veterinary_id, user_id) -- romperían ux_employee_user_per_vet:
-- SELECT veterinary_id, user_id, COUNT(*)
-- FROM public.veterinary_employee
-- GROUP BY veterinary_id, user_id
-- HAVING COUNT(*) > 1;
--
-- Clínicas con más de un OWNER -- no debería existir ninguna todavía (el rol no existía
-- hasta el paso 1 de este mismo script), pero por si se cargó algo manualmente antes --
-- romperían ux_one_owner_per_vet:
-- SELECT veterinary_id, COUNT(*)
-- FROM public.veterinary_employee
-- WHERE role = 'OWNER'
-- GROUP BY veterinary_id
-- HAVING COUNT(*) > 1;

-- 3) Migración de datos: promueve, en cada clínica, al ADMIN activo más antiguo
--    (menor created_at) a OWNER.
--    NO idempotente: si se reejecuta después de que alguien ya haya reasignado el OWNER
--    de una clínica a mano, esto lo pisaría de vuelta al ADMIN activo más antiguo.
UPDATE public.veterinary_employee ve
SET role = 'OWNER'
WHERE ve.id IN (
  SELECT DISTINCT ON (ve2.veterinary_id) ve2.id
  FROM public.veterinary_employee ve2
  WHERE ve2.role = 'ADMIN' AND ve2.active = true
  ORDER BY ve2.veterinary_id, ve2.created_at ASC
);

-- 4) Clínicas que quedaron SIN OWNER (no tenían ningún ADMIN activo para promover).
--    Revisa el resultado: esas clínicas necesitan que alguien les asigne un OWNER a mano.
SELECT v.id, v.name
FROM public.veterinary v
WHERE NOT EXISTS (
  SELECT 1 FROM public.veterinary_employee ve
  WHERE ve.veterinary_id = v.id AND ve.role = 'OWNER'
);

-- 5) Índices únicos (correr solo después de confirmar el diagnóstico del paso 2).
CREATE UNIQUE INDEX IF NOT EXISTS ux_one_owner_per_vet ON public.veterinary_employee(veterinary_id) WHERE role = 'OWNER';
CREATE UNIQUE INDEX IF NOT EXISTS ux_employee_user_per_vet ON public.veterinary_employee(veterinary_id, user_id);
