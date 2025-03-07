package com.johoco.springbatchpgaiapp.batch;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentWriter implements ItemWriter<Document> {
    private final DocumentRepository documentRepository;

    @Override
    public void write(Chunk<? extends Document> documents) {
        documents.forEach(document -> {
            documentRepository.findByFilename(document.getFilename())
                .ifPresentOrElse(
                    existing -> {
                        existing.setContent(document.getContent());
                        existing.setEmbedding(document.getEmbedding());
                        existing.setContentType(document.getContentType());
                        existing.setFileSize(document.getFileSize());
                        existing.setLastModified(document.getLastModified());
                        existing.setStatus(document.getStatus());
                        documentRepository.save(existing);
                    },
                    () -> documentRepository.save(document)
                );
        });
    }
}
