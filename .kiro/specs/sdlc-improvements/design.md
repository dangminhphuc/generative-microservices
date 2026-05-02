# SDLC Improvements — Design

## Overview

This spec covers all SDLC improvement tasks for `api-gateway` and `discovery-server` services, identified from the production readiness assessment.

**Services affected**: `api-gateway`, `discovery-server`  
**Base branch**: `develop`  
**Branch convention**: `fix/<service>-<description>`, `test/<service>-<description>`, `feat/<service>-<description>`, `docs/<service>-<description>`

---

## Git Workflow (applies to every task)

```
1. git checkout develop && git pull origin develop
2. git checkout -b <branch-name>
3. [implement changes]
4. git add <specific files only>
5. git commit -m "<type>(<scope>): <description>"
6. git push -u origin <branch-name>
7. → ASK USER FOR MERGE CONFIRMATION before merging to develop
```

---

## Phase 1 — Security Fixes (api-gateway)

### 1.1 Block Internal Endpoints

**Problem**: Spring Cloud Gateway discovery locator forwards `/*/internal/**` paths to downstream services, exposing `InternalAccountController` to external traffic.

**Root cause**: No blocking route exists before the discovery locator routes.

**Solution**: Add a gateway route with:
- `id: block-internal-endpoints`
- `predicates: Path=/*/internal/**,/internal/**`
- `filters: SetStatus=404`
- `order: -100` (executes before all other routes)

**Files**:
- `api-gateway/src/main/resources/application.yml`

**Correctness properties**:
- Internal paths (`/*/internal/**`, `/internal/**`) must return 404
- Non-internal paths must NOT be affected

---

### 1.2 Fix JWT Exception Handling

**Problem**: `JwtAuthenticationFilter` has a single broad `catch (Exception e)` with no Logger — JWT failures are silent, making security auditing and debugging impossible.

**Root cause**: No SLF4J Logger declared; single catch block does not distinguish JWT errors from system errors.

**Solution**:
- Add `private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class)`
- Split catch into:
  - `catch (JwtException e)` → `log.warn("JWT validation failed: {}", e.getMessage())`
  - `catch (Exception e)` → `log.error("Unexpected error during JWT processing", e)`
- Keep 401 response behavior unchanged

**Files**:
- `api-gateway/src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java`

**Correctness properties**:
- Logger field must be declared
- Two separate catch blocks must exist
- Existing 401 behavior must be preserved

---

### 1.3 Externalize CORS Allowed Origins

**Problem**: `SecurityConfig` hardcodes `"http://localhost:3000"` — cannot be overridden for docker/prod environments via environment variable.

**Root cause**: No `@Value` injection; origin is a literal string in source code.

**Solution**:
- Inject `@Value("${gateway.cors.allowed-origins:http://localhost:3000}")` as a `String` field
- Parse comma-separated string into `List<String>`
- Replace hardcoded `List.of("http://localhost:3000")` with injected value
- Add `gateway.cors.allowed-origins` property to `application.yml`
- Add `gateway.cors.allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}` to `application-docker.yml`

**Files**:
- `api-gateway/src/main/java/com/fintech/gateway/config/SecurityConfig.java`
- `api-gateway/src/main/resources/application.yml`
- `api-gateway/src/main/resources/application-docker.yml`

**Correctness properties**:
- No hardcoded origin literal in `SecurityConfig.java` (only as `@Value` default)
- `gateway.cors.allowed-origins` property present in `application.yml`
- `http://localhost:3000` preserved as default fallback

---

## Phase 2 — Testing

### 2.1 api-gateway Integration Test

**Problem**: No `@SpringBootTest` integration test exists — cannot verify the gateway application context loads correctly.

**Solution**: Create/update `ApiGatewayApplicationTests.java` with:
- Context loads without exception
- `/actuator/health` returns 200
- JWT filter is registered in context
- Public paths bypass JWT filter

**Files**:
- `api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`

---

### 2.2 discovery-server HA Integration Tests

**Problem**: Only 6 test methods exist — no tests for security, health endpoint, or HA peer replication scenarios.

**Solution**: Add tests for:
- Context loads with standalone profile
- `/actuator/health` returns UP
- Basic Auth required for `/eureka/apps` (401 without credentials)
- Basic Auth accepted for `/eureka/apps` (200 with credentials)
- Peer replication URL format validation

**Files**:
- `discovery-server/src/test/java/com/fintech/discovery/DiscoveryServerApplicationTests.java`
- `discovery-server/src/test/java/com/fintech/discovery/DiscoveryServerHaTest.java` (new)

---

## Phase 3 — Deployment

### 3.1 discovery-server HA docker-compose

**Problem**: `docker-compose.yml` runs only 1 discovery-server instance — HA peer replication is configured but never deployed locally.

**Solution**:
- Add `discovery-server-2` service to `docker-compose.yml`
- Configure peer replication:
  - `discovery-server-1` → registers with `discovery-server-2`
  - `discovery-server-2` → registers with `discovery-server-1`
- Update `api-gateway` and other services to use both Eureka URLs
- Add `depends_on` with health check conditions

**Files**:
- `docker-compose.yml`

---

## Phase 4 — Documentation

### 4.1 api-gateway README

**Problem**: `api-gateway` has no README — developers have no reference for configuration, environment variables, or local setup.

**Solution**: Create `api-gateway/README.md` covering:
- Service description and role in the system
- Tech stack
- Configuration profiles (dev/docker/prod)
- Environment variables table with defaults
- JWT authentication flow
- Public vs protected paths
- CORS configuration
- Actuator endpoints
- How to run locally and with Docker

**Files**:
- `api-gateway/README.md` (new)
