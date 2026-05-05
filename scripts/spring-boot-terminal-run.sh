#!/usr/bin/env bash
# Gọi từ Terminal (mặc định thường là zsh); luôn chạy bằng bash để trùng với *.ps1 + load-dotenv-macos.sh.
set -euo pipefail

ROOT="${1:?}"
SUBDIR="${2:?}"
MSG="${3:-}"
USE_ENV="${4:-1}"

HERE="$(cd "$(dirname "$0")" && pwd)"

if [[ "${USE_ENV}" == "1" ]] && [[ -f "${ROOT}/.env" ]]; then
  # shellcheck source=load-dotenv-macos.sh
  source "${HERE}/load-dotenv-macos.sh"
  trustfund_dotenv_load "${ROOT}/.env"
fi

cd "${ROOT}/${SUBDIR}"
[[ -n "${MSG}" ]] && echo "${MSG}"
exec mvn spring-boot:run
