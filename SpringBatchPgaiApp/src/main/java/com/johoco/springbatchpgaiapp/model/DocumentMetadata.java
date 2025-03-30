package com.johoco.springbatchpgaiapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Class representing metadata for a document.
 * This class is used for JSON serialization/deserialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentMetadata {
    
    private String originalFilename;
    private Instant processingTime;
    
    // Additional metadata fields can be added here
    private String processorVersion;
    private String processorName;
}
