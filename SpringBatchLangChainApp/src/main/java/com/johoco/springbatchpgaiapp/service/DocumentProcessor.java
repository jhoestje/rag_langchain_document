package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.model.DocumentMetadata;
import com.johoco.springbatchpgaiapp.model.DocumentStatus;
import com.johoco.springbatchpgaiapp.util.FileOperations;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
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
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    // private final FileManagementService fileManagementService;
    private final FileOperations fileOperations;
    
    @Value("${spring.application.name:SpringBatchPgaiApp}")
    private String applicationName;
    
    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    public DocumentProcessor(EmbeddingStore<TextSegment> embeddingStore, FileOperations fileOperations) {   
        // this.embeddingModel = embeddingModel;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = embeddingStore;
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
            document.setStatus(DocumentStatus.NEW);

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
                document.setStatus(DocumentStatus.PROCESSED);
                // document.setEmbedding(embedding);

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
            } catch (Exception e) {
                log.error("Error generating embedding for file {}: {}", file.getName(), e.getMessage(), e);
                return null;
            }
            
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
        }
    }
}
