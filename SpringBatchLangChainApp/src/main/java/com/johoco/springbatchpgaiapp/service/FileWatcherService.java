package com.johoco.springbatchpgaiapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FileWatcherService {
    private final JobLauncher jobLauncher;
    private final Job processDocumentJob;
    private final FileManagementService fileManagementService;
    private final String inputDirectory;
    
    // Use ConcurrentHashMap for thread safety
    private final ConcurrentHashMap<String, Boolean> processingFiles = new ConcurrentHashMap<>();

    public FileWatcherService(
            JobLauncher jobLauncher,
            Job processDocumentJob,
            FileManagementService fileManagementService,
            @Value("${document.input.directory}") String inputDirectory) {
        this.jobLauncher = jobLauncher;
        this.processDocumentJob = processDocumentJob;
        this.fileManagementService = fileManagementService;
        this.inputDirectory = inputDirectory;
        log.info("FileWatcherService initialized with input directory: {}", inputDirectory);
    }

    @Scheduled(fixedDelayString = "${document.input.polling-interval}")
    public void watchDirectory() {
        File directory = new File(inputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !isFileBeingProcessed(file.getName())) {
                    try {
                        // Mark file as being processed
                        markFileAsProcessing(file.getName());
                        
                        log.info("Processing file: {}", file.getName());
                        JobParameters params = new JobParametersBuilder()
                            .addString("fileName", file.getName())
                            .addLong("timestamp", System.currentTimeMillis())
                            .toJobParameters();
                        
                        JobExecution jobExecution = jobLauncher.run(processDocumentJob, params);
                        
                        // Check job execution status
                        if (jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
                            log.info("Successfully processed file: {}", file.getName());
                            // File should have been moved by DocumentWriter
                        } else if (jobExecution.getExitStatus().equals(ExitStatus.FAILED)) {
                            log.error("Job failed for file: {}", file.getName());
                            // Move to failure directory if still exists
                            if (file.exists()) {
                                fileManagementService.moveToFailureDirectory(file);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
                        // Move to failure directory if still exists
                        if (file.exists()) {
                            fileManagementService.moveToFailureDirectory(file);
                        }
                    } finally {
                        // Unmark file regardless of outcome
                        unmarkFileAsProcessing(file.getName());
                    }
                }
            }
        }
    }
    
    private boolean isFileBeingProcessed(String fileName) {
        return processingFiles.containsKey(fileName);
    }
    
    private void markFileAsProcessing(String fileName) {
        processingFiles.put(fileName, Boolean.TRUE);
    }
    
    private void unmarkFileAsProcessing(String fileName) {
        processingFiles.remove(fileName);
    }
}
