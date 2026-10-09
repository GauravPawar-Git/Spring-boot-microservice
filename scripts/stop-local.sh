#!/usr/bin/env bash
# Stop everything started by scripts/start-local.sh (finds each process by the port it listens on).
set -uo pipefail

stop_port() {
  local name=$1 port=$2 pid=""
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)   # Git Bash on Windows
      pid=$(netstat -ano | awk -v p=":$port" '$2 ~ p"$" && $4 == "LISTENING" {print $5; exit}')
      [ -n "$pid" ] && taskkill //PID "$pid" //F > /dev/null 2>&1 ;;
    *)                      # Linux / macOS
      pid=$(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -1)
      [ -n "$pid" ] && kill "$pid" ;;
  esac
  if [ -n "$pid" ]; then echo "  stopped $name (port $port, pid $pid)"; else echo "  $name was not running"; fi
}

stop_port api-gateway      8765
stop_port comment-service  8082
stop_port user-service     8080
stop_port discovery-server 8761
stop_port config-server    8888
stop_port zipkin           9411
