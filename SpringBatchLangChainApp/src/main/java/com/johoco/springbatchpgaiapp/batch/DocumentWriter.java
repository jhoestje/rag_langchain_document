package com.johoco.springbatchpgaiapp.batch;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import com.johoco.springbatchpgaiapp.service.FileManagementService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWriter implements ItemWriter<Document> {
    private final DocumentRepository documentRepository;
    private final DocumentProcessor documentProcessor;
    private final FileManagementService fileManagementService;

    @PostConstruct
    public void init() {
        log.info("DocumentWriter initialized with documentRepository: {}", documentRepository != null ? "injected" : "null");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Chunk<? extends Document> documents) {
        if (documentRepository == null) {
            log.error("DocumentRepository is null! Document saving will fail.");
            throw new IllegalStateException("DocumentRepository is not initialized");
        }
        
        log.debug("Writing batch of {} documents", documents.size());
        
        for (Document document : documents) {
            try {
                if (document == null) {
                    log.error("Received null document in batch!");
                    continue;
                }

                if (document.getFilename() == null || document.getFilename().trim().isEmpty()) {
                    log.error("Document has no filename!");
                    throw new IllegalArgumentException("Document filename cannot be null or empty");
                }

                log.debug("Looking for existing document with filename: {}", document.getFilename());
                Optional<Document> existingDoc = documentRepository.findByFilename(document.getFilename());
                
                Document savedDocument;
                if (existingDoc.isPresent()) {
                    Document existing = existingDoc.get();
                    log.debug("Found existing document: {} with id: {}", existing.getFilename(), existing.getId());
                    existing.setContent(document.getContent());
                    existing.setEmbedding(document.getEmbedding());
                    existing.setFileSize(document.getFileSize());
                    existing.setLastModified(document.getLastModified());
                    existing.setStatus(document.getStatus());
                    savedDocument = documentRepository.save(existing);
                    log.info("Successfully updated document: {} with id: {}", savedDocument.getFilename(), savedDocument.getId());
                } else {
                    log.debug("No existing document found for filename: {}, creating new", document.getFilename());
                    savedDocument = documentRepository.save(document);
                    log.info("Successfully created new document: {} with id: {}", savedDocument.getFilename(), savedDocument.getId());
                }
                
                if (savedDocument == null || savedDocument.getId() == null) {
                    String error = String.format("Failed to save document: %s. SavedDocument is null or has no ID", document.getFilename());
                    log.error(error);
                    throw new RuntimeException(error);
                }
                
                // Move the original file to success directory
                File originalFile = documentProcessor.getOriginalFile(document.getFilename());
                if (originalFile != null && originalFile.exists()) {
                    boolean moved = fileManagementService.moveToSuccessDirectory(originalFile);
                    if (moved) {
                        log.info("Successfully moved file {} to success directory", originalFile.getName());
                    } else {
                        log.warn("Failed to move file {} to success directory", originalFile.getName());
                    }
                    // Remove from tracking regardless of move success to prevent memory leaks
                    documentProcessor.removeTrackedFile(document.getFilename());
                } else {
                    log.warn("Original file for document {} not found or no longer exists", document.getFilename());
                }
                
            } catch (DataIntegrityViolationException e) {
                String error = String.format("Database error saving document %s: %s", document.getFilename(), e.getMessage());
                log.error(error, e);
                
                // Move the original file to failure directory
                File originalFile = documentProcessor.getOriginalFile(document.getFilename());
                if (originalFile != null && originalFile.exists()) {
                    fileManagementService.moveToFailureDirectory(originalFile);
                    documentProcessor.removeTrackedFile(document.getFilename());
                }
                
                throw new RuntimeException(error, e);
            } catch (Exception e) {
                String error = String.format("Error saving document %s: %s", document.getFilename(), e.getMessage());
                log.error(error, e);
                
                // Move the original file to failure directory
                File originalFile = documentProcessor.getOriginalFile(document.getFilename());
                if (originalFile != null && originalFile.exists()) {
                    fileManagementService.moveToFailureDirectory(originalFile);
                    documentProcessor.removeTrackedFile(document.getFilename());
                }
                
                throw new RuntimeException(error, e);
            }
        }
        
        log.debug("Successfully processed batch of {} documents", documents.size());
    }
}
