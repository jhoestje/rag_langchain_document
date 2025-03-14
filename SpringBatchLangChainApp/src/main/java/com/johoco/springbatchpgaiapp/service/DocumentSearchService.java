package com.johoco.springbatchpgaiapp.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DocumentSearchService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public DocumentSearchService(EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        log.info("DocumentSearchService initialized with embeddingStore and embeddingModel");
    }

    @PostConstruct
    public void init() {
        log.info("DocumentSearchService ready for semantic search");
    }

    /**
     * Search for documents similar to the query
     * 
     * @param query The search query
     * @param maxResults Maximum number of results to return
     * @param minScore Minimum similarity score (0-1)
     * @return List of matching documents with their similarity scores
     */
    public List<EmbeddingMatch<TextSegment>> searchSimilarDocuments(String query, int maxResults, float minScore) {
        log.info("Searching for documents similar to query: {}", query);
        
        try {
            // Generate embedding for the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // Search for similar documents
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    queryEmbedding, 
                    maxResults,
                    minScore
            );
            
            log.info("Found {} matching documents for query", matches.size());
            return matches;
        } catch (Exception e) {
            log.error("Error searching for documents: {}", e.getMessage(), e);
            throw new RuntimeException("Error performing semantic search", e);
        }
    }
}
