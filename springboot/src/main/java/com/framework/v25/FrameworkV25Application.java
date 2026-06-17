package com.framework.v25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FrameworkV25Application {
    public static void main(String[] args) {
        SpringApplication.run(FrameworkV25Application.class, args);
    }
}
