package com.johoco.springbatchpgaiapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.launch.JobLauncher;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import com.johoco.springbatchpgaiapp.util.FileOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileWatcherServiceTest {

    @Mock
    private JobLauncher jobLauncher;
    
    @Mock
    private Job processDocumentJob;
    
    @Mock
    private FileOperations fileOperations;
    
    private FileWatcherService fileWatcherService;
    
    private String failedDirectory = "failed";
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        // Using reflection to set the fields since we can't use constructor with all fields
        fileWatcherService = new FileWatcherService(jobLauncher, processDocumentJob, fileOperations);
        
        // Use reflection to set the inputDirectory field
        java.lang.reflect.Field field = FileWatcherService.class.getDeclaredField("inputDirectory");
        field.setAccessible(true);
        field.set(fileWatcherService, tempDir.toString());
        
        // Use reflection to set the failedDirectory field for testing
        field = FileWatcherService.class.getDeclaredField("failedDirectory");
        field.setAccessible(true);
        field.set(fileWatcherService, failedDirectory);
    }
    
    @Test
    void testWatchDirectoryWithNoFiles() throws Exception {
        // Given an empty directory
        
        // When
        fileWatcherService.watchDirectory();
        
        // Then
        verifyNoInteractions(jobLauncher);
        verifyNoInteractions(fileOperations);
    }
    
    @Test
    void testWatchDirectoryWithOneFile() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        
        JobExecution mockExecution = mock(JobExecution.class);
        when(mockExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobLauncher.run(eq(processDocumentJob), any(JobParameters.class))).thenReturn(mockExecution);
        
        // When
        fileWatcherService.watchDirectory();
        
        // Then
        verify(jobLauncher, times(1)).run(eq(processDocumentJob), any(JobParameters.class));
        verifyNoMoreInteractions(fileOperations); // File should be moved by DocumentWriter
    }
    
    @Test
    void testWatchDirectoryWithFailedJob() throws Exception {
        // Given
        Path testFile = tempDir.resolve("error.txt");
        Files.writeString(testFile, "Error content");
        
        JobExecution mockExecution = mock(JobExecution.class);
        when(mockExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);
        when(jobLauncher.run(eq(processDocumentJob), any(JobParameters.class))).thenReturn(mockExecution);
        
        // When
        fileWatcherService.watchDirectory();
        
        // Then
        verify(jobLauncher, times(1)).run(eq(processDocumentJob), any(JobParameters.class));
        verify(fileOperations, times(1)).moveToFailed(any(File.class), eq(failedDirectory));
    }
    
    @Test
    void testWatchDirectoryWithException() throws Exception {
        // Given
        Path testFile = tempDir.resolve("exception.txt");
        Files.writeString(testFile, "Exception content");
        
        when(jobLauncher.run(eq(processDocumentJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Test exception"));
        
        // When
        fileWatcherService.watchDirectory();
        
        // Then
        verify(jobLauncher, times(1)).run(eq(processDocumentJob), any(JobParameters.class));
        verify(fileOperations, times(1)).moveToFailed(any(File.class), eq(failedDirectory));
    }
    
    @Test
    void testWatchDirectoryWithMultipleFiles() throws Exception {
        // Given
        Path testFile1 = tempDir.resolve("test1.txt");
        Path testFile2 = tempDir.resolve("test2.txt");
        Files.writeString(testFile1, "Test content 1");
        Files.writeString(testFile2, "Test content 2");
        
        JobExecution mockExecution = mock(JobExecution.class);
        when(mockExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobLauncher.run(eq(processDocumentJob), any(JobParameters.class))).thenReturn(mockExecution);
        
        // When
        fileWatcherService.watchDirectory();
        
        // Then
        verify(jobLauncher, times(2)).run(eq(processDocumentJob), any(JobParameters.class));
        verifyNoMoreInteractions(fileOperations);
    }
    
    @Test
    void testWatchNonExistentDirectory() throws Exception {
        // Given
        // Create a new service instance with a non-existent directory
        FileWatcherService service = new FileWatcherService(jobLauncher, processDocumentJob, fileOperations);
        
        try {
            // Use reflection to set the inputDirectory field
            java.lang.reflect.Field field = FileWatcherService.class.getDeclaredField("inputDirectory");
            field.setAccessible(true);
            field.set(service, tempDir.resolve("nonexistent").toString());
            
            // Use reflection to set the failedDirectory field for testing
            field = FileWatcherService.class.getDeclaredField("failedDirectory");
            field.setAccessible(true);
            field.set(service, failedDirectory);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error setting up test", e);
        }
        
        // When
        service.watchDirectory();
        
        // Then
        verifyNoInteractions(jobLauncher);
        verifyNoInteractions(fileOperations);
        
        // Directory should be created
        assertTrue(Files.exists(tempDir.resolve("nonexistent")));
    }
}
