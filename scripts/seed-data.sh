#!/usr/bin/env bash
# Create a realistic set of sample data through the gateway and KEEP it (demo.sh deletes what it creates).
# Run after the services are up (scripts/start-local.sh, docker compose, or a Kubernetes port-forward):
#   ./scripts/seed-data.sh
#   GW=http://host:port ./scripts/seed-data.sh
# Every new user's password is password123, like the built-in seed users.
# Local mode keeps data in memory: restarting user-service / comment-service resets it, then run this again.
set -uo pipefail

GW=${GW:-http://localhost:8765}
JSON='Content-Type: application/json'
PASSWORD=password123
TMP=$(mktemp)
failures=0

field() { grep -oE "\"$1\":\"?[^,\"}]*" | head -1 | sed -E "s/^\"[^\"]*\":\"?//"; }
count() { grep -oE "\"$1\":" | wc -l | tr -d ' '; }

# send METHOD PATH TOKEN BODY EXPECTED_STATUS LABEL  ->  sets $body; prints the request and the answer
send() {
  local method=$1 path=$2 token=$3 data=$4 want=$5 label=$6 status
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$method" "$GW$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-H "$JSON" -d "$data")
  status=$(curl "${args[@]}")
  body=$(cat "$TMP")
  if [ "$status" = "$want" ]; then
    printf '   OK    %-38s %s %s -> %s\n' "$label" "$method" "$path" "$status"
  else
    printf '   FAIL  %-38s %s %s -> expected %s, got %s: %s\n' "$label" "$method" "$path" "$want" "$status" "$body"
    failures=$((failures + 1))
  fi
}
login() { curl -s -H "$JSON" -d "{\"username\":\"$1\",\"password\":\"$2\"}" "$GW/auth/login" | field accessToken; }

echo "Seeding sample data through $GW"
[ "$(curl -s -o /dev/null -w '%{http_code}' "$GW/jpa/posts")" = 200 ] \
  || { echo "   Gateway not ready. Start the services first (scripts/start-local.sh)."; exit 1; }

# Already seeded? (a username can only be registered once)
if [ -n "$(login alice "$PASSWORD")" ]; then
  echo "   alice already exists: the sample data is already there. Restart the services to start from scratch."
  exit 0
fi

echo; echo "== 1. Register three users (public endpoint, each gets a login)"
send POST /auth/register "" '{"name":"Alice Sharma","birthDate":"1995-03-14","username":"alice","password":"password123"}' 201 "register alice"
ALICE_ID=$(echo "$body" | field id)
send POST /auth/register "" '{"name":"Bob Fernandes","birthDate":"1990-08-02","username":"bob","password":"password123"}' 201 "register bob"
BOB_ID=$(echo "$body" | field id)
send POST /auth/register "" '{"name":"Carol Mehta","birthDate":"2000-12-25","username":"carol","password":"password123"}' 201 "register carol"
CAROL_ID=$(echo "$body" | field id)

ALICE=$(login alice "$PASSWORD"); BOB=$(login bob "$PASSWORD"); CAROL=$(login carol "$PASSWORD")
ADMIN=$(login rambo "$PASSWORD"); VIGO=$(login vigo "$PASSWORD")

echo; echo "== 2. Profiles"
send PUT "/jpa/users/$CAROL_ID" "$CAROL" '{"name":"Carol D. Mehta","birthDate":"2000-12-25"}' 200 "carol renames herself"
send POST /jpa/users "$ADMIN" '{"name":"Guest Author","birthDate":"1988-01-01"}' 201 "admin creates a user without login"

echo; echo "== 3. Posts"
send POST "/jpa/users/$ALICE_ID/posts" "$ALICE" '{"description":"Spring Boot auto-configuration explained"}' 201 "alice posts"
ALICE_POST=$(echo "$body" | field id)
send POST "/jpa/users/$ALICE_ID/posts/bulk" "$ALICE" \
  '[{"description":"Why records make great DTOs"},{"description":"@Transactional pitfalls: self-invocation"}]' 201 "alice bulk-posts 2"
send POST "/jpa/users/$BOB_ID/posts" "$BOB" '{"description":"Feign + Resilience4j in 10 minutes"}' 201 "bob posts"
BOB_POST=$(echo "$body" | field id)
send POST "/jpa/users/$BOB_ID/posts" "$BOB" '{"description":"Eureka vs Kubernetes service discovery"}' 201 "bob posts again"
send POST "/jpa/users/$CAROL_ID/posts" "$CAROL" '{"description":"My first week with JWT authentication"}' 201 "carol posts"
CAROL_POST=$(echo "$body" | field id)

echo; echo "== 4. Comments (comment-service checks each post exists via Feign -> user-service)"
send POST /comments "$BOB"   "{\"postId\":$ALICE_POST,\"text\":\"Clear explanation, thanks!\"}" 201 "bob on alice's post"
send POST /comments "$CAROL" "{\"postId\":$ALICE_POST,\"text\":\"Could you cover @Conditional next?\"}" 201 "carol on alice's post"
send POST /comments "$ALICE" "{\"postId\":$ALICE_POST,\"text\":\"Good idea, coming soon.\"}" 201 "alice replies"
send POST /comments "$ADMIN" "{\"postId\":$BOB_POST,\"text\":\"Pinning this for the team.\"}" 201 "rambo on bob's post"
send POST /comments "$VIGO"  "{\"postId\":$CAROL_POST,\"text\":\"Welcome aboard!\"}" 201 "vigo on carol's post"
send POST /comments "$ALICE" '{"postId":1,"text":"This is where it all started."}' 201 "alice on seed post 1"

echo; echo "== Result"
users=$(curl -s "$GW/jpa/users?size=100" -H "Authorization: Bearer $ADMIN" | count birthDate)
posts=$(curl -s "$GW/jpa/posts" | count description)
echo "   users: $users   posts: $posts   comments on post $ALICE_POST: $(curl -s "$GW/comments?postId=$ALICE_POST" | count text)"
echo "   logins (password $PASSWORD): alice (id $ALICE_ID), bob (id $BOB_ID), carol (id $CAROL_ID), plus rambo (ADMIN), shamlal, vigo, diga"
echo "   try:  curl -s $GW/jpa/posts      curl -s \"$GW/comments?postId=$ALICE_POST\""

rm -f "$TMP"
echo
if [ "$failures" = 0 ]; then echo "Seed finished."; else echo "Seed finished with $failures failed request(s)."; exit 1; fi
