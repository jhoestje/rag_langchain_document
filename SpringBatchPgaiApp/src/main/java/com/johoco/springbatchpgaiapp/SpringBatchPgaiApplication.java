package com.johoco.springbatchpgaiapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringBatchPgaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBatchPgaiApplication.class, args);
    }
}
