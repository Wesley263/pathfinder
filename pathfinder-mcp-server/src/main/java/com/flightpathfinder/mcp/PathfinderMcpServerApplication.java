package com.flightpathfinder.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@SpringBootApplication(scanBasePackages = {
        "com.flightpathfinder.mcp",
        "com.flightpathfinder.framework"
})
public class PathfinderMcpServerApplication {

    /**
     * 说明。
     *
     * @param args 参数说明。
     */
    public static void main(String[] args) {
        SpringApplication.run(PathfinderMcpServerApplication.class, args);
    }
}
