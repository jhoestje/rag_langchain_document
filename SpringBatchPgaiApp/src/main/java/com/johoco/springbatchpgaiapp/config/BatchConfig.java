package com.johoco.springbatchpgaiapp.config;

import com.johoco.springbatchpgaiapp.batch.DocumentReader;
import com.johoco.springbatchpgaiapp.batch.DocumentWriter;
import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final DocumentReader documentReader;
    private final DocumentProcessor documentProcessor;
    private final DocumentWriter documentWriter;

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        log.info("Creating transaction manager with dataSource");
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager());
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.setTablePrefix("BATCH_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobLauncher jobLauncher() throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository());
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public Job processDocumentJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("Creating processDocumentJob with jobRepository and transactionManager");
        return new JobBuilder("processDocumentJob", jobRepository)
                .start(processDocumentStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step processDocumentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("Creating processDocumentStep with chunk size 10");
        return new StepBuilder("processDocumentStep", jobRepository)
                .<File, Document>chunk(10, transactionManager)
                .reader(documentReader)
                .processor(documentProcessor)
                .writer(documentWriter)
                .build();
    }
}
