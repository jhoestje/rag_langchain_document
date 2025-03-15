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
        document.setId(1L); // Set an ID for the document
        
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(documentProcessor.getOriginalFile("test.txt")).thenReturn(mockFile);
        when(fileManagementService.moveToSuccessDirectory(mockFile)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
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
        doc1.setId(1L); // Set an ID for doc1
        
        Document doc2 = createTestDocument("test2.txt", "Test content 2");
        doc2.setId(2L); // Set an ID for doc2
        
        File mockFile1 = mock(File.class);
        File mockFile2 = mock(File.class);
        when(mockFile1.exists()).thenReturn(true);
        when(mockFile2.exists()).thenReturn(true);
        
        when(documentProcessor.getOriginalFile("test1.txt")).thenReturn(mockFile1);
        when(documentProcessor.getOriginalFile("test2.txt")).thenReturn(mockFile2);
        when(fileManagementService.moveToSuccessDirectory(any(File.class))).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            return doc.getFilename().equals("test1.txt") ? doc1 : doc2;
        });
        
        // When
        documentWriter.write(new Chunk<>(Arrays.asList(doc1, doc2)));
        
        // Then
        verify(documentRepository, times(2)).save(any(Document.class));
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
        document.setId(1L); // Set an ID for the document
        
        when(documentProcessor.getOriginalFile("missing.txt")).thenReturn(null);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        
        // Then
        verify(documentRepository).save(document);
        verify(documentProcessor).getOriginalFile("missing.txt");
        verify(fileManagementService, never()).moveToSuccessDirectory(any(File.class));
        verify(documentProcessor, never()).removeTrackedFile("missing.txt");
    }
    
    @Test
    void testWriteDocumentWithFileMovementFailure() throws Exception {
        // Given
        Document document = createTestDocument("error.txt", "Error file content");
        document.setId(1L); // Set an ID for the document
        
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(documentProcessor.getOriginalFile("error.txt")).thenReturn(mockFile);
        when(fileManagementService.moveToSuccessDirectory(mockFile)).thenReturn(false);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
        // When
        documentWriter.write(new Chunk<>(Collections.singletonList(document)));
        
        // Then
        verify(documentRepository).save(document);
        verify(documentProcessor).getOriginalFile("error.txt");
        verify(fileManagementService).moveToSuccessDirectory(mockFile);
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
