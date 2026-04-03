# Pathfinder 2.0 当前架构决议

本文档只描述 `pathfinder2.0` 当前已经落地的架构事实，不写未来蓝图口吻。

## 工作区方向

- `E:\flight-plus\pathfinder` 继续作为旧版参考工程保留。
- `E:\flight-plus\pathfinder2.0` 是当前持续演进的主工程。
- 代码结构采用 feature-first 思路，主模块包括：
  - `pathfinder-bootstrap`
  - `pathfinder-framework`
  - `pathfinder-infra-ai`
  - `pathfinder-mcp-server`
- client 与 server 共享的范围只限于：
  - 协议契约
  - 读模型契约
- 双方不共享业务实现。
- legacy ReAct 不再属于 2.0 主链。

## 当前已实现的 RAG 主链

2.0 当前固定主链为：

`rewrite -> intent classify -> KB/MCP split -> retrieval/tool execution -> final answer`

`pathfinder-bootstrap` 中当前的职责边界如下：

- `rag/core/pipeline`：仅承载 stage-one 编排
- `rag/core/rewrite`：问题改写与改写结果模型
- `rag/core/retrieve`：KB retrieval、MCP execution、`KbContext`、`McpContext`、`RetrievalResult`
- `rag/core/answer`：最终回答装配、回答生成与 `AnswerResult`
- `rag/core/memory`：会话记忆抽象、持久化 store、summary 能力
- `rag/core/trace`：请求 trace 生命周期、trace 持久化与查询模型
- `rag/service`：面向应用编排的服务层
- `rag/controller`：同步与 SSE 两类 HTTP 入口及响应映射

## 当前 MCP 架构

### 共享范围

`pathfinder-framework` 当前承载共享契约，包括：
- MCP 协议 envelope
- tool schema 与 descriptor
- tool call payload
- graph snapshot 读模型

### bootstrap 侧职责

`bootstrap/rag` 负责 client-side MCP 抽象，当前包括：
- 本地 tool registry
- tool discovery
- 参数提取
- HTTP MCP client
- remote executor

### mcp-server 侧职责

`pathfinder-mcp-server` 负责 server-side MCP 抽象，当前包括：
- protocol / endpoint / dispatcher
- server-side registry
- tool executors
- graph/path 搜索实现
- JDBC 类工具实现

### 当前已接入的 MCP tools

当前 2.0 已接入以下 tools：
- `graph.path.search`
- `flight.search`
- `price.lookup`
- `visa.check`
- `city.cost`
- `risk.evaluate`

其中：
- `graph.path.search` 依赖 Redis 中的 graph snapshot
- 其余 5 个 tool 当前依赖 JDBC 数据源

## Graph / Path 当前事实

- 主程序负责构建 graph snapshot 并写入 Redis。
- `mcp-server` 只读 Redis snapshot 并在本进程恢复图对象后执行路径搜索。
- `SNAPSHOT_MISS` 当前是结构化业务结果，不算 transport 失败。
- 当前图搜索已从早期 DFS 版本演进到 bounded best-first + frontier control 方案，并具备：
  - lower bounds
  - pruning
  - candidate admission
  - Pareto filtering
  - weighted ranking
- 这块当前已不再明显弱于 1.0 的 graph/path 主能力，但仍可继续通过真实 snapshot 回放做离线校准。

## Memory 当前事实

- `conversationId` 当前已经真正参与主链。
- 查询前会加载 memory context，查询后会写回本轮 user / assistant 消息。
- 当前 memory 已具备：
  - conversation 持久化
  - message 持久化
  - summary 持久化
  - recent turns + summary 注入主链
- 当前不做：
  - vector memory
  - 长期知识记忆
  - 更复杂的多层记忆策略

## Trace 当前事实

- 每次 `/api/rag/chat` 请求都会生成独立 trace。
- trace 当前已具备：
  - 请求内记录
  - 持久化落库
  - 按 traceId 查询 detail
  - 按 requestId / conversationId 查询最近记录
- trace detail 当前能看到：
  - run 信息
  - stages
  - nodes
  - MCP tool 摘要
  - snapshot miss / partial / error 等业务事实

## SSE 当前事实

- 当前同步入口为：`POST /api/rag/chat`
- 当前流式入口为：`POST /api/rag/chat/stream`
- SSE 当前是 chunked streaming，不是真正逐 token 输出。
- 当前事件协议已覆盖：
  - stage one started / completed
  - retrieval started / completed
  - final answer started / chunk / completed
  - error
- 当前 memory / trace 均已接入流式链路，没有被绕开。

## Admin 当前事实

2.0 当前 admin 能力统一放在 `pathfinder-bootstrap/admin`，不单独拆 Maven 模块。

当前已接入的 admin / 运维能力包括：
- 数据 reload / stats / cache invalidate
- graph snapshot 管理
- trace admin
- diagnostics：flights / paths / airport
- memory admin 第一刀

当前仍处于后续补强中的管理能力包括：
- MCP admin
- 更完整的 memory admin 写操作
- admin 前端页面

## 当前已明确不做的事情

- 不把 legacy ReAct 带回 2.0 主链
- 不让 `bootstrap` 反向依赖 `mcp-server` 业务实现
- 不在当前阶段把 admin 拆成独立 Maven 模块
- 不优先把 chunked SSE 立刻升级成真正逐 token 输出

## 当前剩余重点

在不考虑测试的前提下，当前剩余重点主要是：
- 继续补齐 admin 管理面
- 视需要补 MCP admin / memory admin 后续能力
- 后续再评估是否升级为真正逐 token SSE
- 最后再做测试与更完整的运行期验证
