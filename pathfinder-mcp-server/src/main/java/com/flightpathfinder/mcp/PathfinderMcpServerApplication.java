package com.flightpathfinder.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the standalone MCP server process.
 *
 * <p>This application only scans the MCP server protocol layer and framework shared primitives so the
 * server can expose tools without pulling bootstrap business implementations into its runtime.
 */
@SpringBootApplication(scanBasePackages = {
        "com.flightpathfinder.mcp",
        "com.flightpathfinder.framework"
})
public class PathfinderMcpServerApplication {

    /**
     * Boots the dedicated MCP server application.
     *
     * @param args standard Spring Boot startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PathfinderMcpServerApplication.class, args);
    }
}
