# M2-11：OpenAPI与M2回归

返回 [实践计划与进度](../README.md)。

## 目标

M2 全部接口文档化分组，DoD 三条全量回归。

## 范围

- 新增资源按商品、购物车、订单、支付 Tag 分组并标注 Bearer 安全方案。
- 汇总 DoD 回归测试：下单原子性（订单/明细/库存一致或全不落）、下单幂等（同键一单）、非法状态迁移（支付/取消/发货的乱序全部被拒）。
- 整理可复现的接口验证命令清单。

## 学习点

OpenAPI 资源组织、里程碑 DoD 与测试的对应关系。

## 验收

1. Swagger UI 与 OpenAPI JSON 可访问，新接口分组清晰、安全标注正确。
2. DoD 三条各有对应自动化测试证明。
3. `./mvnw -pl services/user-service -am test` 通过。

## 不做

- 不开始 Redis、MQ、延时关单（M2.5 待拆卡）。
- 不动 user-client（M2 后端 DoD 通过后建 C 端接入里程碑）。

## 完成记录

日期：2026-09-20

提交：本次提交（`test: 完成M2接口文档与回归验收`）。

测试与接口证据：在 `boot-server/` 使用 Oracle JDK 25.0.4.1 执行 `./mvnw -q -pl services/user-service -am clean test`，29 份 Surefire 报告共 132 个测试，失败、错误、跳过均为 0；`git diff --check` 通过。

- OpenAPI：`GET /api/v3/api-docs` 与 `GET /api/swagger-ui/index.html` 均由真实 HTTP 测试验证为 200；文档版本更新为 M2。商品/SKU/库存归「商品」，购物车归「购物车」，本人/后台订单归「订单」，模拟支付归「支付」，全部 M2 operation 均声明 `bearerAuth`。
- DoD 原子性：双明细下单在任一 SKU 未配置库存时返回冲突，当前用户订单与明细均不落库，先执行的库存预扣回滚。
- DoD 幂等：同一用户以同一键和同一内容重复下单返回同一订单，数据库只有一单，锁定库存只增加一次。
- DoD 状态机：CANCELLED 后支付、PAID 后取消、PAID 直接确认收货、CREATED 直接发货均返回 `409/40900`；M2-08 至 M2-10 的测试另覆盖并发支付、重复取消、重复发货/确认与完整履约链。
- 静态值审计：OpenAPI 安全方案名仍由 `OpenApiConfig.BEARER_AUTHENTICATION` 单一持有；Tag 与 Controller 局部描述就近声明。本卡未新增部署配置、错误码、角色码、权限码或共享存储键。

### 可复现接口验证命令

先按 `boot-server/README.md` 配置 `JWT_SECRET_BASE64`、`BOOT_ADMIN_USERNAME`、`BOOT_ADMIN_PASSWORD` 并启动服务；以下命令需要 `curl`、`jq`，使用默认 MVC 前缀 `/api`：

```bash
API_BASE='http://localhost:8080/api'
USER_NAME="m2-user-$(date +%s)"
USER_PASSWORD='local-test-password'

curl -fsS "$API_BASE/v3/api-docs" | jq '.info.version, .components.securitySchemes.bearerAuth'
curl -fsS -o /dev/null -w '%{http_code}\n' "$API_BASE/swagger-ui/index.html"

curl -fsS -X POST "$API_BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_NAME\",\"email\":\"$USER_NAME@example.com\",\"password\":\"$USER_PASSWORD\"}"
USER_TOKEN=$(curl -fsS -X POST "$API_BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_NAME\",\"password\":\"$USER_PASSWORD\"}" | jq -r '.data.accessToken')
ADMIN_TOKEN=$(curl -fsS -X POST "$API_BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$BOOT_ADMIN_USERNAME\",\"password\":\"$BOOT_ADMIN_PASSWORD\"}" | jq -r '.data.accessToken')

SKU_CODE="M2-SKU-$(date +%s)"
PRODUCT=$(curl -fsS -X POST "$API_BASE/products" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"M2 验收商品 $SKU_CODE\",\"skus\":[{\"skuCode\":\"$SKU_CODE\",\"price\":1.00}]}")
PRODUCT_ID=$(printf '%s' "$PRODUCT" | jq -r '.data.id')
SKU_ID=$(printf '%s' "$PRODUCT" | jq -r '.data.skus[0].id')
curl -fsS -X PUT "$API_BASE/skus/$SKU_ID/price" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"price":2.00,"version":0}'
curl -fsS -X PUT "$API_BASE/products/$PRODUCT_ID/status" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"status":"ON_SALE"}'
curl -fsS "$API_BASE/products?page=1&size=100" -H "Authorization: Bearer $USER_TOKEN" | jq '.data'
curl -fsS "$API_BASE/products/$PRODUCT_ID" -H "Authorization: Bearer $USER_TOKEN" | jq '.data'
curl -fsS -X PUT "$API_BASE/stocks/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"total":10}'
curl -fsS "$API_BASE/stocks/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data'

curl -fsS -X POST "$API_BASE/cart/items" -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"skuId\":$SKU_ID,\"quantity\":1}"
CART=$(curl -fsS "$API_BASE/cart" -H "Authorization: Bearer $USER_TOKEN")
CART_ID=$(printf '%s' "$CART" | jq -r '.data[0].id')
curl -fsS -X PUT "$API_BASE/cart/items/$CART_ID" -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' -d '{"quantity":2}'
curl -fsS -X DELETE "$API_BASE/cart/items/$CART_ID" -H "Authorization: Bearer $USER_TOKEN"

ORDER=$(curl -fsS -X POST "$API_BASE/orders" -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idempotencyKey\":\"$(uuidgen)\",\"items\":[{\"skuId\":$SKU_ID,\"quantity\":1}]}")
ORDER_ID=$(printf '%s' "$ORDER" | jq -r '.data.id')
curl -fsS "$API_BASE/orders?page=1&size=10" -H "Authorization: Bearer $USER_TOKEN" | jq '.data'
curl -fsS "$API_BASE/orders/$ORDER_ID" -H "Authorization: Bearer $USER_TOKEN" | jq '.data'
curl -fsS -X POST "$API_BASE/orders/$ORDER_ID/pay" -H "Authorization: Bearer $USER_TOKEN" | jq '.data'
curl -fsS "$API_BASE/admin/orders?page=1&size=10" -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data'
curl -fsS -X POST "$API_BASE/admin/orders/$ORDER_ID/ship" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -fsS -X POST "$API_BASE/orders/$ORDER_ID/confirm" -H "Authorization: Bearer $USER_TOKEN"

CANCEL_ORDER=$(curl -fsS -X POST "$API_BASE/orders" -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idempotencyKey\":\"$(uuidgen)\",\"items\":[{\"skuId\":$SKU_ID,\"quantity\":1}]}")
CANCEL_ORDER_ID=$(printf '%s' "$CANCEL_ORDER" | jq -r '.data.id')
curl -fsS -X POST "$API_BASE/orders/$CANCEL_ORDER_ID/cancel" -H "Authorization: Bearer $USER_TOKEN"
```
