package com.rk.ai.rag.service;

import com.rk.ai.rag.model.DocumentRegistry;
import com.rk.ai.rag.repository.DocumentRegistryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentRegistryService Tests")
class DocumentRegistryServiceTest {

    @Mock
    private DocumentRegistryRepository repository;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private DocumentRegistryService service;

    private UUID testDocumentId;
    private String testFilename;
    private String testContentHash;
    private Long testFileSize;
    private Integer testChunkCount;

    @BeforeEach
    void setUp() {
        testDocumentId = UUID.randomUUID();
        testFilename = "test-document.pdf";
        testContentHash = "abc123def456";
        testFileSize = 1024L;
        testChunkCount = 5;
    }

    @Test
    @DisplayName("Should find document by filename when it exists")
    void shouldFindDocumentByFilename() {
        // Given
        DocumentRegistry expectedDoc = new DocumentRegistry(
            testDocumentId, testFilename, testContentHash, testFileSize, testChunkCount
        );
        when(repository.findByFilename(testFilename)).thenReturn(Optional.of(expectedDoc));

        // When
        Optional<DocumentRegistry> result = service.findByFilename(testFilename);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFilename()).isEqualTo(testFilename);
        assertThat(result.get().getContentHash()).isEqualTo(testContentHash);
        verify(repository, times(1)).findByFilename(testFilename);
    }

    @Test
    @DisplayName("Should return empty when document not found")
    void shouldReturnEmptyWhenDocumentNotFound() {
        // Given
        when(repository.findByFilename(testFilename)).thenReturn(Optional.empty());

        // When
        Optional<DocumentRegistry> result = service.findByFilename(testFilename);

        // Then
        assertThat(result).isEmpty();
        verify(repository, times(1)).findByFilename(testFilename);
    }

    @Test
    @DisplayName("Should check if document exists")
    void shouldCheckIfDocumentExists() {
        // Given
        when(repository.existsByFilename(testFilename)).thenReturn(true);

        // When
        boolean exists = service.exists(testFilename);

        // Then
        assertThat(exists).isTrue();
        verify(repository, times(1)).existsByFilename(testFilename);
    }

    @Test
    @DisplayName("Should register new document when not exists")
    void shouldRegisterNewDocument() {
        // Given
        when(repository.findByFilename(testFilename)).thenReturn(Optional.empty());

        ArgumentCaptor<DocumentRegistry> captor = ArgumentCaptor.forClass(DocumentRegistry.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DocumentRegistry result = service.register(
            testDocumentId, testFilename, testContentHash, testFileSize, testChunkCount
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(testDocumentId);
        assertThat(result.getFilename()).isEqualTo(testFilename);
        assertThat(result.getContentHash()).isEqualTo(testContentHash);
        assertThat(result.getFileSize()).isEqualTo(testFileSize);
        assertThat(result.getChunkCount()).isEqualTo(testChunkCount);

        verify(repository, times(1)).findByFilename(testFilename);
        verify(repository, times(1)).save(any(DocumentRegistry.class));
    }

    @Test
    @DisplayName("Should update existing document when already exists")
    void shouldUpdateExistingDocument() {
        // Given
        DocumentRegistry existingDoc = new DocumentRegistry(
            testDocumentId, testFilename, "oldHash", 512L, 3
        );
        when(repository.findByFilename(testFilename)).thenReturn(Optional.of(existingDoc));
        when(repository.save(any(DocumentRegistry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DocumentRegistry result = service.register(
            testDocumentId, testFilename, testContentHash, testFileSize, testChunkCount
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContentHash()).isEqualTo(testContentHash);
        assertThat(result.getFileSize()).isEqualTo(testFileSize);
        assertThat(result.getChunkCount()).isEqualTo(testChunkCount);

        verify(repository, times(1)).findByFilename(testFilename);
        verify(repository, times(1)).save(any(DocumentRegistry.class));
    }

    @Test
    @DisplayName("Should delete document from both vector store and registry")
    void shouldDeleteDocument() {
        // Given
        doNothing().when(vectorStore).delete(any(List.class));
        doNothing().when(repository).deleteById(testDocumentId);

        // When
        service.deleteDocument(testDocumentId, testFilename);

        // Then
        verify(vectorStore, times(1)).delete(eq(List.of(testDocumentId.toString())));
        verify(repository, times(1)).deleteById(testDocumentId);
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() {
        // Given
        doThrow(new RuntimeException("Vector store error"))
            .when(vectorStore).delete(any(List.class));

        // When & Then
        assertThatThrownBy(() -> service.deleteDocument(testDocumentId, testFilename))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to delete document");

        verify(vectorStore, times(1)).delete(any(List.class));
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should generate deterministic document ID from filename")
    void shouldGenerateDeterministicDocumentId() {
        // Given
        String filename1 = "document.pdf";
        String filename2 = "document.pdf";
        String filename3 = "different.pdf";

        // When
        UUID id1 = service.generateDocumentId(filename1);
        UUID id2 = service.generateDocumentId(filename2);
        UUID id3 = service.generateDocumentId(filename3);

        // Then
        assertThat(id1).isEqualTo(id2); // Same filename produces same UUID
        assertThat(id1).isNotEqualTo(id3); // Different filename produces different UUID
    }

    @Test
    @DisplayName("Should handle null filename in generateDocumentId")
    void shouldHandleNullFilenameInGenerateDocumentId() {
        // When & Then
        assertThatThrownBy(() -> service.generateDocumentId(null))
            .isInstanceOf(NullPointerException.class);
    }
}