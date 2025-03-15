package com.johoco.springbatchpgaiapp.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ExecutionContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
@Component
public class DocumentReader implements ItemStreamReader<File> {
    private final String inputDirectory;
    private final Queue<File> filesToProcess;
    private boolean initialized;

    public DocumentReader(@Value("${document.input.directory}") String inputDirectory) {
        this.inputDirectory = inputDirectory;
        this.filesToProcess = new LinkedList<>();
        this.initialized = false;
        log.info("DocumentReader constructed with input directory: {}", inputDirectory);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("Opening DocumentReader...");
        try {
            Path inputPath = Paths.get(inputDirectory);
            if (!Files.exists(inputPath)) {
                log.info("Creating input directory: {}", inputDirectory);
                Files.createDirectories(inputPath);
            }
            
            filesToProcess.clear();
            Files.list(inputPath)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .forEach(filesToProcess::offer);
            
            log.info("DocumentReader opened successfully. Input directory: {}, Found {} files", inputDirectory, filesToProcess.size());
            initialized = true;
        } catch (Exception e) {
            log.error("Error initializing DocumentReader: {}", e.getMessage(), e);
            throw new ItemStreamException("Failed to initialize DocumentReader", e);
        }
    }

    @Override
    public File read() throws Exception {
        if (!initialized) {
            log.error("DocumentReader not initialized! Call open() first.");
            throw new IllegalStateException("Reader must be opened before it can be read");
        }
        
        File nextFile = filesToProcess.poll();
        if (nextFile != null) {
            log.debug("Reading file: {}", nextFile.getName());
        } else {
            log.debug("No more files to process");
        }
        return nextFile;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // No state to update between steps
    }

    @Override
    public void close() throws ItemStreamException {
        log.info("Closing DocumentReader");
        if (filesToProcess != null) {
            filesToProcess.clear();
        }
        initialized = false;
    }
}
