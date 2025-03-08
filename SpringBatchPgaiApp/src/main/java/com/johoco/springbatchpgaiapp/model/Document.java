package com.johoco.springbatchpgaiapp.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "documents", uniqueConstraints = {
    @UniqueConstraint(columnNames = "filename")
})
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;
    
    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "embedding", columnDefinition = "vector")
    private float[] embedding;

    private Long fileSize;
    private String status;
    
    @Column(name = "last_modified")
    private Long lastModified;
}
