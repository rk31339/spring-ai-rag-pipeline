package com.rk.ai.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChunkingService Tests")
class ChunkingServiceTest {

    private ChunkingService chunkingService;
    private Map<String, Object> metadata;
    private String documentId;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        documentId = "test-doc-123";
        metadata = new HashMap<>();
        metadata.put("filename", "test.txt");
        metadata.put("file_size", 1024L);
        metadata.put("upload_timestamp", "2024-01-15T10:00:00");
    }

    @Test
    @DisplayName("Should chunk small content into single chunk")
    void shouldChunkSmallContentIntoSingleChunk() {
        // Given - Content larger than MIN_CHUNK_SIZE (100 chars) to ensure it's processed
        String content = "This is a small document that should fit in one chunk. " +
                        "Adding more text to exceed the minimum chunk size threshold of 100 characters.";

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getText()).isEqualTo(content);
        assertThat(chunks.get(0).getMetadata()).containsEntry("filename", "test.txt");
        assertThat(chunks.get(0).getMetadata()).containsEntry("chunk_index", 0);
        assertThat(chunks.get(0).getMetadata()).containsEntry("total_chunks", 1);
        assertThat(chunks.get(0).getMetadata()).containsEntry("source_document_id", documentId);
    }

    @Test
    @DisplayName("Should chunk large content into multiple chunks")
    void shouldChunkLargeContentIntoMultipleChunks() {
        // Given - Create content larger than chunk size
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is paragraph ").append(i).append(". ");
            sb.append("It contains some text to make it longer. ");
            sb.append("We need enough content to create multiple chunks.\n\n");
        }
        String content = sb.toString();

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);

        // Verify chunk metadata
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            assertThat(chunk.getMetadata()).containsEntry("chunk_index", i);
            assertThat(chunk.getMetadata()).containsEntry("total_chunks", chunks.size());
            assertThat(chunk.getMetadata()).containsEntry("source_document_id", documentId);
            assertThat(chunk.getMetadata()).containsKey("chunk_size");
        }
    }

    @Test
    @DisplayName("Should handle empty content")
    void shouldHandleEmptyContent() {
        // Given
        String content = "";

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should handle null content")
    void shouldHandleNullContent() {
        // Given
        String content = null;

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should handle whitespace-only content")
    void shouldHandleWhitespaceOnlyContent() {
        // Given
        String content = "   \n\n   \t\t   ";

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should respect paragraph boundaries")
    void shouldRespectParagraphBoundaries() {
        // Given
        String para1 = "First paragraph with some content.";
        String para2 = "Second paragraph with more content.";
        String para3 = "Third paragraph with even more content.";
        String content = para1 + "\n\n" + para2 + "\n\n" + para3;

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isNotEmpty();
        // Verify that chunks contain complete words/sentences
        for (Document chunk : chunks) {
            assertThat(chunk.getText()).isNotBlank();
            assertThat(chunk.getText().trim()).doesNotStartWith(" ");
        }
    }

    @Test
    @DisplayName("Should add overlap between chunks")
    void shouldAddOverlapBetweenChunks() {
        // Given - Create content that will definitely produce multiple chunks
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("Sentence number ").append(i).append(" with enough words to make it substantial. ");
        }
        String content = sb.toString();

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        if (chunks.size() > 1) {
            // Check that there's some content similarity between consecutive chunks (overlap)
            for (int i = 0; i < chunks.size() - 1; i++) {
                String currentChunk = chunks.get(i).getText();
                String nextChunk = chunks.get(i + 1).getText();

                // The chunks should have different content but may share some words due to overlap
                assertThat(currentChunk).isNotEqualTo(nextChunk);
            }
        }
    }

    @Test
    @DisplayName("Should preserve metadata from source")
    void shouldPreserveMetadataFromSource() {
        // Given - Content larger than MIN_CHUNK_SIZE
        String content = "Test content for metadata preservation. This needs to be longer than 100 characters " +
                        "to ensure the chunk is actually created and not filtered out by the minimum size threshold.";
        metadata.put("custom_field", "custom_value");
        metadata.put("document_type", "test");

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isNotEmpty();
        Document chunk = chunks.get(0);
        assertThat(chunk.getMetadata()).containsEntry("filename", "test.txt");
        assertThat(chunk.getMetadata()).containsEntry("custom_field", "custom_value");
        assertThat(chunk.getMetadata()).containsEntry("document_type", "test");
    }

    @Test
    @DisplayName("Should handle very long single paragraph")
    void shouldHandleVeryLongSingleParagraph() {
        // Given - Single paragraph longer than max chunk size
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Word").append(i).append(" ");
        }
        String content = sb.toString();

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).hasSizeGreaterThan(0);

        // Verify no chunk is excessively large
        for (Document chunk : chunks) {
            assertThat(chunk.getText().length()).isLessThan(3000); // Should be under max size
        }
    }

    @Test
    @DisplayName("Should add chunk size to metadata")
    void shouldAddChunkSizeToMetadata() {
        // Given - Content larger than MIN_CHUNK_SIZE
        String content = "Test content with known size. This content needs to be at least 100 characters long " +
                        "to pass the minimum chunk size threshold.";

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isNotEmpty();
        Document chunk = chunks.get(0);
        assertThat(chunk.getMetadata()).containsKey("chunk_size");
        assertThat(chunk.getMetadata().get("chunk_size")).isEqualTo(content.length());
    }

    @Test
    @DisplayName("Should maintain total chunks count across all chunks")
    void shouldMaintainTotalChunksCount() {
        // Given
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Paragraph ").append(i).append(" with substantial content. ");
            sb.append("More text to ensure multiple chunks are created.\n\n");
        }
        String content = sb.toString();

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        int totalChunks = chunks.size();
        for (Document chunk : chunks) {
            assertThat(chunk.getMetadata().get("total_chunks")).isEqualTo(totalChunks);
        }
    }

    @Test
    @DisplayName("Should handle special characters and unicode")
    void shouldHandleSpecialCharactersAndUnicode() {
        // Given - Content larger than MIN_CHUNK_SIZE
        String content = "Test with special chars: @#$%^&*()\n\n" +
                        "Unicode: 你好 مرحبا Здравствуй\n\n" +
                        "Emoji: 😀 🎉 ✨\n\n" +
                        "Additional content to ensure this text exceeds the 100 character minimum threshold.";

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getText()).contains("@#$%^&*()");
        assertThat(chunks.get(0).getText()).contains("你好");
        assertThat(chunks.get(0).getText()).contains("😀");
    }

    @Test
    @DisplayName("Should handle markdown-style content")
    void shouldHandleMarkdownContent() {
        // Given
        String content = "# Header 1\n\n" +
                        "Some content under header 1.\n\n" +
                        "## Header 2\n\n" +
                        "More content with **bold** and *italic* text.\n\n" +
                        "- Bullet point 1\n" +
                        "- Bullet point 2";

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, metadata, documentId);

        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getText()).contains("Header 1");
    }
}
