# Plan de desarrollo — veterinary-service (MyAnimaLogVet)

> Documento para el repo `veterinary-service`. Léelo junto con `CLAUDE.md` (arquitectura actual) y `CONTRATOS_COMPARTIDOS.md` (JWT, permisos, eventos).
> Las tareas tienen ID (`VET-xx`). Las que dependen de otro repo lo indican (`requiere USER-03`).
> Mantén las convenciones del repo: hexagonal, un caso de uso por operación (`<Verb><Noun>UseCase` / `Service` / `Controller` / `Request` / `Response`), una excepción de dominio por regla, handler en `GlobalExceptionHandler`, un test por clase.

---

## 0. Qué es este servicio y qué NO es

**Es dueño de:** la clínica (tenant), su verificación legal, su personal y roles, el perfil profesional de los veterinarios, la vinculación con dueños, el consentimiento por mascota, el catálogo de servicios, los horarios y la suscripción SaaS.

**NO es dueño de (y nunca guarda copia de):**

| Dato | Dueño | Cómo lo obtiene Veterinary |
|---|---|---|
| Cuenta, nombre, email del usuario | User service | `UserServicePort` (HTTP interno) |
| Mascota, foto, documentos | Pets | `PetsServicePort` (HTTP interno) |
| Historia clínica (visitas, vacunas, cirugías…) | Medical | No la lee. El front la pide a Medical, y Medical le pregunta permisos a Veterinary |
| Citas | Calendar (ver §10) | `CalendarServicePort` o módulo propio |
| Envío de emails/push/WhatsApp | Notifications | Publica eventos a Pub/Sub |
| Análisis de documentos con IA | IA service | `IaVerificationPort` (HTTP interno) |

Excepción deliberada: `veterinary_patients` guarda un **snapshot** de `pet_name`, `owner_name`, `owner_phone` solo para poder buscar pacientes rápido (§7.3). Se refresca al compartir y al registrar.

---

## 1. Qué pasa con lo que ya existe

| Caso de uso actual | Decisión | Detalle |
|---|---|---|
| `RegisterVeterinary` | 🔧 Modificar | Toma el creador del token, crea al creador como empleado `OWNER`, estado inicial `PENDING_DOCUMENTS`, campos nuevos (NIT, dirección, coordenadas…) |
| `GenerateInviteCode` | ✅ Mantener | Agregar `RevokeInviteCode` |
| `Activate` / `DeActivate` | 🔧 Mover | Pasan a `/admin/veterinaries/{id}/reactivate` y `/suspend`, solo `PLATFORM_ADMIN`. Operan sobre `status`, no sobre el booleano `active` |
| `UpdateVeterinary` | ✅ Mantener | Requiere `CLINIC_CONFIGURE` |
| `CreateVeterinaryEmployee` | 🔁 Reemplazar | Deja de ser endpoint público. Se reemplaza por invitaciones (§6). El servicio interno se reutiliza al aceptar una invitación |
| `UpdateEmployeeRole` | 🔧 Modificar | Requiere `STAFF_MANAGE`. No se puede asignar `OWNER` por aquí. `ADMIN` no puede modificar a un `OWNER` |
| `ActivateEmployee` / `DeactivateEmployee` | 🔧 Modificar | Requiere `STAFF_MANAGE`. El `OWNER` no se puede desactivar |
| `GetActivePlan` / `IsExpired` | ✅ Mantener | Requiere ser miembro de la clínica |
| `CreatePlan` / `UpdatePlan` | 🔧 Mover | Pasan a `/admin/**` (para dar planes manuales a pilotos). La compra normal va por checkout (§9) |
| `LinkByCode` / `LinkByUrl` | 🔁 Unificar | Un solo `LinkToVeterinary` que recibe el código. La URL solo lleva el código dentro |
| `Unlink` | 🔧 Modificar | Desde ambos lados. Al desvincular se revocan todas las mascotas compartidas |

---

## 2. Estructura de paquetes objetivo

Reorganizar por módulo **antes** de agregar funcionalidades (VET-02). Dentro de cada módulo se conserva la división hexagonal.

```
com.myanimalog.veterinary
├── domain
│   ├── clinic/           Veterinary, VeterinaryStatus, VeterinarySettings, exceptions
│   ├── verification/     VerificationRequest, VerificationDocument, DocumentType, VerificationStatus
│   ├── professional/     ProfessionalProfile, LicenseStatus
│   ├── staff/            VeterinaryEmployee, EmployeeRole, EmployeeInvitation, InvitationStatus
│   ├── patients/         UserVeterinaryLink, VeterinaryPatient, PatientOrigin, ConsentStatus
│   ├── catalog/          VeterinaryService (servicio ofrecido), BusinessHours, EmployeeSchedule, ScheduleBlock
│   ├── subscription/     SubscriptionPlan, VeterinarySubscription, SubscriptionStatus, Payment
│   └── security/         Permission, RolePermissions   ← sin Spring
├── application
│   ├── <módulo>/ports/in, ports/out, services, dto     (mismo patrón actual, por módulo)
│   └── shared/ports/out  AuthenticatedUserPort, EventPublisherPort, FileStoragePort,
│                          UserServicePort, PetsServicePort, IaVerificationPort, CalendarServicePort
└── infrastructure
    ├── <módulo>/controllers, adapters, repositories, entity, mapper
    ├── security/          SecurityConfig, JwtClaimsConverter, InternalApiKeyFilter, SpringAuthenticatedUserAdapter
    ├── clients/           RestClient adapters hacia user, pets, ia, calendar
    ├── messaging/         GooglePubSubEventAdapter
    ├── storage/           SupabaseStorageAdapter (logo público, documentos privados)
    ├── jobs/              endpoints /internal/jobs/** para Cloud Scheduler
    └── config/            GlobalExceptionHandler, RestClientConfig, OpenApiConfig
```

---

## 3. Base de datos (scripts manuales)

El esquema se maneja a mano (`ddl-auto: none`) y así se mantiene: cada cambio es un script SQL numerado en `db/scripts/` que una persona ejecuta en el SQL Editor de Supabase. Convención completa en `db/scripts/README.md` y en la sección "Base de datos" de `CLAUDE.md`. No hay motor de migraciones automático ni baseline — cada script de los que siguen se crea cuando llega su tarea (`00N_descripcion.sql`), con la cabecera tarea/fecha/qué-hace, y el código (entidades/mappers) se actualiza en el mismo commit; luego hay que **detenerse** hasta que el script se ejecute en Supabase.

Nombres reales de las tablas existentes (ver `CLAUDE.md`): `veterinary`, `veterinary_employee`, `veterinary_subscription`, `user_veterinary_link` (todas en singular).

### 002_clinic_status_and_profile.sql

```sql
ALTER TABLE veterinary
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING_DOCUMENTS',
  ADD COLUMN IF NOT EXISTS legal_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS nit VARCHAR(20),
  ADD COLUMN IF NOT EXISTS address VARCHAR(250),
  ADD COLUMN IF NOT EXISTS department VARCHAR(100),
  ADD COLUMN IF NOT EXISTS latitude NUMERIC(9,6),
  ADD COLUMN IF NOT EXISTS longitude NUMERIC(9,6),
  ADD COLUMN IF NOT EXISTS logo_url TEXT,
  ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) NOT NULL DEFAULT 'America/Bogota',
  ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'COP',
  ADD COLUMN IF NOT EXISTS default_appointment_minutes INT NOT NULL DEFAULT 30,
  ADD COLUMN IF NOT EXISTS allow_online_booking BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS booking_requires_confirmation BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS cancellation_min_hours INT NOT NULL DEFAULT 4,
  ADD COLUMN IF NOT EXISTS directory_visible BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS created_by UUID,
  ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;

-- `city` y `phone` ya existen en la tabla real; no se tocan aquí.
-- Migrar el booleano existente (no idempotente: no reejecutar si ya corrió)
UPDATE veterinary SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'SUSPENDED' END;
CREATE UNIQUE INDEX IF NOT EXISTS ux_veterinary_nit ON veterinary(nit) WHERE nit IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_veterinary_city ON veterinary(city) WHERE status = 'ACTIVE';
```

### 003_employee_roles.sql

```sql
ALTER TABLE veterinary_employee DROP CONSTRAINT IF EXISTS veterinary_employee_role_check;
ALTER TABLE veterinary_employee ADD CONSTRAINT veterinary_employee_role_check
  CHECK (role IN ('OWNER','ADMIN','VETERINARIAN','ASSISTANT','RECEPTIONIST'));
-- exactamente un OWNER activo por clínica
CREATE UNIQUE INDEX IF NOT EXISTS ux_one_owner_per_vet ON veterinary_employee(veterinary_id) WHERE role = 'OWNER';
CREATE UNIQUE INDEX IF NOT EXISTS ux_employee_user_per_vet ON veterinary_employee(veterinary_id, user_id);
```

### 004_professional_profiles.sql

La licencia es de la **persona**, no del empleo: un veterinario que trabaja en dos clínicas se verifica una sola vez.

```sql
CREATE TABLE professional_profiles (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL UNIQUE,
  full_name VARCHAR(200) NOT NULL,
  document_type VARCHAR(10) NOT NULL,          -- CC, CE, PPT
  document_number VARCHAR(30) NOT NULL,
  license_number VARCHAR(50) NOT NULL,         -- número de tarjeta / matrícula profesional
  profession VARCHAR(10) NOT NULL,             -- MV, MVZ
  specialty VARCHAR(120),
  university VARCHAR(200),
  signature_url TEXT,                          -- firma para fórmulas y certificados (bucket privado)
  license_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED', -- NOT_SUBMITTED, PENDING, VERIFIED, REJECTED
  rejection_reason TEXT,
  verified_at TIMESTAMPTZ,
  verified_by UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_profile_license ON professional_profiles(license_number);
```

### 005_verification.sql

```sql
CREATE TABLE IF NOT EXISTS verification_requests (
  id UUID PRIMARY KEY,
  subject_type VARCHAR(20) NOT NULL,           -- CLINIC, PROFESSIONAL
  subject_id UUID NOT NULL,                    -- veterinary_id o professional_profile_id
  status VARCHAR(30) NOT NULL,                 -- DRAFT, UNDER_REVIEW, NEEDS_CORRECTION, APPROVED, REJECTED
  submitted_by UUID NOT NULL,
  submitted_at TIMESTAMPTZ,
  ai_status VARCHAR(20) NOT NULL DEFAULT 'NOT_RUN',  -- NOT_RUN, RUNNING, DONE, FAILED
  ai_report JSONB,                             -- respuesta del IA service, solo de apoyo
  ai_risk_level VARCHAR(10),                   -- LOW, MEDIUM, HIGH
  reviewer_id UUID,
  reviewer_notes TEXT,
  decided_at TIMESTAMPTZ,
  registry_checked BOOLEAN NOT NULL DEFAULT FALSE,  -- el admin confirmó en el portal oficial
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_verif_status ON verification_requests(status, submitted_at);

CREATE TABLE IF NOT EXISTS verification_documents (
  id UUID PRIMARY KEY,
  request_id UUID NOT NULL REFERENCES verification_requests(id) ON DELETE CASCADE,
  document_type VARCHAR(40) NOT NULL,
  storage_path TEXT NOT NULL,                  -- bucket PRIVADO, nunca URL pública
  mime_type VARCHAR(100) NOT NULL,
  file_size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,                 -- detectar el mismo archivo subido por dos clínicas
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Tipos de documento (`DocumentType`):

| Para | Documentos |
|---|---|
| `CLINIC` | `RUT`, `CHAMBER_OF_COMMERCE` (certificado de existencia y representación legal, o matrícula mercantil si es persona natural), `LEGAL_REP_ID` (cédula del representante), `FACADE_PHOTO` (opcional) |
| `PROFESSIONAL` | `PROFESSIONAL_CARD` (tarjeta o certificado de registro), `ID_DOCUMENT`, `DEGREE` (opcional) |

### 006_employee_invitations.sql

```sql
CREATE TABLE IF NOT EXISTS employee_invitations (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL,
  email VARCHAR(200) NOT NULL,
  role VARCHAR(20) NOT NULL,
  token UUID NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL,                 -- PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED
  invited_by UUID NOT NULL,
  accepted_user_id UUID,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pending_invite ON employee_invitations(veterinary_id, lower(email)) WHERE status = 'PENDING';
```

### 007_patients.sql

```sql
ALTER TABLE user_veterinary_link ADD COLUMN IF NOT EXISTS unlinked_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS veterinary_patients (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL,
  pet_id UUID NOT NULL,
  origin VARCHAR(20) NOT NULL,                 -- SHARED_BY_OWNER, REGISTERED_BY_CLINIC
  consent_status VARCHAR(20) NOT NULL,         -- GRANTED, REVOKED
  granted_by UUID,                             -- null si lo registró la clínica (dueño sin cuenta)
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at TIMESTAMPTZ,
  pet_name VARCHAR(120) NOT NULL,              -- snapshot para búsqueda
  species VARCHAR(50),
  owner_name VARCHAR(200),
  owner_phone VARCHAR(30),
  owner_email VARCHAR(200),
  UNIQUE (veterinary_id, pet_id)
);
CREATE INDEX IF NOT EXISTS ix_patients_search ON veterinary_patients
  USING gin (to_tsvector('spanish', coalesce(pet_name,'') || ' ' || coalesce(owner_name,'')));
CREATE INDEX IF NOT EXISTS ix_patients_phone ON veterinary_patients(veterinary_id, owner_phone);
```

### 008_catalog_schedule.sql

```sql
CREATE TABLE IF NOT EXISTS clinic_services (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(30) NOT NULL,               -- CONSULTATION, VACCINATION, SURGERY, GROOMING, LAB, OTHER
  duration_minutes INT NOT NULL CHECK (duration_minutes BETWEEN 5 AND 600),
  price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
  bookable_online BOOLEAN NOT NULL DEFAULT TRUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (veterinary_id, name)
);

CREATE TABLE IF NOT EXISTS business_hours (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL,
  day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  opens_at TIME NOT NULL,
  closes_at TIME NOT NULL CHECK (closes_at > opens_at)
);

CREATE TABLE IF NOT EXISTS employee_schedules (
  id UUID PRIMARY KEY,
  employee_id UUID NOT NULL,
  day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  starts_at TIME NOT NULL,
  ends_at TIME NOT NULL CHECK (ends_at > starts_at)
);

CREATE TABLE IF NOT EXISTS schedule_blocks (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL,
  employee_id UUID,                            -- null = bloquea toda la clínica (festivo)
  starts_at TIMESTAMPTZ NOT NULL,
  ends_at TIMESTAMPTZ NOT NULL CHECK (ends_at > starts_at),
  reason VARCHAR(200)
);
```

### 009_subscription_plans.sql

```sql
CREATE TABLE IF NOT EXISTS subscription_plans (
  code VARCHAR(30) PRIMARY KEY,                -- FREE, BASIC, PRO
  name VARCHAR(60) NOT NULL,
  monthly_price NUMERIC(12,2) NOT NULL,
  yearly_price NUMERIC(12,2) NOT NULL,
  max_employees INT,                           -- null = ilimitado
  max_patients INT,
  features JSONB NOT NULL,                     -- ["ONLINE_BOOKING","WHATSAPP","REPORTS","AI_SUMMARY"]
  active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE veterinary_subscription
  ADD COLUMN IF NOT EXISTS plan_code VARCHAR(30) REFERENCES subscription_plans(code),
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- TRIAL, ACTIVE, GRACE, EXPIRED, CANCELLED
  ADD COLUMN IF NOT EXISTS billing_period VARCHAR(10),                    -- MONTHLY, YEARLY
  ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS subscription_payments (
  id UUID PRIMARY KEY,
  veterinary_id UUID NOT NULL,
  subscription_id UUID,
  provider VARCHAR(20) NOT NULL,               -- WOMPI
  provider_reference VARCHAR(100) NOT NULL UNIQUE,
  amount NUMERIC(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(20) NOT NULL,                 -- PENDING, APPROVED, DECLINED, ERROR
  raw_event JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## 4. Seguridad (VET-03 … VET-08)

### 4.1 Dependencias

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

Eliminar `jjwt` y `jwt.secret` cuando todo funcione con JWKS.

### 4.2 application.yaml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${JWT_JWKS_URI}          # http://user-service/.well-known/jwks.json
          issuer-uri:                            # vacío: validamos iss manualmente
app:
  security:
    issuer: myanimalog-user-service
    audience: myanimalog-api
    internal-api-key: ${INTERNAL_API_KEY}
```

### 4.3 SecurityConfig

```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final InternalApiKeyFilter internalApiKeyFilter;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**",
                         "/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/public/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/webhooks/payments/**").permitAll() // valida firma propia
        .requestMatchers("/internal/**").hasAuthority("INTERNAL_SERVICE")
        .requestMatchers("/admin/**").hasAuthority("PLATFORM_ADMIN")
        .anyRequest().authenticated())
      .addFilterBefore(internalApiKeyFilter, BearerTokenAuthenticationFilter.class)
      .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(new JwtClaimsConverter())))
      .build();
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks,
                        @Value("${app.security.issuer}") String iss,
                        @Value("${app.security.audience}") String aud) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(iss),
        new JwtClaimValidator<List<String>>("aud", a -> a != null && a.contains(aud))));
    return decoder;
  }
}
```

- `JwtClaimsConverter`: convierte el JWT en `JwtAuthenticationToken` con autoridades `PLATFORM_ADMIN` (si `platform_role` lo es) y `VET_ROLE_<rol>` (si `ctx = VETERINARY`).
- `InternalApiKeyFilter`: si la ruta empieza por `/internal/` y el header `X-Internal-Api-Key` coincide (comparación en tiempo constante, `MessageDigest.isEqual`), pone una autenticación con autoridad `INTERNAL_SERVICE`. Si no coincide → `401`.

### 4.4 Puerto del usuario autenticado (mantiene el dominio libre de Spring)

```java
// application/shared/ports/out
public interface AuthenticatedUserPort {
  AuthenticatedUser current();                 // lanza UnauthenticatedException si no hay
}

// application/shared/dto
public record AuthenticatedUser(
    UUID userId, String email, boolean emailVerified, boolean platformAdmin,
    Optional<ClinicContext> clinic) {}

public record ClinicContext(UUID veterinaryId, UUID employeeId, EmployeeRole role,
                            boolean licensed, VeterinaryStatus status) {}
```

`SpringAuthenticatedUserAdapter` (infraestructura) lo implementa leyendo `SecurityContextHolder`.

### 4.5 Permisos en el dominio

```java
// domain/security
public enum Permission {
  CLINIC_CONFIGURE, SUBSCRIPTION_MANAGE, STAFF_MANAGE, APPOINTMENT_MANAGE, PATIENT_REGISTER,
  CLINICAL_READ, CLINICAL_READ_BASIC, CLINICAL_WRITE, NURSING_WRITE, DOCUMENT_UPLOAD, REPORTS_VIEW
}

public final class RolePermissions {
  private static final Set<Permission> PRE_APPROVAL =
      EnumSet.of(CLINIC_CONFIGURE, STAFF_MANAGE, SUBSCRIPTION_MANAGE);

  public static Set<Permission> resolve(EmployeeRole role, boolean licensed, VeterinaryStatus status) {
    Set<Permission> p = EnumSet.copyOf(base(role));
    if (licensed && EnumSet.of(OWNER, ADMIN, VETERINARIAN).contains(role)) {
      p.add(CLINICAL_WRITE);
      p.add(NURSING_WRITE);
    }
    if (!licensed && role == VETERINARIAN) {
      p.remove(CLINICAL_WRITE);
      p.remove(NURSING_WRITE);
    }
    if (status != VeterinaryStatus.ACTIVE) p.retainAll(PRE_APPROVAL);
    return p;
  }
  // base(role): implementa la matriz de CONTRATOS_COMPARTIDOS.md §3.2
}
```

Test obligatorio: `RolePermissionsTest` con un caso por celda de la matriz.

### 4.6 Servicio de autorización

```java
// application/shared/services
@Service
@RequiredArgsConstructor
public class VeterinaryAuthorizationService {
  private final AuthenticatedUserPort auth;

  /** Para operaciones de personal de la clínica. */
  public ClinicContext require(UUID veterinaryIdFromPath, Permission permission) {
    ClinicContext ctx = auth.current().clinic()
        .orElseThrow(ClinicContextRequiredException::new);              // 403
    if (!ctx.veterinaryId().equals(veterinaryIdFromPath))
      throw new TenantMismatchException();                             // 403
    if (!RolePermissions.resolve(ctx.role(), ctx.licensed(), ctx.status()).contains(permission))
      throw new InsufficientPermissionException(permission);           // 403
    return ctx;
  }

  /** Para operaciones que solo requieren ser miembro (cualquier rol). */
  public ClinicContext requireMember(UUID veterinaryIdFromPath) { … }
}
```

Los claims del token pueden tener hasta 15 minutos de antigüedad. Para operaciones sensibles (`STAFF_MANAGE`, `SUBSCRIPTION_MANAGE`, transferir propiedad) el servicio **vuelve a leer** el empleado de la BD antes de actuar.

### 4.7 Excepciones nuevas → `GlobalExceptionHandler`

| Excepción | HTTP |
|---|---|
| `UnauthenticatedException` | 401 |
| `ClinicContextRequiredException`, `TenantMismatchException`, `InsufficientPermissionException` | 403 |
| `PlatformAdminRequiredException` | 403 |
| `ClinicNotActiveException` | 409 |
| `PlanLimitReachedException` | 402 |
| `FeatureNotInPlanException` | 402 |

---

## 5. Módulo clínica + verificación

### 5.1 Ciclo de vida de la clínica

```
PENDING_DOCUMENTS ──submit──▶ UNDER_REVIEW ──approve──▶ ACTIVE ──suspend──▶ SUSPENDED
        ▲                          │                       ▲                     │
        └──── NEEDS_CORRECTION ◀───┤ request-correction    └─────reactivate──────┘
                                   └──reject──▶ REJECTED
```

- Mientras no esté `ACTIVE`: puede configurar perfil, logo, horarios, catálogo e invitar personal. **No** puede vincular dueños, registrar pacientes, recibir citas ni aparecer en el directorio.
- Al pasar a `ACTIVE` arranca la **prueba de 30 días** (no al registrarse: así la prueba no se consume mientras esperan la revisión).
- `REJECTED` es final para esa solicitud. Pueden crear una nueva solicitud con documentos nuevos (máximo 3 intentos; después, solo soporte).

### 5.2 Endpoints de clínica

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| POST | `/veterinaries` | base | `RegisterVeterinary` (🔧) |
| GET | `/veterinaries/{id}` | miembro | `GetVeterinary` 🆕 |
| PATCH | `/veterinaries/{id}` | `CLINIC_CONFIGURE` | `UpdateVeterinary` ✅ |
| PATCH | `/veterinaries/{id}/settings` | `CLINIC_CONFIGURE` | `UpdateVeterinarySettings` 🆕 |
| PUT | `/veterinaries/{id}/logo` | `CLINIC_CONFIGURE` | `UploadVeterinaryLogo` 🆕 (bucket público `veterinary-logos`, ≤2 MB, png/jpeg/webp) |
| DELETE | `/veterinaries/{id}/logo` | `CLINIC_CONFIGURE` | `DeleteVeterinaryLogo` 🆕 |
| POST | `/veterinaries/{id}/invite-code` | `CLINIC_CONFIGURE` | `GenerateInviteCode` ✅ |
| DELETE | `/veterinaries/{id}/invite-code` | `CLINIC_CONFIGURE` | `RevokeInviteCode` 🆕 |
| GET | `/me/veterinaries` | base | `GetMyVeterinaries` 🆕 (clínicas donde soy empleado activo, con rol y estado — la web lo usa para elegir clínica) |
| GET | `/public/veterinaries` | público | `SearchVeterinaryDirectory` 🆕 (`q`, `city`, `lat`, `lng`, `radiusKm`, paginado; solo `ACTIVE` y `directory_visible`) |
| GET | `/public/veterinaries/{id}` | público | `GetPublicVeterinaryProfile` 🆕 (sin NIT ni datos internos) |
| GET | `/public/veterinaries/by-code/{code}` | público | `PreviewVeterinaryByCode` 🆕 (vista previa antes de vincularse) |
| PATCH | `/admin/veterinaries/{id}/suspend` | `PLATFORM_ADMIN` | `SuspendVeterinary` (antes `DeActivate`) |
| PATCH | `/admin/veterinaries/{id}/reactivate` | `PLATFORM_ADMIN` | `ReactivateVeterinary` (antes `Activate`) |

Reglas de `RegisterVeterinary`:
- El creador sale de `sub` del token. Requiere `email_verified = true`.
- Valida nombre, email, NIT (formato `\d{9}-\d` con dígito de verificación — implementar el algoritmo de la DIAN en un value object `Nit`), teléfono.
- Únicos: nombre, email, NIT.
- En la **misma transacción**: crea la clínica (`PENDING_DOCUMENTS`), crea al creador como empleado `OWNER` y crea una `verification_request` en `DRAFT`.
- Un usuario puede ser `OWNER` de máximo 3 clínicas en estado no final (anti-spam).
- Publica `VETERINARY_REGISTERED`.

### 5.3 Endpoints de verificación

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| GET | `/veterinaries/{id}/verification` | miembro | `GetClinicVerification` |
| POST | `/veterinaries/{id}/verification/documents` | `OWNER` | `UploadVerificationDocument` (multipart: `documentType`, `file`; pdf/jpeg/png, ≤10 MB) |
| DELETE | `/veterinaries/{id}/verification/documents/{docId}` | `OWNER` | `DeleteVerificationDocument` (solo en `DRAFT`/`NEEDS_CORRECTION`) |
| POST | `/veterinaries/{id}/verification/submit` | `OWNER` | `SubmitClinicVerification` |
| GET | `/admin/verifications?status=&subjectType=` | `PLATFORM_ADMIN` | `ListVerificationRequests` |
| GET | `/admin/verifications/{requestId}` | `PLATFORM_ADMIN` | `GetVerificationRequestDetail` (incluye URLs **firmadas** de 10 min y el `ai_report`) |
| POST | `/admin/verifications/{requestId}/rerun-ai` | `PLATFORM_ADMIN` | `RerunVerificationAnalysis` |
| POST | `/admin/verifications/{requestId}/approve` | `PLATFORM_ADMIN` | `ApproveVerification` (body: `registryChecked: true` obligatorio) |
| POST | `/admin/verifications/{requestId}/reject` | `PLATFORM_ADMIN` | `RejectVerification` (body: `reason` obligatorio) |
| POST | `/admin/verifications/{requestId}/request-correction` | `PLATFORM_ADMIN` | `RequestVerificationCorrection` (body: `notes`, `documentTypes[]`) |

Reglas:
- `Submit` exige todos los documentos obligatorios del tipo de sujeto. Si el `OWNER` declaró ser veterinario, su perfil profesional se envía en el mismo paso (§5.4).
- Al enviar: estado `UNDER_REVIEW`, `ai_status = RUNNING`, se dispara el análisis **asíncrono** (`@Async` + `ApplicationEventPublisher` interno) que llama a `IaVerificationPort` (requiere IA-01). La respuesta HTTP no espera a la IA.
- Si la IA falla o tarda más de 60 s → `ai_status = FAILED`. **La revisión humana sigue igual**; la IA nunca bloquea ni decide.
- `Approve` solo es válido si `registryChecked = true` (el admin confirmó en el portal oficial). Se guarda quién aprobó y cuándo.
- `sha256` repetido entre clínicas distintas → se marca en el detalle como alerta para el admin.
- Los documentos van en bucket **privado** `verification-documents` en la ruta `verifications/{requestId}/{documentId}.<ext>`. Nunca se devuelven URLs públicas.
- Retención: documentos de solicitudes `REJECTED` se borran a los 90 días (job).

### 5.4 Perfil profesional

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| GET | `/me/professional-profile` | base | `GetMyProfessionalProfile` |
| PUT | `/me/professional-profile` | base | `UpsertProfessionalProfile` (no editable si está `PENDING`; si está `VERIFIED` y cambia `license_number`, vuelve a `NOT_SUBMITTED`) |
| POST | `/me/professional-profile/documents` | base | `UploadProfessionalDocument` |
| PUT | `/me/professional-profile/signature` | base | `UploadSignature` (png, bucket privado) |
| POST | `/me/professional-profile/submit` | base | `SubmitProfessionalVerification` (crea `verification_request` con `subject_type = PROFESSIONAL`) |
| GET | `/veterinaries/{id}/employees/{employeeId}/professional-profile` | miembro | `GetEmployeeProfessionalProfile` (sin documentos) |

La aprobación usa los mismos endpoints `/admin/verifications/**`. Al aprobar: `license_status = VERIFIED` → el siguiente refresh del token trae `vet_licensed = true`.

### 5.5 Endpoints internos de clínica

| Método | Ruta | Usado por |
|---|---|---|
| GET | `/internal/veterinaries/{id}/members/{userId}` | User service (emitir token de clínica) |
| GET | `/internal/veterinaries/{id}/summary` | Notifications, Calendar (nombre, logo, teléfono, dirección, timezone) |

Respuesta de `members`:

```json
{ "member": true, "employeeId": "uuid", "role": "VETERINARIAN", "active": true,
  "licensed": true, "veterinaryStatus": "ACTIVE", "subscriptionStatus": "TRIAL" }
```

---

## 6. Módulo personal

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| GET | `/veterinaries/{id}/employees` | miembro | `ListEmployees` 🆕 (enriquecido con nombre/email desde `UserServicePort` batch) |
| GET | `/veterinaries/{id}/employees/{employeeId}` | miembro | `GetEmployee` 🆕 |
| POST | `/veterinaries/{id}/employee-invitations` | `STAFF_MANAGE` | `InviteEmployee` 🆕 (`email`, `role`) |
| GET | `/veterinaries/{id}/employee-invitations?status=` | `STAFF_MANAGE` | `ListEmployeeInvitations` 🆕 |
| POST | `/veterinaries/{id}/employee-invitations/{invId}/resend` | `STAFF_MANAGE` | `ResendEmployeeInvitation` 🆕 |
| DELETE | `/veterinaries/{id}/employee-invitations/{invId}` | `STAFF_MANAGE` | `CancelEmployeeInvitation` 🆕 |
| GET | `/me/employee-invitations` | base | `GetMyPendingEmployeeInvitations` 🆕 (por `email` del token) |
| POST | `/employee-invitations/{token}/accept` | base | `AcceptEmployeeInvitation` 🆕 |
| POST | `/employee-invitations/{token}/reject` | base | `RejectEmployeeInvitation` 🆕 |
| PATCH | `/veterinaries/{id}/employees/{employeeId}/role` | `STAFF_MANAGE` | `UpdateEmployeeRole` 🔧 |
| PATCH | `/veterinaries/{id}/employees/{employeeId}/activate` | `STAFF_MANAGE` | `ActivateEmployee` 🔧 |
| PATCH | `/veterinaries/{id}/employees/{employeeId}/deactivate` | `STAFF_MANAGE` | `DeactivateEmployee` 🔧 |
| POST | `/veterinaries/{id}/ownership-transfer` | `OWNER` | `TransferOwnership` 🆕 |
| DELETE | `/veterinaries/{id}/employees/me` | miembro | `LeaveVeterinary` 🆕 |

Reglas:
- Invitar: no se puede invitar con rol `OWNER`. No se puede invitar a alguien que ya es empleado activo. Una sola invitación `PENDING` por clínica+email (`EmployeeInvitationAlreadyExistsException`). Expira en 7 días. Valida límite `max_employees` del plan (cuenta empleados activos + invitaciones pendientes).
- Aceptar: invitación `PENDING` y no vencida; `email` del token **igual** (case-insensitive) al de la invitación; `email_verified = true`. Si el usuario fue empleado antes y está inactivo, se reactiva con el rol nuevo en lugar de crear otra fila. Reutiliza el servicio de `CreateVeterinaryEmployee`.
- `ADMIN` no puede cambiar rol, desactivar ni modificar a un `OWNER` ni a otro `ADMIN`. Nadie puede modificarse a sí mismo el rol.
- `TransferOwnership`: el destino debe ser empleado activo; en una transacción el `OWNER` actual pasa a `ADMIN` y el destino pasa a `OWNER`.
- `LeaveVeterinary`: el `OWNER` no puede irse sin transferir antes.
- Eventos: `EMPLOYEE_INVITED`, `EMPLOYEE_JOINED`, `EMPLOYEE_ROLE_UPDATED`, `EMPLOYEE_DEACTIVATED`, `OWNERSHIP_TRANSFERRED`.

---

## 7. Módulo vinculación y pacientes

### 7.1 Vinculación del dueño (cliente)

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| POST | `/veterinaries/link` | base | `LinkToVeterinary` 🔁 (body: `code`; reemplaza `LinkByCode`/`LinkByUrl`) |
| GET | `/me/linked-veterinaries` | base | `GetMyLinkedVeterinaries` 🆕 |
| DELETE | `/veterinaries/{id}/link` | base | `Unlink` 🔧 (lado dueño) |
| GET | `/veterinaries/{id}/clients` | `CLINICAL_READ_BASIC` | `ListClients` 🆕 |
| DELETE | `/veterinaries/{id}/clients/{userId}` | `CLINIC_CONFIGURE` | `RemoveClient` 🆕 (lado clínica) |

Reglas: la clínica debe estar `ACTIVE`. Al desvincular (cualquier lado) se revocan **todas** las mascotas de ese dueño en esa clínica (`consent_status = REVOKED`). Eventos: `USER_LINKED`, `USER_UNLINKED`.

### 7.2 Consentimiento por mascota

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| POST | `/veterinaries/{id}/patients/share` | base | `SharePetsWithVeterinary` 🆕 (body: `petIds[]`) |
| DELETE | `/veterinaries/{id}/patients/{petId}/share` | base | `RevokePetShare` 🆕 |
| GET | `/me/pets/{petId}/veterinaries` | base | `GetVeterinariesForPet` 🆕 (qué clínicas ven mi mascota) |

Reglas de `Share`:
- El usuario debe estar vinculado a la clínica.
- Por cada `petId`, verificar con `PetsServicePort.isOwner(petId, userId)` (endpoint existente `GET /pets/{petId}/owner/{userId}`, migrado a interno — requiere PETS-04). Si alguno no es suyo → `403` y no se comparte ninguno (todo o nada).
- Si ya existía `REVOKED`, se reactiva (`GRANTED`, nuevo `granted_at`).
- Guarda el snapshot (`pet_name`, `species`, `owner_name`, `owner_phone`, `owner_email`) leyendo Pets y User.
- Valida límite `max_patients` del plan.
- Eventos: `PET_SHARED_WITH_VETERINARY` (destinatario: el `OWNER` de la clínica, o no se notifica — decisión de producto), `PET_SHARE_REVOKED`.

### 7.3 Pacientes desde la clínica

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| GET | `/veterinaries/{id}/patients?q=&species=&page=&size=` | `CLINICAL_READ_BASIC` | `ListVeterinaryPatients` 🆕 (búsqueda full-text en snapshot + teléfono exacto) |
| GET | `/veterinaries/{id}/patients/{petId}` | `CLINICAL_READ_BASIC` | `GetVeterinaryPatient` 🆕 (datos de Pets en vivo vía batch) |
| POST | `/veterinaries/{id}/patients/walk-in` | `PATIENT_REGISTER` | `RegisterWalkInPatient` 🆕 |
| POST | `/veterinaries/{id}/patients/{petId}/claim-invitation` | `PATIENT_REGISTER` | `SendPetClaimInvitation` 🆕 |

`RegisterWalkInPatient` (dueño sin app):

```json
{
  "pet":   { "name": "Max", "species": "DOG", "breed": "Criollo", "sex": "MALE", "birthDate": "2021-05-10" },
  "owner": { "fullName": "Carlos Pérez", "phone": "+573001234567", "email": "carlos@correo.com",
             "documentType": "CC", "documentNumber": "1047…" },
  "sendClaimInvitation": true
}
```

1. Llama `PetsServicePort.createClinicPet(...)` → Pets crea la mascota con `owner_id = null` y `created_by_veterinary_id` (requiere PETS-05).
2. Crea `veterinary_patients` con `origin = REGISTERED_BY_CLINIC`, `consent_status = GRANTED`, `granted_by = null`.
3. Si `sendClaimInvitation` y hay email → `PetsServicePort.createClaimInvitation(petId, email, veterinaryId)` (requiere PETS-06). Pets publica el evento y Notifications envía el email.
4. Si el paso 1 funcionó pero el 2 falla → llamar `PetsServicePort.deleteClinicPet(petId)` (compensación). Loguear si la compensación falla.

Cuando el dueño reclama la mascota en la app, el consentimiento con la clínica que la registró **se mantiene** (el texto de aceptación del reclamo lo dice explícitamente). No hace falta callback: Veterinary no guarda `owner_id`, lo pide a Pets cuando lo necesita.

### 7.4 Endpoint interno de acceso

`GET /internal/veterinaries/{id}/patients/{petId}/access?userId=` — contrato exacto en `CONTRATOS_COMPARTIDOS.md §3`.
Implementación: un solo query que une `veterinary_employee` (activo) + `veterinary` (estado) + `veterinary_patients` (`GRANTED`) + `professional_profiles` (licencia) y aplica `RolePermissions.resolve`.
Es el endpoint más llamado del sistema: índice compuesto y test de rendimiento básico (< 50 ms en local).

---

## 8. Módulo catálogo y horarios

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| GET | `/veterinaries/{id}/services` | miembro | `ListClinicServices` |
| GET | `/public/veterinaries/{id}/services` | público | `ListPublicClinicServices` (solo activos y `bookable_online`) |
| POST | `/veterinaries/{id}/services` | `CLINIC_CONFIGURE` | `CreateClinicService` |
| PATCH | `/veterinaries/{id}/services/{serviceId}` | `CLINIC_CONFIGURE` | `UpdateClinicService` |
| PATCH | `/veterinaries/{id}/services/{serviceId}/deactivate` | `CLINIC_CONFIGURE` | `DeactivateClinicService` (no se borra: hay citas que lo referencian) |
| GET / PUT | `/veterinaries/{id}/business-hours` | miembro / `CLINIC_CONFIGURE` | `GetBusinessHours` / `ReplaceBusinessHours` (reemplaza la semana completa; valida que no se solapen franjas del mismo día) |
| GET / PUT | `/veterinaries/{id}/employees/{employeeId}/schedule` | miembro / `CLINIC_CONFIGURE` | `GetEmployeeSchedule` / `ReplaceEmployeeSchedule` (debe caer dentro del horario de la clínica) |
| GET | `/veterinaries/{id}/schedule-blocks?from=&to=` | miembro | `ListScheduleBlocks` |
| POST | `/veterinaries/{id}/schedule-blocks` | `CLINIC_CONFIGURE` (o el propio empleado para sí mismo) | `CreateScheduleBlock` |
| DELETE | `/veterinaries/{id}/schedule-blocks/{blockId}` | idem | `DeleteScheduleBlock` |
| GET | `/internal/veterinaries/{id}/schedule-rules?from=&to=&employeeId=` | interno | `GetScheduleRules` (lo consume Calendar para calcular disponibilidad) |

Respuesta de `schedule-rules`:

```json
{
  "timezone": "America/Bogota",
  "defaultAppointmentMinutes": 30,
  "allowOnlineBooking": true,
  "bookingRequiresConfirmation": true,
  "cancellationMinHours": 4,
  "businessHours": [{ "dayOfWeek": 1, "opensAt": "08:00", "closesAt": "18:00" }],
  "employees": [{ "employeeId": "uuid", "role": "VETERINARIAN",
                  "schedule": [{ "dayOfWeek": 1, "startsAt": "08:00", "endsAt": "14:00" }] }],
  "blocks": [{ "employeeId": null, "startsAt": "2026-12-25T00:00:00-05:00", "endsAt": "2026-12-26T00:00:00-05:00" }],
  "services": [{ "id": "uuid", "name": "Consulta general", "durationMinutes": 30, "price": 60000 }]
}
```

---

## 9. Módulo suscripción

### 9.1 Planes iniciales (seed en V9)

| Código | Precio mensual | Empleados | Pacientes | Features |
|---|---|---|---|---|
| `FREE` | 0 | 1 | 100 | — |
| `BASIC` | definir | 5 | 1.000 | `ONLINE_BOOKING` |
| `PRO` | definir | ilimitado | ilimitado | `ONLINE_BOOKING`, `WHATSAPP`, `REPORTS`, `AI_SUMMARY` |

La prueba de 30 días se da con el plan `PRO` (`status = TRIAL`). Al terminar sin pago → baja a `FREE` (no se bloquea la clínica, solo se aplican límites; los pacientes por encima del límite quedan en solo lectura).

### 9.2 Endpoints

| Método | Ruta | Token / permiso | Caso de uso |
|---|---|---|---|
| GET | `/public/plans` | público | `ListPlans` |
| GET | `/veterinaries/{id}/subscription` | miembro | `GetActivePlan` ✅ |
| GET | `/veterinaries/{id}/subscription/expired` | miembro | `IsExpired` ✅ |
| GET | `/veterinaries/{id}/subscription/usage` | miembro | `GetPlanUsage` 🆕 (empleados y pacientes usados vs límite) |
| POST | `/veterinaries/{id}/subscription/checkout` | `SUBSCRIPTION_MANAGE` | `CreateCheckout` 🆕 (body: `planCode`, `billingPeriod`; devuelve link de pago) |
| POST | `/webhooks/payments/wompi` | público + firma | `HandlePaymentWebhook` 🆕 |
| POST | `/veterinaries/{id}/subscription/cancel` | `SUBSCRIPTION_MANAGE` | `CancelSubscription` 🆕 (`cancel_at_period_end = true`) |
| GET | `/veterinaries/{id}/subscription/history` | `SUBSCRIPTION_MANAGE` | `GetSubscriptionHistory` 🆕 |
| POST | `/admin/veterinaries/{id}/subscription` | `PLATFORM_ADMIN` | `CreatePlan` 🔧 (asignación manual, pilotos) |
| PATCH | `/admin/veterinaries/{id}/subscription` | `PLATFORM_ADMIN` | `UpdatePlan` 🔧 |
| POST | `/internal/jobs/subscriptions/daily` | interno (Cloud Scheduler) | `RunSubscriptionLifecycle` 🆕 |

### 9.3 Pasarela

- Usar **Wompi en modo sandbox** para el proyecto (llaves de prueba, sin dinero real). Puerto `PaymentGatewayPort` para poder cambiar de proveedor.
- El webhook valida la firma del evento con el secreto de eventos, es **idempotente** (`provider_reference` único) y solo activa el plan con estado `APPROVED`.
- Nunca activar un plan por la redirección del navegador: solo por webhook.

### 9.4 Job diario `RunSubscriptionLifecycle`

1. Suscripciones que vencen en 7 días → `SUBSCRIPTION_EXPIRING`.
2. Vencidas hoy → `GRACE` (5 días, todo funciona) + `SUBSCRIPTION_IN_GRACE`.
3. Gracia terminada → `EXPIRED` y la clínica pasa a plan `FREE` + `SUBSCRIPTION_EXPIRED`.
4. `cancel_at_period_end` y periodo terminado → `CANCELLED` + plan `FREE`.
5. Borrar documentos de verificaciones rechazadas con más de 90 días.

El job debe ser idempotente (correrlo dos veces el mismo día no duplica eventos).

### 9.5 Guardas de plan

`PlanLimitGuard` (servicio de aplicación) con `requireCapacity(veterinaryId, Resource.EMPLOYEES | PATIENTS)` y `requireFeature(veterinaryId, Feature)`. Se llama desde `InviteEmployee`, `AcceptEmployeeInvitation`, `SharePetsWithVeterinary`, `RegisterWalkInPatient` y desde el endpoint interno que consulta Calendar para reservas online.

---

## 10. Agenda — integración con Calendar

**Opción A (recomendada si ya existe un microservicio de calendario/citas):** Veterinary **no** guarda citas. Expone `schedule-rules` (§8) y `access` (§7.4). Calendar guarda las citas con `veterinaryId`, `employeeId`, `petId`, `serviceId`. Ver `CALENDAR_SERVICE_CHANGES.md`.

**Opción B (si no existe):** crear aquí el módulo `appointments` con la tabla y estados descritos en `CALENDAR_SERVICE_CHANGES.md §3`, publicando al topic `veterinary-appointments`. Los endpoints son los mismos de ese archivo, montados bajo `/veterinaries/{id}/appointments`.

Decidir A o B **antes** de VET-30.

---

## 11. Eventos que publica Veterinary

Sobre según `CONTRATOS_COMPARTIDOS.md §4`. Adaptador `GooglePubSubEventAdapter` con mapa `eventType → topic`. **Test obligatorio** que recorra el enum de eventos y verifique que todos tienen topic (en Pets un evento sin topic se pierde en silencio; aquí debe fallar el test).

| Evento | Topic | Destinatario (`userId` / `recipient`) | Payload mínimo |
|---|---|---|---|
| `VETERINARY_REGISTERED` | veterinary-clinic | creador | veterinaryId, name |
| `VERIFICATION_SUBMITTED` | veterinary-verification | `null` + recipient = email de soporte | requestId, subjectType, veterinaryName |
| `VERIFICATION_APPROVED` | veterinary-verification | quien envió | subjectType, veterinaryName, trialEndsAt |
| `VERIFICATION_REJECTED` | veterinary-verification | quien envió | subjectType, reason |
| `VERIFICATION_CORRECTION_REQUESTED` | veterinary-verification | quien envió | notes, documentTypes |
| `VETERINARY_SUSPENDED` / `_REACTIVATED` | veterinary-clinic | OWNER | veterinaryName |
| `EMPLOYEE_INVITED` | veterinary-staff | `null` + recipient = email invitado | veterinaryName, role, acceptUrl, expiresAt |
| `EMPLOYEE_JOINED` | veterinary-staff | OWNER | employeeName, role |
| `EMPLOYEE_ROLE_UPDATED` | veterinary-staff | empleado | oldRole, newRole |
| `EMPLOYEE_DEACTIVATED` | veterinary-staff | empleado | veterinaryName |
| `OWNERSHIP_TRANSFERRED` | veterinary-staff | nuevo OWNER | veterinaryName |
| `USER_LINKED` / `USER_UNLINKED` | veterinary-patients | dueño | veterinaryName |
| `PET_SHARED_WITH_VETERINARY` | veterinary-patients | dueño (confirmación) | petName, veterinaryName |
| `PET_SHARE_REVOKED` | veterinary-patients | dueño | petName, veterinaryName |
| `SUBSCRIPTION_TRIAL_STARTED` | veterinary-subscription | OWNER | planName, trialEndsAt |
| `SUBSCRIPTION_EXPIRING` / `_IN_GRACE` / `_EXPIRED` | veterinary-subscription | OWNER | planName, date |
| `SUBSCRIPTION_RENEWED` / `PAYMENT_FAILED` | veterinary-subscription | OWNER | planName, amount |

---

## 12. Clientes HTTP hacia otros servicios

Puertos en `application/shared/ports/out`, adaptadores con `RestClient` en `infrastructure/clients`, todos con `X-Internal-Api-Key`, timeout 3 s y manejo de error → excepción de dominio `ExternalServiceUnavailableException` (503).

| Puerto | Métodos | Endpoint destino |
|---|---|---|
| `UserServicePort` | `getUser(id)`, `getUsers(ids)`, `findByEmail(email)` | `USER-06` |
| `PetsServicePort` | `isOwner(petId, userId)`, `getPets(ids)`, `createClinicPet(...)`, `deleteClinicPet(id)`, `createClaimInvitation(...)` | `PETS-04`, `PETS-05`, `PETS-06` |
| `IaVerificationPort` | `analyze(request)` | `IA-01` |
| `CalendarServicePort` | (solo opción A, si Veterinary necesita contar citas para reportes) | `CAL-xx` |
| `FileStoragePort` | `uploadPublic`, `uploadPrivate`, `signedUrl(path, ttl)`, `delete` | Supabase Storage |

Variables de entorno nuevas:

```
JWT_JWKS_URI, INTERNAL_API_KEY,
USER_SERVICE_URL, PETS_SERVICE_URL, IA_SERVICE_URL, CALENDAR_SERVICE_URL,
GCP_PROJECT_ID, SUPABASE_URL, SUPABASE_KEY (service role, solo backend),
WOMPI_PUBLIC_KEY, WOMPI_PRIVATE_KEY, WOMPI_EVENTS_SECRET, WOMPI_BASE_URL,
APP_BASE_URL, SUPPORT_EMAIL
```

---

## 13. Tareas en orden

> ☐ = pendiente. Cada tarea incluye sus tests. No se pasa a la siguiente fase con tests rojos.

### Fase 0 — Bases

- ☐ **VET-01** Formalizar el flujo de cambios de esquema manual: `db/scripts/README.md` (convención `NNN_descripcion.sql`, idempotencia, tabla de registro) y sección "Base de datos" en `CLAUDE.md` con el esquema real. No se introduce ningún motor de migraciones. *Listo cuando:* existen `db/scripts/README.md` y `CLAUDE.md` actualizado, y `./mvnw test` pasa igual que antes.
- ☐ **VET-02** Reorganizar paquetes por módulo (§2) sin cambiar comportamiento. *Listo cuando:* todos los tests existentes pasan.
- ☐ **VET-03** Pasar tests a **Testcontainers** (Postgres 16) con perfil `test` que crea el esquema con `ddl-auto: create-drop` (no hay migraciones que aplicar dentro del contenedor). *Listo cuando:* `./mvnw test` corre sin `.env` ni Supabase.
- ☐ **VET-04** `GooglePubSubEventAdapter` + `EventPublisherPort` + sobre de evento + test de cobertura de topics.

### Fase 1 — Seguridad *(requiere USER-01, USER-02)*

- ☐ **VET-05** Resource server JWKS, `SecurityConfig`, `JwtClaimsConverter`, `InternalApiKeyFilter` (§4.1–4.3).
- ☐ **VET-06** `AuthenticatedUserPort` + adaptador, `Permission`, `RolePermissions` + `RolePermissionsTest` (§4.4–4.5).
- ☐ **VET-07** `VeterinaryAuthorizationService` + excepciones + handlers (§4.6–4.7).
- ☐ **VET-08** Quitar `userId` de todos los bodies/params existentes y aplicar permisos a los 16 casos de uso actuales según §1. Tests de controller con `jwt()` incluyendo 401, 403 por rol y 403 por otra clínica.

### Fase 2 — Clínica, roles y verificación

- ☐ **VET-09** Scripts 002 y 003. `VeterinaryStatus` reemplaza el booleano `active` en dominio, entidad y mapper.
- ☐ **VET-10** `RegisterVeterinary` 🔧 (OWNER + verificación DRAFT en una transacción, value object `Nit`).
- ☐ **VET-11** Endpoint interno `members` (§5.5). *Desbloquea USER-03.*
- ☐ **VET-12** `GetVeterinary`, `UpdateVeterinarySettings`, logo, `RevokeInviteCode`, `GetMyVeterinaries`.
- ☐ **VET-13** Mover `Activate`/`DeActivate` a admin (`Suspend`/`Reactivate`).
- ☐ **VET-14** Scripts 004 y 005. `FileStoragePort` con bucket privado y URLs firmadas.
- ☐ **VET-15** Verificación de clínica: subir/borrar documentos, `Submit`, estados (§5.3).
- ☐ **VET-16** Perfil profesional completo (§5.4).
- ☐ **VET-17** Panel admin de verificación: listar, detalle con URLs firmadas, aprobar/rechazar/pedir corrección. Aprobar clínica inicia la prueba (depende de VET-36 para el plan; mientras tanto, crear la suscripción `TRIAL` directo).
- ☐ **VET-18** Análisis asíncrono con `IaVerificationPort` + `rerun-ai` *(requiere IA-01)*. Si IA no está lista, dejar el adaptador devolviendo `FAILED` y seguir.
- ☐ **VET-19** Eventos de verificación y clínica.

### Fase 3 — Personal

- ☐ **VET-20** Script 006. `UserServicePort` *(requiere USER-06)*.
- ☐ **VET-21** Invitaciones de empleado: invitar, listar, reenviar, cancelar, mis invitaciones, aceptar, rechazar.
- ☐ **VET-22** `ListEmployees`, `GetEmployee` (enriquecidos), reglas nuevas de `UpdateEmployeeRole`/`Activate`/`Deactivate`.
- ☐ **VET-23** `TransferOwnership`, `LeaveVeterinary`. Deprecar el endpoint público `CreateVeterinaryEmployee`.
- ☐ **VET-24** Eventos de personal.

### Fase 4 — Pacientes *(requiere PETS-04, PETS-05, PETS-06)*

- ☐ **VET-25** Script 007. `PetsServicePort`.
- ☐ **VET-26** `LinkToVeterinary` (unifica), `Unlink` desde ambos lados con revocación en cascada, `ListClients`, `GetMyLinkedVeterinaries`, preview por código.
- ☐ **VET-27** `SharePetsWithVeterinary`, `RevokePetShare`, `GetVeterinariesForPet`.
- ☐ **VET-28** `ListVeterinaryPatients` (búsqueda), `GetVeterinaryPatient`, `RegisterWalkInPatient` con compensación, `SendPetClaimInvitation`.
- ☐ **VET-29** Endpoint interno `access` (§7.4) con test de cada caso de `hasAccess=false`. *Desbloquea PETS-03, MED-03, CAL-02.*

### Fase 5 — Catálogo, horarios y agenda

- ☐ **VET-30** Decidir opción A o B de §10.
- ☐ **VET-31** Script 008. Servicios de la clínica (CRUD + público).
- ☐ **VET-32** Horario de clínica, horario por empleado, bloqueos.
- ☐ **VET-33** Endpoint interno `schedule-rules`. *Desbloquea CAL-03.*
- ☐ **VET-34** Directorio público con búsqueda por ciudad y distancia (fórmula de Haversine en SQL; PostGIS no es necesario para el volumen del proyecto).
- ☐ **VET-35** (Solo opción B) módulo `appointments`.

### Fase 6 — Suscripción

- ☐ **VET-36** Script 009 + seed de planes. `ListPlans`, `GetPlanUsage`. Adaptar `GetActivePlan`/`IsExpired` al nuevo modelo.
- ☐ **VET-37** `PlanLimitGuard` aplicado en invitaciones, aceptación, share y walk-in.
- ☐ **VET-38** `PaymentGatewayPort` + adaptador Wompi sandbox, `CreateCheckout`, webhook idempotente con firma.
- ☐ **VET-39** `CancelSubscription`, historial, mover `CreatePlan`/`UpdatePlan` a admin.
- ☐ **VET-40** Job diario idempotente + endpoint `/internal/jobs/subscriptions/daily` + configuración de Cloud Scheduler.
- ☐ **VET-41** Eventos de suscripción.

### Fase 7 — Cierre

- ☐ **VET-42** OpenAPI: agrupar por módulo, documentar `bearerAuth`, ejemplos de request/response.
- ☐ **VET-43** Actualizar `CLAUDE.md` del repo con la nueva arquitectura, seguridad y módulos.
- ☐ **VET-44** Colección de Postman/Bruno reorganizada por módulo con variables `{{baseToken}}`, `{{vetToken}}`, `{{adminToken}}`.
- ☐ **VET-45** Dockerfile + despliegue en Cloud Run (service account con `roles/pubsub.publisher`).

---

## 14. Orden global entre repos

```
USER-01, USER-02 (JWT RS256 + JWKS)
   └─▶ VET Fase 0 y 1
         └─▶ VET-11 ─▶ USER-03, USER-04 (token de clínica)
               └─▶ VET Fase 2  ◀── IA-01 (puede ir en paralelo)
                     └─▶ USER-06 ─▶ VET Fase 3
                           └─▶ PETS-01…06 ─▶ VET Fase 4 ─▶ VET-29
                                 ├─▶ MED-01…06
                                 └─▶ VET Fase 5 ─▶ CAL-01…06
NOTIF-01…06 en paralelo desde que existan los primeros eventos
VET Fase 6 al final
```

---

## 15. Criterio de "base técnica completa"

La base técnica de MyAnimaLogVet está completa cuando este recorrido funciona de punta a punta en un ambiente de pruebas:

1. Ana se registra en la app, crea "Clínica Patitas", sube RUT, Cámara de Comercio, cédula y su tarjeta profesional, y envía.
2. La IA deja un reporte; un admin revisa, verifica en el portal oficial y aprueba. Ana recibe el correo y su prueba de 30 días inicia.
3. Ana invita a Luis (veterinario) y a Sofía (recepcionista). Ambos aceptan desde su correo. Luis sube su tarjeta y es aprobado.
4. Sofía registra a "Max", cuyo dueño no tiene la app, y le envía invitación para reclamarlo.
5. Carlos (dueño) instala la app, reclama a Max y ve que Clínica Patitas tiene acceso.
6. Otra dueña se vincula con el código QR, comparte a su gata y pide una cita desde la app. Sofía la confirma.
7. Luis atiende: marca la cita como atendida, registra la visita y aplica una vacuna en Medical. Sofía intenta escribir un diagnóstico y recibe 403.
8. Un empleado de otra clínica intenta leer a Max y recibe 403.
9. La dueña revoca el acceso de la clínica a su gata; Luis ya no puede leer su historia.
10. Se simula el fin de la prueba: llega aviso, periodo de gracia y baja a `FREE`. Ana paga con Wompi sandbox y vuelve a `PRO`.
