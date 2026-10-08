# Scripts de esquema (db/scripts)

El esquema de la base de datos (Supabase/Postgres, esquema `public`) se maneja **a mano**, no con un motor de migraciones. `spring.jpa.hibernate.ddl-auto` se mantiene en `none` en todos los perfiles salvo el de tests (ver `CLAUDE.md`, sección "Base de datos"). El código nunca crea ni altera tablas en desarrollo/producción.

## Convención

- Cada cambio de esquema es un archivo nuevo `NNN_descripcion.sql` en esta carpeta, con numeración consecutiva de 3 dígitos (`001_`, `002_`, `003_`, …). Nunca se reutiliza ni se salta un número.
- Cada script empieza con un comentario de cabecera:

  ```sql
  -- Tarea: VET-xx
  -- Fecha: AAAA-MM-DD
  -- Qué hace: descripción breve del cambio
  ```

- Los scripts deben ser **idempotentes** siempre que el DDL lo permita: `CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS`, `CREATE INDEX IF NOT EXISTS`, etc. Si una sentencia no admite una forma idempotente (p. ej. un `UPDATE` de backfill), se documenta explícitamente en el comentario del script que no es seguro re-ejecutarlo.
- Un script **nunca se edita** después de haberse ejecutado en Supabase. Si hace falta corregir algo, se crea un script nuevo con el siguiente número.
- El esquema **nunca** se cambia a mano desde el panel de Supabase sin dejar el script correspondiente aquí — el panel y este directorio deben quedar siempre en sincronía.
- Una persona ejecuta cada script en el SQL Editor de Supabase y deja constancia en la tabla de abajo.
- **Toda tabla nueva se crea con RLS activado** (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY` en el mismo script que la crea). Esta app accede siempre con el usuario de servicio de Postgres (`DB_USERNAME`/`DB_PASSWORD` en `.env`), nunca con las claves anon/auth de Supabase, así que activar RLS sin políticas (`POLICY`) no bloquea a esta app — solo cierra el acceso por la API pública de Supabase (PostgREST) a claves que no deberían tener acceso de todas formas. Ver `003_enable_rls.sql` para las 4 tablas existentes, que se crearon antes de que esta regla existiera y tenían RLS desactivado.

## Pendiente (para un script de limpieza futuro)

- **`uq_veterinary_employee`**: ya existía en `public.veterinary_employee` antes de `002_employee_roles_and_ownership.sql`. Pendiente confirmar si duplica a `ux_employee_user_per_vet` (creado en `002`) — si sí, eliminar uno de los dos. Confirmar con:
  ```sql
  SELECT conname, pg_get_constraintdef(oid)
  FROM pg_constraint
  WHERE conrelid = 'public.veterinary_employee'::regclass;
  ```
- **`public.veterinary.active`**: columna booleana que el código sigue sincronizando (`active = (status = 'ACTIVE')`) desde que `status` se volvió la fuente de verdad en `001_clinic_status_and_profile.sql`. Eliminarla una vez que ningún código la lea/escriba directamente.

## Registro de ejecución

| Script | Tarea | Fecha de ejecución | Quién |
|---|---|---|---|
| `001_clinic_status_and_profile.sql` | VET-09, VET-10, VET-12 | 2026-10-08 | Luis Santiago Gonzalezrubio |
| `002_employee_roles_and_ownership.sql` | VET-09, VET-10 | 2026-10-08 | Luis Santiago Gonzalezrubio |
| `003_enable_rls.sql` | VET-09 (mantenimiento) | 2026-10-08 | Luis Santiago Gonzalezrubio |
