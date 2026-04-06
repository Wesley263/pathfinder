package com.flightpathfinder.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立 MCP 服务进程启动入口。
 *
 * <p>该应用仅扫描 MCP 服务端协议层与 framework 共享基础组件，
 * 使服务端在暴露工具时无需把 bootstrap 业务实现拉入本进程运行时。
 */
@SpringBootApplication(scanBasePackages = {
        "com.flightpathfinder.mcp",
        "com.flightpathfinder.framework"
})
public class PathfinderMcpServerApplication {

    /**
     * 启动专用 MCP 服务应用。
     *
     * @param args 标准 Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PathfinderMcpServerApplication.class, args);
    }
}
