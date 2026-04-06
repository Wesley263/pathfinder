# Pathfinder 2.0 架构决策索引

本文档索引 2.0 当前已落地的架构决策。每条决策的实现细节见 [docs/domain/](docs/domain/README.md)。

## 1. 工作区与模块结构

- 2.0 独立主工程，1.0 保留为只读参考。
- 采用 feature-first 分层：`bootstrap` / `framework` / `infra-ai` / `mcp-server`。
- client 与 server 只共享协议契约与读模型，不共享业务实现。

**详见**：[runtime-and-dependencies.md](docs/domain/runtime-and-dependencies.md)

## 2. RAG 主链固定

- 主链顺序固定为 `memory load → stage one → retrieval → final answer → memory write`。
- legacy ReAct 不再属于 2.0 主链。
- Stage One 只做前置决策（rewrite + intent classify + KB/MCP split），不执行 retrieval 或 final answer。

**详见**：[rag-mainline-and-api.md](docs/domain/rag-mainline-and-api.md)

## 3. MCP Client/Server 分离

- `bootstrap/rag` 承载 client-side MCP 抽象（registry / discovery / parameter extraction / remote executor）。
- `mcp-server` 承载 protocol + dispatcher + 6 个 tool executor。
- `bootstrap` 不反向依赖 `mcp-server` 的业务实现。

**详见**：[mcp-and-tool-contracts.md](docs/domain/mcp-and-tool-contracts.md)

## 4. Graph Snapshot 只读语义

- 主程序构建 snapshot 并写入 Redis，`mcp-server` 只读 snapshot 恢复图对象。
- `SNAPSHOT_MISS` 是结构化业务结果，不触发本地重建。

**详见**：[graph-snapshot-and-path-search.md](docs/domain/graph-snapshot-and-path-search.md)

## 5. Memory 与 Trace 持久化

- conversation memory 已从内存版升级为持久化版，summary + recent turns 注入主链。
- trace 贯穿全链路，持久化落库后支持按 traceId / requestId / conversationId 查询。
- 当前不做 vector memory 和多层记忆策略。

**详见**：[memory-and-trace.md](docs/domain/memory-and-trace.md)

## 6. Admin 不拆独立模块

- admin 作为 `pathfinder-bootstrap` 内的 feature area，统一走 `/api/admin/**`。
- 避免过早拆部署边界，先收口为稳定后端面。

**详见**：[admin-and-ops-capabilities.md](docs/domain/admin-and-ops-capabilities.md)

## 7. 缓存架构简化

- 移除 Caffeine 进程内 L1 cache，简化为 Redis + PostgreSQL 两层。
- 进程内 graph 对象保留（不属于通用缓存，是搜索运行时状态）。
- Redis graph node TTL 延长为 30 天。

**详见**：[runtime-and-dependencies.md](docs/domain/runtime-and-dependencies.md)

## 8. SSE Chunked Streaming

- 当前 SSE 为 chunked streaming，不是真正逐 token 输出。
- 事件协议已稳定，memory / trace 已接入流式链路。
- 升级为真 token streaming 暂缓。

**详见**：[rag-mainline-and-api.md](docs/domain/rag-mainline-and-api.md)
