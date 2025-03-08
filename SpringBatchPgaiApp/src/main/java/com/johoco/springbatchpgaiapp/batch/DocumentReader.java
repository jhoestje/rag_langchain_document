package com.johoco.springbatchpgaiapp.batch;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class DocumentReader implements ItemStreamReader<File> {
    private final Queue<File> filesToProcess = new ConcurrentLinkedQueue<>();
    private boolean initialized = false;

    @Value("${document.input.directory}")
    private String inputDirectory;

    public void addFile(File file) {
        filesToProcess.offer(file);
    }

    @Override
    public File read() {
        if (!initialized) {
            throw new IllegalStateException("Reader must be opened before it can be read");
        }
        return filesToProcess.poll();
    }

    @Override
    public void open(org.springframework.batch.item.ExecutionContext executionContext) throws ItemStreamException {
        initialized = true;
        // Create input directory if it doesn't exist
        File directory = new File(inputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public void update(org.springframework.batch.item.ExecutionContext executionContext) throws ItemStreamException {
        // No state to update since we're using a queue
    }

    @Override
    public void close() throws ItemStreamException {
        initialized = false;
        filesToProcess.clear();
    }
}
