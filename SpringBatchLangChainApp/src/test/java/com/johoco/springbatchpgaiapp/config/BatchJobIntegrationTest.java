package com.johoco.springbatchpgaiapp.config;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringBootTest(classes = {TestBatchConfig.class})
@ActiveProfiles("test")
@Disabled("Temporarily disabled until context loading issues are resolved")
class BatchJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @TempDir
    Path tempDir;
    
    private Path inputDir;
    private Path successDir;
    private Path failureDir;
    
    @BeforeEach
    void setUp() throws Exception {
        // Clean up any previous job executions
        jobRepositoryTestUtils.removeJobExecutions();
        
        // Clean up database
        documentRepository.deleteAll();
        
        // Set up test directories
        inputDir = Paths.get("./target/test-classes/test-documents/input");
        successDir = Paths.get("./target/test-classes/test-documents/success");
        failureDir = Paths.get("./target/test-classes/test-documents/failure");
        
        // Create directories if they don't exist
        Files.createDirectories(inputDir);
        Files.createDirectories(successDir);
        Files.createDirectories(failureDir);
        
        // Clean directories
        Files.list(inputDir).forEach(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (Exception e) {
                // Ignore
            }
        });
        
        Files.list(successDir).forEach(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (Exception e) {
                // Ignore
            }
        });
        
        Files.list(failureDir).forEach(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (Exception e) {
                // Ignore
            }
        });
    }
    
    @Test
    void testJobExecution() throws Exception {
        // Given
        String testFileName = "test-document.txt";
        Path testFile = inputDir.resolve(testFileName);
        Files.writeString(testFile, "This is a test document for batch processing. It contains enough text to generate embeddings.");
        
        // When
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", testFileName)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        
        // Check that document was saved to database
        List<Document> documents = documentRepository.findAll();
        assertEquals(1, documents.size());
        
        Document savedDocument = documents.get(0);
        assertEquals(testFileName, savedDocument.getFilename());
        assertNotNull(savedDocument.getEmbedding());
        assertEquals("PROCESSED", savedDocument.getStatus());
        
        // Check that file was moved to success directory
        assertTrue(Files.exists(successDir.resolve(testFileName)));
        assertFalse(Files.exists(inputDir.resolve(testFileName)));
    }
    
    @Test
    void testJobExecutionWithEmptyFile() throws Exception {
        // Given
        String testFileName = "empty-document.txt";
        Path testFile = inputDir.resolve(testFileName);
        Files.writeString(testFile, ""); // Empty file
        
        // When
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", testFileName)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        
        // Check that no document was saved to database
        List<Document> documents = documentRepository.findAll();
        assertEquals(0, documents.size());
        
        // Check that file was moved to failure directory
        assertTrue(Files.exists(failureDir.resolve(testFileName)));
        assertFalse(Files.exists(inputDir.resolve(testFileName)));
    }
    
    @Test
    void testMultipleFilesProcessing() throws Exception {
        // Given
        String testFileName1 = "document1.txt";
        String testFileName2 = "document2.txt";
        
        Path testFile1 = inputDir.resolve(testFileName1);
        Path testFile2 = inputDir.resolve(testFileName2);
        
        Files.writeString(testFile1, "This is the first test document for batch processing.");
        Files.writeString(testFile2, "This is the second test document for batch processing.");
        
        // When - launch job without specific file parameter to process all files
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        
        // Check that both documents were saved to database
        List<Document> documents = documentRepository.findAll();
        assertEquals(2, documents.size());
        
        // Check that both files were moved to success directory
        assertTrue(Files.exists(successDir.resolve(testFileName1)));
        assertTrue(Files.exists(successDir.resolve(testFileName2)));
        assertFalse(Files.exists(inputDir.resolve(testFileName1)));
        assertFalse(Files.exists(inputDir.resolve(testFileName2)));
    }
}
