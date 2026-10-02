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

## Registro de ejecución

| Script | Tarea | Fecha de ejecución | Quién |
|---|---|---|---|
| — | — | — | — |
