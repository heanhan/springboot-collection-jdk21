#!/usr/bin/env bash
# 阶段2：认证及用户建档回归。
set -euo pipefail
exec bash "$(dirname "$0")/e2e.sh" --stage 2 "$@"
