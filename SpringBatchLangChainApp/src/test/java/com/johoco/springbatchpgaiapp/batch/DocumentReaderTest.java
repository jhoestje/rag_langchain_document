package com.johoco.springbatchpgaiapp.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentReaderTest {

    private DocumentReader documentReader;
    
    @TempDir
    Path tempDir;
    
    @Mock
    private StepExecution stepExecution;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentReader = new DocumentReader();
        
        // Use reflection to set the inputDirectory field
        try {
            java.lang.reflect.Field field = DocumentReader.class.getDeclaredField("inputDirectory");
            field.setAccessible(true);
            field.set(documentReader, tempDir.toString());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set inputDirectory field: " + e.getMessage());
        }
    }
    
    @Test
    void testReadNonExistentFile() throws Exception {
        // Given a non-existent file
        String fileName = "nonexistent.txt";
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", fileName)
                .toJobParameters();
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        
        // Set the fileName parameter
        documentReader.beforeStep(stepExecution);
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When/Then
        assertThrows(ItemStreamException.class, () -> {
            documentReader.open(executionContext);
        }, "Should throw exception for non-existent file");
    }
    
    @Test
    void testReadSingleFile() throws Exception {
        // Given a directory with a single file
        String fileName = "test.txt";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, "Test content");
        
        // Set up the job parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", fileName)
                .toJobParameters();
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        
        // Set the fileName parameter
        documentReader.beforeStep(stepExecution);
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When
        documentReader.open(executionContext);
        File file = documentReader.read();
        File secondRead = documentReader.read();
        
        // Then
        assertNotNull(file, "A file should be read");
        assertEquals(fileName, file.getName(), "File name should match");
        assertNull(secondRead, "No more files should be available after first read");
    }
    
    @Test
    void testReadWithNoFileName() throws Exception {
        // Given no fileName parameter
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        
        // Set the fileName parameter (which will be null)
        documentReader.beforeStep(stepExecution);
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When/Then
        assertThrows(ItemStreamException.class, () -> {
            documentReader.open(executionContext);
        }, "Should throw exception when no fileName parameter is provided");
    }
    
    @Test
    void testReopen() throws Exception {
        // Given a file
        String fileName = "test.txt";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, "Test content");
        
        // Set up the job parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", fileName)
                .toJobParameters();
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        
        // Set the fileName parameter
        documentReader.beforeStep(stepExecution);
        
        ExecutionContext executionContext = new ExecutionContext();
        
        // When opened first time
        documentReader.open(executionContext);
        File file = documentReader.read(); // Read the file
        assertNotNull(file, "File should be read");
        assertNull(documentReader.read(), "No more files should be available");
        
        // Then close and reopen
        documentReader.close();
        documentReader.open(executionContext);
        
        // Then - should read the file again
        assertNotNull(documentReader.read(), "File should be read after reopen");
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
    
    @Test
    void testMissingFile() throws Exception {
        // Given a file that exists when opened but is deleted before read
        String fileName = "disappearing.txt";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, "Test content");
        
        // Set up the job parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", fileName)
                .toJobParameters();
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        
        // Set the fileName parameter
        documentReader.beforeStep(stepExecution);
        
        ExecutionContext executionContext = new ExecutionContext();
        documentReader.open(executionContext);
        
        // Delete the file before reading
        Files.delete(testFile);
        
        // The reader should still return the file reference even if the file was deleted
        File file = documentReader.read();
        assertNotNull(file, "Should return file reference even if file was deleted");
        assertFalse(file.exists(), "File should no longer exist");
    }
}
