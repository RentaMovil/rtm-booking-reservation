# rtm-booking-reservation

Servicio **Booking & Reservation** de RentaMovil: reservas, ejecución de renta (pickup/devolución)
y notificaciones in-app — un solo ciclo de vida, no tres servicios (ver
`rtm-docs/09-microservices/service-catalog.md` para el porqué).
Documentación del servicio: `rtm-docs/09-microservices/services/04-booking-reservation/`.

| | |
|---|---|
| Stack | Java 21 · Spring Boot 4.1 · Maven · arquitectura hexagonal |
| Puerto local | 3003 |
| Base de datos | SQL Server `rentamovil`, schema `booking`, usuario `booking_app` |
| Migraciones | **no están aquí** — las maneja `rtm-booking-reservation-db` (Liquibase) |

## Estado actual: esqueleto

Compila, tiene sus 4 tablas migradas, 25 tests de dominio en verde y expone `/health` y los
endpoints de lectura. **Crear una reserva todavía no funciona**, y es deliberado: falta la
dependencia de la que salen el precio y la disponibilidad del vehículo. Ver "Lo que falta" abajo.

Lo que sí está completo: el modelo de dominio con sus invariantes, la máquina de estados, el job
de expiración, la verificación del JWT, el endpoint interno de ocupación y todos los endpoints.

## Estructura (hexagonal)

```
src/main/java/com/rentamovil/booking/
├── domain/                   Reglas del negocio. No usa Spring ni JPA
│   ├── entity/                 Reservation, Rental, Notification, InsuranceType, AuthenticatedUser
│   ├── enums/                  ReservationStatus, RentalStatus, NotificationType
│   ├── exception/              BusinessRuleException, InvalidDateRangeException, ...
│   └── port/
│       ├── in/                 casos de uso (ReservationUseCases, RentalUseCases, ...)
│       └── out/                lo que el dominio necesita (*RepositoryPort, VehicleAvailabilityPort, ...)
├── application/usecase/      Implementación de los casos de uso
└── infrastructure/           Adaptadores
    ├── config/                 BeanConfig, WebConfig
    ├── events/                 InProcessEventDispatcher (sin broker, ver events.md)
    ├── fleet/                  PendingVehicleAvailabilityAdapter (placeholder, ver abajo)
    ├── persistence/
    │   ├── entity/             entidades JPA (4 tablas del schema booking + shared.audit)
    │   ├── repository/         Spring Data
    │   └── adapter/            implementan los *RepositoryPort y el AuditPort
    ├── scheduling/             ReservationExpirationJob (INV-015)
    ├── security/               JwtKeyConfig, JwtVerifierAdapter (solo clave pública)
    └── web/
        ├── controller/         ReservationController, RentalController, NotificationController,
        │                       InsuranceController, AdminBookingController,
        │                       InternalReservationController, HealthController
        ├── dto/                requests y responses
        ├── auth/               @CurrentUser y su argument resolver
        ├── exception/          GlobalExceptionHandler
        └── filter/             RequestLogFilter
```

Ver `rtm-docs/05-architecture/hexagonal-architecture.md`.

## El servicio no toca el esquema

`spring.jpa.hibernate.ddl-auto=validate` — Hibernate solo **verifica** al arrancar que las
entidades coincidan con las tablas creadas por `rtm-booking-reservation-db`, y falla si no
coinciden. Nunca crea ni modifica tablas. Un cambio de esquema = un changeset en el repo de
migraciones, primero.

## Lo que falta para poder reservar

**1. El servicio `fleet-maintenance`.** De él salen el precio diario del vehículo y si está
disponible (HU-FLEET-002). `domain-map.md` lo marca como dependencia síncrona por REST.

Mientras tanto, `PendingVehicleAvailabilityAdapter` devuelve `FLEET_SERVICE_UNAVAILABLE` (409) en
vez de inventar un precio. No se puede crear una reserva sin un precio real, y es preferible fallar
de forma explícita a devolver un importe inventado.

**2. Las FKs a fleet y telemetry.** `reservation.vehicle_id`, `pickup_branch_id`,
`return_branch_id` y `rental.gps_id` son `NOT NULL` pero no tienen FK todavía; se añaden en
`rtm-booking-reservation-db` cuando esos servicios existan.

**3. El claim `personId` en el token.** `iam` hoy emite `sub` (userId), `email`, `role` y
`permissions`. Pero `reservation.client_id` apunta a `iam.person`, no a `iam.[user]`, así que este
servicio necesita el `person_id` del titular.

`JwtVerifierAdapter` ya lee ese claim y, si no está, responde 401 con un mensaje claro. **Hay que
añadir `personId` al token en `rtm-iam`** (`JwtTokenProviderAdapter.generateAccessToken`): sin eso,
ningún endpoint autenticado puede funcionar.

**4. El evento `vehicle.status` lo cambia fleet, no este servicio.** Este servicio emite
`RentalStarted`/`RentalCompleted` y fleet reacciona. Está implementado: no escribe nunca
`vehicle.status`.

## Requisitos

- Java 21+
- Maven 3.9+
- SQL Server local levantado y migrado desde `rtm-iam-db` y `rtm-booking-reservation-db`

## Uso local

```bash
# 1. Configuración (una sola vez)
cp .env.example .env
# DATABASE_PASSWORD = el parameter.bookingAppPassword de rtm-booking-reservation-db/liquibase.properties
# APP_BOOKING_RESERVATION_JWT_PUBLIC_KEY = la MISMA clave pública que APP_IAM_JWT_PUBLIC_KEY en
#   rtm-iam/.env (también está en GET /jwks)

# 2. Tests
mvn test

# 3. Levantar el servicio
mvn spring-boot:run

# 4. Verificar
curl http://localhost:3003/health
# {"status":"ok","db":"connected","timestamp":"..."}
```

`/health` responde `503` con `"db":"disconnected"` si la base deja de responder.

### Con Docker

Primero los repos de base de datos, en orden (`rtm-iam-db` → `rtm-booking-reservation-db`), porque
este repo no levanta SQL Server. Después:

```bash
docker compose up -d --build
```

- Contenedor `rentamovil-booking-reservation`, puerto 3003, alias `booking-reservation` en la red.
- Usa el mismo `.env`; solo cambia `DATABASE_URL` para apuntar al contenedor `sqlserver` en vez de
  `localhost` (está en `docker-compose.yml`).
- No correr a la vez `mvn spring-boot:run` y el contenedor: los dos usan el puerto 3003.

## Endpoints

| Método | Ruta | Auth | Historia | Respuestas |
|---|---|---|---|---|
| GET | `/health` | — | — | 200 / 503 |
| GET | `/insurance-plans` | — | HU-BOOKING-002 | 200 · 404 |
| POST | `/reservations` | Bearer | HU-BOOKING-003, 007 | 201 · 400 `INVALID_DATE_RANGE` / `TERMS_NOT_ACCEPTED` · 401 · 409 `FLEET_SERVICE_UNAVAILABLE` |
| GET | `/reservations/mine` | Bearer | HU-BOOKING-004 | 200 · 401 |
| GET | `/reservations/{id}` | Bearer | HU-BOOKING-004 | 200 · 401 · 403 |
| PATCH | `/reservations/{id}` | Bearer | HU-BOOKING-005 | 200 · 400 · 401 · 404 · 409 `RETURN_BRANCH_LOCKED` |
| POST | `/reservations/{id}/cancel` | Bearer | HU-BOOKING-006 | 200 · 401 · 404 · 409 `CANCELLATION_WINDOW_CLOSED` |
| POST | `/rentals/{reservationId}/pickup` | ADMIN | HU-RENTAL-001 | 201 · 401 · 403 · 404 · 409 `RESERVATION_NOT_CONFIRMED` / `RENTAL_ALREADY_STARTED` |
| POST | `/rentals/{id}/return` | ADMIN | HU-RENTAL-002 | 200 · 401 · 403 · 404 · 409 `INVALID_MILEAGE` |
| GET | `/notifications` | Bearer | HU-NOTIF-001 | 200 · 401 |
| PATCH | `/notifications/{id}/read` | Bearer | HU-NOTIF-001 | 200 · 401 · 404 |
| GET | `/admin/reservations?status=` | ADMIN | HU-BOOKING-008 | 200 · 401 · 403 |
| GET | `/admin/rentals` | ADMIN | — | 200 · 401 · 403 |
| POST | `/admin/insurance-plans` | ADMIN | HU-BOOKING-009 | 200 · 400 · 401 · 403 |
| GET | `/internal/reservations/vehicle-occupancy` | **sin JWT** | interno de fleet | 200 · 400 |

`/internal/*` no lleva JWT a propósito: lo consume `fleet-maintenance`, no una persona. El
api-gateway **no debe enrutar** `/internal/*` fuera de la red interna.

El `clientId` nunca se expone en las respuestas ni se acepta en el cuerpo: sale del token, así nadie
puede crear una reserva a nombre de otra persona.

```bash
curl -X POST http://localhost:3003/reservations \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"vehicleId":301,"insuranceTypeId":1,"pickupBranchId":10,"returnBranchId":11,
       "startDate":"2026-11-01T10:00:00","endDate":"2026-11-05T10:00:00","termsAccepted":true}'
```

## Ramas

`feat/`, `fix/`, `chore/` → PR a `develop` (Squash and Merge). Ver
`rtm-docs/00-governance/git-conventions.md`.