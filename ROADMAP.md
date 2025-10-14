# TrueUsed 后端多阶段路线图（2025 Q4 起）

本路线图基于当前技术栈与代码现状：Spring Boot 3、Spring Security（JWT）、Spring Data JPA、MySQL。目标是从 MVP 到可扩展、易观测、合规安全的二手交易平台后端。

## 0. 基线稳定（Week 0-1）

- 配置与启动
  - 修复 `spring.jpa.hibernate.ddl-auto` 配置（目前存在拼写/空格问题），改为 `validate`，引入 Flyway 进行数据库版本化迁移。
  - 分离环境配置（application-dev.yml / application-prod.yml），敏感信息改为环境变量或密钥管理（本地 `.env`、生产 KMS/Secret Manager）。
  - 规范日志与错误返回：统一异常处理器、错误码与 traceId。
  - API 版本化：后端业务路径统一前缀 `/api/v1/**`，`/auth/**` 仍保留。
- 安全加固
  - JWT：访问令牌（短效）+ 刷新令牌（Cookie HttpOnly、Secure、SameSite=Lax/None，按部署情况调整），实现刷新令牌轮换与复用检测，服务端维护刷新令牌状态（Redis/DB）。
  - CORS：允许来源改为可配置白名单，区分 dev/prod。
  - 速率限制：登录、注册、刷新接口加速率限制（如 Bucket4j/Spring RateLimiter）。

验收：

- 开发环境可一键启动（含 MySQL/Flyway），基础路由与鉴权稳定；
- 统一错误响应；
- 安全基线通过基本安全扫描（依赖/漏洞）。

## 1. MVP 功能集（Week 1-3）

范围：用户、商品（帖子）、收藏、基础搜索、个人资料、管理员能力雏形。

- 数据结构设计（MVP 细化）

  - 用户（User）与个人资料最小集

    - 字段：
      - username varchar(50) not null unique
      - email varchar(120) not null unique
      - password varchar(200) not null（存 hash）
      - status enum(ACTIVE, SUSPENDED, BANNED, INACTIVE) not null default ACTIVE
      - nickname varchar(50)
      - avatarUrl varchar(255)
      - bio varchar(300)
      - phone varchar(20) unique 可选
      - emailVerified boolean default false
      - phoneVerified boolean default false
      - lastLoginAt timestamp nullable
      - banReason varchar(200) nullable
      - banUntil timestamp nullable
      - createdAt/updatedAt 继承 BaseEntity
    - 关系：
      - roles: ManyToMany Role（已存在）
      - 后续可外拆一张 user_profiles（1:1）承载更多资料：性别、生日、地址、geo 坐标、社交链接等。
    - 索引：
      - uk_users_username, uk_users_email（唯一）
      - idx_users_nickname（模糊搜索/展示）
      - idx_users_status, idx_users_last_login_at（后台筛选）

  - 商品（Product）

    - 字段：
      - sellerId bigint not null FK -> users(id)
      - title varchar(120) not null
      - description text not null
      - price decimal(12,2) not null check price >= 0
      - currency char(3) not null default 'CNY'
      - status enum(DRAFT, PUBLISHED, RESERVED, SOLD, HIDDEN) not null default DRAFT
      - condition enum(NEW, LIKE_NEW, GOOD, FAIR, POOR) 可选
      - categoryId bigint FK -> categories(id)
      - locationText varchar(100) 可选（城市/区域可读文本）
      - lat decimal(9,6) 可选
      - lng decimal(9,6) 可选
      - viewsCount int not null default 0
      - favoritesCount int not null default 0（可后续去冗余）
      - isDeleted boolean not null default false（软删可选）
      - createdAt/updatedAt 继承 BaseEntity
    - 关系：
      - seller: ManyToOne User（LAZY）
      - category: ManyToOne Category（LAZY）
      - images: OneToMany ProductImage（有序，封面）
    - 索引：
      - idx_products_seller(sellerId)
      - idx_products_category(categoryId)
      - idx_products_status(status)
      - idx_products_price(price)
      - idx_products_created_at(createdAt)
      - idx_products_title(title)（LIKE 搜索）
      - 组合： (status, createdAt)；(categoryId, status, createdAt)

  - 商品图片（ProductImage）

    - 字段：
      - productId bigint not null FK -> products(id)
      - url varchar(255) not null
      - sort int not null default 0
      - isCover boolean not null default false
      - createdAt/updatedAt 继承 BaseEntity
    - 索引：
      - idx_images_product(productId)
    - 关系与约束：
      - Product 侧 OneToMany(mappedBy="product", cascade=控制在创建/更新时使用, orphanRemoval=true)

  - 类目（Category）

    - 字段：
      - name varchar(50) not null
      - parentId bigint nullable
      - slug varchar(50) unique 可选
      - level tinyint 可选
      - path varchar(200) 可选（如 /数码/手机/二手手机）
      - status enum(ACTIVE, INACTIVE) not null default ACTIVE
      - createdAt/updatedAt 继承 BaseEntity
    - 索引与约束：
      - idx_categories_parent(parentId)
      - uk_categories_name_parent(name, parentId)（同级重名禁止）

  - 收藏（Favorite）

    - 字段：
      - userId bigint not null FK -> users(id)
      - productId bigint not null FK -> products(id)
      - createdAt timestamp not null
    - 约束与索引：
      - uk_fav_user_product(userId, productId)（防重复收藏）
      - idx_fav_user(userId)、idx_fav_product(productId)
    - 备注：
      - 删除商品时的联动：可使用外键级联或逻辑删除时代码清理。

  - 基础搜索（查询维度与索引）

    - 入参：q（关键字），categoryId，priceMin/priceMax，sort（createdAt desc | price asc/desc | 热度），page/size
    - 过滤：固定 status=PUBLISHED；isDeleted=false（如启用软删）
    - 索引：
      - title LIKE '%q%' 使用 idx_products_title（量大后评估 FULLTEXT/ES）
      - (status, createdAt) 支持首页最新
      - (categoryId, status, createdAt) 支持类目筛选
      - price 单列索引支持范围查询
    - 位置筛选（可选）：
      - 先按 locationText 粗筛，后续用 lat/lng + Haversine 计算；量大再考虑空间索引。

  - 管理员能力（雏形所需字段）
    - User：status + banReason + banUntil，支持封禁/解封；按 lastLoginAt/createdAt/状态筛选
    - Product：status 增加 HIDDEN（违规下架）；支持按状态批量筛选
    - 审计（可选后续）：AdminAction 记录管理员操作（who/when/what/why）

- 领域模型（初版 ERD）

  - User(id, username, email, passwordHash, status, roles, createdAt,...)
  - Role(id, name[ROLE_USER/ROLE_ADMIN], ...)
  - Product(id, sellerId, title, description, price, categoryId, status[DRAFT/PUBLISHED/SOLD], location, createdAt,...)
  - ProductImage(id, productId, url, sort)
  - Category(id, name, parentId)
  - Favorite(userId, productId, createdAt)

- 核心 API 契约（摘要）

  - Auth：POST /auth/login, /auth/register, /auth/refresh, /auth/logout
  - 用户：GET /api/v1/users/me；PUT /api/v1/users/me（昵称、头像、简介）；GET /api/v1/users/{id}
  - 商品：
    - GET /api/v1/products?page=&size=&q=&categoryId=&priceMin=&priceMax=&sort=
    - GET /api/v1/products/{id}
    - POST /api/v1/products（卖家）
    - PUT /api/v1/products/{id}（卖家）
    - DELETE /api/v1/products/{id}（卖家或管理员）
    - 图片上传：POST /api/v1/products/{id}/images（初期支持本地/MinIO，后续 S3/OSS）
  - 收藏：POST /api/v1/products/{id}/favorite；DELETE /api/v1/products/{id}/favorite；GET /api/v1/favorites
  - 管理：GET /api/admin/users；PUT /api/admin/users/{id}/status

- 其他
  - 分页、排序、过滤约定；
  - 通用响应包装与字段校验（Hibernate Validator）；
  - DTO/Mapper 分层（MapStruct 可选）。

验收：

- 前端 Home/List/Detail/Profile/Favorites 页面依赖的后端接口全部可用；
- 单元测试覆盖率 ≥40%，基础集成测试打通关键链路（登录->发帖->搜索->收藏）。

## 2. 功能增强（Week 3-6）

- 账户与安全
  - 邮箱验证与重置密码（邮件通道，异步发送）。
  - 刷新令牌轮换与黑名单/状态存储（防复用攻击）。
- 交易与互动（基础）
  - 消息/会话模型（Conversation, Message）——先用轮询接口，后续 WebSocket。
  - 评论/评价（Review）模型与接口。
- 搜索与发现
  - 高级过滤与排序（上架时间、价格区间、距离等）；
  - 类目、标签体系；
- 运维
  - 对热点接口引入 Redis 缓存（产品详情、类目树）；
  - 图片上传改用对象存储（MinIO 本地/云 S3）。

验收：

- 留存与互动功能可用（收藏、消息、评论）；
- 关键接口 p95 < 200ms（开发环境近似值）；
- 覆盖率 ≥55%。

## 3. 可扩展性与弹性（Week 6-10）

- 架构
  - 模块化/分层清晰：auth、catalog、user、interaction（消息/评论）、admin；
  - 事件驱动：Spring 事件 or Kafka/RabbitMQ（新增商品、下单、通知等事件）；
  - 并发与一致性：下单/标售时的库存/状态锁（悲观/乐观/分布式锁），幂等键；
- 性能
  - 全站缓存策略（Redis TTL、二级缓存可评估）；
  - N+1 查询治理（JPA fetch plan、批量查询、索引）；
  - 分片/读写分离（后续里程碑）。

验收：

- 常规 QPS 下 CPU/内存稳定；
- 主要列表/详情 p95 < 150ms（近似）；
- 压测报告与热点 SQL 优化记录。

## 4. 可观测与 DevOps（Week 6-10 并行推进）

- 可观测
  - 指标：Micrometer + Prometheus + Grafana（业务 KPI 与系统指标）。
  - 日志：JSON 结构化，集中收集（ELK/Opensearch）。
  - 链路：OpenTelemetry（可选）。
- 交付
  - Docker 化与 Compose（MySQL/Redis/MinIO 一键起）；
  - CI/CD：GitHub Actions（构建、测试、SCA、容器镜像、部署）；
  - 蓝绿/灰度发布（后续 K8s）。

验收：

- 任一环境可一键部署；
- 仪表盘可观测核心接口成功率/延迟；
- CI 必须绿灯方可合入。

## 5. 合规与高级安全（Week 8+）

- 合规
  - 数据最小化与留存策略；导出/删除请求流程（GDPR 方向性要求）。
- 安全
  - 依赖与镜像扫描（OWASP Dependency-Check/Trivy）；
  - 安全头与输入校验；
  - 2FA（可选）；
  - 权限细粒度（基于方法/资源）。

验收：

- 安全与合规检查清单通过；
- 高风险漏洞为 0。

---

## 关键数据模型建议（简版 ERD）

- User(1) - (N) Product：卖家与商品
- Product(1) - (N) ProductImage
- User(N) - (N) Favorite(Product)
- Category(1) - (N) Product
- Conversation(2+ participants) - Message(N)
- Review(User -> Product/User)
- RefreshToken(User(1) - (N) Token)

索引：

- Product(title, categoryId, price, status, createdAt)，联合索引支持常用过滤/排序；
- Favorite(userId, createdAt)；
- Message(conversationId, createdAt)；
- User(username unique, email unique)。

## 主要 API 契约（摘录）

- GET /api/v1/products
  - Query：page,size,q,categoryId,priceMin,priceMax,sort
  - 200：{ content: ProductDTO[], page, size, total }
- POST /api/v1/products（鉴权：ROLE_USER）
  - Body：{ title, description, price, categoryId, images: [url] }
  - 201：ProductDTO
- POST /api/v1/products/{id}/favorite（鉴权） -> 204
- GET /api/v1/favorites -> ProductDTO[]（分页）
- GET /api/v1/users/me -> UserDTO
- PUT /api/v1/users/me -> UserDTO
- 会话/消息（后续）：
  - GET /api/v1/conversations
  - GET /api/v1/conversations/{id}/messages
  - POST /api/v1/conversations/{id}/messages

错误响应统一：

```json
{ "code": "RESOURCE_NOT_FOUND", "message": "...", "traceId": "..." }
```

## 任务拆解与优先级（示例）

- P0（基线）：Flyway 初始化、错误处理器、配置分环境、JWT 刷新 Cookie 设置、安全与速率限制
- P0（MVP）：Product/Favorite 模型与 CRUD、列表查询、用户资料编辑、后台用户管理
- P1：对象存储接入（MinIO/S3）、评论/会话（轮询版）、高级筛选
- P2：Redis 缓存、刷新令牌轮换与复用检测、邮件验证/重置密码
- P2：日志/指标/追踪、Docker/CI、压测与 SQL 优化
- P3：消息 WebSocket、事件总线、读写分离/分片、K8s 与灰度

前置依赖：

- Flyway 在所有实体落地前到位；
- Redis 在令牌、缓存、速率限制之前搭建；
- 对象存储在图片上传前准备；

## 质量门（Quality Gates）

- Build：Maven 构建必须通过；
- Lint/格式：Spotless/Checkstyle 通过；
- 测试：
  - 单元 + WebMvc/RestAssured 集成测试；
  - 覆盖率阈值：MVP ≥40%，增强 ≥55%，规模化 ≥65%；
- 安全：依赖/镜像扫描无高危；
- 数据：Flyway 迁移必须可回滚，有演练记录。

## 发布节奏与运维

- 迭代：2 周一个迭代，主干开发+特性分支；
- 发布：每周一次小版本，语义化版本与变更日志；
- 回滚：蓝绿/灰度策略与回滚手册；
- 备份：数据库自动备份 + 恢复演练。

## 近期下一步（建议）

1. 引入 Flyway，并创建 V1\_\_init.sql（User/Role/Product/Favorite 等基础表）。
2. 统一异常与错误码，输出错误响应规范。
3. 将刷新令牌改为轮换策略并存储状态（Redis），完善登出与失效处理。
4. 完成 Product/Favorite 相关 API 与测试，打通前端页面。
5. Docker Compose 集成 MySQL/Redis/MinIO，并在 README 中提供一键启动说明。
