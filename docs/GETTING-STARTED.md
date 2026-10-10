# Getting started — run it, create data, test it

A step-by-step guide for a first run on your own machine, with no Docker needed. You start the services in the right order, create users, posts and comments, and see each feature work. Every command and output below comes from a real run on Windows (Git Bash).

For Docker Compose, Kubernetes, health checks and troubleshooting, see the [runbook](RUNBOOK.md).

**Contents**
1. [Prepare (once)](#1-prepare-once)
2. [Build](#2-build)
3. [Start the services in order](#3-start-the-services-in-order) — with a script, or by hand
4. [Create sample data automatically](#4-create-sample-data-automatically) — `seed-data.sh` (keeps data) and `demo.sh` (tests features)
5. [Create data by hand: sample request bodies](#5-create-data-by-hand-sample-request-bodies) — every endpoint, with curl, PowerShell, IntelliJ or Swagger
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

## 4. Create sample data automatically

Two scripts, both only need bash and curl. Run them in Git Bash while the services are up.

### 4a. `seed-data.sh`: fill the database and keep the data

```bash
./scripts/seed-data.sh
```

It registers alice, bob and carol (password `password123`), creates posts and comments for them, and leaves everything in place so you can explore it. Real output:

```text
Seeding sample data through http://localhost:8765

== 1. Register three users (public endpoint, each gets a login)
   OK    register alice                         POST /auth/register -> 201
   OK    register bob                           POST /auth/register -> 201
   OK    register carol                         POST /auth/register -> 201

== 2. Profiles
   OK    carol renames herself                  PUT /jpa/users/7 -> 200
   OK    admin creates a user without login     POST /jpa/users -> 201

== 3. Posts
   OK    alice posts                            POST /jpa/users/5/posts -> 201
   OK    alice bulk-posts 2                     POST /jpa/users/5/posts/bulk -> 201
   OK    bob posts                              POST /jpa/users/6/posts -> 201
   OK    bob posts again                        POST /jpa/users/6/posts -> 201
   OK    carol posts                            POST /jpa/users/7/posts -> 201

== 4. Comments (comment-service checks each post exists via Feign -> user-service)
   OK    bob on alice's post                    POST /comments -> 201
   OK    carol on alice's post                  POST /comments -> 201
   OK    alice replies                          POST /comments -> 201
   OK    rambo on bob's post                    POST /comments -> 201
   OK    vigo on carol's post                   POST /comments -> 201
   OK    alice on seed post 1                   POST /comments -> 201

== Result
   users: 8   posts: 12   comments on post 7: 3
   logins (password password123): alice (id 5), bob (id 6), carol (id 7), plus rambo (ADMIN), shamlal, vigo, diga
   try:  curl -s http://localhost:8765/jpa/posts      curl -s "http://localhost:8765/comments?postId=7"

Seed finished.
```

Running it again just says `alice already exists` and stops. Restart the services to start from fresh seed data.

### 4b. `demo.sh`: test every feature

```bash
./scripts/demo.sh
```

It registers a throw-away user, creates posts and comments, tests the rules, errors and security, and deletes the user at the end. Real output:

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

Step 6 shows a real difference between setups. Locally there's no Kafka, so the comments of the deleted user's post remain. With Docker Compose or Kubernetes, the `posts-deleted` event removes them within seconds. Both scripts work against those setups too: `GW=http://localhost:8765 ./scripts/demo.sh`.

## 5. Create data by hand: sample request bodies

Pick one way to send requests: [curl in Git Bash](#53-with-curl-git-bash), [PowerShell](#54-with-powershell), [IntelliJ's HTTP client](#55-with-intellij-idea-http-client) or [Swagger UI](#56-with-swagger-ui-user-service-only). Everything goes through the gateway on port **8765**.

### 5.1 Data that already exists

The database starts with 4 users and 6 posts (Flyway migration `V3__seed_data.sql`). Every seed user's password is `password123`:

| Username | Role | User id | Posts |
| --- | --- | --- | --- |
| `rambo` | ADMIN | 1 | 1, 2 |
| `shamlal` | USER | 2 | 3 |
| `vigo` | USER | 3 | 4 |
| `diga` | USER | 4 | 5, 6 |

New users get ids from 5 upward. In local mode each service uses an in-memory H2 database: **restarting a service resets its data** to these seeds, and comments start empty.

### 5.2 Every endpoint and its request body

"Token" = header `Authorization: Bearer <accessToken from /auth/login>`.

| Method and path | Who may call it | Request body | Success |
| --- | --- | --- | --- |
| `POST /auth/register` | anyone | `{"name":"Gaurav Pawar","birthDate":"1998-04-12","username":"gaurav","password":"my-password-1"}` | 201 + user |
| `POST /auth/login` | anyone | `{"username":"rambo","password":"password123"}` | 200 + token |
| `GET /auth/me` | token | — | 200 + you |
| `GET /jpa/users?page=0&size=5&sort=name,asc` | token | — | 200 + page of users |
| `GET /jpa/users/{id}` | token | — | 200 + user |
| `POST /jpa/users` | ADMIN token | `{"name":"Guest Author","birthDate":"1988-01-01"}` | 201 + user (no login) |
| `PUT /jpa/users/{id}` | ADMIN, or that user | `{"name":"Gaurav B. Pawar","birthDate":"1998-04-12"}` | 200 + user |
| `DELETE /jpa/users/{id}` | ADMIN token | — | 204; their posts go too |
| `POST /jpa/users/{id}/posts` | ADMIN, or that user | `{"description":"Learning microservices"}` | 201 + post |
| `POST /jpa/users/{id}/posts/bulk` | ADMIN, or that user | `[{"description":"one"},{"description":"two"}]` | 201 + posts (all or none) |
| `GET /jpa/users/{id}/posts` | anyone | — | 200 + posts |
| `GET /jpa/posts` | anyone | — | 200 + feed, newest first |
| `GET /jpa/posts/{id}` | anyone | — | 200 + post |
| `GET /jpa/posts/n-plus-one` | token | — | 200 + feed (watch the N+1 SQL in the log) |
| `POST /comments` | token | `{"postId":7,"text":"Great post!"}` | 201 + comment |
| `GET /comments?postId=7` | anyone | — | 200 + comments and post details |
| `DELETE /comments/{id}` | ADMIN, or the comment's author | — | 204 |

**Rules the bodies must follow** (otherwise `400` with one message per field):

| Field | Rule |
| --- | --- |
| `name` | 2–50 characters |
| `birthDate` | `YYYY-MM-DD`, in the past |
| `username` | 3–30 of `a-z`, `0-9`, `_`; unique (else `409`) |
| `password` | 8–72 characters |
| post `description` | not blank, at most 500 characters; at most 10 posts per user (else `409`) |
| comment `text` | not blank, at most 1000 characters, no banned word (`spam`, `scam`); `postId` must exist (else `404`) |

### 5.3 With curl (Git Bash)

Paste these one block at a time. The `# ...` lines under each command are the real answers.

**1. Log in and keep the tokens**

```bash
GW=localhost:8765
login() { curl -s -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}" $GW/auth/login | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4; }
ADMIN=$(login rambo password123)
VIGO=$(login vigo password123)
echo ${#ADMIN} ${#VIGO}          # two lengths (about 160 characters each) = two tokens
```

The full login answer looks like `{"accessToken":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresInSeconds":3600}`.

**2. Register your own user and log in as it**

```bash
curl -s -H 'Content-Type: application/json' $GW/auth/register \
  -d '{"name":"Gaurav Pawar","birthDate":"1998-04-12","username":"gaurav","password":"my-password-1"}'
# {"id":5,"name":"Gaurav Pawar","birthDate":"1998-04-12","age":28}

ME=$(login gaurav my-password-1)
MY_ID=5                                   # the "id" from the answer above
curl -s -H "Authorization: Bearer $ME" $GW/auth/me
# {"id":5,"name":"Gaurav Pawar","birthDate":"1998-04-12","age":28}
```

**3. Users**

```bash
# all users, paged and sorted
curl -s -H "Authorization: Bearer $ME" "$GW/jpa/users?page=0&size=3&sort=name,asc"
# {"content":[{"id":5,"name":"Alice Sharma",...},...],"page":{"size":3,"number":0,"totalElements":8,"totalPages":3}}

# update yourself
curl -s -X PUT -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' \
  -d '{"name":"Gaurav B. Pawar","birthDate":"1998-04-12"}' $GW/jpa/users/$MY_ID
# {"id":5,"name":"Gaurav B. Pawar","birthDate":"1998-04-12","age":28}

# ADMIN only: create a user that has no login
curl -s -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"name":"Guest Author","birthDate":"1988-01-01"}' $GW/jpa/users
```

**4. Posts**

```bash
curl -s -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' \
  -d '{"description":"Learning microservices"}' $GW/jpa/users/$MY_ID/posts
# {"id":7,"description":"Learning microservices","createdAt":"2026-10-10T11:26:12.768328","authorId":5,"authorName":"Gaurav B. Pawar"}
POST_ID=7                                 # the "id" from the answer above

# several posts in one transaction
curl -s -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' \
  -d '[{"description":"Spring Data JPA derived queries"},{"description":"How @Transactional rollback works"}]' \
  $GW/jpa/users/$MY_ID/posts/bulk

curl -s $GW/jpa/users/$MY_ID/posts        # your posts (public)
curl -s $GW/jpa/posts/$POST_ID            # one post (public)
curl -s $GW/jpa/posts                     # the whole feed (public)
```

**5. Comments** (comment-service asks user-service whether the post exists)

```bash
curl -s -H "Authorization: Bearer $VIGO" -H 'Content-Type: application/json' \
  -d "{\"postId\":$POST_ID,\"text\":\"Great post!\"}" $GW/comments
# {"id":1,"postId":7,"authorId":3,"authorUsername":"vigo","text":"Great post!","createdAt":"..."}

curl -s -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' \
  -d "{\"postId\":$POST_ID,\"text\":\"Thanks for reading!\"}" $GW/comments

curl -s "$GW/comments?postId=$POST_ID"
# {"postId":7,"postDetailsAvailable":true,"postDescription":"Learning microservices","postAuthor":"Gaurav B. Pawar",
#  "comments":[{"id":1,...,"authorUsername":"vigo","text":"Great post!",...},{"id":2,...}]}

# delete a comment: its author or an ADMIN -> 204, anyone else -> 403
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $VIGO" $GW/comments/1
```

**6. Try the rules.** Every error has the same JSON shape, for example:

```json
{"timeStamp":"2026-10-10T11:26:54.0431255","message":"Validation failed","details":"uri=/jpa/users",
 "errors":["birthDate: Birth date must be in the past","name: Name must be 2-50 characters"]}
```

| Try | Command | Expect |
| --- | --- | --- |
| Wrong password | `curl -s -H 'Content-Type: application/json' -d '{"username":"rambo","password":"wrong"}' $GW/auth/login` | `401` Invalid username or password |
| Invalid registration | `curl -s -H 'Content-Type: application/json' -d '{"name":"A","birthDate":"2999-01-01","username":"Bad Name!","password":"short"}' $GW/auth/register` | `400` with one message per field |
| Username taken | register `gaurav` again | `409` Username 'gaurav' is already taken |
| Post that doesn't exist | `curl -s -H "Authorization: Bearer $VIGO" -H 'Content-Type: application/json' -d '{"postId":999,"text":"hi"}' $GW/comments` | `404` Post not found with id: 999 |
| Banned word | same, with `"text":"easy scam"` and a real `postId` | `400` Comment contains a banned word: scam |
| Empty comment | same, with `"text":""` | `400` text: Text is required |
| No token | `curl -s -o /dev/null -w '%{http_code}\n' -X POST -H 'Content-Type: application/json' -d '{"description":"x"}' $GW/jpa/users/$MY_ID/posts` | `401` |
| Broken token | `curl -s -H "Authorization: Bearer abc.def.ghi" $GW/auth/me` | `401` Invalid token: MalformedJwtException |
| Post as someone else | `curl -s -H "Authorization: Bearer $ME" -H 'Content-Type: application/json' -d '{"description":"x"}' $GW/jpa/users/1/posts` | `403` |
| Delete someone else's comment | `curl -s -X DELETE -H "Authorization: Bearer $ME" $GW/comments/1` | `403` |
| USER deletes a user | `curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $ME" $GW/jpa/users/2` | `403` |
| ADMIN deletes you | `curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $ADMIN" $GW/jpa/users/$MY_ID` | `204`; your posts are gone too |

### 5.4 With PowerShell

In Windows PowerShell, `curl` is an alias for `Invoke-WebRequest`, so the commands above don't work there. Use `Invoke-RestMethod`, which turns the JSON answer into an object:

```powershell
$GW = "http://localhost:8765"
$JSON = "application/json"

# log in; the token goes into a header table
$login = Invoke-RestMethod -Method Post -Uri "$GW/auth/login" -ContentType $JSON -Body '{"username":"bob","password":"password123"}'
$H = @{ Authorization = "Bearer $($login.accessToken)" }

Invoke-RestMethod -Uri "$GW/auth/me" -Headers $H
# id name          birthDate  age
# 6  Bob Fernandes 1990-08-02  36

# create a post (bob is user 6 after seed-data.sh; use your own id)
$post = Invoke-RestMethod -Method Post -Uri "$GW/jpa/users/6/posts" -Headers $H -ContentType $JSON -Body '{"description":"Posted from PowerShell"}'

# comment on it: build the body from a hashtable
$body = @{ postId = $post.id; text = "First comment from PowerShell" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$GW/comments" -Headers $H -ContentType $JSON -Body $body

(Invoke-RestMethod -Uri "$GW/comments?postId=$($post.id)").comments    # list the comments

# errors throw; catch them to see the status code
try { Invoke-RestMethod -Method Post -Uri "$GW/comments" -Headers $H -ContentType $JSON -Body '{"postId":999,"text":"hi"}' }
catch { $_.Exception.Response.StatusCode.value__ }                    # 404
```

Use `curl.exe` (with `.exe`) if you prefer real curl in PowerShell, but the quoting of JSON bodies differs from Git Bash.

### 5.5 With IntelliJ IDEA HTTP client

Open [`docs/api-requests.http`](api-requests.http) and click the green ▶ next to each request. It has a sample body for every endpoint above, plus the error cases. Run it top to bottom the first time: the login requests store the tokens and the create requests store the new ids, which the later requests reuse (`{{me}}`, `{{myId}}`, `{{postId}}`).

### 5.6 With Swagger UI (user-service only)

1. Open http://localhost:8080/swagger-ui.html.
2. `POST /auth/login` → **Try it out** → `{"username":"rambo","password":"password123"}` → **Execute** → copy `accessToken`.
3. Click **Authorize** (top right), paste the token, then **Authorize**.
4. Every user and post endpoint can now be called from the page, with the bodies from [5.2](#52-every-endpoint-and-its-request-body). Comments live in comment-service, so use one of the other ways for those.

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
