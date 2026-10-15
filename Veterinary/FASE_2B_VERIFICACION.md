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
| GET | `/api/veterinary/admin/verifications?status=&subjectType=&page=&size=` | Bandeja |
| GET | `/api/veterinary/admin/verifications/{requestId}` | Detalle: datos, documentos con URL firmada (10 min), reporte IA, alerta de `sha256` repetido en otra clínica, historial de intentos |
| POST | `…/{requestId}/approve` | `{ "registryChecked": true, "notes"? }` — sin `registryChecked` → `400 REGISTRY_CHECK_REQUIRED` |
| POST | `…/{requestId}/reject` | `{ "reason" }` obligatorio |
| POST | `…/{requestId}/request-correction` | `{ "notes", "documentTypes": [] }` obligatorios |
| POST | `…/{requestId}/rerun-ai` | Volver a correr la IA |

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

---

## 7. Eventos (ya en `VeterinaryEventType`)

| Evento | Destinatario |
|---|---|
| `VETERINARY_REGISTERED` | Creador |
| `VERIFICATION_SUBMITTED` | `null` + `recipient.email` = `SUPPORT_EMAIL` |
| `VERIFICATION_APPROVED` | Quien envió (payload: `subjectType`, `veterinaryName`, `trialEndsAt`) |
| `VERIFICATION_REJECTED` | Quien envió (`reason`, `attemptsRemaining`) |
| `VERIFICATION_CORRECTION_REQUESTED` | Quien envió (`notes`, `documentTypes`) |

---

## 8. Códigos de error

Formato: `{ timestamp, status, error, message, code }`. Se agrega `code` a **todas** las excepciones existentes y nuevas.

| HTTP | `code` |
|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_NIT`, `CONSENT_REQUIRED`, `TRUTHFULNESS_DECLARATION_REQUIRED`, `INVALID_FILE_TYPE`, `REGISTRY_CHECK_REQUIRED`, `MISSING_REQUIRED_DOCUMENTS` |
| 401 | `UNAUTHENTICATED` |
| 403 | `EMAIL_NOT_VERIFIED`, `NOT_VETERINARY_OWNER`, `TENANT_MISMATCH`, `INSUFFICIENT_PERMISSION`, `PLATFORM_ADMIN_REQUIRED`, `CANNOT_MODIFY_SELF`, `CANNOT_MODIFY_OWNER` |
| 404 | `VETERINARY_NOT_FOUND`, `DOCUMENT_NOT_FOUND`, `PROFESSIONAL_PROFILE_NOT_FOUND`, `VERIFICATION_REQUEST_NOT_FOUND` |
| 409 | `VETERINARY_NAME_TAKEN`, `VETERINARY_EMAIL_TAKEN`, `NIT_TAKEN`, `LICENSE_NUMBER_TAKEN`, `CONSENT_VERSION_OUTDATED`, `VERIFICATION_LOCKED`, `INVALID_VERIFICATION_STATE`, `VERIFICATION_ATTEMPTS_EXHAUSTED`, `OWNER_CLINIC_LIMIT_REACHED`, `VETERINARY_NOT_ACTIVE` |
| 413 | `FILE_TOO_LARGE` |
| 503 | `EXTERNAL_SERVICE_UNAVAILABLE` |

---

## 9. Partes de implementación

| Parte | Contenido |
|---|---|
| 1 · Bases | Este documento en el repo, `code` en errores, puerto de almacenamiento (Supabase Storage), script SQL |
| 2 · Asistente de la clínica | Registro con consentimientos, representante, logo, documentos, consulta, envío, reintento |
| 3 · Perfil profesional | Perfil, documentos, firma, envío, `licensed` en el endpoint de miembros |
| 4 · Revisión y IA | Panel admin, aprobar/rechazar/corregir, prueba PRO, quitar atajo de reactivate, puerto IA apagado, eventos, job de limpieza |

Variables nuevas: `SUPABASE_URL`, `SUPABASE_KEY` (service role, solo backend), `SUPPORT_EMAIL`, `AI_VERIFICATION_ENABLED` (default `false`), `IA_SERVICE_URL`, `LEGAL_DATA_POLICY_VERSION`, `LEGAL_TERMS_VERSION`.
