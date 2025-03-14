package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.batch.DocumentReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileWatcherService {
    private final JobLauncher jobLauncher;
    private final Job processDocumentJob;
    
    @Value("${document.input.directory}")
    private String inputDirectory;
    
    private final Map<String, Long> processedFiles = new HashMap<>();

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
                if (file.isFile()) {
                    Long lastModified = processedFiles.get(file.getName());
                    if (lastModified == null || lastModified < file.lastModified()) {
                        try {
                            log.info("Processing file: {}", file.getName());
                            JobParameters params = new JobParametersBuilder()
                                .addString("fileName", file.getName())
                                .addLong("timestamp", System.currentTimeMillis())
                                .toJobParameters();
                            jobLauncher.run(processDocumentJob, params);
                            processedFiles.put(file.getName(), file.lastModified());
                            log.info("Successfully processed file: {}", file.getName());
                        } catch (Exception e) {
                            log.error("Error processing file {}: {}", file.getName(), e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
