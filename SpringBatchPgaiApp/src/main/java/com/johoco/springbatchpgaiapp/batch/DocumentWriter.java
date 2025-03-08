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
                        log.info("Updating existing document: {}", document.getFilename());
                        existing.setContent(document.getContent());
                        existing.setEmbedding(document.getEmbedding());
                        existing.setFileSize(document.getFileSize());
                        existing.setLastModified(document.getLastModified());
                        existing.setStatus(document.getStatus());
                        documentRepository.save(existing);
                        log.info("Successfully updated document: {}", document.getFilename());
                    },
                    () -> {
                        log.info("Saving new document: {}", document.getFilename());
                        documentRepository.save(document);
                        log.info("Successfully saved new document: {}", document.getFilename());
                    }
                );
        });
        log.info("Successfully wrote batch of {} documents", documents.size());
    }
}
