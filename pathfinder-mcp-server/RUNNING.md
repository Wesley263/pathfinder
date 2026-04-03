# Pathfinder MCP Server Running Notes

## What This Module Needs

`pathfinder-mcp-server` is a real runtime module now.
It is no longer a protocol-only placeholder.

Current runtime dependencies:

- JDBC datasource for `flight.search`, `price.lookup`, `visa.check`, `city.cost`, and `risk.evaluate`
- Redis for `graph.path.search`

## Minimum Datasource Contract

Startup requires these datasource values:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Equivalent environment variables:

- `PATHFINDER_MCP_DB_URL`
- `PATHFINDER_MCP_DB_USERNAME`
- `PATHFINDER_MCP_DB_PASSWORD`

If any required datasource value is missing, startup fails fast through `McpServerDatasourceProperties`.

## Redis And Graph Snapshot Contract

`graph.path.search` uses Spring Boot's normal Redis configuration (`spring.data.redis.*`).

At runtime it expects:

- a reachable Redis instance
- a published graph snapshot
- the latest-version key at `graph:snapshot:<graphKey>:latest`
- the snapshot payload key at `graph:snapshot:<graphKey>:version:<snapshotVersion>`

If the snapshot is missing, the tool does not rebuild locally and does not query the database for graph construction.
It returns transport success with structured tool status `SNAPSHOT_MISS`.

## Tool Ownership Summary

- `graph.path.search`: Redis-backed read-model tool
- `flight.search`: JDBC-backed independent server-side implementation
- `price.lookup`: JDBC-backed independent server-side implementation
- `visa.check`: JDBC-backed independent server-side implementation
- `city.cost`: JDBC-backed independent server-side implementation
- `risk.evaluate`: JDBC-backed and rule-based independent server-side implementation

## Example Local Run

```powershell
$env:PATHFINDER_MCP_DB_URL='jdbc:postgresql://localhost:5432/flight_pathfinder'
$env:PATHFINDER_MCP_DB_USERNAME='postgres'
$env:PATHFINDER_MCP_DB_PASSWORD='postgres'
$env:SPRING_DATA_REDIS_HOST='localhost'
$env:SPRING_DATA_REDIS_PORT='6379'
mvn -pl pathfinder-mcp-server spring-boot:run
```

The server can start with JDBC only, but `graph.path.search` will only be usable when Redis is reachable and a graph snapshot has been published.
