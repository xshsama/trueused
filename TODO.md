# 后端 API 实现路线图

## 第一阶段：基础框架和用户认证

这是整个应用的基础，需要最先实现。

1.  **项目初始化和数据库配置:**

    - 创建一个新的 Spring Boot 项目。
    - 配置 `application.properties`，连接到你的数据库（例如 MySQL 或 PostgreSQL）。
    - 添加 Spring Data JPA, Spring Web, Spring Security, JJWT 等必要的依赖。

2.  **用户和角色实体:**

    - 创建 `User` 实体，包含用户名、密码、邮箱、角色等字段。
    - 创建 `Role` 实体，用于定义用户角色 (例如, `ROLE_USER`, `ROLE_ADMIN`)。
    - 建立 `User` 和 `Role` 之间的多对多关系。

3.  **用户认证 API:**
    - 实现用户注册接口 (`/api/auth/register`)。
    - 实现用户登录接口 (`/api/auth/login`)，成功后返回 JWT Token。
    - 使用 Spring Security 和 JWT 实现认证和授权。
    - 创建 `UserPrincipal` 和 `CustomUserDetailsService` 来处理用户认证逻辑。

## 第二阶段：核心功能 - 商品和分类

用户能够登录后，就应该能看到和发布商品。

1.  **分类管理:**

    - 创建 `Category` 实体。
    - 实现获取所有分类的 API (`GET /api/categories`)。
    - (管理员) 实现增删改查分类的 API。

2.  **商品管理:**
    - 创建 `Product` 实体，包含名称、描述、价格、成色、状态、分类、卖家等信息。
    - 创建 `ProductImage` 实体来存储商品图片。
    - 实现发布新商品的 API (`POST /api/products`)。
    - 实现获取商品列表的 API (`GET /api/products`)，支持按分类、关键词搜索和分页。
    - 实现获取单个商品详情的 API (`GET /api/products/{id}`)。
    - 实现更新商品信息的 API (`PUT /api/products/{id}`)。
    - 实现删除商品的 API (`DELETE /api/products/{id}`)。

## 第三阶段：用户交互功能

增强用户体验和互动。

1.  **收藏夹功能:**

    - 创建 `Favorite` 实体，关联 `User` 和 `Product`。
    - 实现添加商品到收藏夹的 API (`POST /api/favorites`)。
    - 实现从收藏夹移除商品的 API (`DELETE /api/favorites/{productId}`)。
    - 实现查看用户收藏夹列表的 API (`GET /api/favorites`)。

2.  **用户信息管理:**
    - 实现获取当前登录用户信息的 API (`GET /api/users/me`)。
    - 实现更新用户信息的 API (`PUT /api/users/me`)。

## 第四阶段：管理员功能

用于后台管理。

1.  **用户管理:**
    - (管理员) 实现获取所有用户列表的 API (`GET /api/admin/users`)。
    - (管理员) 实现更新用户状态（如禁用、启用）的 API。

## 实现流程建议

对于每个功能的实现，建议遵循以下流程：

1.  **定义数据模型 (Entity):** 根据需求设计数据库表结构对应的实体类。
2.  **创建数据访问层 (Repository):** 使用 Spring Data JPA 创建对应的 Repository 接口。
3.  **定义数据传输对象 (DTO):** 创建 DTO 用于在 Controller 和 Service 层之间传输数据，避免直接暴露 Entity。
4.  **创建服务层 (Service):** 编写业务逻辑。
5.  **创建接口层 (Controller):** 暴露 RESTful API 接口，处理 HTTP 请求和响应。
6.  **配置安全规则 (Security):** 在 `SecurityConfig` 中为新的 API 端点配置访问权限。
7.  **编写测试:** (可选但推荐) 为 Service 或 Controller 编写单元测试或集成测试。

按照这个路线图，你可以一步步地完成整个后端应用的开发。
