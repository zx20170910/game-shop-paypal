# Changelog

## 0.1.0-SNAPSHOT - 2026-09-19

### Added

- 初始化 Java 21 / Spring Boot 3.x Maven 后端项目。
- 增加 MySQL/Flyway 订单、支付、Webhook、人工发货和退款表结构。
- 增加游客订单访问令牌、国家/币种白名单和订单会话接口。
- 增加 PayPal Sandbox OAuth、Create Order、Capture、Refund 和 Webhook 验签适配器。
- 增加支付、Webhook、人工发货和退款幂等约束。
- 增加人工发货领取、完成、复核及超时回收接口。
- 增加 Spring Security + JWT 管理员登录和 `ADMIN`、`OPERATOR`、`REVIEWER` 角色权限。
- 增加游戏、商品、服务器目录管理和公开目录查询。
- 增加审计日志、Webhook 查询/重放、支付失败重试和订单过期处理。
- 增加退款和争议 Webhook 的支付状态同步。
- 增加事务 Outbox、发货凭证附件元数据、对账摘要、Actuator、Docker 和 GitHub Actions 配置。
- 增加高金额/重复购买风险记录、人工审核和风险订单发货阻断。
- 增加发货凭证确认、发货任务队列查询、被驳回任务重新排队和对账金额汇总。
- 增加 React/TypeScript PC/H5 响应式商城、结算交接、订单查询和运营台页面。
- 增加管理员商品后台页面：JWT 登录、游戏/商品/服务器列表、新增、编辑、状态启停及管理员 API 接入。

### Boundaries

- 当前版本仅用于内部 Sandbox 原型。
- 未完成真实 RocketMQ/OSS 云端连接、MFA/生产身份提供商、游戏自动发货和生产部署。
- React 前端依赖未在当前环境完成安装，已完成静态语法/翻译键检查；管理员后台需在安装依赖并启动后端后进行真实登录联调。
- 未执行编译、启动、单元测试或集成测试。
