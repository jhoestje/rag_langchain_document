package com.johoco.springbatchpgaiapp.repository;

import com.johoco.springbatchpgaiapp.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByFilename(String filename);
    
    @Query(value = "SELECT * FROM documents d WHERE d.filename = :filename", nativeQuery = true)
    Optional<Document> findByFilenameNative(@Param("filename") String filename);
    
    @Query(value = "SELECT * FROM documents d WHERE d.embedding <-> :embedding\\:\\:real[] LIMIT :limit", nativeQuery = true)
    List<Document> findSimilarDocuments(@Param("embedding") float[] embedding, @Param("limit") int limit);
}
