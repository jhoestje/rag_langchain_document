package com.johoco.springbatchpgaiapp.batch;

import com.johoco.springbatchpgaiapp.model.Document;
import com.johoco.springbatchpgaiapp.repository.DocumentRepository;
import com.johoco.springbatchpgaiapp.service.DocumentProcessor;
import com.johoco.springbatchpgaiapp.service.FileManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.Chunk;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentWriterTest {

    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private DocumentProcessor documentProcessor;
    
    @Mock
    private FileManagementService fileManagementService;
    
    private DocumentWriter documentWriter;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentWriter = new DocumentWriter(documentRepository, documentProcessor, fileManagementService);
    }
    
    @Test
    void testWriteSingleDocument() throws Exception {
        // Given
        Document document = createTestDocument("test.txt", "Test content");
        File mockFile = mock(File.class);
        when(documentProcessor.getOriginalFile("test.txt")).thenReturn(mockFile);
        when(fileManagementService.moveToSuccessDirectory(mockFile)).thenReturn(true);
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        
        // Then
        verify(documentRepository).save(document);
        verify(documentProcessor).getOriginalFile("test.txt");
        verify(fileManagementService).moveToSuccessDirectory(mockFile);
        verify(documentProcessor).removeTrackedFile("test.txt");
    }
    
    @Test
    void testWriteMultipleDocuments() throws Exception {
        // Given
        Document doc1 = createTestDocument("test1.txt", "Test content 1");
        Document doc2 = createTestDocument("test2.txt", "Test content 2");
        
        File mockFile1 = mock(File.class);
        File mockFile2 = mock(File.class);
        
        when(documentProcessor.getOriginalFile("test1.txt")).thenReturn(mockFile1);
        when(documentProcessor.getOriginalFile("test2.txt")).thenReturn(mockFile2);
        when(fileManagementService.moveToSuccessDirectory(any(File.class))).thenReturn(true);
        
        // When
        documentWriter.write(new Chunk<>(Arrays.asList(doc1, doc2)));
        
        // Then
        verify(documentRepository).save(doc1);
        verify(documentRepository).save(doc2);
        verify(documentProcessor).getOriginalFile("test1.txt");
        verify(documentProcessor).getOriginalFile("test2.txt");
        verify(fileManagementService).moveToSuccessDirectory(mockFile1);
        verify(fileManagementService).moveToSuccessDirectory(mockFile2);
        verify(documentProcessor).removeTrackedFile("test1.txt");
        verify(documentProcessor).removeTrackedFile("test2.txt");
    }
    
    @Test
    void testWriteDocumentWithNoOriginalFile() throws Exception {
        // Given
        Document document = createTestDocument("missing.txt", "Missing file content");
        when(documentProcessor.getOriginalFile("missing.txt")).thenReturn(null);
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        
        // Then
        verify(documentRepository).save(document);
        verify(documentProcessor).getOriginalFile("missing.txt");
        verify(fileManagementService, never()).moveToSuccessDirectory(any(File.class));
        verify(documentProcessor).removeTrackedFile("missing.txt");
    }
    
    @Test
    void testWriteDocumentWithFileMovementFailure() throws Exception {
        // Given
        Document document = createTestDocument("error.txt", "Error file content");
        File mockFile = mock(File.class);
        when(documentProcessor.getOriginalFile("error.txt")).thenReturn(mockFile);
        when(fileManagementService.moveToSuccessDirectory(mockFile)).thenReturn(false);
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        
        // Then
        verify(documentRepository).save(document);
        verify(documentProcessor).getOriginalFile("error.txt");
        verify(fileManagementService).moveToSuccessDirectory(mockFile);
        // Should still remove tracked file even if movement fails
        verify(documentProcessor).removeTrackedFile("error.txt");
    }
    
    @Test
    void testWriteEmptyChunk() throws Exception {
        // Given
        Chunk<Document> emptyChunk = new Chunk<>();
        
        // When
        documentWriter.write(emptyChunk);
        
        // Then
        verifyNoInteractions(documentRepository);
        verifyNoInteractions(documentProcessor);
        verifyNoInteractions(fileManagementService);
    }
    
    private Document createTestDocument(String filename, String content) {
        Document document = new Document();
        document.setFilename(filename);
        document.setContent(content);
        document.setFileSize((long) content.length());
        document.setLastModified(Instant.now());
        document.setStatus("PROCESSED");
        document.setEmbedding(new float[] {0.1f, 0.2f, 0.3f});
        return document;
    }
}
