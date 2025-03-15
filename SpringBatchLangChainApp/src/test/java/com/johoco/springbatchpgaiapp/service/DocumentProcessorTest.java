package com.johoco.springbatchpgaiapp.service;

import com.johoco.springbatchpgaiapp.model.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    private FileManagementService fileManagementService;
    
    private DocumentProcessor documentProcessor;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentProcessor = new DocumentProcessor(embeddingStore, fileManagementService);
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
        assertEquals("PROCESSED", result.getStatus(), "Status should be PROCESSED");
        assertNotNull(result.getEmbedding(), "Embedding should not be null");
        
        // Verify embedding store was called
        verify(embeddingStore, times(1)).add(any(), any(TextSegment.class));
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
        
        when(fileManagementService.moveToFailureDirectory(any(File.class))).thenReturn(true);
        
        // When
        Document result = documentProcessor.process(file);
        
        // Then
        assertNull(result, "Result should be null for empty file");
        verify(fileManagementService).moveToFailureDirectory(file);
        verifyNoInteractions(embeddingStore);
    }
    
    @Test
    void testGetAndRemoveTrackedFile() throws Exception {
        // Given
        String content = "This is a test document for processing";
        Path testFile = tempDir.resolve("tracked.txt");
        Files.writeString(testFile, content);
        File file = testFile.toFile();
        
        // Process the file to track it
        Document result = documentProcessor.process(file);
        assertNotNull(result);
        
        // When - get the tracked file
        File trackedFile = documentProcessor.getOriginalFile("tracked.txt");
        
        // Then
        assertNotNull(trackedFile, "Should return the tracked file");
        assertEquals(file.getAbsolutePath(), trackedFile.getAbsolutePath(), "Should return the correct file");
        
        // When - remove the tracked file
        documentProcessor.removeTrackedFile("tracked.txt");
        
        // Then
        assertNull(documentProcessor.getOriginalFile("tracked.txt"), 
                "Should return null after removing the tracked file");
    }
}
