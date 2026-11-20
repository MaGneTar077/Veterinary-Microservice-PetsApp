# Lo que Calendar necesita de Veterinary

> Leer junto con `CONTRATOS_COMPARTIDOS.md` (autenticación entre servicios, formato de error) y `CALENDAR_SERVICE_CHANGES.md` (CAL-04 y "Dependencias de Veterinary").
> Estado verificado el 19 de noviembre contra `https://veterinary-microservice-petsapp-20369076707.us-central1.run.app`, con la clave interna.

## Estado (Fase 5, 2026-11-20)

Los 5 pedidos están resueltos. Detalle completo, ejemplos y reglas en `docs/FASE_5_AGENDA.md`.

| # | Pedido | Estado | Ruta final |
|---|---|---|---|
| 1 | Datos de la clínica | ✅ Resuelto | `GET /internal/veterinaries/{veterinaryId}` |
| 2 | Catálogo de servicios | ✅ Resuelto | `GET /internal/veterinaries/{veterinaryId}/services?activeOnly=&bookableOnline=` (filtros opcionales, combinados con AND) y `GET /internal/veterinaries/{veterinaryId}/services/{serviceId}` (incluye inactivos) |
| 3 | Horarios y bloqueos | ✅ Resuelto | `GET /internal/veterinaries/{veterinaryId}/schedule-rules?from=&to=&employeeId=` |
| 4 | Equipo de la clínica (listado) | ✅ Resuelto | `GET /internal/veterinaries/{veterinaryId}/members?includeInactive=` |
| 5 | Mascota compartida | ✅ Resuelto | `GET /internal/veterinaries/{veterinaryId}/patients/{petId}/shared` |

Diferencias con lo pedido originalmente, por si Calendar ya tenía código contra el borrador: `status`
de la clínica trae los 6 valores reales (no `ACTIVE|INACTIVE|PENDING`); `dayOfWeek` confirmado ISO
(1 = lunes); precios con decimales (`NUMERIC`, no enteros).

## Por qué

Calendar agenda citas entre dueños y clínicas. Para eso necesita leer de Veterinary la clínica, sus servicios, sus horarios, su equipo y si una mascota está compartida con ella. Hoy esa información sale de datos de prueba dentro de Calendar, así que las citas con datos reales fallan. Estos endpoints permiten quitar esos datos de prueba.

## Autenticación

Todos son internos. Calendar envía el header `X-Internal-Api-Key` (comparación en tiempo constante). Sin clave o con clave incorrecta: `401` con el formato de error de `CONTRATOS_COMPARTIDOS.md`. Calendar espera responder en menos de 3 s; si la instancia está en escala a cero, el arranque en frío tarda unos 2 s, así que conviene tener en cuenta ese tiempo.

## Pedidos

### 1. Datos de la clínica

`GET /internal/veterinaries/{veterinaryId}`

Respuesta `200`:

```json
{
  "id": "uuid",
  "name": "string",
  "status": "ACTIVE | INACTIVE | PENDING",
  "allowOnlineBooking": true,
  "bookingRequiresConfirmation": true,
  "cancellationMinHours": 24,
  "timezone": "America/Bogota",
  "address": "string | null",
  "phone": "string | null"
}
```

- `404` si la clínica no existe.
- **Estado actual:** `404` (verificado).
- **Para qué:** decidir si Calendar acepta reservas en línea, cuándo se puede cancelar y en qué zona horaria se muestran las horas.

### 2. Catálogo de servicios

`GET /internal/veterinaries/{veterinaryId}/services`

Respuesta `200`, una lista:

```json
[
  {
    "id": "uuid",
    "name": "Consulta general",
    "durationMinutes": 30,
    "price": 40000,
    "active": true,
    "bookableOnline": true,
    "requiresLicense": false,
    "employeeIds": ["uuid"]
  }
]
```

- Para un servicio concreto también hace falta `GET /internal/veterinaries/{veterinaryId}/services/{serviceId}` (mismo cuerpo, o `404`).
- **Estado actual:** `404` (verificado).
- **Para qué:** el snapshot de la cita (nombre, duración, precio) y la elegibilidad de empleados (`employeeIds`). Sin esto, una cita con un servicio real no puede crearse.

### 3. Horarios y bloqueos

`GET /internal/veterinaries/{veterinaryId}/schedule-rules?from={ISO}&to={ISO}&employeeId={opcional}`

Respuesta `200`:

```json
{
  "timezone": "America/Bogota",
  "businessHours": [{ "dayOfWeek": 1, "opensAt": "08:00", "closesAt": "17:00" }],
  "employeeHours": [{ "employeeId": "uuid", "dayOfWeek": 1, "opensAt": "09:00", "closesAt": "13:00" }],
  "blocks": [{ "employeeId": null, "startsAt": "ISO", "endsAt": "ISO", "reason": "string" }]
}
```

- `dayOfWeek`: 1 = lunes … 7 = domingo. Confirmar si la convención es esa.
- `employeeId: null` en un bloque significa toda la clínica.
- **Estado actual:** `404` (verificado).
- **Para qué:** calcular las franjas libres. Calendar hoy usa lunes a sábado de 8 a 5 fijo en código, así que no respeta los horarios reales.

### 4. Equipo de la clínica (listado)

`GET /internal/veterinaries/{veterinaryId}/members`

Respuesta `200`, una lista:

```json
[
  {
    "employeeId": "uuid",
    "userId": "uuid",
    "name": "string",
    "role": "OWNER | VETERINARIAN | ...",
    "active": true,
    "licensed": true,
    "permissions": ["APPOINTMENT_MANAGE"]
  }
]
```

- **Estado actual:** no hay listado. El endpoint por usuario sí responde `200` (`GET /internal/veterinaries/{id}/members/{userId}`), pero Calendar necesita la lista completa para la agenda.
- **Para qué:** mostrar el nombre del empleado en la agenda y asignar empleados elegibles.

### 5. Mascota compartida con la clínica

`GET /internal/veterinaries/{veterinaryId}/patients/{petId}/shared`

Respuesta `200`:

```json
{ "shared": true }
```

- **Estado actual:** `404` (verificado). Calendar usa hoy un atajo: consulta `patients/{petId}/access` con un empleado activo y toma `hasAccess` como "compartida". Eso es provisional y depende de que ese empleado siga activo.
- **Para qué:** que el dueño solo reserve con clínicas que tienen compartida su mascota, sin depender de un empleado concreto.

## Lo que ya funciona (no hace falta cambiarlo)

- `GET /internal/veterinaries/{id}/members/{userId}`: `200`.
- `GET /internal/veterinaries/{id}/patients/{petId}/access?userId=`: `200`, con `hasAccess`, `employeeId`, `role`, `licensed` y `permissions`.

## Reportado por el panel (fuera de Calendar)

- `GET /admin/veterinaries` sin filtro responde `500` en Cloud Run.
- `GET /api/veterinary/{id}/services`, `business-hours` y `schedule-blocks` responden `404` para el panel. Son los mismos datos que pedimos arriba, así que conviene resolverlos juntos.

## Prioridad sugerida

1. Servicios (pedido 2): sin esto no se pueden crear citas reales.
2. Horarios (pedido 3): sin esto las franjas libres son incorrectas.
3. Datos de la clínica (pedido 1).
4. Listado del equipo (pedido 4).
5. Mascota compartida (pedido 5): hoy hay un atajo que funciona.

## Preguntas abiertas

- ¿La convención de `dayOfWeek` es 1 = lunes?
- ¿El horario por profesional (`/employees/{id}/schedule`) va en la misma respuesta de `schedule-rules` o es otro endpoint?
- ¿Los precios son en pesos enteros o con decimales?
