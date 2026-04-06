package com.flightpathfinder.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server 启动入口。
 *
 * 启动 MCP 服务端进程并加载 mcp 与 framework 相关组件。
 */
@SpringBootApplication(scanBasePackages = {
        "com.flightpathfinder.mcp",
        "com.flightpathfinder.framework"
})
public class PathfinderMcpServerApplication {

    /**
     * 启动应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PathfinderMcpServerApplication.class, args);
    }
}

