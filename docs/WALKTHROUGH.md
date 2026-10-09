# Walkthrough — how this project was built

The project grew in eight phases, from a beginner Spring Boot app to a containerized microservice system running on Kubernetes. Each phase below covers **what** was added, **why**, what **went wrong** and how it was found, and the **talking points** worth bringing to an interview.

All numbers come from real runs: local runs, or GitHub Actions jobs (the CI annotations of each run).

| Phase | Theme | Main result |
| --- | --- | --- |
| [1](#phase-1--clean-core) | Clean core | Layered API with DTOs, correct status codes, 11 tests |
| [2](#phase-2--real-database) | Real database | Flyway, PostgreSQL profile, N+1 shown and fixed (5 queries → 1) |
| [3](#phase-3--security) | Security | Stateless JWT, BCrypt, roles, owner-only actions |
| [4](#phase-4--docker) | Docker | Multi-stage, layered, non-root image; Compose with PostgreSQL |
| [5](#phase-5--microservices) | Microservices | Gateway, Eureka, Feign, circuit breaker, database per service |
| [6](#phase-6--production-readiness) | Production readiness | CI, central config, tracing, metrics, caching, Kafka events |
| [7](#phase-7--everything-in-containers) | Everything in containers | 9-container stack, real Kafka, Testcontainers, end-to-end test in CI |
| [8](#phase-8--kubernetes) | Kubernetes | Helm chart, probes, HPA, deployed on kind in CI |

---

## Phase 1 — Clean core

**What**: Controller → Service → Repository layers, Java records as DTOs (`UserRequest`, `UserResponse`), constructor injection, `@RestControllerAdvice` error handling, pagination and sorting with `PagedModel`.

**Bugs found in the starting code**:
- `@PathVariable String userId` on a `{id}` path, so delete always failed.
- `ResponseEntity.created(...)` built and then thrown away, so create returned 200 instead of 201.
- `ErrorDetails` constructor arguments swapped (message and details).
- The catch-all error handler leaked internal exception messages to clients.

**Why it matters**: separating HTTP, business logic and persistence makes each testable on its own. DTOs keep the database model out of the API contract: no mass assignment, no lazy-loading surprises.

**Talking points**: constructor vs field injection; why not expose entities; 201 + `Location`; `@WebMvcTest` vs `@DataJpaTest` vs `@SpringBootTest`.

## Phase 2 — Real database

**What**:
- `dev` (H2) and `prod` (PostgreSQL) profiles, with secrets from environment variables.
- Flyway migrations replacing `data.sql`, and `ddl-auto=validate`.
- `User 1 —* Post`.
- `open-in-view=false`.
- A transactional bulk insert.

**Measured**: the naive feed ran **5 SQL statements** for 6 posts by 4 authors, and `JOIN FETCH` / `@EntityGraph` ran **1**. A test counts statements with Hibernate statistics, so an N+1 regression fails the build.

**Went wrong**: after deleting `data.sql`, the old copy in `target/` still ran next to Flyway (8 users instead of 4). Lesson: `mvnw clean` after removing resources.

**Talking points**: N+1 and its three fixes; lazy vs eager defaults; why Flyway over `ddl-auto=update`; rollback rules (unchecked vs checked exceptions); the self-invocation trap of `@Transactional`.

## Phase 3 — Security

**What**:
- A stateless `SecurityFilterChain`.
- Register and login endpoints issuing an HS256 JWT.
- A hand-written `JwtAuthenticationFilter`.
- BCrypt password hashes in a Flyway migration.
- `USER`/`ADMIN` roles, and `@PreAuthorize("hasRole('ADMIN') or #id == principal.id")` for owner-only actions.
- JSON 401/403 responses, CORS, and a Swagger "Authorize" button.

**Caught**:
- `AccessDeniedException` from `@PreAuthorize` is thrown inside the controller, so the catch-all handler would have turned it into a 500. It's mapped to 403 explicitly.
- The JWT filter is deliberately not a `@Component`, so Spring Boot doesn't register it a second time as a plain servlet filter.

**Talking points**: authentication vs authorization; what's inside a JWT (encoded, not encrypted); 401 vs 403; why CSRF is off for a header-token API; HS256 vs RS256; BCrypt's salt and cost.

## Phase 4 — Docker

**What**:
- A multi-stage Dockerfile: JDK build stage, JRE runtime stage, non-root user, `MaxRAMPercentage`.
- Spring Boot layered jars: dependencies 66 MB, application 231 KB.
- Compose with PostgreSQL, a health check and `depends_on: service_healthy`.

**Verified**:
- The `prod` profile ran against PostgreSQL first through an embedded PostgreSQL (no Docker on the dev machine).
- The image itself was verified later in CI.

**Talking points**: image vs container; layer caching and Dockerfile order; exec-form `ENTRYPOINT` and `SIGTERM`; JVM memory in containers; volumes; secrets via `.env`.

## Phase 5 — Microservices

**What**:
- `user-service` (the original app), a new `comment-service` with its own database, `api-gateway` (Spring Cloud Gateway), and `discovery-server` (Eureka).
- comment-service checks posts through an OpenFeign client wrapped in a Resilience4j circuit breaker.
- comment-service validates user-service's JWTs with Spring's OAuth2 Resource Server.

**Measured**:
- With user-service stopped, the first comment write took **1.03 s** (connect timeout). After the breaker opened, writes failed in **13–29 ms** without any network call, while reads still answered with `postDetailsAvailable: false`.
- After a restart it recovered on its own (OPEN → HALF_OPEN → CLOSED).

**Version decision**: no stable Spring Cloud release train targeted Boot 4.1 yet. 2025.1.3 (built for Boot 4.0) was verified to run on 4.1.1.

**Talking points**: client-side discovery and its cache delays; what a gateway is for; circuit breaker states; why a 404 must not trip the breaker; database per service and what it costs (no joins, no cross-service transactions).

## Phase 6 — Production readiness

**What**:
- GitHub Actions CI.
- A Config Server with live `/actuator/refresh`.
- Micrometer Tracing + Zipkin.
- Prometheus metrics plus custom counters.
- A Caffeine cache with eviction.
- A Kafka `posts-deleted` event sent after commit (`@TransactionalEventListener(AFTER_COMMIT)`).

**Went wrong, and fixed**:
- **Traces split in two**: the circuit breaker runs calls on another thread, so the ThreadLocal trace context was lost. Fixed with `ContextExecutorService`.
- **`/actuator/refresh` returned 500**: it tried to re-create the Eureka client. Fixed with `eureka.client.refresh.enable=false`.
- **A counter named `comments.created` appeared as `comments_total`**: Prometheus reserves the `_created` suffix. Renamed to `comments.saved`.
- **23 slice tests failed**: `@EnableCaching` sat on the main class. Moved to its own `@Configuration`.
- **CI took five runs to go green**:
  - `mvnw` lacked the executable bit (exit 126).
  - Docker Hub auth returned 504s; images were moved to the ECR Public mirror.
  - `| tee` hid a failure until `pipefail` was enabled.

**Talking points**: the three pillars (logs, metrics, traces); sampling; counter / gauge / timer; cache invalidation; local vs distributed cache; at-least-once delivery and idempotent consumers; outbox pattern.

## Phase 7 — Everything in containers

**What**:
- One parameterized Dockerfile for all five services (`ARG SERVICE`, BuildKit cache mount for `~/.m2`).
- A 9-container Compose file: two PostgreSQL, Kafka in KRaft mode, Zipkin; health checks via bash `/dev/tcp`; memory limits.
- comment-service on its own PostgreSQL.
- A Testcontainers test.
- `scripts/e2e.sh` run by CI.

**Measured in CI**:
- All **7 end-to-end checks** passed.
- Deleting a user removed the comments on their posts **within 3 s** through real Kafka.
- One Zipkin trace spanned all three services.
- The stack used about **2.3 GiB** of memory.
- `RealInfrastructureContainersTest` ran (not skipped) against PostgreSQL 17 and Kafka 4.3.1.

**Went wrong**: parallel image pulls hit ECR Public's anonymous rate limit (`toomanyrequests`). Fixed by pulling one by one with retries.

**Talking points**: `depends_on` with health conditions; KRaft and advertised listeners; Testcontainers vs embedded; what an end-to-end test should cover; why Compose isn't production orchestration.

## Phase 8 — Kubernetes

**What**:
- A Helm chart (`helm/microservices`) with one template for all five Spring services.
- StatefulSets for PostgreSQL and Kafka.
- Startup / liveness / readiness probes on Spring Boot's health groups.
- CPU and memory requests, memory limits, `preStop` delay, init containers waiting for each database.
- A Secret, a ConfigMap for `config-repo/`, and an HPA for comment-service.
- A CI job on a kind cluster.

**Found before the first deploy**:
- **Probes would have got 401**: security allowed only `/actuator/health`. Fixed with `HealthProbesTest`.
- **`runAsNonRoot` needs a numeric UID**: the image now runs as UID 10001.
- **A config checksum annotation would have restarted every service** on a config edit; it's now limited to config-server.

**First CI run on kind**:
- `helm install --wait` was ready in **33 s**: 9/9 pods running, 0 restarts.
- The HPA read `cpu: 72%/70%`.
- The end-to-end Kafka check passed.
- **No traces reached Zipkin**. Kubernetes has no startup order, so the services started before config-server, and the `optional:` config import let them run silently without the shared tracing settings. On Kubernetes the import is now mandatory with fail-fast, so pods exit and are restarted until config-server answers.

**Second CI run on kind (after the fix), all green**:
- Ready after **45 s**.
- api-gateway shows **2 restarts**: it failed fast until config-server answered, then started with the full config. That's the intended behaviour.
- All **7 end-to-end checks** passed, including the trace across three services in Zipkin and the Kafka clean-up (within 4 s).
- The **HPA scaled comment-service to 2 pods** during the start-up CPU peak, then settled at `cpu: 10%/70%`.

**Rolling restart under traffic, a measured weakness**: requests through the gateway every 0.2 s while user-service was replaced gave **139 ok, 32 failed**. Kubernetes did its part (`maxUnavailable: 0`, the new pod ready first). The failures come from **Eureka**: the gateway kept routing to the old pod's IP from its 30-second registry cache after the pod was gone. Calling the Kubernetes Service name directly (Kubernetes-native discovery), or shortening Eureka's caches and adding retries in the gateway, are the fixes. This is the strongest argument in the project for dropping Eureka on Kubernetes.

**Talking points**: Deployment vs StatefulSet; liveness vs readiness vs startup; requests vs limits for JVMs; rolling updates with `maxSurge`/`maxUnavailable`; how the HPA computes replicas (and its 10% tolerance); Secrets aren't encryption; Eureka vs Kubernetes-native discovery.

---

## Afterwards — multi-module restructure

**Problem**: the original app was the repo root (`pom.xml` + `src/`), and the four services added later lived *inside* its folder as unrelated Maven projects. IntelliJ imported only the root pom, so `comment-service/`, `api-gateway/` and the rest were plain folders: no source roots, unresolved imports, no run buttons. The Boot, Java and Spring Cloud versions were also copied into five poms.

**Fix**: a standard multi-module build.

- The root `pom.xml` is now a parent with `<packaging>pom</packaging>`. It lists the five `<modules>` and holds the shared versions (`spring-cloud.version`, JJWT, springdoc) and test settings once.
- The original app moved to `user-service/` with `git mv`, so its history is kept. The artifact `first-project` became `user-service`.
- Every service pom inherits from the parent and lists only its own dependencies.
- `./mvnw verify` builds and tests all 56 tests in one reactor; `./mvnw -pl <module> ...` targets one module.
- The Dockerfile copies the parent pom too (children find it through `../pom.xml`) and defaults to `SERVICE=user-service`. The CI matrix uses `-pl <module>`.

**Cleanups done at the same time**:
- The Phase 1 in-memory `UserResources` + `UserDAO` were removed; the JPA version replaced them long ago.
- The JPA entities (`User`, `Post`, `Role`) moved from `bean` to `entity`.
- The leftover `HELP.md` was deleted.
- The Java package name `com.practice.first_project` was kept: renaming it would touch every file for no functional gain.

---

## What is not done yet

These are the next steps for a senior-level showcase:

- **Identity provider** (Keycloak) with RS256 and JWKS instead of a shared HS256 secret.
- **Transactional outbox**, so events survive a crash between commit and send.
- **Prometheus + Grafana** dashboards and alerts (only the metrics endpoints exist).
- **Quality and security gates**: JaCoCo coverage, SonarQube, Trivy image scanning, Dependabot.
- **Load test** (k6) that also exercises the HPA.
- **Kubernetes-native discovery** (drop Eureka), which would remove the 32 failed requests seen during a rolling restart; also Ingress and PodDisruptionBudgets.
- **Architecture decision records** for the main trade-offs.
