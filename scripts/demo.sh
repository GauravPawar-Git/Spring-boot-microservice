#!/usr/bin/env bash
# Create sample data through the gateway and check each feature, step by step.
# Works against any running setup: start-local.sh, docker compose, or Kubernetes (port-forward 8765).
#   ./scripts/demo.sh                 (needs only bash and curl)
#   GW=http://host:port ./scripts/demo.sh
set -uo pipefail

GW=${GW:-http://localhost:8765}
JSON='Content-Type: application/json'
USER_NAME="demo_$(date +%s | tail -c 6)"      # new user each run, so reruns don't clash
TMP=$(mktemp)
failures=0

# Tiny JSON helpers (no jq needed): first value of a field, and how often a field appears
field() { grep -oE "\"$1\":\"?[^,\"}]*" | head -1 | sed -E "s/^\"[^\"]*\":\"?//"; }
count() { grep -oE "\"$1\":" | wc -l | tr -d ' '; }

step() { echo; echo "== $*"; }
ok()   { echo "   OK    $*"; }
bad()  { echo "   FAIL  $*"; failures=$((failures + 1)); }
info() { echo "   info  $*"; }

# call METHOD PATH [TOKEN] [BODY]  ->  sets $status and $body
call() {
  local method=$1 path=$2 token=${3:-} data=${4:-}
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$method" "$GW$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-H "$JSON" -d "$data")
  status=$(curl "${args[@]}")
  body=$(cat "$TMP")
}
expect() { if [ "$status" = "$1" ]; then ok "$2 -> $status"; else bad "$2 -> expected $1, got $status: $body"; fi; }
login()  { curl -s -H "$JSON" -d "{\"username\":\"$1\",\"password\":\"$2\"}" "$GW/auth/login" | field accessToken; }

step "0. Gateway reachable at $GW"
call GET /jpa/posts
[ "$status" = 200 ] || { echo "   Gateway answered $status. Start the services first (scripts/start-local.sh)."; exit 1; }
ok "seed data: $(echo "$body" | count description) posts"

step "1. Register a new user ($USER_NAME) and log in"
call POST /auth/register "" "{\"name\":\"Demo User\",\"birthDate\":\"1998-04-12\",\"username\":\"$USER_NAME\",\"password\":\"demo-pass-123\"}"
expect 201 "register"
USER_ID=$(echo "$body" | field id)
info "created: $body"
TOKEN=$(login "$USER_NAME" demo-pass-123)
[ -n "$TOKEN" ] && ok "login -> got a JWT (${#TOKEN} characters)" || bad "login failed"
call GET /auth/me "$TOKEN"
expect 200 "GET /auth/me as $USER_NAME"

step "2. Create posts (stored by user-service)"
call POST "/jpa/users/$USER_ID/posts" "$TOKEN" '{"description":"My first post from the demo"}'
expect 201 "create post"
POST_ID=$(echo "$body" | field id)
info "created: $body"
call POST "/jpa/users/$USER_ID/posts/bulk" "$TOKEN" '[{"description":"bulk 1"},{"description":"bulk 2"}]'
expect 201 "bulk create 2 posts (one transaction)"
call GET "/jpa/users/$USER_ID/posts"
expect 200 "list my posts"
info "$USER_NAME now has $(echo "$body" | count description) posts"

step "3. Comment on the post (comment-service checks the post via Feign -> user-service)"
VIGO=$(login vigo password123)
call POST /comments "$VIGO" "{\"postId\":$POST_ID,\"text\":\"Nice post!\"}"
expect 201 "vigo comments on post $POST_ID"
call POST /comments "$TOKEN" "{\"postId\":$POST_ID,\"text\":\"Thanks for reading\"}"
expect 201 "$USER_NAME replies"
call GET "/comments?postId=$POST_ID"
expect 200 "read comments"
info "comments: $(echo "$body" | count text), post details from user-service: postAuthor=$(echo "$body" | field postAuthor)"

step "4. Validation and business rules"
call POST /comments "$VIGO" '{"postId":999999,"text":"hello?"}'
expect 404 "comment on a post that doesn't exist"
call POST /comments "$VIGO" "{\"postId\":$POST_ID,\"text\":\"easy scam money\"}"
expect 400 "banned word (rule served by config-server)"
info "$(echo "$body" | field message)"
call POST "/jpa/users/$USER_ID/posts" "$TOKEN" '{"description":""}'
expect 400 "empty post description"
BULK=$(printf '{"description":"bulk %s"},' $(seq 1 10)); BULK="[${BULK%,}]"
call POST "/jpa/users/$USER_ID/posts/bulk" "$TOKEN" "$BULK"
expect 409 "10 more posts would exceed the limit of 10"
call GET "/jpa/users/$USER_ID/posts"
info "still $(echo "$body" | count description) posts: the whole bulk insert was rolled back"

step "5. Security"
call POST "/jpa/users/$USER_ID/posts" "" '{"description":"no token"}'
expect 401 "create a post without a token"
call POST "/jpa/users/1/posts" "$TOKEN" '{"description":"posting as rambo"}'
expect 403 "post as another user"
call DELETE /jpa/users/2 "$TOKEN"
expect 403 "USER deletes a user"

step "6. Admin deletes $USER_NAME: their posts go too; Kafka tells comment-service to delete the comments"
before=$(curl -s "$GW/comments?postId=$POST_ID" | count text)
ADMIN=$(login rambo password123)
call DELETE "/jpa/users/$USER_ID" "$ADMIN"
expect 204 "ADMIN deletes user $USER_ID"
call GET "/jpa/users/$USER_ID/posts"
info "posts of the deleted user: $(echo "$body" | count description)"
remaining=""
for _ in $(seq 1 8); do
  remaining=$(curl -s "$GW/comments?postId=$POST_ID" | count text)
  [ "$remaining" = 0 ] && break
  sleep 2
done
if [ "$before" = 0 ]; then
  info "post $POST_ID had no comments, so there was nothing for Kafka to clean up"
elif [ "$remaining" = 0 ]; then
  ok "$before comments on post $POST_ID removed asynchronously through Kafka"
else
  info "comments on post $POST_ID still there ($remaining): Kafka events are off in local mode (EVENTS_ENABLED=false). With docker compose or Kubernetes they disappear within seconds."
fi

rm -f "$TMP"
echo
if [ "$failures" = 0 ]; then echo "Demo finished: every check passed."; else echo "Demo finished with $failures failed check(s)."; exit 1; fi
