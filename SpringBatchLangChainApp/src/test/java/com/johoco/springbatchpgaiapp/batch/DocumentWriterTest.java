package com.johoco.springbatchpgaiapp.batch;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.model.DocumentMetadata;
import com.johoco.springbatchpgaiapp.model.DocumentStatus;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import com.johoco.springbatchpgaiapp.util.FileOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import static org.mockito.Mockito.*;

class DocumentWriterTest {

    @Mock
    private DocumentRepository documentRepository;
    
    // DocumentProcessor is not used in DocumentWriter
    
    @Mock
    private FileOperations fileOperations;
    
    private DocumentWriter documentWriter;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentWriter = new DocumentWriter(documentRepository, fileOperations);
        
        // Set the required fields via reflection since they're normally set by @Value
        ReflectionTestUtils.setField(documentWriter, "inputDirectory", "input");
        ReflectionTestUtils.setField(documentWriter, "outputDirectory", "output");
        ReflectionTestUtils.setField(documentWriter, "failedDirectory", "failed");
        ReflectionTestUtils.setField(documentWriter, "currentFileName", "test.txt");
    }
    
    @Test
    void testWriteSingleDocument() throws Exception {
        // Given
        Document document = createTestDocument("test.txt", "Test content");
        document.setId(1L); // Set an ID for the document
        
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(fileOperations.moveFile(any(File.class), eq("output"), eq(false))).thenReturn(mockFile);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
        // Set up StepExecution with COMPLETED status for afterStep
        StepExecution stepExecution = new StepExecution("testStep", new JobExecution(1L));
        stepExecution.setExitStatus(ExitStatus.COMPLETED);
        ReflectionTestUtils.setField(documentWriter, "currentFileName", "test.txt");
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        documentWriter.afterStep(stepExecution);
        
        // Then
        verify(documentRepository).save(document);
        verify(fileOperations).moveFile(any(File.class), eq("output"), eq(false));
    }
    
    @Test
    void testWriteMultipleDocuments() throws Exception {
        // Given
        Document doc1 = createTestDocument("test1.txt", "Test content 1");
        doc1.setId(1L); // Set an ID for doc1
        
        Document doc2 = createTestDocument("test2.txt", "Test content 2");
        doc2.setId(2L); // Set an ID for doc2
        
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            return doc.getFilename().equals("test1.txt") ? doc1 : doc2;
        });
        
        // When
        documentWriter.write(new Chunk<>(Arrays.asList(doc1, doc2)));
        
        // Then
        verify(documentRepository, times(2)).save(any(Document.class));
    }
    
    @Test
    void testWriteDocumentWithNoOriginalFile() throws Exception {
        // Given
        Document document = createTestDocument("missing.txt", "Missing file content");
        document.setId(1L); // Set an ID for the document
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
        // Set up StepExecution with COMPLETED status for afterStep
        StepExecution stepExecution = new StepExecution("testStep", new JobExecution(1L));
        stepExecution.setExitStatus(ExitStatus.COMPLETED);
        ReflectionTestUtils.setField(documentWriter, "currentFileName", "missing.txt");
        
        // Mock file.exists() to return false to simulate missing file
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(false);
        when(mockFile.getAbsolutePath()).thenReturn("input/missing.txt");
        doReturn(mockFile).when(fileOperations).moveFile(any(File.class), anyString(), anyBoolean());
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        ExitStatus exitStatus = documentWriter.afterStep(stepExecution);
        
        // Then
        verify(documentRepository).save(document);
        verify(fileOperations, never()).moveFile(any(File.class), anyString(), anyBoolean());
        assertEquals(ExitStatus.COMPLETED, exitStatus);
    }
    
    @Test
    void testWriteDocumentWithFileMovementFailure() throws Exception {
        // Given
        Document document = createTestDocument("error.txt", "Error file content");
        document.setId(1L); // Set an ID for the document
        
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getAbsolutePath()).thenReturn("input/error.txt");
        when(fileOperations.moveFile(any(File.class), eq("output"), eq(false)))
            .thenThrow(new IOException("Failed to move file"));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
        // Set up StepExecution with COMPLETED status for afterStep
        StepExecution stepExecution = new StepExecution("testStep", new JobExecution(1L));
        stepExecution.setExitStatus(ExitStatus.COMPLETED);
        ReflectionTestUtils.setField(documentWriter, "currentFileName", "error.txt");
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        ExitStatus exitStatus = documentWriter.afterStep(stepExecution);
        
        // Then
        verify(documentRepository).save(document);
        verify(fileOperations).moveFile(any(File.class), eq("output"), eq(false));
        assertTrue(exitStatus.getExitDescription().contains("File processed successfully but could not be moved"));
    }
    
    @Test
    void testWriteEmptyChunk() throws Exception {
        // Given
        Chunk<Document> emptyChunk = new Chunk<>();
        
        // When
        documentWriter.write(emptyChunk);
        
        // Then
        verifyNoInteractions(documentRepository);
        verifyNoInteractions(fileOperations);
    }
    
    private Document createTestDocument(String filename, String content) {
        Document document = new Document();
        document.setFilename(filename);
        document.setContent(content);
        document.setFileSize((long) content.length());
        document.setLastModified(Instant.now());
        document.setStatus(DocumentStatus.PROCESSED);
        document.setEmbedding(new float[] {0.1f, 0.2f, 0.3f});
        
        // Add metadata
        DocumentMetadata metadata = DocumentMetadata.builder()
                .originalFilename(filename)
                .processingTime(Instant.now())
                .processorName("TestProcessor")
                .processorVersion("1.0.0")
                .build();
        document.setMetadata(metadata);
        
        return document;
    }
}
