# Game Shop · PayPal 直连游戏道具商城

基于设计文档实现的首期前后端垂直切片。当前代码可以作为本地开发和 PayPal Sandbox 联调原型；它不是开箱即用的生产系统。生产上线前必须补齐 PayPal 商户配置、云资源、密钥托管、外部服务适配、游戏销售授权、人工发货来源、合规和全量验收。

## 技术范围

- Java 21、Spring Boot 3.x、Maven
- MyBatis、MySQL 8.0、Flyway
- PayPal REST API 适配器，使用环境变量配置 Sandbox 凭证
- 订单会话、PayPal Order/Capture、Webhook 幂等入库
- 人工发货任务领取、完成、复核和超时回收
- 管理员退款约束和退款幂等
- Spring Security + JWT 管理员认证，支持 `ADMIN`、`OPERATOR`、`REVIEWER` 角色
- 游戏、商品、服务器目录管理和公开目录查询
- 发货、退款、目录和 Webhook 重放操作审计
- Webhook 查询、重放，支付退款和争议状态同步
- 订单过期处理和失败支付重试
- 事务 Outbox 事件记录、人工重试和未来 RocketMQ 发布接口
- 发货凭证对象存储上传意图和附件元数据
- 发货凭证确认、任务状态查询和被驳回任务重新排队
- 本地订单/支付/发货/Webhook/Outbox 对账摘要及按币种影响金额汇总
- Actuator health/info/metrics、Dockerfile 和 GitHub Actions 校验配置
- 可配置基础风控：高金额/重复购买记录风险并进入人工审核
- React/TypeScript 响应式商城前端源码，覆盖 PC/H5 商品、结算、订单查询、运营台和管理员商品目录管理

## 重要边界

- 不保存或接收银行卡号。
- 不接入游戏 API、服务器插件或激活码库存。
- 不包含真实 OSS 上传、RocketMQ、MFA 和生产级身份提供商接入。
- OSS 当前只返回未配置的上传意图，RocketMQ 当前只保留 Outbox 和发布接口，未连接真实云服务。
- 风控阈值是配置项，当前不自动拒绝支付；风险驳回后发货任务保持人工复核状态。
- JWT 密钥和管理员密码必须通过环境变量/安全配置注入，仓库不提供默认管理员账号。
- 生产上线前必须完成游戏销售授权、PayPal 商户审核、税务和数据合规评估。

## 核心接口

```text
POST /api/v1/checkout/sessions
POST /api/v1/checkout/sessions/{sessionId}/payments/paypal/order
POST /api/v1/payments/paypal/orders/{paypalOrderId}/capture
GET  /api/v1/orders/{orderNo}/status
POST /api/v1/webhooks/paypal
POST /api/v1/admin/auth/login
GET  /api/v1/catalog/games
GET  /api/v1/catalog/products
GET  /api/v1/catalog/servers
POST /api/v1/admin/catalog/games
POST /api/v1/admin/catalog/products
POST /api/v1/admin/catalog/servers
POST /api/v1/admin/fulfillments/{id}/claim
POST /api/v1/admin/fulfillments/{id}/complete
POST /api/v1/admin/fulfillments/{id}/review
GET  /api/v1/admin/fulfillments?status=MANUAL_PENDING
POST /api/v1/admin/fulfillments/{id}/requeue
POST /api/v1/orders/{orderNo}/refunds
GET  /api/v1/admin/webhooks/paypal
POST /api/v1/admin/webhooks/paypal/{providerEventId}/replay
POST /api/v1/admin/orders/expire-pending
POST /api/v1/admin/orders/{orderNo}/retry-payment
POST /api/v1/admin/orders/{orderNo}/risk/review
GET  /api/v1/admin/audits
POST /api/v1/admin/fulfillments/{fulfillmentId}/attachments/upload-intent
POST /api/v1/admin/fulfillments/{fulfillmentId}/attachments/{attachmentId}/confirm
GET  /api/v1/admin/fulfillments/{fulfillmentId}/attachments
GET  /api/v1/admin/reconciliation/summary
GET  /api/v1/admin/outbox
POST /api/v1/admin/outbox/{id}/retry
```

游客创建订单时，服务端返回 `accessToken`。查询订单状态时必须通过 `X-Order-Access-Token` 传递该令牌。订单创建、PayPal Order、Capture 和退款均要求稳定的 `Idempotency-Key`。

## 配置

使用 `src/main/resources/application.yml` 中的环境变量占位符：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
PAYPAL_ENV=sandbox
PAYPAL_BASE_URL=https://api-m.sandbox.paypal.com
PAYPAL_CLIENT_ID
PAYPAL_CLIENT_SECRET
PAYPAL_WEBHOOK_ID
ORDER_ACCESS_TOKEN_SECRET
JWT_SECRET
JWT_ISSUER
JWT_EXPIRATION_SECONDS
RISK_HIGH_AMOUNT_MINOR
RISK_REPEAT_WINDOW_MINUTES
RISK_REPEAT_LIMIT
OSS_PROVIDER
OSS_BUCKET
OSS_ENDPOINT
OUTBOX_PUBLISHER
```

本地连接 PayPal Sandbox 时，后端至少配置 `PAYPAL_CLIENT_ID`、`PAYPAL_CLIENT_SECRET`；启用 Webhook 签名校验时再配置 `PAYPAL_WEBHOOK_ID`。前端 `web/.env` 只放公开的 Client ID，不得放 Client Secret：

```text
# backend process
PAYPAL_ENV=sandbox
PAYPAL_BASE_URL=https://api-m.sandbox.paypal.com
PAYPAL_CLIENT_ID=<Sandbox REST app client id>
PAYPAL_CLIENT_SECRET=<Sandbox REST app secret>
PAYPAL_WEBHOOK_ID=<Sandbox webhook id>

# web/.env
VITE_PAYPAL_CLIENT_ID=<same Sandbox REST app client id>
VITE_PAYPAL_SDK_URL=https://www.sandbox.paypal.com/web-sdk/v6/core
```

当前页面的支付顺序是：服务端创建本地订单 → 服务端创建 PayPal Order → 浏览器加载 PayPal Sandbox JS SDK 并由买家批准 → 服务端 Capture → Webhook/订单状态对账。未配置前端 Client ID 时按钮不会伪造支付；未配置后端 Client Secret 时服务端会返回配置错误。真实沙盒测试还需要 PayPal Sandbox 商户账号和买家账号。

数据库表结构由 Flyway migration 自动管理；本仓库不包含任何真实密钥、PayPal 账户信息或生产配置。

## 前端开发

前端位于 `web/`，采用一套 React/TypeScript 响应式页面适配 PC 与 H5：

```bash
cd web
npm install
npm run dev
```

Vite 开发服务器默认运行在 `http://localhost:5173`，`/api` 请求代理到本地 Spring Boot `http://localhost:8080`。
商城页面后端目录 API 不可用时会明确显示 Demo fallback，仅用于页面浏览和交互预览，不会触发真实支付。导航中的“商品后台”需要管理员账号登录，Token 只保存在当前页面内存中；后台新增/编辑会调用 `/api/v1/admin/catalog/*`，后端不可用或权限不足时不会伪造保存结果。

PayPal 浏览器 SDK 使用官方 Sandbox v6 地址，后端创建订单和 Capture 仍通过本项目服务端完成；不要从浏览器直接调用 PayPal Client Secret。

无需安装前端依赖时，可使用 `python3 -m http.server 4173` 打开 `http://localhost:4173/preview.html` 查看无依赖静态预览。

## 本地部署与启动手册

### 1. 本地依赖

| 依赖 | 要求 | 用途 |
|---|---|---|
| JDK | Java 21 | 编译和运行 Spring Boot |
| Maven | 3.9+ | 后端构建；本机可直接使用 `/Users/xujiucheng/Desktop/vibecoding/apache-maven-3.9.10/bin/mvn` |
| MySQL | 8.0+ | 订单、支付、商品、Webhook、发货和审计数据 |
| Node.js | 20+，建议 22/24 | 前端开发和构建 |
| npm | 随 Node.js 安装 | 安装前端依赖 |
| PayPal Sandbox | 可选 | 真实 Sandbox Order、批准、Capture、Webhook 联调 |

检查版本：

```bash
java -version
/Users/xujiucheng/Desktop/vibecoding/apache-maven-3.9.10/bin/mvn -version
node --version
npm --version
mysql --version
```

### 2. 创建本地 MySQL 数据库

方式 A：使用本机 MySQL：

```bash
mysql -h127.0.0.1 -uroot -p
```

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS game_shop
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'game_shop'@'127.0.0.1' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON game_shop.* TO 'game_shop'@'127.0.0.1';
FLUSH PRIVILEGES;
```

方式 B：使用 Docker 启动一次性本地 MySQL：

```bash
docker run --name game-shop-mysql \
  -e MYSQL_DATABASE=game_shop \
  -e MYSQL_USER=game_shop \
  -e MYSQL_PASSWORD=change-me \
  -e MYSQL_ROOT_PASSWORD=local-root-change-me \
  -p 3306:3306 \
  -d mysql:8.0
```

Spring Boot 启动时会自动执行 `src/main/resources/db/migration/` 下的 Flyway migration。不要在测试或生产环境手工修改已执行的 migration 文件。

### 3. 启动后端

在项目根目录执行。下面的变量只适用于本地开发，生产环境必须改为密钥托管服务注入的值：

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/game_shop?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
export DB_USERNAME='game_shop'
export DB_PASSWORD='change-me'
export ORDER_ACCESS_TOKEN_SECRET='local-order-access-token-secret-change-me'
export JWT_SECRET='local-dev-jwt-secret-change-me-please-32-bytes'
export JWT_ISSUER='game-shop-local'
export JWT_EXPIRATION_SECONDS='3600'
export PAYPAL_ENV='sandbox'
export PAYPAL_BASE_URL='https://api-m.sandbox.paypal.com'

/Users/xujiucheng/Desktop/vibecoding/apache-maven-3.9.10/bin/mvn spring-boot:run
```

后端默认地址：`http://127.0.0.1:8080`。

验证：

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8080/api/v1/catalog/games
curl http://127.0.0.1:8080/api/v1/catalog/products
```

应看到健康状态 `UP`。没有配置 PayPal Client ID/Secret 时，订单创建仍可用于本地流程测试，但调用 PayPal Order 会返回“PayPal credentials are not configured”，不会产生真实扣款。

### 4. 初始化管理员账号

Flyway 只创建 `admin_users` 和 `admin_user_roles` 表，不创建默认管理员，也没有开放管理员创建接口。首次部署必须由受控的初始化脚本或数据库运维流程写入一个 BCrypt 密码哈希，并同时写入 `ADMIN`、`OPERATOR` 或 `REVIEWER` 角色。

示例结构如下，`<bcrypt-hash>` 必须通过受信任的 BCrypt 工具生成，不能填写明文密码：

```sql
INSERT INTO admin_users (id, username, password_hash, status, created_at, updated_at)
VALUES ('<32-char-id>', '<admin-username>', '<bcrypt-hash>', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO admin_user_roles (admin_id, role_code)
VALUES ('<32-char-id>', 'ADMIN');
```

生产环境不要把管理员密码、SQL 初始化脚本中的真实哈希或 JWT 密钥提交到代码仓库；初始化后应限制数据库账号权限并记录审计。

### 5. 启动前端

在项目根目录执行：

```bash
cp web/.env.example web/.env
npm --prefix web ci
npm --prefix web run dev -- --host 127.0.0.1
```

前端默认地址：`http://127.0.0.1:5173`。开发环境的 Vite 代理会把 `/api` 转发到 `http://localhost:8080`；这个代理只用于开发，不能作为生产反向代理。

`web/.env` 示例：

```text
# 不配置时仍可浏览页面和创建本地订单，但不会显示真实 PayPal 支付按钮
VITE_PAYPAL_CLIENT_ID=<PayPal Sandbox REST app client id>
VITE_PAYPAL_SDK_URL=https://www.sandbox.paypal.com/web-sdk/v6/core

# 只有前后端不在同一域名时才需要；跨域时还要补充后端 CORS 和 HTTPS 配置
# VITE_API_BASE=https://api.example.com/api/v1
```

PayPal Client Secret 只能放在后端环境变量或密钥服务中，禁止写入 `web/.env`、浏览器代码、Docker 镜像和日志。

### 6. 本地构建和检查

```bash
/Users/xujiucheng/Desktop/vibecoding/apache-maven-3.9.10/bin/mvn clean test
npm --prefix web run build
npm --prefix web audit --omit=dev --audit-level=high
```

`mvn test` 当前主要验证编译和项目装配；真实 PayPal Sandbox、Webhook、浏览器和移动端验收仍需按“测试和上线清单”执行。

### 7. 本地 PayPal Sandbox 联调

在 PayPal Developer 控制台创建 Sandbox REST App，并分别准备商户账号和买家账号。后端启动前配置：

```bash
export PAYPAL_ENV='sandbox'
export PAYPAL_BASE_URL='https://api-m.sandbox.paypal.com'
export PAYPAL_CLIENT_ID='<sandbox-client-id>'
export PAYPAL_CLIENT_SECRET='<sandbox-client-secret>'
export PAYPAL_WEBHOOK_ID='<sandbox-webhook-id>'
```

前端 `web/.env` 配置同一个 Sandbox Client ID 后，重启 Vite。浏览器流程为：创建本地订单 → 创建 PayPal Order → 买家批准 → 服务端 Capture → PayPal Webhook/订单状态对账。真实 Webhook 必须使用 PayPal 可以访问的 HTTPS 地址；纯 `localhost` 只能完成没有公网回调的局部测试。

## 生产上线需要购买和配置什么

### 1. 推荐的生产拓扑

```text
用户浏览器
    │ HTTPS
域名/DNS ── CDN/WAF（可选）── ALB/SLB
                              ├── 前端静态站点或 Nginx
                              └── 2 个以上 Spring Boot 实例（ACK 或 ECS）
                                      │ 私网
                                      ├── RDS MySQL 高可用
                                      ├── OSS 私有 Bucket（发货凭证）
                                      ├── RocketMQ（Outbox 异步发布）
                                      └── NAT Gateway ── PayPal Live API
监控：SLS 日志 + CloudMonitor 告警 + 数据库备份/KMS 密钥管理
```

首个正式版本建议前后端使用同一主域名，通过 ALB/Nginx 将 `/api/` 反向代理到 Spring Boot，避免浏览器跨域和 PayPal 回调域名配置复杂化。

### 2. 服务采购清单

| 类别 | 推荐服务 | 最低生产配置建议 | 必须配置 |
|---|---|---|---|
| 域名/DNS | 域名注册商 + 阿里云 DNS | `shop.example.com`、`api.example.com` 或同域名路径 | DNS、解析、域名实名/备案按实际地域和主体办理 |
| 计算 | ACK 托管集群/Serverless 或 ECS | 至少 2 个应用副本；先从 2 核 4 GB 级别压测，不能凭经验保证规格 | VPC、vSwitch、安全组、滚动发布、健康检查 |
| 负载均衡 | ALB/SLB | HTTPS 443，转发到应用 8080 | TLS 证书、HTTP 到 HTTPS 跳转、空闲超时、访问日志 |
| 数据库 | ApsaraDB RDS MySQL 高可用 | MySQL 8.0、自动备份、跨可用区/高可用系列 | 私网访问、白名单、备份保留、慢查询、账号最小权限 |
| 文件存储 | OSS | 私有 Bucket，发货凭证不公开读 | RAM 最小权限、生命周期、版本/删除保护、服务端加密 |
| 消息队列 | RocketMQ Serverless 或托管 RocketMQ | 用于 Outbox 发布、重试和异步任务 | Topic、Group、ACL、重试/DLQ、监控 |
| 出网 | NAT Gateway + EIP | 应用私网出网访问 PayPal | 固定出口、路由表、流量告警 |
| 密钥 | KMS/Secrets Manager 或同等密钥服务 | 生产密钥不落盘 | 轮换、访问审计、按应用授权 |
| 日志 | SLS | 应用、Nginx/ALB、审计和 Webhook 日志 | 脱敏、保留周期、检索和告警 |
| 监控 | CloudMonitor/Prometheus/Grafana | API、JVM、数据库、PayPal、队列和业务指标 | 告警联系人、值班通知、SLO |
| 镜像/发布 | ACR + GitHub Actions 或企业 CI | 镜像按 Git SHA/版本标记 | 漏洞扫描、签名/权限、回滚版本 |
| 防护 | WAF、云防火墙（按风险选择） | 对公网 API 和管理端限流 | 管理端 IP/VPN 限制、WAF 规则、DDoS 评估 |

当前代码没有 Redis/Tair 配置或实际依赖，第一版不要为了“看起来完整”强制购买；只有确认需要分布式限流、缓存、短期状态或队列削峰后再引入。

### 3. 生产环境变量

以下变量应由 KMS/Secrets Manager 注入到容器或 ECS 进程，不能写入仓库、镜像或普通日志：

```text
# RDS MySQL
DB_URL=jdbc:mysql://<rds-private-endpoint>:3306/game_shop?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=true
DB_USERNAME=<least-privilege-db-user>
DB_PASSWORD=<secret>

# PayPal Live；Sandbox 与 Live 必须使用不同应用和凭据
PAYPAL_ENV=live
PAYPAL_BASE_URL=https://api-m.paypal.com
PAYPAL_CLIENT_ID=<live-client-id>
PAYPAL_CLIENT_SECRET=<live-client-secret>
PAYPAL_WEBHOOK_ID=<live-webhook-id>
PAYPAL_TOKEN_CACHE_SECONDS=300

# 应用安全密钥：随机生成，至少 32 字节；不同环境必须不同
ORDER_ACCESS_TOKEN_SECRET=<random-secret>
JWT_SECRET=<random-secret-at-least-32-bytes>
JWT_ISSUER=game-shop-prod
JWT_EXPIRATION_SECONDS=900

# 订单和风控
ORDER_EXPIRATION_MINUTES=30
RISK_HIGH_AMOUNT_MINOR=10000
RISK_REPEAT_WINDOW_MINUTES=60
RISK_REPEAT_LIMIT=3

# 外部服务。只有对应适配器完成并验收后才允许切换为真实值
OSS_PROVIDER=<implemented-oss-provider>
OSS_BUCKET=<private-bucket>
OSS_ENDPOINT=<oss-endpoint>
OUTBOX_PUBLISHER=<implemented-rocketmq-publisher>
```

特别注意：本仓库当前的 `UnconfiguredObjectStorageClient` 只返回“未配置”的上传意图，`UnconfiguredEventPublisher` 只抛出“RocketMQ 未配置”异常。仅设置 `OSS_*` 或 `OUTBOX_PUBLISHER` 变量不会自动获得真实 OSS/RocketMQ 能力；必须先实现并测试对应适配器，再在生产配置中启用。

### 4. PayPal Live 必须配置

生产 PayPal 不是把 Sandbox 地址替换成 Live 地址这么简单，至少需要：

1. 已验证的 PayPal Business 商户账户和收款主体。
2. Live REST App、Live Client ID、Live Client Secret。
3. 生产域名和 HTTPS Webhook，例如 `https://api.example.com/api/v1/webhooks/paypal`。
4. PayPal Webhook ID，并订阅本项目实际处理的事件：`PAYMENT.CAPTURE.COMPLETED`、`PAYMENT.CAPTURE.DENIED`、`PAYMENT.CAPTURE.DECLINED`、`PAYMENT.CAPTURE.REFUNDED`、`CUSTOMER.DISPUTE.CREATED`、`CUSTOMER.DISPUTE.RESOLVED`。
5. PayPal 账户、网站商品说明、退款/争议流程和客服联系方式通过商户审核。
6. 先用 Sandbox 完成订单、批准、Capture、取消、重复回调、退款和争议测试，再切换 Live。

Sandbox、Live 的 Client ID、Client Secret、Webhook ID 和回调地址必须严格隔离，不能混用。PayPal 官方生产环境使用 `https://api-m.paypal.com`，Webhook 要求公网 HTTPS 和稳定的 2xx 响应。

### 5. 前端生产部署

当前前端是 Vite 构建产物，不能把 `npm run dev` 或 `npm run preview` 当成生产服务。构建：

```bash
VITE_PAYPAL_CLIENT_ID='<live-client-id>' \
VITE_PAYPAL_SDK_URL='https://www.paypal.com/web-sdk/v6/core' \
npm --prefix web ci
npm --prefix web run build
```

生产可选两种方式：

- 推荐首版：Nginx/前端容器托管 `web/dist`，同一域名下将 `/api/` 反向代理到后端 8080。
- 静态托管：OSS + CDN 托管 `web/dist`，此时需要构建时设置 `VITE_API_BASE=https://api.example.com/api/v1`，并补充后端 CORS、HTTPS、Cookie/Token 和安全响应头配置。

无论采用哪种方式，前端只允许出现 PayPal Client ID，绝对不能出现 Client Secret、数据库密码、JWT 签名密钥或 OSS AccessKey。

### 6. 上线前发布顺序

```text
1. 完成游戏销售授权、商品来源、人工发货权限和经营主体确认
2. 创建 VPC、私网子网、安全组、RDS、OSS、RocketMQ、KMS、SLS、CloudMonitor
3. 在测试环境接入真实 OSS/RocketMQ 适配器并完成集成测试
4. 发布数据库 migration，创建最小权限 DB 用户和受控管理员账号
5. 部署后端和前端，配置域名、HTTPS、ALB/Nginx 和健康检查
6. 配置 PayPal Sandbox Webhook，完成 PC/H5、取消、重复点击、Capture、退款和回调测试
7. 灰度环境切换 Live 凭据前，复核所有密钥、日志脱敏、备份和回滚点
8. 配置 PayPal Live Webhook，执行小额真实交易和人工发货闭环
9. 核对 PayPal Order ID、Capture ID、订单、Webhook、发货任务、退款和对账记录
10. 开启业务告警和值班通知，再逐步放量
```

### 7. 生产验收清单

- [ ] PayPal Business 账户和 Live App 已验证，主体与网站/收款账户一致。
- [ ] 游戏销售授权、商品来源和人工发货权限有书面依据。
- [ ] RDS 已开启自动备份、恢复演练、私网访问和最小权限。
- [ ] 生产密钥由 KMS/Secrets Manager 注入，不在代码、镜像、日志和前端产物中。
- [ ] 前端重复点击不会产生重复支付；后端 `Idempotency-Key` 已验证。
- [ ] Capture 成功后只创建一份 `MANUAL_PENDING` 发货任务。
- [ ] PayPal Webhook 已验签、幂等、可重放，并能处理重复投递。
- [ ] 退款、争议、拒付、Capture 失败和超时重试已验证。
- [ ] 发货凭证真实上传到私有 OSS，完成后可审计和复核。
- [ ] Chrome、Safari、Edge、Firefox、iOS Safari、Android Chrome 已完成支付回归。
- [ ] 375px、390px、768px 视口无横向滚动，PayPal 弹窗/返回/刷新后订单状态可恢复。
- [ ] SLS、CloudMonitor、PayPal Webhook、ALB、RDS 和 JVM 告警已接收。
- [ ] 已验证数据库恢复、应用回滚、PayPal 重试和 Webhook 重放方案。

以上清单全部通过后，才可以把本项目从“内部 Sandbox 原型”定义为可发布的生产版本。阿里云服务规格和费用必须按实际地域、流量、可用区、备份、带宽和 SLA 使用官方计算器重新核价；PayPal 手续费、拒付/争议损失、税费、游戏授权费和人工运营成本不包含在云资源预算中。

## 官方文档与参考

- [PayPal Sandbox 测试](https://developer.paypal.com/sandbox-testing/overview/)
- [PayPal REST API 请求和环境地址](https://developer.paypal.com/api/make-api-requests)
- [PayPal 生产上线](https://developer.paypal.com/api/rest/production/)
- [PayPal Webhooks](https://developer.paypal.com/api/rest/webhooks/rest/)
- [PayPal JavaScript SDK v6 设置](https://developer.paypal.com/sdk/js/set-up/)
- [阿里云 RDS MySQL](https://help.aliyun.com/zh/rds/)
- [阿里云 ACK](https://help.aliyun.com/zh/ack/)
- [阿里云 OSS](https://help.aliyun.com/zh/oss/)
- [阿里云 RocketMQ](https://help.aliyun.com/zh/apsaramq-for-rocketmq/)
- [阿里云 KMS](https://help.aliyun.com/zh/kms/)
- [阿里云日志服务 SLS](https://help.aliyun.com/zh/sls/)

## GitHub/CSDN 演示说明

仓库可以通过 GitHub Pages 自动发布 `web/dist` 前端演示，工作流文件为 `.github/workflows/pages.yml`。首次使用时，在 GitHub 仓库的 `Settings → Pages → Source` 选择 `GitHub Actions`；推送到 `main` 后，工作流会自动构建并发布页面。

当前已发布：

- GitHub 源码仓库：<https://github.com/zx20170910/game-shop-paypal>
- GitHub Pages 前端演示：<https://zx20170910.github.io/game-shop-paypal/>

GitHub Pages 的地址通常是：

```text
https://<github-username>.github.io/<repository-name>/
```

该页面在没有配置 `VITE_API_BASE` 和后端服务时会使用前端 Demo fallback，因此可演示商品浏览、PC/H5 响应式布局、语言切换、下单交互和订单页面结构，但不会连接真实 MySQL、管理员 API、PayPal、Webhook、发货、退款或对账服务。要演示全部功能，仍必须把 Spring Boot、MySQL、OSS、消息队列和 PayPal Sandbox 部署到独立的 HTTPS 云环境，并通过环境变量/密钥服务配置，不能把 Secret 写入 GitHub Pages。

CSDN 适合发布项目介绍、部署教程、截图和 GitHub/在线演示链接；CSDN 首页地址不是本项目的运行环境，也不能替代后端服务器、MySQL 或 PayPal Webhook。发布 CSDN 文章时不要公开 Client Secret、数据库密码、JWT Secret、OSS AccessKey 或管理员凭据。
