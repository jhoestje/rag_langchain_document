package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class DocumentProcessor implements ItemProcessor<File, Document> {
    private final EmbeddingModel embeddingModel;

    public DocumentProcessor() {
        log.info("Initializing DocumentProcessor with AllMiniLmL6V2EmbeddingModel");
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
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
                Response<Embedding> embeddingResponse = embeddingModel.embed(TextSegment.from(content));
                Embedding embedding = embeddingResponse.content();
                List<Float> vectorList = embedding.vectorAsList();
                float[] vectorArray = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    vectorArray[i] = vectorList.get(i).floatValue();
                }
                document.setEmbedding(vectorArray);
                document.setStatus("PROCESSED");
                log.info("Successfully generated embedding with {} dimensions for document: {}", vectorList.size(), file.getName());
            } catch (Exception e) {
                log.error("Error generating embedding for document {}: {}", file.getName(), e.getMessage(), e);
                document.setEmbedding(null);
                document.setStatus("ERROR_EMBEDDING");
            }
            
            log.info("Successfully processed document: {} with status: {}", document.getFilename(), document.getStatus());
            return document;
        } catch (IOException e) {
            log.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
            throw new RuntimeException("Error processing file: " + file.getName(), e);
        }
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
