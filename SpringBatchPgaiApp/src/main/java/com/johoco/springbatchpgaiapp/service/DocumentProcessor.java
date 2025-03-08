package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessor {
    private final EmbeddingModel embeddingModel;

    public DocumentProcessor() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public Document processDocument(File file) {
        try {
            log.info("Processing document: {}", file.getName());
            String content = Files.readString(file.toPath());
            log.info("Document content length: {} characters", content.length());

            log.info("Generating embedding for document: {}", file.getName());
            Response<Embedding> embeddingResponse = embeddingModel.embed(TextSegment.from(content));
            Embedding embedding = embeddingResponse.content();
            List<Float> vectorList = embedding.vectorAsList();
            log.info("Generated embedding with {} dimensions", vectorList.size());

            float[] vectorArray = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vectorArray[i] = vectorList.get(i).floatValue();
            }

            Document document = new Document();
            document.setFilename(file.getName());
            document.setContent(content);
            document.setEmbedding(vectorArray);
            document.setFileSize(file.length());
            document.setLastModified(file.lastModified());
            document.setStatus("PROCESSED");
            log.info("Successfully processed document: {}", file.getName());
            return document;
        } catch (IOException e) {
            log.error("Error processing document {}: {}", file.getName(), e.getMessage(), e);
            throw new RuntimeException("Error processing document: " + file.getName(), e);
        }
    }
}
