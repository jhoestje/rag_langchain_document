package com.johoco.springbatchpgaiapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@SpringBootApplication(exclude = {BatchAutoConfiguration.class})
@EnableScheduling
public class SpringBatchPgaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBatchPgaiApplication.class, args);
    }

    @Bean
    public PlatformTransactionManager batchTransactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}
