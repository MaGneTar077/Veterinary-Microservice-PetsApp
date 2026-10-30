# Fase 3 — Personal (equipo de la clínica)

Contrato de la Fase 3 del `veterinary-service` para el **frontend web** y para **Notifications**.
Cubre invitaciones por correo, lista de empleados, cambio de rol, retiro, salida voluntaria y
transferencia de propiedad.

Todo lo de aquí está implementado y en verde (suite 919/919). El esquema lo agregó
`db/scripts/007_staff_invitations.sql`, ya ejecutado en Supabase el 2026-10-29.

---

## 1. Qué token usar

| Token | Cuándo | Endpoints de esta fase |
|---|---|---|
| **Normal** (el del login) | El usuario todavía no opera sobre una clínica concreta | `POST /api/veterinary/invitations/preview`, `.../accept`, `.../decline`, `GET /api/veterinary/me/invitations` |
| **De clínica** (`POST {USER_URL}/auth/context/veterinary/{veterinaryId}`) | Todo lo que opera sobre una clínica | el resto |

Todos los endpoints requieren `Authorization: Bearer <token>`. Ninguno es público.

> **Importante (cambio de la Fase 3)**: el rol y el estado de la clínica ya **no** se leen del token,
> se leen de la base en cada petición. Si a alguien le cambian el rol o lo retiran, su token de
> clínica deja de servir para lo que ya no le corresponde **en la siguiente petición**, sin esperar
> los ~60 min que dura el token. En la práctica el frontend debe estar preparado para recibir
> `403 NOT_CLINIC_MEMBER` o `403 INSUFFICIENT_PERMISSION` con un token que hasta hace un momento
> funcionaba: hay que cerrar el contexto de clínica y volver al selector.

---

## 2. Flujo de invitación, de principio a fin

```
1. OWNER/ADMIN   POST /api/veterinary/{id}/invitations  { email, role }
                 → se crea la invitación (PENDING, vence en 7 días)
                 → se publica EMPLOYEE_INVITED con acceptUrl

2. Notifications envía el correo al invitado con ese acceptUrl:
                 {INVITATION_BASE_URL}?token=<token>

3. El invitado abre el enlace. El frontend lee el token del query string y llama
                 POST /api/veterinary/invitations/preview  { token }
                 → muestra clínica, logo, rol, quién invita, vencimiento
                 → si emailMatchesCaller=false, avisa "iniciaste sesión con otra cuenta"
                 → si usable=false, muestra el motivo (vencida / revocada / ya usada)

4. El invitado decide
                 POST /api/veterinary/invitations/accept   { token }   → queda empleado
                 POST /api/veterinary/invitations/decline  { token }   → invitación cerrada
```

### El token va en el **cuerpo**, nunca en la URL

`preview` es un `POST` aunque no modifique nada, por esto mismo: el gateway y Cloud Run registran
la URL completa de cada petición en logs que no controlamos. El token de invitación es una
credencial; no debe quedar en un log de acceso. Tampoco lo guardamos: en la base solo vive su hash
SHA-256.

### La ruta del frontend que necesitamos

El backend arma el enlace como `{INVITATION_BASE_URL}?token=<token>` (si la URL ya tiene query
string, usa `&`). **Necesitamos que el frontend defina y nos confirme esa ruta** para configurar la
variable de entorno `INVITATION_BASE_URL`; por ejemplo:

```
INVITATION_BASE_URL=https://app.myanimalog.com/invitaciones/equipo
```

Esa pantalla tiene que: leer `token` del query string, llamar a `preview`, y si el usuario no tiene
sesión, mandarlo a login y volver conservando el token.

### Reenviar invalida el enlace anterior

`resend` genera un token nuevo y reemplaza el hash guardado, así que **el enlace del correo viejo
deja de funcionar inmediatamente**. Si el usuario tiene dos correos de invitación, solo sirve el del
último.

---

## 3. Endpoints

### 3.1 Invitaciones (lado clínica)

| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/veterinary/{veterinaryId}/invitations` | `STAFF_MANAGE` (+ ser OWNER si `role=ADMIN`) |
| GET | `/api/veterinary/{veterinaryId}/invitations?status=PENDING` | `STAFF_MANAGE` |
| POST | `/api/veterinary/{veterinaryId}/invitations/{invitationId}/resend` | `STAFF_MANAGE` (+ OWNER si la invitación es de ADMIN) |
| DELETE | `/api/veterinary/{veterinaryId}/invitations/{invitationId}` | `STAFF_MANAGE` |

Reglas:

- Roles invitables: `ADMIN`, `VETERINARIAN`, `ASSISTANT`, `RECEPTIONIST`. **Nunca `OWNER`** (a OWNER
  solo se llega registrando la clínica o por transferencia de propiedad).
- Solo el OWNER puede invitar con rol `ADMIN`, y solo el OWNER puede reenviar una invitación de ADMIN.
- La clínica debe estar `ACTIVE`.
- El correo se normaliza a minúsculas. Una sola invitación `PENDING` por clínica + correo.
- Si existe una `PENDING` **vencida** para ese correo, al reinvitar se marca `REVOKED` y se crea una
  nueva. Si existe una `PENDING` **vigente** → `409 INVITATION_ALREADY_PENDING`.
- `status` por defecto es `PENDING`; para el historial hay que pedir `ACCEPTED`, `DECLINED` o
  `REVOKED` explícitamente (no existe "todas").

`POST .../invitations` → `201`:

```json
{
  "invitation": {
    "id": "f0b0...", "veterinaryId": "9a1c...",
    "email": "nuevo@clinica.com", "role": "VETERINARIAN",
    "status": "PENDING", "expired": false,
    "invitedBy": "7c21...",
    "expiresAt": "2026-11-05T10:12:00", "decidedAt": null,
    "createdAt": "2026-10-29T10:12:00"
  },
  "seatsUsed": 4
}
```

`seatsUsed` = empleados activos + invitaciones `PENDING` vigentes. Hoy es **solo informativo**: el
plan de suscripción todavía no tiene un límite de empleados (llega en la Fase 6), así que nada
bloquea por este número.

`GET .../invitations` → `200` `{ "invitations": [ ...mismo objeto... ] }`.
`resend` y `DELETE` → `200` `{ "invitation": { ... } }`.

> Ninguna respuesta incluye el token ni su hash, en ningún endpoint.

Una invitación puede estar `status=PENDING` **y** `expired=true` a la vez: en ese caso la UI debe
ofrecer "reenviar", no "esperando respuesta".

### 3.2 Invitaciones (lado invitado)

| Método | Ruta | Token |
|---|---|---|
| POST | `/api/veterinary/invitations/preview` | normal |
| POST | `/api/veterinary/invitations/accept` | normal |
| POST | `/api/veterinary/invitations/decline` | normal |
| GET | `/api/veterinary/me/invitations` | normal |

Cuerpo de los tres primeros: `{ "token": "..." }`.

`preview` → `200` **incluso si la invitación ya no sirve** (es un estado que pintar, no un error).
El único error es un token inexistente → `404 INVITATION_NOT_FOUND`.

```json
{
  "invitationId": "f0b0...",
  "veterinaryId": "9a1c...",
  "veterinaryName": "Clínica El Bosque",
  "veterinaryCity": "Bogotá",
  "logoUrl": "https://.../logo.png",
  "role": "VETERINARIAN",
  "invitedByName": "Ana Dueña",
  "email": "nuevo@clinica.com",
  "emailMatchesCaller": true,
  "status": "PENDING",
  "expired": false,
  "usable": true,
  "expiresAt": "2026-11-05T10:12:00"
}
```

- `usable: true` solo si se puede aceptar **ahora mismo con esta sesión** (PENDING + vigente +
  correo coincide + correo verificado). Úsalo para habilitar el botón.
- `invitedByName` y `veterinaryName`/`logoUrl` pueden venir `null` si no se pudieron resolver; la
  pantalla debe tolerarlo.

`accept` → `200`:

```json
{
  "employeeId": "11aa...", "veterinaryId": "9a1c...",
  "veterinaryName": "Clínica El Bosque",
  "role": "VETERINARIAN",
  "licensed": false,
  "reactivated": false
}
```

- `licensed` es **informativo**: dice si la persona tiene un perfil profesional `VERIFIED`. Sirve
  para mostrar "completá tu perfil profesional" a un veterinario nuevo. El valor que de verdad
  manda en el token lo resuelve el User service al emitirlo.
- `reactivated: true` significa que la persona ya había trabajado en esa clínica y se reactivó su
  registro (conserva su historial) en vez de crear uno nuevo.
- Después de aceptar, el frontend debe **pedir un token de clínica nuevo** para esa clínica.

`decline` → `200` `{ invitationId, veterinaryId, status: "DECLINED", decidedAt }`.

`GET /api/veterinary/me/invitations` → `200`, solo `PENDING` vigentes dirigidas al correo del token:

```json
{
  "invitations": [
    {
      "invitationId": "f0b0...", "veterinaryId": "9a1c...",
      "veterinaryName": "Clínica El Bosque", "veterinaryCity": "Bogotá",
      "logoUrl": "https://.../logo.png",
      "role": "VETERINARIAN",
      "expiresAt": "2026-11-05T10:12:00"
    }
  ]
}
```

**Esta lista no trae el token**, a propósito: desde acá se ve que la invitación existe, pero para
aceptarla hace falta el enlace que llegó al correo. Es un badge de "tenés 2 invitaciones", no una
pantalla de aceptación.

### 3.3 Empleados

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/veterinary/{veterinaryId}/employees?includeInactive=false` | cualquier miembro activo |
| PATCH | `/api/veterinary/{veterinaryId}/employees/{employeeId}/role` | `STAFF_MANAGE` (+ OWNER si el rol destino es ADMIN) |
| DELETE | `/api/veterinary/{veterinaryId}/employees/{employeeId}` | `STAFF_MANAGE` |
| POST | `/api/veterinary/{veterinaryId}/leave` | cualquier miembro activo |

`GET .../employees` → `200`:

```json
{
  "employees": [
    {
      "employeeId": "11aa...", "userId": "7c21...",
      "name": "Ana Dueña", "email": "owner@clinica.com",
      "role": "OWNER", "active": true,
      "createdAt": "2026-08-01T09:00:00", "removedAt": null,
      "self": true
    }
  ]
}
```

- `name`/`email` se resuelven contra el User service y pueden venir `null`.
- `self: true` marca la fila del propio caller: no le ofrezcas "retirar" ahí (usaría `leave`).
- Nunca incluye documento de identidad ni datos clínicos. El estado de licencia de un veterinario se
  consulta con `GET /api/veterinary/{id}/employees/{employeeId}/professional-profile` (Fase 2B).
- `includeInactive=true` agrega a los retirados, con su `removedAt`.

**Reglas de quién puede tocar a quién** (cambiar rol y retirar):

- Nadie se modifica a sí mismo → `403 CANNOT_MODIFY_SELF`.
- Al `OWNER` no lo toca nadie → `403 CANNOT_MODIFY_OWNER` (para cambiarlo está la transferencia).
- Un `ADMIN` no puede modificar ni retirar a otro `ADMIN` → `403 CANNOT_MODIFY_PEER_ADMIN`.
- Solo el `OWNER` puede promover a `ADMIN` → `403 ONLY_OWNER_CAN_MANAGE_ADMINS`.
- No se puede asignar `OWNER` por este endpoint → `400 INVALID_EMPLOYEE_ROLE`.

**`DELETE` no borra**: marca `active=false` y `removedAt`, para no perder la trazabilidad de las
historias clínicas que el empleado firmó. Devuelve `200` con el empleado y `active: false`.

**`leave`** → `200` `{ employeeId, veterinaryId, removedAt }`. Funciona también si la clínica está
suspendida (irse no debería depender del estado de la clínica). El `OWNER` no puede irse:
`409 OWNER_MUST_TRANSFER_OWNERSHIP`.

### 3.4 Transferencia de propiedad

`POST /api/veterinary/{veterinaryId}/ownership-transfer`, **solo el OWNER**, body
`{ "newOwnerEmployeeId": "..." }`.

El destino debe ser un empleado **activo de la misma clínica**. En una sola transacción: el OWNER
actual pasa a `ADMIN` y el destino pasa a `OWNER`. Nunca queda la clínica sin OWNER ni con dos.

```json
{
  "veterinaryId": "9a1c...",
  "previousOwner": { "employeeId": "11aa...", "userId": "7c21...", "role": "ADMIN" },
  "newOwner":      { "employeeId": "22bb...", "userId": "8d34...", "role": "OWNER" },
  "legalRepresentativeChanged": false
}
```

> **El representante legal de la clínica NO cambia con la propiedad.** Son cosas distintas: quién
> manda en la cuenta vs. quién firma legalmente por la clínica (datos de verificación, Fase 2B). Si
> el nuevo dueño también debe ser el representante legal, hay que actualizarlo aparte con
> `PUT /api/veterinary/{id}/legal-representative`. El campo `legalRepresentativeChanged` viene
> siempre en `false` para que esto quede explícito en la respuesta.

Después de transferir, **las dos partes necesitan un token de clínica nuevo**: el anterior dueño
quedó ADMIN y el nuevo quedó OWNER.

---

## 4. Códigos de error

Formato uniforme en todos los casos: `{ timestamp, status, error, message, code }`.

| HTTP | `code` | Significado |
|---|---|---|
| 400 | `VALIDATION_ERROR` | correo de invitación ausente o mal formado; `newOwnerEmployeeId` ausente, ya es el OWNER, o es un empleado retirado |
| 400 | `INVALID_EMPLOYEE_ROLE` | rol ausente o `OWNER` |
| 403 | `ONLY_OWNER_CAN_MANAGE_ADMINS` | un ADMIN intentó invitar/reenviar/promover a ADMIN |
| 403 | `INVITATION_EMAIL_MISMATCH` | el correo del token no es el invitado |
| 403 | `EMAIL_NOT_VERIFIED` | el correo del token no está verificado |
| 403 | `NOT_CLINIC_MEMBER` | el caller ya no es empleado activo de esa clínica (token viejo) |
| 403 | `CLINIC_SUSPENDED` | la clínica está suspendida |
| 403 | `INSUFFICIENT_PERMISSION` | el rol en la base no tiene el permiso |
| 403 | `TENANT_MISMATCH` | el `vet_id` del token no es la clínica de la ruta |
| 403 | `CANNOT_MODIFY_SELF` / `CANNOT_MODIFY_OWNER` / `CANNOT_MODIFY_PEER_ADMIN` | reglas de jerarquía |
| 403 | `NOT_VETERINARY_OWNER` | la acción es exclusiva del OWNER (transferencia) |
| 404 | `INVITATION_NOT_FOUND` | token inexistente, o invitación de otra clínica |
| 404 | `EMPLOYEE_NOT_FOUND` | el empleado no existe o es de otra clínica |
| 409 | `INVITATION_ALREADY_PENDING` | ya hay una invitación vigente para ese correo |
| 409 | `INVITATION_NOT_PENDING` | ya fue aceptada, rechazada o revocada |
| 409 | `EMPLOYEE_ALREADY_EXISTS` | ese correo/usuario ya es empleado activo |
| 409 | `CLINIC_NOT_ACTIVE` | la clínica no está aprobada (invitar/reenviar/aceptar) |
| 409 | `OWNER_MUST_TRANSFER_OWNERSHIP` | el OWNER intentó irse sin transferir |
| 409 | `OWNERSHIP_TRANSFER_CONFLICT` | otra transferencia ocurrió en paralelo; reintentar |
| 410 | `INVITATION_EXPIRED` | la invitación venció (quien invitó puede reenviarla) |

`404 INVITATION_NOT_FOUND` y `404 EMPLOYEE_NOT_FOUND` son deliberadamente iguales para "no existe" y
"es de otra clínica": no revelamos que un id existe en otra clínica.

---

## 5. Eventos (para Notifications)

Sobre común de `CONTRATOS_COMPARTIDOS.md §4`, topic **`veterinary-staff`**. Todos se publican
**después del commit** de la transacción, así que si la operación falla no sale ningún evento.

| Evento | Destinatario | Payload |
|---|---|---|
| `EMPLOYEE_INVITED` | `recipient.email` = correo del invitado (`userId` es `null`: puede no tener cuenta) | `veterinaryId`, `veterinaryName`, `role`, **`acceptUrl`**, `expiresAt` |
| `EMPLOYEE_JOINED` | `userId` = OWNER activo | `veterinaryName`, `employeeId`, `employeeEmail`, `employeeName` (puede ser `null`), `role` |
| `EMPLOYEE_ROLE_UPDATED` | `userId` = el empleado afectado | `employeeId`, `oldRole`, `newRole` |
| `EMPLOYEE_DEACTIVATED` | ver `leftVoluntarily` | `employeeId`, `role`, `removedAt`, **`leftVoluntarily`** |
| `OWNERSHIP_TRANSFERRED` | **dos eventos**, uno a cada parte | `veterinaryName`, `previousOwnerEmployeeId`, `newOwnerEmployeeId`, **`recipientIsNewOwner`** |

Detalles que importan para redactar los mensajes:

- **`EMPLOYEE_INVITED`**: `acceptUrl` ya trae el token. Es el único lugar donde el token existe en
  claro — no lo guardamos ni lo logueamos. El correo es la única vía para aceptar. Se publica otra
  vez en cada `resend`, con un `acceptUrl` nuevo que invalida el anterior.
- **`EMPLOYEE_DEACTIVATED`** reutiliza un solo tipo para dos situaciones distintas, y la bandera
  decide el texto:
  - `leftVoluntarily: false` → **la clínica retiró** al empleado. El destinatario (`userId`) es **el
    empleado retirado**: "fuiste retirado del equipo de X".
  - `leftVoluntarily: true` → **el empleado se fue solo**. El destinatario (`userId`) es **el OWNER
    activo**: "Y dejó el equipo de X".
  No hay un `EMPLOYEE_LEFT`; la bandera es la que distingue el caso.
- **`OWNERSHIP_TRANSFERRED`** se publica dos veces, una por parte, con el mismo payload salvo
  `recipientIsNewOwner`: `true` para el nuevo dueño ("ahora sos el dueño de X"), `false` para el
  anterior ("transferiste X a …"). Así Notifications no necesita comparar ids para elegir el texto.
- Rechazar una invitación **no** publica evento (la clínica lo ve en el listado, la fila queda
  `DECLINED`).

---

## 6. Rutas deprecadas — qué tiene que migrar el frontend

| Ruta vieja | Reemplazo | Estado |
|---|---|---|
| `PATCH /api/veterinary/employees/{employeeId}/role` | `PATCH /api/veterinary/{veterinaryId}/employees/{employeeId}/role` | funciona, `@Deprecated` |
| `PATCH /api/veterinary/employees/{employeeId}/deactivate` | `DELETE /api/veterinary/{veterinaryId}/employees/{employeeId}` | funciona, `@Deprecated` |
| `PATCH /api/veterinary/employees/{employeeId}/activate` | — (sin ruta anidada todavía) | funciona, `@Deprecated` |
| `POST /api/veterinary/employees` | invitaciones (§3.1 + §3.2) | **se elimina antes del despliegue** |

Los tres `PATCH` siguen funcionando igual y aplican exactamente las mismas reglas que las rutas
nuevas: como no llevan la clínica en la ruta, la toman del token, así que un `employeeId` de otra
clínica responde `404 EMPLOYEE_NOT_FOUND`.

**`POST /api/veterinary/employees` hay que dejar de usarlo.** Agrega a una persona al equipo por
`userId` **sin su consentimiento**: nunca acepta nada, simplemente aparece como empleada. Lo
reemplaza el flujo de invitación, donde la persona acepta desde su propia cuenta y con su correo
verificado. Ese endpoint se elimina antes del despliegue a producción, así que cualquier pantalla
que lo use hay que migrarla ya.
