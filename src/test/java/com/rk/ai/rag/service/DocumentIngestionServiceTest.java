package com.rk.ai.rag.service;

import com.rk.ai.rag.exception.DocumentProcessingException;
import com.rk.ai.rag.model.DocumentRegistry;
import com.rk.ai.rag.model.UploadResponse;
import com.rk.ai.rag.reader.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentIngestionService Tests")
class DocumentIngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private DocumentRegistryService documentRegistryService;

    @Mock
    private PdfDocumentReader pdfReader;

    @Mock
    private WordDocumentReader wordReader;

    @Mock
    private ExcelDocumentReader excelReader;

    @Mock
    private CsvDocumentReader csvReader;

    @Mock
    private JsonDocumentReader jsonReader;

    @Mock
    private TextDocumentReader textReader;

    @InjectMocks
    private DocumentIngestionService service;

    private MockMultipartFile validTextFile;
    private MockMultipartFile validPdfFile;
    private List<Document> mockChunks;

    @BeforeEach
    void setUp() {
        validTextFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "This is test content".getBytes()
        );

        validPdfFile = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );

        // Setup mock chunks
        mockChunks = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", "test.txt");
        mockChunks.add(new Document("Chunk 1", metadata));
        mockChunks.add(new Document("Chunk 2", metadata));
        mockChunks.add(new Document("Chunk 3", metadata));
    }

    @Test
    @DisplayName("Should successfully process single text file")
    void shouldSuccessfullyProcessSingleTextFile() throws Exception {
        // Given
        MultipartFile[] files = {validTextFile};
        String extractedContent = "This is test content";

        when(textReader.extractText(any(), anyString())).thenReturn(extractedContent);
        when(chunkingService.chunkDocument(anyString(), anyMap(), anyString())).thenReturn(mockChunks);
        doNothing().when(vectorStore).add(anyList());
        when(documentRegistryService.findByFilename(anyString())).thenReturn(Optional.empty());
        when(documentRegistryService.register(any(UUID.class), anyString(), anyString(), anyLong(), anyInt()))
            .thenReturn(new DocumentRegistry(UUID.randomUUID(), "test.txt", "hash", 100L, 3));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getTotalFiles()).isEqualTo(1);
        assertThat(response.getProcessedFiles()).isEqualTo(1);
        assertThat(response.getTotalChunks()).isEqualTo(3);

        verify(textReader, times(1)).extractText(any(), anyString());
        verify(chunkingService, times(1)).chunkDocument(anyString(), anyMap(), anyString());
        verify(vectorStore, times(1)).add(anyList());
        verify(documentRegistryService, times(1)).register(any(UUID.class), anyString(), anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should successfully process multiple files")
    void shouldSuccessfullyProcessMultipleFiles() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile("file1", "doc1.txt", "text/plain", "Content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "doc2.md", "text/markdown", "Content 2".getBytes());
        MultipartFile[] files = {file1, file2};

        when(textReader.extractText(any(), anyString())).thenReturn("Content");
        when(chunkingService.chunkDocument(anyString(), anyMap(), anyString())).thenReturn(mockChunks);
        doNothing().when(vectorStore).add(anyList());
        when(documentRegistryService.findByFilename(anyString())).thenReturn(Optional.empty());
        when(documentRegistryService.register(any(UUID.class), anyString(), anyString(), anyLong(), anyInt()))
            .thenReturn(new DocumentRegistry(UUID.randomUUID(), "test.txt", "hash", 100L, 3));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getTotalFiles()).isEqualTo(2);
        assertThat(response.getProcessedFiles()).isEqualTo(2);

        verify(textReader, times(2)).extractText(any(), anyString());
        verify(chunkingService, times(2)).chunkDocument(anyString(), anyMap(), anyString());
        verify(vectorStore, times(2)).add(anyList());
    }

    @Test
    @DisplayName("Should reject empty file")
    void shouldRejectEmptyFile() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            new byte[0]
        );
        MultipartFile[] files = {emptyFile};

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getProcessedFiles()).isEqualTo(0);
        assertThat(response.getDocuments().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(response.getDocuments().get(0).getErrorMessage()).contains("File is empty");

        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("Should reject file exceeding size limit")
    void shouldRejectFileExceedingSizeLimit() {
        // Given - Create 11MB file (exceeds 10MB limit)
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.txt",
            "text/plain",
            largeContent
        );
        MultipartFile[] files = {largeFile};

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getProcessedFiles()).isEqualTo(0);
        assertThat(response.getDocuments().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(response.getDocuments().get(0).getErrorMessage()).contains("exceeds maximum allowed size");

        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("Should reject unsupported file type")
    void shouldRejectUnsupportedFileType() {
        // Given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
            "file",
            "document.xyz",
            "application/unknown",
            "Content".getBytes()
        );
        MultipartFile[] files = {unsupportedFile};

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getProcessedFiles()).isEqualTo(0);
        assertThat(response.getDocuments().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(response.getDocuments().get(0).getErrorMessage()).contains("Unsupported file type");

        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("Should process PDF file")
    void shouldProcessPdfFile() throws Exception {
        // Given
        MultipartFile[] files = {validPdfFile};
        String extractedContent = "PDF text content";

        when(pdfReader.extractText(any(), anyString())).thenReturn(extractedContent);
        when(chunkingService.chunkDocument(anyString(), anyMap(), anyString())).thenReturn(mockChunks);
        doNothing().when(vectorStore).add(anyList());
        when(documentRegistryService.findByFilename(anyString())).thenReturn(Optional.empty());
        when(documentRegistryService.register(any(UUID.class), anyString(), anyString(), anyLong(), anyInt()))
            .thenReturn(new DocumentRegistry(UUID.randomUUID(), "document.pdf", "hash", 100L, 3));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getProcessedFiles()).isEqualTo(1);

        verify(pdfReader, times(1)).extractText(any(), eq("document.pdf"));
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    @DisplayName("Should skip duplicate document with same content")
    void shouldSkipDuplicateDocumentWithSameContent() throws Exception {
        // Given
        MultipartFile[] files = {validTextFile};
        String extractedContent = "This is test content";

        // Calculate actual SHA-256 hash like the service does
        String contentHash = calculateActualSha256Hash(extractedContent);

        DocumentRegistry existingDoc = new DocumentRegistry(
            UUID.randomUUID(), "test.txt", contentHash, 100L, 5
        );

        when(textReader.extractText(any(), anyString())).thenReturn(extractedContent);
        when(documentRegistryService.findByFilename("test.txt")).thenReturn(Optional.of(existingDoc));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getProcessedFiles()).isEqualTo(1);
        assertThat(response.getTotalChunks()).isEqualTo(5); // Uses cached chunk count

        verify(chunkingService, never()).chunkDocument(anyString(), anyMap(), anyString());
        verify(vectorStore, never()).add(anyList());
        verify(documentRegistryService, never()).register(any(), anyString(), anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should update document when content changes")
    void shouldUpdateDocumentWhenContentChanges() throws Exception {
        // Given
        MultipartFile[] files = {validTextFile};
        String extractedContent = "This is NEW test content";

        DocumentRegistry existingDoc = new DocumentRegistry(
            UUID.randomUUID(), "test.txt", "oldHash123", 100L, 5
        );

        when(textReader.extractText(any(), anyString())).thenReturn(extractedContent);
        when(documentRegistryService.findByFilename("test.txt")).thenReturn(Optional.of(existingDoc));
        doNothing().when(documentRegistryService).deleteDocument(any(UUID.class), anyString());
        when(chunkingService.chunkDocument(anyString(), anyMap(), anyString())).thenReturn(mockChunks);
        doNothing().when(vectorStore).add(anyList());
        when(documentRegistryService.register(any(UUID.class), anyString(), anyString(), anyLong(), anyInt()))
            .thenReturn(new DocumentRegistry(UUID.randomUUID(), "test.txt", "newHash", 100L, 3));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getProcessedFiles()).isEqualTo(1);

        verify(documentRegistryService, times(1)).deleteDocument(existingDoc.getDocumentId(), "test.txt");
        verify(chunkingService, times(1)).chunkDocument(anyString(), anyMap(), anyString());
        verify(vectorStore, times(1)).add(anyList());
        verify(documentRegistryService, times(1)).register(any(), anyString(), anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should return partial success when some files fail")
    void shouldReturnPartialSuccessWhenSomeFilesFail() throws Exception {
        // Given
        MockMultipartFile goodFile = new MockMultipartFile("file1", "good.txt", "text/plain", "Content".getBytes());
        MockMultipartFile badFile = new MockMultipartFile("file2", "bad.xyz", "application/unknown", "Content".getBytes());
        MultipartFile[] files = {goodFile, badFile};

        when(textReader.extractText(any(), anyString())).thenReturn("Content");
        when(chunkingService.chunkDocument(anyString(), anyMap(), anyString())).thenReturn(mockChunks);
        doNothing().when(vectorStore).add(anyList());
        when(documentRegistryService.findByFilename(anyString())).thenReturn(Optional.empty());
        when(documentRegistryService.register(any(UUID.class), anyString(), anyString(), anyLong(), anyInt()))
            .thenReturn(new DocumentRegistry(UUID.randomUUID(), "good.txt", "hash", 100L, 3));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(response.getTotalFiles()).isEqualTo(2);
        assertThat(response.getProcessedFiles()).isEqualTo(1);
        assertThat(response.getDocuments()).hasSize(2);

        // Check individual statuses
        assertThat(response.getDocuments().stream()
            .filter(d -> d.getStatus().equals("SUCCESS"))
            .count()).isEqualTo(1);
        assertThat(response.getDocuments().stream()
            .filter(d -> d.getStatus().equals("FAILED"))
            .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle text extraction failure")
    void shouldHandleTextExtractionFailure() throws Exception {
        // Given
        MultipartFile[] files = {validTextFile};
        when(textReader.extractText(any(), anyString())).thenThrow(new IOException("Read error"));

        // When
        UploadResponse response = service.ingestDocuments(files);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getProcessedFiles()).isEqualTo(0);
        assertThat(response.getDocuments().get(0).getStatus()).isEqualTo("FAILED");

        verify(vectorStore, never()).add(anyList());
    }

    // Helper method to calculate actual SHA-256 hash like the service does
    private String calculateActualSha256Hash(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
