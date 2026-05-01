# NFR Requirements — Transfer Service

Same baseline as Account Service (see account-service/nfr-requirements/) with these differences:

## Performance
- Transfer initiation p95 < 300ms (includes REST call to Account Service)
- Transfer throughput: 50 req/s (lower than account queries)

## Resilience (additional)
- **Circuit Breaker**: Required for REST calls to Account Service (Resilience4j or Spring Retry)
- **Retry**: Kafka consumer retries on transient failures (3 attempts, exponential backoff)
- **Timeout**: REST client timeout 5s to Account Service

## Security — Same as Account Service
- SECURITY-01 through SECURITY-15 applicability identical

## PBT — Same framework (jqwik), 4 testable properties identified

## Tech Stack — Same as Account Service plus:
- Spring WebClient or RestClient for outbound REST calls to Account Service
- Resilience4j for circuit breaker (optional, can use Spring Retry initially)
