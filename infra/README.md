# Infrastructure Setup Guide

## Prometheus Credentials

Prometheus scrapes metrics from `discovery-server` using HTTP Basic Auth. Credentials are **not** stored in source control — they are injected at runtime via environment variables.

### How it works

`infra/prometheus/prometheus.yml` is listed in `.gitignore` and is **never committed**. Instead, `prometheus.yml.template` is committed with `${EUREKA_USERNAME}` and `${EUREKA_PASSWORD}` placeholders. When `docker-compose up` starts the Prometheus container, it runs `envsubst` to generate the real `prometheus.yml` inside the container from the template.

### Setup steps

**1. Set the required environment variables**

The simplest approach is to create a `.env` file in the project root (it is already gitignored via the default Docker Compose `.env` convention):

```bash
# .env  — DO NOT commit this file
EUREKA_USERNAME=eureka
EUREKA_PASSWORD=<your-secure-password>
```

Alternatively, export them in your shell before running docker-compose:

```bash
export EUREKA_USERNAME=eureka
export EUREKA_PASSWORD=<your-secure-password>
```

**2. Start the stack**

```bash
docker-compose up -d
```

Docker Compose will substitute `EUREKA_USERNAME` and `EUREKA_PASSWORD` into the Prometheus template at container startup. No manual file copying is needed.

**3. Verify Prometheus is scraping**

Open [http://localhost:9090/targets](http://localhost:9090/targets) and confirm that `discovery-server-1` and `discovery-server-2` show state **UP**.

### Notes

- `EUREKA_USERNAME` defaults to `eureka` if not set (matches the discovery-server default).
- `EUREKA_PASSWORD` has **no default** — `docker-compose up` will fail with a clear error if it is not set. This is intentional to prevent accidental startup with missing credentials.
- The `discovery-server` services in `docker-compose.yml` also read `EUREKA_USERNAME` and `EUREKA_PASSWORD` from the same environment, so a single `.env` file configures everything consistently.
