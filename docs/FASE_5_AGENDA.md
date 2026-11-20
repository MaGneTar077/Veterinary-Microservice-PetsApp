# Fase 5 — Agenda (catálogo, horarios y bloqueos)

Guía para el frontend (dueños/clínica) y para Calendar. Cubre los bloques (b) a (f) de la Fase 5:
catálogo de servicios, horarios de la clínica y de empleados, bloqueos de agenda, y los endpoints
internos que consume Calendar. Ver también `docs/integracion/CALENDAR_REQUESTS_TO_VETERINARY.md`
(los 5 pedidos originales de Calendar, ahora resueltos) y `CONTRATOS_COMPARTIDOS.md` (autenticación,
formato de error).

## Índice

1. Convenciones generales (multi-tenant, error, fechas/horas)
2. Catálogo de servicios (bloque b)
3. Horarios de la clínica y de empleados (bloque c)
4. Bloqueos de agenda (bloque c)
5. Internos: catálogo, datos de clínica, miembros, schedule-rules, shared (bloques b2/d/e/f)
6. Catálogo para el dueño (quién lo expone)
7. Configuración de reservas (`.../settings`)

---

## 1. Convenciones generales

### Multi-tenant

- **Token de otra clínica** (el `vet_id` del token de clínica no coincide con el `{veterinaryId}` de
  la ruta): `403 TENANT_MISMATCH`.
- **Recurso de otra clínica** (`serviceId`, `employeeId`, `blockId` existen pero son de otra
  clínica): `404` con el código del recurso (`CLINIC_SERVICE_NOT_FOUND`, `EMPLOYEE_NOT_FOUND`,
  `SCHEDULE_BLOCK_NOT_FOUND`). Nunca se delata que el recurso existe en otra clínica.

### Formato de error

`{timestamp, status, error, message, code}` (`CONTRATOS_COMPARTIDOS.md`). Un `code` nuevo por cada
caso de validación — ver la tabla de cada sección.

### Fechas y horas — cómo debe interpretarlas Calendar

| Dato | Formato | Zona |
|---|---|---|
| `opensAt`/`closesAt` (horarios de clínica y empleado) | `"HH:mm"` | **Local** a `timezone` de la clínica (la misma que devuelve `schedule-rules` y `GET .../services` no incluye zona porque no tiene horas) |
| `startsAt`/`endsAt` de un bloqueo (`schedule-blocks`, `blocks[]` de `schedule-rules`) | ISO 8601 con offset (`2026-03-02T00:00:00-05:00`) | El offset viaja explícito; `schedule-rules` los devuelve normalizados en **UTC** (`Z`) |
| `createdAt`/`updatedAt` de un servicio del catálogo | `"2026-11-19T10:00:00"` (`LocalDateTime`) | **Sin zona** — son metadatos administrativos, no horas de atención; no usarlos para calcular disponibilidad |

Calendar nunca debe asumir que `opensAt`/`closesAt` están en UTC: son horas de pared de la clínica.
Para convertirlas a instantes, combínalas con `timezone` (que `schedule-rules` y
`GET /internal/veterinaries/{id}` ya traen).

### Límites

| Límite | Valor |
|---|---|
| Rango `[from, to)` de `schedule-blocks` y `schedule-rules` | máximo 92 días; `to > from`; los dos obligatorios |
| Franjas por día (horario de clínica o de empleado) | máximo 4 |
| `category` de un servicio | `CONSULTATION`, `VACCINATION`, `SURGERY`, `GROOMING`, `LAB`, `OTHER` |
| `durationMinutes` de un servicio | 5 a 600 |
| `price` | `>= 0`, hasta 2 decimales |
| `reason` de un bloqueo | máximo 200 caracteres |

---

## 2. Catálogo de servicios (bloque b)

Rutas públicas bajo `/api/veterinary/{veterinaryId}/services`. Los servicios **nunca se borran**,
solo se desactivan (`active: false`) — puede haber citas en Calendar que los referencian por id.

### GET `/api/veterinary/{veterinaryId}/services`

Cualquier miembro activo de la clínica (`requireActiveMember`, sin permiso específico). Devuelve
**todos** los servicios (activos e inactivos) — la clínica necesita verlos para reactivar o editar.

Respuesta `200`:
```json
{
  "items": [
    {
      "id": "c1a1b1c1-0000-0000-0000-000000000001",
      "name": "Consulta general",
      "category": "CONSULTATION",
      "durationMinutes": 30,
      "price": 40000,
      "bookableOnline": true,
      "active": true,
      "requiresLicense": false,
      "allowedRoles": ["OWNER", "ADMIN", "VETERINARIAN"]
    }
  ]
}
```

### POST `/api/veterinary/{veterinaryId}/services`

Permiso `CLINIC_CONFIGURE`.

Request:
```json
{
  "name": "Consulta general",
  "category": "CONSULTATION",
  "durationMinutes": 30,
  "price": 40000,
  "bookableOnline": true,
  "requiresLicense": false,
  "allowedRoles": ["OWNER", "ADMIN", "VETERINARIAN"]
}
```
`bookableOnline` default `true`, `requiresLicense` default `false`, `allowedRoles` default
`["OWNER","ADMIN","VETERINARIAN"]` si se omite (nunca lista vacía).

Respuesta `201`:
```json
{
  "id": "c1a1b1c1-0000-0000-0000-000000000001",
  "veterinaryId": "9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b",
  "name": "Consulta general",
  "category": "CONSULTATION",
  "durationMinutes": 30,
  "price": 40000,
  "bookableOnline": true,
  "active": true,
  "requiresLicense": false,
  "allowedRoles": ["OWNER", "ADMIN", "VETERINARIAN"],
  "createdAt": "2026-11-19T10:00:00",
  "updatedAt": "2026-11-19T10:00:00"
}
```

- `400 VALIDATION_ERROR` — nombre vacío/>120, `category` inválida, duración fuera de 5-600, precio
  negativo o con más de 2 decimales, `allowedRoles` vacío o con un rol que no existe.
- `409 CLINIC_SERVICE_NAME_TAKEN` — ya hay un servicio **activo** con ese nombre (sin distinguir
  mayúsculas) en la misma clínica. Reutilizar el nombre de uno **desactivado** sí está permitido.
- `403 INSUFFICIENT_PERMISSION` — sin `CLINIC_CONFIGURE` (ASSISTANT/RECEPTIONIST/VETERINARIAN).
- `404 TENANT_MISMATCH`/`403` según corresponda si el token no es de esta clínica.

### PATCH `/api/veterinary/{veterinaryId}/services/{serviceId}`

Permiso `CLINIC_CONFIGURE`. Edición parcial (campo `null`/ausente = sin cambios).

Request: `{"price": 50000}` → Respuesta `200`: mismo cuerpo que el POST con el precio actualizado.

- `404 CLINIC_SERVICE_NOT_FOUND` — no existe o es de otra clínica.
- `409 CLINIC_SERVICE_NAME_TAKEN` — el nuevo nombre choca con **otro** servicio activo (reenviar el
  mismo nombre sin cambios nunca da 409).
- `400 VALIDATION_ERROR` igual que el POST.

### PATCH `/api/veterinary/{veterinaryId}/services/{serviceId}/deactivate`

Permiso `CLINIC_CONFIGURE`. Sin body.

Respuesta `200`: mismo cuerpo, `"active": false`. `404 CLINIC_SERVICE_NOT_FOUND` si no existe o es
de otra clínica.

---

## 3. Horarios de la clínica y de empleados (bloque c)

Las horas son **locales** a `timezone` de la clínica, formato `"HH:mm"`. `dayOfWeek` es ISO:
`1 = lunes … 7 = domingo`.

### GET / PUT `/api/veterinary/{veterinaryId}/business-hours`

GET: cualquier miembro activo. PUT: `CLINIC_CONFIGURE`, reemplaza la **semana completa** en una
transacción (si falla el insert, la semana anterior queda intacta — nunca una clínica a medio
reemplazar).

El PUT recibe un **arreglo simple** (no un objeto envoltorio):
```json
[
  { "dayOfWeek": 1, "opensAt": "08:00", "closesAt": "12:00" },
  { "dayOfWeek": 1, "opensAt": "14:00", "closesAt": "18:00" }
]
```
Una lista vacía (`[]`) es válida y deja la clínica **sin horario configurado**.

Respuesta (GET y PUT, misma forma — `{timezone, items}`):
```json
{
  "timezone": "America/Bogota",
  "items": [
    { "dayOfWeek": 1, "opensAt": "08:00", "closesAt": "12:00" },
    { "dayOfWeek": 1, "opensAt": "14:00", "closesAt": "18:00" }
  ]
}
```

- `400 INVALID_BUSINESS_HOURS` — `closesAt <= opensAt`, franjas solapadas el mismo día, más de 4
  franjas en un día, `dayOfWeek` fuera de 1-7, hora que no es `"HH:mm"` exacto.
- `403 INSUFFICIENT_PERMISSION` — PUT sin `CLINIC_CONFIGURE`.

### GET / PUT `/api/veterinary/{veterinaryId}/employees/{employeeId}/schedule`

GET: cualquier miembro activo. PUT: `CLINIC_CONFIGURE`. Mismo formato de arreglo/validaciones que
`business-hours`; reemplazo transaccional. `404 EMPLOYEE_NOT_FOUND` si el empleado no existe, no es
de esta clínica, o no está activo.

**No** se exige que el horario del empleado caiga dentro del de la clínica — Calendar hace la
intersección al calcular disponibilidad.

Request (PUT):
```json
[{ "dayOfWeek": 1, "opensAt": "09:00", "closesAt": "13:00" }]
```

Respuesta:
```json
{
  "employeeId": "22bb33cc-44dd-4ee6-8ff7-889900112233",
  "timezone": "America/Bogota",
  "usesClinicHours": false,
  "items": [{ "dayOfWeek": 1, "opensAt": "09:00", "closesAt": "13:00" }]
}
```

**Una lista vacía (`[]`) borra las filas del empleado y lo vuelve al horario de la clínica**
(`usesClinicHours: true`) — no lo deja "sin horario".

---

## 4. Bloqueos de agenda (bloque c)

`employeeId: null` = bloqueo de **toda la clínica**. Se borran de verdad (no hay baja lógica: son
configuración, no dato clínico).

### GET `/api/veterinary/{veterinaryId}/schedule-blocks?from=&to=`

Cualquier miembro activo. `from`/`to` **obligatorios** (ISO con offset), `to > from`, rango máximo
92 días. Devuelve los bloques que se solapan con `[from, to)` (abierto a la derecha: uno que empieza
exactamente en `to` no cuenta; uno que empieza antes de `from` y termina dentro sí).

Respuesta `200`:
```json
{
  "items": [
    {
      "id": "d1e1f1a1-0000-0000-0000-000000000001",
      "employeeId": null,
      "startsAt": "2026-03-02T00:00:00-05:00",
      "endsAt": "2026-03-03T00:00:00-05:00",
      "reason": "Festivo"
    }
  ]
}
```

- `400 INVALID_SCHEDULE_RANGE` — falta `from`/`to`, `to <= from`, o rango > 92 días.

### POST `/api/veterinary/{veterinaryId}/schedule-blocks`

- `CLINIC_CONFIGURE` → puede bloquear a cualquiera, o toda la clínica (`employeeId: null`).
- Sin ese permiso, un **empleado activo puede bloquear solo su propio tiempo** (`employeeId` debe
  ser el suyo); la identidad sale de su membresía resuelta contra la base, no del body ni del
  claim — mandar el `employeeId` de otra persona no sirve para hacerse pasar por ella.
- Un bloqueo de toda la clínica **siempre** exige `CLINIC_CONFIGURE`, aunque el caller sea un
  empleado activo.

Request:
```json
{
  "employeeId": null,
  "startsAt": "2026-03-02T00:00:00-05:00",
  "endsAt": "2026-03-03T00:00:00-05:00",
  "reason": "Festivo"
}
```

Respuesta `201`: mismo cuerpo + `"id"`.

- `400 INVALID_SCHEDULE_BLOCK` — `endsAt <= startsAt`, `reason` > 200 caracteres.
- `403 INSUFFICIENT_PERMISSION` — empleado intentando bloquear a otro, o toda la clínica sin
  `CLINIC_CONFIGURE`.
- `404 EMPLOYEE_NOT_FOUND` — si `employeeId` no es un empleado activo de esta clínica.

### DELETE `/api/veterinary/{veterinaryId}/schedule-blocks/{blockId}`

`CLINIC_CONFIGURE`, o el empleado dueño del bloqueo (`employeeId` del bloqueo == el suyo).
Respuesta `204`. `404 SCHEDULE_BLOCK_NOT_FOUND` si no existe o es de otra clínica. `403` si otro
empleado sin `CLINIC_CONFIGURE` intenta borrar un bloqueo que no es suyo.

---

## 5. Internos (`/internal/**`, `X-Internal-Api-Key`, fuera del gateway)

### GET `/internal/veterinaries/{veterinaryId}/services?activeOnly=&bookableOnline=`

Los dos filtros son **opcionales** y se combinan con **AND**. Sin parámetros, no filtra nada.

- `activeOnly=true` → solo servicios activos.
- `bookableOnline=true|false` → solo los que coincidan. Cualquier otro valor (`"yes"`, `"1"`, ...) →
  `400 VALIDATION_ERROR`.

Respuesta `200` (lista plana):
```json
[
  {
    "id": "c1a1b1c1-0000-0000-0000-000000000001",
    "name": "Consulta general",
    "category": "CONSULTATION",
    "durationMinutes": 30,
    "price": 40000,
    "active": true,
    "bookableOnline": true,
    "requiresLicense": false,
    "employeeIds": ["22bb33cc-44dd-4ee6-8ff7-889900112233"]
  }
]
```

`employeeIds` lo calcula Veterinary, no Calendar: empleados **activos** de la clínica cuyo rol está
en `allowedRoles` y, si `requiresLicense`, con `professional_profiles.license_status == VERIFIED`
(el mismo origen que usa `access`, nunca un claim). Una sola consulta de empleados por petición,
sin importar cuántos servicios haya. Si `employeeIds` queda vacío, el servicio existe pero **no es
reservable** — Calendar debe tratarlo así, no como un error.

### GET `/internal/veterinaries/{veterinaryId}/services/{serviceId}`

Igual, para un servicio puntual — **incluye inactivos** (Calendar los necesita para citas ya
creadas sobre ellos). `404 CLINIC_SERVICE_NOT_FOUND` si no existe o es de otra clínica.

### GET `/internal/veterinaries/{veterinaryId}` (datos de la clínica)

Respuesta `200`:
```json
{
  "id": "9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b",
  "name": "Clínica El Bosque",
  "status": "ACTIVE",
  "allowOnlineBooking": true,
  "bookingRequiresConfirmation": true,
  "cancellationMinHours": 4,
  "timezone": "America/Bogota",
  "address": "Calle 1 #2-3",
  "phone": "3001234567"
}
```
`status` trae los 6 valores reales (`PENDING_DOCUMENTS`, `UNDER_REVIEW`, `NEEDS_CORRECTION`,
`ACTIVE`, `REJECTED`, `SUSPENDED`). `address`/`phone` pueden ser `null`. **Nunca** incluye NIT, razón
social, representante legal ni email. `404 VETERINARY_NOT_FOUND` si no existe.

No confundir con `GET /internal/veterinaries/{id}/summary` (de Medical, sin cambios: solo
`id/name/timezone/status/logoUrl`).

### GET `/internal/veterinaries/{veterinaryId}/members?includeInactive=`

Por defecto solo empleados activos; con `includeInactive=true` también los retirados
(`active: false`).

Respuesta `200` (lista plana):
```json
[
  {
    "employeeId": "22bb33cc-44dd-4ee6-8ff7-889900112233",
    "userId": "aa11bb22-cc33-4dd4-8ee5-ff6677889900",
    "name": "Ana Pérez",
    "role": "VETERINARIAN",
    "active": true,
    "licensed": true,
    "permissions": ["APPOINTMENT_MANAGE", "PATIENT_REGISTER", "CLINICAL_READ", "CLINICAL_READ_BASIC", "NURSING_WRITE", "DOCUMENT_UPLOAD", "CLINICAL_WRITE"]
  }
]
```

- `name` es **best-effort**: viene de `UserServicePort`, resuelto en paralelo para todo el listado
  con un presupuesto total de 1500 ms — si no responde a tiempo, `null`. Nunca se inventa ni se usa
  el email como nombre.
- Sin `email` ni `phone`.
- `licensed` sale del mismo origen que `access` (`professional_profiles.license_status ==
  VERIFIED` por `user_id`), nunca de un claim.
- `permissions` = `RolePermissions.resolve(role, licensed, status de la clínica)`; un empleado
  **retirado** siempre tiene `permissions: []`, sin importar el rol.
- `404 VETERINARY_NOT_FOUND` si la clínica no existe.

### GET `/internal/veterinaries/{veterinaryId}/schedule-rules?from=&to=&employeeId=`

Para que Calendar calcule franjas libres. `from`/`to` obligatorios (máximo 92 días), `employeeId`
opcional.

Respuesta `200`:
```json
{
  "timezone": "America/Bogota",
  "businessHours": [{ "dayOfWeek": 1, "opensAt": "08:00", "closesAt": "17:00" }],
  "employeeHours": [{ "employeeId": "22bb33cc-44dd-4ee6-8ff7-889900112233", "dayOfWeek": 1, "opensAt": "09:00", "closesAt": "13:00" }],
  "blocks": [{ "employeeId": null, "startsAt": "2026-03-02T00:00:00Z", "endsAt": "2026-03-03T00:00:00Z", "reason": "Festivo" }]
}
```
`opensAt`/`closesAt` en `"HH:mm"` locales a `timezone`; `startsAt`/`endsAt` de `blocks` como
instantes **normalizados en UTC**.

**Regla de horas efectivas (`employeeHours`)**, solo empleados activos:
- Si el empleado tiene **al menos una fila** en su horario propio → se devuelven exactamente esas
  filas. Un día sin fila es un día en el que **no atiende**: no se completa con el horario de la
  clínica.
- Si **no tiene ninguna fila** → hereda una copia de `businessHours` con su `employeeId`
  (`usesClinicHours`, igual que en el GET de horario de empleado).
- Si la clínica **no tiene** `businessHours` y el empleado tampoco tiene filas → no genera ninguna
  hora para él.

Con `employeeId`, la misma regla aplica solo a esa persona (`404 EMPLOYEE_NOT_FOUND` si no es
activo de esa clínica). `blocks` de toda la clínica (`employeeId: null`) **siempre** se incluyen,
también al filtrar por `employeeId`.

- `400 INVALID_SCHEDULE_RANGE`, `404 VETERINARY_NOT_FOUND` / `EMPLOYEE_NOT_FOUND`.

### GET `/internal/veterinaries/{veterinaryId}/patients/{petId}/shared`

**Siempre `200`**, nunca `404`:
```json
{ "shared": true }
```
`shared = true` exige consentimiento `GRANTED` de esa mascota con esa clínica **y** que el dueño
que lo dio siga vinculado (`LINKED`) — sin depender de empleados ni del estado de la clínica.
Clínica/mascota inexistentes o consentimiento revocado → `200` con `shared: false`. No expone el id
del dueño.

---

## 6. Catálogo para el dueño (quién lo expone)

**Veterinary no tiene un endpoint público de catálogo para dueños.** Calendar lo ofrece leyendo el
interno:

```
GET /internal/veterinaries/{veterinaryId}/services?activeOnly=true&bookableOnline=true
```

y valida aparte que la clínica acepte reservas en línea con:

```
GET /internal/veterinaries/{veterinaryId}
```
comprobando `status == "ACTIVE"` y `allowOnlineBooking == true`.

---

## 7. Configuración de reservas

Defaults reales en la base (desde que se crea la clínica): `allowOnlineBooking: true`,
`bookingRequiresConfirmation: true`, `cancellationMinHours: 4`. Se editan con:

```
PATCH /api/veterinary/{veterinaryId}/settings
```
permiso `CLINIC_CONFIGURE` (`UpdateVeterinarySettingsService`, ya existía antes de la Fase 5 — no
hizo falta agregar nada nuevo).
