package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.model.DocumentMetadata;
import com.johoco.springbatchpgaiapp.model.DocumentStatus;
import com.johoco.springbatchpgaiapp.util.FileOperations;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentProcessorTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;
    
    @Mock
    private FileOperations fileOperations;
    
    private DocumentProcessor documentProcessor;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentProcessor = new DocumentProcessor(embeddingStore, fileOperations);
        
        // Set application name and version via reflection
        ReflectionTestUtils.setField(documentProcessor, "applicationName", "test-app");
        ReflectionTestUtils.setField(documentProcessor, "applicationVersion", "1.0.0");
    }
    
    @Test
    void testProcessValidFile() throws Exception {
        // Given
        String content = "This is a test document for processing";
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, content);
        File file = testFile.toFile();
        
        // When
        Document result = documentProcessor.process(file);
        
        // Then
        assertNotNull(result, "Processed document should not be null");
        assertEquals("test.txt", result.getFilename(), "Filename should match");
        assertEquals(content, result.getContent(), "Content should match");
        assertEquals(content.length(), result.getFileSize(), "File size should match content length");
        assertNotNull(result.getLastModified(), "Last modified date should be set");
        assertEquals(DocumentStatus.PROCESSED, result.getStatus(), "Status should be PROCESSED");
        assertNotNull(result.getEmbedding(), "Embedding should not be null");
        
        // Verify embedding store was called
        verify(embeddingStore, times(1)).add(any(), any(TextSegment.class));
        
        // Verify metadata was set
        assertNotNull(result.getMetadata(), "Metadata should not be null");
        DocumentMetadata metadata = result.getMetadata();
        assertEquals("test.txt", metadata.getOriginalFilename(), "Original filename should be set in metadata");
        assertNotNull(metadata.getProcessingTime(), "Processing time should be set");
        assertEquals("test-app", metadata.getProcessorName(), "Processor name should be set");
        assertEquals("1.0.0", metadata.getProcessorVersion(), "Processor version should be set");
    }
    
    @Test
    void testProcessNullFile() throws Exception {
        // When
        Document result = documentProcessor.process(null);
        
        // Then
        assertNull(result, "Result should be null for null file");
        verifyNoInteractions(embeddingStore);
    }
    
    @Test
    void testProcessEmptyFile() throws Exception {
        // Given
        Path testFile = tempDir.resolve("empty.txt");
        Files.writeString(testFile, "");
        File file = testFile.toFile();
        
        when(fileOperations.readFileContent(any(File.class))).thenReturn("");
        
        // When
        Document result = documentProcessor.process(file);
        
        // Then
        assertNull(result, "Result should be null for empty file");
        verify(fileOperations).readFileContent(file);
        verifyNoInteractions(embeddingStore);
    }
    
    // Note: The file tracking functionality has been moved out of DocumentProcessor
    // as part of the refactoring to create the FileOperations class
}
