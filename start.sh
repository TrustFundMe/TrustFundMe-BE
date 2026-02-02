#!/bin/bash
# Mac/Linux equivalent of start.bat
# Usage: ./start.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$SCRIPT_DIR/scripts/run-all-services.sh"
