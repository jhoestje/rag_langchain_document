package com.johoco.springbatchpgaiapp.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EmbeddingStoreConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;

    @PostConstruct
    public void init() {
        log.info("Initializing EmbeddingStoreConfig");
    }

    @Bean
    public EmbeddingStore<TextSegment> pgVectorEmbeddingStore() {
        log.info("Creating PgVectorEmbeddingStore");
        
        // Extract database name from JDBC URL
        String database = jdbcUrl.substring(jdbcUrl.lastIndexOf("/") + 1);
        if (database.contains("?")) {
            database = database.substring(0, database.indexOf("?"));
        }
        
        // Create a custom table for PgVectorEmbeddingStore instead of using the existing documents table
        // This avoids conflicts with our existing schema
        return PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database(database)
                .user(username)
                .password(password)
                .table("embedding_store") // Use a different table name
                .dimension(384)
                .useIndex(true)
                .indexListSize(100)
                .createTable(true) // Let PgVectorEmbeddingStore create its own table
                .build();
    }
}
