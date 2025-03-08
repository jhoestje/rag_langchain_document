package com.johoco.springbatchpgaiapp.batch;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWriter implements ItemWriter<Document> {
    private final DocumentRepository documentRepository;

    @Override
    public void write(Chunk<? extends Document> documents) {
        log.info("Writing batch of {} documents", documents.size());
        
        documents.forEach(document -> {
            documentRepository.findByFilename(document.getFilename())
                .ifPresentOrElse(
                    existing -> {
                        log.info("Found existing document: {} with id: {}", existing.getFilename(), existing.getId());
                        existing.setContent(document.getContent());
                        existing.setEmbedding(document.getEmbedding());
                        existing.setFileSize(document.getFileSize());
                        existing.setLastModified(document.getLastModified());
                        existing.setStatus(document.getStatus());
                        Document updatedDocument = documentRepository.save(existing);
                        log.info("Successfully updated document: {} with id: {}", updatedDocument.getFilename(), updatedDocument.getId());
                    },
                    () -> {
                        log.info("Creating new document: {}", document.getFilename());
                        Document savedDocument = documentRepository.save(document);
                        log.info("Successfully created new document: {} with id: {}", savedDocument.getFilename(), savedDocument.getId());
                    }
                );
        });
        
        log.info("Successfully processed batch of {} documents", documents.size());
    }
}
