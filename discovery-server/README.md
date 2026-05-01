# Discovery Server

Netflix Eureka-based service registry for the Fintech Platform microservices. All services register here on startup and discover each other through Eureka, enabling dynamic routing via the API Gateway without hardcoded URLs.

## Role in the Platform

```
┌─────────────────────────────────────────────────────────┐
│                   Discovery Cluster                     │
│  ┌─────────────────┐       ┌─────────────────┐         │
│  │ discovery-server │◄─────►│ discovery-server │         │
│  │       -1         │ peer  │       -2         │         │
│  │    :8761         │ sync  │    :8762         │         │
│  └────────▲─────────┘       └────────▲─────────┘         │
│           │                          │                   │
└───────────┼──────────────────────────┼───────────────────┘
            │  register / discover     │
   ┌────────┴──────────────────────────┴────────┐
   │    account-service    transfer-service      │
   │    api-gateway        transaction-history   │
   └─────────────────────────────────────────────┘
```

Every microservice registers itself with the discovery cluster on startup and periodically sends heartbeats. Other services query the registry to locate instances, enabling client-side load balancing and failover.

## Environment Profiles

The server uses Spring profiles to adapt its behavior per environment. If no profile is specified, `dev` is used by default.

| Profile | Mode | Peering | Use Case |
|---------|------|---------|----------|
| `dev` | Standalone | Disabled | Local development, IDE, single instance |
| `docker` | HA Cluster | Enabled | Docker Compose, local HA testing |
| `prod` | HA Cluster | Enabled | Production deployment |

### dev (default)

- Runs as a single standalone instance on `localhost:8761`
- `register-with-eureka` and `fetch-registry` are both `false` (no peer)
- Suitable for running from IDE or `mvn spring-boot:run`

### docker

- Designed for the docker-compose HA setup with 2 instances
- Each instance registers with its peer via `EUREKA_PEER_URLS`
- Self-preservation enabled with `renewal-percent-threshold: 0.49` (tuned for a small 2-node cluster)
- Hostname set via `EUREKA_INSTANCE_HOSTNAME` environment variable

### prod

- Same HA behavior as `docker` but with production-tuned settings
- `renewal-percent-threshold: 0.85` (standard Eureka default for larger clusters)
- `eviction-interval-timer-in-ms: 30000` for faster stale-instance cleanup

## Running

### Standalone (dev)

```bash
# From project root
mvn spring-boot:run -pl discovery-server

# Or build and run the jar
mvn clean package -pl discovery-server -am -DskipTests
java -jar discovery-server/target/*.jar
```

The dashboard is available at http://localhost:8761 (credentials: `eureka` / `password`).

### High Availability (docker)

```bash
# Start only the discovery cluster
docker compose up discovery-server-1 discovery-server-2

# Or start the full platform
docker compose up -d
```

| Instance | Host Port | Dashboard |
|----------|-----------|-----------|
| discovery-server-1 | 8761 | http://localhost:8761 |
| discovery-server-2 | 8762 | http://localhost:8762 |

Both instances peer-replicate their registries. If one goes down, the other continues serving the full registry. When the failed instance recovers, it automatically syncs back.

## Security

The Eureka Dashboard and API are protected with HTTP Basic Authentication. Only the health endpoint is publicly accessible (needed for Docker/load balancer health checks).

### Credentials

Credentials are read from environment variables with sensible defaults for development:

| Variable | Default | Description |
|----------|---------|-------------|
| `EUREKA_USERNAME` | `eureka` | Basic Auth username |
| `EUREKA_PASSWORD` | `password` | Basic Auth password |

To change credentials, set the environment variables before starting the server:

```bash
# Standalone
EUREKA_USERNAME=admin EUREKA_PASSWORD=s3cret mvn spring-boot:run -pl discovery-server

# Docker Compose — edit the environment section in docker-compose.yml
# or use a .env file
```

### Access Rules

| Path | Auth Required | Notes |
|------|---------------|-------|
| `/actuator/health` | No | Public — used by Docker healthcheck and load balancers |
| `/actuator/**` | Yes | All other actuator endpoints require Basic Auth |
| `/eureka/**` | Yes | Eureka REST API (CSRF disabled for client registration) |
| `/` (Dashboard) | Yes | Eureka web dashboard |

### Service Client Configuration

Service clients include credentials in the Eureka connection URL:

```
http://eureka:password@discovery-server-1:8761/eureka/,http://eureka:password@discovery-server-2:8761/eureka/
```

This is set via the `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` environment variable in docker-compose.

## Actuator Endpoints

Spring Boot Actuator endpoints exposed by the server:

| Endpoint | URL | Auth | Description |
|----------|-----|------|-------------|
| Health | `/actuator/health` | No | Application health status (UP/DOWN) |
| Info | `/actuator/info` | Yes | Application info metadata |
| Metrics | `/actuator/metrics` | Yes | Micrometer metrics (JVM, HTTP, etc.) |
| Prometheus | `/actuator/prometheus` | Yes | Metrics in Prometheus text format |

### Accessing Endpoints

```bash
# Health (no auth needed)
curl http://localhost:8761/actuator/health

# Prometheus metrics (auth required)
curl -u eureka:password http://localhost:8761/actuator/prometheus

# Specific metric
curl -u eureka:password http://localhost:8761/actuator/metrics/jvm.memory.used
```

Prometheus is pre-configured to scrape both discovery server instances (see `infra/prometheus/prometheus.yml`).

## High Availability

### How Peer Replication Works

In `docker` and `prod` profiles, each Eureka instance registers with its peer(s) via `EUREKA_PEER_URLS`. When a service registers with one instance, that registration is replicated to all peers. Each instance maintains a full copy of the registry.

### Failure Behavior

| Scenario | Behavior |
|----------|----------|
| One peer goes down | Remaining instance continues serving the full registry from its local copy. Logs a warning about the unavailable peer and retries periodically. |
| Peer recovers | Automatic registry sync resumes. No manual intervention needed. |
| All peers down | Each instance operates independently with its own local registry. |
| Network partition | Self-preservation mode prevents premature eviction of registered instances. |

### Self-Preservation

Self-preservation prevents Eureka from evicting instances when it detects that fewer heartbeats than expected are arriving (which may indicate a network issue rather than actual instance failures).

- **docker profile**: threshold at `0.49` — tuned for a 2-node cluster where losing one peer means losing 50% of heartbeats
- **prod profile**: threshold at `0.85` — standard setting for larger clusters

## Environment Variables

| Variable | Default | Used In | Description |
|----------|---------|---------|-------------|
| `EUREKA_USERNAME` | `eureka` | All profiles | Basic Auth username |
| `EUREKA_PASSWORD` | `password` | All profiles | Basic Auth password |
| `EUREKA_INSTANCE_HOSTNAME` | `discovery-server` | docker, prod | Hostname for this instance |
| `EUREKA_PEER_URLS` | — | docker, prod | Peer instance URL(s) with credentials |
| `SPRING_PROFILES_ACTIVE` | `dev` (app) / `docker` (Dockerfile) | All | Active Spring profile |

## Logging

- **dev profile**: Human-readable console output with timestamp, thread, correlationId, level, logger, and message
- **docker/prod profiles**: Structured JSON output (LogstashEncoder) suitable for log aggregation systems (ELK, CloudWatch, etc.)

The `correlationId` from MDC is included in every log entry when available, enabling request tracing across services.

## Build

```bash
# Compile
mvn clean compile -pl discovery-server -am

# Package (skip tests)
mvn clean package -pl discovery-server -am -DskipTests

# Run tests
mvn test -pl discovery-server

# Full verify
mvn clean verify -pl discovery-server -am

# Docker image
docker build -f discovery-server/Dockerfile .
```
