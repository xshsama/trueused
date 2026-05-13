# 问题解决日志

本文件用于记录每次任务完成后的问题与解决过程。

## 2026-03-03 初始化日志机制

- 问题描述：
  仓库中缺少统一的“问题解决记录”方式。
- 根因分析：
  没有强制记录规则，也没有统一的 Markdown 日志文件。
- 解决方法：
  新增 `AGENTS.md` 记录规则，并创建本日志文件模板。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed/AGENTS.md`
  `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
- 验证方式：
  检查两个文件是否存在且模板字段完整。
- 结果：
  后续任务可按统一格式持续追加记录。
- 后续建议：
  每次任务完成后追加一条日志。

## YYYY-MM-DD 任务标题

- 问题描述：
- 根因分析：
- 解决方法：
- 修改文件：
- 验证方式：
- 结果：
- 后续建议：

## 2026-03-04 日志看起来是空的

- 问题描述：
  用户反馈 Markdown 日志文件看起来是空的。
- 根因分析：
  实际项目目录是 `/Users/xshsama/code/TrueUsed/TrueUsed`，可能查看了外层目录。
- 解决方法：
  确认真实日志路径并核对内容，验证历史记录存在。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
- 验证方式：
  校验日志行数并打印该路径下文件内容。
- 结果：
  确认活动项目中的日志并非空文件。
- 后续建议：
  后续统一在内层项目路径打开并维护日志。

## 2026-03-04 前后端分别提交 Git

- 问题描述：
  需要将前端与后端当前变更各提交一次。
- 根因分析：
  两个仓库都存在大量未提交改动，影响后续版本追踪。
- 解决方法：
  在后端仓库与前端仓库分别执行 `git add -A` 与 `git commit`。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed`（后端仓库，已提交）
  `/Users/xshsama/code/TrueUsed/TrueUsed-web`（前端仓库，已提交）
- 验证方式：
  查看提交输出并记录最新提交哈希。
- 结果：
  后端提交哈希为 `dce7a89`，前端提交哈希为 `ea2e12a`。
- 后续建议：
  如需同步远端，可分别执行 `git push`。

## 2026-03-04 清理冗余文档与无用截图

- 问题描述：
  需要检查并删除老旧冗余的 Markdown 文件和无用图片文件。
- 根因分析：
  根目录存在历史合并后的旧计划文档，以及未被项目引用的临时截图，造成目录噪音。
- 解决方法：
  删除已明确“内容已合并到 ROADMAP”的旧文档，以及 7 张未被引用的调试截图。
- 修改文件：
  删除 `/Users/xshsama/code/TrueUsed/CHAT_IMPLEMENTATION_PLAN.md`
  删除 `/Users/xshsama/code/TrueUsed/CHAT_OPTIMIZATION_PLAN.md`
  删除 `/Users/xshsama/code/TrueUsed/pw_order_after_submit.png`
  删除 `/Users/xshsama/code/TrueUsed/pw_order_product.png`
  删除 `/Users/xshsama/code/TrueUsed/pw_order_settlement_before_submit.png`
  删除 `/Users/xshsama/code/TrueUsed/trueused_login_result.png`
  删除 `/Users/xshsama/code/TrueUsed/trueused_register_result.png`
  删除 `/Users/xshsama/code/TrueUsed/ui_click_order_after_fix.png`
  删除 `/Users/xshsama/code/TrueUsed/ui_click_order_result.png`
  更新 `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
- 验证方式：
  重新扫描根目录文件清单并逐一确认目标文件不存在。
- 结果：
  已完成高置信度冗余文件清理，根目录仅保留仍有参考价值的文档。
- 后续建议：
  后续调试截图建议放入临时目录并定期清理。

## 2026-03-12 加固 JWT 类型校验与服务端登出撤销

- 问题描述：
  认证过滤器仅校验 JWT 是否合法，未强制要求 `typ=access`；登出流程只清理浏览器 Cookie，旧 token 在有效期内仍可能继续使用。
- 根因分析：
  HTTP 与 WebSocket 认证入口都只调用了通用 `validateToken`；服务端没有持久化撤销状态，且前端登出请求默认不会携带 `Authorization` 头。
- 解决方法：
  在 HTTP/WebSocket 认证入口增加 `access token` 类型校验；新增持久化撤销表与 `TokenRevocationService`，对登出和 refresh 轮换时的旧令牌做服务端撤销；前端登出请求显式附带当前 `Authorization` 头，确保 access token 可被服务端注销。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/auth/controller/AuthController.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/auth/service/LoginService.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/security/jwt/JwtAuthenticationFilter.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/security/jwt/JwtTokenProvider.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/config/WebSocketConfig.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/entity/RevokedToken.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/security/repository/RevokedTokenRepository.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/java/com/xsh/trueused/security/service/TokenRevocationService.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/main/resources/db/migration/V7__revoked_tokens.sql`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/test/java/com/xsh/trueused/security/jwt/JwtAuthenticationFilterTest.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed/src/test/java/com/xsh/trueused/auth/service/LoginServiceTest.java`
  `/Users/xshsama/code/TrueUsed/TrueUsed-web/src/api/auth.js`
  `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
- 验证方式：
  补充过滤器与登录服务的单元测试；尝试执行 `./mvnw -Dtest=JwtAuthenticationFilterTest,LoginServiceTest test`；同时静态检查差异与关键文件逻辑。
- 结果：
  代码已完成加固与测试补充；当前环境缺少 Java Runtime，导致 Maven 测试未能实际执行，其余静态检查通过。
- 后续建议：
  在具备 JDK 的环境执行迁移与测试；后续可增加过期撤销记录清理任务，并考虑将撤销查询迁移到 Redis 以降低数据库热路径压力。

## 2026-03-17 更新前后端说明文档

- 问题描述：
  前后端 README 与实现记录没有覆盖本轮登录鉴权、首页改版、验货报告页调整和测试数据开关等最近改动，文档与代码现状存在偏差。
- 根因分析：
  本轮开发先完成了代码更新，但对应的仓库说明文件没有同步维护，导致项目亮点、配置说明和页面实现记录仍停留在旧版本。
- 解决方法：
  更新后端 README，补充令牌撤销、刷新轮换、WebSocket 鉴权与测试数据 Seeder 说明；更新前端 README 与 Implementation 文档，补充首页、验货报告页和登出联动的当前实现状态。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed/README.md`
  `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
  `/Users/xshsama/code/TrueUsed/TrueUsed-web/README.md`
  `/Users/xshsama/code/TrueUsed/TrueUsed-web/Implementation.md`
- 验证方式：
  人工核对 README/Implementation 中的模块说明、配置项和页面描述是否与当前工作区代码改动一致，再检查 git diff 确认仅修改目标文档。
- 结果：
  前后端说明文档已与当前本地实现基本对齐，便于后续直接提交并同步到 GitHub。
- 后续建议：
  后续每一轮功能合并时同步更新 README 里的“当前已实现模块”和“遗留问题”，避免再次出现文档滞后。

## 2026-03-18 吃透支付流程

- 问题描述：
  需要梳理 TrueUsed 当前从下单、收银台支付、支付宝回调、钱包冻结、发货、确认收货到退款/取消的完整支付链路，并明确前后端代码入口与关键状态流转。
- 根因分析：
  支付相关逻辑分散在结算页、收银台、订单服务、支付服务、钱包服务和定时任务中；同时支付宝与钱包两条链路共用订单状态机，但资金处理方式并不完全一致，单看单个文件很难形成全局认知。
- 解决方法：
  逐一核对 `Settlement.vue`、`Payment.vue`、`PaymentSuccess.vue`、订单与钱包 API、`OrderCommandService`、支付策略、`AlipayController`、`AlipayService`、`WalletService`、状态机与定时任务，确认真实调用链、状态变化、异步回调位置与退款结算分支，并记录当前实现中的关键风险点。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
- 验证方式：
  通过静态代码走查交叉验证前端路由、API 调用、后端控制器、服务层、状态机和定时任务，确认每个支付分支都能在代码中闭环定位。
- 结果：
  已确认系统存在两条主支付链路：支付宝通过 `/api/alipay/pay` 发起、由 `/api/alipay/notify` 异步落单；钱包支付通过 `/api/orders/{id}/pay-wallet` 立即冻结资金并更新订单。订单在确认收货或自动确认后才给卖家入账，取消/退款则退回买家钱包；同时识别出支付宝金额校验与订单查询权限两个高风险薄弱点。
- 后续建议：
  优先补齐支付宝回调中的订单归属与金额核验，明确外部支付与内部钱包台账的一致性策略，并为订单详情查询增加买卖双方权限校验。

## 2026-03-18 详细讲解支付宝支付分支

- 问题描述：
  需要把支付宝分支按前端发起支付、后端生成表单、浏览器跳转支付宝、异步回调更新订单、前端回跳页轮询确认的顺序讲清楚，并明确每段代码的真实职责。
- 根因分析：
  当前实现把“下单”“拉起支付宝”“支付结果确认”拆散在多个页面和服务里，容易误以为 `/api/alipay/pay` 就已经完成了支付落账，或误把回跳页当成最终支付确认点。
- 解决方法：
  重新核对 `Payment.vue`、`payments.js`、`AlipayController`、`AlipayService`、`OrderCommandService`、`CallbackOrderPaymentStrategy`、`PaymentSuccess.vue`、`SecurityConfig` 与支付宝配置，按请求流和状态流拆解职责边界与异步时序。
- 修改文件：
  `/Users/xshsama/code/TrueUsed/TrueUsed/docs/issue-log.md`
- 验证方式：
  通过静态代码走查核对前端按钮动作、后端接口、支付宝回调放行配置、回跳页轮询逻辑和订单状态更新代码是否首尾相连。
- 结果：
  已确认订单在进入收银台前就已创建；`/api/alipay/pay` 只生成并返回自动提交的支付宝表单，不改订单状态；真正的支付确认来自 `/api/alipay/notify` 的签名校验与回调处理，前端 `/payment/success` 页面只负责轮询订单状态并展示结果。
- 后续建议：
  后续若继续完善支付宝链路，建议补上支付前订单归属与金额校验、回调业务字段校验，以及“支付发起日志”和“回调处理日志”的统一追踪字段。
