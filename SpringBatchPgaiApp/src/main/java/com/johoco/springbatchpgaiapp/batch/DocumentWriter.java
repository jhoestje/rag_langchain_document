package com.johoco.springbatchpgaiapp.batch;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.model.DocumentMetadata;
import com.johoco.springbatchpgaiapp.model.DocumentStatus;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import com.johoco.springbatchpgaiapp.util.FileOperations;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWriter implements ItemWriter<Document> {
    private final DocumentRepository documentRepository;
    private final FileOperations fileOperations;
    
    @Value("${document.input.directory}")
    private String inputDirectory;
    
    @Value("${document.output.directory}")
    private String outputDirectory;
    
    @Value("${document.output.failed-directory}")
    private String failedDirectory;
    
    private String currentFileName;
    
    @PostConstruct
    public void init() {
        log.info("DocumentWriter initialized with documentRepository: {}", documentRepository != null ? "injected" : "null");
    }
    
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        this.currentFileName = jobParameters.getString("fileName");
        log.info("DocumentWriter initialized with fileName parameter: {}", currentFileName);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Chunk<? extends Document> documents) {
        if (documentRepository == null) {
            log.error("DocumentRepository is null! Document saving will fail.");
            throw new IllegalStateException("DocumentRepository is not initialized");
        }
        
        int size = documents.size();
        log.debug("Writing {} document{}", size, size == 1 ? "" : "s");
        
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
                    existing.setFileSize(document.getFileSize());
                    existing.setLastModified(document.getLastModified());
                    existing.setStatus(document.getStatus());
                    existing.setMetadata(document.getMetadata());
                    savedDocument = documentRepository.save(existing);
                    log.info("Successfully updated document: {} with id: {}", savedDocument.getFilename(), savedDocument.getId());
                } else {
                    log.debug("No existing document found for filename: {}, creating new", document.getFilename());
                    savedDocument = documentRepository.save(document);
                    log.info("Successfully saved new document: {} with id: {}", savedDocument.getFilename(), savedDocument.getId());
                }
            } catch (DataIntegrityViolationException e) {
                log.error("Data integrity violation saving document {}: {}", document.getFilename(), e.getMessage(), e);
                // Mark the document as failed
                markDocumentAsFailed(document, "data integrity violation");
                // Still throw the exception to fail the job
                throw e;
            } catch (Exception e) {
                log.error("Error saving document {}: {}", document.getFilename(), e.getMessage(), e);
                // Mark the document as failed
                markDocumentAsFailed(document, "error");
                // Still throw the exception to fail the job
                throw new RuntimeException("Error saving document: " + document.getFilename(), e);
            }
        }
    }
    
    /**
     * Marks a document as failed and attempts to save it to the database.
     * 
     * @param document the document to mark as failed
     * @param reason the reason for the failure (for logging purposes)
     */
    private void markDocumentAsFailed(Document document, String reason) {
        try {
            document.setStatus(DocumentStatus.FAILED);
            
            // Update metadata with failure information
            DocumentMetadata metadata = document.getMetadata();
            if (metadata == null) {
                metadata = DocumentMetadata.builder()
                        .originalFilename(document.getFilename())
                        .processingTime(Instant.now())
                        .build();
                document.setMetadata(metadata);
            }
            
            Document savedDocument = documentRepository.save(document);
            log.info("Marked document as FAILED due to {}: {}", reason, savedDocument.getFilename());
        } catch (Exception saveEx) {
            log.error("Could not save document with FAILED status: {}", saveEx.getMessage(), saveEx);
        }
    }
    
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (currentFileName == null) {
            log.warn("No fileName parameter found in job parameters");
            return stepExecution.getExitStatus();
        }
        
        try {
            File inputFile = new File(inputDirectory, currentFileName);
            if (!inputFile.exists()) {
                log.warn("Could not find file to move: {}", inputFile.getAbsolutePath());
                return stepExecution.getExitStatus();
            }
            
            boolean isSuccessful = ExitStatus.COMPLETED.equals(stepExecution.getExitStatus());
            String targetDirectory = isSuccessful ? outputDirectory : failedDirectory;
            
            log.info("Step execution status: {}, moving file to {}", 
                    stepExecution.getExitStatus().getExitCode(), 
                    targetDirectory);
            
            File movedFile = fileOperations.moveFile(inputFile, targetDirectory, !isSuccessful);
            log.info("Successfully moved file to: {}", movedFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("Error moving file {}: {}", currentFileName, e.getMessage(), e);
            // We don't want to fail the job if moving the file fails, but we should mark it as a warning
            if (ExitStatus.COMPLETED.equals(stepExecution.getExitStatus())) {
                return ExitStatus.COMPLETED.addExitDescription("File processed successfully but could not be moved: " + e.getMessage());
            }
        }
        
        return stepExecution.getExitStatus();
    }
}
