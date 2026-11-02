# Despliegue en Cloud Run — `veterinary-service`

Guía operativa del despliegue. No cambia nada de la lógica del servicio: el único ajuste de
configuración que trajo este trabajo fue `server.port`, que ahora respeta la variable `PORT` que
Cloud Run inyecta.

---

## 1. Imagen

```bash
# Build local
docker build -t myanimalog-veterinary .

# Artifact Registry (ajustá proyecto/región/repo)
docker tag myanimalog-veterinary \
  us-central1-docker.pkg.dev/<PROYECTO>/<REPO>/veterinary-service:latest
docker push us-central1-docker.pkg.dev/<PROYECTO>/<REPO>/veterinary-service:latest
```

- **Multi-etapa**: etapa 1 `maven:3.9-eclipse-temurin-17` compila; etapa 2 `eclipse-temurin:17-jre-jammy`
  solo lleva el JRE y el jar. La imagen final no tiene Maven, ni el repositorio `~/.m2`, ni el código
  fuente.
- **Jar**: `Veterinary-0.0.1-SNAPSHOT.jar` (`artifactId` + `version` del `pom.xml`), copiado como
  `app.jar`.
- **Usuario no root**: `appuser:appgroup` (system, sin shell de login). El `WORKDIR /app` le queda
  en propiedad.
- **Tests en el build**: se saltan con `-Dmaven.test.skip=true` (no `-DskipTests`). No es por
  comodidad: los tests de este servicio levantan el contexto de Spring y necesitan la base real de
  Supabase, que no existe dentro del build. **La suite se corre antes de construir la imagen**
  (`./mvnw test`, 919/919), no durante.
- **Memoria JVM**: `-XX:MaxRAMPercentage=75`, sin `-Xmx` fijo, para que el heap se calcule desde la
  memoria que Cloud Run le dé al contenedor. Si subís el límite de memoria del servicio, la JVM lo
  aprovecha sin reconstruir la imagen.

### Puerto del contenedor en Cloud Run: **8082**

`server.port` quedó como `${PORT:${SERVER_PORT:8082}}`:

1. En Cloud Run manda `PORT`, que Cloud Run inyecta con el valor del campo "Puerto del contenedor".
2. En local sigue valiendo `SERVER_PORT` del `.env`.
3. Si no hay ninguna de las dos, 8082.

Poné **8082** en "Puerto del contenedor" para que coincida con el `EXPOSE` del Dockerfile y con el
puerto local. Si lo dejás en 8080 (el default de Cloud Run) **también funciona**, porque la app
obedece a `PORT`; lo único que no debe pasar es fijar `SERVER_PORT` en Cloud Run con un valor
distinto al del campo (ganaría `PORT`, pero el `.env`/variable quedaría mintiendo).

### Nada de secretos dentro de la imagen

`.dockerignore` excluye `.env`, `.env.*`, `*.env`, `*.pem`, `*.key`, `*.p12`, `*.jks`, `*.keystore`,
`keys/`, `secrets/`, `credentials*.json`, `sa-*.json`, `service-account*.json`,
`application-local.yml` y `application-dev.yml`, más `.git/`, `target/`, `.idea/`, `postman/`,
`scripts/`, `db/` y los `*.md`.

Además, **este servicio no lee ningún archivo de secretos** — lo verifiqué: no hay
`ClassPathResource`, `FileInputStream`, `new File(...)` ni uso de `GOOGLE_APPLICATION_CREDENTIALS`
en `src/main`. Toda la configuración entra por variables de entorno. `application.yaml` importa
`optional:file:.env`, y el `optional:` hace que la ausencia del archivo en la imagen sea inofensiva.

Cuando se prenda Pub/Sub (`EVENTS_ENABLED=true`), las credenciales **no** son un archivo: en Cloud
Run se usa la identidad de la cuenta de servicio del servicio (ADC). Basta con darle el rol
`roles/pubsub.publisher` a esa cuenta de servicio; no hay que montar ningún JSON.

---

## 2. Variables de entorno

No hay perfil `prod`: el único `application.yaml` **es** la configuración de producción (el perfil
`test` vive solo en el classpath de tests). Por eso `SPRING_PROFILES_ACTIVE` **no hace falta** —
ponerlo en `prod` no carga nada extra, solo confunde. Si querés dejarlo explícito, usá
`SPRING_PROFILES_ACTIVE=default`.

Leyenda de **Secreto**: ✅ = va en Secret Manager y se referencia desde Cloud Run (nunca como texto
plano en la consola ni en un `.yaml` versionado).

| Variable | Obligatoria | Default | Secreto | Qué pasa si falta |
|---|:-:|---|:-:|---|
| `DB_URL` | **Sí** | — | ✅ (lleva el host del proyecto) | La app **no arranca**: Hikari no puede resolver `spring.datasource.url`. Usar el pooler transaccional de Supabase (puerto 6543). |
| `DB_USERNAME` | **Sí** | — | ✅ | No arranca. |
| `DB_PASSWORD` | **Sí** | — | ✅ | No arranca. |
| `JWT_JWKS_URI` | **Sí** | — | No (es una URL pública) | **No arranca**: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` no se puede resolver. Valor: `{USER_SERVICE_URL}/.well-known/jwks.json`. |
| `INTERNAL_API_KEY` | **Sí** | — | ✅ | No arranca (`InternalApiKeyFilter` la pide en el constructor). Debe ser **idéntica** en todos los servicios. |
| `SUPABASE_URL` | **Sí** | — | No | No arranca (`SupabaseStorageAdapter`). |
| `SUPABASE_KEY` | **Sí** | — | ✅ | No arranca. **Tiene que ser la `service_role`**, no la `anon`: el adaptador sube a un bucket privado y genera URLs firmadas, y con la `anon` esas operaciones fallan con 403 → `503 EXTERNAL_SERVICE_UNAVAILABLE`. (La tabla de `CLAUDE.md` dice "anon key"; está mal y hay que corregirla — la sección "Almacenamiento de archivos" del mismo archivo sí dice service role.) |
| `APP_BASE_URL` | **Sí** | — | No | No arranca. Base pública usada para construir el `inviteLink` de clientes. |
| `PORT` | No (la inyecta Cloud Run) | `SERVER_PORT`, luego `8082` | No | Cloud Run siempre la manda; en local no se usa. |
| `SERVER_PORT` | No | `8082` | No | Solo para local. **No la pongas en Cloud Run.** |
| `EVENTS_ENABLED` | No | `true` | No | Con el default `true` y sin `GCP_PROJECT_ID` la app **no arranca**. Para el piloto: **`false`**. |
| `GCP_PROJECT_ID` | Solo si `EVENTS_ENABLED=true` | `` | No | Con eventos prendidos y sin esto: `IllegalArgumentException: The project ID can't be null or empty` al arrancar. |
| `INVITATION_BASE_URL` | Sí para invitar personal | `` (vacío) | No | La app arranca, pero el enlace del correo sale como `?token=...` sin dominio: **inservible**. Es la ruta del frontend que recibe el token (ver `FASE_3_PERSONAL.md §2`). |
| `USER_SERVICE_URL` | Recomendada | `` (vacío) | No | Arranca igual. Sin ella, las llamadas al User service fallan y los campos que dependen de ellas caen a su fallback: `account` → `{ id }` en el detalle del admin, y `name`/`email` → `null` en la lista de empleados. Nada se cae. |
| `SUPPORT_EMAIL` | No | `` (vacío) | No | El evento `VERIFICATION_SUBMITTED` sale con `recipient.email` vacío; con `EVENTS_ENABLED=false` da igual. |
| `AI_VERIFICATION_ENABLED` | No | `false` | No | Dejar en **`false`**. En `false` se registra `NoOpIaVerificationAdapter`, `ai_status` queda `NOT_RUN` y `POST .../rerun-ai` responde `409 AI_VERIFICATION_DISABLED`. |
| `IA_SERVICE_URL` | Solo si `AI_VERIFICATION_ENABLED=true` | `` | No | Con la IA apagada no se usa. |
| `LEGAL_DATA_POLICY_VERSION` | No | `2026-10-v1` | No | Si no coincide con la versión que manda el frontend al registrar: `409 CONSENT_VERSION_OUTDATED`. |
| `LEGAL_TERMS_VERSION` | No | `2026-10-v1` | No | Igual que la anterior. |
| `RUES_URL` | No | `https://www.rues.org.co/` | No | Solo cambia el `registryLookupUrl` que ve el admin. |
| `COMVEZCOL_URL` | No | `https://consejoprofesionalmvz.gov.co/consulta-de-profesionales/` | No | Igual que la anterior. |

### Valores que hoy están fijos en `application.yaml` (no son variables)

Estos cuatro **no** se leen del entorno, están escritos en el archivo. Si hiciera falta cambiarlos
en Cloud Run sin tocar el código, se pueden sobrescribir con el nombre en mayúsculas y guiones bajos
(Spring Boot mapea `APP_SECURITY_ISSUER` → `app.security.issuer`), pero lo normal es dejarlos así:

| Propiedad | Valor fijo | Variable de override |
|---|---|---|
| `app.security.issuer` | `myanimalog-user-service` | `APP_SECURITY_ISSUER` |
| `app.security.audience` | `myanimalog-api` | `APP_SECURITY_AUDIENCE` |
| `app.storage.logos-bucket` | `veterinary-logos` (bucket público) | `APP_STORAGE_LOGOS_BUCKET` |
| `app.storage.verification-bucket` | `verification-documents` (bucket privado) | `APP_STORAGE_VERIFICATION_BUCKET` |

Los dos buckets se crean a mano en el panel de Supabase; el servicio no los crea.

### `JWT_ACCEPT_LEGACY` no existe en este servicio

La pediste en la lista, pero **no hay ninguna referencia** a esa variable (ni a `accept-legacy`) en
el código ni en la configuración. Este servicio solo valida JWT por JWKS (`NimbusJwtDecoder` +
`JwtTokenValidator` con `iss`/`aud`); el `jjwt` y el `JWT_SECRET` se eliminaron en VET-05/06/07.
Si existe, es del User service. **No la configures acá**: no haría nada.

### Arranque mínimo sin Pub/Sub ni IA — confirmado

Con `EVENTS_ENABLED=false` y `AI_VERIFICATION_ENABLED=false` (o simplemente sin definir la segunda):

- `spring.cloud.gcp.core.enabled` y `spring.cloud.gcp.pubsub.enabled` quedan en `false`, así que la
  autoconfiguración de GCP **se salta por completo** y nunca pide credenciales ni `GCP_PROJECT_ID`.
  En su lugar se registra `NoOpEventPublisher` (solo loguea en DEBUG).
- La IA registra `NoOpIaVerificationAdapter`, que no hace ninguna llamada de red.

Las únicas dependencias externas que quedan para arrancar son **Postgres (Supabase)** y que
`JWT_JWKS_URI` esté definida — el JWKS se baja en la primera validación de token, no al arrancar,
así que si el User service todavía no está arriba el servicio igual arranca y responde
`/actuator/health`.

### Comando de ejemplo

```bash
gcloud run deploy veterinary-service \
  --image us-central1-docker.pkg.dev/<PROYECTO>/<REPO>/veterinary-service:latest \
  --region us-central1 \
  --port 8082 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --service-account veterinary-service@<PROYECTO>.iam.gserviceaccount.com \
  --set-env-vars EVENTS_ENABLED=false,AI_VERIFICATION_ENABLED=false \
  --set-env-vars SUPABASE_URL=https://<ref>.supabase.co \
  --set-env-vars APP_BASE_URL=https://app.myanimalog.com \
  --set-env-vars INVITATION_BASE_URL=https://app.myanimalog.com/<RUTA-DEL-FRONT> \
  --set-env-vars USER_SERVICE_URL=https://user-service-xxxx.run.app \
  --set-env-vars JWT_JWKS_URI=https://user-service-xxxx.run.app/.well-known/jwks.json \
  --set-env-vars SUPPORT_EMAIL=soporte@myanimalog.com \
  --set-secrets DB_URL=vet-db-url:latest \
  --set-secrets DB_USERNAME=vet-db-username:latest \
  --set-secrets DB_PASSWORD=vet-db-password:latest \
  --set-secrets SUPABASE_KEY=supabase-service-role:latest \
  --set-secrets INTERNAL_API_KEY=internal-api-key:latest
```

Healthcheck: `GET /actuator/health` (abierto, sin token). `/actuator/info` también está expuesto;
el resto de actuator, no.

---

## 3. Base de datos: scripts que tienen que estar aplicados

El esquema se administra a mano en el SQL Editor de Supabase (no hay Flyway). Antes de apuntar el
servicio a una base, verificá que estén los **siete**:

| Script | Qué agrega | Ejecutado |
|---|---|---|
| `001_clinic_status_and_profile.sql` | `veterinary.status` + perfil/legal + settings por clínica | 2026-10-08 |
| `002_employee_roles_and_ownership.sql` | CHECK de roles, `ux_one_owner_per_vet`, `ux_employee_user_per_vet` | 2026-10-08 |
| `003_enable_rls.sql` | RLS en las 4 tablas originales | 2026-10-08 |
| `004_verification.sql` | `legal_consents`, `professional_profiles`, `verification_requests`, `verification_documents` + representante legal y logo | 2026-10-15 |
| `005_original_file_name.sql` | `verification_documents.original_file_name` | confirmado aplicado |
| `006_admin_review.sql` | `rejection_reason_code`, `suspended_at`, `suspension_reason` | 2026-10-26 |
| `007_staff_invitations.sql` | `employee_invitations` + `veterinary_employee.removed_at` | 2026-10-29 |

Verificación rápida en la base de destino:

```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
-- Esperado: employee_invitations, legal_consents, professional_profiles,
-- user_veterinary_link, verification_documents, verification_requests,
-- veterinary, veterinary_employee, veterinary_subscription

SELECT column_name FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'veterinary_employee';
-- Esperado: incluye removed_at
```

`spring.jpa.hibernate.ddl-auto` está en `none`: **el servicio no crea ni altera tablas**. Si falta
un script, el síntoma es un error de SQL en la primera petición que toque esa columna, no un fallo
al arrancar.

Los dos buckets de Supabase Storage también tienen que existir: `veterinary-logos` (público) y
`verification-documents` (privado).

---

## 4. Qué espera este servicio de su alrededor

### Del gateway

- Enruta **solo** `/api/veterinary/**` hacia este servicio. Por eso las rutas de admin viven en
  `/api/veterinary/admin/**` y no en un `/admin/**` genérico, que nunca llegaría.
- **`/internal/**` NUNCA se expone por el gateway.** Son rutas de servicio a servicio, autenticadas
  con el header `X-Internal-Api-Key` y no con un JWT de usuario. Si el gateway las publicara,
  cualquiera con la llave (o probando) hablaría directo con el núcleo del servicio. Son dos:
  `GET /internal/veterinaries/{veterinaryId}/members/{userId}` y
  `POST /internal/jobs/verification-documents/cleanup`.
- `/swagger-ui.html`, `/api-docs/**`, `/actuator/health` y `/actuator/info` están abiertos en el
  servicio. Decidí a nivel de gateway si los publicás; en producción lo razonable es **no** publicar
  Swagger.
- Debe pasar el header `Authorization` tal cual (el servicio valida el JWT por su cuenta).

### Del User service

1. **JWKS**: `GET {USER_SERVICE_URL}/.well-known/jwks.json`, que es lo que apunta `JWT_JWKS_URI`.
   Los tokens tienen que venir con `iss=myanimalog-user-service` y `aud=myanimalog-api`.
2. **Tokens de clínica**: `POST {USER_SERVICE_URL}/auth/context/veterinary/{veterinaryId}` con los
   claims `ctx=VETERINARY`, `vet_id`, `employee_id`, `vet_role`, `vet_licensed`, `vet_status`. Para
   decidir si puede emitirlo, el User service consume
   `GET /internal/veterinaries/{veterinaryId}/members/{userId}` de este servicio (con
   `X-Internal-Api-Key`).
   > Recordatorio de la Fase 3: este servicio **ya no confía** en `vet_role`/`vet_status` del token
   > para autorizar (los relee de la base en cada petición). Los claims siguen siendo necesarios
   > para identificar el contexto de clínica y el tenant, nada más.
3. **Detalle de usuarios**: `GET {USER_SERVICE_URL}/internal/users/{id}` → `{ id, email, emailVerified, name }`.
   Sin esto, los nombres/correos en el panel de admin y en la lista de empleados salen `null`
   (nunca tumba la respuesta).
4. **Pendiente del lado de ellos**: un endpoint en lote (`POST /internal/users/batch`) para no hacer
   N llamadas por página. Hoy la lista de empleados hace una llamada por empleado.

### De Notifications

Con `EVENTS_ENABLED=false` **no sale ningún evento**, así que no se envía ningún correo — incluido
el de invitación de personal. Para que las invitaciones lleguen por correo hay que: crear los cinco
topics (`gcloud pubsub topics create veterinary-clinic veterinary-verification veterinary-staff
veterinary-patients veterinary-subscription`), darle `roles/pubsub.publisher` a la cuenta de
servicio, poner `EVENTS_ENABLED=true` + `GCP_PROJECT_ID`, y que Notifications consuma
`veterinary-staff`. El contrato de los payloads está en `FASE_3_PERSONAL.md §5`.

---

## 5. Antes de la clínica piloto

- [ ] **Eliminar `POST /api/veterinary/employees`** (`CreateEmployee`, `@Deprecated(forRemoval = true)`).
      Agrega a una persona al equipo por `userId` **sin su consentimiento**; lo reemplaza el flujo de
      invitaciones de la Fase 3. Hay que confirmar con el frontend que ninguna pantalla lo usa y
      borrar controller + use case + service + DTOs + tests. Mientras exista, cualquiera con
      `STAFF_MANAGE` puede meter a un usuario arbitrario a su clínica sin que esa persona acepte.
- [ ] Confirmar la ruta del frontend para `INVITATION_BASE_URL`.
- [ ] Decidir si el gateway publica Swagger (recomendado: no).
- [ ] Corregir en `CLAUDE.md` la fila de `SUPABASE_KEY`, que dice "anon key" cuando tiene que ser la
      `service_role`.
- [ ] Revisar los índices de búsqueda del panel admin (`pg_trgm`) si se espera volumen — está en la
      deuda técnica de `CLAUDE.md`.
