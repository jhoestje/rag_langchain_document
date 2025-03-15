package com.johoco.springbatchpgaiapp.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocumentReaderTest {

    private DocumentReader documentReader;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        String inputDirPath = tempDir.toString();
        documentReader = new DocumentReader(inputDirPath);
    }
    
    @Test
    void testReadEmptyDirectory() throws Exception {
        // Given an empty directory
        ExecutionContext executionContext = new ExecutionContext();
        
        // When
        documentReader.open(executionContext);
        File file = documentReader.read();
        
        // Then
        assertNull(file, "No files should be read from an empty directory");
    }
    
    @Test
    void testReadSingleFile() throws Exception {
        // Given a directory with a single file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When
        documentReader.open(executionContext);
        File file = documentReader.read();
        File secondRead = documentReader.read();
        
        // Then
        assertNotNull(file, "A file should be read");
        assertEquals("test.txt", file.getName(), "File name should match");
        assertNull(secondRead, "No more files should be available");
    }
    
    @Test
    void testReadMultipleFiles() throws Exception {
        // Given a directory with multiple files
        Path testFile1 = tempDir.resolve("test1.txt");
        Path testFile2 = tempDir.resolve("test2.txt");
        Files.writeString(testFile1, "Test content 1");
        Files.writeString(testFile2, "Test content 2");
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When
        documentReader.open(executionContext);
        
        // Then - should read both files
        assertNotNull(documentReader.read(), "First file should be read");
        assertNotNull(documentReader.read(), "Second file should be read");
        assertNull(documentReader.read(), "No more files should be available");
    }
    
    @Test
    void testReopen() throws Exception {
        // Given a directory with a file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When opened first time
        documentReader.open(executionContext);
        documentReader.read(); // Read the file
        
        // Then close and reopen
        documentReader.close();
        
        // Add another file before reopening
        Path testFile2 = tempDir.resolve("test2.txt");
        Files.writeString(testFile2, "Test content 2");
        
        documentReader.open(executionContext);
        
        // Then - should read both files again
        assertNotNull(documentReader.read(), "First file should be read after reopen");
        assertNotNull(documentReader.read(), "Second file should be read after reopen");
        assertNull(documentReader.read(), "No more files should be available");
    }
    
    @Test
    void testReadWithoutOpen() {
        // When trying to read without opening
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            documentReader.read();
        });
        
        // Then
        assertTrue(exception.getMessage().contains("must be opened"), 
                "Should throw exception with message about opening reader");
    }
}
