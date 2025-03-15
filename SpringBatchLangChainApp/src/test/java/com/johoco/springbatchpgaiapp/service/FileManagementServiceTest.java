package com.johoco.springbatchpgaiapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileManagementServiceTest {

    private FileManagementService fileManagementService;
    
    @TempDir
    Path tempDir;
    
    private Path successDir;
    private Path failureDir;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create success and failure directories
        successDir = tempDir.resolve("success");
        failureDir = tempDir.resolve("failure");
        Files.createDirectories(successDir);
        Files.createDirectories(failureDir);
        
        fileManagementService = new FileManagementService(
                successDir.toString(),
                failureDir.toString()
        );
        
        // Initialize the service
        fileManagementService.init();
    }
    
    @Test
    void testMoveToSuccessDirectory() throws Exception {
        // Given
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "Test content");
        File file = sourceFile.toFile();
        
        // When
        boolean result = fileManagementService.moveToSuccessDirectory(file);
        
        // Then
        assertTrue(result, "File should be moved successfully");
        assertFalse(Files.exists(sourceFile), "Source file should no longer exist");
        assertTrue(Files.exists(successDir.resolve("test.txt")), "File should exist in success directory");
        assertEquals("Test content", Files.readString(successDir.resolve("test.txt")), 
                "File content should be preserved");
    }
    
    @Test
    void testMoveToFailureDirectory() throws Exception {
        // Given
        Path sourceFile = tempDir.resolve("error.txt");
        Files.writeString(sourceFile, "Error content");
        File file = sourceFile.toFile();
        
        // When
        boolean result = fileManagementService.moveToFailureDirectory(file);
        
        // Then
        assertTrue(result, "File should be moved successfully");
        assertFalse(Files.exists(sourceFile), "Source file should no longer exist");
        assertTrue(Files.exists(failureDir.resolve("error.txt")), "File should exist in failure directory");
        assertEquals("Error content", Files.readString(failureDir.resolve("error.txt")), 
                "File content should be preserved");
    }
    
    @Test
    void testMoveNonExistentFile() {
        // Given
        File nonExistentFile = new File(tempDir.toString(), "nonexistent.txt");
        
        // When
        boolean successResult = fileManagementService.moveToSuccessDirectory(nonExistentFile);
        boolean failureResult = fileManagementService.moveToFailureDirectory(nonExistentFile);
        
        // Then
        assertFalse(successResult, "Moving non-existent file to success directory should fail");
        assertFalse(failureResult, "Moving non-existent file to failure directory should fail");
    }
    
    @Test
    void testMoveNullFile() {
        // When
        boolean successResult = fileManagementService.moveToSuccessDirectory(null);
        boolean failureResult = fileManagementService.moveToFailureDirectory(null);
        
        // Then
        assertFalse(successResult, "Moving null file to success directory should fail");
        assertFalse(failureResult, "Moving null file to failure directory should fail");
    }
    
    @Test
    void testMoveFileWithSameNameTwice() throws Exception {
        // Given
        Path sourceFile1 = tempDir.resolve("duplicate.txt");
        Files.writeString(sourceFile1, "First content");
        File file1 = sourceFile1.toFile();
        
        // Move first file
        boolean result1 = fileManagementService.moveToSuccessDirectory(file1);
        assertTrue(result1, "First file should be moved successfully");
        
        // Create second file with same name
        Path sourceFile2 = tempDir.resolve("duplicate.txt");
        Files.writeString(sourceFile2, "Second content");
        File file2 = sourceFile2.toFile();
        
        // When - move second file
        boolean result2 = fileManagementService.moveToSuccessDirectory(file2);
        
        // Then - second file should replace first
        assertTrue(result2, "Second file should be moved successfully");
        assertEquals("Second content", Files.readString(successDir.resolve("duplicate.txt")), 
                "Second file content should replace first file content");
    }
}
