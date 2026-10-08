# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`veterinary-service` is a Spring Boot 3.5 / Java 17 microservice (part of the larger "Mascotas" / MyAnimaLog platform) that owns veterinary clinics: registration/verification status, suspension/reactivation, staff (employees) and roles, invite-based linking of app users to a clinic, and subscription plans. It talks to a Postgres database hosted on Supabase and is designed to run standalone on port 8082 (configurable).

## Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=RegisterVeterinaryServiceTest

# Run a single test method
./mvnw test -Dtest=RegisterVeterinaryServiceTest#shouldRegisterVeterinarySuccessfully

# Run the app locally
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

Tests (including `VeterinaryApplicationTests#contextLoads`) boot the full Spring context, which requires a reachable database. There's no H2/test profile — `application.yaml` loads credentials from the `.env` file at the project root (via `spring-dotenv`, `spring.config.import: optional:file:.env`) and connects to the real Supabase Postgres instance. A valid `.env` must exist locally before running `./mvnw test`.

Swagger/OpenAPI UI is available at `/swagger-ui.html` (raw spec at `/api-docs`) when the app is running. Actuator exposes only `/actuator/health` and `/actuator/info`.

### Variables de entorno (`.env`)

| Variable | Requerida | Default | Uso |
|---|---|---|---|
| `DB_URL` | Sí | — | JDBC de Postgres (Supabase, pooler transaccional puerto 6543) |
| `DB_USERNAME` | Sí | — | Usuario de la base |
| `DB_PASSWORD` | Sí | — | Password de la base |
| `SERVER_PORT` | No | `8082` | Puerto HTTP del servicio |
| `SUPABASE_URL` | Sí | — | URL del proyecto Supabase |
| `SUPABASE_KEY` | Sí | — | Anon key de Supabase |
| `APP_BASE_URL` | Sí | — | Base URL pública, usada para construir `inviteLink` |
| `EVENTS_ENABLED` | No | `true` | Activa/desactiva la publicación de eventos a Pub/Sub — ver sección Eventos |
| `GCP_PROJECT_ID` | Solo si `EVENTS_ENABLED=true` contra un proyecto real | — | Proyecto de Google Cloud para Pub/Sub |
| `JWT_JWKS_URI` | Sí | — | URL del JWKS del User service (`GET {USER_SERVICE_URL}/.well-known/jwks.json`), usada por `NimbusJwtDecoder` |
| `INTERNAL_API_KEY` | Sí | — | Llave compartida para `X-Internal-Api-Key` en rutas `/internal/**` (debe ser idéntica en todos los servicios) |

**Importante para desarrollo local**: con `EVENTS_ENABLED` sin definir (default `true`) y sin `GCP_PROJECT_ID`, la app **no arranca** — la autoconfiguración de `spring-cloud-gcp-starter-pubsub` intenta construir un `PublisherFactory` real y falla con `IllegalArgumentException: The project ID can't be null or empty`. En local, deja `EVENTS_ENABLED=false` en `.env` (ya está así) a menos que de verdad vayas a publicar a un proyecto GCP real.

## Architecture

This service follows **hexagonal architecture (ports & adapters)**, organized by **business module** (`clinic`, `staff`, `subscription`, `patients`), strictly one-feature-per-slice within each module: every use case gets its own request/response DTO pair, inbound port, service, and controller.

```
domain/
  <module>/model/        domain models for that module (plain Lombok @Data/@Builder, no JPA annotations)
  <module>/enums/         e.g. domain/staff/enums/EmployeeRole (OWNER, ADMIN, VETERINARIAN, ASSISTANT, RECEPTIONIST)
  <module>/exceptions/    exceptions thrown only by services in that module
  shared/exceptions/      exceptions thrown by services in more than one module
                          (InvalidVeterinaryNameException, VeterinaryNotActiveException,
                          VeterinaryNotFoundException)

application/
  <module>/ports/in/      one *UseCase interface per operation (e.g. RegisterVeterinaryUseCase)
  <module>/ports/out/     repository interface(s) that module's services depend on
                          (VeterinaryRepositoryPort [clinic], VeterinaryEmployeeRepositoryPort [staff],
                          VeterinarySubscriptionRepositoryPort [subscription],
                          UserVeterinaryLinkRepositoryPort [patients])
  <module>/services/      one *Service per use case; implements the matching *UseCase; validates input,
                          loads/checks domain state, applies the change, persists via the out-port, maps
                          to the response DTO. All business rules live here. Services commonly import
                          another module's out-port directly (e.g. subscription's CreatePlanService reads
                          VeterinaryRepositoryPort from clinic) — cross-module reuse happens via ports,
                          not new abstractions.
  <module>/dto/           request/response records per use case (Lombok @Builder POJOs)

infrastructure/
  <module>/controllers/   one @RestController per use case, thin — delegates straight to the *UseCase
  <module>/adapters/      implement that module's out-port(s) on top of Spring Data JPA repositories
  <module>/repositories/  Spring Data JpaRepository interfaces (query derivation + one @Query)
  <module>/entity/        @Entity classes (JPA-mapped; separate from domain models)
  <module>/mapper/        hand-written entity <-> domain model converters (no MapStruct)
  security/               SecurityConfig, JwtClaimsConverter, InternalApiKeyFilter,
                          SpringAuthenticatedUserAdapter, RestAuthenticationEntryPoint,
                          RestAccessDeniedHandler, JwtTokenValidator, ImportSecurityConfig
  config/                 GlobalExceptionHandler (module-agnostic, imports exceptions from every
                          domain/<module>/exceptions and domain/shared/exceptions)

domain/security/          Permission, RolePermissions — framework-free (no Spring types), shared
                          by every module's future authorization checks
```

`VeterinaryApplication` (the `@SpringBootApplication` root) and `VeterinaryApplicationTests` stay at the top-level `com.MyAnimaLog.Veterinary` package — there is no `@EnableJpaRepositories`/`@EntityScan` override, so Spring's default component/repository scan (rooted at that package) covers every module subpackage automatically.

Flow for every endpoint: `Controller -> *UseCase (port in) -> *Service -> *RepositoryPort (port out) -> *RepositoryAdapter -> Spring Data JpaRepository -> *Entity`, with `*Mapper` converting between `*Entity` and the domain model at the adapter boundary. Controllers and services never touch JPA entities directly — only domain models and DTOs cross that boundary.

When adding a new use case, follow the existing 6-file pattern (`dto` request+response, `ports/in` interface, `services` implementation, `controllers` endpoint) inside the relevant module package, rather than extending an existing class — each operation is intentionally isolated. Pick the module by which aggregate owns the data (`clinic` = `Veterinary`, `staff` = `VeterinaryEmployee`, `subscription` = `VeterinarySubscription`, `patients` = `UserVeterinaryLink`); if an exception is only ever thrown by one module's services, it belongs in that module's `domain/<module>/exceptions`, not `shared`.

### Security

Real JWT auth as of VET-05/06/07/11, per `CONTRATOS_COMPARTIDOS.md` §1–3. As of **VET-08**, the 16 pre-existing use cases call `VeterinaryAuthorizationService`. **VET-09/10/12/13** (Fase 2A) added the real clinic/role model on top of that — see the permission table below.

- **Token validation**: `spring-boot-starter-oauth2-resource-server`, `SecurityConfig.jwtDecoder()` builds a `NimbusJwtDecoder` from `${JWT_JWKS_URI}` and validates `iss=myanimalog-user-service` + `aud=myanimalog-api` (`JwtTokenValidator`, framework-adjacent but no network call at construction time — the JWKS fetch happens lazily on first `decode()`). `jjwt` and `jwt.secret`/`JWT_SECRET` were removed — they were never actually used by this service.
- **Authorities**: `JwtClaimsConverter` turns a `Jwt` into a `JwtAuthenticationToken` granting `PLATFORM_ADMIN` when `platform_role=PLATFORM_ADMIN`, and `VET_ROLE_<rol>` when `ctx=VETERINARY` (clinic tokens don't exist yet — the User service will start minting them once it calls VET-11's members endpoint).
- **Route rules** (`SecurityConfig.securityFilterChain`): `/swagger-ui/**`, `/api-docs/**`, `/actuator/health`, `/actuator/info`, `/public/**` are open; `/internal/**` requires authority `INTERNAL_SERVICE` (never a user JWT); `/admin/**` **and** `/api/veterinary/admin/**` both require `PLATFORM_ADMIN`; everything else requires any authenticated token. The second matcher exists because the API gateway only routes `/api/veterinary/**` to this service — a bare `/admin/**` path would never reach it (see "Admin routing convention" below). Services also call `requirePlatformAdmin()` themselves (defense in depth, not just the filter-chain matcher).
- **`/internal/**`**: `InternalApiKeyFilter` (runs before `BearerTokenAuthenticationFilter`) compares the `X-Internal-Api-Key` header against `${INTERNAL_API_KEY}` with `MessageDigest.isEqual` (constant-time). Match → sets a `PreAuthenticatedAuthenticationToken` with `INTERNAL_SERVICE` and continues the chain. No match → writes the 401 JSON directly via `RestAuthenticationEntryPoint` and stops (never reaches `/internal/**`'s own `hasAuthority` check, which would otherwise need an authenticated principal to even evaluate).
- **401/403 JSON format**: `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` write the exact same `{timestamp, status, error, message}` shape as `GlobalExceptionHandler` (`SecurityErrorResponseWriter`), since these two cases are resolved at the filter level, before `@RestControllerAdvice` ever runs.
- **`AuthenticatedUserPort`** (`application/shared/ports/out`) + **`SpringAuthenticatedUserAdapter`** (`infrastructure/security`): reads the current `JwtAuthenticationToken` from `SecurityContextHolder` and maps claims to `AuthenticatedUser` (`userId`, `email`, `emailVerified`, `platformAdmin`, `clinic: Optional<ClinicContext>`) and `ClinicContext` (`veterinaryId`, `employeeId`, `role`, `licensed`, `status`). Throws `UnauthenticatedException` if there's no JWT principal.
- **`VeterinaryAuthorizationService`** (`application/shared/services`): `require(veterinaryId, permission)`, `requireMember(veterinaryId)`, `requirePlatformAdmin()`, `requireSelfOrPermission(targetUserId, veterinaryId, permission)` (allows the call if the caller's own `userId` matches `targetUserId`, regardless of clinic context; otherwise requires `permission` over the clinic — used by `Unlink`). Throws `ClinicContextRequiredException` / `TenantMismatchException` / `InsufficientPermissionException` / `PlatformAdminRequiredException` (all `domain/shared/exceptions`, all mapped to 403 in `GlobalExceptionHandler`; `UnauthenticatedException` maps to 401).
- **`Permission`** (`domain/security`, 11 values from `CONTRATOS_COMPARTIDOS.md` §3.1) and **`RolePermissions.resolve(role, licensed, status)`** (`domain/security`) implement the role → permission matrix for the 5 roles added in **VET-09** (`EmployeeRole`: `OWNER`, `ADMIN`, `VETERINARIAN`, `ASSISTANT`, `RECEPTIONIST`):

  | Permiso | OWNER | ADMIN | VETERINARIAN | ASSISTANT | RECEPTIONIST |
  |---|:-:|:-:|:-:|:-:|:-:|
  | `CLINIC_CONFIGURE` | ✅ | ✅ | | | |
  | `SUBSCRIPTION_MANAGE` | ✅ | | | | |
  | `STAFF_MANAGE` | ✅ | ✅ | | | |
  | `APPOINTMENT_MANAGE` | ✅ | ✅ | ✅ | ✅ | ✅ |
  | `PATIENT_REGISTER` | ✅ | ✅ | ✅ | ✅ | ✅ |
  | `CLINICAL_READ` | ✅ | ✅ | ✅ | ✅ | |
  | `CLINICAL_READ_BASIC` | ✅ | ✅ | ✅ | ✅ | ✅ |
  | `CLINICAL_WRITE` | 🔑 | 🔑 | 🔑 | | |
  | `NURSING_WRITE` | 🔑 | 🔑 | ✅ (incondicional) | ✅ (incondicional) | |
  | `DOCUMENT_UPLOAD` | ✅ | ✅ | ✅ | ✅ | |
  | `REPORTS_VIEW` | ✅ | ✅ | | | |

  🔑 = solo si `licensed=true` (siempre `false` hoy — no existe perfil profesional todavía; `NURSING_WRITE` es incondicional para `VETERINARIAN`/`ASSISTANT`, no depende de la licencia). Si `status != ACTIVE` (cualquier valor distinto de `ACTIVE`), el resultado se reduce a la intersección con `{CLINIC_CONFIGURE, STAFF_MANAGE, SUBSCRIPTION_MANAGE}` — para `OWNER` eso deja las tres; para `ADMIN`, solo las dos primeras (nunca tuvo `SUBSCRIPTION_MANAGE`); para el resto de roles, nada.
- **`VeterinaryStatus`** (`domain/clinic/enums`, 6 valores de `CONTRATOS_COMPARTIDOS.md` §1.3: `PENDING_DOCUMENTS`, `UNDER_REVIEW`, `NEEDS_CORRECTION`, `ACTIVE`, `REJECTED`, `SUSPENDED`) se **persiste** en `public.veterinary.status` desde **VET-09** (antes se derivaba de `active`). `VeterinaryStatus.impliesActiveFlag()` (`this == ACTIVE`) es la única dirección de derivación que queda: el booleano `active` se recalcula desde `status` cada vez que el código cambia el estado (`RegisterVeterinaryService`, `SuspendVeterinaryService`, `ReactivateVeterinaryService`), nunca al revés. La columna `active` sigue viva solo por compatibilidad con código que no se tocó todavía (`CreateEmployeeService`, `LinkByCodeService`/`LinkByUrlService` siguen leyendo `veterinary.getActive()`) — ver "Pendiente" en `db/scripts/README.md`.
- **Endpoint interno de miembros** — `GET /internal/veterinaries/{veterinaryId}/members/{userId}` (`GetVeterinaryMemberController`/`Service`/`UseCase`, módulo `clinic`, consume los 3 puertos de clinic/staff/subscription): lo usará el User service para decidir si puede emitir un token de clínica. Si la clínica no existe **o** el usuario no es empleado, responde `200` con `member=false` (nunca `404`) y el resto de campos en `null`; `licensed` siempre `false` por ahora.
- **Permisos por caso de uso (VET-08, extendido en VET-09/10/12/13)** — las rutas no cambiaron salvo donde se indica, solo quién puede llamarlas y de dónde sale la identidad del caller (`AuthenticatedUserPort`, nunca un campo del body/path que identifique a quien hace la petición):

  | Caso de uso | Permiso / chequeo | Notas |
  |---|---|---|
  | `RegisterVeterinary` | token normal + `email_verified=true` | El creador sale del token y se crea como `OWNER` activo de la clínica nueva (`EmailNotVerifiedException` si `email_verified=false`), en la misma transacción (`@Transactional`). Estado inicial `PENDING_DOCUMENTS` (`active=false`). Valida NIT (`Nit.of`, `InvalidNitException`/`VeterinaryNitAlreadyExistsException`); máximo 3 clínicas en estado no `REJECTED` por `OWNER` (`TooManyOwnedVeterinariesException`). |
  | `GenerateInviteCode` / `UpdateVeterinary` / `UpdateVeterinarySettings` / `DeleteInviteCode` | `CLINIC_CONFIGURE` | `UpdateVeterinarySettings` y `DeleteInviteCode` son de VET-12; no exigen `status=ACTIVE`, una clínica `PENDING_DOCUMENTS` ya puede configurarse. |
  | `GetVeterinaryProfile` (`GET /api/veterinary/{id}`) / `GetActivePlan` / `IsExpired` | `requireMember` (cualquier rol, solo tenant match) | `GetVeterinaryProfile` es de VET-12. |
  | `GetMyVeterinaries` (`GET /api/veterinary/me`) | token normal | Sin chequeo de clínica: lista las clínicas donde el caller es empleado **activo**, con su rol y el `status` de cada clínica (selector de clínica del frontend). VET-12. |
  | `SuspendVeterinary` (`PATCH /api/veterinary/admin/veterinaries/{id}/suspend`) | `PLATFORM_ADMIN` | VET-13, reemplaza a `ActivateVeterinary`/`DeActivateVeterinary`. Solo `ACTIVE → SUSPENDED`; cualquier otro estado de origen → `InvalidVeterinaryStatusTransitionException` (409). |
  | `ReactivateVeterinary` (`PATCH /api/veterinary/admin/veterinaries/{id}/reactivate`) | `PLATFORM_ADMIN` | VET-13. `SUSPENDED → ACTIVE` (no toca `approved_at`, ya estaba aprobada). **Temporalmente** (`TODO(VET-17)`, hasta que exista el flujo de verificación) también acepta `PENDING_DOCUMENTS → ACTIVE` y `UNDER_REVIEW → ACTIVE`, fijando `approved_at=now()`. Cualquier otro origen → 409. |
  | `CreateEmployee` | `STAFF_MANAGE` | No se puede asignar `OWNER` por aquí (`InvalidEmployeeRoleException`, 400) — VET-09. |
  | `UpdateEmployeeRole` / `DeActivateEmployee` | `STAFF_MANAGE` | Nadie puede modificar su propio registro (`CannotModifySelfException`, 403). El `OWNER` no se puede desactivar ni cambiar de rol, sea quien sea el actor (`CannotModifyOwnerException`, 403) — como siempre hay exactamente un `OWNER` por clínica (índice único `ux_one_owner_per_vet`), esto sustituyó a la regla de "último ADMIN" (`LastAdminException`, eliminada en VET-09). Un `ADMIN` tampoco puede modificar a otro `ADMIN` (`CannotModifyPeerAdminException`, 403). `UpdateEmployeeRole` tampoco permite asignar `OWNER` (`InvalidEmployeeRoleException`, 400). El actor se relee de la base por `employeeId` del token (no se confía en `vet_role`/`vet_status` del claim, que puede tener hasta 60 min de antigüedad). |
  | `ActivateEmployee` | `STAFF_MANAGE` | Sin restricción de auto-activación ni de rol del target. |
  | `CreatePlan` / `UpdatePlan` | `PLATFORM_ADMIN` | Siguen en `/api/veterinary/{veterinaryId}/subscription`; no se movieron a `/api/veterinary/admin/**` en VET-13 para no romper rutas que ya pueda usar el frontend — moverlas requiere confirmar con el usuario primero. |
  | `LinkByCode` / `LinkByUrl` | token normal | El `userId` sale del token, ya no viene en el body. Rechazan si la clínica no está `active` (`VeterinaryNotActiveException`) — una clínica `PENDING_DOCUMENTS` no puede vincular dueños todavía. |
  | `Unlink` | self **o** `CLINIC_CONFIGURE` | Permitido si el `userId` del path == `userId` del token (auto-desvinculación), o si el token es de clínica, su `vet_id` coincide con el `veterinaryId` del path y tiene `CLINIC_CONFIGURE` (no `STAFF_MANAGE`: el usuario vinculado es un cliente, no un empleado — ver `VeterinaryAuthorizationService.requireSelfOrPermission`). |

  **Admin routing convention** (VET-13): las operaciones que requieren `PLATFORM_ADMIN` sobre un recurso de este servicio van bajo `/api/veterinary/admin/...` (no bajo el `/admin/**` genérico — el gateway solo enruta `/api/veterinary/**` hacia este servicio). `SecurityConfig` protege ese prefijo igual que `/admin/**`. Futuras rutas de administración (verificaciones, planes manuales) deben seguir esta misma convención; mover endpoints existentes (p. ej. `CreatePlan`/`UpdatePlan`) a este prefijo solo si no rompe rutas que ya use el frontend — si las rompe, confirmar con el usuario antes.

  **Frontend**: todo endpoint salvo los listados como públicos/internos necesita `Authorization: Bearer <token>`. Usa el **token normal** (el que devuelve el login) para `RegisterVeterinary`, `GetMyVeterinaries`, `LinkByCode`/`LinkByUrl`/`Unlink` (auto-desvinculación); usa el **token de clínica** (`POST {USER_URL}/auth/context/veterinary/{veterinaryId}`) para todo lo demás que opera sobre una clínica concreta.
- **Probar manualmente** (con el User service corriendo en `localhost:8080`):
  ```powershell
  $login = Invoke-RestMethod -Uri "http://localhost:8080/auth/local" -Method Post `
      -ContentType "application/json" -Body '{"email":"...","password":"..."}'
  $token = $login.accessToken   # o el campo que use el User service

  Invoke-RestMethod -Uri "http://localhost:8082/api/veterinary/register" -Method Post `
      -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" `
      -Body '{"name":"Clinica X","city":"Bogota","email":"x@x.com"}'
  ```
- **Testing**: `@WebMvcTest` does **not** auto-detect `SecurityConfig`'s plain `@Component` collaborators (`InternalApiKeyFilter`, `JwtClaimsConverter`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`) — confirmed empirically (`NoSuchBeanDefinitionException` without it). Every `@WebMvcTest` controller test needs `@ImportSecurityConfig` (a composed `@Import` annotation in `infrastructure/security`) plus `.with(jwt())` (from `spring-security-test`) on each `mockMvc.perform(...)` call that hits a non-public, non-internal endpoint.

### Error handling

`GlobalExceptionHandler` (`infrastructure/config`, `@RestControllerAdvice`) maps each domain exception to an HTTP status and a uniform JSON body (`timestamp`, `status`, `error`, `message`), with one explicit import per exception class (no wildcard imports — every module's `domain/<module>/exceptions` plus `domain/shared/exceptions`). Any new domain exception needs a corresponding `@ExceptionHandler` method added there, or it falls through to the generic 500 handler.

### `.gitignore` gotcha: don't add a bare `out/` rule

`.gitignore` ignores `/out/` (anchored to the repo root, for IntelliJ's module build output) rather than a bare `out/`. A bare `out/` pattern matches **any** directory named `out` anywhere in the tree, which would silently untrack every `application/<module>/ports/out/*RepositoryPort.java` file (confirmed: this exact bug previously kept all 4 of those port interfaces out of git history entirely, invisible to `git status` since ignored paths don't show as untracked). If you ever see a `*RepositoryPort.java` missing from `git log`/`git show HEAD:...`, suspect this pattern regressing.

### Domain model relationships

- `Veterinary` — the clinic; has `tenantId`, a persisted `status` (`VeterinaryStatus`) kept in sync with the legacy `active` flag, invite fields (`inviteCode`, `inviteLink`) generated on demand, legal/profile fields (`legalName`, `nit: Nit`, `address`, `department`, `latitude`/`longitude`), per-clinic settings (`timezone`, `currency`, `defaultAppointmentMinutes`, `allowOnlineBooking`, `bookingRequiresConfirmation`, `cancellationMinHours`, `directoryVisible`), and `createdBy`/`approvedAt` (VET-09/10).
- `Nit` (`domain/clinic/model`) — Colombian tax ID value object, format `123456789-0`. `Nit.of(raw)` validates format + DIAN check digit (throws `InvalidNitException`); `Nit.fromPersisted(value)` skips validation for data already in the DB (used by `VeterinaryMapper` so a row that predates validation never fails to load).
- `VeterinaryEmployee` — staff member (`veterinaryId` + `userId` + `EmployeeRole` + `active`). Exactly one active `OWNER` per clinic (DB constraint `ux_one_owner_per_vet`, VET-09).
- `VeterinarySubscription` — a plan period (`veterinaryId`, `plan`, `startDate`/`endDate`, `active`); only one active subscription per veterinary is allowed at a time (enforced in `CreatePlanService`).
- `UserVeterinaryLink` — join between an app user and a clinic, created via invite code/URL (`LinkByCodeService` / `LinkByUrlService`), removable via `UnlinkService`.

### Key business rules worth knowing before changing a service

- `RegisterVeterinaryService`: validates name/email/NIT format, enforces unique name/email/NIT, generates a random `tenantId`; creates the caller (from the token) as an active `OWNER` `VeterinaryEmployee` of the new clinic, status `PENDING_DOCUMENTS` (`active=false`), in the same `@Transactional` method. Caps a user at 3 owned clinics not in status `REJECTED`.
- `GenerateInviteCodeService`: generates a unique `VET-XXXXXXXX` code (`SecureRandom`, retries on collision) and builds `inviteLink` from `app.base-url`. `DeleteInviteCodeService` (VET-12) clears both fields back to `null`.
- `LinkByCodeService` / `LinkByUrlService`: reject if the clinic is inactive (`active=false`, i.e. not `ACTIVE`) or the user is already linked.
- `CreatePlanService`: rejects if the clinic is inactive or already has an active subscription; `UpdatePlanService`/`GetActivePlanService`/`IsExpiredService` operate on the single active (or latest) subscription per clinic.
- `ActivateEmployeeService` is a simple state toggle via `toBuilder()`. `UpdateEmployeeRoleService`/`DeActivateEmployeeService` additionally re-fetch the acting employee fresh from the DB (via the token's `employeeId`) and block self-modification, modifying the `OWNER`, or an `ADMIN` modifying another `ADMIN` — see the permission table above.
- `SuspendVeterinaryService`/`ReactivateVeterinaryService` (VET-13): status-transition guards (`InvalidVeterinaryStatusTransitionException` on an invalid `from` state) plus `VeterinaryStatus.impliesActiveFlag()` to keep `active` in sync. `GetVeterinaryProfileService`/`UpdateVeterinarySettingsService`/`GetMyVeterinariesService` (VET-12) are straightforward reads/partial-updates behind the permission checks in the table above.

## Base de datos

El esquema vive en Supabase/Postgres (`public`) y se administra **a mano** en el SQL Editor de Supabase — no hay Flyway ni ningún otro motor de migraciones. `spring.jpa.hibernate.ddl-auto` se mantiene en `none` en todos los perfiles salvo el de tests.

### Esquema real actual

- **`veterinary`**: `id uuid PK`, `name varchar NOT NULL`, `city varchar NOT NULL`, `phone varchar NULL`, `email varchar NULL UNIQUE`, `invite_code varchar NULL UNIQUE`, `invite_link text NULL`, `tenant_id varchar NOT NULL`, `active bool NOT NULL` (compatibilidad, ver nota de `VeterinaryStatus` arriba), `created_at timestamp NOT NULL` (sin zona horaria), `updated_at timestamptz NULL`. Desde `001_clinic_status_and_profile.sql` (VET-09/10/12) además: `status varchar(30) NOT NULL DEFAULT 'PENDING_DOCUMENTS'`, `legal_name varchar NULL`, `nit varchar(20) NULL UNIQUE parcial` (índice único solo `WHERE nit IS NOT NULL`), `address varchar NULL`, `department varchar NULL`, `latitude/longitude numeric(9,6) NULL`, `timezone varchar NOT NULL DEFAULT 'America/Bogota'`, `currency varchar(3) NOT NULL DEFAULT 'COP'`, `default_appointment_minutes int NOT NULL DEFAULT 30`, `allow_online_booking/booking_requires_confirmation/directory_visible bool NOT NULL DEFAULT true`, `cancellation_min_hours int NOT NULL DEFAULT 4`, `created_by uuid NULL`, `approved_at timestamptz NULL`. RLS activado desde `003_enable_rls.sql`.
- **`veterinary_employee`**: `id uuid PK`, `veterinary_id uuid FK → veterinary`, `user_id uuid`, `role varchar` (CHECK `IN ('OWNER','ADMIN','VETERINARIAN','ASSISTANT','RECEPTIONIST')` desde `002_employee_roles_and_ownership.sql`), `active bool`, `created_at timestamptz`. Todas `NOT NULL`. Índices únicos: `ux_one_owner_per_vet` (parcial, `WHERE role='OWNER'`, un solo OWNER por clínica) y `ux_employee_user_per_vet` (`veterinary_id, user_id`) — hay además un `uq_veterinary_employee` preexistente que probablemente duplica a este último, pendiente de limpieza (ver `db/scripts/README.md`). RLS activado desde `003_enable_rls.sql`.
- **`veterinary_subscription`**: `id uuid PK`, `veterinary_id uuid FK → veterinary`, `plan varchar`, `start_date date`, `end_date date`, `active bool`, `created_at timestamptz`. Todas `NOT NULL`.
- **`user_veterinary_link`**: `id uuid PK`, `user_id uuid`, `veterinary_id uuid FK → veterinary`, `status varchar`, `linked_at timestamptz`. Todas `NOT NULL`.

Nota: los modelos de dominio (`domain/model`) no exponen todas estas restricciones (p. ej. nulabilidad) — son el esquema simplificado en memoria, no un espejo 1:1 de las columnas. Antes de asumir que una columna es `NOT NULL` o nullable, confía en esta tabla, no en el `@Data`/`@Builder` del modelo.

### Reglas de `db/scripts/`

Ver `db/scripts/README.md` para la convención completa (numeración `NNN_descripcion.sql`, cabecera con tarea/fecha/qué hace, idempotencia, scripts inmutables una vez ejecutados, tabla de registro de ejecución).

### Regla para Claude Code

Cuando una tarea necesite cambiar el esquema: escribe el script en `db/scripts/`, actualiza las entidades (`infrastructure/entity`) y mappers (`infrastructure/mapper`) que correspondan, y **detente** — no asumas que el script ya fue ejecutado en Supabase ni intentes correr la app o los tests contra el esquema nuevo hasta que el usuario confirme que lo aplicó manualmente en el SQL Editor.

## Eventos (Google Cloud Pub/Sub)

Infraestructura de eventos según el sobre de `CONTRATOS_COMPARTIDOS.md §4`. **Todavía ningún caso de uso publica eventos** — VET-04 solo deja listas las piezas; la emisión real desde los servicios llega en tareas posteriores.

- **Dependencia**: `com.google.cloud:spring-cloud-gcp-starter-pubsub`, versión gestionada por el BOM `com.google.cloud:spring-cloud-gcp-dependencies:7.4.10` (serie `7.x`, la compatible con Spring Boot `3.5.x` según la tabla de compatibilidad oficial del repo `GoogleCloudPlatform/spring-cloud-gcp`).
- **Puerto**: `EventPublisherPort` (`application/shared/ports/out`) — `void publish(DomainEvent event)`.
- **Sobre** (`application/shared/dto`): `DomainEvent` (record: `eventType`, `userId`, `recipient`, `payload`, `occurredAt`, `metadata`), `Recipient` (`email`/`phone`/`name`), `EventMetadata` (record completo, más `EventMetadata.of(veterinaryId, correlationId)` que fija `version="1.0"` y `source="veterinary-service"`). `DomainEvent.create(...)` es el factory: lanza `IllegalArgumentException` si `userId` y `recipient` son ambos `null` (evento inválido — regla de `CONTRATOS_COMPARTIDOS.md §4.1`: nunca se publica, se loguea en el emisor).
- **Tipos de evento**: `VeterinaryEventType` (`domain/shared/enums`), los 22 eventos de `VETERINARY_SERVICE_PLAN.md §11` (de `VETERINARY_REGISTERED` a `PAYMENT_FAILED`).
- **Adaptador**: `GooglePubSubEventAdapter` (`infrastructure/messaging`) implementa el puerto sobre `PubSubTemplate`, serializando con el `ObjectMapper` de Spring (fechas ISO-8601 vía `jackson-datatype-jsr310`). Mapa estático `VeterinaryEventType -> topic`:

  | Topic | Eventos |
  |---|---|
  | `veterinary-clinic` | `VETERINARY_REGISTERED`, `VETERINARY_SUSPENDED`, `VETERINARY_REACTIVATED` |
  | `veterinary-verification` | `VERIFICATION_SUBMITTED`, `VERIFICATION_APPROVED`, `VERIFICATION_REJECTED`, `VERIFICATION_CORRECTION_REQUESTED` |
  | `veterinary-staff` | `EMPLOYEE_INVITED`, `EMPLOYEE_JOINED`, `EMPLOYEE_ROLE_UPDATED`, `EMPLOYEE_DEACTIVATED`, `OWNERSHIP_TRANSFERRED` |
  | `veterinary-patients` | `USER_LINKED`, `USER_UNLINKED`, `PET_SHARED_WITH_VETERINARY`, `PET_SHARE_REVOKED` |
  | `veterinary-subscription` | `SUBSCRIPTION_TRIAL_STARTED`, `SUBSCRIPTION_EXPIRING`, `SUBSCRIPTION_IN_GRACE`, `SUBSCRIPTION_EXPIRED`, `SUBSCRIPTION_RENEWED`, `PAYMENT_FAILED` |

  **Regla obligatoria**: todo valor nuevo que se agregue a `VeterinaryEventType` necesita una entrada en ese mapa — `VeterinaryEventTypeTopicCoverageTest` recorre `VeterinaryEventType.values()` y falla si a alguno le falta topic (a diferencia de Pets, donde un evento sin topic se pierde en silencio).
- **Fallos nunca se propagan**: una excepción al serializar, al llamar `pubSubTemplate.publish(...)`, o una falla asíncrona del `CompletableFuture` devuelto, se captura y se loguea (`eventType`, `veterinaryId`) dentro de `GooglePubSubEventAdapter`; jamás llega al caso de uso que invocó `publish(...)`.
- **Apagado sin credenciales**: `app.events.enabled` (env `EVENTS_ENABLED`, default `true`) está atado en `application.yaml` también a `spring.cloud.gcp.core.enabled` y `spring.cloud.gcp.pubsub.enabled`. En `false`, la autoconfiguración de GCP se salta por completo (no pide credenciales) y se registra `NoOpEventPublisher` (`infrastructure/messaging`, solo loguea en DEBUG) en vez de `GooglePubSubEventAdapter` — ambos usan `@ConditionalOnProperty` sobre `app.events.enabled` y son mutuamente excluyentes.
- **Tests**: el perfil `test` (`src/test/resources/application-test.yaml`) fuerza `app.events.enabled=false` y `spring.cloud.gcp.{pubsub,core}.enabled=false`, independientemente de lo que tenga `.env`. Se activa con `@ActiveProfiles("test")` en cualquier test que levante el contexto completo de Spring (hoy solo `VeterinaryApplicationTests`); los `@WebMvcTest` y los tests de servicio con Mockito puro no lo necesitan porque nunca disparan la autoconfiguración de GCP.
- **Crear los topics reales** (una sola vez, fuera de la app):
  ```bash
  gcloud pubsub topics create veterinary-clinic veterinary-verification \
    veterinary-staff veterinary-patients veterinary-subscription
  ```

## Deuda técnica conocida

- **`GlobalExceptionHandler.handleGeneric` (catch-all `Exception.class`) se traga el 404 de Spring para rutas sin handler mapeado**, convirtiéndolo en `500 Unexpected error: No static resource ...` o `500` genérico, sin importar el método HTTP. Se descubrió al escribir `SecurityFilterChainTest` (VET-05): una request a una ruta `/admin/**` sin controlador que la atienda, con token `PLATFORM_ADMIN` válido (pasa la autorización), debería dar `404`, pero da `500`. Pendiente: que `handleGeneric` reconozca `NoResourceFoundException`/`NoHandlerFoundException` (o excepciones que ya traen su propio `HttpStatusCode`, p. ej. `ErrorResponseException`) y respete su status en vez de forzar 500 siempre. No se tocó en VET-05 porque es un comportamiento preexistente ajeno a seguridad.
- **`CreatePlanService` lanza la excepción equivocada al validar el nombre del plan.** En `application/subscription/services/CreatePlanService.java`, la validación `if (request.getPlan() == null || request.getPlan().isBlank())` lanza `InvalidVeterinaryNameException("Plan name is required")` — la excepción de `domain/shared/exceptions` pensada para el *nombre de la clínica* (la usan `RegisterVeterinaryService`/`UpdateVeterinaryService` de `clinic` para ese mismo propósito), no para el nombre del plan de suscripción. Por eso terminó en `domain/shared/exceptions` en VET-02 (la usan dos módulos) en vez de quedarse en `domain/subscription/exceptions`. El comportamiento HTTP actual es correcto por coincidencia (400 + mensaje "Plan name is required", manejado por el mismo `@ExceptionHandler` en `GlobalExceptionHandler`), pero el acoplamiento es conceptualmente incorrecto. Pendiente: crear una excepción propia (p. ej. `InvalidSubscriptionPlanException`) en `domain/subscription/exceptions`, lanzarla en su lugar, agregar su `@ExceptionHandler`, y evaluar si `InvalidVeterinaryNameException` puede volver a `domain/clinic/exceptions` una vez que ya no la use ningún otro módulo. No se tocó en VET-02 porque esa tarea era solo mover clases, no cambiar lógica.

## Testing conventions

Every service and controller has a matching `*Test` class under `src/test/java` mirroring the main package structure (`application/<module>/services/*ServiceTest`, `infrastructure/<module>/controllers/*ControllerTest`, `infrastructure/<module>/adapters/*RepositoryAdapterTest`). Service tests use Mockito to mock the out-ports; controller tests use `MockMvc`/Spring test slices. Follow this one-test-class-per-class pattern for new use cases.
