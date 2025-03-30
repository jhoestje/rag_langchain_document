package com.johoco.springbatchpgaiapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.johoco.springbatchpgaiapp.util.FileOperations;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileWatcherService {
    private final JobLauncher jobLauncher;
    private final Job processDocumentJob;
    private final FileOperations fileOperations;
    
    @Value("${document.input.directory}")
    private String inputDirectory;
    
    @Scheduled(fixedDelayString = "${document.input.polling-interval}")
    public void watchDirectory() {
        try {
            File directory = fileOperations.ensureDirectoryExists(inputDirectory);

            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        processFile(file);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error ensuring directory exists: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a single file by launching a Spring Batch job.
     * 
     * @param file The file to process
     */
    private void processFile(File file) {
        try {
            log.info("Processing file: {}", file.getName());
            JobParameters params = new JobParametersBuilder()
                .addString("fileName", file.getName())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(processDocumentJob, params);
            log.info("Successfully submitted job for file: {}", file.getName());
        } catch (Exception e) {
            log.error("Error processing file {}: {}", file.getName(), e.getMessage());
        }
    }
}
