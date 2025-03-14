package com.johoco.springbatchpgaiapp.model;

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

    @Column(name = "embedding", columnDefinition = "vector")
    private float[] embedding;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_modified", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant lastModified;

    @Column(nullable = false)
    private String status;
}
