#!/usr/bin/env bash
# Start every service locally as a jar (no Docker), in dependency order, waiting for each to be healthy.
#
#   ./mvnw -q package -DskipTests      # build first (once, and after code changes)
#   ./scripts/start-local.sh           # optional: ZIPKIN_JAR=/path/to/zipkin-server-3.6.1-exec.jar ./scripts/start-local.sh
#   ./scripts/stop-local.sh
#
# Logs go to .run/logs/<service>.log. Kafka is not used in this mode (EVENTS_ENABLED defaults to false).
set -euo pipefail
cd "$(dirname "$0")/.."

LOGS=.run/logs
mkdir -p "$LOGS"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
VERSION=0.0.1-SNAPSHOT

for module in config-server discovery-server user-service comment-service api-gateway; do
  if [ ! -f "$module/target/$module-$VERSION.jar" ]; then
    echo "Missing $module/target/$module-$VERSION.jar. Build first:  ./mvnw -q package -DskipTests"
    exit 1
  fi
done

is_up() { curl -s -o /dev/null -f "$1"; }

# start <name> <jar> <health-url>: skip if already running, otherwise launch and wait up to 2 minutes
start() {
  local name=$1 jar=$2 url=$3
  if is_up "$url"; then
    echo "  $name is already running"
    return 0
  fi
  printf "  starting %-17s" "$name"
  # subshell + &: the java process keeps running after this script ends
  ( "$JAVA" -jar "$jar" > "$LOGS/$name.log" 2>&1 & )
  for _ in $(seq 1 60); do
    if is_up "$url"; then echo " up"; return 0; fi
    sleep 2
  done
  echo " FAILED, see $LOGS/$name.log"
  exit 1
}

echo "1/3 Infrastructure"
if [ -n "${ZIPKIN_JAR:-}" ]; then
  start zipkin "$ZIPKIN_JAR" http://localhost:9411/health
else
  echo "  zipkin skipped (set ZIPKIN_JAR to the zipkin-server exec jar to include tracing)"
fi
start config-server    "config-server/target/config-server-$VERSION.jar"       http://localhost:8888/actuator/health
start discovery-server "discovery-server/target/discovery-server-$VERSION.jar" http://localhost:8761/eureka/apps

echo "2/3 Services (they read their settings from config-server and register with Eureka)"
start user-service     "user-service/target/user-service-$VERSION.jar"         http://localhost:8080/actuator/health
start comment-service  "comment-service/target/comment-service-$VERSION.jar"   http://localhost:8082/actuator/health
start api-gateway      "api-gateway/target/api-gateway-$VERSION.jar"           http://localhost:8765/actuator/health

# The gateway and comment-service find the others through Eureka, whose client caches refresh every 30 s.
# Until then the gateway answers 503 (its fallback) and comment-service cannot reach user-service.
# Ready = the whole chain works: gateway -> comment-service -> (Feign) user-service returns post details.
# A plain 200 on /comments is not enough: comment-service also answers 200 with postDetailsAvailable:false.
echo "3/3 Waiting until gateway -> comment-service -> user-service works (up to ~2 minutes)"
posts="" chain=""
for _ in $(seq 1 40); do
  posts=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8765/jpa/posts || true)
  chain=$(curl -s 'http://localhost:8765/comments?postId=1' | grep -o '"postDetailsAvailable":true' || true)
  if [ "$posts" = 200 ] && [ -n "$chain" ]; then
    echo "  all services can reach each other"
    echo
    echo "All services are up. Entry point: http://localhost:8765"
    echo "  Eureka:  http://localhost:8761     Swagger (user-service): http://localhost:8080/swagger-ui.html"
    echo "  Next:    ./scripts/seed-data.sh     creates sample users, posts and comments (kept)"
    echo "           ./scripts/demo.sh          tests every feature step by step"
    exit 0
  fi
  sleep 3
done
echo "  not ready (posts=$posts, post details reachable: ${chain:-no}); check $LOGS/comment-service.log and $LOGS/api-gateway.log"
exit 1
