package com.johoco.springbatchpgaiapp.config;

import com.johoco.springbatchpgaiapp.batch.DocumentReader;
import com.johoco.springbatchpgaiapp.batch.DocumentWriter;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    
    @Mock
    private JobBuilder jobBuilder;
    
    @Mock
    private StepBuilder stepBuilder;
    
    @Mock
    private Step step;
    
    @Mock
    private Job job;
    
    @InjectMocks
    private BatchConfig batchConfig;
    
    @Test
    void testTransactionManager() {
        // When
        PlatformTransactionManager result = batchConfig.transactionManager();
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    void testTaskExecutor() {
        // When
        TaskExecutor result = batchConfig.taskExecutor();
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    void testJobLauncher() throws Exception {
        // Given
        when(jobRepository.getClass()).thenReturn((Class) JobRepository.class);
        
        // When
        JobLauncher result = batchConfig.jobLauncher();
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof TaskExecutorJobLauncher);
    }
    
    @Test
    void testProcessDocumentStep() {
        // Given
        when(jobRepository.getJobNames()).thenReturn(new String[0]);
        when(stepBuilder.chunk(any(Integer.class))).thenReturn(stepBuilder);
        when(stepBuilder.reader(documentReader)).thenReturn(stepBuilder);
        when(stepBuilder.processor(documentProcessor)).thenReturn(stepBuilder);
        when(stepBuilder.writer(documentWriter)).thenReturn(stepBuilder);
        when(stepBuilder.build()).thenReturn(step);
        
        // Mock the behavior needed for the test
        doReturn(stepBuilder).when(jobRepository).createStepBuilder(any());
        
        // When
        Step result = batchConfig.processDocumentStep();
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    void testProcessDocumentJob() {
        // Given
        when(jobRepository.getJobNames()).thenReturn(new String[0]);
        when(jobBuilder.start(any(Step.class))).thenReturn(jobBuilder);
        when(jobBuilder.build()).thenReturn(job);
        
        // Mock the behavior needed for the test
        doReturn(jobBuilder).when(jobRepository).createJobBuilder(any());
        
        // When
        Job result = batchConfig.processDocumentJob();
        
        // Then
        assertNotNull(result);
    }
}
