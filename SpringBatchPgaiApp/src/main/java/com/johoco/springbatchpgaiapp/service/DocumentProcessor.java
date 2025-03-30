package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.util.FileOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
public class DocumentProcessor implements ItemProcessor<File, Document> {
    
    private final FileOperations fileOperations;

    public DocumentProcessor(FileOperations fileOperations) {   
        this.fileOperations = fileOperations;
    }

    @Override
    public Document process(File file) throws Exception {
        if (file == null) {
            log.error("Received null file to process");
            return null;
        }

        log.debug("Processing file: {}", file.getName());
        
        try {
            String content = fileOperations.readFileContent(file);
            if (content == null || content.trim().isEmpty()) {
                log.warn("File {} is empty", file.getName());
                return null;
            }

            Document document = new Document();
            document.setFilename(file.getName());
            document.setContent(content);
            document.setFileSize(fileOperations.getFileSize(file));
            document.setLastModified(Instant.ofEpochMilli(fileOperations.getLastModified(file)));

            document.setStatus(Document.STATUS_PROCESSED);
            
            log.info("Successfully processed document: {} with status: {}", document.getFilename(), document.getStatus());
            return document;
        } catch (IOException e) {
            log.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
            throw new RuntimeException("Error processing file: " + file.getName(), e);
        }
    }
}
