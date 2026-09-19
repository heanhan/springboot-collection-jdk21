#!/usr/bin/env bash
# 阶段4：下单、取消及异步库存释放。
set -euo pipefail
exec bash "$(dirname "$0")/e2e.sh" --stage 4 "$@"
