package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.model.DocumentMetadata;
import com.johoco.springbatchpgaiapp.model.DocumentStatus;
import com.johoco.springbatchpgaiapp.model.FileExtension;
import com.johoco.springbatchpgaiapp.processor.FileProcessor;
import com.johoco.springbatchpgaiapp.processor.FileProcessorFactory;
import com.johoco.springbatchpgaiapp.util.FileOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
public class DocumentProcessor implements ItemProcessor<File, Document> {
    
    private final FileOperations fileOperations;
    private final FileProcessorFactory fileProcessorFactory;
    
    @Value("${spring.application.name:SpringBatchPgaiApp}")
    private String applicationName;
    
    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    public DocumentProcessor(FileOperations fileOperations, FileProcessorFactory fileProcessorFactory) {   
        this.fileOperations = fileOperations;
        this.fileProcessorFactory = fileProcessorFactory;
    }

    @Override
    public Document process(File file) throws Exception {
        if (file == null) {
            log.error("Received null file to process");
            return null;
        }

        log.debug("Processing file: {}", file.getName());
        FileExtension extension = FileExtension.fromFile(file);
        log.debug("File extension: {}", extension.getValue());
        
        try {
            // Get the appropriate processor for this file type
            FileProcessor processor = fileProcessorFactory.getProcessorForFile(file);
            if (processor == null) {
                log.error("No suitable processor found for file type: {}", extension.getValue());
                throw new UnsupportedOperationException("Unsupported file type: " + extension.getValue());
            }
            
            // Extract content using the appropriate processor
            String content = processor.extractContent(file);
            if (content == null || content.trim().isEmpty()) {
                log.warn("File {} is empty", file.getName());
                return null;
            }

            Document document = new Document();
            document.setFilename(file.getName());
            document.setContent(content);
            document.setFileSize(fileOperations.getFileSize(file));
            document.setLastModified(Instant.ofEpochMilli(fileOperations.getLastModified(file)));
            document.setStatus(DocumentStatus.PROCESSED);
            
            // Create and set metadata
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .originalFilename(file.getName())
                    .processingTime(Instant.now())
                    .processorName(applicationName)
                    .processorVersion(applicationVersion)
                    .build();
            document.setMetadata(metadata);
            
            log.info("Successfully processed document: {} with status: {}", document.getFilename(), document.getStatus());
            return document;
        } catch (IOException e) {
            log.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
            throw new RuntimeException("Error processing file: " + file.getName(), e);
        } catch (UnsupportedOperationException e) {
            log.error("Unsupported file type for {}: {}", file.getName(), e.getMessage());
            throw e;
        }
    }
}
