# Runbook — running and checking the system

How to start the project in each supported way, how to check that every service is healthy, and how to fix the problems that actually came up while building it.

- [Prerequisites](#prerequisites) (and [opening it in IntelliJ](#opening-the-project-in-intellij-idea))
- [Ports and URLs](#ports-and-urls)
- [Option A — one service, no setup](#option-a--one-service-no-setup)
- [Option B — all services as jars (no Docker)](#option-b--all-services-as-jars-no-docker)
- [Option C — the full stack with Docker Compose](#option-c--the-full-stack-with-docker-compose)
- [Option D — Kubernetes with Helm](#option-d--kubernetes-with-helm)
- [Checking the services](#checking-the-services)
- [Try the main flows](#try-the-main-flows)
- [Running the tests](#running-the-tests)
- [Troubleshooting](#troubleshooting)

## Prerequisites

| Tool | Needed for | Check |
| --- | --- | --- |
| JDK 25 | everything | `java -version` (or set `JAVA_HOME`) |
| Git Bash (Windows) or any bash | the commands below, `scripts/e2e.sh` | `bash --version` |
| curl, jq | sample requests, `scripts/e2e.sh` | `jq --version` |
| Docker Desktop (with WSL 2 on Windows) | options C and D, Testcontainers tests | `docker version` |
| kind, kubectl, Helm 3+ | option D | `kind version`, `helm version` |

Maven is not needed: every command uses the wrapper `./mvnw` from the repo root. The root `pom.xml` is the parent of the five modules, so `./mvnw <goal>` runs on all of them and `-pl <module>` picks one.

On Windows, if `java` is not on the PATH, point `JAVA_HOME` at a JDK 25 first, for example `export JAVA_HOME="C:/Users/<you>/.jdks/jbr-25.0.4.1"` in Git Bash.

### Opening the project in IntelliJ IDEA

Open the repo folder (or **File → Open → `pom.xml` at the root → Open as Project**). IntelliJ reads the parent pom and imports all five modules, each with its sources, dependencies and a runnable `*Application` class.

After pulling the multi-module restructure into an existing checkout: open the **Maven** tool window and click **Reload All Maven Projects**. If modules still show as plain folders, close the project, delete the `.idea` folder, and open the root `pom.xml` again.

## Ports and URLs

| Component | Port | Useful URL |
| --- | --- | --- |
| **api-gateway** (the entry point) | 8765 | http://localhost:8765/jpa/posts |
| user-service | 8080 | http://localhost:8080/swagger-ui.html |
| comment-service | 8082 | http://localhost:8082/actuator/health |
| discovery-server (Eureka) | 8761 | http://localhost:8761 |
| config-server | 8888 | http://localhost:8888/comment-service/default |
| Zipkin | 9411 | http://localhost:9411 |
| PostgreSQL ×2, Kafka | internal only | reachable from inside Docker / the cluster |

Seed accounts (password `password123` for all): `rambo` (ADMIN), `shamlal`, `vigo`, `diga` (USER).

## Option A — one service, no setup

user-service alone with an in-memory H2 database. The quickest way to try the API, security, JPA and Swagger.

```bash
./mvnw -pl user-service spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html → `POST /auth/login` → copy `accessToken` → **Authorize**.
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, user `sa`, empty password).
- It logs "cannot reach Eureka / config server" warnings: harmless, both are optional.

## Option B — all services as jars (no Docker)

Six processes, each in its own terminal. Kafka is off in this mode (no broker), everything else works.

**1. Build all five projects** (once, and after code changes; stop running services first on Windows, see [Troubleshooting](#troubleshooting)):

```bash
./mvnw -q package -DskipTests        # builds all five modules
```

**2. Start them in this order** (Zipkin is optional; download [zipkin-server-3.6.1-exec.jar](https://repo1.maven.org/maven2/io/zipkin/zipkin-server/3.6.1/zipkin-server-3.6.1-exec.jar) once):

```bash
java -jar zipkin-server-3.6.1-exec.jar                                   # :9411 (optional)
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar          # :8888  start before the others
java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar    # :8761
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar            # :8080
java -jar comment-service/target/comment-service-0.0.1-SNAPSHOT.jar      # :8082
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar              # :8765
```

**3. Wait ~30 seconds** after the last one starts: Eureka clients and the gateway refresh their registry caches every 30 s. Requests sent earlier may get `503` / "No servers available".

## Option C — the full stack with Docker Compose

Nine containers: the five services, a PostgreSQL per business service, Kafka (KRaft) and Zipkin. Kafka events are on. This is what CI runs on every push.

```bash
cp .env.example .env                    # then set DB_PASSWORD and JWT_SECRET in .env
head -c 32 /dev/urandom | base64        # a good JWT_SECRET
docker compose up -d --build --wait     # builds 5 images, starts 9 containers, waits for all health checks
./scripts/e2e.sh                        # 7 end-to-end checks
```

Expected end of the e2e output:

```text
PASS  comments on post 5 removed by the Kafka event within 3s
PASS  Zipkin has a single trace across api-gateway, comment-service and user-service
All end-to-end checks passed.
```

| Command | Does |
| --- | --- |
| `docker compose ps` | State and health of every container |
| `docker compose logs -f comment-service` | Follow one service |
| `docker compose up -d --build user-service` | Rebuild and restart one service after a change |
| `docker compose exec users-db psql -U postgres -d users` | SQL shell in user-service's database |
| `docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic posts-deleted --from-beginning` | Watch the events |
| `docker compose down -v` | Stop everything and delete the database volumes |

Needs about 2.5 GB of RAM for Docker (2.3 GiB measured in CI).

## Option D — Kubernetes with Helm

The same images on a local [kind](https://kind.sigs.k8s.io/) cluster, installed with the chart in `helm/microservices`. These are the steps the CI `Kubernetes (kind + Helm)` job runs.

```bash
# 1. images
docker build -t ms/user-service:ci .            # SERVICE defaults to user-service
for s in comment-service api-gateway discovery-server config-server; do docker build --build-arg SERVICE=$s -t ms/$s:ci .; done

# 2. cluster + images inside it (no registry needed)
kind create cluster --name ms
for i in ms/user-service:ci ms/comment-service:ci ms/api-gateway:ci ms/discovery-server:ci ms/config-server:ci \
         public.ecr.aws/docker/library/postgres:17-alpine apache/kafka:4.3.1 ghcr.io/openzipkin/zipkin:3.6.1; do
  docker pull -q $i 2>/dev/null; kind load docker-image $i --name ms
done

# 3. optional: metrics for the HorizontalPodAutoscaler
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deployment metrics-server -n kube-system --type=json \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

# 4. install (secrets on the command line, config-repo files from the repo)
helm upgrade --install ms helm/microservices \
  --set imageTag=ci \
  --set secrets.dbPassword=$(openssl rand -hex 16) \
  --set secrets.jwtSecret=$(openssl rand -base64 32) \
  --set-file configRepo.application=config-repo/application.properties \
  --set-file configRepo.commentService=config-repo/comment-service.properties \
  --wait --timeout 10m

# 5. reach it and test it
kubectl port-forward svc/api-gateway 8765:8765 &
kubectl port-forward svc/discovery-server 8761:8761 &
kubectl port-forward svc/zipkin 9411:9411 &
./scripts/e2e.sh
```

| Command | Does |
| --- | --- |
| `kubectl get pods` | All 9 pods should be `1/1 Running` |
| `kubectl get hpa` | comment-service autoscaler and its CPU reading |
| `kubectl top pods` | CPU and memory per pod (needs metrics-server) |
| `kubectl logs deploy/comment-service -f` | Follow a service |
| `kubectl rollout restart deployment/user-service` | Rolling restart |
| `helm history ms` / `helm rollback ms 1` | Release history / roll back |
| `helm uninstall ms && kind delete cluster --name ms` | Remove everything |

With Docker Desktop's built-in Kubernetes instead of kind, skip step 2's `kind load` (Docker Desktop's cluster sees locally built images) and run the rest unchanged.

## Checking the services

Work through this list after any start-up (options B–D).

| Check | Command | Healthy result |
| --- | --- | --- |
| Each service is up | `curl localhost:8080/actuator/health` (and 8082, 8765, 8888) | `{"status":"UP"...}` |
| Kubernetes probe groups | `curl localhost:8080/actuator/health/liveness` | `{"status":"UP"}` (no token needed) |
| Everyone registered | open http://localhost:8761 | `API-GATEWAY`, `COMMENT-SERVICE`, `USER-SERVICE` listed |
| Gateway routes | `curl localhost:8765/actuator/gateway/routes` | routes `user-service` and `comment-service` |
| Config server serves files | `curl localhost:8888/comment-service/default` | contains `app.comments.banned-words` |
| Services loaded central config | user-service / comment-service log at start-up | `Located environment: name=..., profiles=[...]` |
| Tracing | make a comment through :8765, open Zipkin → **Run query** | newest trace shows 3 services |
| Metrics | `curl localhost:8082/actuator/prometheus \| grep comments_` | `comments_saved_total`, `comments_rejected_total` |
| Cache | `curl localhost:8080/actuator/prometheus \| grep cache_gets_total` | hit / miss counters for cache `posts` |
| Kafka (options C, D) | delete a user, then read their posts' comments | comments gone within seconds |

## Try the main flows

All through the gateway. In Git Bash:

```bash
GW=localhost:8765
login() { curl -s -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"password123\"}" $GW/auth/login | jq -r .accessToken; }
VIGO=$(login vigo); ADMIN=$(login rambo)

# sync call: comment-service asks user-service (Feign + circuit breaker) whether post 5 exists
curl -s -H "Authorization: Bearer $VIGO" -H 'Content-Type: application/json' \
  -d '{"postId":5,"text":"Nice post"}' $GW/comments | jq

# read comments with post details from user-service
curl -s "$GW/comments?postId=5" | jq

# security: a USER can't delete users (403), an ADMIN can (204)
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $VIGO"  $GW/jpa/users/4
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $ADMIN" $GW/jpa/users/4

# async (options C, D): the posts-deleted event removed the comments on Diga's posts
curl -s "$GW/comments?postId=5" | jq '.comments | length'      # 0

# content rule from the config server
curl -s -H "Authorization: Bearer $VIGO" -H 'Content-Type: application/json' \
  -d '{"postId":1,"text":"easy scam"}' $GW/comments | jq .message  # "Comment contains a banned word: scam"
```

**Circuit breaker**: stop user-service, then `GET $GW/comments?postId=1` still answers (with `postDetailsAvailable: false`) and new comments get `503` in milliseconds. Start user-service again and the breaker closes by itself.

**Live config change**: edit `config-repo/comment-service.properties`, then
`curl -X POST -H "Authorization: Bearer $ADMIN" localhost:8082/actuator/refresh` → `["app.comments.banned-words"]`.

## Running the tests

```bash
./mvnw verify                          # all modules: 56 tests (user-service 41, comment-service 14, config-server 1)
./mvnw -pl comment-service verify      # one module
```

`RealInfrastructureContainersTest` uses Testcontainers (real PostgreSQL 17 + Kafka). Without Docker it is reported as **skipped**, not failed.

## Troubleshooting

Every entry below happened while building this project.

| Symptom | Cause | Fix |
| --- | --- | --- |
| `java: command not found` | JDK not on the PATH | set `JAVA_HOME` (see Prerequisites) |
| Maven: `Unable to rename ... .jar.original` | Windows locks the jar of a running service | stop that service, then build |
| 8 users instead of 4 after deleting a resource file | Maven doesn't remove deleted files from `target/` | `./mvnw clean ...` |
| `503` / "No servers available for service" right after start-up | Eureka and load-balancer caches refresh every 30 s | wait ~30 s |
| Comment-service traces start a new trace at the Feign call | circuit-breaker threads lose the trace context | fixed (`ContextExecutorService` in `CircuitBreakerConfig`) |
| Pods never become ready on Kubernetes, probes get `401` | security only allowed `/actuator/health` | fixed (`/actuator/health/**` permitted) |
| Pod refused: "image has non-numeric user" | `runAsNonRoot` needs a numeric UID | fixed (image runs as UID 10001) |
| No traces in Zipkin on Kubernetes | services started before config-server and skipped the optional import | fixed: config import is mandatory with fail-fast on Kubernetes; pods restart until config-server answers |
| A few `5xx` through the gateway during `kubectl rollout restart` (CI: 32 of 171 requests) | the gateway routes from Eureka's cached registry, which still lists the old pod for up to ~30 s | known limitation; see the walkthrough (Phase 8). Kubernetes-native discovery removes it |
| api-gateway shows 1–2 restarts right after `helm install` | it starts before config-server and fails fast on purpose | none needed: Kubernetes restarts it until config-server answers |
| `toomanyrequests: Rate exceeded` or `504` pulling images | registry rate limits / outages | retry; CI pulls images one by one with backoff |
| `./mvnw: Permission denied` (exit 126) on Linux | file committed from Windows without the executable bit | `git update-index --chmod=+x mvnw` |
| Port already in use | an earlier run is still alive | stop it (`netstat -ano \| findstr :8080` on Windows) |
