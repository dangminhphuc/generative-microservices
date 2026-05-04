# API Gateway

The API Gateway is the single entry point for all client traffic in the Fintech Platform. It sits in front of the downstream microservices and handles:

- **Request routing** — path-based routing to `account-service`, `transfer-service`, and `transaction-history-service` via Spring Cloud Gateway and Eureka service discovery.
- **JWT authentication** — validates Bearer tokens on protected endpoints and forwards `X-User-Id` / `X-User-Email` headers to downstream services.
- **Rate limiting** — Redis-backed token-bucket rate limiting (10 req/min per IP on auth routes, 100 req/min per user on protected routes).
- **CORS** — configurable allowed origins for browser-based clients.
- **Circuit breaking** — Resilience4j circuit breakers with a fallback controller returning `503 Service Unavailable`.
- **Internal endpoint blocking** — prevents external access to `/*/internal/**` paths.
- **Gateway secret injection** — attaches an `X-Gateway-Secret` header so downstream services can verify requests originated from the gateway.

---

## Tech Stack

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Gateway | (managed by Spring Cloud BOM) |
| Spring Cloud Netflix Eureka Client | (managed by Spring Cloud BOM) |
| Resilience4j (Circuit Breaker) | (managed by Spring Cloud BOM) |
| Spring Boot Actuator | (managed by Spring Boot parent) |
| Spring Data Redis Reactive | (managed by Spring Boot parent) |
| JJWT (io.jsonwebtoken) | 0.12.6 |
| Build tool | Maven (multi-module, parent `fintech-platform`) |
| Container runtime | Eclipse Temurin 21 (Alpine) |

---

## Configuration Profiles

The gateway uses Spring profiles to separate environment-specific settings. The default profile is `dev`.

### `dev` (default)

Activated when no profile is set, or explicitly with `SPRING_PROFILES_ACTIVE=dev`.

| Setting | Value |
|---|---|
| Eureka hostname | `localhost` |
| Eureka URL | `http://eureka:password@localhost:8761/eureka/` |
| Redis | `localhost:6379` |
| CORS origins | `http://localhost:3000` |
| JWT secret | Built-in development default (see Environment Variables) |
| Gateway internal secret | `dev-gateway-secret-for-local-only` |

### `docker`

Activated with `SPRING_PROFILES_ACTIVE=docker`. Used inside `docker-compose`.

| Setting | Value |
|---|---|
| Eureka hostname | `${EUREKA_INSTANCE_HOSTNAME:api-gateway}` |
| Eureka URL | `${EUREKA_DEFAULT_ZONE:http://eureka:password@discovery-server:8761/eureka/}` |
| Redis | `${REDIS_HOST}:${REDIS_PORT}` (set by docker-compose) |
| CORS origins | `${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}` |

### `prod`

Activated with `SPRING_PROFILES_ACTIVE=prod`. All secrets are **required** — the application will fail to start if they are missing.

| Setting | Value |
|---|---|
| Eureka hostname | `${EUREKA_INSTANCE_HOSTNAME:api-gateway}` |
| Eureka URL | `${EUREKA_DEFAULT_ZONE}` (**required**, no default) |
| CORS origins | `${GATEWAY_CORS_ALLOWED_ORIGINS}` (**required**, no default) |
| JWT secret | `${JWT_SECRET}` (**required**, no default — enforced by `JwtSecretValidator`) |

---

## Environment Variables

| Variable | Description | Default | Required in prod? |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev`, `docker`, `prod`) | `dev` | Yes |
| `JWT_SECRET` | HMAC-SHA256 signing key for JWT validation (≥ 256 bits) | `fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256` | **Yes** (no default) |
| `REDIS_HOST` | Redis hostname for rate limiter | `localhost` | Yes |
| `REDIS_PORT` | Redis port | `6379` | Yes |
| `EUREKA_INSTANCE_HOSTNAME` | Hostname this instance registers with in Eureka | `api-gateway` | Yes |
| `EUREKA_DEFAULT_ZONE` | Eureka service URL(s), comma-separated | `http://eureka:password@discovery-server:8761/eureka/` | **Yes** (no default) |
| `EUREKA_USERNAME` | Eureka Basic Auth username (used in docker-compose) | `eureka` | Yes |
| `EUREKA_PASSWORD` | Eureka Basic Auth password (used in docker-compose) | — | **Yes** (no default) |
| `GATEWAY_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins | `http://localhost:3000` | **Yes** (no default) |
| `GATEWAY_INTERNAL_SECRET` | Shared secret injected as `X-Gateway-Secret` header | `dev-gateway-secret-for-local-only` | Yes |

---

## JWT Authentication Flow

The `JwtAuthenticationFilter` is a `GlobalFilter` (order `-1`) that runs on every request:

```
Client request
      │
      ▼
┌─────────────────────────┐
│  Is path public?        │──── Yes ──▶ Forward to downstream (no auth)
│  (see Public Paths)     │
└─────────┬───────────────┘
          │ No
          ▼
┌─────────────────────────┐
│  Authorization header   │──── Missing/invalid ──▶ 401 Unauthorized
│  present with "Bearer"? │
└─────────┬───────────────┘
          │ Yes
          ▼
┌─────────────────────────┐
│  Parse & verify JWT     │──── JwtException ──▶ log.warn + 401 Unauthorized
│  with HMAC-SHA256 key   │──── Other Exception ──▶ log.error + 401 Unauthorized
└─────────┬───────────────┘
          │ Valid
          ▼
┌─────────────────────────┐
│  Extract claims:        │
│  • subject → X-User-Id  │
│  • email  → X-User-Email│
└─────────┬───────────────┘
          │
          ▼
   Forward to downstream
   (with injected headers)
```

After JWT validation, the `GatewaySecretFilter` (order `0`) appends the `X-Gateway-Secret` header so downstream services can verify the request came through the gateway.

---

## Public vs Protected Paths

### Public paths (bypass JWT authentication)

These paths are defined in `JwtAuthenticationFilter.PUBLIC_PATHS`:

| Path prefix | Description |
|---|---|
| `/api/auth/register` | User registration |
| `/api/auth/login` | User login |
| `/api/auth/refresh` | Token refresh |
| `/actuator` | Health, metrics, and info endpoints |

### Blocked paths

| Path pattern | Behavior |
|---|---|
| `/*/internal/**` | Returns `404 Not Found` (blocked by gateway route, order `-100`) |
| `/internal/**` | Returns `404 Not Found` (blocked by gateway route, order `-100`) |

### Protected paths (require valid JWT)

All other paths require a valid `Authorization: Bearer <token>` header. The main routed services are:

| Path prefix | Downstream service | Rate limit |
|---|---|---|
| `/api/auth/**` | `account-service` | 10 req/min per IP |
| `/api/accounts/**` | `account-service` | 100 req/min per user |
| `/api/transfers/**` | `transfer-service` | 100 req/min per user |
| `/api/transactions/**` | `transaction-history-service` | 100 req/min per user |

> **Note:** `/api/auth/**` routes are public for JWT purposes but still rate-limited at 10 req/min per IP to prevent brute-force attacks.

All routes use `StripPrefix=1` — the `/api` segment is removed before forwarding to the downstream service.

---

## CORS Configuration

CORS is configured in `SecurityConfig` via a `CorsWebFilter` bean.

**Default settings:**

| Setting | Value |
|---|---|
| Allowed origins | `http://localhost:3000` (configurable via `GATEWAY_CORS_ALLOWED_ORIGINS`) |
| Allowed methods | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Allowed headers | `Authorization`, `Content-Type`, `X-Requested-With` |
| Allow credentials | `true` |
| Max age | `3600` seconds (1 hour) |

**Overriding origins:**

Set the `GATEWAY_CORS_ALLOWED_ORIGINS` environment variable to a comma-separated list:

```bash
# Single origin
export GATEWAY_CORS_ALLOWED_ORIGINS=https://app.example.com

# Multiple origins
export GATEWAY_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

In `docker-compose.yml`, set it in the service environment:

```yaml
environment:
  GATEWAY_CORS_ALLOWED_ORIGINS: https://app.example.com,https://admin.example.com
```

---

## Actuator Endpoints

The following actuator endpoints are exposed:

| Endpoint | Description |
|---|---|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus-format metrics scrape endpoint |

All actuator paths are public (no JWT required). Metrics are tagged with `application: api-gateway`.

---

## Circuit Breakers

Each downstream route has a Resilience4j circuit breaker. When a circuit opens, requests are forwarded to the `FallbackController` which returns:

```json
{
  "error": "Service temporarily unavailable",
  "code": "SERVICE_UNAVAILABLE"
}
```

**Circuit breaker configuration** (same for all instances):

| Parameter | Value |
|---|---|
| Sliding window size | 10 |
| Failure rate threshold | 50% |
| Wait duration in open state | 30 seconds |
| Permitted calls in half-open state | 3 |

Circuit breaker instances: `account-service-auth-cb`, `account-service-cb`, `transfer-service-cb`, `transaction-history-service-cb`.

---

## How to Run

### Prerequisites

- Java 21
- Maven 3.9+
- Redis (for rate limiting)
- A running Eureka discovery server (see `discovery-server/`)

### Run locally (dev profile)

```bash
# From the project root — build the common module first
mvn -pl common install -DskipTests

# Start the gateway
mvn -pl api-gateway spring-boot:run
```

The gateway starts on `http://localhost:8080` with the `dev` profile. It expects:
- Eureka at `http://localhost:8761`
- Redis at `localhost:6379`

### Run with Docker Compose

```bash
# Create a .env file with required secrets
echo "EUREKA_PASSWORD=your-eureka-password" > .env

# Build and start all services
docker compose up --build
```

The gateway is available at:
- `http://localhost:8080` (api-gateway-1)
- `http://localhost:8090` (api-gateway-2)

### Build the Docker image manually

```bash
# From the project root (build context needs parent pom + common module)
docker build -f api-gateway/Dockerfile -t api-gateway .
```

The Dockerfile uses a multi-stage build:
1. **Build stage** — `eclipse-temurin:21-jdk-alpine` with Maven, builds `common` and `api-gateway` modules.
2. **Runtime stage** — `eclipse-temurin:21-jre-alpine`, copies the fat JAR, exposes port `8080`.

### Verify the gateway is running

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```
