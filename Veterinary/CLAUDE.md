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
| `SUPPORT_EMAIL` | No | `` (vacío) | Destinatario del evento `VERIFICATION_SUBMITTED` (Fase 2B Parte 2+) |
| `AI_VERIFICATION_ENABLED` | No | `false` | Prende el puerto de verificación por IA (`CONTRATOS_COMPARTIDOS.md` §13); apagado hoy, `ai_status` queda siempre `NOT_RUN` |
| `IA_SERVICE_URL` | Solo si `AI_VERIFICATION_ENABLED=true` | — | URL del servicio de IA de verificación |
| `LEGAL_DATA_POLICY_VERSION` | No | `2026-10-v1` | Versión vigente del consentimiento `DATA_TREATMENT` (`legal_consents`); un consentimiento con una versión distinta → `409 CONSENT_VERSION_OUTDATED` |
| `LEGAL_TERMS_VERSION` | No | `2026-10-v1` | Versión vigente del consentimiento `TERMS` |
| `USER_SERVICE_URL` | No (vacío = las llamadas fallan y el detalle cae al fallback `{ id }`) | `` (vacío) | Base URL del User service, usada por `UserServiceRestAdapter` (`GET {USER_SERVICE_URL}/internal/users/{id}`) para resolver `account` en el detalle del admin (Fase 2B Parte 4A) |
| `RUES_URL` | No | `https://www.rues.org.co/` | `registryLookupUrl` del detalle del admin cuando el sujeto es `CLINIC` |
| `COMVEZCOL_URL` | No | `https://consejoprofesionalmvz.gov.co/consulta-de-profesionales/` | `registryLookupUrl` del detalle del admin cuando el sujeto es `PROFESSIONAL` |

**Importante para desarrollo local**: con `EVENTS_ENABLED` sin definir (default `true`) y sin `GCP_PROJECT_ID`, la app **no arranca** — la autoconfiguración de `spring-cloud-gcp-starter-pubsub` intenta construir un `PublisherFactory` real y falla con `IllegalArgumentException: The project ID can't be null or empty`. En local, deja `EVENTS_ENABLED=false` en `.env` (ya está así) a menos que de verdad vayas a publicar a un proyecto GCP real.

## Architecture

This service follows **hexagonal architecture (ports & adapters)**, organized by **business module** (`clinic`, `staff`, `subscription`, `patients`, and since **Fase 2B Parte 1**: `legal`, `professional`, `verification`), strictly one-feature-per-slice within each module: every use case gets its own request/response DTO pair, inbound port, service, and controller.

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
                          UserVeterinaryLinkRepositoryPort [patients],
                          LegalConsentRepositoryPort [legal] — entity/mapper/repository/adapter only, no
                          *UseCase/*Service/controller yet (nothing writes legal_consents outside
                          RegisterVeterinaryService, which reads this port directly),
                          ProfessionalProfileRepositoryPort [professional],
                          VerificationRequestRepositoryPort / VerificationDocumentRepositoryPort / IaVerificationPort [verification]);
                          application/shared/ports/out also has UserServicePort (Fase 2B Parte 4A,
                          infrastructure/clients/UserServiceRestAdapter — resolves account info for the admin detail)
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
  storage/                SupabaseStorageAdapter (implements FileStoragePort over the Supabase
                          Storage REST API), StorageProperties, Sha256 — see "Almacenamiento de
                          archivos" below (Fase 2B Parte 1)
  ai/                     NoOpIaVerificationAdapter / IaHttpVerificationAdapter (implement
                          IaVerificationPort, mutually exclusive via @ConditionalOnProperty on
                          app.ai.verification-enabled) — Fase 2B Parte 4A
  clients/                UserServiceRestAdapter (implements UserServicePort) — Fase 2B Parte 4A

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
- **`VeterinaryAuthorizationService`** (`application/shared/services`): `require(veterinaryId, permission)`, `requireMember(veterinaryId)`, `requirePlatformAdmin()`, `requireSelfOrPermission(targetUserId, veterinaryId, permission)` (allows the call if the caller's own `userId` matches `targetUserId`, regardless of clinic context; otherwise requires `permission` over the clinic — used by `Unlink`). Throws `ClinicContextRequiredException` / `TenantMismatchException` / `InsufficientPermissionException` / `PlatformAdminRequiredException` (all `domain/shared/exceptions`, all mapped to 403 in `GlobalExceptionHandler`; `UnauthenticatedException` maps to 401). Since **Fase 2B Parte 2**, also: `requireVeterinaryOwner(veterinaryId)` — decision D1 of `FASE_2B_VERIFICACION.md`: accepts a normal token or a clinic token (if its `vet_id` matches the path, else `TenantMismatchException`), but **always** re-checks the database (`VeterinaryEmployeeRepositoryPort.findByVeterinaryIdAndUserId`) that the caller is the clinic's active `OWNER` employee — never trusts `vet_role` alone, because this must keep working while the clinic is `REJECTED` (no clinic token would even be issued for it) or in any other status. No match → `NotVeterinaryOwnerException` (403, `NOT_VETERINARY_OWNER`). `requireOwnerOrPermission(veterinaryId, permission)` — passes if a clinic token matching the path already has `permission` (no DB hit); otherwise falls back to `requireVeterinaryOwner` (used by the logo endpoints, so OWNER keeps access pre-approval and `CLINIC_CONFIGURE` staff keep access once the clinic operates).
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
- **Endpoint interno de miembros** — `GET /internal/veterinaries/{veterinaryId}/members/{userId}` (`GetVeterinaryMemberController`/`Service`/`UseCase`, módulo `clinic`, consume los 3 puertos de clinic/staff/subscription): lo usará el User service para decidir si puede emitir un token de clínica. Si la clínica no existe **o** el usuario no es empleado, responde `200` con `member=false` (nunca `404`) y el resto de campos en `null`; `licensed` es `true` solo cuando el usuario tiene un `ProfessionalProfile` con `licenseStatus == VERIFIED` (Fase 2B Parte 3).
- **Permisos por caso de uso (VET-08, extendido en VET-09/10/12/13)** — las rutas no cambiaron salvo donde se indica, solo quién puede llamarlas y de dónde sale la identidad del caller (`AuthenticatedUserPort`, nunca un campo del body/path que identifique a quien hace la petición):

  | Caso de uso | Permiso / chequeo | Notas |
  |---|---|---|
  | `RegisterVeterinary` | token normal + `email_verified=true` | El creador sale del token y se crea como `OWNER` activo de la clínica nueva (`EmailNotVerifiedException` si `email_verified=false`), en la misma transacción (`@Transactional`). Estado inicial `PENDING_DOCUMENTS` (`active=false`). Valida NIT (`Nit.of`, `InvalidNitException`/`VeterinaryNitAlreadyExistsException`); máximo 3 clínicas en estado no `REJECTED` por `OWNER` (`TooManyOwnedVeterinariesException`). Desde **Fase 2B Parte 2**: `name`/`legalName`/`phone`/`email`/`department`/`city`/`address` obligatorios (`InvalidVeterinaryFieldException`/`InvalidVeterinaryEmailException`), `latitude`/`longitude` opcionales pero juntas y en rango; exige `acceptedDataPolicyVersion`/`acceptedTermsVersion` iguales a `app.legal.data-policy-version`/`terms-version` (`ConsentRequiredException`/`ConsentVersionOutdatedException`) y, en la misma transacción, crea 2 filas en `legal_consents` y una `verification_request` `CLINIC` en `DRAFT` (intento 1); publica `VETERINARY_REGISTERED`. |
  | `UpdateLegalRepresentative` (`PUT /api/veterinary/{id}/legal-representative`) | `requireVeterinaryOwner` | Fase 2B Parte 2. Solo con la `verification_request` de la clínica abierta (`DRAFT`/`NEEDS_CORRECTION`); si no → `VerificationLockedException` (409). `documentType` `CC`/`CE`/`PPT`; `documentNumber` solo dígitos si `CC`, alfanumérico si `CE`/`PPT` (`InvalidLegalRepresentativeException`, 400). |
  | `UploadLogo` / `DeleteLogo` (`PUT`/`DELETE /api/veterinary/{id}/logo`) | `requireOwnerOrPermission(id, CLINIC_CONFIGURE)` | Fase 2B Parte 2. PNG/JPEG/WEBP ≤2MB validados por firma de archivo real, no por `Content-Type` (`InvalidFileTypeException`/`FileTooLargeException`). Bucket público `app.storage.logos-bucket`, ruta `veterinaries/{id}/logo.<ext>`; si la extensión cambia, borra el archivo anterior. |
  | `UploadVerificationDocument` / `DeleteVerificationDocument` (`POST /api/veterinary/{id}/verification/documents`, `DELETE .../documents/{docId}`) | `requireVeterinaryOwner` | Fase 2B Parte 2. Solo con la solicitud abierta (`VerificationLockedException` si no). Tipos `RUT`/`CHAMBER_OF_COMMERCE`/`LEGAL_REP_ID`/`FACADE_PHOTO`; PDF/JPEG/PNG ≤10MB por firma real. Subir el mismo tipo otra vez reemplaza el documento anterior (borra la fila y el archivo viejo después de guardar el nuevo); si falla el guardado en base después de subir, borra el archivo subido (compensación). Bucket privado `app.storage.verification-bucket`. |
  | `GetClinicVerification` (`GET /api/veterinary/{id}/verification`) | `requireVeterinaryOwner` | Fase 2B Parte 2. Nunca incluye `ai_report`/`ai_risk_level` ni URLs de archivos — solo nombre/tipo/estado/fecha por documento. |
  | `SubmitClinicVerification` (`POST .../verification/submit`) | `requireVeterinaryOwner` | Fase 2B Parte 2. `truthfulnessDeclared != true` → 400; faltan representante legal completo o alguno de los 3 documentos obligatorios → `MissingRequiredDocumentsException` (400); solicitud no `DRAFT`/`NEEDS_CORRECTION` → `InvalidVerificationStateException` (409). Pasa a `UNDER_REVIEW` (clínica y solicitud), limpia `correction_document_types`/`correction_requested_at` si venía de `NEEDS_CORRECTION`, publica `VERIFICATION_SUBMITTED` (`recipient.email=app.support-email`). Desde **Fase 2B Parte 3**: si `includeProfessionalProfile=true`, llama a `ProfessionalProfileSubmissionService.submitForUser(callerId)` dentro de la misma transacción (`@Transactional` ya envolvía todo el método) — si el perfil no existe o no está listo, esa llamada lanza y el `@Transactional` revierte también los cambios de la clínica que ya se habían guardado (nunca queda la clínica en `UNDER_REVIEW` sin que el perfil también se haya enviado). |
  | `RetryClinicVerification` (`POST .../verification/retry`) | `requireVeterinaryOwner` | Fase 2B Parte 2. Solo si la última solicitud es `REJECTED` (si no → `InvalidVerificationStateException`, 409); al 3er intento agotado → `VerificationAttemptsExhaustedException` (409). Crea una `verification_request` `DRAFT` nueva (`attempt_number + 1`, sin copiar documentos) y vuelve la clínica a `PENDING_DOCUMENTS`. |
  | `GetMyProfessionalProfile` (`GET /api/veterinary/me/professional-profile`) | token normal | Fase 2B Parte 3. El perfil es siempre el del `userId` del token — nunca un parámetro. 404 `ProfessionalProfileNotFoundException` si no existe. |
  | `UpsertProfessionalProfile` (`PUT /api/veterinary/me/professional-profile`) | token normal | Fase 2B Parte 3. Crea (+ `verification_request` `PROFESSIONAL` `DRAFT`) o actualiza. En `PENDING` no se puede editar nada (`VerificationLockedException`, 409). Si está `VERIFIED` y cambia algún dato de identidad (`fullName`/`documentType`/`documentNumber`/`profession`/`licenseNumber`) vuelve a `NOT_SUBMITTED` y crea una nueva solicitud `DRAFT`; `specialty`/`university` se editan libres sin disparar eso. Si está `REJECTED`, cualquier edición reintenta (máx. 3 intentos, igual que la clínica) — `VerificationAttemptsExhaustedException` (409) al agotarlos. `licenseNumber` único (`existsByLicenseNumberAndUserIdNot`, excluye el propio perfil) → `LicenseNumberAlreadyExistsException` (409, `LICENSE_NUMBER_TAKEN`). |
  | `UploadProfessionalDocument` / `DeleteProfessionalDocument` (`POST /api/veterinary/me/professional-profile/documents`, `DELETE .../documents/{docId}`) | token normal | Fase 2B Parte 3. Mismas reglas de archivo/reemplazo/compensación que las de la clínica (`ClinicDocumentType` → `ProfessionalDocumentType`: `PROFESSIONAL_CARD`/`ID_DOCUMENT` obligatorios, `DEGREE` opcional), sobre la `verification_request` `PROFESSIONAL` del perfil del caller, solo si está `DRAFT`/`NEEDS_CORRECTION`. |
  | `UploadProfessionalSignature` (`PUT .../me/professional-profile/signature`) | token normal | Fase 2B Parte 3. Solo PNG, ≤1MB, bucket privado, ruta `professionals/{profileId}/signature.png` (upsert — se puede resubir en cualquier estado de la solicitud). Guarda `signature_url` como la ruta, nunca una URL pública ni firmada. |
  | `SubmitProfessionalProfile` (`POST .../me/professional-profile/submit`) | token normal | Fase 2B Parte 3. Exige `PROFESSIONAL_CARD` e `ID_DOCUMENT` (`MissingRequiredDocumentsException`, 400); solicitud debe estar `DRAFT`/`NEEDS_CORRECTION` (`InvalidVerificationStateException`, 409). `license_status → PENDING`, solicitud `→ UNDER_REVIEW`, publica `VERIFICATION_SUBMITTED` (`subjectType=PROFESSIONAL`). La transición real vive en `ProfessionalProfileSubmissionService` (no implementa ningún `*UseCase` — es un colaborador interno compartido, ver más abajo), reusada tal cual por `SubmitClinicVerification` cuando `includeProfessionalProfile=true`. |
  | `GetEmployeeProfessionalProfile` (`GET /api/veterinary/{id}/employees/{employeeId}/professional-profile`) | `requireMember` | Fase 2B Parte 3. Resuelve `employeeId → userId` vía `VeterinaryEmployeeRepositoryPort.findById` (404 `EmployeeNotFoundException` si no es de esa clínica). Solo expone `fullName`, `profession`, `licenseNumber`, `specialty`, `university`, `licenseStatus` — nunca `documentType`/`documentNumber` (documento de identidad), documentos, ni firma. |
  | `GenerateInviteCode` / `UpdateVeterinary` / `UpdateVeterinarySettings` / `DeleteInviteCode` | `CLINIC_CONFIGURE` | `UpdateVeterinarySettings` y `DeleteInviteCode` son de VET-12; no exigen `status=ACTIVE`, una clínica `PENDING_DOCUMENTS` ya puede configurarse. |
  | `GetVeterinaryProfile` (`GET /api/veterinary/{id}`) / `GetActivePlan` / `IsExpired` | `requireMember` (cualquier rol, solo tenant match) | `GetVeterinaryProfile` es de VET-12. |
  | `GetMyVeterinaries` (`GET /api/veterinary/me`) | token normal | Sin chequeo de clínica: lista las clínicas donde el caller es empleado **activo**, con su rol y el `status` de cada clínica (selector de clínica del frontend). VET-12. |
  | `SuspendVeterinary` (`PATCH /api/veterinary/admin/veterinaries/{id}/suspend`) | `PLATFORM_ADMIN` | VET-13, reemplaza a `ActivateVeterinary`/`DeActivateVeterinary`. Solo `ACTIVE → SUSPENDED`; cualquier otro estado de origen → `InvalidVeterinaryStatusTransitionException` (409). |
  | `ReactivateVeterinary` (`PATCH /api/veterinary/admin/veterinaries/{id}/reactivate`) | `PLATFORM_ADMIN` | VET-13. Solo `SUSPENDED → ACTIVE` (no toca `approved_at`, ya estaba aprobada). Cualquier otro origen → 409. El atajo temporal que también aceptaba `PENDING_DOCUMENTS`/`UNDER_REVIEW → ACTIVE` (`TODO(VET-17)`) se quitó en **Fase 2B Parte 4A** — aprobar clínicas nuevas es exclusivo de `ApproveVerification`. |
  | `ListAdminVerifications` (`GET /api/veterinary/admin/verifications?status=&subjectType=&page=&size=`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. Cola de revisión con `counts` por estado (respetan `subjectType`, no `status`); default `status=UNDER_REVIEW`. |
  | `GetAdminVerificationDetail` (`GET .../admin/verifications/{requestId}`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. Datos declarados + `account` (vía `UserServicePort`, cae a `{ id }` si el User service falla) + documentos con `signedUrl` de 10 min + `ai` + `duplicates` (mismo `sha256` en otro sujeto) + `history`. |
  | `RenewVerificationDocumentSignedUrl` (`GET .../{requestId}/documents/{documentId}/signed-url`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. Renueva una URL firmada vencida en el visor del admin. |
  | `ApproveVerification` (`POST .../{requestId}/approve`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. Exige `registryChecked: true` (`RegistryCheckRequiredException`, 400). Solo si la solicitud está `UNDER_REVIEW` (`InvalidVerificationStateException`, 409, también si otro admin ya decidió — ver `claimForDecision` abajo). Un admin no puede aprobar su propia solicitud (`CannotReviewOwnRequestException`, 403). Clínica → `ACTIVE` + prueba `PRO_TRIAL` de 30 días si no había suscripción activa (+ evento `SUBSCRIPTION_TRIAL_STARTED`); perfil → `VERIFIED`. Publica `VERIFICATION_APPROVED`. |
  | `RejectVerification` (`POST .../{requestId}/reject`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. `reasonCode` (`RejectionReasonCode`) y `notes` (máx. 1000) obligatorios (`InvalidReviewDecisionException`, 400). Mismas reglas de estado/concurrencia/conflicto de interés que `approve`. Clínica → `REJECTED`; perfil → `REJECTED` con `rejectionReason=notes`. Publica `VERIFICATION_REJECTED` (`attemptsRemaining`, `documentsDeletionDate`). |
  | `RequestVerificationCorrection` (`POST .../{requestId}/request-correction`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. `notes` y `documentTypes` obligatorios; cada tipo debe pertenecer al sujeto (`InvalidDocumentTypeException`, 400 si no). Mismas reglas de estado/concurrencia/conflicto de interés. Clínica → `NEEDS_CORRECTION`; perfil → `NOT_SUBMITTED` (vuelve a editable). Publica `VERIFICATION_CORRECTION_REQUESTED`. |
  | `RerunVerificationAnalysis` (`POST .../{requestId}/rerun-ai`) | `PLATFORM_ADMIN` | Fase 2B Parte 4A. Con `AI_VERIFICATION_ENABLED=false` (o cualquier adaptador cuyo `isEnabled()` sea `false`) → `409 AI_VERIFICATION_DISABLED`. Si está encendida, publica el mismo `VerificationSubmittedEvent` que el envío — mismo flujo async, no hay una segunda implementación. |
  | `CleanupVerificationDocumentsJob` (`POST /internal/jobs/verification-documents/cleanup`) | `INTERNAL_SERVICE` (`X-Internal-Api-Key`, no un usuario) | Fase 2B Parte 4A. Borra documentos (storage + fila) de solicitudes `REJECTED` con `decided_at` de hace más de 90 días. Idempotente. |
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

`GlobalExceptionHandler` (`infrastructure/config`, `@RestControllerAdvice`) maps each domain exception to an HTTP status and a uniform JSON body (`timestamp`, `status`, `error`, `message`, `code`), with one explicit import per exception class (no wildcard imports — every module's `domain/<module>/exceptions` plus `domain/shared/exceptions`). Any new domain exception needs a corresponding `@ExceptionHandler` method added there (status **and** `code`), or it falls through to the generic 500 handler (`code=INTERNAL_ERROR`). Since **Fase 2B Parte 1**, the same `{timestamp, status, error, message, code}` shape is also used by:

- `MethodArgumentNotValidException` (Spring's `@Valid` binding errors) → 400, `code=VALIDATION_ERROR`, `message` is a `field: defaultMessage` list joined with `; `.
- `NoResourceFoundException` / `NoHandlerFoundException` (a route with no matching handler) → 404, `code=NOT_FOUND`. This fixes the previously-known bug where an unmapped route fell through to the catch-all `Exception` handler and returned 500 instead of 404 (see former "Deuda técnica conocida" entry, now resolved).
- The generic catch-all `Exception.class` → 500, `code=INTERNAL_ERROR`, `message="Error interno, intenta de nuevo"`. **Never the real exception message or stack trace** — a production `RegisterVeterinary` failure once leaked a raw SQL constraint-violation message (table/column names) straight into the HTTP response before this was fixed (Fase 2B Parte 2 hardening). The real exception is logged server-side (`log.error("Unexpected error", ex)`) for debugging. `DataIntegrityViolationException` (Spring Data, e.g. a DB constraint violation reaching this far) gets the same treatment via its own `@ExceptionHandler` so it's never left to the generic one by accident.
- `RestAuthenticationEntryPoint` (401, no/invalid JWT) and `RestAccessDeniedHandler` (403, filter-chain `hasAuthority(...)` rejection) — resolved by Spring Security *before* `@RestControllerAdvice` ever runs, so they write the same JSON shape themselves via `SecurityErrorResponseWriter.write(response, status, message, code)`: `code=UNAUTHENTICATED` for the entry point, `code=INSUFFICIENT_PERMISSION` for the access-denied handler.

**Code table** (one stable `code` per domain exception; `?` marks codes that don't appear in `FASE_2B_VERIFICACION.md` §8 and were assigned here to keep every exception covered):

| HTTP | `code` | Exception(s) |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `InvalidVeterinaryNameException`, `InvalidVeterinaryEmailException`, `MethodArgumentNotValidException` |
| 400 | `INVALID_NIT` | `InvalidNitException` |
| 400 | `INVALID_EMPLOYEE_ROLE` ? | `InvalidEmployeeRoleException` |
| 400 | `INVALID_INVITE_CODE` ? | `InvalidInviteCodeException` |
| 400 | `CONSENT_REQUIRED` | `ConsentRequiredException` (`RegisterVeterinary`, Fase 2B Parte 2) |
| 400 | `TRUTHFULNESS_DECLARATION_REQUIRED` | `TruthfulnessDeclarationRequiredException` (`SubmitClinicVerification`) |
| 400 | `MISSING_REQUIRED_DOCUMENTS` | `MissingRequiredDocumentsException` (`SubmitClinicVerification`) |
| 400 | `INVALID_FILE_TYPE` | `InvalidFileTypeException` (`domain/shared/exceptions` — logo and verification-document uploads; thrown when the file's real magic-number signature doesn't match an allowed type, never from a trusted `Content-Type` header) |
| 400 | `VALIDATION_ERROR` (reused) | `InvalidVeterinaryFieldException` (`RegisterVeterinary` — legalName/city/department/address/phone/lat-long), `InvalidLegalRepresentativeException`, `InvalidDocumentTypeException`, `InvalidProfessionalProfileException` (`UpsertProfessionalProfile`), `InvalidReviewDecisionException` (`RejectVerification`/`RequestVerificationCorrection`, Fase 2B Parte 4A — missing/too-long `notes`, missing `reasonCode`/`documentTypes`) |
| 400 | `REGISTRY_CHECK_REQUIRED` | `RegistryCheckRequiredException` (`ApproveVerification`, Fase 2B Parte 4A) |
| 403 | `EMAIL_NOT_VERIFIED` | `EmailNotVerifiedException` |
| 403 | `TENANT_MISMATCH` | `TenantMismatchException` |
| 403 | `INSUFFICIENT_PERMISSION` | `InsufficientPermissionException`, Spring Security access-denied handler |
| 403 | `PLATFORM_ADMIN_REQUIRED` | `PlatformAdminRequiredException` |
| 403 | `CANNOT_MODIFY_SELF` | `CannotModifySelfException` |
| 403 | `CANNOT_MODIFY_OWNER` | `CannotModifyOwnerException` |
| 403 | `CANNOT_MODIFY_PEER_ADMIN` ? | `CannotModifyPeerAdminException` |
| 403 | `CLINIC_CONTEXT_REQUIRED` ? | `ClinicContextRequiredException` |
| 403 | `NOT_VETERINARY_OWNER` | `NotVeterinaryOwnerException` (`domain/shared/exceptions`, thrown by `VeterinaryAuthorizationService.requireVeterinaryOwner`/`requireOwnerOrPermission`) |
| 403 | `CANNOT_REVIEW_OWN_REQUEST` | `CannotReviewOwnRequestException` (`domain/verification/exceptions`, Fase 2B Parte 4A — `ApproveVerification`/`RejectVerification`/`RequestVerificationCorrection` when `submitted_by == caller`) |
| 404 | `VETERINARY_NOT_FOUND` | `VeterinaryNotFoundException` |
| 404 | `EMPLOYEE_NOT_FOUND` ? | `EmployeeNotFoundException` |
| 404 | `SUBSCRIPTION_NOT_FOUND` ? | `SubscriptionNotFoundException` |
| 404 | `NO_ACTIVE_SUBSCRIPTION` ? | `NoActiveSubscriptionException` |
| 404 | `USER_NOT_LINKED` ? | `UserNotLinkedException` |
| 404 | `NOT_FOUND` ? | `NoResourceFoundException`, `NoHandlerFoundException` (unmapped route) |
| 404 | `DOCUMENT_NOT_FOUND` | `VerificationDocumentNotFoundException` (`DeleteVerificationDocument`) |
| 404 | `VERIFICATION_REQUEST_NOT_FOUND` | `VerificationRequestNotFoundException` (`GetClinicVerification`/`SubmitClinicVerification`/`RetryClinicVerification` — should not happen in practice since `RegisterVeterinary` always creates one, but guards against the edge case) |
| 409 | `VETERINARY_NAME_TAKEN` | `VeterinaryAlreadyExistsException` |
| 409 | `VETERINARY_EMAIL_TAKEN` | `VeterinaryEmailAlreadyExistsException` |
| 409 | `NIT_TAKEN` | `VeterinaryNitAlreadyExistsException` |
| 409 | `OWNER_CLINIC_LIMIT_REACHED` | `TooManyOwnedVeterinariesException` |
| 409 | `VETERINARY_NOT_ACTIVE` | `VeterinaryNotActiveException` |
| 409 | `CONSENT_VERSION_OUTDATED` | `ConsentVersionOutdatedException` (`RegisterVeterinary`) |
| 409 | `VERIFICATION_LOCKED` | `VerificationLockedException` (`domain/shared/exceptions` — used by `UpdateLegalRepresentative` and the verification-document upload/delete services; thrown when the clinic's verification request isn't `DRAFT`/`NEEDS_CORRECTION`) |
| 409 | `INVALID_VERIFICATION_STATE` | `InvalidVeterinaryStatusTransitionException` (`Suspend`/`ReactivateVeterinary`, operates on `VeterinaryStatus`) **and** `InvalidVerificationStateException` (`domain/verification/exceptions`, Fase 2B Parte 2 — `SubmitClinicVerification`/`RetryClinicVerification`, operates on `VerificationRequest.status`); two exception classes share this code on purpose, since both describe "wrong state for this transition" on different aggregates |
| 409 | `VERIFICATION_ATTEMPTS_EXHAUSTED` | `VerificationAttemptsExhaustedException` (`RetryClinicVerification` — 3rd `REJECTED` attempt) |
| 409 | `EMPLOYEE_ALREADY_EXISTS` ? | `EmployeeAlreadyExistsException` |
| 409 | `ACTIVE_SUBSCRIPTION_ALREADY_EXISTS` ? | `ActiveSubscriptionAlreadyExistsException` |
| 409 | `USER_ALREADY_LINKED` ? | `UserAlreadyLinkedException` |
| 409 | `DOCUMENT_NOT_IN_CORRECTION` | `DocumentNotInCorrectionException` (`domain/shared/exceptions`, Fase 2B Parte 4A — uploading/deleting a document type that wasn't requested while the request is `NEEDS_CORRECTION`; used by both the clinic and professional document services via the shared `CorrectionGate`) |
| 409 | `AI_VERIFICATION_DISABLED` | `AiVerificationDisabledException` (`domain/verification/exceptions`, Fase 2B Parte 4A — `RerunVerificationAnalysis` when `IaVerificationPort.isEnabled()` is `false`) |
| 413 | `FILE_TOO_LARGE` | `FileTooLargeException` (`domain/shared/exceptions` — logo >2MB, verification document >10MB) |
| 503 | `EXTERNAL_SERVICE_UNAVAILABLE` | `ExternalServiceUnavailableException` (`domain/shared/exceptions`, thrown by `SupabaseStorageAdapter` on any Supabase Storage REST failure) |
| 500 | `INTERNAL_ERROR` | generic catch-all |

Since **Fase 2B Parte 3**, also wired: `PROFESSIONAL_PROFILE_NOT_FOUND` (404, `ProfessionalProfileNotFoundException`, `domain/professional/exceptions`) and `LICENSE_NUMBER_TAKEN` (409, `LicenseNumberAlreadyExistsException`, `domain/professional/exceptions`). Since **Fase 2B Parte 4A**, `REGISTRY_CHECK_REQUIRED` is wired too (admin `approve`) — every code listed in `FASE_2B_VERIFICACION.md` §8 now maps to a real exception.

### `.gitignore` gotcha: don't add a bare `out/` rule

`.gitignore` ignores `/out/` (anchored to the repo root, for IntelliJ's module build output) rather than a bare `out/`. A bare `out/` pattern matches **any** directory named `out` anywhere in the tree, which would silently untrack every `application/<module>/ports/out/*RepositoryPort.java` file (confirmed: this exact bug previously kept all 4 of those port interfaces out of git history entirely, invisible to `git status` since ignored paths don't show as untracked). If you ever see a `*RepositoryPort.java` missing from `git log`/`git show HEAD:...`, suspect this pattern regressing.

### Domain model relationships

- `Veterinary` — the clinic; has `tenantId`, a persisted `status` (`VeterinaryStatus`) kept in sync with the legacy `active` flag, invite fields (`inviteCode`, `inviteLink`) generated on demand, legal/profile fields (`legalName`, `nit: Nit`, `address`, `department`, `latitude`/`longitude`), per-clinic settings (`timezone`, `currency`, `defaultAppointmentMinutes`, `allowOnlineBooking`, `bookingRequiresConfirmation`, `cancellationMinHours`, `directoryVisible`), `createdBy`/`approvedAt` (VET-09/10), and since **Fase 2B Parte 2**: legal-representative fields (`legalRepIsOwner`, `legalRepName`, `legalRepDocumentType`, `legalRepDocumentNumber` — plain `String`s, not an enum, validated in `UpdateLegalRepresentativeService`) and `logoUrl`. Since **Fase 2B Parte 4A** (`006_admin_review.sql`): `suspendedAt`/`suspensionReason` — columns exist and are mapped end to end, but no service writes them yet; that's Fase 2B Parte 4B (admin clinic panel).
- `Nit` (`domain/clinic/model`) — Colombian tax ID value object, format `123456789-0`. `Nit.of(raw)` validates format + DIAN check digit (throws `InvalidNitException`); `Nit.fromPersisted(value)` skips validation for data already in the DB (used by `VeterinaryMapper` so a row that predates validation never fails to load).
- `VeterinaryEmployee` — staff member (`veterinaryId` + `userId` + `EmployeeRole` + `active`). Exactly one active `OWNER` per clinic (DB constraint `ux_one_owner_per_vet`, VET-09).
- `VeterinarySubscription` — a plan period (`veterinaryId`, `plan`, `startDate`/`endDate`, `active`); only one active subscription per veterinary is allowed at a time (enforced in `CreatePlanService`).
- `UserVeterinaryLink` — join between an app user and a clinic, created via invite code/URL (`LinkByCodeService` / `LinkByUrlService`), removable via `UnlinkService`.
- `LegalConsent` (`domain/legal/model`) — one row per acceptance of `DATA_TREATMENT` or `TERMS` (`userId`, optional `veterinaryId`, `version`, `acceptedAt`). Written by `RegisterVeterinaryService` (Fase 2B Parte 2) — two rows per registration, versions validated against `app.legal.data-policy-version`/`terms-version`.
- `ProfessionalProfile` (`domain/professional/model`) — the vet's professional license, one per **person** (`userId` unique, not per employment — `ux_professional_profiles_user`), with its own `licenseStatus` (`NOT_SUBMITTED`/`PENDING`/`VERIFIED`/`REJECTED`) independent of any clinic's verification. Since **Fase 2B Parte 3**, `UpsertProfessionalProfileService` creates/updates it (and `UploadProfessionalSignatureService` writes `signatureUrl`, always a storage *path*, never a public or signed URL); `GetVeterinaryMemberService` reads it to decide the internal `licensed` flag (`true` only when `licenseStatus == VERIFIED`). Since **Fase 2B Parte 4A**, `ApproveVerificationService`/`RejectVerificationService`/`RequestVerificationCorrectionService` are what actually move `licenseStatus` to `VERIFIED`/`REJECTED`/`NOT_SUBMITTED` and set `verifiedAt`/`verifiedBy`/`rejectionReason`.
- `VerificationRequest` / `VerificationDocument` (`domain/verification/model`) — a request is either `subjectType=CLINIC` (`subjectId`=`veterinary.id`) or `PROFESSIONAL` (`subjectId`=`professional_profiles.id`); at most one non-final (`DRAFT`/`UNDER_REVIEW`/`NEEDS_CORRECTION`) request per subject (`ux_verification_requests_open_subject`). `VerificationRequest.aiReport` is a raw JSON string (`jsonb`, mapped via Hibernate's `@JdbcTypeCode(SqlTypes.JSON)`), written only by `VerificationAiAnalysisListener` (Fase 2B Parte 4A) — never by the submit/decision services themselves; `correctionDocumentTypes` is a `text[]` (`@JdbcTypeCode(SqlTypes.ARRAY)`), written by `RequestVerificationCorrectionService` (Fase 2B Parte 4A) and read by `VerificationDocumentStatusCalculator`/`CorrectionGate` to compute a document's status and gate uploads/deletes. `VerificationRequest.rejectionReasonCode` (`006_admin_review.sql`, Fase 2B Parte 4A, nullable) is set by `RejectVerificationService`. `RegisterVeterinaryService` creates the clinic's first `CLINIC` request (`DRAFT`, `attemptNumber=1`); `UpsertProfessionalProfileService` creates the first `PROFESSIONAL` one the same way. `VerificationDocument.originalFileName` (Fase 2B Parte 3, `005_original_file_name.sql`, nullable — existing rows stay `NULL`) holds the sanitized client-supplied file name (`infrastructure/storage/FileNameSanitizer` — strips any path, caps at 255 chars); `VerificationDocument.displayFileName()` returns it when present, else falls back to the generated `{documentId}.<ext>` from `storagePath` — this is what every `fileName` field in API responses actually calls, never the raw path. `ClinicDocumentType` (`RUT`/`CHAMBER_OF_COMMERCE`/`LEGAL_REP_ID` required, `FACADE_PHOTO` optional) and `ProfessionalDocumentType` (`PROFESSIONAL_CARD`/`ID_DOCUMENT` required, `DEGREE` optional, Fase 2B Parte 3) are the two `domain/verification/enums` that drive each upload/submit service's required-document checks.

### Key business rules worth knowing before changing a service

- `RegisterVeterinaryService`: validates name/email/NIT format, enforces unique name/email/NIT, generates a random `tenantId`; creates the caller (from the token) as an active `OWNER` `VeterinaryEmployee` of the new clinic, status `PENDING_DOCUMENTS` (`active=false`), in the same `@Transactional` method. Caps a user at 3 owned clinics not in status `REJECTED`. Since Fase 2B Parte 2, same transaction also validates the two consent versions, saves the two `LegalConsent` rows, creates the first `DRAFT` `VerificationRequest`, and publishes `VETERINARY_REGISTERED`.
- `GenerateInviteCodeService`: generates a unique `VET-XXXXXXXX` code (`SecureRandom`, retries on collision) and builds `inviteLink` from `app.base-url`. `DeleteInviteCodeService` (VET-12) clears both fields back to `null`.
- `LinkByCodeService` / `LinkByUrlService`: reject if the clinic is inactive (`active=false`, i.e. not `ACTIVE`) or the user is already linked.
- `CreatePlanService`: rejects if the clinic is inactive or already has an active subscription; `UpdatePlanService`/`GetActivePlanService`/`IsExpiredService` operate on the single active (or latest) subscription per clinic.
- `ActivateEmployeeService` is a simple state toggle via `toBuilder()`. `UpdateEmployeeRoleService`/`DeActivateEmployeeService` additionally re-fetch the acting employee fresh from the DB (via the token's `employeeId`) and block self-modification, modifying the `OWNER`, or an `ADMIN` modifying another `ADMIN` — see the permission table above.
- `SuspendVeterinaryService`/`ReactivateVeterinaryService` (VET-13): status-transition guards (`InvalidVeterinaryStatusTransitionException` on an invalid `from` state) plus `VeterinaryStatus.impliesActiveFlag()` to keep `active` in sync. `GetVeterinaryProfileService`/`UpdateVeterinarySettingsService`/`GetMyVeterinariesService` (VET-12) are straightforward reads/partial-updates behind the permission checks in the table above.
- `UploadLogoService`/`UploadVerificationDocumentService` (Fase 2B Parte 2): both sniff the file's real type from its leading bytes (`FileSignatureDetector`, `infrastructure/storage`) and ignore the client-supplied `Content-Type` for that check; both reject before ever calling `FileStoragePort` if the type/size is wrong (no wasted upload). `UploadVerificationDocumentService` additionally computes `sha256` (`infrastructure/storage/Sha256`) and, on a DB save failure *after* the file is already uploaded, deletes the just-uploaded file as compensation (so a failed document-row insert never leaves an orphaned private file).
- `GetClinicVerificationService`: computes each `ClinicDocumentType`'s status (`PENDING`/`READY`/`NEEDS_CORRECTION`) by cross-referencing `VerificationRequest.correctionDocumentTypes`/`correctionRequestedAt` against each document's `uploadedAt` — never trusts a document's mere presence as "fixed" if it was uploaded *before* the correction was requested. `canSubmit` requires `canEdit` (status `DRAFT`/`NEEDS_CORRECTION`) **and** a complete legal representative **and** all 3 required documents present.
- `UpsertProfessionalProfileService` (Fase 2B Parte 3): on create, always makes a fresh `DRAFT` `VerificationRequest` (`attemptNumber=1`). On update: `PENDING` blocks every field unconditionally; `VERIFIED` only resets to `NOT_SUBMITTED` (+ new `DRAFT`, `attemptNumber+1`) when an *identity* field changes (`fullName`/`documentType`/`documentNumber`/`profession`/`licenseNumber` — `specialty`/`university` never trigger this); `REJECTED` always starts a new attempt on any edit (capped at 3, same as the clinic). `licenseNumber` uniqueness excludes the caller's own row (`existsByLicenseNumberAndUserIdNot`) so re-saving your own unchanged license number never 409s.
- `ProfessionalProfileSubmissionService` (Fase 2B Parte 3, `application/professional/services`) is **not** a `*UseCase` implementation — it's the shared DRAFT/NEEDS_CORRECTION → UNDER_REVIEW transition (`license_status → PENDING`, requires `PROFESSIONAL_CARD`+`ID_DOCUMENT`) called by both `SubmitProfessionalProfileService` (the standalone endpoint) and `SubmitClinicVerificationService` (`includeProfessionalProfile=true`) — written once so both paths enforce identical rules and so the joint-submission call naturally joins whatever `@Transactional` the caller is already in (no `@Transactional` on this class itself).
- `UploadLogoService`/`UploadVerificationDocumentService`'s file-signature/size pattern (see above) is reused as-is by `UploadProfessionalDocumentService` (`ProfessionalDocumentType` instead of `ClinicDocumentType`) and `UploadProfessionalSignatureService` (PNG only, ≤1MB, always allowed regardless of request status — a signature isn't gated by `DRAFT`/`NEEDS_CORRECTION` like the other documents are).
- `CorrectionGate` (`application/verification/services`, Fase 2B Parte 4A, plain static utility — not a port) is the single place that enforces "while `NEEDS_CORRECTION`, only upload/delete a document type that's actually in `correction_document_types`" (`DocumentNotInCorrectionException`, 409); called identically by `UploadVerificationDocumentService`/`DeleteVerificationDocumentService` (clinic) and `UploadProfessionalDocumentService`/`DeleteProfessionalDocumentService` (professional).
- `VerificationDocumentStatusCalculator` (`application/verification/services`, Fase 2B Parte 4A, also a static utility) computes the `PENDING`/`READY`/`NEEDS_CORRECTION`/`REVIEWED` status shown to the clinic/professional for each document type; used by both `GetClinicVerificationService` and `GetMyProfessionalProfileService` so the two `GET` endpoints never drift from each other on this logic.
- `ApproveVerificationService`/`RejectVerificationService`/`RequestVerificationCorrectionService` (Fase 2B Parte 4A) all follow the same shape: `requirePlatformAdmin()` → validate the body → load the request and require it's `UNDER_REVIEW` (`InvalidVerificationStateException`, 409) → reject self-review (`submitted_by == caller`, `CannotReviewOwnRequestException`, 403) → **atomically claim it** via `VerificationRequestRepositoryPort.claimForDecision(id, UNDER_REVIEW, <newStatus>)` (a conditional `UPDATE ... WHERE id = ? AND status = 'UNDER_REVIEW'`; 0 rows updated means another admin already decided it in between → `InvalidVerificationStateException`, 409) → only then save the rest of the fields and apply the subject-specific effect (clinic/profile) → publish the matching event. This claim step is what actually prevents the race two admins can hit by both loading the same `UNDER_REVIEW` request before either decides.
- `VerificationAiAnalysisListener` (`application/verification/services`, Fase 2B Parte 4A) is a `@Component` (not a `*Service`/`*UseCase`) listening for the internal `VerificationSubmittedEvent` via `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` (enabled by `infrastructure/config/AsyncConfig`'s `@EnableAsync`). `SubmitClinicVerificationService`, `ProfessionalProfileSubmissionService`, and `RerunVerificationAnalysisService` all publish the same event via Spring's `ApplicationEventPublisher` (framework-level, unrelated to `EventPublisherPort`/Pub-Sub). If `IaVerificationPort.isEnabled()` is `false` the listener does nothing at all (not even a DB read) — `ai_status` stays `NOT_RUN` forever until the IA is turned on. When enabled: sets `RUNNING`, calls `analyze(...)` with each document as a 10-minute signed URL, then `DONE` (with `aiReport`/`aiRiskLevel`) or `FAILED` on any exception — never touches `VerificationRequest.status`. An unexpected `recommendation` value (anything other than `READY_FOR_HUMAN_REVIEW`/`NEEDS_ATTENTION`) is logged at `WARN` and otherwise ignored; document content and extracted data are never logged.

## Almacenamiento de archivos (Supabase Storage)

Infraestructura lista desde **Fase 2B Parte 1**; `UploadLogoService`/`DeleteLogoService` (clinic logo, bucket público) y `UploadVerificationDocumentService`/`DeleteVerificationDocumentService` (documentos de verificación, bucket privado) la usan desde **Fase 2B Parte 2** — ver la tabla de permisos arriba. Desde **Fase 2B Parte 4A**, `createSignedUrl` también la llaman `GetAdminVerificationDetailService`/`RenewVerificationDocumentSignedUrlService` (URLs de 10 min para el visor del admin) y `VerificationAiAnalysisListener` (URLs de 10 min que se mandan a la IA en vez de los archivos).

- **Puerto**: `FileStoragePort` (`application/shared/ports/out`) — `uploadPublic(bucket, path, bytes, contentType)` devuelve la URL pública; `uploadPrivate(...)` no devuelve nada (el archivo solo se lee después con una URL firmada); `createSignedUrl(bucket, path, ttl)` devuelve una URL temporal; `delete(bucket, path)`.
- **Adaptador**: `SupabaseStorageAdapter` (`infrastructure/storage`), sobre `RestClient` (Spring, no `RestTemplate`) contra la API REST de Supabase Storage (`${supabase.url}/storage/v1`), autenticado con `${supabase.key}` (el service role que ya usa el resto de la app — nunca las claves anon/auth) en las cabeceras `apikey` y `Authorization: Bearer`. El `POST /object/...` de subida manda además `x-upsert: true` (Fase 2B Parte 2) para poder resubir al mismo `path` sin que Supabase lo rechace con 409 — necesario para el logo cuando la extensión no cambia (ver `UploadLogoService`). **La key nunca se loguea** — los `catch` solo registran `bucket`/`path`, nunca el cuerpo de la petición ni las cabeceras. Cualquier fallo de red o respuesta de error de Supabase se traduce a `ExternalServiceUnavailableException` (`domain/shared/exceptions`, 503, `code=EXTERNAL_SERVICE_UNAVAILABLE`).
- **Buckets**: `app.storage.logos-bucket` (default `veterinary-logos`, público) y `app.storage.verification-bucket` (default `verification-documents`, privado — solo URLs firmadas de corta duración, nunca una URL pública directa). Configurables vía `StorageProperties` (`@ConfigurationProperties(prefix = "app.storage")`); se crean a mano en el panel de Supabase, no por código.
- **`FileSignatureDetector`** (`infrastructure/storage`, Fase 2B Parte 2): `detect(byte[])` identifica `PNG`/`JPEG`/`WEBP`/`PDF` por los primeros bytes del archivo (magic numbers) — nunca por `Content-Type`, que el cliente controla y puede mentir. Usado por `UploadLogoService` (solo `PNG`/`JPEG`/`WEBP`) y `UploadVerificationDocumentService` (solo `PDF`/`JPEG`/`PNG`) antes de llamar a `FileStoragePort`.
- **`Sha256`** (`infrastructure/storage`): utilidad `hex(byte[])` para calcular el hash de un archivo subido — se guarda en `verification_documents.sha256` para detectar el mismo archivo reusado en otra clínica (regla 3 de `FASE_2B_VERIFICACION.md` §6; es una alerta informativa, no bloquea).
- **Tests**: `SupabaseStorageAdapterTest` levanta un `com.sun.net.httpserver.HttpServer` real en un puerto libre (no llama a Supabase) y verifica método/ruta/cuerpo de cada operación más el mapeo a `ExternalServiceUnavailableException` cuando el servidor fake responde 5xx.

## Base de datos

El esquema vive en Supabase/Postgres (`public`) y se administra **a mano** en el SQL Editor de Supabase — no hay Flyway ni ningún otro motor de migraciones. `spring.jpa.hibernate.ddl-auto` se mantiene en `none` en todos los perfiles salvo el de tests.

### Esquema real actual

- **`veterinary`**: `id uuid PK`, `name varchar NOT NULL`, `city varchar NOT NULL`, `phone varchar NULL`, `email varchar NULL UNIQUE`, `invite_code varchar NULL UNIQUE`, `invite_link text NULL`, `tenant_id varchar NOT NULL`, `active bool NOT NULL` (compatibilidad, ver nota de `VeterinaryStatus` arriba), `created_at timestamp NOT NULL` (sin zona horaria), `updated_at timestamptz NULL`. Desde `001_clinic_status_and_profile.sql` (VET-09/10/12) además: `status varchar(30) NOT NULL DEFAULT 'PENDING_DOCUMENTS'`, `legal_name varchar NULL`, `nit varchar(20) NULL UNIQUE parcial` (índice único solo `WHERE nit IS NOT NULL`), `address varchar NULL`, `department varchar NULL`, `latitude/longitude numeric(9,6) NULL`, `timezone varchar NOT NULL DEFAULT 'America/Bogota'`, `currency varchar(3) NOT NULL DEFAULT 'COP'`, `default_appointment_minutes int NOT NULL DEFAULT 30`, `allow_online_booking/booking_requires_confirmation/directory_visible bool NOT NULL DEFAULT true`, `cancellation_min_hours int NOT NULL DEFAULT 4`, `created_by uuid NULL`, `approved_at timestamptz NULL`. Desde `004_verification.sql` (Fase 2B Parte 1) además: `legal_rep_is_owner bool NULL`, `legal_rep_name varchar(200) NULL`, `legal_rep_document_type varchar(10) NULL` (CHECK `IN ('CC','CE','PPT')` o `NULL`), `legal_rep_document_number varchar(30) NULL`, `logo_url text NULL`. Desde `006_admin_review.sql` (Fase 2B Parte 4A) además: `suspended_at timestamptz NULL`, `suspension_reason text NULL` (mapeadas en `VeterinaryEntity`/`VeterinaryMapper`, sin uso todavía — las escribirá la Fase 2B Parte 4B). RLS activado desde `003_enable_rls.sql`.
- **`veterinary_employee`**: `id uuid PK`, `veterinary_id uuid FK → veterinary`, `user_id uuid`, `role varchar` (CHECK `IN ('OWNER','ADMIN','VETERINARIAN','ASSISTANT','RECEPTIONIST')` desde `002_employee_roles_and_ownership.sql`), `active bool`, `created_at timestamptz`. Todas `NOT NULL`. Índices únicos: `ux_one_owner_per_vet` (parcial, `WHERE role='OWNER'`, un solo OWNER por clínica) y `ux_employee_user_per_vet` (`veterinary_id, user_id`) — hay además un `uq_veterinary_employee` preexistente que probablemente duplica a este último, pendiente de limpieza (ver `db/scripts/README.md`). RLS activado desde `003_enable_rls.sql`.
- **`veterinary_subscription`**: `id uuid PK`, `veterinary_id uuid FK → veterinary`, `plan varchar`, `start_date date`, `end_date date`, `active bool`, `created_at timestamptz`. Todas `NOT NULL`.
- **`user_veterinary_link`**: `id uuid PK`, `user_id uuid`, `veterinary_id uuid FK → veterinary`, `status varchar`, `linked_at timestamptz`. Todas `NOT NULL`.
- **`legal_consents`** (desde `004_verification.sql`, Fase 2B Parte 1): `id uuid PK DEFAULT gen_random_uuid()`, `user_id uuid NOT NULL`, `veterinary_id uuid NULL FK → veterinary`, `consent_type varchar(20) NOT NULL` (CHECK `IN ('DATA_TREATMENT','TERMS')`), `version varchar(20) NOT NULL`, `accepted_at timestamptz NOT NULL DEFAULT now()`. RLS activado. Sin uso todavía (entidad/mapper/repositorio listos, sin caso de uso).
- **`professional_profiles`** (desde `004_verification.sql`): `id uuid PK DEFAULT gen_random_uuid()`, `user_id uuid NOT NULL` (único — la licencia es de la persona, no del empleo), `full_name varchar(200) NOT NULL`, `document_type varchar(10) NOT NULL` (CHECK `IN ('CC','CE','PPT')`), `document_number varchar(30) NOT NULL`, `license_number varchar(50) NOT NULL UNIQUE`, `profession varchar(10) NOT NULL` (CHECK `IN ('MV','MVZ')`), `specialty varchar(120) NULL`, `university varchar(200) NULL`, `signature_url text NULL`, `license_status varchar(20) NOT NULL DEFAULT 'NOT_SUBMITTED'` (CHECK `IN ('NOT_SUBMITTED','PENDING','VERIFIED','REJECTED')`), `rejection_reason text NULL`, `verified_at timestamptz NULL`, `verified_by uuid NULL`, `created_at/updated_at timestamptz NOT NULL DEFAULT now()`. RLS activado. En uso desde Fase 2B Parte 3 (`UpsertProfessionalProfileService`); `rejection_reason`/`verified_at`/`verified_by` los escribe la Parte 4A (`ApproveVerificationService`/`RejectVerificationService`).
- **`verification_requests`** (desde `004_verification.sql`): `id uuid PK DEFAULT gen_random_uuid()`, `subject_type varchar(20) NOT NULL` (CHECK `IN ('CLINIC','PROFESSIONAL')`), `subject_id uuid NOT NULL`, `status varchar(30) NOT NULL` (CHECK `IN ('DRAFT','UNDER_REVIEW','NEEDS_CORRECTION','APPROVED','REJECTED')`), `attempt_number int NOT NULL DEFAULT 1`, `submitted_by uuid NOT NULL`, `submitted_at timestamptz NULL`, `truthfulness_declared_at timestamptz NULL`, `ai_status varchar(20) NOT NULL DEFAULT 'NOT_RUN'` (CHECK `IN ('NOT_RUN','RUNNING','DONE','FAILED')`), `ai_report jsonb NULL`, `ai_risk_level varchar(10) NULL` (CHECK `IN ('LOW','MEDIUM','HIGH')` o `NULL`), `reviewer_id uuid NULL`, `reviewer_notes text NULL`, `decided_at timestamptz NULL`, `registry_checked bool NOT NULL DEFAULT false`, `correction_document_types text[] NULL`, `correction_requested_at timestamptz NULL`, `created_at timestamptz NOT NULL DEFAULT now()`. Desde `006_admin_review.sql` (Fase 2B Parte 4A) además: `rejection_reason_code varchar(40) NULL` (CHECK `IN ('NOT_IN_REGISTRY','DATA_MISMATCH','DOCUMENT_INVALID','OTHER')` o `NULL`). Índice único `ux_verification_requests_open_subject` (`subject_type, subject_id` `WHERE status IN ('DRAFT','UNDER_REVIEW','NEEDS_CORRECTION')`) — máximo una solicitud no final por sujeto. RLS activado. En uso desde Fase 2B Parte 2 (clínica) y Parte 3 (perfil); `ai_*`/`reviewer_*`/`decided_at`/`registry_checked`/`correction_*`/`rejection_reason_code` los escribe la Parte 4A.
- **`verification_documents`** (desde `004_verification.sql`): `id uuid PK DEFAULT gen_random_uuid()`, `request_id uuid NOT NULL FK → verification_requests ON DELETE CASCADE`, `document_type varchar(40) NOT NULL`, `storage_path text NOT NULL`, `mime_type varchar(100) NOT NULL`, `file_size_bytes bigint NOT NULL`, `sha256 varchar(64) NOT NULL`, `uploaded_at timestamptz NOT NULL DEFAULT now()`. Desde `005_original_file_name.sql` (Fase 2B Parte 3) además: `original_file_name varchar(255) NULL`. Índice único `(request_id, document_type)`. RLS activado. En uso desde Fase 2B Parte 2.

Nota: los modelos de dominio (`domain/model`) no exponen todas estas restricciones (p. ej. nulabilidad) — son el esquema simplificado en memoria, no un espejo 1:1 de las columnas. Antes de asumir que una columna es `NOT NULL` o nullable, confía en esta tabla, no en el `@Data`/`@Builder` del modelo.

### Reglas de `db/scripts/`

Ver `db/scripts/README.md` para la convención completa (numeración `NNN_descripcion.sql`, cabecera con tarea/fecha/qué hace, idempotencia, scripts inmutables una vez ejecutados, tabla de registro de ejecución).

### Regla para Claude Code

Cuando una tarea necesite cambiar el esquema: escribe el script en `db/scripts/`, actualiza las entidades (`infrastructure/entity`) y mappers (`infrastructure/mapper`) que correspondan, y **detente** — no asumas que el script ya fue ejecutado en Supabase ni intentes correr la app o los tests contra el esquema nuevo hasta que el usuario confirme que lo aplicó manualmente en el SQL Editor.

**Columnas `NOT NULL` con `DEFAULT` en la base**: Hibernate manda el valor de Java tal cual al `INSERT` — si el campo del modelo de dominio es `null`, inserta `NULL` explícito y el `DEFAULT` de Postgres **nunca se aplica** (confirmado en producción: `RegisterVeterinary` falló con `null value in column ai_status violates not-null constraint` porque `VerificationRequest.aiStatus` no tenía valor por defecto en Java, aunque la columna sí lo tiene en `004_verification.sql`). Por eso toda columna `NOT NULL DEFAULT ...` necesita su valor replicado en el **modelo de dominio**, con `@Builder.Default` (no alcanza con anotarlo solo en la `*Entity` — el modelo de dominio es lo que el servicio realmente construye). Ver `VerificationRequest`/`VerificationDocument`/`LegalConsent`/`ProfessionalProfile` para el patrón, y `Fase2BNewTablesPersistenceIntegrationTest` (test de integración contra Supabase real, con rollback automático) que guarda cada una con solo los campos mínimos para detectar esto a futuro.

## Eventos (Google Cloud Pub/Sub)

Infraestructura de eventos según el sobre de `CONTRATOS_COMPARTIDOS.md §4`. **Todavía ningún caso de uso publica eventos** — VET-04 solo deja listas las piezas; la emisión real desde los servicios llega en tareas posteriores.

- **Dependencia**: `com.google.cloud:spring-cloud-gcp-starter-pubsub`, versión gestionada por el BOM `com.google.cloud:spring-cloud-gcp-dependencies:7.4.10` (serie `7.x`, la compatible con Spring Boot `3.5.x` según la tabla de compatibilidad oficial del repo `GoogleCloudPlatform/spring-cloud-gcp`).
- **Puerto**: `EventPublisherPort` (`application/shared/ports/out`) — `void publish(DomainEvent event)`.
- **Sobre** (`application/shared/dto`): `DomainEvent` (record: `eventType`, `userId`, `recipient`, `payload`, `occurredAt`, `metadata`), `Recipient` (`email`/`phone`/`name`), `EventMetadata` (record completo, más `EventMetadata.of(veterinaryId, correlationId)` que fija `version="1.0"` y `source="veterinary-service"`). `DomainEvent.create(...)` es el factory: lanza `IllegalArgumentException` si `userId` y `recipient` son ambos `null` (evento inválido — regla de `CONTRATOS_COMPARTIDOS.md §4.1`: nunca se publica, se loguea en el emisor).
- **Tipos de evento**: `VeterinaryEventType` (`domain/shared/enums`), los 22 eventos de `VETERINARY_SERVICE_PLAN.md §11` (de `VETERINARY_REGISTERED` a `PAYMENT_FAILED`).
- **No confundir con `VerificationSubmittedEvent`** (`application/verification/dto`, Fase 2B Parte 4A): es un evento interno de Spring (`ApplicationEventPublisher.publishEvent(...)`), no un `DomainEvent`/Pub-Sub — solo dispara el análisis de IA async (`VerificationAiAnalysisListener`) dentro del mismo proceso, nunca sale del servicio.
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

**Testing `@Async`/`@TransactionalEventListener` code** (Fase 2B Parte 4A, `VerificationAiAnalysisListener`): tests call the listener's method directly as a plain object (`new VerificationAiAnalysisListener(...).onVerificationSubmitted(event)`), never through a Spring context — a direct Java call runs synchronously in the test thread regardless of the `@Async`/`@TransactionalEventListener` annotations on the method, so there's nothing async or transactional to wait for or mock. The services that publish `VerificationSubmittedEvent` (`SubmitClinicVerificationService`, `ProfessionalProfileSubmissionService`, `RerunVerificationAnalysisService`) just mock `ApplicationEventPublisher` and verify `publishEvent(...)` was called with the right event — they don't need the listener on the classpath at all.

**Mockito-only service tests cannot catch DB-level bugs** (wrong column mapping, a missing `NOT NULL`/`DEFAULT` value — see the rule above) since the real adapter/Hibernate/Postgres layer is never exercised. For that, add a case to `Fase2BNewTablesPersistenceIntegrationTest` (`@SpringBootTest @ActiveProfiles("test") @Transactional`, saves through the real `*RepositoryPort` → `*Mapper` → `*Entity` → Hibernate → Supabase, auto-rolled-back) when adding a table/column — or start a new similarly-named class if the scope stops fitting that one.
