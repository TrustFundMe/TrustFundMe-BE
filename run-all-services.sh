#!/usr/bin/env bash
# macOS — mở nhiều cửa sổ Terminal chạy toàn bộ dịch vụ (tương đương run-all-services.ps1).
# Chạy: chmod +x run-all-services.sh && ./run-all-services.sh
#
# Service không có thư mục trong repo sẽ bị bỏ qua (không mở Terminal / không chạy mvn ở root).
#
# JDK: project định nghĩa java.version 17. Trên Mac nếu mvn đang chạy JDK 25+, Lombok 1.18.x có thể không
# chạy annotation processor đúng → lỗi "cannot find symbol" (builder, getId, log, ...).
# Export TRUSTFUND_JAVA_HOME trỏ tới JDK 17, hoặc brew install openjdk@17 để script tự gắn JAVA_HOME trong mỗi cửa sổ.
#
# Cổng trùng: nếu chạy script / mvn hai lần mà Eureka cũ vẫn bật → "Port 8761 was already in use".
# Đóng process cũ (hoặc kill PID script in ra). Bỏ qua kiểm tra cổng: SKIP_PORT_CHECK=1 ./run-all-services.sh

set -euo pipefail

# Chờ giữa các bước mở cửa sổ (giây). Có thể ghi đè: DISCOVERY_SLEEP_SEC=5 SERVICE_SLEEP_SEC=3 ./run-all-services.sh
DISCOVERY_SLEEP_SEC="${DISCOVERY_SLEEP_SEC:-3}"
SERVICE_SLEEP_SEC="${SERVICE_SLEEP_SEC:-2}"

ROOT="$(cd "$(dirname "$0")" && pwd)"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Script này dựa trên Terminal.app (macOS). Trên Linux bạn có thể dùng tmux hoặc tự mở từng terminal."
  exit 1
fi

resolve_java17_for_mvn() {
  if [[ -n "${TRUSTFUND_JAVA_HOME:-}" ]] && [[ -x "${TRUSTFUND_JAVA_HOME}/bin/java" ]]; then
    echo "$TRUSTFUND_JAVA_HOME"
    return 0
  fi
  for _c in \
    "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"; do
    if [[ -x "${_c}/bin/java" ]]; then
      echo "$_c"
      return 0
    fi
  done
  return 1
}

MVN_JAVA_PREFIX=""
if JAVA17="$(resolve_java17_for_mvn || true)" && [[ -n "$JAVA17" ]]; then
  MVN_JAVA_PREFIX="export JAVA_HOME='$JAVA17' && "
  echo "Đã gắn JAVA_HOME → JDK 17 cho mvn: $JAVA17"
else
  echo "Cảnh báo: không tìm thấy JDK 17 — mvn có thể dùng JDK 25+ và gây lỗi compile Lombok. Cài: brew install openjdk@17"
fi

precheck_standard_ports_if_needed() {
  [[ "${SKIP_PORT_CHECK:-}" == "1" ]] && return 0
  local row subdir port label pids
  local rows=(
    "discovery-server|8761|Eureka Discovery"
    "api-gateway|8080|API Gateway"
    "identity-service|8081|Identity Service"
    "campaign-service|8082|Campaign Service"
    "media-service|8083|Media Service"
    "feed-service|8084|Feed Service"
    "flag-service|8085|Flag Service"
    "chat-service|8086|Chat Service"
    "payment-service|8087|Payment Service"
    "notification-service|8088|Notification Service"
  )
  for row in "${rows[@]}"; do
    IFS='|' read -r subdir port label <<<"$row"
    [[ ! -d "$ROOT/$subdir" ]] && continue
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | xargs || true)"
    if [[ -n "${pids}" ]]; then
      echo ""
      echo "Dừng script: cổng $port ($label — $subdir) đang được dùng."
      echo "PID đang LISTEN: $pids"
      echo "Tắt process cũ rồi chạy lại, ví dụ: kill $pids"
      echo "Hoặc: SKIP_PORT_CHECK=1 ./run-all-services.sh (không khuyến nghị nếu mở trùng cùng service)"
      exit 1
    fi
  done
}

precheck_standard_ports_if_needed

# Một dòng lệnh, toàn && — nếu cd thất bại thì không chạy mvn (tránh BUILD FAILURE ở thư mục gốc).
# use_env=1: load .env ở root nếu có (giống các run-*.ps1). use_env=0: chỉ cd vào module (discovery).
run_if_dir() {
  local subdir="$1"
  local echo_line="$2"
  local use_env="${3:-1}"
  if [[ ! -d "$ROOT/$subdir" ]]; then
    echo "[bỏ qua] $subdir — không có thư mục trong repo."
    return 1
  fi
  local cmd
  if [[ "$use_env" == "1" ]]; then
    cmd="${MVN_JAVA_PREFIX}cd '$ROOT' && { [ ! -f .env ] || { set -a && . ./.env && set +a; }; } && cd '$ROOT/$subdir' && echo '$echo_line' && mvn spring-boot:run"
  else
    cmd="${MVN_JAVA_PREFIX}cd '$ROOT/$subdir' && echo '$echo_line' && mvn spring-boot:run"
  fi
  osascript -e "tell application \"Terminal\" to do script \"$cmd\""
  return 0
}

echo "Đang mở các cửa sổ Terminal để chạy dịch vụ..."
echo "Mỗi dịch vụ chạy trong một cửa sổ riêng (chỉ những thư mục module có sẵn)."
echo ""

if run_if_dir "discovery-server" "Starting Discovery Server (port 8761)..." 0; then
  sleep "$DISCOVERY_SLEEP_SEC"
fi

if run_if_dir "api-gateway" "Starting API Gateway (port 8080)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "identity-service" "Starting Identity Service (port 8081)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "campaign-service" "Starting Campaign Service (port 8082)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "media-service" "Starting Media Service (port 8083)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "feed-service" "Starting Feed Service (port 8084)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "flag-service" "Starting Flag Service (port 8085)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "chat-service" "Starting Chat Service (port 8086)..."; then
  sleep "$SERVICE_SLEEP_SEC"
fi

if run_if_dir "payment-service" "Starting Payment Service (port 8087)..."; then
  :
fi

run_if_dir "notification-service" "Starting Notification Service (port 8088)..." || true

echo ""
echo "Hoàn tất. Các module không có trong repo đã được bỏ qua (xem dòng [bỏ qua] ở trên)."
echo "Thứ tự dự kiến khi đủ thư mục: Discovery 8761, Gateway 8080, Identity 8081, Campaign 8082,"
echo "  Media 8083, Feed 8084, Flag 8085, Chat 8086, Payment 8087, Notification 8088."
