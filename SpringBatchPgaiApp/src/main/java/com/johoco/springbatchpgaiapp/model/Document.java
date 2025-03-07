package com.johoco.springbatchpgaiapp.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String content;

    @Column(name = "embedding", columnDefinition = "vector")
    private float[] embedding;

    private String contentType;
    private Long fileSize;
    private String status;
    
    @Column(name = "last_modified")
    private Long lastModified;
}
