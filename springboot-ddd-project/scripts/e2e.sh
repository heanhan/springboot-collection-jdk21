#!/usr/bin/env bash
# 仅用于隔离的本地测试库：创建测试用户/订单；完整链路实际消耗一件示例库存。
# 不输出 Token，不自动重试写请求，不删除订单/流水，不回补已发货商品库存。
set -euo pipefail
set +x

STAGE=6
CHECK_ONLY=false
while (($#)); do
  case "$1" in
    --stage) STAGE="${2:?缺少阶段编号}"; shift 2 ;;
    --check) CHECK_ONLY=true; shift ;;
    --help)
      printf '%s\n' '用法: bash scripts/e2e.sh [--stage 2|3|4|5|6] [--check]' \
        '依赖 bash/curl/jq；默认测试完整链路。--check 只检查服务健康，不写入数据。' \
        '阶段5/6要求支付服务 PAYMENT_MOCK_ENABLED=true；请使用独立测试环境。' \
        '可配置 AUTH_URL/USER_URL/PRODUCT_URL/INVENTORY_URL/ORDER_URL/PAYMENT_URL/LOGISTICS_URL/CART_URL、SKU_ID、E2E_TIMEOUT。'
      exit 0 ;;
    *) printf '未知参数: %s\n' "$1" >&2; exit 2 ;;
  esac
done
[[ "$STAGE" =~ ^[2-6]$ ]] || { printf '阶段必须为 2～6\n' >&2; exit 2; }
for cmd in curl jq; do command -v "$cmd" >/dev/null || { printf '缺少命令: %s\n' "$cmd" >&2; exit 2; }; done
AUTH_URL=${AUTH_URL:-http://localhost:8081}
USER_URL=${USER_URL:-http://localhost:8082}
PRODUCT_URL=${PRODUCT_URL:-http://localhost:8083}
INVENTORY_URL=${INVENTORY_URL:-http://localhost:8084}
ORDER_URL=${ORDER_URL:-http://localhost:8085}
PAYMENT_URL=${PAYMENT_URL:-http://localhost:8086}
LOGISTICS_URL=${LOGISTICS_URL:-http://localhost:8087}
CART_URL=${CART_URL:-http://localhost:8088}
SKU_ID=${SKU_ID:-2000}
WAREHOUSE_ID=${WAREHOUSE_ID:-1}
E2E_TIMEOUT=${E2E_TIMEOUT:-90}
[[ "$E2E_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || { printf 'E2E_TIMEOUT 必须为正整数\n' >&2; exit 2; }
TOKEN=''
HTTP=''
BODY=''
log() { printf '%s\n' "$*" >&2; }
fail() { log "失败: $*"; exit 1; }

request() {
  local method=$1 url=$2 payload=${3:-} response
  local args=(-sS --connect-timeout 3 --max-time 15 -X "$method" -H 'Content-Type: application/json')
  [[ -z "$TOKEN" ]] || args+=(-H "Authorization: Bearer $TOKEN")
  [[ -z "$payload" ]] || args+=(--data "$payload")
  if ! response=$(curl "${args[@]}" -w $'\n%{http_code}' "$url"); then
    HTTP=000; BODY='{}'; return 1
  fi
  HTTP=${response##*$'\n'}
  BODY=${response%$'\n'*}
}
response_ok() { [[ "$HTTP" == 2?? ]] && jq -e '.code == "0000"' >/dev/null 2>&1 <<<"$BODY"; }
api() {
  request "$@" || fail "无法连接 $2"
  response_ok || fail "$1 $2 HTTP=$HTTP code=$(jq -r '.code // "unknown"' <<<"$BODY" 2>/dev/null || true)"
  jq -c '.data' <<<"$BODY"
}
assert_json() { jq -e "$2" >/dev/null <<<"$1" || fail "$3"; }
expect_rejected() {
  request "$@" || fail "预期业务拒绝，但请求未送达 $2"
  [[ "$HTTP" != 5?? && "$HTTP" != 404 ]] || fail "服务错误或端点不存在，不能算作预期拒绝: $2 HTTP=$HTTP"
  jq -e '.code != null and .code != "0000"' >/dev/null 2>&1 <<<"$BODY" || fail "本应拒绝的请求未被明确拒绝: $2"
}
poll() {
  local url=$1 predicate=$2 deadline=$((SECONDS + E2E_TIMEOUT))
  while ((SECONDS < deadline)); do
    if request GET "$url" && response_ok && jq -e ".data | ($predicate)" >/dev/null 2>&1 <<<"$BODY"; then
      jq -c '.data' <<<"$BODY"; return 0
    fi
    sleep 1
  done
  fail "等待超时: $url，期望 $predicate，最后 HTTP=$HTTP；请检查服务日志、Outbox 和 MQ 消费状态"
}
health() {
  request GET "$1/actuator/health" || fail "服务尚未启动: $1"
  [[ "$HTTP" == 200 ]] && jq -e '.status == "UP"' >/dev/null <<<"$BODY" || fail "服务不健康: $1 HTTP=$HTTP"
}

if [[ "$STAGE" == 3 ]]; then
  health "$PRODUCT_URL"; health "$INVENTORY_URL"
else
  health "$USER_URL"; health "$AUTH_URL"
  if ((STAGE >= 4)); then health "$PRODUCT_URL"; health "$INVENTORY_URL"; health "$ORDER_URL"; fi
  if ((STAGE >= 5)); then health "$PAYMENT_URL"; health "$LOGISTICS_URL"; fi
  if ((STAGE >= 6)); then health "$CART_URL"; fi
fi
if "$CHECK_ONLY"; then log '服务健康检查通过（未执行业务写入）'; exit 0; fi
RUN_ID="$(date +%s)_${RANDOM}"

# 阶段3：直接调用发布契约，重复预占/释放不改变最终库存。
if [[ "$STAGE" == 3 ]]; then
  sku=$(api GET "$PRODUCT_URL/product/internal/sku/$SKU_ID")
  assert_json "$sku" '.status == "ON_SALE"' '示例 SKU 未上架'
  stock_url="$INVENTORY_URL/inventory/internal/stock/$WAREHOUSE_ID/$SKU_ID"
  before=$(api GET "$stock_url")
  assert_json "$before" '.availableQty >= 1' '测试库存不足'
  biz="e2e_$RUN_ID"
  body=$(jq -nc --arg biz "$biz" --arg sku "$SKU_ID" --arg wh "$WAREHOUSE_ID" '{bizNo:$biz,items:[{skuId:$sku,warehouseId:$wh,quantity:1}]}')
  api POST "$INVENTORY_URL/inventory/internal/stock/lock" "$body" >/dev/null
  api POST "$INVENTORY_URL/inventory/internal/stock/lock" "$body" >/dev/null
  locked=$(api GET "$stock_url")
  assert_json "$locked" ".availableQty == ($(jq '.availableQty' <<<"$before") - 1) and .lockedQty == ($(jq '.lockedQty' <<<"$before") + 1)" '重复预占导致库存不一致'
  release=$(jq -nc --arg biz "$biz" '{bizNo:$biz,reason:"E2E"}')
  api POST "$INVENTORY_URL/inventory/internal/stock/release" "$release" >/dev/null
  api POST "$INVENTORY_URL/inventory/internal/stock/release" "$release" >/dev/null
  after=$(api GET "$stock_url")
  assert_json "$after" ".availableQty == $(jq '.availableQty' <<<"$before") and .lockedQty == $(jq '.lockedQty' <<<"$before")" '释放后库存未恢复'
  expect_rejected POST "$INVENTORY_URL/inventory/internal/stock/lock" "$body"
  log '阶段3通过：商品契约、预占幂等、释放幂等、取消屏障'; exit 0
fi

# 每次创建独立账号，等待跨上下文建档后登录。
username="e2e_$RUN_ID"
password="E2e_${RUN_ID}!"
mobile="139$(printf '%08d' "$(( ($(date +%s) + RANDOM) % 100000000 ))")"
register=$(jq -nc --arg u "$username" --arg p "$password" --arg m "$mobile" '{username:$u,password:$p,mobile:$m,nickname:"联调测试"}')
user_id=$(api POST "$AUTH_URL/auth/register" "$register" | jq -er '.')
poll "$USER_URL/user/internal/$user_id" '.userId != null' >/dev/null
login_body=$(jq -nc --arg u "$username" --arg p "$password" '{username:$u,password:$p,device:"e2e"}')
session=$(api POST "$AUTH_URL/auth/login" "$login_body")
TOKEN=$(jq -er '.accessToken' <<<"$session")
refresh=$(jq -er '.refreshToken' <<<"$session")
api GET "$AUTH_URL/auth/userinfo" >/dev/null
refresh_body=$(jq -nc --arg t "$refresh" '{refreshToken:$t}')
session=$(api POST "$AUTH_URL/auth/refresh" "$refresh_body")
TOKEN=$(jq -er '.accessToken' <<<"$session")
expect_rejected POST "$AUTH_URL/auth/refresh" "$refresh_body"
log '注册、用户建档、登录、Token 刷新及旧 RefreshToken 拒绝通过'
logout() {
  api POST "$AUTH_URL/auth/logout" >/dev/null
  expect_rejected GET "$AUTH_URL/auth/userinfo"
  TOKEN=''
}
if [[ "$STAGE" == 2 ]]; then logout; log '阶段2通过：认证及注销失效'; exit 0; fi

sku=$(api GET "$PRODUCT_URL/product/internal/sku/$SKU_ID")
assert_json "$sku" '.status == "ON_SALE"' '示例 SKU 未上架'
stock_url="$INVENTORY_URL/inventory/stocks/sku/$SKU_ID"
baseline=$(api GET "$stock_url" | jq '{available:map(.availableQty)|add,locked:map(.lockedQty)|add}')
assert_json "$baseline" '.available >= 1' '测试库存不足'
order_body=$(jq -nc --arg sku "$SKU_ID" '{items:[{skuId:$sku,quantity:1}],shippingAddress:{province:"北京市",city:"北京市",district:"朝阳区",detail:"示例路1号",zipCode:"100000",receiver:"测试用户",mobile:"13800000000"},remark:"E2E"}')

if ((STAGE >= 6)); then
  api POST "$CART_URL/cart/items" "$(jq -nc --arg sku "$SKU_ID" '{skuId:$sku,quantity:1}')" >/dev/null
  api PUT "$CART_URL/cart/items/$SKU_ID" '{"quantity":1,"checked":true}' >/dev/null
  preview=$(api GET "$CART_URL/cart/preview")
  assert_json "$preview" '.items|length == 1' '购物车预览条目错误'
fi

# 下单/取消验证：取消通过 MQ 释放，必须等待释放完成才进入下一用例。
cancel_id=$(api POST "$ORDER_URL/orders" "$order_body" | jq -er '.')
log "取消用例 orderId=$cancel_id"
api POST "$ORDER_URL/orders/$cancel_id/cancel" '{"reason":"E2E取消"}' >/dev/null
api POST "$ORDER_URL/orders/$cancel_id/cancel" '{"reason":"重复取消"}' >/dev/null
poll "$ORDER_URL/orders/$cancel_id" '.status == "CANCELLED"' >/dev/null
poll "$stock_url" "(map(.availableQty)|add) == $(jq '.available' <<<"$baseline") and (map(.lockedQty)|add) == $(jq '.locked' <<<"$baseline")" >/dev/null
if ((STAGE >= 5)); then poll "$PAYMENT_URL/payment/by-order/$cancel_id" '.status == "CLOSED"' >/dev/null; fi
if [[ "$STAGE" == 4 ]]; then logout; log '阶段4通过：下单、重复取消、库存释放'; exit 0; fi

# 完整履约：金额取订单快照，绝不硬编码价格；回调执行两次验证幂等。
order_id=$(api POST "$ORDER_URL/orders" "$order_body" | jq -er '.')
log "履约用例 orderId=$order_id（本次会消耗一件库存）"
order=$(api GET "$ORDER_URL/orders/$order_id")
if ((STAGE >= 6)); then
  assert_json "$order" ".payAmount == $(jq '.payAmount' <<<"$preview")" '购物车与下单价格不一致（请确保测试期间商品价格不变）'
fi
payment=$(poll "$PAYMENT_URL/payment/by-order/$order_id" '.status == "PENDING"')
payment_id=$(jq -er '.paymentId' <<<"$payment")
api POST "$PAYMENT_URL/payment/$payment_id/pay" '{"channel":"MOCK"}' >/dev/null
callback=$(jq -nc --arg id "$payment_id" --arg trade "e2e_$RUN_ID" --argjson amount "$(jq '.payAmount' <<<"$order")" '{paymentId:$id,amount:$amount,tradeNo:$trade}')
api POST "$PAYMENT_URL/payment/mock/callback" "$callback" >/dev/null
api POST "$PAYMENT_URL/payment/mock/callback" "$callback" >/dev/null
shipments=$(poll "$LOGISTICS_URL/logistics/by-order/$order_id" 'length == 1')
shipment_id=$(jq -er '.[0].shipmentId' <<<"$shipments")
poll "$ORDER_URL/orders/$order_id" '.status == "SHIPPED" or .status == "COMPLETED"' >/dev/null
api POST "$LOGISTICS_URL/logistics/$shipment_id/deliver" >/dev/null
api POST "$LOGISTICS_URL/logistics/$shipment_id/deliver" >/dev/null
poll "$ORDER_URL/orders/$order_id" '.status == "COMPLETED"' >/dev/null
poll "$stock_url" "(map(.availableQty)|add) == ($(jq '.available' <<<"$baseline") - 1) and (map(.lockedQty)|add) == $(jq '.locked' <<<"$baseline")" >/dev/null

refund=$(api POST "$PAYMENT_URL/payment/$payment_id/refund" '{"reason":"E2E全额退款"}')
repeat_refund=$(api POST "$PAYMENT_URL/payment/$payment_id/refund" '{"reason":"重复退款"}')
[[ $(jq -r '.refundId' <<<"$refund") == $(jq -r '.refundId' <<<"$repeat_refund") ]] || fail '重复退款创建了不同退款单'
poll "$ORDER_URL/orders/$order_id" '.status == "REFUNDED"' >/dev/null
poll "$PAYMENT_URL/payment/by-order/$order_id" '.status == "REFUNDED"' >/dev/null
if ((STAGE >= 6)); then
  api DELETE "$CART_URL/cart/items" >/dev/null
  empty_cart=$(api GET "$CART_URL/cart/items")
  assert_json "$empty_cart" 'length == 0' '购物车未清空'
fi
logout
log '通过：下单、重复回调、实扣、发货、重复签收、退款幂等。已发货库存须退货验收后入库，不自动回补。'
