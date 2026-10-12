#!/usr/bin/env bash
# End-to-end test of the running docker-compose stack. Everything goes through the gateway (:8765)
# except the Eureka and Zipkin checks. Needs: bash, curl, jq.
#   docker compose up -d --build --wait && ./scripts/e2e.sh
set -euo pipefail

GW=http://localhost:8765
JSON='Content-Type: application/json'
pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; exit 1; }

# Retry a command until it succeeds (services register with Eureka and caches warm up asynchronously)
retry() {
  local tries=$1; shift
  for ((i = 1; i <= tries; i++)); do
    if "$@"; then return 0; fi
    sleep 3
  done
  return 1
}

login() {
  curl -sf -H "$JSON" -d "{\"username\":\"$1\",\"password\":\"password123\"}" "$GW/auth/login" | jq -r .accessToken
}

# 1. Service discovery: all three clients registered with Eureka
eureka_has_all() {
  local apps
  apps=$(curl -sf -H 'Accept: application/json' http://localhost:8761/eureka/apps | jq -r '.applications.application[].name' | sort | tr '\n' ' ')
  [[ "$apps" == "API-GATEWAY COMMENT-SERVICE USER-SERVICE " ]]
}
retry 40 eureka_has_all || fail "Eureka registry incomplete"
pass "Eureka lists API-GATEWAY, COMMENT-SERVICE, USER-SERVICE"

# 2. Gateway -> user-service, login against PostgreSQL (Flyway seed + BCrypt)
gateway_login() { VIGO=$(login vigo) && [[ -n "$VIGO" && "$VIGO" != null ]]; }
retry 40 gateway_login || fail "login through the gateway"
# Retried too: right after warm-up the gateway can still answer one request with its 503 fallback,
# and a bare ADMIN=$(login rambo) under `set -e` would end the script with curl's exit code 22.
admin_login() { ADMIN=$(login rambo) && [[ -n "$ADMIN" && "$ADMIN" != null ]]; }
retry 10 admin_login || fail "admin login through the gateway"
pass "login through the gateway (user-service on PostgreSQL)"

# 3. Gateway -> comment-service -> (Feign) user-service: comment on Diga's post 5
comment_created() {
  STATUS=$(curl -s -o /tmp/comment.json -w '%{http_code}' -H "Authorization: Bearer $VIGO" -H "$JSON" \
    -d '{"postId":5,"text":"Commenting on a post that will disappear"}' "$GW/comments")
  [[ "$STATUS" == 201 ]]
}
retry 40 comment_created || fail "create comment (last status $STATUS)"
pass "comment created: $(jq -c '{id, postId, authorUsername}' /tmp/comment.json)"

COUNT=$(curl -sf "$GW/comments?postId=5" | jq '.comments | length')
[[ "$COUNT" -ge 1 ]] || fail "comment not readable"
pass "post 5 has $COUNT comment(s), post details: $(curl -sf "$GW/comments?postId=5" | jq -c '{postDetailsAvailable, postAuthor}')"

# 4. Async: delete Diga (user 4) -> user-service publishes posts-deleted -> comment-service deletes the comments
curl -sf -o /dev/null -X DELETE -H "Authorization: Bearer $ADMIN" "$GW/jpa/users/4" || fail "delete user 4"
pass "admin deleted user 4 (Diga), owner of posts 5 and 6"

comments_gone() { [[ "$(curl -sf "$GW/comments?postId=5" | jq '.comments | length')" == 0 ]]; }
START=$(date +%s)
retry 20 comments_gone || fail "comments on post 5 still there: Kafka event not processed"
pass "comments on post 5 removed by the Kafka event within $(( $(date +%s) - START ))s"

# 5. Tracing: one trace that spans gateway, comment-service and user-service
trace_spans_three_services() {
  curl -sf "http://localhost:9411/api/v2/traces?serviceName=comment-service&spanName=http%20post%20%2Fcomments&limit=1" \
    | jq -e '[.[0][].localEndpoint.serviceName] | unique | (index("api-gateway") != null and index("comment-service") != null and index("user-service") != null)' > /dev/null
}
retry 10 trace_spans_three_services || fail "no trace across all three services in Zipkin"
pass "Zipkin has a single trace across api-gateway, comment-service and user-service"

echo "All end-to-end checks passed."
