package com.johoco.springbatchpgaiapp.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ExecutionContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class DocumentReader implements ItemStreamReader<File> {
    @Value("${document.input.directory}")
    private String inputDirectory;
    
    private File fileToProcess;
    private boolean fileProcessed;
    private String fileName;

    public DocumentReader() {
        log.info("DocumentReader constructed");
    }
    
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        this.fileName = jobParameters.getString("fileName");
        log.info("DocumentReader initialized with fileName parameter: {}", fileName);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("Opening DocumentReader for file: {}", fileName);
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                log.error("No fileName parameter provided");
                throw new ItemStreamException("fileName parameter is required");
            }
            
            Path inputPath = Paths.get(inputDirectory);
            if (!Files.exists(inputPath)) {
                log.info("Creating input directory: {}", inputDirectory);
                Files.createDirectories(inputPath);
            }
            
            Path filePath = inputPath.resolve(fileName);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.error("File does not exist or is not a regular file: {}", filePath);
                throw new ItemStreamException("File not found: " + filePath);
            }
            
            fileToProcess = filePath.toFile();
            fileProcessed = false;
            
            log.info("DocumentReader opened successfully for file: {}", fileToProcess.getName());
        } catch (Exception e) {
            log.error("Error initializing DocumentReader: {}", e.getMessage(), e);
            throw new ItemStreamException("Failed to initialize DocumentReader", e);
        }
    }

    @Override
    public File read() throws Exception {
        if (fileToProcess == null) {
            log.error("DocumentReader not initialized! Call open() first.");
            throw new IllegalStateException("Reader must be opened before it can be read");
        }
        
        if (fileProcessed) {
            log.debug("File already processed: {}", fileToProcess.getName());
            return null;
        }
        
        log.debug("Reading file: {}", fileToProcess.getName());
        fileProcessed = true;
        return fileToProcess;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // No state to update between steps
    }

    @Override
    public void close() throws ItemStreamException {
        log.info("Closing DocumentReader");
        fileToProcess = null;
        fileProcessed = false;
    }
}
