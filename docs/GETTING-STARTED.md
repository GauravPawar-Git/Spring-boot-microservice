# Getting started — run it, create data, test it

A step-by-step guide for a first run on your own machine, with no Docker needed. You start the services in the right order, create users, posts and comments, and see each feature work. Every command and output below comes from a real run on Windows (Git Bash).

For Docker Compose, Kubernetes, health checks and troubleshooting, see the [runbook](RUNBOOK.md).

**Contents**
1. [Prepare (once)](#1-prepare-once)
2. [Build](#2-build)
3. [Start the services in order](#3-start-the-services-in-order) — with a script, or by hand
4. [Create and test data automatically](#4-create-and-test-data-automatically)
5. [Create and test data by hand](#5-create-and-test-data-by-hand) — curl or Swagger
6. [Watch it work](#6-watch-it-work)
7. [Stop, reset, start again](#7-stop-reset-start-again)

---

## 1. Prepare (once)

You need **JDK 25** and **Git Bash** (comes with Git for Windows). Open Git Bash in the project folder:

```bash
cd /d/Gaurav/Spring_boot_projects/first-project
export JAVA_HOME="C:/Users/maahi/.jdks/jbr-25.0.4.1"    # any JDK 25; only needed if java isn't on your PATH
"$JAVA_HOME/bin/java" -version                           # should say 25
```

Optional, for tracing: download [zipkin-server-3.6.1-exec.jar](https://repo1.maven.org/maven2/io/zipkin/zipkin-server/3.6.1/zipkin-server-3.6.1-exec.jar) once, into the project folder.

## 2. Build

```bash
./mvnw -q package -DskipTests
```

This builds all five services (about 1 minute the first time) into:

```
user-service/target/user-service-0.0.1-SNAPSHOT.jar
comment-service/target/comment-service-0.0.1-SNAPSHOT.jar
api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar
config-server/target/config-server-0.0.1-SNAPSHOT.jar
```

Rebuild after changing code. On Windows, stop the services first (step 7), because a running service locks its jar.

## 3. Start the services in order

### The order, and why

| # | Service | Port | Must start after | Because |
| --- | --- | --- | --- | --- |
| 0 | Zipkin (optional) | 9411 | — | receives traces; nothing depends on it |
| 1 | config-server | 8888 | — | the services read their shared settings from it at startup |
| 2 | discovery-server (Eureka) | 8761 | — | the services register here and look each other up |
| 3 | user-service | 8080 | 1, 2 | |
| 4 | comment-service | 8082 | 1, 2 | calls user-service through Eureka |
| 5 | api-gateway | 8765 | 1, 2 | routes to user- and comment-service through Eureka |

### Option A: one script (recommended)

```bash
ZIPKIN_JAR=zipkin-server-3.6.1-exec.jar ./scripts/start-local.sh     # or just ./scripts/start-local.sh without Zipkin
```

It starts each service in the background, waits until it's healthy before starting the next, and waits until the whole chain works. Real output:

```text
1/3 Infrastructure
  starting zipkin            up
  starting config-server     up
  starting discovery-server  up
2/3 Services (they read their settings from config-server and register with Eureka)
  starting user-service      up
  starting comment-service   up
  starting api-gateway       up
3/3 Waiting until gateway -> comment-service -> user-service works (up to ~2 minutes)
  all services can reach each other

All services are up. Entry point: http://localhost:8765
```

This takes 1–3 minutes. Most of it is step 3/3: the services discover each other through Eureka, whose caches refresh every 30 s. Logs are in `.run/logs/<service>.log`.

### Option B: by hand, one terminal per service

Open six Git Bash windows in the project folder (set `JAVA_HOME` in each) and start them in this order. Wait for the **"Started ..."** line in each window before starting the next.

```bash
java -jar zipkin-server-3.6.1-exec.jar                                  # optional
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar         # wait for: Started ConfigServerApplication
java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar   # wait for: Started DiscoveryServerApplication
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar           # wait for: Started FirstProjectApplication
java -jar comment-service/target/comment-service-0.0.1-SNAPSHOT.jar     # wait for: Started CommentServiceApplication
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar             # wait for: Started ApiGatewayApplication
```

Then **wait about 30–60 seconds**. Open http://localhost:8761: Eureka should list `API-GATEWAY`, `COMMENT-SERVICE` and `USER-SERVICE`.

In IntelliJ you can do the same by running each module's `*Application` class in the order above.

### Is everything ready?

```bash
curl -s "localhost:8765/comments?postId=1"
```

Ready when the answer contains `"postDetailsAvailable":true`: the gateway reached comment-service, which reached user-service. Before that, you'll see a `503` from the gateway or `"postDetailsAvailable":false`. That's normal during the first minute.

## 4. Create and test data automatically

```bash
./scripts/demo.sh
```

It registers a new user, creates posts and comments, and tests the rules, errors and security. Only bash and curl are needed. Real output:

```text
== 1. Register a new user (demo_88207) and log in
   OK    register -> 201
   info  created: {"id":5,"name":"Demo User","birthDate":"1998-04-12","age":28}
   OK    login -> got a JWT (167 characters)
   OK    GET /auth/me as demo_88207 -> 200

== 2. Create posts (stored by user-service)
   OK    create post -> 201
   OK    bulk create 2 posts (one transaction) -> 201
   info  demo_88207 now has 3 posts

== 3. Comment on the post (comment-service checks the post via Feign -> user-service)
   OK    vigo comments on post 7 -> 201
   OK    demo_88207 replies -> 201
   info  comments: 2, post details from user-service: postAuthor=Demo User

== 4. Validation and business rules
   OK    comment on a post that doesn't exist -> 404
   OK    banned word (rule served by config-server) -> 400
   OK    empty post description -> 400
   OK    10 more posts would exceed the limit of 10 -> 409
   info  still 3 posts: the whole bulk insert was rolled back

== 5. Security
   OK    create a post without a token -> 401
   OK    post as another user -> 403
   OK    USER deletes a user -> 403

== 6. Admin deletes demo_88207: their posts go too; Kafka tells comment-service to delete the comments
   OK    ADMIN deletes user 5 -> 204
   info  comments on post 7 still there (2): Kafka events are off in local mode ...

Demo finished: every check passed.
```

Step 6 shows a real difference between setups. Locally there's no Kafka, so the comments of the deleted user's post remain. With Docker Compose or Kubernetes, the `posts-deleted` event removes them within seconds. The script works against those setups too: `GW=http://localhost:8765 ./scripts/demo.sh`.

## 5. Create and test data by hand

### Data that already exists

The database starts with 4 users and 6 posts (Flyway migration `V3__seed_data.sql`). Every seed user's password is `password123`:

| Username | Role | Posts |
| --- | --- | --- |
| `rambo` | ADMIN | 1, 2 |
| `shamlal` | USER | 3 |
| `vigo` | USER | 4 |
| `diga` | USER | 5, 6 |

In local mode each service uses an in-memory H2 database. **Restarting a service resets its data** to these seeds; comments start empty.

### With curl

Paste these into Git Bash one block at a time. Everything goes through the gateway on port 8765.

**Log in and keep the tokens**

```bash
GW=localhost:8765
login() { curl -s -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}" $GW/auth/login | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4; }
ADMIN=$(login rambo password123)
VIGO=$(login vigo password123)
echo ${#ADMIN} ${#VIGO}          # two lengths (about 160 characters each) = two tokens
```

**Register your own user**

```bash
curl -s -H 'Content-Type: application/json' $GW/auth/register \
  -d '{"name":"Gaurav","birthDate":"1998-04-12","username":"gaurav","password":"my-password-1"}'
# {"id":5,"name":"Gaurav","birthDate":"1998-04-12","age":28}

ME=$(login gaurav my-password-1)
curl -s -H "Authorization: Bearer $ME" $GW/auth/me
```

**Create a post** (use your own id from the register answer)

```bash
curl -s -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' \
  -d '{"description":"Learning microservices"}' $GW/jpa/users/5/posts
# {"id":7,"description":"Learning microservices",...,"authorId":5,"authorName":"Gaurav"}

curl -s $GW/jpa/users/5/posts         # your posts (public)
curl -s $GW/jpa/posts                 # the whole feed
```

**Comment on it** (comment-service asks user-service whether post 7 exists)

```bash
curl -s -H "Authorization: Bearer $VIGO" -H 'Content-Type: application/json' \
  -d '{"postId":7,"text":"Great post!"}' $GW/comments
curl -s "$GW/comments?postId=7"       # comments + "postDetailsAvailable":true, "postAuthor":"Gaurav"
```

**Try the rules**

| Try | Command | Expect |
| --- | --- | --- |
| Post that doesn't exist | `curl -s -H "Authorization: Bearer $VIGO" -H 'Content-Type: application/json' -d '{"postId":999,"text":"hi"}' $GW/comments` | `404` Post not found |
| Banned word | same, with `"text":"easy scam"` | `400` banned word: scam |
| No token | `curl -s -o /dev/null -w '%{http_code}\n' -X POST -H 'Content-Type: application/json' -d '{"description":"x"}' $GW/jpa/users/5/posts` | `401` |
| Post as someone else | `curl -s -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' -d '{"description":"x"}' $GW/jpa/users/1/posts` | `403` |
| USER deletes a user | `curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $ME" $GW/jpa/users/2` | `403` |
| ADMIN deletes you | `curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $ADMIN" $GW/jpa/users/5` | `204`; your posts are gone too |

### With Swagger UI (user-service only)

1. Open http://localhost:8080/swagger-ui.html.
2. `POST /auth/login` → **Try it out** → `{"username":"rambo","password":"password123"}` → **Execute** → copy `accessToken`.
3. Click **Authorize** (top right), paste the token, then **Authorize**.
4. Every user and post endpoint can now be called from the page. Comments live in comment-service, so use curl for those.

## 6. Watch it work

| What | How |
| --- | --- |
| Services registered | http://localhost:8761 |
| A request across 3 services | Make a comment, then open http://localhost:9411 → **Run query** → newest trace (needs Zipkin running) |
| Circuit breaker and fallback | Stop user-service (close its window, or find its PID with `netstat -ano \| grep :8080` and run `taskkill //PID <pid> //F`). Then `curl -s -w ' %{http_code}\n' localhost:8765/jpa/posts` returns `503 user-service is unavailable` and comment reads still answer with `"postDetailsAvailable":false`. Start it again and both recover on their own. |
| Live config change | Edit `config-repo/comment-service.properties` (add a word to `app.comments.banned-words`), then `curl -X POST -H "Authorization: Bearer $ADMIN" localhost:8082/actuator/refresh` |
| Metrics | http://localhost:8082/actuator/prometheus (search `comments_saved_total`) |
| Logs | `.run/logs/<service>.log` when started with the script |

## 7. Stop, reset, start again

```bash
./scripts/stop-local.sh       # stops all six by port; works for script- and hand-started services
```

```text
  stopped api-gateway (port 8765, pid 39072)
  stopped comment-service (port 8082, pid 47192)
  stopped user-service (port 8080, pid 49496)
  stopped discovery-server (port 8761, pid 48292)
  stopped config-server (port 8888, pid 27488)
  stopped zipkin (port 9411, pid 49460)
```

Starting again gives fresh seed data, because H2 lives in memory. To keep data between runs, use Docker Compose (PostgreSQL volumes). See the [runbook](RUNBOOK.md#option-c--the-full-stack-with-docker-compose).
