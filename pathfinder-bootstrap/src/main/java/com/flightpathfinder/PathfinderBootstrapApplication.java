package com.flightpathfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.flightpathfinder")
@EnableScheduling
public class PathfinderBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(PathfinderBootstrapApplication.class, args);
    }
}

