# Pathfinder 2.0

Pathfinder 2.0 是航线路径智能问答系统的重构主工程，基于 RAG + MCP 架构。
旧工程 `E:\flight-plus\pathfinder` 继续保留为 1.0 参考工程，不作为默认写入目标。

## 模块一览

| 模块 | 职责 |
|------|------|
| `pathfinder-bootstrap` | 主 Spring Boot 应用，承载 RAG 主链、admin 后端面、flight 业务 |
| `pathfinder-framework` | 共享协议契约、统一返回、异常、requestId / trace 基础类型 |
| `pathfinder-infra-ai` | AI 基础设施抽象（chat / embedding / model routing） |
| `pathfinder-mcp-server` | 独立 MCP server，承载 tool executors、graph/path 搜索与 JDBC 类工具 |

模块间依赖方向：`bootstrap` → `framework` + `infra-ai`，`mcp-server` → `framework`。
client 与 server 只共享协议契约与读模型，不共享业务实现。

## 快速启动

### pathfinder-bootstrap

需要 PostgreSQL + Redis + `pathfinder-mcp-server` 可达。

```powershell
mvn -pl pathfinder-bootstrap spring-boot:run
```

### pathfinder-mcp-server

详见 [RUNNING.md](pathfinder-mcp-server/RUNNING.md)。

```powershell
mvn -pl pathfinder-mcp-server spring-boot:run
```

## 文档导航

| 文档 | 作用 |
|------|------|
| [ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md) | 架构决策索引 |
| [docs/STATUS.md](docs/STATUS.md) | 项目当前状态入口（优先阅读） |
| [docs/README.md](docs/README.md) | 文档目录说明与阅读顺序 |
| [docs/domain/](docs/domain/README.md) | 核心实现细节（主链 / MCP / 图搜索 / memory / trace / admin） |
| [docs/analysis/](docs/analysis/) | 项目分析、取舍说明、下一步建议 |
| [docs/plan/](docs/plan/) | 测试计划与实施方案 |
| [.env.example](.env.example) | 环境变量模板 |
