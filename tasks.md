# SDLC Improvement Tasks

## Overview

Improvement plan for `api-gateway` and `discovery-server` based on SDLC readiness assessment.

**Branch convention**: `fix/<service>-<short-description>` or `feat/<service>-<short-description>`  
**Base branch**: `develop`  
**Merge target**: `develop`

---

## Git Workflow (per task)

Each task follows this exact workflow:

```
1. git checkout develop && git pull origin develop
2. git checkout -b <branch-name>
3. [implement changes]
4. git add <specific files>
5. git commit -m "<type>(<scope>): <description>"
6. git push -u origin <branch-name>
7. → ASK FOR MERGE CONFIRMATION before merging
```

---

## Phase 1 — Security Fixes (api-gateway)

### Task 1.1 — Block internal endpoints

**Branch**: `fix/api-gateway-block-internal-endpoints`  
**Priority**: 🔴 Critical  
**Files to change**:
- `api-gateway/src/main/resources/application.yml`

**What to do**:
- Add a Spring Cloud Gateway route with `id: block-internal-endpoints`
- Predicate: `Path=/*/internal/**,/internal/**`
- Filter: `SetStatus=404`
- Order: `-100` (must execute before discovery locator routes)

**Verify before commit**:
- [ ] Route appears in `application.yml` with correct order
- [ ] `BugConditionExplorationTest.isBugCondition_InternalExposed_noBlockingRouteInApplicationYml` passes
- [ ] `PreservationPropertyTest` still passes (non-internal paths not affected)

**Commit message**: `fix(api-gateway): block internal endpoints via gateway route (order -100, SetStatus 404)`

---

### Task 1.2 — Fix JWT exception handling

**Branch**: `fix/api-gateway-jwt-exception-logging`  
**Priority**: 🔴 High  
**Files to change**:
- `api-gateway/src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java`

**What to do**:
- Add SLF4J `Logger` field via `LoggerFactory.getLogger`
- Split `catch (Exception e)` into two blocks:
  - `catch (JwtException e)` → `log.warn("JWT validation failed: {}", e.getMessage())`
  - `catch (Exception e)` → `log.error("Unexpected error during JWT processing", e)`
- Keep existing 401 response behavior unchanged

**Verify before commit**:
- [ ] `Logger` field declared in class
- [ ] Two separate catch blocks present
- [ ] `log.warn` used for `JwtException`
- [ ] `log.error` used for generic `Exception`
- [ ] `BugConditionExplorationTest.isBugCondition_JwtException_catchBlockIsUndifferentiatedAndSilent` passes
- [ ] `JwtAuthenticationFilterTest` still passes

**Commit message**: `fix(api-gateway): distinguish JwtException (WARN) from system exceptions (ERROR) with SLF4J logging`

---

### Task 1.3 — Externalize CORS allowed origins

**Branch**: `fix/api-gateway-externalize-cors`  
**Priority**: 🟡 Medium  
**Files to change**:
- `api-gateway/src/main/java/com/fintech/gateway/config/SecurityConfig.java`
- `api-gateway/src/main/resources/application.yml`
- `api-gateway/src/main/resources/application-dev.yml`
- `api-gateway/src/main/resources/application-docker.yml`

**What to do**:
- In `SecurityConfig.java`:
  - Add `@Value("${gateway.cors.allowed-origins:http://localhost:3000}")` field
  - Parse comma-separated string into list
  - Replace hardcoded `List.of("http://localhost:3000")` with injected value
- In `application.yml`:
  - Add `gateway.cors.allowed-origins: http://localhost:3000`
- In `application-docker.yml`:
  - Add `gateway.cors.allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}`

**Verify before commit**:
- [ ] No hardcoded origin string in `SecurityConfig.java` (only as default fallback in `@Value`)
- [ ] `gateway.cors.allowed-origins` property present in `application.yml`
- [ ] `BugConditionExplorationTest.isBugCondition_CorsHardcode_allowedOriginsIsLiteralInSourceCode` passes
- [ ] `PreservationPropertyTest` still passes (localhost:3000 preserved as fallback)

**Commit message**: `fix(api-gateway): externalize CORS allowed-origins via GATEWAY_CORS_ALLOWED_ORIGINS env var`

---

## Phase 2 — Testing

### Task 2.1 — Add api-gateway integration test

**Branch**: `test/api-gateway-integration-test`  
**Priority**: 🟡 Medium  
**Files to change**:
- `api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java` (create or update)

**What to do**:
- Create `@SpringBootTest` test class
- Add test: Spring context loads without exception
- Add test: `/actuator/health` returns 200
- Add test: JWT filter is registered in context
- Add test: Public paths bypass JWT filter (e.g. `/api/auth/login`)

**Verify before commit**:
- [ ] All 4 test methods present
- [ ] Tests pass with `mvn test -pl api-gateway`
- [ ] `BugConditionExplorationTest.isBugCondition_NoTest_integrationTestClassDoesNotExist` passes

**Commit message**: `test(api-gateway): add integration tests for context load, health endpoint, JWT filter, public path bypass`

---

### Task 2.2 — Add discovery-server HA integration tests

**Branch**: `test/discovery-server-ha-tests`  
**Priority**: 🔴 Critical  
**Files to change**:
- `discovery-server/src/test/java/com/fintech/discovery/DiscoveryServerApplicationTests.java` (update)
- `discovery-server/src/test/java/com/fintech/discovery/DiscoveryServerHaTest.java` (create)

**What to do**:
- Add test: context loads with `standalone` profile
- Add test: Eureka dashboard endpoint accessible
- Add test: `/actuator/health` returns UP
- Add test: Basic Auth required for `/eureka/apps` (401 without credentials)
- Add test: Basic Auth accepted for `/eureka/apps` (200 with credentials)
- Add test: Peer replication URL format validation (prod profile)

**Verify before commit**:
- [ ] All tests pass with `mvn test -pl discovery-server`
- [ ] No test requires external Eureka peer (use `@SpringBootTest` with standalone profile)

**Commit message**: `test(discovery-server): add HA integration tests for security, health, and peer replication`

---

## Phase 3 — Deployment

### Task 3.1 — Discovery server HA docker-compose setup

**Branch**: `feat/discovery-server-ha-docker-compose`  
**Priority**: 🔴 Critical  
**Files to change**:
- `docker-compose.yml` (root level, update)

**What to do**:
- Add second discovery-server instance (`discovery-server-2`)
- Configure peer replication between `discovery-server-1` and `discovery-server-2`:
  - `discovery-server-1` registers with `discovery-server-2`
  - `discovery-server-2` registers with `discovery-server-1`
- Update `api-gateway` and other services to use both Eureka URLs
- Add health check dependencies between services

**Verify before commit**:
- [ ] `docker-compose up` starts both discovery-server instances
- [ ] Both instances show each other in Eureka dashboard
- [ ] api-gateway connects to both Eureka instances
- [ ] `docker-compose ps` shows all services healthy

**Commit message**: `feat(discovery-server): add multi-peer Eureka HA setup in docker-compose`

---

## Phase 4 — Documentation

### Task 4.1 — Create api-gateway README

**Branch**: `docs/api-gateway-readme`  
**Priority**: 🟡 Medium  
**Files to change**:
- `api-gateway/README.md` (create)

**What to do**:
- Service description and role in the system
- Tech stack (Spring Cloud Gateway, JWT, Eureka)
- Configuration profiles (dev/docker/prod) and differences
- Environment variables table (with defaults)
- JWT authentication flow diagram (text-based)
- Public vs protected paths list
- CORS configuration guide
- Actuator endpoints
- How to run locally
- How to run with Docker

**Verify before commit**:
- [ ] All sections present
- [ ] Environment variables table complete
- [ ] No placeholder text left

**Commit message**: `docs(api-gateway): add comprehensive README`

---

## Progress Tracker

| Task | Status | Branch | PR |
|------|--------|--------|----|
| 1.1 Block internal endpoints | ⬜ Not started | - | - |
| 1.2 JWT exception logging | ⬜ Not started | - | - |
| 1.3 Externalize CORS | ⬜ Not started | - | - |
| 2.1 api-gateway integration test | ⬜ Not started | - | - |
| 2.2 discovery-server HA tests | ⬜ Not started | - | - |
| 3.1 HA docker-compose | ⬜ Not started | - | - |
| 4.1 api-gateway README | ⬜ Not started | - | - |
