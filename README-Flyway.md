# Flyway 使用说明（TrueUsed 后端）

本项目使用 Flyway 管理数据库结构演进，确保“可重复、可审计、可回滚（前滚修复）”。

## 关键约定

- 迁移目录：`src/main/resources/db/migration`
- 版本脚本：`V{n}__{描述}.sql`（例：`V2__users_avatar_url_length_to_255.sql`）
- 可重复脚本：`R__{描述}.sql`（适合字典/种子数据，脚本变化会重跑）
- 禁止生产库直改结构；所有变更必须以迁移脚本合入主干。

## 当前状态

- 已启用 Flyway，JPA 设为校验：`spring.jpa.hibernate.ddl-auto=validate`
- 首次接管使用了基线模式（application.properties）：
  - `spring.flyway.baseline-on-migrate=true`
  - `spring.flyway.baseline-version=1`
- 已提供示例模板：`V1__init.sql.example`（不会被执行）。
- 已提交迁移：`V2__users_avatar_url_length_to_255.sql`（将 `users.avatar_url` 统一为 255）。

## 常用操作

### 启动并迁移

```bash
cd TrueUsed
./mvnw -DskipTests spring-boot:run
```

观察日志中 Flyway 的执行情况：首次会 Baseline 到 1，并应用版本脚本（如 V2）。

### 打包构建

```bash
cd TrueUsed
./mvnw -DskipTests clean package
```

### 校验数据库

- 表 `flyway_schema_history` 记录已应用版本与校验和（checksum）。
- 数据库结构应与实体一致（JPA validate 通过）。

## 如何补齐 V1（推荐，便于一键重建）

1. 在开发机导出“无数据”的结构 SQL：
   ```bash
   mysqldump -u <user> -p --no-data --skip-comments --databases <db_name> > /tmp/schema.sql
   ```
2. 手工清理 SQL，仅保留本项目需要的表/索引，统一字符集/引擎（InnoDB + utf8mb4）。
3. 放入 `db/migration`，命名为 `V1__init.sql`（去掉 `.example`）。
4. 验证新环境（空库）：启动应用 → Flyway 应用 V1 → JPA validate 通过。
5. 之后可在 `application.properties` 中关闭 Baseline 接管：
   ```properties
   spring.flyway.baseline-on-migrate=false
   ```

## 迁移编写规范

- 明确的 `ALTER` 语义；新增非空列建议“允许 NULL → 回填数据 → 设 NOT NULL”三步。
- 索引/唯一约束显式命名（便于后续变更/删除）。
- 大表变更注意锁表风险（必要时分批/离线窗口执行策略）。
- 脚本幂等性：如需使用 `IF EXISTS/IF NOT EXISTS` 或 `ON DUPLICATE KEY UPDATE`，请确保目标 MySQL 版本支持。

## 故障与修复

- 原则：使用“前滚”迁移修复，不建议手动回退数据库。
- 如因修改脚本导致 checksum 不一致且目标结构已与新脚本一致，可使用 `flyway repair` 修复校验记录（需谨慎）：
  ```bash
  # 安装并配置 Flyway CLI 后
  flyway -url=jdbc:mysql://... -user=... -password=... repair
  ```

## 团队协作

- 每个涉及实体/表结构变更的 PR 必须包含迁移脚本与验证步骤。
- 评审关注点：命名、幂等性、回滚策略（如何“前滚”修复）、对不同环境的影响说明。
- 建议在 CI 中增加：启动 → migrate → WebMvc 测试 → 打包。

---

如需我为你把当前数据库导出并生成正式的 `V1__init.sql`，请告知数据库连接方式（或由你导出后我来清理整理）。
