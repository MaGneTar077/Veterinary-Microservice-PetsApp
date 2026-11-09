# Error: Veterinary no responde las rutas internas que usa Medical

Written for: equipo del microservicio de Veterinary.

## 1. Resumen

Medical no puede verificar los permisos de una clínica sobre una mascota porque las rutas internas que lo permiten responden **404 `NOT_FOUND`** en el servicio desplegado. Por eso todas las rutas de clínica de Medical fallan con **503** (`EXTERNAL_SERVICE_UNAVAILABLE`, "Servicio interno respondió 404").

El 404 es el mismo que devuelve una ruta que no existe, así que las rutas no están implementadas en la versión desplegada.

Esto coincide con `MYANIMALOG_VET_BACKEND_CONTRATO.md`, que marca estas rutas con 📋 (planificadas):

- `GET /internal/veterinaries/{id}/patients/{petId}/access` (VET-29)
- `GET /internal/veterinaries/{id}/summary`

## 2. Qué probamos

Servicio: `https://veterinary-microservice-petsapp-bjo5vkqo4q-uc.a.run.app` (revisión activa `veterinary-microservice-petsapp-00016-plc`, generación 16).

Todas las pruebas con la clave del secreto `internal-api-key` en el header `X-Internal-Api-Key`:

| Llamada | Respuesta |
|---|---|
| `GET /internal/veterinaries/43c3bdee-5668-4481-8f75-5ad88148b7e0/patients/00000000-0000-4000-8000-000000000301/access?userId=00000000-0000-4000-8000-000000000001` | **404** `NOT_FOUND` |
| `GET /internal/veterinaries/43c3bdee-5668-4481-8f75-5ad88148b7e0/summary` | **404** `NOT_FOUND` |
| `GET /internal/ruta-que-no-existe` (control) | **404** `NOT_FOUND` |

La respuesta de las dos rutas reales es idéntica a la de la ruta inventada. Eso indica que no están registradas.

Con una clave falsa, Veterinary sí responde **401** `UNAUTHENTICATED`, así que la validación de `X-Internal-Api-Key` sí existe. El problema es solo la ruta.

## 3. Qué espera Medical

### 3.1 `GET /internal/veterinaries/{veterinaryId}/patients/{petId}/access?userId={userId}`

Medical lo llama en cada petición de clínica (crear visita, peso, laboratorio, cirugía, tratamientos, vacunas, documentos, lectura de historia). Usa la respuesta para decidir si deja pasar la petición.

Respuesta esperada (200), según `CONTRATOS_COMPARTIDOS.md` §3:

```json
{
  "hasAccess": true,
  "veterinaryStatus": "ACTIVE",
  "employeeId": "uuid",
  "role": "VETERINARIAN",
  "licensed": true,
  "permissions": ["CLINICAL_READ", "CLINICAL_WRITE", "NURSING_WRITE", "DOCUMENT_UPLOAD"]
}
```

Cómo lo usa Medical:

| Operación de Medical | Permiso que exige en `permissions` |
|---|---|
| Leer historia clínica | `CLINICAL_READ` |
| Escribir visitas, cirugías, laboratorios, tratamientos, medicación | `CLINICAL_WRITE` |
| Registrar peso, signos, aplicar vacuna | `NURSING_WRITE` |
| Subir documentos | `DOCUMENT_UPLOAD` |

Si `hasAccess` es `false` o falta el permiso, Medical responde **403**. Si la llamada falla, Medical responde **503**.

Cuando la mascota no está compartida con la clínica, o el usuario no es empleado activo, responde `hasAccess: false`, no 404.

Medical cachea la respuesta 60 segundos por `(veterinaryId, petId, userId)`.

### 3.2 `GET /internal/veterinaries/{veterinaryId}/summary`

Medical lo usa para poner el nombre de la clínica en los eventos de notificación. Respuesta esperada (200):

```json
{
  "id": "uuid",
  "name": "Clínica ejemplo",
  "timezone": "America/Bogota"
}
```

Si falla, Medical omite el nombre y sigue publicando el evento. Igual conviene que responda.

## 4. Qué hay que hacer

1. Implementar `GET /internal/veterinaries/{id}/patients/{petId}/access?userId=` con la forma de la sección 3.1, aplicando la matriz de permisos de §3.2 del contrato.
2. Implementar `GET /internal/veterinaries/{id}/summary` con la forma de la sección 3.2.
3. Mantener la validación de `X-Internal-Api-Key` (ya funciona).
4. Confirmar que la clínica y la mascota de prueba existan en la base de Veterinary. Medical ya envía el `vet_id` del token del empleado, así que la prueba debe usar ese mismo ID.

## 5. Cómo comprobar que quedó listo

Con la clave correcta, sin mostrarla en el chat:

```bash
curl -H "X-Internal-Api-Key: <clave>" \
  "https://veterinary-microservice-petsapp-bjo5vkqo4q-uc.a.run.app/internal/veterinaries/<vetId>/patients/<petId>/access?userId=<userId>"
# esperado: 200 con hasAccess, permissions, etc.

curl -H "X-Internal-Api-Key: <clave>" \
  "https://veterinary-microservice-petsapp-bjo5vkqo4q-uc.a.run.app/internal/veterinaries/<vetId>/summary"
# esperado: 200 con id, name, timezone
```

Con una clave falsa ambas deben responder 401. Con una clínica o mascota que no existan, `access` debe responder 200 con `hasAccess: false`, no 404.

Cuando estas dos rutas respondan así, Medical podrá validar las peticiones de clínica sin cambios.
