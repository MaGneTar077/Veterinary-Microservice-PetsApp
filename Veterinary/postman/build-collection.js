#!/usr/bin/env node
/*
 * Genera postman/MyAnimaLogVet.postman_collection.json (Postman Collection v2.1).
 *
 * La colección se genera en vez de editarse a mano para que la estructura repetitiva
 * (auth por carpeta, scripts de test, ejemplos) no se desincronice entre requests.
 * Después de tocar este archivo:
 *
 *   node postman/build-collection.js && node postman/check-coverage.js --table
 *
 * Fuente de verdad de las rutas: los controladores. check-coverage.js lo verifica.
 */

const fs = require('fs');

// ---------- helpers ----------

const bearer = (variable) => ({
  type: 'bearer',
  bearer: [{ key: 'token', value: `{{${variable}}}`, type: 'string' }],
});

const noAuth = { type: 'noauth' };

/** Convierte "{{vet_url}}/api/veterinary/{{veterinaryId}}/logo?x=1" en el objeto url de v2.1. */
function parseUrl(raw, query) {
  const [base, qs] = raw.split('?');
  const segments = base.replace(/^\{\{[a-z_]+\}\}\/?/, '').split('/').filter(Boolean);
  const hostVar = (base.match(/^\{\{([a-z_]+)\}\}/) || [])[1];
  const url = { raw, host: [`{{${hostVar}}}`], path: segments };
  if (query && query.length) {
    url.query = query.map((q) => ({
      key: q.key,
      value: q.value,
      description: q.description,
      disabled: q.disabled === true,
    }));
  } else if (qs) {
    url.query = qs.split('&').map((pair) => {
      const [key, value] = pair.split('=');
      return { key, value };
    });
  }
  return url;
}

function jsonBody(obj) {
  return {
    mode: 'raw',
    raw: JSON.stringify(obj, null, 2),
    options: { raw: { language: 'json' } },
  };
}

function formdataBody(fields) {
  return {
    mode: 'formdata',
    formdata: fields.map((f) => ({
      key: f.key,
      type: f.type || 'text',
      value: f.value,
      src: f.src,
      description: f.description,
    })),
  };
}

function testScript(lines) {
  return [
    {
      listen: 'test',
      script: { type: 'text/javascript', exec: lines },
    },
  ];
}

/** Test estándar: verifica el status y, opcionalmente, guarda un id de la respuesta. */
function expectStatus(status, saves = []) {
  const lines = [
    `pm.test("status ${status}", function () {`,
    `    pm.response.to.have.status(${status});`,
    '});',
  ];
  if (saves.length) {
    lines.push('');
    lines.push('if (pm.response.code === ' + status + ') {');
    lines.push('    const body = pm.response.json();');
    for (const s of saves) {
      lines.push(`    if (${s.path}) {`);
      lines.push(`        pm.environment.set("${s.variable}", ${s.path});`);
      lines.push(`        console.log("${s.variable} =", ${s.path});`);
      lines.push('    }');
    }
    lines.push('}');
  }
  return testScript(lines);
}

function example({ name, status, code, body, req }) {
  return {
    name,
    originalRequest: req,
    status: code || 'OK',
    code: status,
    _postman_previewlanguage: 'json',
    header: [{ key: 'Content-Type', value: 'application/json' }],
    cookie: [],
    body: JSON.stringify(body, null, 2),
  };
}

function request({ name, method, raw, description, body, query, tests, examples, headers }) {
  const req = {
    method,
    header: headers || (body && body.mode === 'raw' ? [{ key: 'Content-Type', value: 'application/json' }] : []),
    url: parseUrl(raw, query),
    description,
  };
  if (body) req.body = body;

  const item = { name, request: req };
  if (tests) item.event = tests;
  if (examples) {
    item.response = examples.map((e) =>
      example({ ...e, req: { ...req, url: parseUrl(raw, query) } })
    );
  }
  return item;
}

function folder({ name, description, auth, items }) {
  const f = { name, description, item: items };
  if (auth) f.auth = auth;
  return f;
}

// ---------- Errores comunes, para no repetir texto ----------

const ERR_CLINIC_TOKEN = [
  '**Errores comunes con token de clínica**',
  '- `401 UNAUTHENTICATED` — falta el token o está vencido.',
  '- `403 CLINIC_CONTEXT_REQUIRED` — se usó un token normal donde hace falta uno de clínica.',
  '- `403 TENANT_MISMATCH` — el `vet_id` del token no es la clínica de la ruta.',
  '- `403 NOT_CLINIC_MEMBER` — en la base ya no sos empleado activo (te retiraron o te fuiste). El token puede seguir siendo válido: hay que volver al selector de clínica.',
  '- `403 INSUFFICIENT_PERMISSION` — tu rol **en la base** no tiene el permiso.',
].join('\n');

const ERR_ADMIN = [
  '**Errores comunes de admin**',
  '- `401 UNAUTHENTICATED` — falta el token.',
  '- `403 PLATFORM_ADMIN_REQUIRED` / `403 INSUFFICIENT_PERMISSION` — el token no es de un `PLATFORM_ADMIN`.',
].join('\n');

const NOTE_DB_ROLE = [
  '> Desde la Fase 3 el rol y el estado de la clínica se leen **de la base** en cada petición, no del token.',
  '> Un cambio de rol o un retiro surte efecto en la petición siguiente, sin esperar a que el token expire.',
].join('\n');

// ---------- 00 · Autenticación (User service) ----------

const auth00 = folder({
  name: '00 · Autenticación (User service)',
  auth: noAuth,
  description: [
    'Lo mínimo para conseguir tokens. **Estos endpoints son del User service**, no de este servicio,',
    'y usan `{{user_url}}`.',
    '',
    '⚠️ Las rutas de esta carpeta se tomaron de `CLAUDE.md` (login y token de clínica están',
    'documentados ahí). `refresh` y `/auth/me` **no** están verificados contra el código del User',
    'service: si no coinciden, ajustá la ruta — no son endpoints de Veterinary y el validador de',
    'cobertura los ignora.',
    '',
    'Orden: **Login** (guarda `access_token`) → **Mis clínicas** (carpeta 02, para tener un',
    '`veterinaryId`) → **Token de clínica** (guarda `clinic_token`).',
  ].join('\n'),
  items: [
    request({
      name: 'POST Login (token normal)',
      method: 'POST',
      raw: '{{user_url}}/auth/local',
      description: [
        'Devuelve el **token normal**, que es el que usan: registrar clínica, `GET /me`,',
        'el asistente de verificación del dueño, el perfil profesional, aceptar/rechazar',
        'invitaciones y vincularse como cliente.',
        '',
        'Guarda `access_token` y `refresh_token` en el environment.',
      ].join('\n'),
      body: jsonBody({ email: '{{owner_email}}', password: '{{owner_password}}' }),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const token = body.accessToken || body.access_token || body.token;',
        '    if (token) {',
        '        pm.environment.set("access_token", token);',
        '        console.log("access_token guardado");',
        '    } else {',
        '        console.warn("No encontre el token en la respuesta; ajusta el script al contrato real del User service");',
        '    }',
        '    const refresh = body.refreshToken || body.refresh_token;',
        '    if (refresh) pm.environment.set("refresh_token", refresh);',
        '}',
      ]),
      examples: [
        {
          name: '200 · Login correcto',
          status: 200,
          body: {
            accessToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...',
            refreshToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...',
            expiresIn: 3600,
          },
        },
      ],
    }),
    request({
      name: 'POST Refresh del token',
      method: 'POST',
      raw: '{{user_url}}/auth/refresh',
      description: 'Renueva el `access_token` a partir del `refresh_token`. Ruta **no verificada** contra el código del User service.',
      body: jsonBody({ refreshToken: '{{refresh_token}}' }),
      tests: testScript([
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const token = body.accessToken || body.access_token || body.token;',
        '    if (token) pm.environment.set("access_token", token);',
        '}',
      ]),
    }),
    request({
      name: 'POST Token de clínica (seleccionar clínica)',
      method: 'POST',
      raw: '{{user_url}}/auth/context/veterinary/{{veterinaryId}}',
      description: [
        'Cambia el contexto a una clínica concreta y devuelve el **token de clínica**, con los claims',
        '`ctx=VETERINARY`, `vet_id`, `employee_id`, `vet_role`, `vet_licensed`, `vet_status`.',
        '',
        'Es el token que usan las carpetas 02, 03, 08, 10 y parte de 04. Guarda `clinic_token`.',
        '',
        NOTE_DB_ROLE,
      ].join('\n'),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const token = body.accessToken || body.access_token || body.token;',
        '    if (token) {',
        '        pm.environment.set("clinic_token", token);',
        '        console.log("clinic_token guardado");',
        '    }',
        '}',
      ]),
      examples: [
        {
          name: '200 · Token de clínica',
          status: 200,
          body: { accessToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...', expiresIn: 3600 },
        },
      ],
    }),
    request({
      name: 'GET Mi usuario (User service)',
      method: 'GET',
      raw: '{{user_url}}/auth/me',
      description: [
        'Para confirmar con qué cuenta estás autenticado y si el correo está verificado',
        '(`email_verified` es obligatorio para registrar clínica y para aceptar invitaciones).',
        '',
        'Ruta **no verificada** contra el código del User service.',
      ].join('\n'),
      headers: [{ key: 'Authorization', value: 'Bearer {{access_token}}' }],
    }),
  ],
});

// ---------- 01 · Registro de clínica ----------

const f01 = folder({
  name: '01 · Registro de clínica',
  description: [
    'Primer paso del dueño. Usa el **token normal** y exige `email_verified=true`.',
    '',
    'Crea la clínica en `PENDING_DOCUMENTS`, al caller como `OWNER` activo, dos filas en',
    '`legal_consents` y la primera `verification_request` (`DRAFT`) — todo en una transacción.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Registrar clínica',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/register',
      description: [
        'Token: **normal**. Requiere `email_verified=true`.',
        '',
        '**Obligatorios**: `name`, `legalName`, `nit`, `phone`, `email`, `department`, `city`,',
        '`address`, `acceptedDataPolicyVersion`, `acceptedTermsVersion`.',
        '`latitude`/`longitude` son opcionales, pero van las dos o ninguna.',
        '',
        'El NIT se valida con el dígito de verificación de la DIAN (formato `123456789-0`).',
        'Las versiones de consentimiento deben coincidir con `LEGAL_DATA_POLICY_VERSION` y',
        '`LEGAL_TERMS_VERSION` del servidor (por defecto `2026-10-v1`).',
        '',
        'Máximo **3 clínicas** por dueño en estado distinto de `REJECTED`.',
        '',
        '**Errores**',
        '- `400 VALIDATION_ERROR` — falta un campo obligatorio o el correo no tiene formato válido.',
        '- `400 INVALID_NIT` — formato o dígito de verificación incorrectos.',
        '- `400 CONSENT_REQUIRED` — falta alguna de las dos versiones de consentimiento.',
        '- `403 EMAIL_NOT_VERIFIED` — el correo de la cuenta no está verificado.',
        '- `409 VETERINARY_NAME_TAKEN` / `VETERINARY_EMAIL_TAKEN` / `NIT_TAKEN`.',
        '- `409 CONSENT_VERSION_OUTDATED` — la versión enviada no es la vigente.',
        '- `409 OWNER_CLINIC_LIMIT_REACHED` — ya tenés 3 clínicas no rechazadas.',
      ].join('\n'),
      body: jsonBody({
        name: 'Clínica Veterinaria El Bosque',
        legalName: 'Clínica Veterinaria El Bosque S.A.S.',
        nit: '900123456-7',
        phone: '+573001234567',
        email: 'contacto@elbosquevet.co',
        department: 'Cundinamarca',
        city: 'Bogotá',
        address: 'Calle 134 # 7-83, Usaquén',
        latitude: 4.710989,
        longitude: -74.072092,
        acceptedDataPolicyVersion: '{{legal_data_policy_version}}',
        acceptedTermsVersion: '{{legal_terms_version}}',
      }),
      tests: expectStatus(201, [{ variable: 'veterinaryId', path: 'body.id' }]),
      examples: [
        {
          name: '201 · Clínica registrada',
          status: 201,
          code: 'Created',
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            name: 'Clínica Veterinaria El Bosque',
            city: 'Bogotá',
            phone: '+573001234567',
            email: 'contacto@elbosquevet.co',
            tenantId: 'f2b7c8d9-1234-4567-89ab-cdef01234567',
            active: false,
            status: 'PENDING_DOCUMENTS',
          },
        },
        {
          name: '409 · NIT ya registrado',
          status: 409,
          code: 'Conflict',
          body: {
            timestamp: '2026-10-30T10:12:00.123',
            status: 409,
            error: 'Conflict',
            message: 'A veterinary with this NIT already exists',
            code: 'NIT_TAKEN',
          },
        },
      ],
    }),
  ],
});

// ---------- 02 · Mis clínicas y perfil ----------

const f02 = folder({
  name: '02 · Mis clínicas y perfil de la clínica',
  auth: bearer('clinic_token'),
  description: [
    '`GET /me` usa el **token normal** (está sobrescrito en el request); el resto de la carpeta usa',
    'el **token de clínica**.',
    '',
    NOTE_DB_ROLE,
    '',
    ERR_CLINIC_TOKEN,
  ].join('\n'),
  items: [
    request({
      name: 'GET Mis clínicas',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/me',
      description: [
        'Token: **normal** (no de clínica). Selector de clínica del frontend: lista las clínicas donde',
        'sos empleado **activo**, con tu rol y el `status` de cada una.',
        '',
        'Guarda el `veterinaryId` de la primera clínica para el resto de la colección.',
      ].join('\n'),
      headers: [{ key: 'Authorization', value: 'Bearer {{access_token}}' }],
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const list = body.veterinaries || body;',
        '    if (Array.isArray(list) && list.length > 0) {',
        '        const first = list[0];',
        '        const id = first.veterinaryId || first.id;',
        '        if (id) pm.environment.set("veterinaryId", id);',
        '    }',
        '}',
      ]),
      examples: [
        {
          name: '200 · Dos clínicas',
          status: 200,
          body: {
            veterinaries: [
              {
                veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
                name: 'Clínica Veterinaria El Bosque',
                city: 'Bogotá',
                role: 'OWNER',
                status: 'ACTIVE',
              },
              {
                veterinaryId: '1b2c3d4e-5f60-7182-93a4-b5c6d7e8f901',
                name: 'Veterinaria Pet Care Medellín',
                city: 'Medellín',
                role: 'VETERINARIAN',
                status: 'PENDING_DOCUMENTS',
              },
            ],
          },
        },
      ],
    }),
    request({
      name: 'GET Perfil de la clínica',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}',
      description: [
        'Token: **de clínica**. Permiso: cualquier miembro activo (`requireMember`).',
        '',
        'Es la **única** pantalla que sigue respondiendo `200` con la clínica `SUSPENDED` (devuelve',
        '`status` y `suspensionReason`), justamente para poder explicar el estado. Todo el resto de',
        'los endpoints con token de clínica responden `403 CLINIC_SUSPENDED`.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Perfil completo',
          status: 200,
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            name: 'Clínica Veterinaria El Bosque',
            legalName: 'Clínica Veterinaria El Bosque S.A.S.',
            nit: '900123456-7',
            email: 'contacto@elbosquevet.co',
            phone: '+573001234567',
            department: 'Cundinamarca',
            city: 'Bogotá',
            address: 'Calle 134 # 7-83, Usaquén',
            status: 'ACTIVE',
            active: true,
            logoUrl: 'https://xyz.supabase.co/storage/v1/object/public/veterinary-logos/veterinaries/9a1c.../logo.png',
            timezone: 'America/Bogota',
            currency: 'COP',
            defaultAppointmentMinutes: 30,
            allowOnlineBooking: true,
            bookingRequiresConfirmation: true,
            cancellationMinHours: 4,
            directoryVisible: true,
            suspendedAt: null,
            suspensionReason: null,
          },
        },
      ],
    }),
    request({
      name: 'PATCH Actualizar datos de la clínica',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}',
      description: [
        'Token: **de clínica**. Permiso: `CLINIC_CONFIGURE` (OWNER o ADMIN).',
        '',
        'Funciona también con la clínica en `PENDING_DOCUMENTS` (no exige estar aprobada).',
        '',
        '- `400 VALIDATION_ERROR` — nombre o correo inválidos.',
        '- `409 VETERINARY_NAME_TAKEN` / `VETERINARY_EMAIL_TAKEN`.',
      ].join('\n'),
      body: jsonBody({
        name: 'Clínica Veterinaria El Bosque',
        city: 'Bogotá',
        phone: '+573009876543',
        email: 'contacto@elbosquevet.co',
      }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Actualizada',
          status: 200,
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            name: 'Clínica Veterinaria El Bosque',
            city: 'Bogotá',
            phone: '+573009876543',
            email: 'contacto@elbosquevet.co',
            status: 'ACTIVE',
          },
        },
      ],
    }),
    request({
      name: 'PATCH Configuración de la clínica',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/settings',
      description: [
        'Token: **de clínica**. Permiso: `CLINIC_CONFIGURE`.',
        '',
        'Parámetros de agenda y visibilidad. Todos los campos son opcionales (actualización parcial).',
      ].join('\n'),
      body: jsonBody({
        timezone: 'America/Bogota',
        currency: 'COP',
        defaultAppointmentMinutes: 30,
        allowOnlineBooking: true,
        bookingRequiresConfirmation: false,
        cancellationMinHours: 6,
        directoryVisible: true,
      }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Configuración guardada',
          status: 200,
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            timezone: 'America/Bogota',
            currency: 'COP',
            defaultAppointmentMinutes: 30,
            allowOnlineBooking: true,
            bookingRequiresConfirmation: false,
            cancellationMinHours: 6,
            directoryVisible: true,
          },
        },
      ],
    }),
    request({
      name: 'PUT Subir logo',
      method: 'PUT',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/logo',
      description: [
        'Token: **de clínica** (o normal si sos el OWNER). Permiso: `requireOwnerOrPermission(CLINIC_CONFIGURE)`.',
        '',
        '`multipart/form-data` con el campo **`file`**. PNG, JPEG o WEBP, máximo **2 MB**.',
        'El tipo se valida por la **firma real del archivo** (magic numbers), no por el `Content-Type`:',
        'renombrar un `.exe` a `.png` no sirve.',
        '',
        '- `400 INVALID_FILE_TYPE` — el archivo no es PNG/JPEG/WEBP de verdad.',
        '- `413 FILE_TOO_LARGE` — más de 2 MB.',
        '- `403 CLINIC_SUSPENDED` — la clínica está suspendida.',
        '- `503 EXTERNAL_SERVICE_UNAVAILABLE` — falló Supabase Storage.',
      ].join('\n'),
      body: formdataBody([
        { key: 'file', type: 'file', src: [], description: 'PNG/JPEG/WEBP ≤ 2MB' },
      ]),
      headers: [],
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Logo subido',
          status: 200,
          body: {
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            logoUrl: 'https://xyz.supabase.co/storage/v1/object/public/veterinary-logos/veterinaries/9a1c.../logo.png',
          },
        },
      ],
    }),
    request({
      name: 'DELETE Quitar logo',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/logo',
      description: 'Token: **de clínica** (o normal si sos el OWNER). Permiso: `requireOwnerOrPermission(CLINIC_CONFIGURE)`. Borra el archivo del bucket público y deja `logoUrl` en `null`.',
      tests: expectStatus(200),
      examples: [
        { name: '200 · Logo eliminado', status: 200, body: { veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', logoUrl: null } },
      ],
    }),
    request({
      name: 'POST Generar código de invitación de clientes',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/invite-code',
      description: [
        'Token: **de clínica**. Permiso: `CLINIC_CONFIGURE`.',
        '',
        '⚠️ Esto **no** tiene nada que ver con las invitaciones de personal (carpeta 08). Es el código',
        '`VET-XXXXXXXX` que usan los **dueños de mascotas** para vincularse a la clínica (carpeta 11).',
      ].join('\n'),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    if (body.inviteCode) pm.environment.set("inviteCode", body.inviteCode);',
        '    if (body.inviteLink) pm.environment.set("inviteLink", body.inviteLink);',
        '}',
      ]),
      examples: [
        {
          name: '200 · Código generado',
          status: 200,
          body: {
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            inviteCode: 'VET-7K2M9XQ4',
            inviteLink: 'https://app.myanimalog.com/vincular?code=VET-7K2M9XQ4',
          },
        },
      ],
    }),
    request({
      name: 'DELETE Borrar código de invitación de clientes',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/invite-code',
      description: 'Token: **de clínica**. Permiso: `CLINIC_CONFIGURE`. Deja `inviteCode` e `inviteLink` en `null`; los enlaces ya repartidos dejan de funcionar.',
      tests: expectStatus(200),
      examples: [
        { name: '200 · Código eliminado', status: 200, body: { veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', inviteCode: null, inviteLink: null } },
      ],
    }),
    request({
      name: 'GET Plan activo',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/subscription/active',
      description: [
        'Token: **de clínica**. Permiso: `requireActiveMember` (cualquier rol activo, clínica no suspendida).',
        '',
        '- `404 NO_ACTIVE_SUBSCRIPTION` — la clínica no tiene suscripción activa.',
        '- `403 CLINIC_SUSPENDED` — clínica suspendida.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Prueba PRO_TRIAL',
          status: 200,
          body: {
            id: '7f8e9d0c-1b2a-3948-5766-madeup000001',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            plan: 'PRO_TRIAL',
            startDate: '2026-10-30',
            endDate: '2026-11-29',
            active: true,
          },
        },
      ],
    }),
    request({
      name: 'GET ¿Plan vencido?',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/subscription/is-expired',
      description: 'Token: **de clínica**. Permiso: `requireActiveMember`. Devuelve si la suscripción actual ya venció.',
      tests: expectStatus(200),
      examples: [
        { name: '200 · Vigente', status: 200, body: { veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', expired: false, endDate: '2026-11-29' } },
      ],
    }),
  ],
});

// ---------- 03 · Verificación (dueño) ----------

const f03 = folder({
  name: '03 · Verificación — asistente del dueño',
  auth: bearer('access_token'),
  description: [
    'Asistente que completa el dueño para que la clínica pase de `PENDING_DOCUMENTS` a `ACTIVE`.',
    '',
    'Token: **normal del OWNER** (también acepta token de clínica si el `vet_id` coincide), pero',
    'siempre se verifica **contra la base** que seas el `OWNER` activo — por eso sigue funcionando',
    'con la clínica `REJECTED` o en cualquier otro estado (`requireVeterinaryOwner`).',
    '',
    'Flujo: representante legal → 3 documentos obligatorios → `GET` para revisar `canSubmit` →',
    'enviar. Si el admin rechaza, `retry` abre un intento nuevo (máximo 3).',
    '',
    '**Errores comunes**',
    '- `403 NOT_VETERINARY_OWNER` — no sos el dueño activo de esa clínica.',
    '- `409 VERIFICATION_LOCKED` — la solicitud no está `DRAFT`/`NEEDS_CORRECTION` (ya se envió).',
  ].join('\n'),
  items: [
    request({
      name: 'GET Estado de la verificación',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/verification',
      description: [
        'Estado del asistente: qué falta, qué documentos hay y si ya se puede enviar.',
        '',
        '`canSubmit` exige: solicitud editable (`canEdit`), representante legal completo y los **3**',
        'documentos obligatorios (`RUT`, `CHAMBER_OF_COMMERCE`, `LEGAL_REP_ID`).',
        '',
        'Nunca devuelve el informe de la IA ni URLs de archivos: solo nombre, tipo, estado y fecha.',
        '',
        'Si el admin pidió correcciones, un documento vuelve a `NEEDS_CORRECTION` **aunque esté',
        'presente**, si se subió antes de que se pidiera la corrección.',
      ].join('\n'),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    if (body.requestId) pm.environment.set("requestId", body.requestId);',
        '    const docs = body.documents || [];',
        '    const ready = docs.find(function (d) { return d.documentId; });',
        '    if (ready) pm.environment.set("documentId", ready.documentId);',
        '}',
      ]),
      examples: [
        {
          name: '200 · Falta un documento',
          status: 200,
          body: {
            requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
            status: 'DRAFT',
            attemptNumber: 1,
            attemptsRemaining: 2,
            canEdit: true,
            canSubmit: false,
            legalRepresentativeComplete: true,
            documents: [
              { documentType: 'RUT', status: 'READY', documentId: 'd1e2f3a4-b5c6-4d78-9e0f-1a2b3c4d5e6f', fileName: 'rut-el-bosque.pdf', uploadedAt: '2026-10-30T09:12:00' },
              { documentType: 'CHAMBER_OF_COMMERCE', status: 'READY', documentId: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', fileName: 'camara-comercio.pdf', uploadedAt: '2026-10-30T09:14:00' },
              { documentType: 'LEGAL_REP_ID', status: 'PENDING', documentId: null, fileName: null, uploadedAt: null },
              { documentType: 'FACADE_PHOTO', status: 'PENDING', documentId: null, fileName: null, uploadedAt: null },
            ],
          },
        },
      ],
    }),
    request({
      name: 'PUT Representante legal',
      method: 'PUT',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/legal-representative',
      description: [
        'Datos de quien firma legalmente por la clínica.',
        '',
        '`documentType`: `CC`, `CE` o `PPT`. Con `CC` el `documentNumber` debe ser **solo dígitos**;',
        'con `CE`/`PPT` puede ser alfanumérico.',
        '',
        '⚠️ El representante legal **no** cambia con la transferencia de propiedad (carpeta 10): si',
        'cambia el dueño y también debe cambiar el representante, hay que actualizarlo acá.',
        '',
        '- `400 VALIDATION_ERROR` — tipo o número de documento inválidos.',
        '- `409 VERIFICATION_LOCKED` — la solicitud ya no es editable.',
      ].join('\n'),
      body: jsonBody({
        legalRepIsOwner: true,
        legalRepName: 'María Fernanda Gómez Restrepo',
        legalRepDocumentType: 'CC',
        legalRepDocumentNumber: '1020304050',
      }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Representante guardado',
          status: 200,
          body: {
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            legalRepIsOwner: true,
            legalRepName: 'María Fernanda Gómez Restrepo',
            legalRepDocumentType: 'CC',
            legalRepDocumentNumber: '1020304050',
          },
        },
      ],
    }),
    request({
      name: 'POST Subir documento de verificación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/verification/documents',
      description: [
        '`multipart/form-data` con **`file`** y **`documentType`**.',
        '',
        '`documentType`: `RUT`, `CHAMBER_OF_COMMERCE`, `LEGAL_REP_ID` (obligatorios) o `FACADE_PHOTO`',
        '(opcional). PDF, JPEG o PNG, máximo **10 MB**, validados por firma real del archivo.',
        '',
        'Subir dos veces el mismo tipo **reemplaza** el anterior (borra la fila y el archivo viejo).',
        'Van a un bucket **privado**: solo se leen con URLs firmadas de 10 minutos desde el panel del admin.',
        '',
        '- `400 INVALID_FILE_TYPE` · `413 FILE_TOO_LARGE`',
        '- `409 VERIFICATION_LOCKED` — solicitud no editable.',
        '- `409 DOCUMENT_NOT_IN_CORRECTION` — en `NEEDS_CORRECTION`, ese tipo no estaba entre los pedidos.',
      ].join('\n'),
      body: formdataBody([
        { key: 'file', type: 'file', src: [], description: 'PDF/JPEG/PNG ≤ 10MB' },
        { key: 'documentType', value: 'RUT', description: 'RUT | CHAMBER_OF_COMMERCE | LEGAL_REP_ID | FACADE_PHOTO' },
      ]),
      headers: [],
      tests: expectStatus(201, [{ variable: 'documentId', path: 'body.documentId || body.id' }]),
      examples: [
        {
          name: '201 · Documento subido',
          status: 201,
          code: 'Created',
          body: {
            documentId: 'd1e2f3a4-b5c6-4d78-9e0f-1a2b3c4d5e6f',
            documentType: 'RUT',
            fileName: 'rut-el-bosque.pdf',
            mimeType: 'application/pdf',
            fileSizeBytes: 284512,
            uploadedAt: '2026-10-30T09:12:00',
          },
        },
      ],
    }),
    request({
      name: 'DELETE Borrar documento de verificación',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/verification/documents/{{documentId}}',
      description: [
        'Borra la fila y el archivo del bucket privado.',
        '',
        '- `404 DOCUMENT_NOT_FOUND` — no existe o no es de esta clínica.',
        '- `409 VERIFICATION_LOCKED` · `409 DOCUMENT_NOT_IN_CORRECTION`.',
      ].join('\n'),
      tests: expectStatus(204),
      examples: [{ name: '204 · Documento eliminado', status: 204, code: 'No Content', body: {} }],
    }),
    request({
      name: 'POST Enviar verificación a revisión',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/verification/submit',
      description: [
        'Pasa la clínica y la solicitud a `UNDER_REVIEW` y publica `VERIFICATION_SUBMITTED`.',
        '',
        '`truthfulnessDeclared` **tiene que ser `true`** (declaración de veracidad).',
        '',
        'Con `includeProfessionalProfile: true` envía **en la misma transacción** el perfil profesional',
        'del dueño (carpeta 04): si ese perfil no está listo, falla todo y la clínica **no** queda',
        'enviada.',
        '',
        '- `400 TRUTHFULNESS_DECLARATION_REQUIRED` — falta la declaración.',
        '- `400 MISSING_REQUIRED_DOCUMENTS` — falta el representante legal completo o alguno de los 3 documentos.',
        '- `409 INVALID_VERIFICATION_STATE` — la solicitud no está `DRAFT`/`NEEDS_CORRECTION`.',
      ].join('\n'),
      body: jsonBody({ truthfulnessDeclared: true, includeProfessionalProfile: false }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Enviada',
          status: 200,
          body: {
            requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
            status: 'UNDER_REVIEW',
            veterinaryStatus: 'UNDER_REVIEW',
            submittedAt: '2026-10-30T09:30:00',
            attemptNumber: 1,
          },
        },
        {
          name: '400 · Faltan documentos',
          status: 400,
          code: 'Bad Request',
          body: {
            timestamp: '2026-10-30T09:29:00.001',
            status: 400,
            error: 'Bad Request',
            message: 'Missing required documents: LEGAL_REP_ID',
            code: 'MISSING_REQUIRED_DOCUMENTS',
          },
        },
      ],
    }),
    request({
      name: 'POST Reintentar verificación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/verification/retry',
      description: [
        'Solo si la última solicitud quedó **`REJECTED`**. Crea una solicitud `DRAFT` nueva',
        '(`attemptNumber + 1`, **sin copiar documentos**) y devuelve la clínica a `PENDING_DOCUMENTS`.',
        '',
        '- `409 INVALID_VERIFICATION_STATE` — la última solicitud no está rechazada.',
        '- `409 VERIFICATION_ATTEMPTS_EXHAUSTED` — ya se agotaron los 3 intentos.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Intento 2 abierto',
          status: 200,
          body: {
            requestId: 'f6a7b8c9-d0e1-4234-9567-8a9b0c1d2e3f',
            status: 'DRAFT',
            attemptNumber: 2,
            attemptsRemaining: 1,
            veterinaryStatus: 'PENDING_DOCUMENTS',
          },
        },
      ],
    }),
  ],
});

// ---------- 04 · Perfil profesional ----------

const f04 = folder({
  name: '04 · Perfil profesional del veterinario',
  auth: bearer('access_token'),
  description: [
    'Token: **normal**. El perfil es siempre el del `userId` del token, nunca un parámetro.',
    '',
    'La licencia es **de la persona**, no del empleo: un veterinario tiene un solo perfil aunque',
    'trabaje en varias clínicas. Su `licenseStatus` (`NOT_SUBMITTED`/`PENDING`/`VERIFIED`/`REJECTED`)',
    'es independiente de la verificación de cualquier clínica.',
    '',
    'El último request de la carpeta es la excepción: usa **token de clínica** para ver el perfil',
    '(reducido) de un colega.',
  ].join('\n'),
  items: [
    request({
      name: 'GET Mi perfil profesional',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/me/professional-profile',
      description: 'Token: **normal**. `404 PROFESSIONAL_PROFILE_NOT_FOUND` si todavía no creaste el perfil.',
      tests: expectStatus(200, [{ variable: 'professionalProfileId', path: 'body.id' }]),
      examples: [
        {
          name: '200 · Perfil en borrador',
          status: 200,
          body: {
            id: 'a1b2c3d4-e5f6-4789-0abc-def012345678',
            fullName: 'Juan David Martínez Soto',
            documentType: 'CC',
            documentNumber: '1098765432',
            licenseNumber: 'MVZ-45821',
            profession: 'MVZ',
            specialty: 'Medicina interna de pequeños animales',
            university: 'Universidad Nacional de Colombia',
            licenseStatus: 'NOT_SUBMITTED',
            signatureUrl: null,
            documents: [
              { documentType: 'PROFESSIONAL_CARD', status: 'PENDING', documentId: null },
              { documentType: 'ID_DOCUMENT', status: 'PENDING', documentId: null },
              { documentType: 'DEGREE', status: 'PENDING', documentId: null },
            ],
          },
        },
      ],
    }),
    request({
      name: 'PUT Crear o actualizar mi perfil profesional',
      method: 'PUT',
      raw: '{{vet_url}}/api/veterinary/me/professional-profile',
      description: [
        'Token: **normal**. Crea el perfil (+ su `verification_request` `DRAFT`) o lo actualiza.',
        '',
        'Reglas según el estado de la licencia:',
        '- `PENDING` → **no se puede editar nada** (`409 VERIFICATION_LOCKED`).',
        '- `VERIFIED` → cambiar un dato de identidad (`fullName`, `documentType`, `documentNumber`,',
        '  `profession`, `licenseNumber`) devuelve la licencia a `NOT_SUBMITTED` y abre una solicitud',
        '  nueva. `specialty` y `university` se editan sin disparar eso.',
        '- `REJECTED` → cualquier edición abre un reintento (máximo 3).',
        '',
        '- `400 VALIDATION_ERROR` — datos inválidos.',
        '- `409 LICENSE_NUMBER_TAKEN` — ese número de licencia ya es de otra persona.',
        '- `409 VERIFICATION_ATTEMPTS_EXHAUSTED` — 3 intentos agotados.',
      ].join('\n'),
      body: jsonBody({
        fullName: 'Juan David Martínez Soto',
        documentType: 'CC',
        documentNumber: '1098765432',
        licenseNumber: 'MVZ-45821',
        profession: 'MVZ',
        specialty: 'Medicina interna de pequeños animales',
        university: 'Universidad Nacional de Colombia',
      }),
      tests: expectStatus(200, [{ variable: 'professionalProfileId', path: 'body.id' }]),
      examples: [
        {
          name: '200 · Perfil creado',
          status: 200,
          body: {
            id: 'a1b2c3d4-e5f6-4789-0abc-def012345678',
            fullName: 'Juan David Martínez Soto',
            licenseNumber: 'MVZ-45821',
            profession: 'MVZ',
            licenseStatus: 'NOT_SUBMITTED',
          },
        },
      ],
    }),
    request({
      name: 'POST Subir documento del perfil profesional',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/me/professional-profile/documents',
      description: [
        '`multipart/form-data` con **`file`** y **`documentType`**.',
        '',
        '`documentType`: `PROFESSIONAL_CARD` e `ID_DOCUMENT` (obligatorios para enviar) o `DEGREE`',
        '(opcional). PDF/JPEG/PNG ≤ 10 MB, por firma real. Bucket privado.',
        '',
        'Mismas reglas de reemplazo y de `NEEDS_CORRECTION` que los documentos de la clínica.',
      ].join('\n'),
      body: formdataBody([
        { key: 'file', type: 'file', src: [], description: 'PDF/JPEG/PNG ≤ 10MB' },
        { key: 'documentType', value: 'PROFESSIONAL_CARD', description: 'PROFESSIONAL_CARD | ID_DOCUMENT | DEGREE' },
      ]),
      headers: [],
      tests: expectStatus(201, [{ variable: 'professionalDocumentId', path: 'body.documentId || body.id' }]),
      examples: [
        {
          name: '201 · Tarjeta profesional subida',
          status: 201,
          code: 'Created',
          body: {
            documentId: 'b2c3d4e5-f6a7-4890-1bcd-ef0123456789',
            documentType: 'PROFESSIONAL_CARD',
            fileName: 'tarjeta-profesional.pdf',
            mimeType: 'application/pdf',
            fileSizeBytes: 198340,
            uploadedAt: '2026-10-30T10:02:00',
          },
        },
      ],
    }),
    request({
      name: 'DELETE Borrar documento del perfil profesional',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/me/professional-profile/documents/{{professionalDocumentId}}',
      description: 'Token: **normal**. Solo con la solicitud en `DRAFT`/`NEEDS_CORRECTION`. `404 DOCUMENT_NOT_FOUND` si no es tuyo.',
      tests: expectStatus(204),
      examples: [{ name: '204 · Eliminado', status: 204, code: 'No Content', body: {} }],
    }),
    request({
      name: 'PUT Subir firma',
      method: 'PUT',
      raw: '{{vet_url}}/api/veterinary/me/professional-profile/signature',
      description: [
        '`multipart/form-data` con **`file`**. **Solo PNG**, máximo **1 MB**.',
        '',
        'Se puede resubir en cualquier estado de la solicitud (no está sujeta a `DRAFT`/`NEEDS_CORRECTION`',
        'como los otros documentos). Se guarda la **ruta** en el bucket privado, nunca una URL pública.',
      ].join('\n'),
      body: formdataBody([{ key: 'file', type: 'file', src: [], description: 'PNG ≤ 1MB' }]),
      headers: [],
      tests: expectStatus(200),
      examples: [
        { name: '200 · Firma guardada', status: 200, body: { profileId: 'a1b2c3d4-e5f6-4789-0abc-def012345678', signatureUrl: 'professionals/a1b2c3d4-e5f6-4789-0abc-def012345678/signature.png' } },
      ],
    }),
    request({
      name: 'POST Enviar perfil profesional a revisión',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/me/professional-profile/submit',
      description: [
        'Token: **normal**. Exige `PROFESSIONAL_CARD` e `ID_DOCUMENT` subidos.',
        '',
        'Pasa `licenseStatus` a `PENDING`, la solicitud a `UNDER_REVIEW`, y publica',
        '`VERIFICATION_SUBMITTED` con `subjectType=PROFESSIONAL`.',
        '',
        '- `400 MISSING_REQUIRED_DOCUMENTS` — falta alguno de los dos obligatorios.',
        '- `409 INVALID_VERIFICATION_STATE` — la solicitud no está `DRAFT`/`NEEDS_CORRECTION`.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Enviado',
          status: 200,
          body: {
            profileId: 'a1b2c3d4-e5f6-4789-0abc-def012345678',
            licenseStatus: 'PENDING',
            requestId: 'c4d5e6f7-a8b9-4012-8345-6789abcdef01',
            status: 'UNDER_REVIEW',
            submittedAt: '2026-10-30T10:15:00',
          },
        },
      ],
    }),
    request({
      name: 'GET Perfil profesional de un empleado',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/employees/{{employeeId}}/professional-profile',
      description: [
        'Token: **de clínica**. Permiso: `requireMember` (cualquier miembro).',
        '',
        'Vista reducida de un colega: `fullName`, `profession`, `licenseNumber`, `specialty`,',
        '`university`, `licenseStatus`. **Nunca** devuelve documento de identidad, documentos ni firma.',
        '',
        '- `404 EMPLOYEE_NOT_FOUND` — el empleado no es de esta clínica.',
        '- `404 PROFESSIONAL_PROFILE_NOT_FOUND` — ese empleado no tiene perfil profesional.',
      ].join('\n'),
      headers: [{ key: 'Authorization', value: 'Bearer {{clinic_token}}' }],
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Perfil del colega',
          status: 200,
          body: {
            fullName: 'Juan David Martínez Soto',
            profession: 'MVZ',
            licenseNumber: 'MVZ-45821',
            specialty: 'Medicina interna de pequeños animales',
            university: 'Universidad Nacional de Colombia',
            licenseStatus: 'VERIFIED',
          },
        },
      ],
    }),
  ],
});

// ---------- 05 · Admin · Verificaciones ----------

const f05 = folder({
  name: '05 · Admin · Cola de revisión de verificaciones',
  auth: bearer('admin_token'),
  description: [
    'Token: **de un usuario `PLATFORM_ADMIN`** (`{{admin_token}}`).',
    '',
    'Las tres decisiones (`approve`, `reject`, `request-correction`) comparten reglas:',
    'la solicitud debe estar `UNDER_REVIEW`, un admin **no puede decidir su propia solicitud**',
    '(`403 CANNOT_REVIEW_OWN_REQUEST`), y la toman con un `UPDATE` condicional: si otro admin',
    'decidió primero, responde `409 INVALID_VERIFICATION_STATE` en vez de pisar la decisión.',
    '',
    ERR_ADMIN,
  ].join('\n'),
  items: [
    request({
      name: 'GET Cola de verificaciones',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/verifications?status=UNDER_REVIEW&subjectType=CLINIC&page=0&size=20',
      description: [
        'Cola de revisión, con `counts` por estado. Por defecto `status=UNDER_REVIEW`.',
        '',
        'Los `counts` respetan el filtro `subjectType` pero **no** el de `status` (son el total por',
        'estado dentro de ese tipo de sujeto).',
      ].join('\n'),
      query: [
        { key: 'status', value: 'UNDER_REVIEW', description: 'DRAFT | UNDER_REVIEW | NEEDS_CORRECTION | APPROVED | REJECTED (default UNDER_REVIEW)' },
        { key: 'subjectType', value: 'CLINIC', description: 'CLINIC | PROFESSIONAL', disabled: true },
        { key: 'page', value: '0', description: 'desde 0' },
        { key: 'size', value: '20', description: 'máximo 50' },
      ],
      tests: expectStatus(200, [{ variable: 'requestId', path: '(body.items && body.items[0] && body.items[0].requestId)' }]),
      examples: [
        {
          name: '200 · Una clínica esperando',
          status: 200,
          body: {
            items: [
              {
                requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
                subjectType: 'CLINIC',
                subjectId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
                subjectName: 'Clínica Veterinaria El Bosque',
                status: 'UNDER_REVIEW',
                attemptNumber: 1,
                submittedAt: '2026-10-30T09:30:00',
                aiStatus: 'NOT_RUN',
                aiRiskLevel: null,
              },
            ],
            total: 1,
            page: 0,
            size: 20,
            counts: { UNDER_REVIEW: 1, NEEDS_CORRECTION: 0, APPROVED: 12, REJECTED: 3 },
          },
        },
      ],
    }),
    request({
      name: 'GET Detalle de una verificación',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/verifications/{{requestId}}',
      description: [
        'Todo lo que el admin necesita para decidir: datos declarados, `account` del solicitante',
        '(vía User service; cae a `{ id }` si falla), documentos con **`signedUrl` de 10 minutos**,',
        'resultado de la IA, `duplicates` (mismo `sha256` en otro sujeto) e historial de intentos.',
        '',
        '`registryLookupUrl` apunta al RUES (clínicas) o a COMVEZCOL (profesionales) para el',
        'chequeo manual en el registro público.',
      ].join('\n'),
      tests: expectStatus(200, [{ variable: 'documentId', path: '(body.documents && body.documents[0] && body.documents[0].documentId)' }]),
      examples: [
        {
          name: '200 · Detalle de clínica',
          status: 200,
          body: {
            requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
            subjectType: 'CLINIC',
            status: 'UNDER_REVIEW',
            attemptNumber: 1,
            submittedAt: '2026-10-30T09:30:00',
            truthfulnessDeclaredAt: '2026-10-30T09:30:00',
            registryChecked: false,
            registryLookupUrl: 'https://www.rues.org.co/',
            declared: {
              name: 'Clínica Veterinaria El Bosque',
              legalName: 'Clínica Veterinaria El Bosque S.A.S.',
              nit: '900123456-7',
              city: 'Bogotá',
              department: 'Cundinamarca',
              address: 'Calle 134 # 7-83, Usaquén',
              legalRepName: 'María Fernanda Gómez Restrepo',
              legalRepDocumentType: 'CC',
              legalRepDocumentNumber: '1020304050',
            },
            account: { id: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7', email: 'owner@elbosquevet.co', name: 'María Fernanda Gómez' },
            documents: [
              {
                documentId: 'd1e2f3a4-b5c6-4d78-9e0f-1a2b3c4d5e6f',
                documentType: 'RUT',
                fileName: 'rut-el-bosque.pdf',
                mimeType: 'application/pdf',
                signedUrl: 'https://xyz.supabase.co/storage/v1/object/sign/verification-documents/...',
                signedUrlExpiresAt: '2026-10-30T10:40:00',
              },
            ],
            ai: { status: 'NOT_RUN', riskLevel: null, report: null },
            duplicates: [],
            history: [{ requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d', attemptNumber: 1, status: 'UNDER_REVIEW', submittedAt: '2026-10-30T09:30:00', decidedAt: null }],
          },
        },
      ],
    }),
    request({
      name: 'GET Renovar URL firmada de un documento',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/verifications/{{requestId}}/documents/{{documentId}}/signed-url',
      description: 'Para cuando la URL de 10 minutos del visor venció. Devuelve una nueva. `404 DOCUMENT_NOT_FOUND` si el documento no pertenece a esa solicitud.',
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · URL renovada',
          status: 200,
          body: {
            documentId: 'd1e2f3a4-b5c6-4d78-9e0f-1a2b3c4d5e6f',
            signedUrl: 'https://xyz.supabase.co/storage/v1/object/sign/verification-documents/...',
            expiresAt: '2026-10-30T10:50:00',
          },
        },
      ],
    }),
    request({
      name: 'POST Aprobar verificación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/admin/verifications/{{requestId}}/approve',
      description: [
        '`registryChecked` **tiene que ser `true`**: es la constancia de que el admin verificó el',
        'registro público (RUES/COMVEZCOL) a mano.',
        '',
        'Clínica → `ACTIVE` + prueba `PRO_TRIAL` de 30 días si no tenía suscripción activa.',
        'Perfil profesional → `VERIFIED`. Publica `VERIFICATION_APPROVED`.',
        '',
        '- `400 REGISTRY_CHECK_REQUIRED` — `registryChecked` no es `true`.',
        '- `403 CANNOT_REVIEW_OWN_REQUEST` — es tu propia solicitud.',
        '- `409 INVALID_VERIFICATION_STATE` — no está `UNDER_REVIEW`, u otro admin decidió primero.',
      ].join('\n'),
      body: jsonBody({ registryChecked: true, notes: 'NIT verificado en RUES. Cámara de comercio vigente.' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Aprobada con prueba',
          status: 200,
          body: {
            requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
            status: 'APPROVED',
            decidedAt: '2026-10-30T11:00:00',
            subjectType: 'CLINIC',
            veterinaryStatus: 'ACTIVE',
            trial: { plan: 'PRO_TRIAL', startDate: '2026-10-30', endDate: '2026-11-29' },
          },
        },
        {
          name: '400 · Falta el chequeo del registro',
          status: 400,
          code: 'Bad Request',
          body: {
            timestamp: '2026-10-30T10:59:00.001',
            status: 400,
            error: 'Bad Request',
            message: 'registryChecked must be true',
            code: 'REGISTRY_CHECK_REQUIRED',
          },
        },
      ],
    }),
    request({
      name: 'POST Rechazar verificación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/admin/verifications/{{requestId}}/reject',
      description: [
        '`reasonCode` y `notes` son **obligatorios**. `reasonCode`: `NOT_IN_REGISTRY`,',
        '`DATA_MISMATCH`, `DOCUMENT_INVALID` u `OTHER`. `notes` máximo 1000 caracteres.',
        '',
        'Clínica → `REJECTED`; perfil → `REJECTED` con `rejectionReason = notes`.',
        'Publica `VERIFICATION_REJECTED` con `attemptsRemaining` y `documentsDeletionDate`',
        '(los documentos de solicitudes rechazadas se borran a los 90 días, carpeta 13).',
        '',
        '- `400 VALIDATION_ERROR` — falta `reasonCode`/`notes` o las notas son muy largas.',
        '- `403 CANNOT_REVIEW_OWN_REQUEST` · `409 INVALID_VERIFICATION_STATE`.',
      ].join('\n'),
      body: jsonBody({
        reasonCode: 'DATA_MISMATCH',
        notes: 'El NIT del RUT no coincide con el registrado en el formulario. Corregir y volver a enviar.',
      }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Rechazada',
          status: 200,
          body: {
            requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
            status: 'REJECTED',
            reasonCode: 'DATA_MISMATCH',
            decidedAt: '2026-10-30T11:05:00',
            attemptsRemaining: 2,
            documentsDeletionDate: '2027-01-28',
          },
        },
      ],
    }),
    request({
      name: 'POST Pedir correcciones',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/admin/verifications/{{requestId}}/request-correction',
      description: [
        '`notes` y `documentTypes` **obligatorios**. Cada tipo debe pertenecer al sujeto',
        '(`400 VALIDATION_ERROR` si se pide un `PROFESSIONAL_CARD` en una solicitud de clínica).',
        '',
        'Clínica → `NEEDS_CORRECTION`; perfil → `NOT_SUBMITTED` (vuelve a editable).',
        'Solo se podrán resubir los tipos pedidos (`409 DOCUMENT_NOT_IN_CORRECTION` para el resto).',
        'Publica `VERIFICATION_CORRECTION_REQUESTED`.',
      ].join('\n'),
      body: jsonBody({
        notes: 'La foto de la cédula del representante legal está ilegible. Subir una nueva en buena resolución.',
        documentTypes: ['LEGAL_REP_ID'],
      }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Correcciones pedidas',
          status: 200,
          body: {
            requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d',
            status: 'NEEDS_CORRECTION',
            correctionDocumentTypes: ['LEGAL_REP_ID'],
            correctionRequestedAt: '2026-10-30T11:10:00',
            veterinaryStatus: 'NEEDS_CORRECTION',
          },
        },
      ],
    }),
    request({
      name: 'POST Re-ejecutar análisis de IA',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/admin/verifications/{{requestId}}/rerun-ai',
      description: [
        'Vuelve a lanzar el análisis automático de documentos.',
        '',
        '⚠️ Con `AI_VERIFICATION_ENABLED=false` (lo actual en el piloto) responde siempre',
        '`409 AI_VERIFICATION_DISABLED`. Es el comportamiento esperado, no un error.',
      ].join('\n'),
      tests: testScript([
        'pm.test("200 si la IA esta prendida, 409 AI_VERIFICATION_DISABLED si no", function () {',
        '    pm.expect([200, 202, 409]).to.include(pm.response.code);',
        '});',
      ]),
      examples: [
        {
          name: '409 · IA apagada (lo normal hoy)',
          status: 409,
          code: 'Conflict',
          body: {
            timestamp: '2026-10-30T11:12:00.001',
            status: 409,
            error: 'Conflict',
            message: 'AI verification is disabled',
            code: 'AI_VERIFICATION_DISABLED',
          },
        },
      ],
    }),
  ],
});

// ---------- 06 · Admin · Clínicas ----------

const f06 = folder({
  name: '06 · Admin · Clínicas',
  auth: bearer('admin_token'),
  description: [
    'Token: **`PLATFORM_ADMIN`**.',
    '',
    'Incluye también crear/actualizar el plan de una clínica: esos dos endpoints exigen',
    '`PLATFORM_ADMIN` aunque su ruta no esté bajo `/admin/**` (se dejaron donde estaban para no',
    'romper el frontend).',
    '',
    ERR_ADMIN,
  ].join('\n'),
  items: [
    request({
      name: 'GET Listar clínicas',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/veterinaries?status=ACTIVE&search=bosque&city=Bogotá&department=Cundinamarca&plan=PRO_TRIAL&page=0&size=20',
      description: [
        'Listado con filtros. `size` máximo **50**. Ordenado por `createdAt` descendente.',
        '',
        '`search` busca en nombre, razón social y NIT.',
        '',
        '⚠️ Este listado **no** resuelve `ownerName`/`ownerEmail` (haría N llamadas al User service por',
        'página). Esos datos están en el detalle.',
      ].join('\n'),
      query: [
        { key: 'status', value: 'ACTIVE', description: 'PENDING_DOCUMENTS | UNDER_REVIEW | NEEDS_CORRECTION | ACTIVE | REJECTED | SUSPENDED', disabled: true },
        { key: 'search', value: 'bosque', description: 'nombre, razón social o NIT', disabled: true },
        { key: 'city', value: 'Bogotá', disabled: true },
        { key: 'department', value: 'Cundinamarca', disabled: true },
        { key: 'plan', value: 'PRO_TRIAL', disabled: true },
        { key: 'page', value: '0' },
        { key: 'size', value: '20', description: 'máximo 50' },
      ],
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Una clínica',
          status: 200,
          body: {
            items: [
              {
                id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
                name: 'Clínica Veterinaria El Bosque',
                legalName: 'Clínica Veterinaria El Bosque S.A.S.',
                nit: '900123456-7',
                city: 'Bogotá',
                department: 'Cundinamarca',
                status: 'ACTIVE',
                plan: 'PRO_TRIAL',
                employeeCount: 4,
                createdAt: '2026-10-30T09:00:00',
                approvedAt: '2026-10-30T11:00:00',
              },
            ],
            total: 1,
            page: 0,
            size: 20,
          },
        },
      ],
    }),
    request({
      name: 'GET Detalle de una clínica',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/veterinaries/{{veterinaryId}}',
      description: [
        'Ficha completa: datos, estado, suscripción, equipo e historial de verificaciones.',
        '',
        '`ownerName` sale del User service, con **fallback a `legalRepName`** si no viene o si el',
        'User service falla.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Detalle',
          status: 200,
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            name: 'Clínica Veterinaria El Bosque',
            legalName: 'Clínica Veterinaria El Bosque S.A.S.',
            nit: '900123456-7',
            status: 'ACTIVE',
            city: 'Bogotá',
            department: 'Cundinamarca',
            address: 'Calle 134 # 7-83, Usaquén',
            phone: '+573001234567',
            email: 'contacto@elbosquevet.co',
            ownerId: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7',
            ownerName: 'María Fernanda Gómez',
            legalRepName: 'María Fernanda Gómez Restrepo',
            createdAt: '2026-10-30T09:00:00',
            approvedAt: '2026-10-30T11:00:00',
            suspendedAt: null,
            suspensionReason: null,
            subscription: { plan: 'PRO_TRIAL', startDate: '2026-10-30', endDate: '2026-11-29', active: true },
            employees: [
              { employeeId: '11aa22bb-33cc-4dd5-8ee6-ff7788990011', role: 'OWNER', active: true },
              { employeeId: '22bb33cc-44dd-4ee6-8ff7-889900112233', role: 'VETERINARIAN', active: true },
            ],
            verificationHistory: [
              { requestId: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d', attemptNumber: 1, status: 'APPROVED', submittedAt: '2026-10-30T09:30:00', decidedAt: '2026-10-30T11:00:00' },
            ],
          },
        },
      ],
    }),
    request({
      name: 'PATCH Suspender clínica',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/admin/veterinaries/{{veterinaryId}}/suspend',
      description: [
        'Solo `ACTIVE → SUSPENDED`, con un `UPDATE` condicional (si otro admin cambió el estado,',
        '`409 CLINIC_NOT_ACTIVE`).',
        '',
        '`reason` **obligatorio**, entre **10 y 500** caracteres. Se guarda en `suspension_reason` y',
        'se muestra en el perfil de la clínica.',
        '',
        'Un admin **no puede suspender una clínica donde es empleado o dueño**',
        '(`403 CANNOT_MANAGE_OWN_CLINIC`).',
        '',
        'Efecto inmediato: todos los endpoints con token de clínica de esa clínica empiezan a',
        'responder `403 CLINIC_SUSPENDED`, **sin esperar** a que expiren los tokens ya emitidos.',
        'La única excepción es `GET /api/veterinary/{id}` (perfil), para poder mostrar el motivo.',
      ].join('\n'),
      body: jsonBody({ reason: 'Reporte de ejercicio ilegal de la profesión; verificación de licencias en curso.' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Suspendida',
          status: 200,
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            name: 'Clínica Veterinaria El Bosque',
            status: 'SUSPENDED',
            active: false,
            suspendedAt: '2026-10-30T12:00:00',
            suspensionReason: 'Reporte de ejercicio ilegal de la profesión; verificación de licencias en curso.',
          },
        },
        {
          name: '409 · No estaba activa',
          status: 409,
          code: 'Conflict',
          body: {
            timestamp: '2026-10-30T12:00:00.001',
            status: 409,
            error: 'Conflict',
            message: 'Clinic is not ACTIVE',
            code: 'CLINIC_NOT_ACTIVE',
          },
        },
      ],
    }),
    request({
      name: 'PATCH Reactivar clínica',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/admin/veterinaries/{{veterinaryId}}/reactivate',
      description: [
        'Solo `SUSPENDED → ACTIVE` (`409 CLINIC_NOT_SUSPENDED` si no). Limpia `suspendedAt` y',
        '`suspensionReason`; **no** toca `approvedAt`.',
        '',
        'Misma regla de "no es mi propia clínica" (`403 CANNOT_MANAGE_OWN_CLINIC`).',
        '',
        '⚠️ Esto **no** sirve para aprobar una clínica nueva: eso es exclusivo de',
        '`POST /admin/verifications/{requestId}/approve` (carpeta 05).',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Reactivada',
          status: 200,
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            status: 'ACTIVE',
            active: true,
            suspendedAt: null,
            suspensionReason: null,
          },
        },
      ],
    }),
    request({
      name: 'POST Crear plan de suscripción',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/subscription',
      description: [
        'Token: **`PLATFORM_ADMIN`** (aunque la ruta no esté bajo `/admin/**`).',
        '',
        '- `409 ACTIVE_SUBSCRIPTION_ALREADY_EXISTS` — la clínica ya tiene una suscripción activa.',
        '- `409 VETERINARY_NOT_ACTIVE` — la clínica no está activa.',
        '- `400 VALIDATION_ERROR` — falta el nombre del plan.',
      ].join('\n'),
      body: jsonBody({ plan: 'PRO', startDate: '2026-11-01', endDate: '2027-11-01' }),
      tests: expectStatus(201),
      examples: [
        {
          name: '201 · Plan creado',
          status: 201,
          code: 'Created',
          body: {
            id: '8e9f0a1b-2c3d-4e5f-9607-182930abcdef',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            plan: 'PRO',
            startDate: '2026-11-01',
            endDate: '2027-11-01',
            active: true,
          },
        },
      ],
    }),
    request({
      name: 'PATCH Actualizar plan de suscripción',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/subscription',
      description: 'Token: **`PLATFORM_ADMIN`**. Opera sobre la suscripción activa (o la última). `404 SUBSCRIPTION_NOT_FOUND` si no hay ninguna.',
      body: jsonBody({ plan: 'PRO', endDate: '2028-11-01' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Plan actualizado',
          status: 200,
          body: {
            id: '8e9f0a1b-2c3d-4e5f-9607-182930abcdef',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            plan: 'PRO',
            startDate: '2026-11-01',
            endDate: '2028-11-01',
            active: true,
          },
        },
      ],
    }),
  ],
});

// ---------- 07 · Admin · Profesionales ----------

const f07 = folder({
  name: '07 · Admin · Profesionales',
  auth: bearer('admin_token'),
  description: ['Token: **`PLATFORM_ADMIN`**.', '', ERR_ADMIN].join('\n'),
  items: [
    request({
      name: 'GET Listar profesionales',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/professionals?search=martinez&verified=true&page=0&size=20',
      description: [
        '`search` busca **solo** por `fullName` y `licenseNumber` — nunca por correo, porque el perfil',
        'profesional no guarda el correo. `verified=true` filtra `licenseStatus == VERIFIED`.',
      ].join('\n'),
      query: [
        { key: 'search', value: 'martinez', description: 'nombre o número de licencia', disabled: true },
        { key: 'verified', value: 'true', description: 'true = solo licencias VERIFIED', disabled: true },
        { key: 'page', value: '0' },
        { key: 'size', value: '20', description: 'máximo 50' },
      ],
      tests: expectStatus(200, [{ variable: 'professionalId', path: '(body.items && body.items[0] && body.items[0].id)' }]),
      examples: [
        {
          name: '200 · Un profesional',
          status: 200,
          body: {
            items: [
              {
                id: 'a1b2c3d4-e5f6-4789-0abc-def012345678',
                fullName: 'Juan David Martínez Soto',
                licenseNumber: 'MVZ-45821',
                profession: 'MVZ',
                licenseStatus: 'VERIFIED',
                verifiedAt: '2026-10-30T11:20:00',
                clinics: [{ veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', name: 'Clínica Veterinaria El Bosque', role: 'VETERINARIAN' }],
              },
            ],
            total: 1,
            page: 0,
            size: 20,
          },
        },
      ],
    }),
    request({
      name: 'GET Detalle de un profesional',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/admin/professionals/{{professionalId}}',
      description: [
        'Ficha completa, incluido el documento de identidad y el historial de verificaciones.',
        '',
        'Es el **único** lugar donde se resuelve el correo de un profesional (una llamada al User',
        'service; si falla, `email: null`).',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Detalle',
          status: 200,
          body: {
            id: 'a1b2c3d4-e5f6-4789-0abc-def012345678',
            userId: '8d34b5c6-7d8e-4f90-a1b2-c3d4e5f6a7b8',
            email: 'juan.martinez@elbosquevet.co',
            fullName: 'Juan David Martínez Soto',
            documentType: 'CC',
            documentNumber: '1098765432',
            licenseNumber: 'MVZ-45821',
            profession: 'MVZ',
            specialty: 'Medicina interna de pequeños animales',
            university: 'Universidad Nacional de Colombia',
            licenseStatus: 'VERIFIED',
            verifiedAt: '2026-10-30T11:20:00',
            verifiedBy: '3f4a5b6c-7d8e-4901-a2b3-c4d5e6f7a8b9',
            rejectionReason: null,
            registryLookupUrl: 'https://consejoprofesionalmvz.gov.co/consulta-de-profesionales/',
            verificationHistory: [{ requestId: 'c4d5e6f7-a8b9-4012-8345-6789abcdef01', attemptNumber: 1, status: 'APPROVED', submittedAt: '2026-10-30T10:15:00', decidedAt: '2026-10-30T11:20:00' }],
          },
        },
      ],
    }),
  ],
});

// ---------- 08 · Invitaciones (clínica) ----------

const f08 = folder({
  name: '08 · Personal · Invitaciones (lado de la clínica)',
  auth: bearer('clinic_token'),
  description: [
    'Token: **de clínica**. Permiso: `STAFF_MANAGE` (OWNER o ADMIN).',
    '',
    '**Solo el OWNER** puede invitar con rol `ADMIN` o reenviar una invitación de `ADMIN`.',
    'Nunca se puede invitar como `OWNER` (a OWNER se llega registrando la clínica o por',
    'transferencia de propiedad).',
    '',
    'La clínica tiene que estar **`ACTIVE`** para invitar.',
    '',
    'El token de invitación viaja **solo en el correo** (evento `EMPLOYEE_INVITED`); en la base se',
    'guarda únicamente su hash SHA-256 y **ninguna** respuesta lo devuelve. Con `EVENTS_ENABLED=false`',
    'no se envía el correo: ver el README para fijar un token de prueba con SQL.',
    '',
    ERR_CLINIC_TOKEN,
  ].join('\n'),
  items: [
    request({
      name: 'POST Invitar empleado',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/invitations',
      description: [
        'Permiso: `STAFF_MANAGE`; + ser **OWNER** si `role=ADMIN`.',
        '',
        '`role`: `ADMIN`, `VETERINARIAN`, `ASSISTANT` o `RECEPTIONIST`.',
        'El correo se normaliza a minúsculas. La invitación vence en **7 días**.',
        '',
        'Si ya existía una invitación `PENDING` **vencida** para ese correo, se marca `REVOKED` y se',
        'crea una nueva. Si la `PENDING` está **vigente**, responde `409`.',
        '',
        '`seatsUsed` (empleados activos + invitaciones pendientes vigentes) es **informativo**: el plan',
        'todavía no tiene límite de empleados, así que no bloquea nada.',
        '',
        '- `400 VALIDATION_ERROR` — correo ausente o mal formado.',
        '- `400 INVALID_EMPLOYEE_ROLE` — rol ausente o `OWNER`.',
        '- `403 ONLY_OWNER_CAN_MANAGE_ADMINS` — un ADMIN intentó invitar a un ADMIN.',
        '- `409 INVITATION_ALREADY_PENDING` — ya hay una invitación vigente para ese correo.',
        '- `409 EMPLOYEE_ALREADY_EXISTS` — ese correo ya es empleado activo.',
        '- `409 CLINIC_NOT_ACTIVE` — la clínica no está aprobada.',
      ].join('\n'),
      body: jsonBody({ email: 'laura.herrera@gmail.com', role: 'VETERINARIAN' }),
      tests: expectStatus(201, [{ variable: 'invitationId', path: 'body.invitation && body.invitation.id' }]),
      examples: [
        {
          name: '201 · Invitación creada',
          status: 201,
          code: 'Created',
          body: {
            invitation: {
              id: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
              veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
              email: 'laura.herrera@gmail.com',
              role: 'VETERINARIAN',
              status: 'PENDING',
              expired: false,
              invitedBy: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7',
              expiresAt: '2026-11-06T12:30:00',
              decidedAt: null,
              createdAt: '2026-10-30T12:30:00',
            },
            seatsUsed: 5,
          },
        },
        {
          name: '403 · Un ADMIN no puede invitar ADMINs',
          status: 403,
          code: 'Forbidden',
          body: {
            timestamp: '2026-10-30T12:31:00.001',
            status: 403,
            error: 'Forbidden',
            message: 'Only the clinic OWNER can manage ADMIN staff',
            code: 'ONLY_OWNER_CAN_MANAGE_ADMINS',
          },
        },
      ],
    }),
    request({
      name: 'GET Listar invitaciones',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/invitations?status=PENDING',
      description: [
        'Por defecto `status=PENDING`. Para ver el historial hay que pedir `ACCEPTED`, `DECLINED` o',
        '`REVOKED` explícitamente (no existe "todas").',
        '',
        '⚠️ Una invitación puede estar `status=PENDING` **y** `expired=true` a la vez: en ese caso la UI',
        'debe ofrecer "reenviar", no "esperando respuesta".',
      ].join('\n'),
      query: [{ key: 'status', value: 'PENDING', description: 'PENDING (default) | ACCEPTED | DECLINED | REVOKED' }],
      tests: expectStatus(200, [{ variable: 'invitationId', path: '(body.invitations && body.invitations[0] && body.invitations[0].id)' }]),
      examples: [
        {
          name: '200 · Una pendiente y una vencida',
          status: 200,
          body: {
            invitations: [
              {
                id: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
                veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
                email: 'laura.herrera@gmail.com',
                role: 'VETERINARIAN',
                status: 'PENDING',
                expired: false,
                invitedBy: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7',
                expiresAt: '2026-11-06T12:30:00',
                decidedAt: null,
                createdAt: '2026-10-30T12:30:00',
              },
              {
                id: 'a9b8c7d6-e5f4-4321-8098-7f6e5d4c3b2a',
                veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
                email: 'carlos.rojas@hotmail.com',
                role: 'RECEPTIONIST',
                status: 'PENDING',
                expired: true,
                invitedBy: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7',
                expiresAt: '2026-10-20T08:00:00',
                decidedAt: null,
                createdAt: '2026-10-13T08:00:00',
              },
            ],
          },
        },
      ],
    }),
    request({
      name: 'POST Reenviar invitación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/invitations/{{invitationId}}/resend',
      description: [
        'Genera un **token nuevo** y extiende el vencimiento otros 7 días.',
        '',
        '⚠️ El enlace anterior **deja de funcionar inmediatamente** (se sobrescribe el hash guardado).',
        'Si el invitado tiene dos correos, solo sirve el último.',
        '',
        'Funciona también sobre una invitación **vencida** — es justamente la salida para ese caso.',
        '',
        '- `403 ONLY_OWNER_CAN_MANAGE_ADMINS` — un ADMIN reenviando una invitación de ADMIN.',
        '- `404 INVITATION_NOT_FOUND` — no existe o es de otra clínica.',
        '- `409 INVITATION_NOT_PENDING` — ya fue aceptada, rechazada o revocada.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Reenviada',
          status: 200,
          body: {
            invitation: {
              id: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
              email: 'laura.herrera@gmail.com',
              role: 'VETERINARIAN',
              status: 'PENDING',
              expired: false,
              expiresAt: '2026-11-07T09:00:00',
              createdAt: '2026-10-30T12:30:00',
            },
          },
        },
      ],
    }),
    request({
      name: 'DELETE Revocar invitación',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/invitations/{{invitationId}}',
      description: [
        'Cancela una invitación pendiente (vigente o vencida). El enlace deja de servir.',
        '',
        'Cualquiera con `STAFF_MANAGE` puede revocar, **incluso** una invitación de `ADMIN`: revocar',
        'nunca escala privilegios.',
        '',
        '- `404 INVITATION_NOT_FOUND` · `409 INVITATION_NOT_PENDING`.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Revocada',
          status: 200,
          body: {
            invitation: {
              id: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
              email: 'laura.herrera@gmail.com',
              role: 'VETERINARIAN',
              status: 'REVOKED',
              expired: false,
              decidedAt: '2026-10-30T13:00:00',
            },
          },
        },
      ],
    }),
  ],
});

// ---------- 09 · Invitaciones (invitado) ----------

const f09 = folder({
  name: '09 · Personal · Invitaciones (lado del invitado)',
  auth: bearer('access_token'),
  description: [
    'Token: **normal del invitado** (todavía no es miembro de la clínica, así que no puede tener un',
    'token de clínica).',
    '',
    'El token de invitación va **en el cuerpo**, nunca en la URL: el gateway y Cloud Run registran la',
    'URL completa de cada petición en logs que no controlamos. Por eso `preview` es un `POST` aunque',
    'no modifique nada.',
    '',
    'Orden de validación (identidad antes de estado, para no filtrarle el estado a quien no es el',
    'invitado): correo del JWT == invitado → correo verificado → `PENDING` → no vencida.',
    '',
    'Poné el token en la variable `invitation_token` del environment.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Vista previa de la invitación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/invitations/preview',
      description: [
        'Pantalla que ve el invitado al abrir el enlace, antes de decidir.',
        '',
        'Responde `200` **incluso si la invitación ya no sirve** (vencida, revocada, usada, o abierta',
        'con otra cuenta): `status`, `expired`, `usable` y `emailMatchesCaller` son estados que el',
        'frontend pinta, no errores. El único error es un token inexistente → `404 INVITATION_NOT_FOUND`.',
        '',
        '- `usable: true` solo si se puede aceptar **ahora mismo con esta sesión**. Usalo para',
        '  habilitar el botón.',
        '- `emailMatchesCaller: false` → "iniciaste sesión con otra cuenta".',
        '- `veterinaryName`, `logoUrl` e `invitedByName` pueden venir `null` si no se pudieron resolver.',
      ].join('\n'),
      body: jsonBody({ token: '{{invitation_token}}' }),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    if (body.veterinaryId) pm.environment.set("veterinaryId", body.veterinaryId);',
        '    pm.test("la invitacion es usable con esta sesion", function () {',
        '        pm.expect(body.usable, "usable").to.be.true;',
        '    });',
        '}',
      ]),
      examples: [
        {
          name: '200 · Lista para aceptar',
          status: 200,
          body: {
            invitationId: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            veterinaryName: 'Clínica Veterinaria El Bosque',
            veterinaryCity: 'Bogotá',
            logoUrl: 'https://xyz.supabase.co/storage/v1/object/public/veterinary-logos/veterinaries/9a1c.../logo.png',
            role: 'VETERINARIAN',
            invitedByName: 'María Fernanda Gómez',
            email: 'laura.herrera@gmail.com',
            emailMatchesCaller: true,
            status: 'PENDING',
            expired: false,
            usable: true,
            expiresAt: '2026-11-06T12:30:00',
          },
        },
        {
          name: '200 · Vencida (no es error)',
          status: 200,
          body: {
            invitationId: 'a9b8c7d6-e5f4-4321-8098-7f6e5d4c3b2a',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            veterinaryName: 'Clínica Veterinaria El Bosque',
            role: 'RECEPTIONIST',
            email: 'carlos.rojas@hotmail.com',
            emailMatchesCaller: true,
            status: 'PENDING',
            expired: true,
            usable: false,
            expiresAt: '2026-10-20T08:00:00',
          },
        },
      ],
    }),
    request({
      name: 'POST Aceptar invitación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/invitations/accept',
      description: [
        'Crea el empleado con el rol de la invitación, **o reactiva** el registro inactivo que ya',
        'existía si esa persona ya había trabajado en la clínica (conserva su `id` y su historial:',
        '`reactivated: true`).',
        '',
        '`licensed` es **informativo** (dice si tiene un perfil profesional `VERIFIED`): sirve para',
        'mostrarle "completá tu perfil profesional". El valor que manda en el token lo resuelve el',
        'User service al emitirlo.',
        '',
        '**Después de aceptar hay que pedir un token de clínica nuevo** (carpeta 00).',
        '',
        '- `403 INVITATION_EMAIL_MISMATCH` — el correo del token no es el invitado.',
        '- `403 EMAIL_NOT_VERIFIED` — tu correo no está verificado.',
        '- `404 INVITATION_NOT_FOUND` — token inexistente (o ausente/vacío).',
        '- `409 INVITATION_NOT_PENDING` — ya fue aceptada, rechazada o revocada.',
        '- `409 EMPLOYEE_ALREADY_EXISTS` — ya sos empleado activo de esa clínica.',
        '- `409 CLINIC_NOT_ACTIVE` — la clínica no está activa.',
        '- `410 INVITATION_EXPIRED` — venció; quien invitó puede reenviarla.',
      ].join('\n'),
      body: jsonBody({ token: '{{invitation_token}}' }),
      tests: expectStatus(200, [
        { variable: 'employeeId', path: 'body.employeeId' },
        { variable: 'veterinaryId', path: 'body.veterinaryId' },
      ]),
      examples: [
        {
          name: '200 · Aceptada (empleado nuevo)',
          status: 200,
          body: {
            employeeId: '33cc44dd-55ee-4ff6-8007-112233445566',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            veterinaryName: 'Clínica Veterinaria El Bosque',
            role: 'VETERINARIAN',
            licensed: false,
            reactivated: false,
          },
        },
        {
          name: '410 · Invitación vencida',
          status: 410,
          code: 'Gone',
          body: {
            timestamp: '2026-10-30T13:10:00.001',
            status: 410,
            error: 'Gone',
            message: 'Invitation has expired',
            code: 'INVITATION_EXPIRED',
          },
        },
      ],
    }),
    request({
      name: 'POST Rechazar invitación',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/invitations/decline',
      description: [
        'Cierra la invitación sin crear ningún empleado. Mismas validaciones de identidad y estado que',
        'aceptar.',
        '',
        'A diferencia de aceptar, **no** exige que la clínica esté `ACTIVE` (sacarse de encima una',
        'invitación no debería depender del estado de la clínica) y **no publica ningún evento**.',
      ].join('\n'),
      body: jsonBody({ token: '{{invitation_token}}' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Rechazada',
          status: 200,
          body: {
            invitationId: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            status: 'DECLINED',
            decidedAt: '2026-10-30T13:15:00',
          },
        },
      ],
    }),
    request({
      name: 'GET Mis invitaciones pendientes',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/me/invitations',
      description: [
        'Token: **normal**. Solo invitaciones `PENDING` **vigentes** dirigidas al correo del token',
        '(el correo sale del token, nunca de un parámetro).',
        '',
        '⚠️ **No devuelve el token** de cada invitación, a propósito: desde acá se ve que existen, pero',
        'para aceptar hace falta el enlace que llegó al correo. Es un badge de "tenés 2 invitaciones",',
        'no una pantalla de aceptación.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Una invitación',
          status: 200,
          body: {
            invitations: [
              {
                invitationId: 'f0b0c1d2-e3f4-4a5b-8c6d-7e8f90a1b2c3',
                veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
                veterinaryName: 'Clínica Veterinaria El Bosque',
                veterinaryCity: 'Bogotá',
                logoUrl: null,
                role: 'VETERINARIAN',
                expiresAt: '2026-11-06T12:30:00',
              },
            ],
          },
        },
      ],
    }),
  ],
});

// ---------- 10 · Equipo ----------

const f10 = folder({
  name: '10 · Personal · Equipo',
  auth: bearer('clinic_token'),
  description: [
    'Token: **de clínica**.',
    '',
    NOTE_DB_ROLE,
    '',
    '**Reglas de jerarquía** (cambiar rol y retirar): nadie se modifica a sí mismo, al `OWNER` no lo',
    'toca nadie, un `ADMIN` no puede tocar a otro `ADMIN`, y solo el `OWNER` puede promover a `ADMIN`.',
    '',
    'Retirar **no borra**: marca `active=false` + `removedAt`, para no perder la trazabilidad de las',
    'historias clínicas que el empleado firmó.',
    '',
    ERR_CLINIC_TOKEN,
  ].join('\n'),
  items: [
    request({
      name: 'GET Ver el equipo',
      method: 'GET',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/employees?includeInactive=false',
      description: [
        'Permiso: **cualquier miembro activo** (`requireActiveMember`), sin permiso específico — un',
        'veterinario tiene que poder ver con quién trabaja.',
        '',
        '`name`/`email` se resuelven contra el User service y pueden venir `null`.',
        '`self: true` marca tu propia fila (no le ofrezcas "retirar" ahí; para eso está "salir").',
        '',
        'Nunca expone documento de identidad ni datos clínicos. El estado de licencia de un',
        'veterinario se consulta en la carpeta 04.',
        '',
        '`includeInactive=true` agrega a los retirados, con su `removedAt`.',
      ].join('\n'),
      query: [{ key: 'includeInactive', value: 'false', description: 'true incluye a los empleados retirados' }],
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const others = (body.employees || []).filter(function (e) { return !e.self && e.role !== "OWNER"; });',
        '    if (others.length > 0) {',
        '        pm.environment.set("employeeId", others[0].employeeId);',
        '        console.log("employeeId =", others[0].employeeId);',
        '    }',
        '}',
      ]),
      examples: [
        {
          name: '200 · Equipo de 3',
          status: 200,
          body: {
            employees: [
              { employeeId: '11aa22bb-33cc-4dd5-8ee6-ff7788990011', userId: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7', name: 'María Fernanda Gómez', email: 'owner@elbosquevet.co', role: 'OWNER', active: true, createdAt: '2026-10-30T09:00:00', removedAt: null, self: true },
              { employeeId: '22bb33cc-44dd-4ee6-8ff7-889900112233', userId: '8d34b5c6-7d8e-4f90-a1b2-c3d4e5f6a7b8', name: 'Juan David Martínez Soto', email: 'juan.martinez@elbosquevet.co', role: 'VETERINARIAN', active: true, createdAt: '2026-10-30T12:40:00', removedAt: null, self: false },
              { employeeId: '33cc44dd-55ee-4ff6-8007-112233445566', userId: '9e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', name: null, email: null, role: 'RECEPTIONIST', active: true, createdAt: '2026-10-30T12:45:00', removedAt: null, self: false },
            ],
          },
        },
      ],
    }),
    request({
      name: 'PATCH Cambiar el rol de un empleado',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/employees/{{employeeId}}/role',
      description: [
        'Permiso: `STAFF_MANAGE`; + ser **OWNER** si el rol destino es `ADMIN`.',
        '',
        'Publica `EMPLOYEE_ROLE_UPDATED` **al empleado**, con `oldRole`/`newRole`.',
        '',
        '- `400 INVALID_EMPLOYEE_ROLE` — rol ausente o `OWNER` (para OWNER está la transferencia).',
        '- `403 CANNOT_MODIFY_SELF` — nadie se cambia el rol a sí mismo.',
        '- `403 CANNOT_MODIFY_OWNER` — al OWNER no lo toca nadie.',
        '- `403 CANNOT_MODIFY_PEER_ADMIN` — un ADMIN no puede tocar a otro ADMIN.',
        '- `403 ONLY_OWNER_CAN_MANAGE_ADMINS` — un ADMIN intentó promover a ADMIN.',
        '- `404 EMPLOYEE_NOT_FOUND` — no existe o es de otra clínica.',
      ].join('\n'),
      body: jsonBody({ role: 'ADMIN' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Rol cambiado',
          status: 200,
          body: {
            id: '22bb33cc-44dd-4ee6-8ff7-889900112233',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            userId: '8d34b5c6-7d8e-4f90-a1b2-c3d4e5f6a7b8',
            role: 'ADMIN',
          },
        },
        {
          name: '403 · No podés tocar al OWNER',
          status: 403,
          code: 'Forbidden',
          body: {
            timestamp: '2026-10-30T13:30:00.001',
            status: 403,
            error: 'Forbidden',
            message: 'The OWNER cannot be modified',
            code: 'CANNOT_MODIFY_OWNER',
          },
        },
      ],
    }),
    request({
      name: 'DELETE Retirar empleado',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/employees/{{employeeId}}',
      description: [
        'Permiso: `STAFF_MANAGE`. **No borra la fila**: marca `active=false` + `removedAt`.',
        '',
        'Efecto inmediato: en su siguiente petición, ese empleado recibe `403 NOT_CLINIC_MEMBER`',
        'aunque su token de clínica siga vigente.',
        '',
        'Publica `EMPLOYEE_DEACTIVATED` **al empleado**, con `leftVoluntarily: false`.',
        '',
        'Mismas guardas que cambiar el rol (`CANNOT_MODIFY_SELF`, `CANNOT_MODIFY_OWNER`,',
        '`CANNOT_MODIFY_PEER_ADMIN`, `404 EMPLOYEE_NOT_FOUND`).',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Retirado',
          status: 200,
          body: {
            id: '33cc44dd-55ee-4ff6-8007-112233445566',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            userId: '9e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9',
            role: 'RECEPTIONIST',
            active: false,
          },
        },
      ],
    }),
    request({
      name: 'POST Salir de la clínica',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/leave',
      description: [
        'Token: **de clínica**. Permiso: cualquier miembro activo (`requireMember`).',
        '',
        'Funciona también si la clínica está **suspendida**: irse no debería depender del estado de la',
        'clínica. El `employeeId` sale del contexto, nunca de la petición.',
        '',
        'Publica `EMPLOYEE_DEACTIVATED` con `leftVoluntarily: true` **al OWNER** (no al que se va).',
        '',
        '- `409 OWNER_MUST_TRANSFER_OWNERSHIP` — el OWNER tiene que transferir la propiedad primero.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Salida registrada',
          status: 200,
          body: {
            employeeId: '22bb33cc-44dd-4ee6-8ff7-889900112233',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            removedAt: '2026-10-30T13:45:00',
          },
        },
        {
          name: '409 · El OWNER debe transferir primero',
          status: 409,
          code: 'Conflict',
          body: {
            timestamp: '2026-10-30T13:44:00.001',
            status: 409,
            error: 'Conflict',
            message: 'The OWNER must transfer ownership before leaving the veterinary',
            code: 'OWNER_MUST_TRANSFER_OWNERSHIP',
          },
        },
      ],
    }),
    request({
      name: 'POST Transferir propiedad',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/{{veterinaryId}}/ownership-transfer',
      description: [
        'Token: **normal o de clínica**, pero **solo el OWNER** (verificado contra la base, funciona en',
        'cualquier estado de la clínica).',
        '',
        'El destino debe ser un empleado **activo de la misma clínica**. En una sola transacción: el',
        'OWNER actual pasa a `ADMIN` y el destino pasa a `OWNER`. La clínica nunca queda sin OWNER ni',
        'con dos.',
        '',
        '⚠️ **El representante legal NO cambia** (`legalRepresentativeChanged: false`). Son cosas',
        'distintas: quién manda en la cuenta vs. quién firma legalmente por la clínica. Si también debe',
        'cambiar, actualizalo con `PUT /{id}/legal-representative` (carpeta 03).',
        '',
        '⚠️ Después de transferir, **las dos partes necesitan un token de clínica nuevo**.',
        '',
        '- `400 VALIDATION_ERROR` — falta `newOwnerEmployeeId`, ya es el OWNER, o es un empleado retirado.',
        '- `403 NOT_VETERINARY_OWNER` — no sos el dueño.',
        '- `404 EMPLOYEE_NOT_FOUND` — el destino no existe o es de otra clínica.',
        '- `409 OWNERSHIP_TRANSFER_CONFLICT` — otra transferencia ocurrió en paralelo; reintentar.',
      ].join('\n'),
      body: jsonBody({ newOwnerEmployeeId: '{{employeeId}}' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Propiedad transferida',
          status: 200,
          body: {
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            previousOwner: { employeeId: '11aa22bb-33cc-4dd5-8ee6-ff7788990011', userId: '7c21a3b4-5c6d-4e7f-8091-a2b3c4d5e6f7', role: 'ADMIN' },
            newOwner: { employeeId: '22bb33cc-44dd-4ee6-8ff7-889900112233', userId: '8d34b5c6-7d8e-4f90-a1b2-c3d4e5f6a7b8', role: 'OWNER' },
            legalRepresentativeChanged: false,
          },
        },
        {
          name: '409 · Carrera perdida',
          status: 409,
          code: 'Conflict',
          body: {
            timestamp: '2026-10-30T13:50:00.001',
            status: 409,
            error: 'Conflict',
            message: 'Ownership changed while the transfer was in progress; retry',
            code: 'OWNERSHIP_TRANSFER_CONFLICT',
          },
        },
      ],
    }),
  ],
});

// ---------- 11 · Pacientes / clientes ----------

const f11 = folder({
  name: '11 · Pacientes / clientes',
  auth: bearer('access_token'),
  description: [
    'Vinculación de **dueños de mascotas** (clientes) a una clínica. Token: **normal del cliente** —',
    'el `userId` sale del token, nunca del body.',
    '',
    'Es lo único que existe hoy de este módulo; el resto (mascotas compartidas, pacientes) llega en',
    'la Fase 4.',
    '',
    'El código/enlace lo genera la clínica en la carpeta 02.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Vincularme con un código',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/link/code',
      description: [
        'Token: **normal**. El cliente se vincula con el código `VET-XXXXXXXX`.',
        '',
        '- `400 INVALID_INVITE_CODE` — el código no existe.',
        '- `409 VETERINARY_NOT_ACTIVE` — la clínica todavía no está aprobada.',
        '- `409 USER_ALREADY_LINKED` — ya estás vinculado a esa clínica.',
      ].join('\n'),
      body: jsonBody({ code: '{{inviteCode}}' }),
      tests: expectStatus(201),
      examples: [
        {
          name: '201 · Vinculado',
          status: 201,
          code: 'Created',
          body: {
            id: '44dd55ee-66ff-4007-8118-223344556677',
            userId: 'aa11bb22-cc33-4dd4-8ee5-ff6677889900',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            status: 'LINKED',
            linkedAt: '2026-10-30T14:00:00',
          },
        },
      ],
    }),
    request({
      name: 'POST Vincularme con un enlace',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/link/url',
      description: 'Igual que el anterior, pero con el `inviteLink` completo en vez del código. Mismos errores.',
      body: jsonBody({ url: '{{inviteLink}}' }),
      tests: expectStatus(201),
      examples: [
        {
          name: '201 · Vinculado',
          status: 201,
          code: 'Created',
          body: {
            id: '55ee66ff-7700-4118-8229-334455667788',
            userId: 'aa11bb22-cc33-4dd4-8ee5-ff6677889900',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            status: 'LINKED',
            linkedAt: '2026-10-30T14:05:00',
          },
        },
      ],
    }),
    request({
      name: 'DELETE Desvincular cliente',
      method: 'DELETE',
      raw: '{{vet_url}}/api/veterinary/link/{{veterinaryId}}/user/{{clientUserId}}',
      description: [
        'Dos formas de llamarlo:',
        '',
        '1. **El propio cliente** se desvincula: token normal, y el `userId` de la ruta es el suyo.',
        '2. **La clínica** lo desvincula: **token de clínica** con `CLINIC_CONFIGURE` (no `STAFF_MANAGE`:',
        '   el vinculado es un cliente, no un empleado).',
        '',
        '- `403 INSUFFICIENT_PERMISSION` — ni sos ese usuario ni tenés `CLINIC_CONFIGURE` en esa clínica.',
        '- `404 USER_NOT_LINKED` — no había vínculo.',
      ].join('\n'),
      tests: expectStatus(204),
      examples: [{ name: '204 · Desvinculado', status: 204, code: 'No Content', body: {} }],
    }),
  ],
});

// ---------- 12 · Internos ----------

const f12 = folder({
  name: '12 · Internos (servicio a servicio)',
  auth: noAuth,
  description: [
    '🚫 **Nunca expuestos por el gateway.** Son rutas de servicio a servicio: se autentican con el',
    'header `X-Internal-Api-Key` y **no** con un JWT de usuario. Si el gateway las publicara,',
    'cualquiera podría hablarle directo al núcleo del servicio.',
    '',
    'Sin bearer: la carpeta tiene la autenticación en `noauth` y el header va en cada request.',
    '',
    'La llave es la misma `INTERNAL_API_KEY` en todos los servicios; se compara en tiempo constante',
    '(`MessageDigest.isEqual`). Si no coincide: `401 UNAUTHENTICATED`.',
  ].join('\n'),
  items: [
    request({
      name: 'GET Membresía de un usuario en una clínica',
      method: 'GET',
      raw: '{{vet_url}}/internal/veterinaries/{{veterinaryId}}/members/{{memberUserId}}',
      description: [
        'Lo consume el **User service** para decidir si puede emitir un token de clínica, y con qué',
        'rol/estado.',
        '',
        'Si la clínica no existe **o** el usuario no es empleado, responde `200` con `member: false`',
        '(nunca `404`) y el resto en `null`.',
        '',
        '`licensed` es `true` solo si el usuario tiene un `ProfessionalProfile` con',
        '`licenseStatus == VERIFIED`.',
      ].join('\n'),
      headers: [{ key: 'X-Internal-Api-Key', value: '{{internal_api_key}}' }],
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Es miembro',
          status: 200,
          body: {
            member: true,
            employeeId: '22bb33cc-44dd-4ee6-8ff7-889900112233',
            role: 'VETERINARIAN',
            active: true,
            licensed: true,
            veterinaryStatus: 'ACTIVE',
            subscriptionStatus: 'ACTIVE',
          },
        },
        {
          name: '200 · No es miembro (no es 404)',
          status: 200,
          body: {
            member: false,
            employeeId: null,
            role: null,
            active: null,
            licensed: false,
            veterinaryStatus: 'ACTIVE',
            subscriptionStatus: 'NONE',
          },
        },
      ],
    }),
  ],
});

// ---------- 13 · Jobs ----------

const f13 = folder({
  name: '13 · Jobs (servicio a servicio)',
  auth: noAuth,
  description: [
    '🚫 **Nunca expuestos por el gateway**, igual que la carpeta 12. Header `X-Internal-Api-Key`.',
    '',
    'Pensados para un Cloud Scheduler que los invoque con la llave interna.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Limpieza de documentos de verificaciones rechazadas (90 días)',
      method: 'POST',
      raw: '{{vet_url}}/internal/jobs/verification-documents/cleanup',
      description: [
        'Borra documentos (archivo en Storage + fila) de solicitudes **`REJECTED`** con `decided_at` de',
        'hace más de **90 días**.',
        '',
        'Es **idempotente**: correrlo dos veces el mismo día no rompe nada y el segundo pasa sin borrar.',
        '',
        'Sugerencia: Cloud Scheduler diario.',
      ].join('\n'),
      headers: [{ key: 'X-Internal-Api-Key', value: '{{internal_api_key}}' }],
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Nada que borrar',
          status: 200,
          body: { requestsProcessed: 0, documentsDeleted: 0, cutoff: '2026-08-01T00:00:00' },
        },
      ],
    }),
  ],
});

// ---------- 99 · Deprecados ----------

const f99 = folder({
  name: '99 · Deprecados',
  auth: bearer('clinic_token'),
  description: [
    '⚠️ **No usar en código nuevo.** Están acá para poder probarlos mientras el frontend migra.',
    '',
    '- Los tres `PATCH /api/veterinary/employees/...` son **alias** de las rutas anidadas por clínica',
    '  (carpeta 10). Aplican exactamente las mismas reglas; como no llevan la clínica en la ruta, la',
    '  toman del token, así que un `employeeId` de otra clínica responde `404 EMPLOYEE_NOT_FOUND`.',
    '- **`POST /api/veterinary/employees` se elimina antes de la clínica piloto.** Agrega a una',
    '  persona al equipo por `userId` **sin su consentimiento**. Lo reemplazan las invitaciones',
    '  (carpetas 08 y 09).',
  ].join('\n'),
  items: [
    request({
      name: '[DEPRECATED] POST Crear empleado directo (sin consentimiento)',
      method: 'POST',
      raw: '{{vet_url}}/api/veterinary/employees',
      description: [
        '🔴 **Se elimina antes de la clínica piloto.** Migrá a invitaciones (carpeta 08 + 09).',
        '',
        'Permiso: `STAFF_MANAGE`. Agrega al usuario indicado como empleado **sin que acepte nada**.',
        '',
        'No permite asignar `OWNER` (`400 INVALID_EMPLOYEE_ROLE`).',
        '- `409 EMPLOYEE_ALREADY_EXISTS` · `409 VETERINARY_NOT_ACTIVE`.',
      ].join('\n'),
      body: jsonBody({
        veterinaryId: '{{veterinaryId}}',
        userId: '{{memberUserId}}',
        role: 'ASSISTANT',
      }),
      tests: expectStatus(201),
      examples: [
        {
          name: '201 · Empleado creado',
          status: 201,
          code: 'Created',
          body: {
            id: '66ff7700-8811-4229-833a-445566778899',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            userId: '9e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9',
            role: 'ASSISTANT',
            active: true,
            createdAt: '2026-10-30T14:20:00',
          },
        },
      ],
    }),
    request({
      name: '[DEPRECATED] PATCH Cambiar rol (ruta vieja)',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/employees/{{employeeId}}/role',
      description: 'Alias de `PATCH /api/veterinary/{veterinaryId}/employees/{employeeId}/role` (carpeta 10). La clínica sale del token.',
      body: jsonBody({ role: 'ASSISTANT' }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Rol cambiado',
          status: 200,
          body: {
            id: '22bb33cc-44dd-4ee6-8ff7-889900112233',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            userId: '8d34b5c6-7d8e-4f90-a1b2-c3d4e5f6a7b8',
            role: 'ASSISTANT',
          },
        },
      ],
    }),
    request({
      name: '[DEPRECATED] PATCH Retirar empleado (ruta vieja)',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/employees/{{employeeId}}/deactivate',
      description: 'Alias de `DELETE /api/veterinary/{veterinaryId}/employees/{employeeId}` (carpeta 10). Desactiva + `removedAt`, no borra.',
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Retirado',
          status: 200,
          body: {
            id: '33cc44dd-55ee-4ff6-8007-112233445566',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            userId: '9e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9',
            role: 'RECEPTIONIST',
            active: false,
          },
        },
      ],
    }),
    request({
      name: '[DEPRECATED] PATCH Reactivar empleado (ruta vieja)',
      method: 'PATCH',
      raw: '{{vet_url}}/api/veterinary/employees/{{employeeId}}/activate',
      description: [
        'Permiso: `STAFF_MANAGE`. Vuelve a activar un empleado retirado y limpia `removedAt`.',
        '',
        'No tiene equivalente anidado todavía; es la única de las tres rutas viejas que sigue siendo la',
        'única forma de hacer esto. Sin restricciones de auto-activación ni de rol del target.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Reactivado',
          status: 200,
          body: {
            id: '33cc44dd-55ee-4ff6-8007-112233445566',
            veterinaryId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            userId: '9e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9',
            role: 'RECEPTIONIST',
            active: true,
          },
        },
      ],
    }),
  ],
});

// ---------- Colección ----------

const collection = {
  info: {
    _postman_id: 'b7e1c3a4-5d6f-4a8b-9c0d-1e2f3a4b5c6d',
    name: 'MyAnimaLog · Veterinary Service',
    description: [
      '# MyAnimaLog · Veterinary Service',
      '',
      'Colección completa del `veterinary-service` (**61 endpoints**, todos los del código, incluidos',
      'los `/internal/**` y los deprecados). Generada desde los controladores con',
      '`postman/build-collection.js` y verificada con `postman/check-coverage.js`.',
      '',
      '## Cómo empezar',
      '',
      '1. Importá la colección y **uno** de los dos environments (`Local` o `CloudRun`).',
      '2. Llená en el environment: `owner_email`, `owner_password` y (para las carpetas 05/06/07)',
      '   `admin_email`/`admin_password`. En `CloudRun`, además las URLs.',
      '3. **00 · Autenticación** → `POST Login` guarda `access_token`.',
      '4. **01 · Registro de clínica** (o **02 · GET Mis clínicas** si ya tenés una) para tener',
      '   `veterinaryId`.',
      '5. **00 · POST Token de clínica** guarda `clinic_token`. Desde acá funcionan 02, 03, 08, 10.',
      '6. El resto en orden: 03 verificación → 05 el admin aprueba → 08 invitás → 09 el invitado',
      '   acepta → 10 equipo.',
      '',
      '## Qué token usa cada carpeta',
      '',
      '| Carpeta | Token |',
      '|---|---|',
      '| 00 Autenticación | ninguno (son del User service) |',
      '| 01 Registro | `access_token` (normal) |',
      '| 02 Mis clínicas y perfil | `clinic_token`, salvo `GET /me` que usa `access_token` |',
      '| 03 Verificación | `access_token` del OWNER (verificado contra la base) |',
      '| 04 Perfil profesional | `access_token`, salvo el perfil de un colega que usa `clinic_token` |',
      '| 05 · 06 · 07 Admin | `admin_token` (usuario `PLATFORM_ADMIN`) |',
      '| 08 Invitaciones (clínica) | `clinic_token` |',
      '| 09 Invitaciones (invitado) | `access_token` del invitado |',
      '| 10 Equipo | `clinic_token` |',
      '| 11 Pacientes | `access_token` del cliente |',
      '| 12 · 13 Internos y jobs | ninguno; header `X-Internal-Api-Key` |',
      '| 99 Deprecados | `clinic_token` |',
      '',
      'La autenticación por defecto de la colección es `Bearer {{access_token}}`; cada carpeta la',
      'sobrescribe donde hace falta.',
      '',
      '## El rol se lee de la base, no del token',
      '',
      'Desde la Fase 3, el rol y el estado de la clínica se resuelven **contra la base en cada',
      'petición**. Si te cambian el rol o te retiran, tu token de clínica deja de servir para lo que ya',
      'no te corresponde **en la petición siguiente**, sin esperar los ~60 min que dura. En Postman eso',
      'se ve como un `403 NOT_CLINIC_MEMBER` o `403 INSUFFICIENT_PERMISSION` con un token que funcionaba',
      'hace un momento: hay que volver a **00 · POST Token de clínica**.',
      '',
      '## Probar invitaciones en local sin correos',
      '',
      'Con `EVENTS_ENABLED=false` (lo normal en local y en el piloto) **no sale ningún evento, así que',
      'no se envía ningún correo** — incluido el de invitación. El token en claro solo existe dentro de',
      'ese evento, así que no hay forma de leerlo desde la API.',
      '',
      'Para probar las carpetas 09 en local, fijá un token conocido con SQL después de crear la',
      'invitación en 08:',
      '',
      '```sql',
      "UPDATE public.employee_invitations",
      "SET token_hash = encode(sha256(convert_to('token-prueba-1','UTF8')),'hex')",
      "WHERE id = '<invitationId>';",
      '```',
      '',
      'Y poné `invitation_token = token-prueba-1` en el environment.',
      '',
      '**Verificado en el código**: `Sha256.hex()` usa `MessageDigest("SHA-256")` y formatea cada byte',
      'con `%02x`, o sea **hex en minúsculas**, sobre `token.getBytes(UTF_8)` — exactamente lo que',
      "produce `encode(sha256(convert_to(...,'UTF8')),'hex')`. Y `ResendInvitationService` **sí**",
      'reemplaza `token_hash` por el hash de un token nuevo, así que reenviar invalida el token que',
      'fijaste y hay que volver a correr el `UPDATE`.',
      '',
      'Para que la invitación siga siendo usable: `status = PENDING`, `expires_at` en el futuro, y el',
      'correo del JWT igual al `email` de la fila y verificado.',
      '',
      '## Variables de URL',
      '',
      '`{{vet_url}}` es la **base del servicio sin** `/api/veterinary` (ej. `http://localhost:8082`);',
      'cada request lleva la ruta completa. Así la misma variable sirve para `/api/veterinary/**` y',
      'para `/internal/**`. `{{gateway_url}}` está en el environment local por si querés probar a',
      'través del gateway: cambiá `vet_url` por ese valor.',
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  auth: bearer('access_token'),
  item: [auth00, f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11, f12, f13, f99],
  variable: [
    { key: 'legal_data_policy_version', value: '2026-10-v1', type: 'string' },
    { key: 'legal_terms_version', value: '2026-10-v1', type: 'string' },
  ],
};

fs.writeFileSync(
  'postman/MyAnimaLogVet.postman_collection.json',
  JSON.stringify(collection, null, 2) + '\n',
  'utf8'
);

const count = (items) =>
  items.reduce((acc, i) => acc + (i.item ? count(i.item) : 1), 0);

console.log('postman/MyAnimaLogVet.postman_collection.json escrito');
console.log(`Carpetas: ${collection.item.length} · Requests: ${count(collection.item)}`);
