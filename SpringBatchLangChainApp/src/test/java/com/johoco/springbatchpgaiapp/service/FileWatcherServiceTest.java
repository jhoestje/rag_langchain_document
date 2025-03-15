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
    private FileManagementService fileManagementService;
    
    private FileWatcherService fileWatcherService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileWatcherService = new FileWatcherService(
                jobLauncher,
                processDocumentJob,
                fileManagementService,
                tempDir.toString()
        );
    }
    
    @Test
    void testWatchDirectoryWithNoFiles() throws Exception {
        // Given an empty directory
        
        // When
        fileWatcherService.watchDirectory();
        
        // Then
        verifyNoInteractions(jobLauncher);
        verifyNoInteractions(fileManagementService);
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
        verifyNoMoreInteractions(fileManagementService); // File should be moved by DocumentWriter
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
        verify(fileManagementService, times(1)).moveToFailureDirectory(any(File.class));
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
        verify(fileManagementService, times(1)).moveToFailureDirectory(any(File.class));
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
        verifyNoMoreInteractions(fileManagementService);
    }
    
    @Test
    void testWatchNonExistentDirectory() throws Exception {
        // Given
        FileWatcherService service = new FileWatcherService(
                jobLauncher,
                processDocumentJob,
                fileManagementService,
                tempDir.resolve("nonexistent").toString()
        );
        
        // When
        service.watchDirectory();
        
        // Then
        verifyNoInteractions(jobLauncher);
        verifyNoInteractions(fileManagementService);
        
        // Directory should be created
        assertTrue(Files.exists(tempDir.resolve("nonexistent")));
    }
}
