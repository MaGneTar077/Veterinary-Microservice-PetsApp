# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`veterinary-service` is a Spring Boot 3.5 / Java 17 microservice (part of the larger "Mascotas" / MyAnimaLog platform) that owns veterinary clinics: registration, activation, staff (employees), invite-based linking of app users to a clinic, and subscription plans. It talks to a Postgres database hosted on Supabase and is designed to run standalone on port 8082 (configurable).

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
| `JWT_SECRET` | Sí | — | Secreto HS256 para `jjwt` (sin uso real hoy — ver sección Security) |
| `APP_BASE_URL` | Sí | — | Base URL pública, usada para construir `inviteLink` |
| `EVENTS_ENABLED` | No | `true` | Activa/desactiva la publicación de eventos a Pub/Sub — ver sección Eventos |
| `GCP_PROJECT_ID` | Solo si `EVENTS_ENABLED=true` contra un proyecto real | — | Proyecto de Google Cloud para Pub/Sub |

## Architecture

This service follows **hexagonal architecture (ports & adapters)**, organized by **business module** (`clinic`, `staff`, `subscription`, `patients`), strictly one-feature-per-slice within each module: every use case gets its own request/response DTO pair, inbound port, service, and controller.

```
domain/
  <module>/model/        domain models for that module (plain Lombok @Data/@Builder, no JPA annotations)
  <module>/enums/         e.g. domain/staff/enums/EmployeeRole (VETERINARIAN, ASSISTANT, ADMIN)
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
  security/               SecurityConfig
  config/                 GlobalExceptionHandler (module-agnostic, imports exceptions from every
                          domain/<module>/exceptions and domain/shared/exceptions)
```

`VeterinaryApplication` (the `@SpringBootApplication` root) and `VeterinaryApplicationTests` stay at the top-level `com.MyAnimaLog.Veterinary` package — there is no `@EnableJpaRepositories`/`@EntityScan` override, so Spring's default component/repository scan (rooted at that package) covers every module subpackage automatically.

Flow for every endpoint: `Controller -> *UseCase (port in) -> *Service -> *RepositoryPort (port out) -> *RepositoryAdapter -> Spring Data JpaRepository -> *Entity`, with `*Mapper` converting between `*Entity` and the domain model at the adapter boundary. Controllers and services never touch JPA entities directly — only domain models and DTOs cross that boundary.

When adding a new use case, follow the existing 6-file pattern (`dto` request+response, `ports/in` interface, `services` implementation, `controllers` endpoint) inside the relevant module package, rather than extending an existing class — each operation is intentionally isolated. Pick the module by which aggregate owns the data (`clinic` = `Veterinary`, `staff` = `VeterinaryEmployee`, `subscription` = `VeterinarySubscription`, `patients` = `UserVeterinaryLink`); if an exception is only ever thrown by one module's services, it belongs in that module's `domain/<module>/exceptions`, not `shared`.

### Security

`SecurityConfig` (`infrastructure/security`) currently **permits all requests** (`anyRequest().permitAll()`, CSRF disabled) — there is no authentication/authorization enforced at this layer despite the `jjwt` dependency and `jwt.secret` config being present. Caller identity (e.g. `userId` for linking) is passed directly in request bodies/path variables, not derived from a token. Treat this as a known gap, not an intended design, when touching security-sensitive code.

### Error handling

`GlobalExceptionHandler` (`infrastructure/config`, `@RestControllerAdvice`) maps each domain exception to an HTTP status and a uniform JSON body (`timestamp`, `status`, `error`, `message`), with one explicit import per exception class (no wildcard imports — every module's `domain/<module>/exceptions` plus `domain/shared/exceptions`). Any new domain exception needs a corresponding `@ExceptionHandler` method added there, or it falls through to the generic 500 handler.

### `.gitignore` gotcha: don't add a bare `out/` rule

`.gitignore` ignores `/out/` (anchored to the repo root, for IntelliJ's module build output) rather than a bare `out/`. A bare `out/` pattern matches **any** directory named `out` anywhere in the tree, which would silently untrack every `application/<module>/ports/out/*RepositoryPort.java` file (confirmed: this exact bug previously kept all 4 of those port interfaces out of git history entirely, invisible to `git status` since ignored paths don't show as untracked). If you ever see a `*RepositoryPort.java` missing from `git log`/`git show HEAD:...`, suspect this pattern regressing.

### Domain model relationships

- `Veterinary` — the clinic; has `tenantId`, `active` flag, and invite fields (`inviteCode`, `inviteLink`) generated on demand.
- `VeterinaryEmployee` — staff member (`veterinaryId` + `userId` + `EmployeeRole` + `active`).
- `VeterinarySubscription` — a plan period (`veterinaryId`, `plan`, `startDate`/`endDate`, `active`); only one active subscription per veterinary is allowed at a time (enforced in `CreatePlanService`).
- `UserVeterinaryLink` — join between an app user and a clinic, created via invite code/URL (`LinkByCodeService` / `LinkByUrlService`), removable via `UnlinkService`.

### Key business rules worth knowing before changing a service

- `RegisterVeterinaryService`: validates name/email format, enforces unique name and email, generates a random `tenantId`.
- `GenerateInviteCodeService`: generates a unique `VET-XXXXXXXX` code (`SecureRandom`, retries on collision) and builds `inviteLink` from `app.base-url`.
- `LinkByCodeService` / `LinkByUrlService`: reject if the clinic is inactive or the user is already linked.
- `CreatePlanService`: rejects if the clinic is inactive or already has an active subscription; `UpdatePlanService`/`GetActivePlanService`/`IsExpiredService` operate on the single active (or latest) subscription per clinic.
- Activation/deactivation services (`Activate/DeActivate{Employee,Veterinary}Service`) are simple state toggles via `toBuilder()`.

## Base de datos

El esquema vive en Supabase/Postgres (`public`) y se administra **a mano** en el SQL Editor de Supabase — no hay Flyway ni ningún otro motor de migraciones. `spring.jpa.hibernate.ddl-auto` se mantiene en `none` en todos los perfiles salvo el de tests.

### Esquema real actual

- **`veterinary`**: `id uuid PK`, `name varchar NOT NULL`, `city varchar NOT NULL`, `phone varchar NULL`, `email varchar NULL UNIQUE`, `invite_code varchar NULL UNIQUE`, `invite_link text NULL`, `tenant_id varchar NOT NULL`, `active bool NOT NULL`, `created_at timestamp NOT NULL` (sin zona horaria), `updated_at timestamptz NULL`.
- **`veterinary_employee`**: `id uuid PK`, `veterinary_id uuid FK → veterinary`, `user_id uuid`, `role varchar`, `active bool`, `created_at timestamptz`. Todas `NOT NULL`.
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

- **`CreatePlanService` lanza la excepción equivocada al validar el nombre del plan.** En `application/subscription/services/CreatePlanService.java`, la validación `if (request.getPlan() == null || request.getPlan().isBlank())` lanza `InvalidVeterinaryNameException("Plan name is required")` — la excepción de `domain/shared/exceptions` pensada para el *nombre de la clínica* (la usan `RegisterVeterinaryService`/`UpdateVeterinaryService` de `clinic` para ese mismo propósito), no para el nombre del plan de suscripción. Por eso terminó en `domain/shared/exceptions` en VET-02 (la usan dos módulos) en vez de quedarse en `domain/subscription/exceptions`. El comportamiento HTTP actual es correcto por coincidencia (400 + mensaje "Plan name is required", manejado por el mismo `@ExceptionHandler` en `GlobalExceptionHandler`), pero el acoplamiento es conceptualmente incorrecto. Pendiente: crear una excepción propia (p. ej. `InvalidSubscriptionPlanException`) en `domain/subscription/exceptions`, lanzarla en su lugar, agregar su `@ExceptionHandler`, y evaluar si `InvalidVeterinaryNameException` puede volver a `domain/clinic/exceptions` una vez que ya no la use ningún otro módulo. No se tocó en VET-02 porque esa tarea era solo mover clases, no cambiar lógica.

## Testing conventions

Every service and controller has a matching `*Test` class under `src/test/java` mirroring the main package structure (`application/<module>/services/*ServiceTest`, `infrastructure/<module>/controllers/*ControllerTest`, `infrastructure/<module>/adapters/*RepositoryAdapterTest`). Service tests use Mockito to mock the out-ports; controller tests use `MockMvc`/Spring test slices. Follow this one-test-class-per-class pattern for new use cases.
