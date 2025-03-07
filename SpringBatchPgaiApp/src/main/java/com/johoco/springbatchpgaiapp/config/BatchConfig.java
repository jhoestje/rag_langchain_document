package com.johoco.springbatchpgaiapp.config;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import com.johoco.springbatchpgaiapp.batch.DocumentReader;
import com.johoco.springbatchpgaiapp.batch.DocumentWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DocumentProcessor documentProcessor;

    @Bean
    public Job processDocumentJob() {
        return new JobBuilder("processDocumentJob", jobRepository)
                .start(processDocumentStep())
                .build();
    }

    @Bean
    public Step processDocumentStep() {
        return new StepBuilder("processDocumentStep", jobRepository)
                .<File, Document>chunk(1, transactionManager)
                .reader(documentReader())
                .processor(documentItemProcessor())
                .writer(documentWriter())
                .build();
    }

    @Bean
    public ItemReader<File> documentReader() {
        return new DocumentReader();
    }

    @Bean
    public ItemProcessor<File, Document> documentItemProcessor() {
        return documentProcessor::processDocument;
    }

    @Bean
    public ItemWriter<Document> documentWriter() {
        return new DocumentWriter();
    }
}
