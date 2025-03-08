package com.johoco.springbatchpgaiapp.config;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import com.johoco.springbatchpgaiapp.batch.DocumentReader;
import com.johoco.springbatchpgaiapp.batch.DocumentWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {
    private final DocumentProcessor documentProcessor;
    private final DocumentReader documentReader;
    private final DocumentWriter documentWriter;
    private final DataSource dataSource;

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
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
    public Job processDocumentJob(Step processDocumentStep) throws Exception {
        return new JobBuilder("processDocumentJob", jobRepository())
                .start(processDocumentStep)
                .build();
    }

    @Bean
    public Step processDocumentStep() throws Exception {
        return new StepBuilder("processDocumentStep", jobRepository())
                .<File, Document>chunk(10, transactionManager())
                .reader(documentReader)
                .processor((ItemProcessor<File, Document>) documentProcessor::processDocument)
                .writer(documentWriter)
                .build();
    }
}
