package com.rk.ai.rag.reader;

import java.io.InputStream;

/**
 * Interface for extracting text content from various document formats.
 */
public interface DocumentReader {
    
    /**
     * Extracts text content from the provided input stream.
     * 
     * @param inputStream the input stream of the document
     * @param filename the original filename (used for error messages)
     * @return extracted text content
     * @throws Exception if text extraction fails
     */
    String extractText(InputStream inputStream, String filename) throws Exception;
}
