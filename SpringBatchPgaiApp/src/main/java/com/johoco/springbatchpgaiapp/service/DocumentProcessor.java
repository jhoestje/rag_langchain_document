package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Service
public class DocumentProcessor {
    private final EmbeddingModel embeddingModel;

    public DocumentProcessor() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public Document processDocument(File file) {
        try {
            String content = Files.readString(file.toPath());
            float[] embedding = embeddingModel.embed(TextSegment.from(content)).vector();

            Document document = new Document();
            document.setFilename(file.getName());
            document.setContent(content);
            document.setEmbedding(embedding);
            document.setContentType(Files.probeContentType(file.toPath()));
            document.setFileSize(file.length());
            document.setLastModified(file.lastModified());
            document.setStatus("PROCESSED");

            return document;
        } catch (IOException e) {
            log.error("Error processing document: " + file.getName(), e);
            throw new RuntimeException("Failed to process document", e);
        }
    }
}
