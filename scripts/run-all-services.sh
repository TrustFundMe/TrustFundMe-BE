#!/usr/bin/env bash
# macOS — tương đương scripts/run-all-services.ps1: 9 service, nghỉ 2s giữa các bước mở Terminal.
# Repo root = thư mục cha của scripts/. Chạy: ./run-all-services.sh (từ thư mục scripts) hoặc bash scripts/run-all-services.sh
#
# Thư mục module không tồn tại sẽ được bỏ qua (không mở Terminal / không chạy mvn ở sai chỗ).
#
# JDK 17 khuyến nghị (java.version trong pom). JDK 25+ thường làm Lombok không generate code → cannot find symbol.
# TRUSTFUND_JAVA_HOME hoặc Homebrew openjdk@17 được dùng tự động trong mỗi cửa sổ.
#
# Eureka / các service chỉ được một tiến trình per cổng. Trùng → "Port already in use". SKIP_PORT_CHECK=1 để bỏ kiểm tra.

set -euo pipefail

STEP_SLEEP_SEC="${STEP_SLEEP_SEC:-2}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Script này cần Terminal.app (macOS + osascript)."
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

TF_JAVA_HOME=""
if JAVA17="$(resolve_java17_for_mvn || true)" && [[ -n "$JAVA17" ]]; then
  TF_JAVA_HOME="$JAVA17"
  echo "Đã gắn JAVA_HOME → JDK 17 cho mvn: $JAVA17"
else
  echo "Cảnh báo: không tìm thấy JDK 17 — có thể lỗi Lombok khi compile. Cài: brew install openjdk@17"
fi

# Terminal.app mặc định chạy lệnh bằng zsh; chuỗi cho osascript cần escape " và \.
trustfund_escape_for_osascript() {
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

precheck_standard_ports_if_needed() {
  [[ "${SKIP_PORT_CHECK:-}" == "1" ]] && return 0
  local row subdir port label pids
  local rows=(
    "discovery-server|8761|Eureka Discovery"
    "api-gateway|8080|API Gateway"
    "identity-service|8081|Identity Service"
    "campaign-service|8082|Campaign Service"
    "media-service|8083|Media Service"
    "chat-service|8086|Chat Service"
    "payment-service|8087|Payment Service"
    "notification-service|8088|Notification Service"
    "audit-service|8089|Audit Service"
  )
  for row in "${rows[@]}"; do
    IFS='|' read -r subdir port label <<<"$row"
    [[ ! -d "$ROOT/$subdir" ]] && continue
    # lsof exits 1 when nothing matches; with pipefail + set -e that would kill the whole script.
    lsof_out=""
    lsof_out="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null)" || true
    pids="${lsof_out//$'\n'/ }"
    if [[ -n "${pids}" ]]; then
      echo ""
      echo "Dừng script: cổng $port ($label — $subdir) đang được dùng. PID: $pids"
      echo "Tắt process cũ: kill $pids — hoặc SKIP_PORT_CHECK=1 ./scripts/run-all-services.sh"
      exit 1
    fi
  done
}

precheck_standard_ports_if_needed

# Khớp scripts/run-*.ps1: discovery & api-gateway không load .env; còn lại load .env ở ROOT.
# Luôn exec qua bash (spring-boot-terminal-run.sh) vì Terminal mặc định zsh — tránh lệch với PowerShell.
run_if_dir() {
  local subdir="$1"
  local echo_line="$2"
  local use_env="${3:-1}"
  if [[ ! -d "$ROOT/$subdir" ]]; then
    echo "[bỏ qua] $subdir — không có thư mục trong repo."
    return 1
  fi
  local runner="$ROOT/scripts/spring-boot-terminal-run.sh"
  local env_prefix=""
  [[ -n "${TF_JAVA_HOME}" ]] && env_prefix=$(printf 'export JAVA_HOME=%q; ' "${TF_JAVA_HOME}")
  # printf %q: an toàn khi echo_line hoặc đường dẫn có ký tự đặc biệt
  local inner="${env_prefix}exec /bin/bash --noprofile --norc $(printf '%q' "${runner}") $(printf '%q' "${ROOT}") $(printf '%q' "${subdir}") $(printf '%q' "${echo_line}") $(printf '%q' "${use_env}")"
  local esc
  esc="$(trustfund_escape_for_osascript "${inner}")"
  osascript -e "tell application \"Terminal\" to do script \"${esc}\""
  return 0
}

echo "Opening Terminal windows for all 9 services (theo scripts/run-all-services.ps1)..."
echo "Each service runs in a separate window (chỉ các module có trong repo)."
echo ""

# 1 Discovery
if run_if_dir "discovery-server" "Starting Discovery Server (port 8761)..." 0; then
  sleep "$STEP_SLEEP_SEC"
fi

# 2 API Gateway — scripts/run-api-gateway.ps1 không load .env
if run_if_dir "api-gateway" "Starting API Gateway (port 8080)..." 0; then
  sleep "$STEP_SLEEP_SEC"
fi

# 3 Identity
if run_if_dir "identity-service" "Starting Identity Service (port 8081)..."; then
  sleep "$STEP_SLEEP_SEC"
fi

# 4 Campaign
if run_if_dir "campaign-service" "Starting Campaign Service (port 8082)..."; then
  sleep "$STEP_SLEEP_SEC"
fi

# 5 Media
if run_if_dir "media-service" "Starting Media Service (port 8083)..."; then
  sleep "$STEP_SLEEP_SEC"
fi

# 6 Chat
if run_if_dir "chat-service" "Starting Chat Service (port 8086)..."; then
  sleep "$STEP_SLEEP_SEC"
fi

# 7 Payment
if run_if_dir "payment-service" "Starting Payment Service (port 8087)..."; then
  sleep "$STEP_SLEEP_SEC"
fi

# 8 Notification
if run_if_dir "notification-service" "Starting Notification Service (port 8088)..."; then
  sleep "$STEP_SLEEP_SEC"
fi

# 9 Audit
run_if_dir "audit-service" "Starting Audit Service (port 8089)..." || true

echo ""
echo "Done. (Port list giống run-all-services.ps1.)"
echo "  1. Discovery Server (8761)"
echo "  2. API Gateway (8080)"
echo "  3. Identity Service (8081)"
echo "  4. Campaign Service (8082)"
echo "  5. Media Service (8083)"
echo "  6. Chat Service (8086)"
echo "  7. Payment Service (8087)"
echo "  8. Notification Service (8088)"
echo "  9. Audit Service (8089)"
echo "Các dòng [bỏ qua] ở trên là module chưa có trong clone hiện tại."
