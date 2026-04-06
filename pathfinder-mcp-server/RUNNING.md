# Pathfinder MCP Server 运行说明

## 模块定位

`pathfinder-mcp-server` 当前是一个真正的运行期模块，不再是纯协议占位。

当前运行依赖：
- JDBC 数据源：供 `flight.search`、`price.lookup`、`visa.check`、`city.cost`、`risk.evaluate` 使用
- Redis：供 `graph.path.search` 使用

## 数据源最低配置

启动时需要以下数据源配置：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

对应环境变量：

- `PATHFINDER_MCP_DB_URL`
- `PATHFINDER_MCP_DB_USERNAME`
- `PATHFINDER_MCP_DB_PASSWORD`

如果缺少任一必填项，启动将通过 `McpServerDatasourceProperties` 快速失败。

## Redis 与图快照配置

`graph.path.search` 使用 Spring Boot 标准 Redis 配置（`spring.data.redis.*`）。

运行时期望：
- Redis 实例可达
- 已发布的 graph snapshot
- latest-version key 位于 `graph:snapshot:<graphKey>:latest`
- snapshot payload key 位于 `graph:snapshot:<graphKey>:version:<snapshotVersion>`

如果 snapshot 不存在，该工具不会本地重建也不会回源数据库构图，
而是返回 transport 成功 + 结构化工具状态 `SNAPSHOT_MISS`。

## 工具归属一览

| 工具 | 数据来源 | 说明 |
|------|---------|------|
| `graph.path.search` | Redis graph snapshot | 只读读模型工具 |
| `flight.search` | JDBC | 独立 server 端实现 |
| `price.lookup` | JDBC | 独立 server 端实现 |
| `visa.check` | JDBC | 独立 server 端实现 |
| `city.cost` | JDBC | 独立 server 端实现 |
| `risk.evaluate` | JDBC + 规则 | 独立 server 端实现 |

## 本地启动示例

```powershell
$env:PATHFINDER_MCP_DB_URL='jdbc:postgresql://localhost:5432/flight_pathfinder'
$env:PATHFINDER_MCP_DB_USERNAME='postgres'
$env:PATHFINDER_MCP_DB_PASSWORD='postgres'
$env:SPRING_DATA_REDIS_HOST='localhost'
$env:SPRING_DATA_REDIS_PORT='6379'
mvn -pl pathfinder-mcp-server spring-boot:run
```

Server 可以只依赖 JDBC 启动，但 `graph.path.search` 仅在 Redis 可达且已发布 graph snapshot 时可用。
