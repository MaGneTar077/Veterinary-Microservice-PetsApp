# Fase 2B — Registro y verificación de clínicas (veterinary-service)

> Especificación cerrada de la Fase 2B. Fuente: diseño de Figma del registro (`MYANIMALOG_VET_REGISTRO.md`, frames `1861:19` a `1911:368`, panel admin `2040:300` y `2040:753`) + decisiones del equipo (15 oct 2026).
> Va en la raíz del repo de Veterinary, junto a `CONTRATOS_COMPARTIDOS.md` y `VETERINARY_SERVICE_PLAN.md`. Si algo de aquí contradice al plan, **manda este documento**.
> Rutas: prefijo `/api/veterinary/...` para la clínica y el usuario; `/api/veterinary/admin/...` para la plataforma; `/internal/...` entre servicios.

---

## 1. Decisiones

| # | Decisión |
|---|---|
| D1 | **El asistente de registro y todo lo de verificación usan el token normal** (`ctx: USER`) y verifican **en la base** que el usuario es el OWNER de la clínica. Motivo: el User service niega el token de clínica a clínicas `REJECTED`, y la clínica rechazada debe poder ver el motivo y reintentar; además el asistente no cambia de token a mitad del flujo. El token de clínica también se acepta (si su `vet_id` coincide). |
| D2 | El representante legal se guarda en `veterinary` (4 columnas). Editable solo con la solicitud en `DRAFT` o `NEEDS_CORRECTION`. |
| D3 | NIT y dígito de verificación: el frontend los une y envía `nit: "901234567-8"`. El backend valida con el value object `Nit` existente. |
| D4 | Consentimientos en una tabla `legal_consents` (quién, qué tipo, qué versión, cuándo). Dos tipos: `DATA_TREATMENT` (tratamiento de datos + análisis de documentos con IA) y `TERMS` (términos y condiciones de MyAnimaLogVet). Las versiones vigentes se configuran en `application.yaml`. |
| D5 | Declaración de veracidad al enviar: se guarda la fecha en la solicitud. |
| D6 | Si el usuario **no es veterinario**, el paso 5 se salta y el registro sigue normal. La clínica puede aprobarse igual; no tendrá escritura clínica hasta que un veterinario con tarjeta verificada sea parte del equipo. |
| D7 | Si el usuario **es veterinario pero no es el representante legal**, el perfil profesional pide su nombre completo, tipo y número de documento. El perfil siempre recibe esos tres campos; el frontend los prellena desde el paso 3 cuando "eres tú". |
| D8 | La verificación de la clínica y la del perfil profesional son **independientes**: el admin decide cada una por separado. Rechazar el perfil no rechaza la clínica. |
| D9 | Al aprobar la clínica se crea una suscripción de prueba PRO de 30 días con la tabla actual (`plan = 'PRO_TRIAL'`). Se migra al modelo de planes en la Fase 6. |
| D10 | IA: puerto y adaptador listos con el contrato de `IA_SERVICE_CHANGES` (sección 13 del maestro), **apagado por defecto** (`AI_VERIFICATION_ENABLED=false`). Sin IA, `ai_status = NOT_RUN` y la revisión humana sigue igual. |
| D11 | Borrado a los 90 días de documentos de solicitudes rechazadas: endpoint de job listo; su programación en Cloud Scheduler queda para la Fase 6. |
| D12 | Departamento y ciudad: lista fija en el frontend (DANE). El backend solo valida que no vengan vacíos. |
| D13 | Todo error devuelve además un campo `code` estable (sección 8). |

---

## 2. Campos del asistente

### Antes de empezar (`1861:19`) — se envían en el registro

| Campo | Obligatorio | Backend |
|---|---|---|
| Acepto la política de tratamiento de datos y el análisis de documentos con IA | Sí | `acceptedDataPolicyVersion` → `legal_consents` tipo `DATA_TREATMENT` |
| Acepto los términos y condiciones de MyAnimaLogVet | Sí | `acceptedTermsVersion` → `legal_consents` tipo `TERMS` |

Las versiones deben ser iguales a las vigentes (`app.legal.data-policy-version`, `app.legal.terms-version`); si no → `409 CONSENT_VERSION_OUTDATED`. Si faltan → `400 CONSENT_REQUIRED`.

Requisito previo: `email_verified = true` en el token (ya existe). Si no → `403 EMAIL_NOT_VERIFIED`.

### Paso 1 · Crea tu cuenta (`1899:73`)

Lo maneja el **User service**. Veterinary no recibe nada.

### Paso 2 · Tu clínica (`1901:132`) — `POST /api/veterinary/register`

| Campo (request) | Obligatorio | Columna | Validación |
|---|---|---|---|
| `name` | Sí | `name` | No vacío, único → `409 VETERINARY_NAME_TAKEN` |
| `legalName` | Sí | `legal_name` | No vacío |
| `nit` | Sí | `nit` | `\d{9}-\d` + DV DIAN → `400 INVALID_NIT`; único → `409 NIT_TAKEN` |
| `phone` | Sí | `phone` | 7 a 15 dígitos, `+` opcional |
| `email` | Sí | `email` | Formato; único → `409 VETERINARY_EMAIL_TAKEN` |
| `department` | Sí | `department` | No vacío |
| `city` | Sí | `city` | No vacío |
| `address` | Sí | `address` | No vacío |
| `latitude`, `longitude` | No (recomendado) | `latitude`, `longitude` | Si viene uno, viene el otro; rangos válidos |
| Logo | No | `logo_url` | **Llamada aparte** después del registro (sección 5) |

> Cambia respecto a la Fase 2A: `legalName`, `phone`, `department` y `address` pasan a obligatorios.

### Paso 3 · Representante legal (`1902:215`) — `PUT /api/veterinary/{id}/legal-representative`

| Campo | Obligatorio | Columna |
|---|---|---|
| `isOwner` (¿eres tú?) | Sí | `legal_rep_is_owner` |
| `fullName` | Sí | `legal_rep_name` |
| `documentType` (`CC`, `CE`, `PPT`) | Sí | `legal_rep_document_type` |
| `documentNumber` | Sí (solo dígitos para CC; alfanumérico para CE y PPT) | `legal_rep_document_number` |

### Paso 4 · Documentos (`1910:20`)

| Documento | `documentType` | Obligatorio |
|---|---|---|
| RUT | `RUT` | Sí |
| Cámara de Comercio (o matrícula mercantil si es persona natural) | `CHAMBER_OF_COMMERCE` | Sí |
| Cédula del representante | `LEGAL_REP_ID` | Sí |
| Foto de la fachada | `FACADE_PHOTO` | No |

PDF, JPEG o PNG, hasta 10 MB. Un documento activo por tipo: subir otro del mismo tipo **reemplaza** al anterior (borra el archivo viejo).

### Paso 5 · Perfil profesional (`1910:131`) — `PUT /api/veterinary/me/professional-profile`

Solo si el usuario responde "Sí, soy veterinario". Si responde "No", el frontend salta el paso y no llama nada.

| Campo | Obligatorio | Columna |
|---|---|---|
| `fullName` | Sí | `full_name` |
| `documentType` (`CC`, `CE`, `PPT`) | Sí | `document_type` |
| `documentNumber` | Sí | `document_number` |
| `profession` (`MV`, `MVZ`) | Sí | `profession` |
| `licenseNumber` | Sí | `license_number` (único → `409 LICENSE_NUMBER_TAKEN`) |
| `specialty` | No | `specialty` |
| `university` | No | `university` |

Documentos del perfil (`POST /api/veterinary/me/professional-profile/documents`):

| Documento | `documentType` | Obligatorio |
|---|---|---|
| Tarjeta profesional | `PROFESSIONAL_CARD` | Sí |
| Cédula | `ID_DOCUMENT` | Sí |
| Título | `DEGREE` | No |

La firma (PNG) **no** va en el registro: se sube después desde "Mi perfil profesional".

### Paso 6 · Revisa y envía (`1910:244`) — `POST /api/veterinary/{id}/verification/submit`

| Campo | Obligatorio |
|---|---|
| `truthfulnessDeclared: true` | Sí → si no, `400 TRUTHFULNESS_DECLARATION_REQUIRED` |
| `includeProfessionalProfile` | No (default `false`). Si `true`, envía también el perfil profesional del usuario |

---

## 3. Estados

### Clínica (`veterinary.status`) y solicitud (`verification_requests.status`)

| Acción | Solicitud | Clínica |
|---|---|---|
| Registro | `DRAFT` (intento 1) | `PENDING_DOCUMENTS` |
| Enviar | `UNDER_REVIEW` | `UNDER_REVIEW` |
| Admin pide corrección | `NEEDS_CORRECTION` | `NEEDS_CORRECTION` |
| Reenviar tras corregir | `UNDER_REVIEW` | `UNDER_REVIEW` |
| Admin aprueba | `APPROVED` | `ACTIVE` (+ `approved_at`, prueba PRO 30 días) |
| Admin rechaza | `REJECTED` | `REJECTED` |
| Reintentar tras rechazo (máx. 3 intentos en total) | nueva `DRAFT` (intento N+1) | `PENDING_DOCUMENTS` |

- Con el 3er rechazo, `attemptsRemaining = 0` y reintentar → `409 VERIFICATION_ATTEMPTS_EXHAUSTED` ("contacta a soporte").
- Representante, logo y documentos solo se modifican con la solicitud en `DRAFT` o `NEEDS_CORRECTION` → si no, `409 VERIFICATION_LOCKED`.
- Se elimina el atajo temporal de la Fase 2A (TODO VET-17): `reactivate` vuelve a aceptar solo `SUSPENDED → ACTIVE`. Aprobar clínicas nuevas es exclusivo de `approve`.

### Estado de cada documento (lo calcula el backend para la UI)

| Estado | Regla |
|---|---|
| `PENDING` | No hay archivo de ese tipo |
| `READY` | Hay archivo y no está pedido en corrección (o se subió después de pedirla) |
| `NEEDS_CORRECTION` | Su tipo está en `correction_document_types` y se subió antes de `correction_requested_at` |
| `REVIEWED` | La solicitud está en `NEEDS_CORRECTION` pero este tipo de documento **no** fue pedido — ya quedó aprobado en la revisión anterior, no se puede resubir ni borrar (`409 DOCUMENT_NOT_IN_CORRECTION` si se intenta) |

### Perfil profesional (`license_status`)

`NOT_SUBMITTED` → enviar → `PENDING` → admin aprueba `VERIFIED` / rechaza `REJECTED` / pide corrección (vuelve editable). Si cambia `licenseNumber` estando `VERIFIED`, vuelve a `NOT_SUBMITTED`. No editable en `PENDING`.

Al quedar `VERIFIED`, el endpoint interno de miembros devuelve `licensed = true` para ese usuario en todas sus clínicas (llega al token en el siguiente refresh, hasta 60 min).

---

## 4. Datos (script SQL, todo en `public`, RLS activado en tablas nuevas)

**`veterinary`** — columnas nuevas: `legal_rep_is_owner BOOLEAN`, `legal_rep_name VARCHAR(200)`, `legal_rep_document_type VARCHAR(10)`, `legal_rep_document_number VARCHAR(30)`, `logo_url TEXT`.

**`legal_consents`** — `id`, `user_id`, `veterinary_id`, `consent_type` (`DATA_TREATMENT` | `TERMS`), `version`, `accepted_at`.

**`professional_profiles`** — como en el plan (sección 3, V4): `user_id` único, `full_name`, `document_type`, `document_number`, `license_number` único, `profession`, `specialty`, `university`, `signature_url`, `license_status`, `rejection_reason`, `verified_at`, `verified_by`, timestamps.

**`verification_requests`** — como en el plan (V5) más: `attempt_number INT`, `truthfulness_declared_at TIMESTAMPTZ`, `correction_document_types TEXT[]`, `correction_requested_at TIMESTAMPTZ`. Índice: una sola solicitud no final (`DRAFT`, `UNDER_REVIEW`, `NEEDS_CORRECTION`) por sujeto.

**`verification_documents`** — como en el plan (V5). Índice único `(request_id, document_type)`.

**Storage (Supabase, se crean a mano):** bucket público `veterinary-logos` (`veterinaries/{id}/logo.<ext>`); bucket privado `verification-documents` (`verifications/{requestId}/{documentId}.<ext>`), solo URLs firmadas de 10 min.

---

## 5. Endpoints

### Clínica (token normal + OWNER verificado en la base; D1)

| Método | Ruta | Para qué |
|---|---|---|
| POST | `/api/veterinary/register` | Paso 2 + consentimientos (modificar el existente) |
| PUT | `/api/veterinary/{id}/legal-representative` | Paso 3 |
| PUT | `/api/veterinary/{id}/logo` | Subir logo (multipart `file`; PNG/JPEG/WEBP ≤2 MB) |
| DELETE | `/api/veterinary/{id}/logo` | Quitar logo |
| GET | `/api/veterinary/{id}/verification` | Estado completo para el asistente y las pantallas de estado |
| POST | `/api/veterinary/{id}/verification/documents` | Subir o reemplazar (multipart `documentType`, `file`) |
| DELETE | `/api/veterinary/{id}/verification/documents/{docId}` | Borrar |
| POST | `/api/veterinary/{id}/verification/submit` | Paso 6 |
| POST | `/api/veterinary/{id}/verification/retry` | Nuevo intento tras rechazo |

El logo y la configuración siguen permitidos también para OWNER/ADMIN con token de clínica (`CLINIC_CONFIGURE`) una vez la clínica está operando.

Respuesta de `GET …/verification`:

```json
{
  "veterinaryId": "uuid", "veterinaryStatus": "NEEDS_CORRECTION",
  "requestId": "uuid", "requestStatus": "NEEDS_CORRECTION",
  "attemptNumber": 1, "maxAttempts": 3, "attemptsRemaining": 2,
  "submittedAt": "…", "decidedAt": null,
  "reviewerNotes": "La Cámara de Comercio tiene más de 30 días",
  "rejectionReason": null,
  "rejectionReasonCode": null,
  "legalRepresentative": { "isOwner": true, "fullName": "…", "documentType": "CC", "documentNumber": "…" },
  "documents": [
    { "documentType": "RUT", "required": true, "status": "READY", "documentId": "uuid",
      "fileName": "rut.pdf", "uploadedAt": "…" },
    { "documentType": "CHAMBER_OF_COMMERCE", "required": true, "status": "NEEDS_CORRECTION", "documentId": "uuid", "fileName": "…", "uploadedAt": "…" },
    { "documentType": "LEGAL_REP_ID", "required": true, "status": "PENDING", "documentId": null },
    { "documentType": "FACADE_PHOTO", "required": false, "status": "PENDING", "documentId": null }
  ],
  "professionalProfile": { "exists": true, "licenseStatus": "PENDING" },
  "canEdit": true, "canSubmit": false, "missingRequired": ["LEGAL_REP_ID"],
  "documentsDeletionDate": null
}
```

Nunca incluye el reporte de la IA ni el nivel de riesgo. `documentsDeletionDate` = `decided_at + 90 días` cuando está rechazada (pantalla "No aprobada").

### Perfil profesional (token normal; el perfil es del usuario del token)

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/veterinary/me/professional-profile` | Ver (404 si no existe) |
| PUT | `/api/veterinary/me/professional-profile` | Crear o editar (paso 5) |
| POST | `/api/veterinary/me/professional-profile/documents` | Subir o reemplazar documento |
| DELETE | `/api/veterinary/me/professional-profile/documents/{docId}` | Borrar documento |
| PUT | `/api/veterinary/me/professional-profile/signature` | Firma (PNG, bucket privado) |
| POST | `/api/veterinary/me/professional-profile/submit` | Enviar a verificación (si no se envió con la clínica) |
| GET | `/api/veterinary/{id}/employees/{employeeId}/professional-profile` | Ver perfil de un empleado (miembro de la clínica; sin documentos ni documento de identidad) |

### Plataforma (`PLATFORM_ADMIN`)

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/veterinary/admin/verifications?status=&subjectType=&page=&size=` | Bandeja. `status` default `UNDER_REVIEW`; `page` default `0`, `size` default `20`. Respuesta: `{ items, page, size, total, counts: { UNDER_REVIEW, NEEDS_CORRECTION, APPROVED, REJECTED } }` — `counts` respeta el filtro `subjectType` pero no el de `status` |
| GET | `/api/veterinary/admin/verifications/{requestId}` | Detalle: datos declarados, `account` (del User service, con `{ id }` si el User service falla), documentos con `signedUrl` (10 min), `ai` (`aiStatus`/`aiRiskLevel`/`report` parseado), `duplicates` (otro sujeto con el mismo `sha256`), `history` (intentos anteriores), `registryLookupUrl` |
| GET | `…/{requestId}/documents/{documentId}/signed-url` | Renueva la URL firmada de un documento cuando venció en el visor del admin |
| POST | `…/{requestId}/approve` | `{ "registryChecked": true, "notes"? }` — sin `registryChecked: true` → `400 REGISTRY_CHECK_REQUIRED` |
| POST | `…/{requestId}/reject` | `{ "reasonCode": "NOT_IN_REGISTRY"\|"DATA_MISMATCH"\|"DOCUMENT_INVALID"\|"OTHER", "notes" }` — ambos obligatorios, `notes` máx. 1000 |
| POST | `…/{requestId}/request-correction` | `{ "notes", "documentTypes": [] }` — ambos obligatorios, `notes` máx. 1000, los tipos deben pertenecer al sujeto (`ClinicDocumentType`/`ProfessionalDocumentType`) → si no, `400 VALIDATION_ERROR` |
| POST | `…/{requestId}/rerun-ai` | Vuelve a correr la IA (mismo flujo async que el envío); con `AI_VERIFICATION_ENABLED=false` → `409 AI_VERIFICATION_DISABLED` |

Las tres decisiones (`approve`/`reject`/`request-correction`) solo son válidas con la solicitud en `UNDER_REVIEW` → si no, `409 INVALID_VERIFICATION_STATE` (incluye el caso de que otro admin ya decidió mientras tanto — UPDATE condicional por `id`+`status`, nunca una carrera). Un admin no puede decidir una solicitud que él mismo envió (`submitted_by = caller`) → `403 CANNOT_REVIEW_OWN_REQUEST`.

### Interno

| Método | Ruta | Para qué |
|---|---|---|
| POST | `/internal/jobs/verification-documents/cleanup` | Borra archivos de solicitudes `REJECTED` con más de 90 días (idempotente) |

---

## 6. Reglas

1. Solo el OWNER modifica representante, documentos y envía. ADMIN no.
2. Un usuario puede ser OWNER de máximo 3 clínicas no `REJECTED` (ya existe).
3. `sha256` repetido en una clínica distinta → alerta en el detalle del admin; no bloquea.
4. Aprobar clínica, en una transacción: solicitud `APPROVED`, clínica `ACTIVE`, `approved_at`, `reviewer_id`, `decided_at`, suscripción `PRO_TRIAL` de 30 días (si no tiene una activa).
5. Aprobar perfil: `license_status = VERIFIED`, `verified_at`, `verified_by`.
6. Las URLs firmadas solo las ve el admin. La clínica ve nombre, tipo, estado y fecha de sus documentos, no un enlace.
7. Documentos y datos extraídos nunca se escriben en logs.
8. Con la solicitud en `NEEDS_CORRECTION`, solo se puede subir o borrar un documento cuyo tipo esté en `correction_document_types` — cualquier otro tipo → `409 DOCUMENT_NOT_IN_CORRECTION` (aplica igual a la clínica y al perfil profesional).
9. El análisis de IA corre **después del commit** del envío (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`), nunca dentro de la misma transacción que crea/actualiza la solicitud — así un envío conjunto que falla (clínica + perfil) nunca deja un análisis de IA corriendo para algo que en realidad se revirtió. La IA nunca cambia `verification_requests.status`.

---

## 7. Eventos (ya en `VeterinaryEventType`)

| Evento | Destinatario | Payload |
|---|---|---|
| `VETERINARY_REGISTERED` | Creador | `veterinaryId`, `name` |
| `VERIFICATION_SUBMITTED` | `null` + `recipient.email` = `SUPPORT_EMAIL` | `veterinaryId`/`requestId`/`attemptNumber` (clínica) o `subjectType`/`profileId`/`requestId` (perfil) |
| `VERIFICATION_APPROVED` | Quien envió (`submitted_by`) | `subjectType`, `name` (nombre de la clínica o del profesional), `trialEndsAt` (solo si se creó una prueba nueva) |
| `SUBSCRIPTION_TRIAL_STARTED` | Quien envió la clínica aprobada | `veterinaryId`, `trialEndsAt` — solo se publica si al aprobar no había ya una suscripción activa |
| `VERIFICATION_REJECTED` | Quien envió | `subjectType`, `reasonCode`, `notes`, `attemptsRemaining`, `documentsDeletionDate` |
| `VERIFICATION_CORRECTION_REQUESTED` | Quien envió | `subjectType`, `notes`, `documentTypes` |
| `VETERINARY_SUSPENDED` (Parte 4B) | OWNER activo de la clínica (si existe uno; si no, no se publica) | `veterinaryName`, `reason` (el de la suspensión, 10–500 caracteres), `supportEmail` |
| `VETERINARY_REACTIVATED` (Parte 4B) | OWNER activo de la clínica (si existe uno; si no, no se publica) | `veterinaryName` |

`occurredAt` no va en `payload` — lo pone `DomainEvent.create(...)` en el sobre común (`{eventType, userId, recipient, payload, occurredAt, metadata}`, `occurredAt = Instant.now()`), igual para los 22 eventos de `VeterinaryEventType`, no solo estos dos.

**Todos los eventos se publican después del commit** (Parte 4B, bloque c2): `GooglePubSubEventAdapter.publish(...)` registra un `TransactionSynchronization` y envía de verdad en `afterCommit()` si hay una transacción activa (`TransactionSynchronizationManager.isSynchronizationActive()`); sin transacción activa, publica de inmediato. Esto evita que un `rollback` posterior a `publish(...)` dentro del mismo método `@Transactional` deje salir un evento para un cambio que nunca se persistió (p. ej. que el dueño reciba "clínica suspendida" sin que la base haya cambiado). El cambio vive solo en el adaptador — ninguno de los 8 servicios que llaman `eventPublisherPort.publish(...)` sabe que el envío se retrasa. `NoOpEventPublisher` (`EVENTS_ENABLED=false`) no se tocó, porque solo loguea y no tiene nada que una transacción pueda deshacer.

---

## 8. Códigos de error

Formato: `{ timestamp, status, error, message, code }`. Se agrega `code` a **todas** las excepciones existentes y nuevas.

| HTTP | `code` |
|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_NIT`, `CONSENT_REQUIRED`, `TRUTHFULNESS_DECLARATION_REQUIRED`, `INVALID_FILE_TYPE`, `REGISTRY_CHECK_REQUIRED`, `MISSING_REQUIRED_DOCUMENTS` |
| 401 | `UNAUTHENTICATED` |
| 403 | `EMAIL_NOT_VERIFIED`, `NOT_VETERINARY_OWNER`, `TENANT_MISMATCH`, `INSUFFICIENT_PERMISSION`, `PLATFORM_ADMIN_REQUIRED`, `CANNOT_MODIFY_SELF`, `CANNOT_MODIFY_OWNER`, `CANNOT_REVIEW_OWN_REQUEST`, `CLINIC_SUSPENDED` (Parte 4B — token de clínica sobre una clínica `SUSPENDED`, excepto `GetVeterinaryProfile`), `CANNOT_MANAGE_OWN_CLINIC` (Parte 4B — un admin no puede suspender/reactivar una clínica donde es empleado/dueño) |
| 404 | `VETERINARY_NOT_FOUND`, `DOCUMENT_NOT_FOUND`, `PROFESSIONAL_PROFILE_NOT_FOUND`, `VERIFICATION_REQUEST_NOT_FOUND` |
| 409 | `VETERINARY_NAME_TAKEN`, `VETERINARY_EMAIL_TAKEN`, `NIT_TAKEN`, `LICENSE_NUMBER_TAKEN`, `CONSENT_VERSION_OUTDATED`, `VERIFICATION_LOCKED`, `INVALID_VERIFICATION_STATE`, `VERIFICATION_ATTEMPTS_EXHAUSTED`, `OWNER_CLINIC_LIMIT_REACHED`, `VETERINARY_NOT_ACTIVE`, `DOCUMENT_NOT_IN_CORRECTION`, `AI_VERIFICATION_DISABLED`, `CLINIC_NOT_ACTIVE` (Parte 4B — `suspend` fuera de `ACTIVE`), `CLINIC_NOT_SUSPENDED` (Parte 4B — `reactivate` fuera de `SUSPENDED`) |
| 413 | `FILE_TOO_LARGE` |
| 503 | `EXTERNAL_SERVICE_UNAVAILABLE` |

Nota: `suspend`/`reactivate` (VET-13) usaban antes el código compartido `INVALID_VERIFICATION_STATE` (vía `InvalidVeterinaryStatusTransitionException`, ya eliminada) — desde la Parte 4B tienen códigos propios (`CLINIC_NOT_ACTIVE`/`CLINIC_NOT_SUSPENDED`), más claros para el frontend.

---

## 9. Partes de implementación

| Parte | Contenido | Estado |
|---|---|---|
| 1 · Bases | Este documento en el repo, `code` en errores, puerto de almacenamiento (Supabase Storage), script SQL | ☑ Hecho (2026-10-15) |
| 2 · Asistente de la clínica | Registro con consentimientos, representante, logo, documentos, consulta, envío, reintento | ☑ Hecho (2026-10-15) |
| 3 · Perfil profesional | Perfil, documentos, firma, envío, `licensed` en el endpoint de miembros | ☑ Hecho (2026-10-21) |
| 4A · Revisión, decisiones e IA | Cola, detalle, aprobar/rechazar/corregir (concurrencia + conflicto de interés), prueba PRO, regla de corrección (`REVIEWED`/`DOCUMENT_NOT_IN_CORRECTION`), quitar atajo de `reactivate`, puerto IA + adaptadores NoOp/HTTP, disparo async tras commit, `rerun-ai`, eventos, job de limpieza | ☑ Hecho (2026-10-27) |
| 4B · Panel admin de clínicas y lista de profesionales | Pantalla de clínicas del admin (listar/filtrar, ver detalle no ligado a una solicitud, suspender/reactivar desde el panel) y lista de profesionales (buscar por licencia/nombre, ver estado); `CLINIC_SUSPENDED` en todo endpoint de token de clínica salvo el perfil; eventos publicados tras el commit | ☑ Hecho (2026-10-28) |

Notas de la Parte 2 (diferencias menores de nombres respecto a este documento, por consistencia con el resto del código):
- Endpoints de `GET`/`POST`/`DELETE` sobre `.../verification` implementados como `GetClinicVerification`/`SubmitClinicVerification`/`RetryClinicVerification` (en vez de un nombre genérico "Verification"), dejando el prefijo `Clinic` libre para el equivalente de perfil profesional en la Parte 3.
- `legal_rep_document_type`/`legal_rep_document_number` se guardan como `String` simple en el dominio (no un enum compartido con `professional_profiles.document_type`), para no acoplar los módulos `clinic` y `professional` a nivel de dominio; la validación `CC`/`CE`/`PPT` vive en `UpdateLegalRepresentativeService`.
- `rejectionReason` en la respuesta de `GET .../verification` siempre es `null` por ahora — la tabla `verification_requests` no tiene una columna separada de esa; cuando se implemente `reject` en la Parte 4 se decidirá si reusa `reviewer_notes` o se agrega la columna.
- El `fileName` que ve la clínica en `GET .../verification` y en la respuesta de subida era el nombre generado en storage (`{documentId}.<ext>`) — **resuelto en la Parte 3** con `005_original_file_name.sql` (ver sus notas abajo).

Notas de la Parte 3:
- `005_original_file_name.sql` agrega `verification_documents.original_file_name` (nullable, filas existentes en `NULL`). `VerificationDocument.displayFileName()` devuelve ese valor saneado (sin ruta, máx. 255 — `FileNameSanitizer`) cuando existe, y si no cae al nombre generado por storage; usado tanto por los documentos de la clínica como por los del perfil profesional.
- Los documentos del perfil profesional reusan el mismo par `VerificationDocument`/`VerificationRequest` (`subjectType=PROFESSIONAL`, `subjectId=professional_profiles.id`) y el mismo `FileStoragePort`/`FileSignatureDetector` que los de la clínica — solo cambia el enum de tipos (`ProfessionalDocumentType` en vez de `ClinicDocumentType`) y de dónde sale el sujeto (perfil del token, no el `veterinaryId` del path).
- El envío (`POST .../me/professional-profile/submit`) y el envío conjunto (`SubmitClinicVerification` con `includeProfessionalProfile=true`) comparten la misma transición vía `ProfessionalProfileSubmissionService` — no hay dos implementaciones de "qué significa enviar un perfil profesional".
- `GetEmployeeProfessionalProfileResponse` nunca incluye `documentType`/`documentNumber` (documento de identidad) ni rutas de documentos/firma — solo lo explícitamente pedido en la sección 6: nombre, profesión, tarjeta, especialidad, universidad, `licenseStatus`.

Notas de la Parte 4A:
- `006_admin_review.sql` agrega `verification_requests.rejection_reason_code` y `veterinary.suspended_at`/`suspension_reason` (estas dos últimas las usará la Parte 4B, pero viajaron en el mismo script).
- Concurrencia: decidir una solicitud (`approve`/`reject`/`request-correction`) hace un `UPDATE` condicional (`WHERE id = ? AND status = 'UNDER_REVIEW'`) antes de guardar el resto de los campos — si otro admin ya decidió, el `UPDATE` afecta 0 filas y el segundo admin recibe `409 INVALID_VERIFICATION_STATE`, nunca una condición de carrera silenciosa.
- `GetAdminVerificationDetailService` resuelve `account` llamando a `UserServicePort` (`GET {USER_SERVICE_URL}/internal/users/{id}`, adaptador `UserServiceRestAdapter`, timeout 3 s); si falla, cae a `{ id }` sin romper el resto del detalle.
- `IaVerificationPort.isEnabled()` (no solo la property `AI_VERIFICATION_ENABLED`) es lo que de verdad decide si `VerificationAiAnalysisListener` llama al adaptador — `NoOpIaVerificationAdapter` siempre devuelve `false`; esto es lo que hace que `ai_status` nunca salga de `NOT_RUN` con la IA apagada, sin repetir el nombre de la property en varios sitios.
- El análisis de IA vive enteramente en `VerificationAiAnalysisListener`, disparado por un evento interno de Spring (`VerificationSubmittedEvent`, no un `DomainEvent`/Pub-Sub) que tanto `SubmitClinicVerificationService` como `ProfessionalProfileSubmissionService` publican al terminar; `rerun-ai` publica el mismo evento. `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` (habilitado en `AsyncConfig`) asegura que nunca corre si la transacción que envió se revierte.
- `CorrectionGate` (`application/verification/services`) centraliza la regla "solo se puede subir/borrar un documento pedido en corrección" para los 4 servicios de documentos (clínica y perfil, subir y borrar); `VerificationDocumentStatusCalculator` centraliza el cálculo de `PENDING`/`READY`/`NEEDS_CORRECTION`/`REVIEWED` para los `GET` de clínica y de perfil — ninguna de las dos lógicas está duplicada entre módulos.

Notas de la Parte 4B:
- Listados (`ListAdminClinicsService`/`ListAdminProfessionalsService`) resuelven empleados/clínicas/suscripciones en consultas en lote (`findByVeterinaryIdIn`/`findByUserIdInAndActiveTrue`/`findActiveByVeterinaryIdIn`/`findByIdIn`) — nunca una consulta por fila; cero llamadas al `UserServicePort` en los listados (ver la nota de la sección 10 sobre `email`/`ownerName`).
- `VeterinaryAuthorizationService.requireActiveMember(veterinaryId)` = `requireMember(veterinaryId)` + reconsulta a la base de `veterinary.status` — rechaza con `ClinicSuspendedException` si está `SUSPENDED`. Lo usan `require(id, permission)`, `requireOwnerOrPermission` y la rama de permiso de `requireSelfOrPermission`; `requireMember` por sí solo (sin el chequeo) solo lo sigue usando `GetVeterinaryProfileService`, la única pantalla que debe seguir funcionando con la clínica suspendida.
- `SuspendVeterinaryService`/`ReactivateVeterinaryService` hacen el mismo patrón de `claimForDecision` que ya usaba la Parte 4A (`VeterinaryRepositoryPort.claimStatusTransition`, `UPDATE condicional WHERE status = ?`) para que dos admins decidiendo al mismo tiempo nunca produzcan una carrera silenciosa.
- `InvalidVeterinaryStatusTransitionException` (VET-13) se eliminó — `suspend`/`reactivate` ya no la usan, ver la nota de la sección 8.
- Bloque c2: `GooglePubSubEventAdapter.publish(...)` difiere el envío real a `afterCommit()` cuando hay una transacción activa — ver la nota al final de la sección 7. Cambio aislado al adaptador; ningún servicio que publica eventos se modificó.
- Pendiente del lado del User service (no se puede resolver desde Veterinary): (1) `POST /internal/users/batch` para mostrar `email`/`ownerName` reales en los listados admin sin N llamadas por página (ver sección 10); (2) no debería emitir un token de clínica nuevo para una clínica `SUSPENDED` — hoy Veterinary se protege igual revalidando contra la base en cada llamada (`requireActiveMember`), pero evitar emitir el token de entrada sería una capa extra de defensa.

Variables nuevas: `SUPABASE_URL`, `SUPABASE_KEY` (service role, solo backend), `SUPPORT_EMAIL`, `AI_VERIFICATION_ENABLED` (default `false`), `IA_SERVICE_URL`, `LEGAL_DATA_POLICY_VERSION`, `LEGAL_TERMS_VERSION`, `USER_SERVICE_URL` (Parte 4A, usado por el detalle del admin), `RUES_URL`/`COMVEZCOL_URL` (Parte 4A, opcionales — tienen default).

---

## 10. Todos los endpoints de la Fase 2B (para el frontend)

> Tabla acumulada de las Partes 1 a 4A. `{{base}}` = `/api/veterinary`. "Token normal" = el que devuelve el login (`ctx: USER`); también se acepta un token de clínica cuyo `vet_id` coincida con el de la ruta, salvo donde se diga lo contrario. Los 404/401/403 genéricos (sin token, `NOT_FOUND` de ruta inexistente) no se repiten fila por fila — ver sección 8.

| Método | Ruta | Token / permiso | Para qué |
|---|---|---|---|
| POST | `{{base}}/register` | Token normal + `email_verified=true` | Registrar la clínica (paso 2) + consentimientos; crea al creador como `OWNER` y la primera `verification_request` `CLINIC` en `DRAFT` |
| PUT | `{{base}}/{veterinaryId}/legal-representative` | Token normal, OWNER verificado en base (D1) | Representante legal (paso 3); solo con la solicitud `DRAFT`/`NEEDS_CORRECTION` |
| PUT | `{{base}}/{veterinaryId}/logo` | OWNER (D1) u OWNER/ADMIN con `CLINIC_CONFIGURE` | Subir logo (multipart `file`; PNG/JPEG/WEBP ≤2 MB) |
| DELETE | `{{base}}/{veterinaryId}/logo` | OWNER (D1) u OWNER/ADMIN con `CLINIC_CONFIGURE` | Quitar logo |
| GET | `{{base}}/{veterinaryId}/verification` | OWNER (D1) | Estado completo de la verificación para el asistente y las pantallas de estado |
| POST | `{{base}}/{veterinaryId}/verification/documents` | OWNER (D1) | Subir o reemplazar documento de la clínica (multipart `documentType`, `file`) |
| DELETE | `{{base}}/{veterinaryId}/verification/documents/{documentId}` | OWNER (D1) | Borrar documento de la clínica |
| POST | `{{base}}/{veterinaryId}/verification/submit` | OWNER (D1) | Enviar a revisión (paso 6); `includeProfessionalProfile` opcional |
| POST | `{{base}}/{veterinaryId}/verification/retry` | OWNER (D1) | Nuevo intento tras rechazo (máx. 3 en total) |
| GET | `{{base}}/me/professional-profile` | Token normal | Ver mi perfil profesional (404 si no existe) |
| PUT | `{{base}}/me/professional-profile` | Token normal | Crear o editar mi perfil (paso 5) |
| POST | `{{base}}/me/professional-profile/documents` | Token normal | Subir o reemplazar documento del perfil |
| DELETE | `{{base}}/me/professional-profile/documents/{documentId}` | Token normal | Borrar documento del perfil |
| PUT | `{{base}}/me/professional-profile/signature` | Token normal | Firma (PNG ≤1 MB, bucket privado) |
| POST | `{{base}}/me/professional-profile/submit` | Token normal | Enviar mi perfil a revisión (si no se envió junto con la clínica) |
| GET | `{{base}}/{veterinaryId}/employees/{employeeId}/professional-profile` | Miembro de la clínica (`requireMember`) | Ver el perfil profesional de un empleado — nunca documento de identidad, documentos ni firma |
| GET | `{{base}}/admin/verifications?status=&subjectType=&page=&size=` | `PLATFORM_ADMIN` | Cola de revisión, con `counts` por estado |
| GET | `{{base}}/admin/verifications/{requestId}` | `PLATFORM_ADMIN` | Detalle completo (datos, documentos con `signedUrl`, IA, duplicados, historial) |
| GET | `{{base}}/admin/verifications/{requestId}/documents/{documentId}/signed-url` | `PLATFORM_ADMIN` | Renovar la URL firmada de un documento |
| POST | `{{base}}/admin/verifications/{requestId}/approve` | `PLATFORM_ADMIN` | Aprobar (clínica → `ACTIVE` + prueba PRO 30 días; perfil → `VERIFIED`) |
| POST | `{{base}}/admin/verifications/{requestId}/reject` | `PLATFORM_ADMIN` | Rechazar con `reasonCode` + `notes` |
| POST | `{{base}}/admin/verifications/{requestId}/request-correction` | `PLATFORM_ADMIN` | Pedir corrección de documentos específicos |
| POST | `{{base}}/admin/verifications/{requestId}/rerun-ai` | `PLATFORM_ADMIN` | Volver a correr el análisis de IA (`409` si está apagada) |
| POST | `/internal/jobs/verification-documents/cleanup` | `X-Internal-Api-Key` (servicio a servicio, no un usuario) | Borra documentos de solicitudes `REJECTED` con más de 90 días |
| GET | `{{base}}/admin/veterinaries?status=&search=&plan=&city=&department=&page=&size=` | `PLATFORM_ADMIN` | Parte 4B. Lista de clínicas para el panel admin — todos los filtros son opcionales; `size` máx 50; orden `createdAt` desc |
| GET | `{{base}}/admin/veterinaries/{veterinaryId}` | `PLATFORM_ADMIN` | Parte 4B. Detalle de una clínica: datos, dueño, plan, fechas, `suspensionReason`, empleados, historial de decisiones de verificación |
| PATCH | `{{base}}/admin/veterinaries/{veterinaryId}/suspend` | `PLATFORM_ADMIN` | Parte 4B. Body `{ "reason": "..." }` (10–500 caracteres, obligatorio). Solo `ACTIVE → SUSPENDED`; `403 CANNOT_MANAGE_OWN_CLINIC` si el admin es empleado/dueño de esa clínica |
| PATCH | `{{base}}/admin/veterinaries/{veterinaryId}/reactivate` | `PLATFORM_ADMIN` | Parte 4B. Solo `SUSPENDED → ACTIVE` (ya no acepta `PENDING_DOCUMENTS`/`UNDER_REVIEW`, ver Parte 4A); misma regla de "no sobre tu propia clínica" |
| GET | `{{base}}/admin/professionals?search=&verified=&page=&size=` | `PLATFORM_ADMIN` | Parte 4B. Lista de perfiles profesionales — `search` busca por nombre o número de tarjeta (nunca por email, ver nota abajo); `verified` filtra por `licenseStatus == VERIFIED`; `size` máx 50 |
| GET | `{{base}}/admin/professionals/{profileId}` | `PLATFORM_ADMIN` | Parte 4B. Detalle de un perfil: datos, email, documento de identidad, clínicas donde trabaja con su rol |

No están en esta tabla (pre-existentes, fuera de Fase 2B pero relacionados): `GET /internal/veterinaries/{id}/members/{userId}` (ahora devuelve `licensed` real, Parte 3).

**Nota para el frontend — listados sin email/ownerName (Parte 4B):** ni `GET .../admin/veterinaries` ni `GET .../admin/professionals` incluyen `ownerEmail`/`email` ni un `ownerName` resuelto del User service — el listado de clínicas trae `legalRepName` (el representante legal declarado, no necesariamente el dueño de la cuenta) y el de profesionales no trae ningún campo de contacto. Esto es deliberado: el User service no tiene hoy un endpoint en lote, así que resolverlo por fila en un listado paginado significaría N llamadas HTTP por página. Los campos reales (`ownerName`/`ownerEmail` en el detalle de clínica, `email` en el detalle de profesional) sí se resuelven — una sola llamada al User service por detalle, con fallback (`legalRepName`, o `null`) si falla. Pendiente del lado del User service: un `POST /internal/users/batch` que reciba una lista de ids y devuelva `{id, name, email}` por cada uno — con eso los listados podrían mostrar los campos reales en una sola llamada por página en vez de omitirlos.

**Nota para el frontend — `403 CLINIC_SUSPENDED` (Parte 4B):** cualquier endpoint que use un token de clínica (incluyendo `admin/verifications/**` no aplica, ese usa token normal) puede devolver `403 CLINIC_SUSPENDED` si la clínica fue suspendida después de que el token se emitió (el claim `vet_status` del token puede tener hasta 60 min de antigüedad; Veterinary siempre reconsulta la base). La única excepción es `GET {{base}}/{veterinaryId}` (perfil/estado de la clínica), que sigue respondiendo `200` e incluye `status` y `suspensionReason` — ante un `403 CLINIC_SUSPENDED` en cualquier otro endpoint, el frontend debe redirigir a la pantalla de "clínica suspendida" y consultar ese perfil para mostrar el motivo.

---

## 11. Ejemplos de respuesta (Parte 4B)

**`GET {{base}}/admin/veterinaries?status=ACTIVE&page=0&size=20`** → `200`:

```json
{
  "items": [
    {
      "id": "b3f1c2a0-1111-4a2b-9c3d-000000000001",
      "name": "Clínica El Bosque",
      "legalName": "Clínica El Bosque SAS",
      "nit": "900123456-1",
      "city": "Bogotá",
      "department": "Bogotá D.C.",
      "status": "ACTIVE",
      "plan": "PRO_TRIAL",
      "legalRepName": "María Gómez",
      "employeesCount": 4,
      "createdAt": "2026-09-01T10:15:00",
      "approvedAt": "2026-09-03T14:00:00",
      "suspendedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

**`GET {{base}}/admin/veterinaries/{veterinaryId}`** → `200`:

```json
{
  "id": "b3f1c2a0-1111-4a2b-9c3d-000000000001",
  "name": "Clínica El Bosque",
  "legalName": "Clínica El Bosque SAS",
  "nit": "900123456-1",
  "phone": "3001234567",
  "email": "contacto@elbosque.com",
  "address": "Calle 10 # 20-30",
  "city": "Bogotá",
  "department": "Bogotá D.C.",
  "status": "ACTIVE",
  "plan": "PRO_TRIAL",
  "legalRepName": "María Gómez",
  "ownerName": "Carlos Pérez",
  "ownerEmail": "carlos@elbosque.com",
  "createdAt": "2026-09-01T10:15:00",
  "approvedAt": "2026-09-03T14:00:00",
  "suspendedAt": null,
  "suspensionReason": null,
  "employees": [
    { "employeeId": "e1...", "userId": "u1...", "role": "OWNER", "active": true },
    { "employeeId": "e2...", "userId": "u2...", "role": "VETERINARIAN", "active": true }
  ],
  "verificationHistory": [
    { "attemptNumber": 1, "status": "APPROVED", "rejectionReasonCode": null, "reviewerNotes": null, "decidedAt": "2026-09-03T14:00:00" }
  ]
}
```

Nota: `ownerName` sale del usuario `OWNER` activo vía User service; si ese llamado falla o el usuario no tiene nombre registrado, cae a `legalRepName` (en este ejemplo serían distintos porque el representante legal declarado no es necesariamente quien usa la cuenta).

**`PATCH {{base}}/admin/veterinaries/{veterinaryId}/suspend`** — body `{ "reason": "Pago atrasado por más de 60 días" }` → `200`:

```json
{
  "id": "b3f1c2a0-1111-4a2b-9c3d-000000000001",
  "name": "Clínica El Bosque",
  "status": "SUSPENDED",
  "active": false,
  "suspendedAt": "2026-10-28T09:00:00",
  "suspensionReason": "Pago atrasado por más de 60 días"
}
```

**`GET {{base}}/admin/professionals?verified=true&page=0&size=20`** → `200`:

```json
{
  "items": [
    {
      "id": "c4a2d3b0-2222-4a2b-9c3d-000000000002",
      "fullName": "Juan Pérez",
      "licenseNumber": "TP-12345",
      "profession": "MV",
      "licenseStatus": "VERIFIED",
      "workplaces": [
        { "veterinaryId": "b3f1c2a0-1111-4a2b-9c3d-000000000001", "name": "Clínica El Bosque", "role": "VETERINARIAN" }
      ],
      "createdAt": "2026-08-15T09:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

**`GET {{base}}/admin/professionals/{profileId}`** → `200`:

```json
{
  "id": "c4a2d3b0-2222-4a2b-9c3d-000000000002",
  "fullName": "Juan Pérez",
  "email": "juan.perez@example.com",
  "documentType": "CC",
  "documentNumber": "1020304050",
  "licenseNumber": "TP-12345",
  "profession": "MV",
  "specialty": "Cirugía",
  "university": "Universidad Nacional",
  "licenseStatus": "VERIFIED",
  "rejectionReason": null,
  "verifiedAt": "2026-08-20T11:30:00",
  "workplaces": [
    { "veterinaryId": "b3f1c2a0-1111-4a2b-9c3d-000000000001", "name": "Clínica El Bosque", "role": "VETERINARIAN" }
  ],
  "createdAt": "2026-08-15T09:00:00"
}
```
