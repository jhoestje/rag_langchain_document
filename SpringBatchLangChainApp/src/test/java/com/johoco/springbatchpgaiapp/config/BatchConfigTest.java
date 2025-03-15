package com.johoco.springbatchpgaiapp.config;

import com.johoco.springbatchpgaiapp.batch.DocumentReader;
import com.johoco.springbatchpgaiapp.batch.DocumentWriter;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BatchConfigTest {

    @Mock
    private DataSource dataSource;
    
    @Mock
    private EntityManagerFactory entityManagerFactory;
    
    @Mock
    private DocumentReader documentReader;
    
    @Mock
    private DocumentProcessor documentProcessor;
    
    @Mock
    private DocumentWriter documentWriter;
    
    @Mock
    private JobRepository jobRepository;
    
    @Mock
    private PlatformTransactionManager transactionManager;
    
    @Test
    void testTransactionManager() {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource, entityManagerFactory, 
                                                 documentReader, documentProcessor, documentWriter);
        
        // When
        PlatformTransactionManager result = batchConfig.transactionManager();
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof JpaTransactionManager);
    }
    
    @Test
    void testProcessDocumentStep() {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource, entityManagerFactory, 
                                                documentReader, documentProcessor, documentWriter);
        
        // When
        Step result = batchConfig.processDocumentStep(jobRepository, transactionManager);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    void testProcessDocumentJob() {
        // Given
        BatchConfig batchConfig = new BatchConfig(dataSource, entityManagerFactory, 
                                                documentReader, documentProcessor, documentWriter);
        
        // When
        Job result = batchConfig.processDocumentJob(jobRepository, transactionManager);
        
        // Then
        assertNotNull(result);
    }
}
