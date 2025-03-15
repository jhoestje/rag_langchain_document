package com.johoco.springbatchpgaiapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {BatchAutoConfiguration.class})
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.johoco.springbatchpgaiapp.repository")
@EnableTransactionManagement
public class SpringBatchLangChainApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBatchLangChainApplication.class, args);
    }
}
