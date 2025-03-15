package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DocumentProcessor implements ItemProcessor<File, Document> {
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final FileManagementService fileManagementService;
    
    // Map to track original files and their processing status
    private final Map<String, File> processedFiles = new HashMap<>();

    public DocumentProcessor(EmbeddingStore<TextSegment> embeddingStore, FileManagementService fileManagementService) {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = embeddingStore;
        this.fileManagementService = fileManagementService;
        log.info("DocumentProcessor initialized with embeddingModel: {}", 
                this.embeddingModel.getClass().getSimpleName());
    }
    
    @PostConstruct
    public void init() {
        log.info("DocumentProcessor post-construction initialization complete");
    }

    @Override
    public Document process(File file) throws Exception {
        if (file == null) {
            log.error("Received null file to process");
            return null;
        }

        log.debug("Processing file: {}", file.getName());
        
        try {
            String content = readFileContent(file);
            if (content == null || content.trim().isEmpty()) {
                log.warn("File {} is empty", file.getName());
                fileManagementService.moveToFailureDirectory(file);
                return null;
            }

            Document document = new Document();
            document.setFilename(file.getName());
            document.setContent(content);
            document.setFileSize(file.length());
            document.setLastModified(Instant.ofEpochMilli(file.lastModified()));
            document.setStatus("PROCESSING");

            try {
                log.debug("Generating embedding for document: {}", file.getName());
                TextSegment segment = TextSegment.from(content);
                Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
                Embedding embedding = embeddingResponse.content();
                
                // Convert to float array for storage in the document entity
                float[] vectorArray = new float[embedding.vector().length];
                for (int i = 0; i < embedding.vector().length; i++) {
                    vectorArray[i] = embedding.vector()[i];
                }
                document.setEmbedding(vectorArray);
                document.setStatus("PROCESSED");
                
                // Also store in the embedding store with metadata
                try {
                    // Create a simple TextSegment with the content
                    // The PgVectorEmbeddingStore will handle the storage in its own table
                    embeddingStore.add(embedding, TextSegment.from(content));
                    log.info("Successfully stored embedding in PgVector store for document: {}", file.getName());
                } catch (Exception e) {
                    log.error("Error storing embedding in PgVector store: {}", e.getMessage(), e);
                    // Continue processing - we still have the embedding in the document entity
                }
                
                log.info("Successfully generated embedding with {} dimensions for document: {}", 
                         vectorArray.length, file.getName());
                
                // Store the original file reference for later movement
                processedFiles.put(document.getFilename(), file);
                
            } catch (Exception e) {
                log.error("Error generating embedding for document {}: {}", file.getName(), e.getMessage(), e);
                document.setEmbedding(null);
                document.setStatus("ERROR_EMBEDDING");
                
                // Move file to failure directory
                fileManagementService.moveToFailureDirectory(file);
                return null;
            }
            
            log.info("Successfully processed document: {} with status: {}", document.getFilename(), document.getStatus());
            return document;
        } catch (IOException e) {
            log.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
            // Move file to failure directory
            fileManagementService.moveToFailureDirectory(file);
            throw new RuntimeException("Error processing file: " + file.getName(), e);
        }
    }
    
    /**
     * Get the original file for a processed document
     * 
     * @param filename The document filename
     * @return The original file or null if not found
     */
    public File getOriginalFile(String filename) {
        return processedFiles.get(filename);
    }
    
    /**
     * Remove a file from the tracking map
     * 
     * @param filename The document filename
     */
    public void removeTrackedFile(String filename) {
        processedFiles.remove(filename);
    }

    private String readFileContent(File file) throws IOException {
        try {
            log.debug("Reading content from file: {}", file.getName());
            String content = Files.readString(file.toPath());
            log.debug("Successfully read {} characters from file: {}", content.length(), file.getName());
            return content;
        } catch (IOException e) {
            log.error("Error reading file {}: {}", file.getName(), e.getMessage(), e);
            throw e;
        }
    }
}
