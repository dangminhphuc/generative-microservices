# api-gateway

The `api-gateway` is the single entry point for all external traffic in the fintech platform. It handles request routing to downstream microservices, JWT authentication, and CORS policy enforcement. No client ever talks directly to a backend service — every request goes through this gateway.

## Table of Contents

- [Service Description](#service-description)
- [Tech Stack](#tech-stack)
- [Configuration Profiles](#configuration-profiles)
- [Environment Variables](#environment-variables)
- [JWT Authentication Flow](#jwt-authentication-flow)
- [Public vs Protected Paths](#public-vs-protected-paths)
- [CORS Configuration](#cors-configuration)
- [Actuator Endpoints](#actuator-endpoints)
- [Running Locally and with Docker](#running-locally-and-with-docker)

---

## Service Description

The api-gateway sits in front of all backend services and is responsible for:

- **Routing** — forwards requests to `account-service`, `transfer-service`, and `transaction-history-service` via Eureka service discovery (load-balanced with `lb://`)
- **JWT validation** — inspects the `Authorization: Bearer <token>` header on every protected request and rejects invalid or missing tokens with `401 Unauthorized`
- **Header propagation** — after a successful JWT validation, injects `X-User-Id` and `X-User-Email` headers into the forwarded request so downstream services don't need to re-parse the token
- **Internal endpoint blocking** — a high-priority route (`order: -100`) blocks all paths matching `/*/internal/**` and `/internal/**` with a `404` response, preventing exposure of internal-only controllers
- **CORS** — applies a global CORS policy configured via environment variable, allowing the frontend origin(s) to make cross-origin requests

---

## Tech Stack

| Component | Library / Version |
|---|---|
| Runtime | Java 21 (eclipse-temurin:21) |
| Framework | Spring Boot 3.x |
| Gateway | Spring Cloud Gateway (reactive, WebFlux-based) |
| Service discovery | Spring Cloud Netflix Eureka Client |
| JWT | jjwt-api / jjwt-impl / jjwt-jackson |
| Observability | Spring Boot Actuator, Micrometer + Prometheus |
| Build | Maven (spring-boot-maven-plugin) |

---

## Configuration Profiles

The service ships with two YAML configuration files.

### `application.yml` (default / dev profile)

Used when running locally with `mvn spring-boot:run` or without an explicit `SPRING_PROFILES_ACTIVE`.

Key settings:

```yaml
server:
  port: 8080

jwt:
  secret: ${JWT_SECRET:fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}

gateway:
  cors:
    allowed-origins: http://localhost:3000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Eureka is not explicitly configured here — the default Spring Cloud Eureka client settings apply (`http://localhost:8761/eureka/`).

### `application-docker.yml` (docker profile)

Activated by setting `SPRING_PROFILES_ACTIVE=docker`. Overrides Eureka and CORS settings for containerised deployments.

```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:api-gateway}
    prefer-ip-address: false
  client:
    service-url:
      defaultZone: ${EUREKA_DEFAULT_ZONE:http://eureka:password@discovery-server:8761/eureka/}

gateway:
  cors:
    allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

**Differences between profiles:**

| Setting | dev (application.yml) | docker (application-docker.yml) |
|---|---|---|
| Eureka URL | `http://localhost:8761/eureka/` (default) | `${EUREKA_DEFAULT_ZONE}` — points to named Docker service |
| Eureka hostname | IP-based (default) | `${EUREKA_INSTANCE_HOSTNAME:api-gateway}` |
| CORS origins | `http://localhost:3000` (hardcoded in yml) | `${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}` |

---

## Environment Variables

| Variable | Profile | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | all | `fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256` | HMAC-SHA256 signing key. **Must be at least 256 bits (32 chars).** Change in production. |
| `GATEWAY_CORS_ALLOWED_ORIGINS` | docker | `http://localhost:3000` | Comma-separated list of allowed CORS origins, e.g. `https://app.example.com,https://admin.example.com` |
| `EUREKA_DEFAULT_ZONE` | docker | `http://eureka:password@discovery-server:8761/eureka/` | Eureka server URL(s). Supports comma-separated list for HA. |
| `EUREKA_INSTANCE_HOSTNAME` | docker | `api-gateway` | Hostname the gateway registers under in Eureka. |
| `SPRING_PROFILES_ACTIVE` | docker | _(none)_ | Set to `docker` to activate the docker profile. |

> **Security note**: Never commit a real `JWT_SECRET` to source control. Inject it via a secrets manager or environment variable at runtime.

---

## JWT Authentication Flow

The `JwtAuthenticationFilter` is a `GlobalFilter` with `order = -1`, meaning it runs before route filters but after the internal-blocking route (`order = -100`).

```
Client
  │
  │  HTTP request
  ▼
api-gateway (port 8080)
  │
  ├─ [1] Check path against PUBLIC_PATHS list
  │       If public → skip JWT, forward immediately
  │
  ├─ [2] Read Authorization header
  │       If missing or not "Bearer <token>" → 401 Unauthorized
  │
  ├─ [3] Parse and verify JWT signature
  │       Uses HMAC-SHA256 with JWT_SECRET
  │       If invalid/expired → log WARN, return 401 Unauthorized
  │       If unexpected error → log ERROR, return 401 Unauthorized
  │
  ├─ [4] Extract claims from token payload
  │       subject  → X-User-Id header
  │       "email"  → X-User-Email header
  │
  └─ [5] Forward mutated request to downstream service
              (downstream reads X-User-Id / X-User-Email — no JWT re-parsing needed)
```

**Token format**: Standard JWT signed with HS256. The payload must contain:
- `sub` — user ID (forwarded as `X-User-Id`)
- `email` — user email (forwarded as `X-User-Email`)

Tokens are issued by `account-service` via the `/api/auth/login` and `/api/auth/refresh` endpoints.

---

## Public vs Protected Paths

### Public paths (no JWT required)

These paths bypass the `JwtAuthenticationFilter` entirely:

| Path prefix | Routed to | Purpose |
|---|---|---|
| `/api/auth/register` | account-service | New user registration |
| `/api/auth/login` | account-service | Login — returns JWT access + refresh tokens |
| `/api/auth/refresh` | account-service | Exchange refresh token for new access token |
| `/actuator` | api-gateway itself | Health checks and metrics (see Actuator section) |

### Blocked paths (always 404)

These paths are blocked at the gateway before any filter or route runs:

| Path pattern | Status | Reason |
|---|---|---|
| `/*/internal/**` | 404 | Prevents access to internal service controllers |
| `/internal/**` | 404 | Prevents access to internal service controllers |

### Protected paths (JWT required)

All other paths require a valid `Authorization: Bearer <token>` header:

| Path prefix | Routed to | Strip prefix |
|---|---|---|
| `/api/accounts/**` | account-service | `/api` stripped |
| `/api/transfers/**` | transfer-service | `/api` stripped |
| `/api/transactions/**` | transaction-history-service | `/api` stripped |

---

## CORS Configuration

CORS is handled by a `CorsWebFilter` bean in `SecurityConfig`. The allowed origins are read from the `gateway.cors.allowed-origins` property.

**Allowed methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

**Allowed headers**: `Authorization`, `Content-Type`, `X-Requested-With`

**Credentials**: allowed (`allowCredentials = true`)

**Max age**: 3600 seconds (1 hour preflight cache)

### Setting allowed origins

**Local dev** — edit `application.yml`:

```yaml
gateway:
  cors:
    allowed-origins: http://localhost:3000
```

**Docker / production** — set the `GATEWAY_CORS_ALLOWED_ORIGINS` environment variable. Use a comma-separated list for multiple origins:

```bash
GATEWAY_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

In `docker-compose.yml`:

```yaml
api-gateway:
  environment:
    GATEWAY_CORS_ALLOWED_ORIGINS: "https://app.example.com,https://admin.example.com"
```

If the variable is not set, the default `http://localhost:3000` is used.

---

## Actuator Endpoints

The following actuator endpoints are exposed at `/actuator/*`:

| Endpoint | URL | Description |
|---|---|---|
| Health | `GET /actuator/health` | Returns `{"status":"UP"}` when the service is healthy. Used by Docker health checks. |
| Info | `GET /actuator/info` | Application metadata (name, version). |
| Metrics | `GET /actuator/metrics` | Lists all available Micrometer metric names. |
| Prometheus | `GET /actuator/prometheus` | Prometheus-format metrics scraped by the `prometheus` service in docker-compose. |

All actuator paths are public (no JWT required). The `/actuator/health` endpoint is used by Docker Compose `healthcheck` configurations in dependent services.

---

## Running Locally and with Docker

### Prerequisites

- Java 21+
- Maven 3.9+
- A running Eureka server at `http://localhost:8761` (or set `EUREKA_DEFAULT_ZONE`)

### Run locally with Maven

```bash
# From the repo root
mvn spring-boot:run -pl api-gateway
```

The gateway starts on port `8080`. It connects to Eureka at `http://localhost:8761/eureka/` by default.

To override the JWT secret:

```bash
JWT_SECRET=my-super-secret-key-at-least-32-chars mvn spring-boot:run -pl api-gateway
```

### Build and run with Docker

The Dockerfile uses a multi-stage build. It must be built from the **repo root** (not from inside `api-gateway/`) because it copies the `common` module:

```bash
# Build the image from the repo root
docker build -f api-gateway/Dockerfile -t api-gateway:local .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e JWT_SECRET=my-super-secret-key-at-least-32-chars \
  -e EUREKA_DEFAULT_ZONE=http://eureka:password@localhost:8761/eureka/ \
  -e GATEWAY_CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  api-gateway:local
```

### Run the full stack with Docker Compose

```bash
# From the repo root — required env vars
export EUREKA_PASSWORD=yourpassword

docker-compose up --build
```

The api-gateway will be available at `http://localhost:8080`. It waits for both `discovery-server-1` and `discovery-server-2` to pass their health checks before starting.

To bring down the stack:

```bash
docker-compose down
```

To rebuild only the api-gateway image:

```bash
docker-compose up --build api-gateway
```
