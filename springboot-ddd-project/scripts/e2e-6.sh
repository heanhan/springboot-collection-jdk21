#!/usr/bin/env bash
# 阶段6：含购物车的完整链路。
set -euo pipefail
exec bash "$(dirname "$0")/e2e.sh" --stage 6 "$@"
