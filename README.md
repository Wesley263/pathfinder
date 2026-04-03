# Pathfinder 2.0

`E:\flight-plus\pathfinder2.0` 是 Pathfinder 当前持续演进的主工程。  
`E:\flight-plus\pathfinder` 继续保留为 1.0 参考工程，不作为默认写入目标。

## 模块划分

当前主模块包括：
- `pathfinder-bootstrap`：主 Spring Boot 应用、RAG 主链、admin 后端面
- `pathfinder-framework`：共享协议契约、统一返回、request/trace 基础类型、web 约定
- `pathfinder-infra-ai`：AI 基础设施抽象
- `pathfinder-mcp-server`：独立 MCP server、tool executors、graph/path 搜索与 JDBC 类工具实现

## 当前固定主链

当前同步主链已经固定为：

`request -> memory load -> stage one -> retrieval -> final answer -> memory write`

其中：
- `stage one`：rewrite -> intent classify -> KB/MCP split
- `retrieval`：KB retrieval + MCP execution
- `final answer`：基于 `rewriteResult + intentSplitResult + KbContext + McpContext` 组装最终回答
- `response`：同步 JSON 响应，由 `POST /api/rag/chat` 返回

当前不再把 legacy ReAct 作为 2.0 主链的一部分。

## 当前已接入的 MCP Tools

当前 2.0 已接入 6 个 MCP tools：
- `graph.path.search`
- `flight.search`
- `price.lookup`
- `visa.check`
- `city.cost`
- `risk.evaluate`

说明：
- `graph.path.search` 读取 Redis 中的 graph snapshot，在 `pathfinder-mcp-server` 内恢复图对象并执行路径搜索。
- snapshot miss 以结构化业务状态 `SNAPSHOT_MISS` 返回。
- 其余五个工具当前都由 `pathfinder-mcp-server` 以 JDBC / 规则方式独立实现。

## 当前 RAG 分层事实

`pathfinder-bootstrap` 中当前的职责边界如下：
- `rag/core/pipeline`：仅负责 stage-one 编排
- `rag/core/rewrite`：问题改写与改写结果模型
- `rag/core/retrieve`：KB retrieval、MCP execution、`KbContext`、`McpContext`、`RetrievalResult`
- `rag/core/answer`：最终回答装配、回答生成、`AnswerResult`
- `rag/core/memory`：conversation memory、summary、持久化 store
- `rag/core/trace`：请求 trace 生命周期、trace 持久化与查询能力
- `rag/service`：应用编排层
- `rag/controller`：同步与 SSE 两类 HTTP 入口及响应映射

## Memory 与 Trace 当前状态

### Memory
- `conversationId` 当前已经真正生效。
- conversation memory 已具备持久化能力，不再只是纯内存 recent turns。
- 当前已支持：
  - conversation 持久化
  - message 持久化
  - summary 持久化
  - recent turns + summary 进入主链
- 当前不做：
  - vector memory
  - 长期知识记忆
  - 更复杂的多层记忆治理

### Trace
- 每次 `/api/rag/chat` 请求都会生成独立 trace。
- 当前 trace 已支持：
  - 请求内记录
  - 持久化落库
  - 按 `traceId` 查询 detail
  - 按 `requestId` / `conversationId` 查询最近记录
- `RagTraceController` 已提供查询入口。

## 当前 SSE 状态

- 同步接口：`POST /api/rag/chat`
- 流式接口：`POST /api/rag/chat/stream`
- 当前 SSE 已可用，但属于 chunked streaming，不是真正逐 token 输出。
- 事件协议已覆盖 stage one、retrieval、final answer 和 error 几类阶段事件。
- memory / trace 已接入流式链路，没有被绕开。

## 当前 Admin 后端面

当前 admin 后端统一放在 `/api/admin/**` 下，能力包括：
- data stats / reload / cache invalidate
- graph snapshot 管理
- trace admin
- diagnostics（flights / paths / airport）
- memory admin 第一刀
- MCP admin 第一刀

注意：
- 当前 `/api/admin/data/reload` 已改为真正的外部数据入库 ETL 入口，负责导入 OpenFlights / visa policy / city cost，并在有数据变更时失效 graph snapshot 与本地 MCP registry。
- admin 前端页面尚未开始建设，当前主要是后端管理接口。

## 当前运行前提

- `pathfinder-bootstrap` 依赖 `pathfinder-mcp-server` 可达，才能执行 MCP tools。
- `pathfinder-mcp-server` 需要 JDBC datasource，供 `flight.search`、`price.lookup`、`visa.check`、`city.cost`、`risk.evaluate` 使用。
- `graph.path.search` 还依赖 Redis 和已发布的 graph snapshot。

`pathfinder-mcp-server` 的运行前提见：
- [RUNNING.md](E:/flight-plus/pathfinder2.0/pathfinder-mcp-server/RUNNING.md)

## 当前建议的下一步

当前项目已经从“主能力搭建阶段”进入“管理面与产品化收口阶段”。
当前最自然的下一步是：
- 先做 admin 前端骨架和页面信息架构
- 再视情况补 data reload 增强项、admin 后续能力
- 最后再评估是否把 chunked SSE 升级为真正逐 token 输出

## 相关文档

2.0 当前文档入口位于：
- [docs/README.md](E:/flight-plus/pathfinder2.0/docs/README.md)
- [docs/STATUS.md](E:/flight-plus/pathfinder2.0/docs/STATUS.md)
- [docs/domain/README.md](E:/flight-plus/pathfinder2.0/docs/domain/README.md)


