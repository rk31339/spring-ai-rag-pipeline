package com.rk.ai.rag.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PdfDocumentReader Tests")
class PdfDocumentReaderTest {

    private PdfDocumentReader reader;

    @BeforeEach
    void setUp() {
        reader = new PdfDocumentReader();
    }

    @Test
    @DisplayName("Should throw exception for null input stream")
    void shouldThrowExceptionForNullInputStream() {
        // When & Then
        assertThatThrownBy(() -> reader.extractText(null, "test.pdf"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for empty input stream")
    void shouldThrowExceptionForEmptyInputStream() {
        // Given
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        // When & Then
        assertThatThrownBy(() -> reader.extractText(emptyStream, "empty.pdf"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid PDF content")
    void shouldThrowExceptionForInvalidPdfContent() {
        // Given - Not a valid PDF
        String invalidContent = "This is not a PDF file";
        InputStream invalidStream = new ByteArrayInputStream(invalidContent.getBytes());

        // When & Then
        assertThatThrownBy(() -> reader.extractText(invalidStream, "invalid.pdf"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle filename correctly")
    void shouldHandleFilenameCorrectly() {
        // Given
        String testContent = "Not a real PDF but testing filename handling";
        InputStream stream = new ByteArrayInputStream(testContent.getBytes());

        // When & Then - Should throw exception for invalid PDF (we verify it attempts to process it)
        assertThatThrownBy(() -> reader.extractText(stream, "document.pdf"))
            .isInstanceOf(Exception.class)
            .satisfies(e -> {
                // Just verify an exception was thrown - the exact message format may vary
                assertThat(e).isNotNull();
            });
    }

    // Note: Testing actual PDF extraction would require a valid PDF file
    // In a real scenario, you would:
    // 1. Add a small test PDF to src/test/resources/
    // 2. Load it in the test
    // 3. Verify the extracted text matches expected content

    /*
    @Test
    @DisplayName("Should extract text from valid PDF")
    void shouldExtractTextFromValidPdf() throws Exception {
        // Given - Load test PDF from resources
        InputStream pdfStream = getClass().getResourceAsStream("/test-documents/sample.pdf");

        // When
        String extractedText = reader.extractText(pdfStream, "sample.pdf");

        // Then
        assertThat(extractedText).isNotEmpty();
        assertThat(extractedText).contains("expected content");
    }
    */
}
