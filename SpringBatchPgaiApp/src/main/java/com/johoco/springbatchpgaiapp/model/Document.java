package com.johoco.springbatchpgaiapp.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "documents", uniqueConstraints = {
    @UniqueConstraint(name = "uk_documents_filename", columnNames = "filename")
})
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String filename;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_modified", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant lastModified;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;
    
    @Transient
    private DocumentMetadata metadata;
    
    /**
     * Gets the metadata object by deserializing the JSON string.
     * 
     * @return the metadata object
     */
    public DocumentMetadata getMetadata() {
        if (metadata == null && metadataJson != null && !metadataJson.isEmpty()) {
            try {
                metadata = new ObjectMapper().readValue(metadataJson, DocumentMetadata.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing metadata", e);
            }
        }
        return metadata;
    }
    
    /**
     * Sets the metadata object and serializes it to JSON.
     * 
     * @param metadata the metadata object to set
     */
    public void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
        if (metadata != null) {
            try {
                this.metadataJson = new ObjectMapper().writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing metadata", e);
            }
        } else {
            this.metadataJson = null;
        }
    }
}
