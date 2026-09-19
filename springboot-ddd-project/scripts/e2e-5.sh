#!/usr/bin/env bash
# 阶段5：支付、物流、签收及全额退款。
set -euo pipefail
exec bash "$(dirname "$0")/e2e.sh" --stage 5 "$@"
