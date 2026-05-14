# TrueUsed Backend

TrueUsed 后端是一个围绕“二手交易 + 平台验货 + mock 物流 + mock 售后”搭建的 Spring Boot 服务。当前项目目标不是生产落地，而是把二手平台的核心链路跑通，并让前端能够演示完整业务流程。

## 项目定位

- 面向二手交易平台场景
- 同时支持 `卖家自出` 和 `平台验货` 两种成交模式
- 以 mock 数据驱动交易、物流、验货和售后闭环
- 适合课程项目、作品集展示和流程答辩，不是生产级系统

## 技术栈

- Java 21
- Spring Boot 3.5
- Spring Security + JWT
- Spring Data JPA
- MySQL
- Redis
- Flyway
- WebSocket / STOMP
- MapStruct
- Lombok
- Cloudinary
- Alipay Sandbox

## 当前已实现模块

- 用户认证与个人信息
- access token / refresh token 签发、刷新轮换与登出撤销
- 商品发布、上下架、收藏
- 两种交易模式下的订单创建与支付
- 平台验货报告与订单绑定
- mock 物流生成、物流快照持久化、签收状态约束
- 钱包支付与订单结算
- 通知与消息基础能力
- WebSocket 鉴权接入 access token 校验
- 退款申请、卖家审批、拒绝后重提、mock 手动完成退款

## 核心业务流程

### 1. 卖家自出

1. 商品发布并上架
2. 买家下单
3. 买家支付成功，订单进入 `PAID`
4. 卖家手动发货，订单进入 `SHIPPED`
5. mock 物流按时间推进
6. 买家确认收货，订单完成

### 2. 平台验货

1. 商品以平台验货模式发布
2. 平台生成验货结果与验货报告
3. 买家下单并支付，订单进入 `PENDING_SHIPMENT`
4. 平台仓自动生成一条 mock 出库物流
5. 订单进入 `SHIPPED`
6. 买家在订单详情查看验货报告与物流轨迹
7. 物流推进到可签收节点后，买家确认收货

### 3. 售后退款

1. 买家发起退款申请，订单进入 `REFUNDING`
2. 卖家在售后详情页同意或拒绝
3. `REFUND_ONLY` 同意后直接完成退款
4. `RETURN_REFUND` 暂不做逆向物流，由卖家手动完成一次 mock 退款闭环
5. 若卖家拒绝，买家可以重新发起申请

## mock 说明

以下能力当前仍为 mock 或半 mock：

- 物流轨迹按时间自动生成，不接第三方物流平台
- 验货结果和验货明细以模拟数据驱动
- 客服、通知、消息中的部分场景是演示数据
- `退货退款` 不包含真实逆向物流，只保留手动完成闭环
- 测试数据可通过本地数据开关或 Seeder 补充

## 认证与会话说明

- 登录成功后返回 access token，并通过 HttpOnly Cookie 下发 refresh token
- refresh token 在刷新时会做一次性轮换，旧 token 会进入撤销表
- 登出会同时撤销当前 access token 与 refresh token，不再只是清理浏览器 Cookie
- HTTP 接口与 WebSocket 握手阶段都会校验 token 类型、用户状态和撤销状态
- 撤销记录通过 Flyway `V7__revoked_tokens.sql` 建表持久化

## 本地运行

### 环境要求

- JDK 21
- MySQL 8+
- Redis 6+
- Maven 3.9+

### 配置说明

项目默认从 `src/main/resources/application.properties` 读取配置，敏感项通过环境变量覆盖。可参考仓库中的 `.env.example` 准备本地配置。

关键项包括：

- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `JWT_SECRET`
- `CLOUDINARY_*`
- `ALIPAY_*`
- `APP_TEST_DATA_ENABLED`

当 `app.test-data.enabled=true` 时，启动过程会执行 `TestDataSeeder`，自动初始化测试用户、商品、订单、聊天、优惠券和评价等演示数据；默认关闭。

### 启动步骤

```bash
./mvnw spring-boot:run
```

默认端口：`8081`

Flyway 会在启动时自动执行数据库迁移，当前迁移目录位于：

```text
src/main/resources/db/migration
```

## 与前端联调

- 前端开发端口：`5173`
- Vite 代理会把 `/api` 请求转发到 `http://localhost:8081`
- 支付成功回跳页默认指向前端支付成功页

## Docker 运行

仓库根目录提供 `docker-compose.yml`，默认端口：

- 前端：`http://localhost`
- 后端：`http://localhost:8081`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

如需替换 JWT、Cloudinary、支付宝沙箱等配置，复制根目录 `.env.example` 为 `.env` 后修改。

## 当前项目亮点

- 同一个项目里明确区分了 `卖家自出` 和 `平台验货` 两条交易链
- 平台验货报告已经和订单绑定，不再是独立页面漂浮在流程之外
- mock 物流不只是展示文案，而是已经影响订单状态和确认收货时机
- 售后链路已经补到“可申请、可审批、可拒绝重提、可手动完成退款”的程度
- 登录刷新、登出注销和 WebSocket 鉴权已经补到服务端撤销校验，演示链路更完整
- 帮助中心、客服中心、客服消息等辅助页面也能承接核心流程说明

## 当前遗留问题

- 未实现真实逆向物流
- 验货、物流、客服消息仍以 mock 数据为主
- 缺少系统化自动化测试
- 第三方能力目前仍偏演示性质，未做生产化配置隔离
- 撤销表当前只做写入与校验，尚未补充过期数据清理策略
- 部分 Seeder / 本地配置文件仍在开发阶段整理中

## 仓库状态说明

当前后端主线功能已经基本闭环，但仓库里仍可能存在未提交的本地开发文件，例如测试数据 Seeder、应用配置或临时仓库改动。收尾交付前建议再做一次人工检查。
