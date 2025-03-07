package com.example.pgai.batch;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class DocumentReader implements ItemReader<File> {
    private final Queue<File> filesToProcess = new ConcurrentLinkedQueue<>();

    @Value("${document.input.directory}")
    private String inputDirectory;

    public void addFile(File file) {
        filesToProcess.offer(file);
    }

    @Override
    public File read() {
        return filesToProcess.poll();
    }
}
