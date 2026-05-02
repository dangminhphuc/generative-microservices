# SDLC Improvements — Tasks

## Phase 1 — Security Fixes (api-gateway)

- [x] 1.1 Block internal endpoints via gateway route
  - [x] 1.1.1 Checkout develop and create branch `fix/api-gateway-block-internal-endpoints`
  - [x] 1.1.2 Add `block-internal-endpoints` route to `application.yml` with `Path=/*/internal/**,/internal/**`, `SetStatus=404`, `order: -100`
  - [x] 1.1.3 Verify `BugConditionExplorationTest.isBugCondition_InternalExposed_noBlockingRouteInApplicationYml` passes
  - [x] 1.1.4 Verify `PreservationPropertyTest` still passes
  - [x] 1.1.5 Commit: `fix(api-gateway): block internal endpoints via gateway route (order -100, SetStatus 404)`
  - [x] 1.1.6 Push branch and ask user for merge confirmation

- [x] 1.2 Fix JWT exception handling with SLF4J logging
  - [x] 1.2.1 Checkout develop and create branch `fix/api-gateway-jwt-exception-logging`
  - [x] 1.2.2 Add `private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class)` to `JwtAuthenticationFilter.java`
  - [x] 1.2.3 Split `catch (Exception e)` into `catch (JwtException e)` with `log.warn` and `catch (Exception e)` with `log.error`
  - [x] 1.2.4 Verify `BugConditionExplorationTest.isBugCondition_JwtException_catchBlockIsUndifferentiatedAndSilent` passes
  - [x] 1.2.5 Verify `JwtAuthenticationFilterTest` still passes
  - [x] 1.2.6 Commit: `fix(api-gateway): distinguish JwtException (WARN) from system exceptions (ERROR) with SLF4J logging`
  - [x] 1.2.7 Push branch and ask user for merge confirmation

- [x] 1.3 Externalize CORS allowed-origins via environment variable
  - [x] 1.3.1 Checkout develop and create branch `fix/api-gateway-externalize-cors`
  - [x] 1.3.2 Add `@Value("${gateway.cors.allowed-origins:http://localhost:3000}")` field to `SecurityConfig.java`
  - [x] 1.3.3 Parse comma-separated string into `List<String>` and replace hardcoded origin
  - [x] 1.3.4 Add `gateway.cors.allowed-origins: http://localhost:3000` to `application.yml`
  - [x] 1.3.5 Add `gateway.cors.allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}` to `application-docker.yml`
  - [x] 1.3.6 Verify `BugConditionExplorationTest.isBugCondition_CorsHardcode_allowedOriginsIsLiteralInSourceCode` passes
  - [x] 1.3.7 Verify `PreservationPropertyTest` still passes (localhost:3000 preserved as fallback)
  - [x] 1.3.8 Commit: `fix(api-gateway): externalize CORS allowed-origins via GATEWAY_CORS_ALLOWED_ORIGINS env var`
  - [x] 1.3.9 Push branch and ask user for merge confirmation

## Phase 2 — Testing

- [x] 2.1 Add api-gateway integration tests
  - [x] 2.1.1 Checkout develop and create branch `test/api-gateway-integration-test`
  - [x] 2.1.2 Create/update `ApiGatewayApplicationTests.java` with `@SpringBootTest`
  - [x] 2.1.3 Add test: Spring context loads without exception
  - [x] 2.1.4 Add test: `/actuator/health` returns 200
  - [x] 2.1.5 Add test: JWT filter is registered in context
  - [x] 2.1.6 Add test: public paths bypass JWT filter (e.g. `/api/auth/login`)
  - [x] 2.1.7 Run `mvn test -pl api-gateway` and verify all tests pass
  - [x] 2.1.8 Commit: `test(api-gateway): add integration tests for context load, health endpoint, JWT filter, public path bypass`
  - [ ] 2.1.9 Push branch and ask user for merge confirmation

- [~] 2.2 Add discovery-server HA integration tests
  - [ ] 2.2.1 Checkout develop and create branch `test/discovery-server-ha-tests`
  - [ ] 2.2.2 Update `DiscoveryServerApplicationTests.java` with standalone profile tests
  - [ ] 2.2.3 Create `DiscoveryServerHaTest.java` with security and health tests
  - [ ] 2.2.4 Add test: `/actuator/health` returns UP
  - [ ] 2.2.5 Add test: 401 without Basic Auth on `/eureka/apps`
  - [ ] 2.2.6 Add test: 200 with valid Basic Auth on `/eureka/apps`
  - [ ] 2.2.7 Add test: peer replication URL format validation
  - [ ] 2.2.8 Run `mvn test -pl discovery-server` and verify all tests pass
  - [ ] 2.2.9 Commit: `test(discovery-server): add HA integration tests for security, health, and peer replication`
  - [ ] 2.2.10 Push branch and ask user for merge confirmation

## Phase 3 — Deployment

- [ ] 3.1 discovery-server HA docker-compose setup
  - [ ] 3.1.1 Checkout develop and create branch `feat/discovery-server-ha-docker-compose`
  - [ ] 3.1.2 Add `discovery-server-2` service to `docker-compose.yml`
  - [ ] 3.1.3 Configure peer replication: `discovery-server-1` ↔ `discovery-server-2`
  - [ ] 3.1.4 Update `api-gateway` defaultZone to include both Eureka instances
  - [ ] 3.1.5 Update other services (account-service, etc.) to use both Eureka URLs
  - [ ] 3.1.6 Add `depends_on` with health check conditions for dependent services
  - [ ] 3.1.7 Run `docker-compose up` and verify both instances appear in Eureka dashboard
  - [ ] 3.1.8 Verify `docker-compose ps` shows all services healthy
  - [ ] 3.1.9 Commit: `feat(discovery-server): add multi-peer Eureka HA setup in docker-compose`
  - [ ] 3.1.10 Push branch and ask user for merge confirmation

## Phase 4 — Documentation

- [ ] 4.1 Create api-gateway README
  - [ ] 4.1.1 Checkout develop and create branch `docs/api-gateway-readme`
  - [ ] 4.1.2 Create `api-gateway/README.md`
  - [ ] 4.1.3 Add section: Service description and role in the system
  - [ ] 4.1.4 Add section: Tech stack
  - [ ] 4.1.5 Add section: Configuration profiles (dev/docker/prod) with differences
  - [ ] 4.1.6 Add section: Environment variables table with defaults
  - [ ] 4.1.7 Add section: JWT authentication flow
  - [ ] 4.1.8 Add section: Public vs protected paths list
  - [ ] 4.1.9 Add section: CORS configuration guide
  - [ ] 4.1.10 Add section: Actuator endpoints
  - [ ] 4.1.11 Add section: How to run locally and with Docker
  - [ ] 4.1.12 Commit: `docs(api-gateway): add comprehensive README`
  - [ ] 4.1.13 Push branch and ask user for merge confirmation
