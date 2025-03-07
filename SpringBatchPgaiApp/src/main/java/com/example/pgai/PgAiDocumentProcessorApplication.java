package com.example.pgai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PgAiDocumentProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PgAiDocumentProcessorApplication.class, args);
    }
}
