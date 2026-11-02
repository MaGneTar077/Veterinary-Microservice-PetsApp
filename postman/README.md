# Colección de Postman — `veterinary-service`

Cubre los **61 endpoints** del servicio (todos los del código, incluidos los `/internal/**` y los
deprecados), más 4 requests del User service para conseguir tokens.

| Archivo | Qué es |
|---|---|
| `MyAnimaLogVet.postman_collection.json` | La colección (v2.1). **Generada**, no editar a mano. |
| `MyAnimaLogVet-Local.postman_environment.json` | Environment para correr todo en tu máquina. |
| `MyAnimaLogVet-CloudRun.postman_environment.json` | Environment para Cloud Run (URLs a reemplazar). |
| `build-collection.js` | Genera la colección. Es el archivo que **sí** se edita. |
| `check-coverage.js` | Compara los controladores con la colección. Debe dar 0 faltantes y 0 sobrantes. |

## Importar

1. Postman → **Import** → arrastrá los tres `.json` (la colección y los dos environments).
2. Arriba a la derecha, seleccioná el environment (**Local** o **Cloud Run**).
3. Llená los valores de abajo.

## Qué llenar en cada environment

**Local** ya viene con las URLs puestas; solo faltan las credenciales:

| Variable | Qué poner |
|---|---|
| `owner_email` / `owner_password` | Usuario dueño de clínica, con **correo verificado** (si no, `403 EMAIL_NOT_VERIFIED` al registrar). |
| `admin_email` / `admin_password` | Usuario con `platform_role=PLATFORM_ADMIN`. Solo para las carpetas 05, 06 y 07. |
| `internal_api_key` | El mismo `INTERNAL_API_KEY` del `.env` del servicio. Solo para 12 y 13. |
| `invitation_token` | Token de invitación en claro. Ver "Probar invitaciones sin correos". |

**Cloud Run**: además de lo anterior, reemplazá las tres URLs que dicen `REEMPLAZAR`
(`vet_url`, `user_url`, `gateway_url`).

> `vet_url` es la base del servicio **sin** `/api/veterinary` (ej. `http://localhost:8082`): cada
> request lleva la ruta completa, así la misma variable sirve para `/api/veterinary/**` y para
> `/internal/**`.
>
> Para probar a través del gateway, poné el valor de `gateway_url` en `vet_url`. Ojo: el gateway
> **no** expone `/internal/**`, así que las carpetas 12 y 13 solo funcionan apuntando directo al
> servicio.

### Valores que se llenan solos

Los scripts de test guardan estas variables al vuelo, no hay que escribirlas: `access_token`,
`refresh_token`, `clinic_token`, `veterinaryId`, `employeeId`, `invitationId`, `requestId`,
`documentId`, `professionalProfileId`, `professionalDocumentId`, `professionalId`, `inviteCode`,
`inviteLink`.

Se llenan a mano solo cuando querés apuntar a algo concreto: `memberUserId` (un `userId` para el
endpoint interno de membresía y para el `CreateEmployee` deprecado) y `clientUserId` (el cliente a
desvincular en la carpeta 11).

`admin_token` **no** se guarda solo: logueate con las credenciales de admin en **00 · POST Login** y
copiá el `access_token` resultante a `admin_token`.

Todas las variables de token y contraseña son de tipo `secret` y vienen **vacías**: los archivos de
este repo no contienen ningún secreto real, y Postman las oculta al compartir.

## Orden de prueba recomendado

```
00 · Login                        → access_token
01 · Registrar clínica            → veterinaryId   (o 02 · GET Mis clínicas si ya tenés una)
00 · Token de clínica             → clinic_token
03 · Verificación: representante legal → 3 documentos → GET (canSubmit) → submit
05 · Admin: cola → detalle → approve            (con admin_token)   → clínica ACTIVE
00 · Token de clínica             (de nuevo: el status de la clínica cambió)
08 · Invitar empleado             → invitationId
     (fijar invitation_token con el SQL de abajo)
09 · Preview → Accept             (con el access_token del INVITADO)
10 · Ver el equipo → cambiar rol → retirar / salir / transferir propiedad
```

Dos momentos en los que hay que **volver a pedir el token de clínica**: después de que el admin
aprueba la clínica (cambia `vet_status`) y después de una transferencia de propiedad (cambian los
roles de las dos partes).

## El rol se lee de la base, no del token

Desde la Fase 3 el rol y el estado de la clínica se resuelven **contra la base en cada petición**.
Si te cambian el rol o te retiran, tu token de clínica deja de servir para lo que ya no te
corresponde **en la petición siguiente**, sin esperar los ~60 min que dura.

En Postman eso se ve como un `403 NOT_CLINIC_MEMBER` o `403 INSUFFICIENT_PERMISSION` con un token
que funcionaba hace un minuto. No es un bug: pedí un token nuevo en **00**.

## Probar invitaciones sin correos

Con `EVENTS_ENABLED=false` (lo normal en local y en el piloto) **no sale ningún evento, así que no
se envía ningún correo** — incluido el de invitación. El token en claro solo existe dentro de ese
evento; no hay forma de leerlo desde la API ni de la base (solo se guarda su hash SHA-256).

Para probar la carpeta 09, fijá un token conocido con SQL después de crear la invitación en 08:

```sql
UPDATE public.employee_invitations
SET token_hash = encode(sha256(convert_to('token-prueba-1','UTF8')),'hex')
WHERE id = '<invitationId>';
```

Y poné `invitation_token = token-prueba-1` en el environment.

**Verificado en el código** (las dos preguntas que suelen surgir acá):

- `Sha256.hex()` usa `MessageDigest("SHA-256")` y formatea cada byte con `%02x` → **hex en
  minúsculas**, sobre `token.getBytes(UTF_8)`. Es exactamente lo que produce
  `encode(sha256(convert_to(...,'UTF8')),'hex')`.
- `ResendInvitationService` **sí** reemplaza `token_hash` por el hash de un token nuevo. O sea:
  reenviar **invalida** el token que fijaste a mano y hay que volver a correr el `UPDATE`.

Para que la invitación siga siendo usable: `status = PENDING`, `expires_at` en el futuro, y el
correo del JWT igual al `email` de la fila **y verificado**.

## Regenerar y verificar la colección

La colección se genera para que la estructura repetitiva (auth por carpeta, scripts, ejemplos) no se
desincronice. Si agregás o cambiás un endpoint:

```bash
node postman/build-collection.js          # reescribe el .json de la colección
node postman/check-coverage.js            # 0 faltantes, 0 sobrantes
node postman/check-coverage.js --table    # + tabla carpeta / método / ruta
```

`check-coverage.js` lee los controladores de `src/main/java` (clase `@RestController` →
`@RequestMapping` como base + cada `@*Mapping`) y los compara con los `url.raw` de la colección,
normalizando las path variables. Los requests del User service (`{{user_url}}`, carpeta 00) se
ignoran porque no son endpoints de este servicio.

Si agregás un endpoint y te olvidás de la colección, `check-coverage.js` falla con exit code 1 y te
dice exactamente qué falta.

## Advertencias

- **La carpeta 00 es del User service**, no de este servicio. `POST /auth/local` y
  `POST /auth/context/veterinary/{id}` están documentados en `CLAUDE.md`; `POST /auth/refresh` y
  `GET /auth/me` **no están verificados** contra el código del User service — si las rutas no
  coinciden, ajustalas (no afectan la cobertura).
- **Las carpetas 12 y 13 nunca deben estar expuestas por el gateway.** Son servicio a servicio, con
  `X-Internal-Api-Key` y sin JWT.
- **La carpeta 99 son endpoints deprecados.** `POST /api/veterinary/employees` se elimina antes de la
  clínica piloto: agrega a una persona al equipo sin su consentimiento. No lo uses en el frontend.
- Los requests que suben archivos (logo, documentos, firma) tienen el campo `file` vacío: hay que
  elegir el archivo a mano en Postman antes de enviarlos.
