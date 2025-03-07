package com.example.pgai.repository;

import com.example.pgai.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByFilename(String filename);
    
    @Query(value = "SELECT * FROM documents ORDER BY embedding <-> cast(?1 as vector) LIMIT ?2", nativeQuery = true)
    List<Document> findSimilarDocuments(float[] embedding, int limit);
}
