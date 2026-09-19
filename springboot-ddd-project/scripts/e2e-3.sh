#!/usr/bin/env bash
# 阶段3：商品及库存契约回归。
set -euo pipefail
exec bash "$(dirname "$0")/e2e.sh" --stage 3 "$@"
