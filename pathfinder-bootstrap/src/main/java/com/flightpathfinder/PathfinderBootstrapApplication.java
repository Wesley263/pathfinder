package com.flightpathfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * 用于提供当前领域能力的默认实现。
 */

@SpringBootApplication(scanBasePackages = "com.flightpathfinder")
@EnableScheduling
public class PathfinderBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(PathfinderBootstrapApplication.class, args);
    }
}




