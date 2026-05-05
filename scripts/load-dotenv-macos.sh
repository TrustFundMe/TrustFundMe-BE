#!/usr/bin/env bash
# Khớp cách các script *.ps1 nạp .env: trim, gỡ ngoặc, map SHARED_*.
# Chuẩn hóa CRLF (\r) và BOM UTF-8 — tránh JDBC URL chứa ký tự "vô hình"
# khiến MySQL Connector báo "claims to not accept jdbcUrl".

trustfund_trim_lr() {
  local _x="$1"
  while [[ -n "${_x}" && "${_x}" == [[:space:]]* ]]; do
    _x="${_x#?}"
  done
  while [[ -n "${_x}" && "${_x}" == *[[:space:]] ]]; do
    _x="${_x%?}"
  done
  printf '%s' "${_x}"
}

trustfund_dotenv_load() {
  local env_file="${1:-}"

  [[ -n "${env_file}" && -f "${env_file}" ]] || return 0

  local line key value raw_key len first last t

  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"

    t="$(trustfund_trim_lr "${line}")"
    [[ -z "${t}" ]] && continue

    [[ "${t:0:1}" == '#' ]] && continue

    raw_key="${line%%=*}"
    [[ "${raw_key}" == "${line}" ]] && continue

    value="${line#*=}"

    key="$(trustfund_trim_lr "${raw_key}")"
    value="$(trustfund_trim_lr "${value}")"

    len=${#value}
    if [[ "${len}" -ge 2 ]]; then
      first="${value:0:1}"
      last="${value:len-1:1}"
      if [[ "${first}" == "'" && "${last}" == "'" ]]; then
        value="${value:1:len-2}"
      elif [[ "${first}" == '"' && "${last}" == '"' ]]; then
        value="${value:1:len-2}"
      fi
    fi

    # UTF-8 BOM ở đầu giá trị
    if [[ ${#value} -ge 3 && "${value:0:3}" == $'\xEF\xBB\xBF' ]]; then
      value="${value:3}"
    fi

    [[ -z "${key}" ]] && continue

    case "${key}" in
      SHARED_*)
        export "${key#SHARED_}"="${value}"
        ;;
      *)
        export "${key}"="${value}"
        ;;
    esac
  done <"${env_file}"

  return 0
}
