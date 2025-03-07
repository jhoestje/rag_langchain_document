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
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileWatcherService {
    private final JobLauncher jobLauncher;
    private final Job processDocumentJob;
    private final DocumentReader documentReader;
    
    @Value("${document.input.directory}")
    private String inputDirectory;
    
    private final Map<String, Long> processedFiles = new HashMap<>();

    @Scheduled(fixedDelayString = "${document.polling-interval}")
    public void watchDirectory() {
        File directory = new File(inputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Long lastModified = processedFiles.get(file.getName());
                    if (lastModified == null || lastModified < file.lastModified()) {
                        try {
                            documentReader.addFile(file);
                            JobParameters params = new JobParametersBuilder()
                                .addString("fileName", file.getName())
                                .addLong("timestamp", System.currentTimeMillis())
                                .toJobParameters();
                            
                            jobLauncher.run(processDocumentJob, params);
                            processedFiles.put(file.getName(), file.lastModified());
                        } catch (Exception e) {
                            log.error("Error processing file: " + file.getName(), e);
                        }
                    }
                }
            }
        }
    }
}
